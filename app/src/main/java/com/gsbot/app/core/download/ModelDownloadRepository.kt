package com.gsbot.app.core.download

import android.content.Context
import com.gsbot.app.core.model.ModelDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale
import kotlin.coroutines.coroutineContext

/**
 * App-private, resumable artifact downloader.
 *
 * A model is only returned as installed after its byte length and SHA-256
 * match the trusted catalog metadata. Downloads are restricted to HTTPS.
 */
class ModelDownloadRepository(context: Context) {
    private val modelDirectory = File(context.filesDir, "models").apply { mkdirs() }

    suspend fun download(
        model: ModelDescriptor,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit = { _, _ -> }
    ): File = withContext(Dispatchers.IO) {
        validateDescriptor(model)

        val destination = File(modelDirectory, model.fileName)
        val partial = File(modelDirectory, "${model.fileName}.part")

        if (destination.isFile && destination.length() == model.sizeBytes &&
            sha256(destination).equals(model.sha256, ignoreCase = true)
        ) {
            return@withContext destination
        }
        if (destination.exists()) destination.delete()

        var offset = partial.length()
        if (offset > model.sizeBytes) {
            partial.delete()
            offset = 0L
        }

        val connection = (URL(model.artifactUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = true
            setRequestProperty("Accept-Encoding", "identity")
            if (offset > 0L) setRequestProperty("Range", "bytes=$offset-")
        }

        try {
            connection.connect()
            val status = connection.responseCode

            if (status == 416) {
                if (partial.length() == model.sizeBytes &&
                    sha256(partial).equals(model.sha256, ignoreCase = true)
                ) {
                    promote(partial, destination)
                    return@withContext destination
                }
                partial.delete()
                throw ModelDownloadException("服务器拒绝续传，请重新开始下载")
            }

            val append = offset > 0L && status == HttpURLConnection.HTTP_PARTIAL
            if (append) {
                val range = connection.getHeaderField("Content-Range").orEmpty()
                if (!range.startsWith("bytes $offset-", ignoreCase = true)) {
                    throw ModelDownloadException("服务器返回的续传范围不匹配")
                }
            } else if (status == HttpURLConnection.HTTP_OK) {
                offset = 0L
            } else {
                throw ModelDownloadException("下载请求失败，HTTP $status")
            }

            val responseLength = connection.contentLengthLong
            if (responseLength > 0L && offset + responseLength > model.sizeBytes) {
                throw ModelDownloadException("下载文件大小超过模型目录声明")
            }

            connection.inputStream.use { input ->
                RandomAccessFile(partial, "rw").use { output ->
                    if (append) output.seek(offset) else {
                        output.setLength(0L)
                        output.seek(0L)
                    }

                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var downloaded = if (append) offset else 0L
                    onProgress(downloaded, model.sizeBytes)

                    while (true) {
                        coroutineContext.ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        downloaded += count
                        if (downloaded > model.sizeBytes) {
                            throw ModelDownloadException("下载文件大小超过预期")
                        }
                        onProgress(downloaded, model.sizeBytes)
                    }
                    output.fd.sync()
                }
            }
        } catch (error: ModelDownloadException) {
            throw error
        } catch (error: Exception) {
            // Keep the partial file so a later attempt can resume it.
            throw ModelDownloadException("下载中断：${error.message ?: "网络错误"}", error)
        } finally {
            connection.disconnect()
        }

        if (partial.length() != model.sizeBytes) {
            throw ModelDownloadException(
                "文件大小不匹配：收到 ${partial.length()} / ${model.sizeBytes} 字节"
            )
        }

        val actualHash = sha256(partial)
        if (!actualHash.equals(model.sha256, ignoreCase = true)) {
            partial.delete()
            throw ModelDownloadException("SHA-256 校验失败，已删除不完整或损坏的模型文件")
        }

        promote(partial, destination)
        destination
    }

    fun installedModels(): List<File> =
        modelDirectory.listFiles()?.filter { it.isFile && !it.name.endsWith(".part") }.orEmpty()

    fun deleteModel(fileName: String): Boolean {
        requireSafeFileName(fileName)
        val target = File(modelDirectory, fileName)
        val partial = File(modelDirectory, "$fileName.part")
        val targetDeleted = !target.exists() || target.delete()
        val partialDeleted = !partial.exists() || partial.delete()
        return targetDeleted && partialDeleted
    }

    private fun validateDescriptor(model: ModelDescriptor) {
        requireSafeFileName(model.fileName)
        require(model.sizeBytes > 0L) { "模型文件大小必须大于 0" }
        require(SHA256_PATTERN.matches(model.sha256)) { "模型 SHA-256 格式无效" }
        val url = URL(model.artifactUrl)
        require(url.protocol.equals("https", ignoreCase = true)) {
            "模型下载地址必须使用 HTTPS"
        }
    }

    private fun requireSafeFileName(fileName: String) {
        require(fileName.matches(Regex("[A-Za-z0-9][A-Za-z0-9._-]{0,180}")) &&
            fileName != "." && fileName != ".."
        ) { "不安全的模型文件名" }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") {
            "%02x".format(Locale.ROOT, it)
        }
    }

    private fun promote(partial: File, destination: File) {
        if (destination.exists() && !destination.delete()) {
            throw ModelDownloadException("无法替换旧模型文件")
        }
        if (!partial.renameTo(destination)) {
            throw ModelDownloadException("无法将已验证模型写入正式目录")
        }
    }

    companion object {
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 30_000
        private val SHA256_PATTERN = Regex("[A-Fa-f0-9]{64}")
    }
}

class ModelDownloadException(message: String, cause: Throwable? = null) :
    Exception(message, cause)
