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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    Column(
        modifier = Modifier.fillMaxSize().background(Ink).padding(horizontal = 22.dp, vertical = 28.dp),
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
                Text("把 AI 带到\n你的设备里面。", color = Color.White, fontSize = 30.sp, lineHeight = 37.sp, fontWeight = FontWeight.Bold)
                Text(
                    "GsBot 正在搭建本地运行环境。模型和运行组件将由应用统一下载、校验与管理。",
                    color = Muted, fontSize = 14.sp, lineHeight = 22.sp
                )
                Button(
                    onClick = { },
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        disabledContainerColor = Accent.copy(alpha = 0.18f),
                        disabledContentColor = Accent
                    )
                ) {
                    Text("初始化功能开发中", modifier = Modifier.padding(vertical = 5.dp), fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Text("运行状态", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        StatusCard(Icons.Rounded.Memory, "本地模型", "尚未安装模型", "模型目录与下载校验模块待接入")
        StatusCard(Icons.Rounded.Terminal, "Linux 运行环境", "尚未初始化", "Android 用户空间运行时正在规划")

        Spacer(modifier = Modifier.weight(1f))
        Text("GSBOT  ·  LOCAL FIRST  ·  VERSION 0.1.0", color = Color(0xFF626873), fontSize = 10.sp, letterSpacing = 1.2.sp)
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
