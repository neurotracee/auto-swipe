package com.neurotracee.autoswipe.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.neurotracee.autoswipe.AppPrefs
import com.neurotracee.autoswipe.BuildConfig
import com.neurotracee.autoswipe.Updater

/**
 * 设置：主题、更新检查方式与更新源、关于。
 * 这里的每一项都是本机偏好，改完立即生效并写回 SharedPreferences。
 */
@Composable
fun SettingsDialog(onClose: () -> Unit, onCheckUpdate: () -> Unit) {
    val ctx = LocalContext.current
    var autoCheck by remember { mutableStateOf(AppPrefs.autoCheck) }
    var source by remember { mutableStateOf(AppPrefs.updateSource) }
    var theme by remember { mutableStateOf(AppPrefs.themeMode) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface) {
            Column(
                Modifier
                    .heightIn(max = (LocalConfiguration.current.screenHeightDp * 0.86f).dp)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "设置",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onClose) { Text("关闭", fontSize = 12.5.sp) }
                }

                Spacer(Modifier.height(10.dp))
                SectionLabel("外观")
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceChip("跟随系统", theme == "system", Modifier.weight(1f)) {
                        theme = "system"
                        AppPrefs.updateTheme("system")
                    }
                    ChoiceChip("浅色", theme == "light", Modifier.weight(1f)) {
                        theme = "light"
                        AppPrefs.updateTheme("light")
                    }
                    ChoiceChip("深色", theme == "dark", Modifier.weight(1f)) {
                        theme = "dark"
                        AppPrefs.updateTheme("dark")
                    }
                }

                Spacer(Modifier.height(16.dp))
                Divider()
                Spacer(Modifier.height(12.dp))
                SectionLabel("更新")
                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "启动时自动检查",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "只读发布页的版本号，找不到新版本就完全不打扰",
                            fontSize = 10.5.sp,
                            lineHeight = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoCheck,
                        onCheckedChange = {
                            autoCheck = it
                            AppPrefs.updateAutoCheck(it)
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = BrandBlue)
                    )
                }

                Spacer(Modifier.height(10.dp))
                Text(
                    "更新源",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceChip("跟随环境", source == "auto", Modifier.weight(1f)) {
                        source = "auto"
                        AppPrefs.updateUpdateSource("auto")
                    }
                    ChoiceChip("Gitee", source == "gitee", Modifier.weight(1f)) {
                        source = "gitee"
                        AppPrefs.updateUpdateSource("gitee")
                    }
                    ChoiceChip("GitHub", source == "github", Modifier.weight(1f)) {
                        source = "github"
                        AppPrefs.updateUpdateSource("github")
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "当前会走：" + AppPrefs.resolvedUpdateSource.let {
                        if (it == "gitee") "Gitee" else "GitHub"
                    } + "（另一个连不上时会自动换过去）",
                    fontSize = 10.5.sp,
                    lineHeight = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                TextButton(onClick = onCheckUpdate, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                    Text("立即检查更新", fontSize = 12.5.sp)
                }

                Spacer(Modifier.height(14.dp))
                Divider()
                Spacer(Modifier.height(12.dp))
                SectionLabel("关于")
                Spacer(Modifier.height(8.dp))
                Text(
                    "自动翻页 v" + BuildConfig.VERSION_NAME,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "一个只做一件事的小工具：按随机间隔帮你上滑。手势与悬浮球都由系统权限控制，" +
                        "你随时能在系统设置里收回；识别、翻页、更新检查之外不做任何联网。",
                    fontSize = 11.sp,
                    lineHeight = 17.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "开源仓库",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        if (!openUrlInBrowser(ctx, REPO_URL)) {
                            android.widget.Toast.makeText(
                                ctx, "没能打开浏览器，请手动访问 " + REPO_URL, android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onClose,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("完成", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun ChoiceChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(999.dp)
    Surface(
        shape = shape,
        color = if (selected) BrandBlue.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .border(
                width = 1.dp,
                color = if (selected) BrandBlue else Color.Transparent,
                shape = shape
            )
            .clickable { onClick() }
    ) {
        Text(
            label,
            Modifier.padding(vertical = 7.dp),
            fontSize = 11.5.sp,
            color = if (selected) BrandBlue else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun Divider() {
    Surface(
        Modifier
            .fillMaxWidth()
            .height(1.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    ) {}
}
