package com.itosfish.colorfeatureenhance.ui.components

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.itosfish.colorfeatureenhance.FeatureMode
import com.itosfish.colorfeatureenhance.R
import com.itosfish.colorfeatureenhance.utils.CLog
import com.itosfish.colorfeatureenhance.utils.CSU
import com.itosfish.colorfeatureenhance.utils.ModuleAutoUpdater
import com.itosfish.colorfeatureenhance.utils.showAboutDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorOSTopAppBar(
    title: String,
    currentMode: FeatureMode,
    onModeChange: (FeatureMode) -> Unit,
    isSearchActive: Boolean = false,
    onSearchClick: (() -> Unit)? = null,
    onRefresh: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var showMoreMenu by remember { mutableStateOf(false) }

    // 动态计算菜单宽度：半屏宽度 + 10dp
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val menuWidth = remember(windowInfo.containerSize.width) {
        with(density) {
            (windowInfo.containerSize.width.toDp() / 2) + 10.dp
        }
    }

    TopAppBar(
        title = { Text(title) },
        actions = {
            // 模式切换按钮
            TextButton(onClick = {
                val next =
                    if (currentMode == FeatureMode.APP) FeatureMode.OPLUS else FeatureMode.APP
                onModeChange(next)
            }) {
                val labelRes =
                    if (currentMode == FeatureMode.APP) R.string.mode_app else R.string.mode_oplus
                Text(text = stringResource(id = labelRes))
            }

            // 搜索按钮（可选）
            if (onSearchClick != null) {
                IconButton(onClick = onSearchClick) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = stringResource(id = R.string.search_feature),
                        tint = if (isSearchActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // 更多菜单按钮
            Box {
                IconButton(onClick = { showMoreMenu = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "更多"
                    )
                }

                // 下拉菜单 - 覆盖TopAppBar
                DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false },
                    offset = DpOffset(
                        x = 0.dp,
                        y = -TopAppBarDefaults.TopAppBarExpandedHeight
                    ), // 向上偏移TopAppBar高度
                    shape = RoundedCornerShape(8.dp), // 增大圆角
                    modifier = Modifier.width(menuWidth) // 动态菜单宽度：半屏+10dp
                ) {
                    // 强制更新模块选项
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "更新模块",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        onClick = {
                            showMoreMenu = false
                            forceUpdateModule(context as Activity)
                        }
                    )

                    // 导出日志选项
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "导出日志",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        onClick = {
                            showMoreMenu = false
                            exportLogs(context as Activity)
                        }
                    )

                    // 关于选项
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(id = R.string.about),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        onClick = {
                            showMoreMenu = false
                            showAboutDialog(context as Activity)
                        }
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors()
    )
}

/**
 * 强制更新模块功能
 */
private fun forceUpdateModule(activity: Activity) {
    CoroutineScope(Dispatchers.Main).launch {
        try {
            Toast.makeText(activity, "正在更新模块，请稍候...", Toast.LENGTH_SHORT).show()
            CLog.i("TopAppBar", "用户手动触发模块更新")

            val updateResult = ModuleAutoUpdater.forceUpdateModule()

            when (updateResult) {
                is ModuleAutoUpdater.UpdateResult.Success -> {
                    Toast.makeText(activity, "模块更新成功！", Toast.LENGTH_SHORT).show()
                    CLog.i("TopAppBar", "手动模块更新成功")
                }
                is ModuleAutoUpdater.UpdateResult.Failed -> {
                    Toast.makeText(activity, "模块更新失败: ${updateResult.reason}", Toast.LENGTH_LONG).show()
                    CLog.e("TopAppBar", "手动模块更新失败: ${updateResult.reason}")
                }
                else -> {
                    Toast.makeText(activity, "模块更新状态未知", Toast.LENGTH_SHORT).show()
                    CLog.w("TopAppBar", "手动模块更新状态未知")
                }
            }
        } catch (e: Exception) {
            Toast.makeText(activity, "模块更新异常: ${e.message}", Toast.LENGTH_LONG).show()
            CLog.e("TopAppBar", "手动模块更新异常", e)
        }
    }
}

/**
 * 导出日志功能 - 使用shell写入/sdcard/
 */
private fun exportLogs(activity: Activity) {
    try {
        val logs = CLog.getFormattedLogs()

        if (logs == "暂无日志记录") {
            Toast.makeText(activity, "暂无日志可导出", Toast.LENGTH_SHORT).show()
            return
        }

        // 生成文件名
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val timestamp = dateFormat.format(Date())
        val fileName = "ColorFeatureEnhance_logs_$timestamp.txt"
        val filePath = "/sdcard/$fileName"

        // 创建临时文件
        val tempFile = java.io.File(activity.cacheDir, fileName)
        tempFile.writeText(logs)

        // 使用shell命令复制到/sdcard/
        val copyCommand = "cp \"${tempFile.absolutePath}\" \"$filePath\""
        val result = CSU.runWithSu(copyCommand)

        if (result.exitCode == 0) {
            // 验证文件是否成功创建
            if (CSU.fileExists(filePath)) {
                Toast.makeText(activity, "日志已导出到: $filePath", Toast.LENGTH_LONG).show()
                CLog.i("TopAppBar", "日志导出成功: $filePath")
            } else {
                Toast.makeText(activity, "日志导出失败: 文件未创建", Toast.LENGTH_SHORT).show()
                CLog.e("TopAppBar", "日志导出失败: 文件未创建")
            }
        } else {
            Toast.makeText(activity, "日志导出失败: Shell命令执行失败", Toast.LENGTH_SHORT).show()
            CLog.e("TopAppBar", "日志导出失败: Shell命令执行失败, 退出码: ${result.exitCode}")
        }

        // 清理临时文件
        tempFile.delete()

    } catch (e: Exception) {
        Toast.makeText(activity, "日志导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
        CLog.e("TopAppBar", "日志导出失败", e)
    }
}