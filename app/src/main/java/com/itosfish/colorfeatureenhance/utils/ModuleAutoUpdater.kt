package com.itosfish.colorfeatureenhance.utils

import android.widget.Toast
import com.itosfish.colorfeatureenhance.MainActivity.Companion.app
import com.itosfish.colorfeatureenhance.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 模块自动更新管理器
 * 负责检测应用版本变化并自动更新模块
 */
object ModuleAutoUpdater {
    private const val TAG = "ModuleAutoUpdater"
    
    /**
     * 更新结果
     */
    sealed class UpdateResult {
        object Success : UpdateResult()
        object NoUpdateNeeded : UpdateResult()
        data class Failed(val reason: String, val exception: Throwable? = null) : UpdateResult()
    }
    
    /**
     * 检查并执行模块自动更新
     * @param showUserNotification 是否显示用户通知
     * @return 更新结果
     */
    suspend fun checkAndUpdateModule(showUserNotification: Boolean = true): UpdateResult = withContext(Dispatchers.IO) {
        try {
            CLog.i(TAG, "开始检查模块自动更新")
            
            // 1. 获取版本信息
            val versionInfo = AppVersionManager.getCurrentVersionInfo()
            if (versionInfo == null) {
                CLog.e(TAG, "无法获取应用版本信息")
                return@withContext UpdateResult.Failed("无法获取应用版本信息")
            }
            
            CLog.i(TAG, AppVersionManager.getVersionChangeDescription(versionInfo))
            
            // 2. 检查是否需要更新模块（基于应用版本变化）
            val needsUpdate = shouldUpdateModuleBasedOnAppVersion(versionInfo)
            if (!needsUpdate) {
                CLog.i(TAG, "模块无需更新")
                return@withContext UpdateResult.NoUpdateNeeded
            }
            
            // 3. 显示更新通知（如果需要）
            if (showUserNotification) {
                showUpdateNotification(versionInfo)
            }
            
            // 4. 执行模块更新
            CLog.i(TAG, "开始执行模块更新...")
            val updateSuccess = performModuleUpdate(versionInfo)
            
            if (updateSuccess) {
                // 5. 保存新的版本信息
                AppVersionManager.saveCurrentVersionInfo()
                
                // 6. 显示成功通知
                if (showUserNotification) {
                    showUpdateSuccessNotification(versionInfo)
                }
                
                CLog.i(TAG, "模块自动更新成功")
                return@withContext UpdateResult.Success
            } else {
                CLog.e(TAG, "模块自动更新失败")
                
                // 显示失败通知
                if (showUserNotification) {
                    showUpdateFailedNotification(versionInfo)
                }
                
                return@withContext UpdateResult.Failed("模块安装失败")
            }
            
        } catch (e: Exception) {
            CLog.e(TAG, "模块自动更新过程中发生异常", e)
            return@withContext UpdateResult.Failed("更新过程异常: ${e.message}", e)
        }
    }

    /**
     * 基于应用版本变化判断是否需要更新模块
     * 只要应用版本发生变化（更新、降级、首次启动），就更新模块
     */
    private fun shouldUpdateModuleBasedOnAppVersion(versionInfo: AppVersionManager.VersionInfo): Boolean {
        return when (versionInfo.changeType) {
            AppVersionManager.VersionChangeType.FIRST_LAUNCH -> {
                CLog.i(TAG, "首次启动，需要安装模块")
                true
            }
            AppVersionManager.VersionChangeType.MAJOR_UPDATE -> {
                CLog.i(TAG, "应用版本更新: ${versionInfo.previousVersionName} -> ${versionInfo.versionName}，需要更新模块")
                true
            }
            AppVersionManager.VersionChangeType.DOWNGRADE -> {
                CLog.w(TAG, "应用版本降级: ${versionInfo.previousVersionName} -> ${versionInfo.versionName}，需要重新安装模块")
                true
            }
            AppVersionManager.VersionChangeType.NO_CHANGE -> {
                CLog.d(TAG, "应用版本无变化，模块无需更新")
                false
            }
        }
    }

