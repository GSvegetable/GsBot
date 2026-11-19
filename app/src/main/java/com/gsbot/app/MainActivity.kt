package com.gsbot.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gsbot.app.core.download.ModelDownloadRepository
import com.gsbot.app.core.model.ModelCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Ink = Color(0xFF101114)
private val Panel = Color(0xFF1B1D22)
private val Muted = Color(0xFF969BA6)
private val Accent = Color(0xFFB8F36B)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Accent,
                    background = Ink,
                    surface = Panel,
                    onPrimary = Ink,
                    onBackground = Color.White,
                    onSurface = Color.White
                )
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = Ink) {
                    HomeScreen()
                }
            }
        }
    }
}

@Composable
private fun HomeScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val model = ModelCatalog.starter
    val repository = remember(context) { ModelDownloadRepository(context) }

    var installed by remember { mutableStateOf(false) }
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var statusText by remember { mutableStateOf("正在检查本地模型…") }

    LaunchedEffect(repository, model) {
        installed = withContext(Dispatchers.IO) { repository.isInstalled(model) }
        statusText = if (installed) "已安装 · SHA-256 校验通过" else "尚未下载"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("GSBOT", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                Text("你的本地 AI 工作空间", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
            }
            Box(
                modifier = Modifier.size(42.dp).background(Panel, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Icon(Icons.Rounded.Tune, contentDescription = "设置", tint = Color.White)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Panel)
        ) {
            Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Text("从这里开始", color = Muted, fontSize = 13.sp)
                Text(
                    "把 AI 带到\n你的设备里面。",
                    color = Color.White,
                    fontSize = 30.sp,
                    lineHeight = 37.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "先安装一个本地模型。下载完成后，GsBot 会校验文件完整性，再交给设备端推理引擎。",
                    color = Muted,
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )
            }
        }

        Text("模型库", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Panel)
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                Text(model.displayName, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Text(model.description, color = Muted, fontSize = 13.sp, lineHeight = 20.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("格式  GGUF", color = Muted, fontSize = 11.sp)
                    Text("许可  ${model.licenseName}", color = Muted, fontSize = 11.sp)
                }
                Text(statusText, color = if (installed) Accent else Muted, fontSize = 12.sp)
                if (downloading) {
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        color = Accent,
                        trackColor = Color(0xFF353941)
                    )
                    Text("${(progress * 100).toInt()}%", color = Muted, fontSize = 11.sp)
                }
                Button(
                    onClick = {
                        scope.launch {
                            downloading = true
                            progress = 0f
                            statusText = "正在连接模型源…"
                            try {
                                repository.download(model) { received, total ->
                                    scope.launch {
                                        progress = if (total > 0L) received.toFloat() / total.toFloat() else 0f
                                        statusText = "正在下载 · ${received / (1024 * 1024)} / ${total / (1024 * 1024)} MiB"
                                    }
                                }
                                installed = true
                                progress = 1f
                                statusText = "已安装 · SHA-256 校验通过"
                            } catch (error: Exception) {
                                statusText = error.message ?: "下载失败，可重试续传"
                            } finally {
                                downloading = false
                            }
                        }
                    },
                    enabled = !installed && !downloading && statusText != "正在检查本地模型…",
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Accent,
                        contentColor = Ink,
                        disabledContainerColor = Accent.copy(alpha = 0.16f),
                        disabledContentColor = Accent
                    )
                ) {
                    Text(
                        when {
                            installed -> "模型已安装"
                            downloading -> "正在下载…"
                            else -> "下载模型 · 约 1.19 GiB"
                        },
                        modifier = Modifier.padding(vertical = 5.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    "来源：Hugging Face · ggml-org。模型权重不会打包进 APK。",
                    color = Color(0xFF777E89),
                    fontSize = 10.sp,
                    lineHeight = 15.sp
                )
            }
        }

        Text("运行环境", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        StatusCard(
            icon = Icons.Rounded.Memory,
            title = "本地推理引擎",
            subtitle = if (installed) "模型已就绪，推理引擎待接入" else "等待安装模型",
            detail = "正在准备 llama.cpp Android NDK 集成"
        )
        StatusCard(
            icon = Icons.Rounded.Terminal,
            title = "Linux 运行环境",
            subtitle = "尚未初始化",
            detail = "Linux 用户空间安装器将在后续阶段接入"
        )

        Text(
            "GSBOT  ·  LOCAL FIRST  ·  VERSION 0.1.0",
            color = Color(0xFF626873),
            fontSize = 10.sp,
            letterSpacing = 1.2.sp
        )
    }
}

@Composable
private fun StatusCard(icon: ImageVector, title: String, subtitle: String, detail: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Panel)
    ) {
        Row(
            modifier = Modifier.padding(17.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Box(
                modifier = Modifier.size(44.dp).background(Color(0xFF292D33), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Icon(icon, contentDescription = null, tint = Accent)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(subtitle, color = Accent, fontSize = 12.sp)
                Text(detail, color = Muted, fontSize = 11.sp)
            }
        }
    }
}