    /**
     * 执行模块更新
     */
    private suspend fun performModuleUpdate(versionInfo: AppVersionManager.VersionInfo): Boolean = withContext(Dispatchers.IO) {
        try {
            CLog.i(TAG, "正在更新模块...")
            
            // 记录更新前的模块版本
            val oldModuleVersion = ConfigUtils.moduleVersion
            CLog.d(TAG, "当前模块版本: $oldModuleVersion")
            CLog.d(TAG, "最新模块版本: ${ConfigUtils.LATEST_MODULE_VERSION}")
            
            // 执行模块安装
            val installSuccess = ConfigUtils.installModule()
            
            if (installSuccess) {
                // 验证模块版本是否更新
                val newModuleVersion = ConfigUtils.moduleVersion
                CLog.i(TAG, "模块更新完成: $oldModuleVersion -> $newModuleVersion")
                
                // 记录更新信息到日志
                CLog.i(TAG, "模块同步更新详情:")
                CLog.i(TAG, "  应用版本: ${versionInfo.versionName} (${versionInfo.versionCode})")
                CLog.i(TAG, "  变化类型: ${versionInfo.changeType}")
                CLog.i(TAG, "  模块版本: $oldModuleVersion -> $newModuleVersion")
                CLog.i(TAG, "  更新原因: 应用版本变化触发模块同步更新")
                
                return@withContext true
            } else {
                CLog.e(TAG, "模块安装失败")
                return@withContext false
            }
            
        } catch (e: Exception) {
            CLog.e(TAG, "执行模块更新时发生异常", e)
            return@withContext false
        }
    }
    
    /**
     * 显示更新通知
     */
    private suspend fun showUpdateNotification(versionInfo: AppVersionManager.VersionInfo) = withContext(Dispatchers.Main) {
        val message = when (versionInfo.changeType) {
            AppVersionManager.VersionChangeType.FIRST_LAUNCH -> 
                app.getString(R.string.module_auto_update_first_launch)
            AppVersionManager.VersionChangeType.MAJOR_UPDATE -> 
                app.getString(R.string.module_auto_update_app_updated, versionInfo.previousVersionName, versionInfo.versionName)
            AppVersionManager.VersionChangeType.DOWNGRADE -> 
                app.getString(R.string.module_auto_update_app_downgraded, versionInfo.previousVersionName, versionInfo.versionName)
            AppVersionManager.VersionChangeType.NO_CHANGE -> 
                app.getString(R.string.module_auto_update_module_outdated)
        }
        
        CLog.i(TAG, "显示更新通知: $message")
    }
    
    /**
     * 显示更新成功通知
     */
    private suspend fun showUpdateSuccessNotification(versionInfo: AppVersionManager.VersionInfo) = withContext(Dispatchers.Main) {
        val message = app.getString(R.string.module_auto_update_success, versionInfo.versionName)
        
        Toast.makeText(app, message, Toast.LENGTH_SHORT).show()
        CLog.i(TAG, "显示更新成功通知: $message")
    }
    
    /**
     * 显示更新失败通知
     */
    private suspend fun showUpdateFailedNotification(versionInfo: AppVersionManager.VersionInfo) = withContext(Dispatchers.Main) {
        // 这里可以显示一个对话框，让用户选择重试或手动安装
        CLog.w(TAG, "模块自动更新失败，用户可能需要手动处理")
    }
    
    /**
     * 强制更新模块（忽略版本检查）
     */
    suspend fun forceUpdateModule(): UpdateResult = withContext(Dispatchers.IO) {
        try {
            CLog.i(TAG, "开始强制更新模块")
            
            val installSuccess = ConfigUtils.installModule()
            
            if (installSuccess) {
                // 保存当前版本信息
                AppVersionManager.saveCurrentVersionInfo()
                
                CLog.i(TAG, "强制更新模块成功")
                return@withContext UpdateResult.Success
            } else {
                CLog.e(TAG, "强制更新模块失败")
                return@withContext UpdateResult.Failed("模块安装失败")
            }
            
        } catch (e: Exception) {
            CLog.e(TAG, "强制更新模块时发生异常", e)
            return@withContext UpdateResult.Failed("强制更新异常: ${e.message}", e)
        }
    }
}
