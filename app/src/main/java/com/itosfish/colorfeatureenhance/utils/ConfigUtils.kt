package com.itosfish.colorfeatureenhance.utils

import android.system.Os
import android.util.Log
import com.itosfish.colorfeatureenhance.MainActivity.Companion.app
import java.io.File
import java.io.FileOutputStream

/**
 * 新架构的配置管理工具类
 * 负责配置目录初始化、模块安装等基础功能
 */
object ConfigUtils {
    const val LATEST_MODULE_VERSION = 20

    private const val TAG = "ConfigUtils"

    // 配置文件名常量
    private const val APP_FEATURES_FILE = "com.oplus.app-features.xml"
    private const val OPLUS_FEATURES_FILE = "com.oplus.oplus-feature.xml"

    // 配置目录路径
    private val baseDir = app.getExternalFilesDir(null)?.absolutePath!!
    private val configsDir = "$baseDir/configs"
    private val systemBaselineDir = "$configsDir/system_baseline"
    private val userPatchesDir = "$configsDir/user_patches"
    private val mergedOutputDir = "$configsDir/merged_output"



    /**
     * 初始化新架构的配置管理系统
     * 创建必要的目录结构，但不复制任何配置文件
     * 配置文件的复制由模块脚本负责
     */
    fun initializeConfigSystem(): Boolean {
        try {
            CLog.i(TAG, "开始初始化配置管理系统")
            CLog.d(TAG, "基础目录: $baseDir")
            CLog.d(TAG, "配置目录: $configsDir")

            // 检查基础目录是否可访问
            val baseFile = File(baseDir)
            if (!baseFile.exists()) {
                CLog.e(TAG, "基础目录不存在: $baseDir")
                return false
            }

            CLog.d(TAG, "基础目录检查: 存在=${baseFile.exists()}, 是目录=${baseFile.isDirectory()}, 可写=${baseFile.canWrite()}")

            // 创建目录结构
            CLog.d(TAG, "创建目录结构...")
            createDirectoryStructure()

            // 确保权限正确
            CLog.d(TAG, "检查并修复权限...")
            val permissionFixed = ensureProperPermissions()
            if (!permissionFixed) {
                CLog.w(TAG, "权限修复失败，但继续执行")
            }

            // 验证目录创建结果
            val success = verifyDirectoryStructure()
            if (success) {
                CLog.i(TAG, "配置管理系统初始化完成")
            } else {
                CLog.e(TAG, "目录结构验证失败")
            }

            return success

        } catch (e: Exception) {
            CLog.e(TAG, "配置系统初始化失败", e)
            return false
        }
    }

    /**
     * 创建新架构的目录结构
     */
    private fun createDirectoryStructure() {
        try {
            val directories = listOf(
                systemBaselineDir to "system_baseline",
                userPatchesDir to "user_patches",
                mergedOutputDir to "merged_output"
            )

            directories.forEach { (path, name) ->
                val dir = File(path)
                if (!dir.exists()) {
                    val success = dir.mkdirs()
                    if (success) {
                        CLog.d(TAG, "成功创建目录: $name ($path)")
                    } else {
                        CLog.w(TAG, "创建目录失败: $name ($path)")
                    }
                } else {
                    CLog.d(TAG, "目录已存在: $name ($path)")
                }
            }
        } catch (e: Exception) {
            CLog.e(TAG, "创建目录结构时发生异常", e)
            throw e // 重新抛出异常，因为目录创建失败是严重问题
        }
    }



    /**
     * 验证目录结构是否正确创建
     */
    private fun verifyDirectoryStructure(): Boolean {
        val directories = listOf(
            systemBaselineDir to "system_baseline",
            userPatchesDir to "user_patches",
            mergedOutputDir to "merged_output"
        )

        var allSuccess = true
        directories.forEach { (path, name) ->
            val dir = File(path)
            val exists = dir.exists()
            val isDirectory = dir.isDirectory()

            CLog.d(TAG, "目录检查: $name - 存在:$exists, 是目录:$isDirectory")

            if (exists && isDirectory) {
                CLog.d(TAG, "目录验证成功: $name")
            } else {
                CLog.e(TAG, "目录验证失败: $name ($path) - 存在:$exists, 是目录:$isDirectory")
                allSuccess = false
            }
        }

        return allSuccess
    }

    /**
     * 确保配置目录有正确的权限（简化版：统一使用660权限）
     */
    private fun ensureProperPermissions(): Boolean {
        return try {
            val configDir = File(configsDir)
            if (!configDir.exists()) {
                CLog.d(TAG, "配置目录不存在，无需修复权限")
                return true
            }

            // 检查是否可写
            if (!configDir.canWrite()) {
                CLog.w(TAG, "配置目录无写权限，使用简化权限修复（660权限）")

                val fixCmd = """
                    CONFIG_DIR="$configsDir"

                    # 检查配置目录是否存在
                    if [ ! -d "${'$'}CONFIG_DIR" ]; then
                        echo "配置目录不存在: ${'$'}CONFIG_DIR"
                        exit 1
                    fi

                    echo "设置配置目录及所有子目录和文件为660权限..."

                    # 统一设置660权限
                    chmod 660 "${'$'}CONFIG_DIR" 2>/dev/null
                    find "${'$'}CONFIG_DIR" -type d -exec chmod 660 {} \; 2>/dev/null
                    find "${'$'}CONFIG_DIR" -type f -exec chmod 660 {} \; 2>/dev/null

                    # 验证结果
                    FINAL_PERM=${'$'}(stat -c %a "${'$'}CONFIG_DIR" 2>/dev/null)
                    echo "简化权限修复完成: 权限=${'$'}FINAL_PERM"
                """.trimIndent()

                val result = CSU.runWithSu(fixCmd)
                CLog.i(TAG, "简化权限修复结果: ${result.output}")

                // 重新检查是否可写
                val canWriteAfterFix = configDir.canWrite()
                CLog.i(TAG, "简化权限修复后可写状态: $canWriteAfterFix")
                return canWriteAfterFix
            } else {
                CLog.d(TAG, "配置目录权限正常")
                return true
            }
        } catch (e: Exception) {
            CLog.e(TAG, "简化权限检查失败", e)
            false
        }
    }

    /**
     * 检查系统基线配置是否存在
     */
    fun hasSystemBaseline(): Boolean {
        return File(systemBaselineDir, APP_FEATURES_FILE).exists() &&
                File(systemBaselineDir, OPLUS_FEATURES_FILE).exists()
    }

    /**
     * 检查合并输出配置是否存在
     */
    fun hasMergedOutput(): Boolean {
        return File(mergedOutputDir, APP_FEATURES_FILE).exists() &&
                File(mergedOutputDir, OPLUS_FEATURES_FILE).exists()
    }

    /**
     * 获取配置目录路径
     */
    fun getConfigPaths(): ConfigPaths {
        return ConfigPaths(
            configsDir = configsDir,
            systemBaselineDir = systemBaselineDir,
            userPatchesDir = userPatchesDir,
            mergedOutputDir = mergedOutputDir,
            appFeaturesFile = APP_FEATURES_FILE,
            oplusFeaturesFile = OPLUS_FEATURES_FILE
        )
    }

    data class ConfigPaths(
        val configsDir: String,
        val systemBaselineDir: String,
        val userPatchesDir: String,
        val mergedOutputDir: String,
        val appFeaturesFile: String,
        val oplusFeaturesFile: String
    )

    /**
     * 安装模块（简化版）
     * 从 assets 解压模块到指定目录
     */
    fun installModule(): Boolean {
        return try {
            Log.i(TAG, "开始安装模块")

            // 临时工作目录
            val tempDir = File(app.cacheDir, "moduleTemp").apply {
                if (exists()) deleteRecursively()
                mkdirs()
            }

            // 解压模块文件
            val tempZip = File(tempDir, "mod.zip")
            if (!extractModuleAssets(tempZip)) {
                Log.e(TAG, "解压模块资源失败")
                return false
            }

            if (CSU.installModule(tempZip.absolutePath)) {
                Log.i(TAG, "模块安装成功")
                true
            } else {
                Log.e(TAG, "模块安装失败")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "模块安装过程中发生异常", e)
            false
        }
    }

    /**
     * 从 assets 提取模块文件
     */
    private fun extractModuleAssets(tempZip: File): Boolean {
        return try {
            app.assets.open("mod.zip").use { input ->
                FileOutputStream(tempZip).use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "提取模块资源失败", e)
            false
        }
    }

    /*
        /**
         * 解压 zip 文件到指定目录
         */
        private fun unzip(zipFile: File, destDir: File): Boolean {
            return try {
                ZipInputStream(zipFile.inputStream()).use { zis ->
                    var entry: ZipEntry?
                    while (zis.nextEntry.also { entry = it } != null) {
                        val newFile = File(destDir, entry!!.name)
                        if (entry!!.isDirectory) {
                            newFile.mkdirs()
                        } else {
                            newFile.parentFile?.mkdirs()
                            FileOutputStream(newFile).use { fos ->
                                zis.copyTo(fos)
                            }
                        }
                    }
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    */

    /**
     * 检查模块是否已安装（基于模块版本）
     * 注意：新的自动更新策略不再使用此方法，而是基于应用版本变化
     */
    fun shouldInstallModule(): Boolean {
        return moduleVersion < LATEST_MODULE_VERSION
    }

    /**
     * 获取模块版本信息
     */
    val moduleVersion: Int
        get() {
            val modulePropsPath = "/data/adb/modules/ColorOSFeaturesEnhance/module.prop"
            if (!CSU.fileExists(modulePropsPath)) return -1

            return try {
                val output =
                    CSU.runWithSu("grep '^versionCode=' \"$modulePropsPath\" | cut -d'=' -f2").output
                output.trim().takeIf { it.isNotEmpty() }?.toInt() ?: -1
            } catch (e: Exception) {
                CLog.e(TAG, "获取模块版本失败", e)
                -1
            }
        }

    /**
     * 将merged_output的配置复制到模块挂载目录
     * 每次保存配置后调用此方法
     */
    fun copyMergedConfigToModule(): Boolean {
        try {
            CLog.i(TAG, "开始复制合并配置到模块目录")

            val configPaths = getConfigPaths()
            CLog.d(TAG, "配置路径: mergedOutputDir=${configPaths.mergedOutputDir}")

            val moduleBase = "/data/adb/cos_feat_e"
            val moduleDirs = listOf(
                "$moduleBase/my_product/etc/extension",
                "$moduleBase/anymount/my_product/etc/extension"
            )

            val shellCmd = StringBuilder()

            // 创建模块目录
            moduleDirs.forEach { dir ->
                shellCmd.append("mkdir -p \"$dir\" && ")
            }

            var hasFilesToCopy = false

            // 复制app-features.xml
            val appFeaturesSource = "${configPaths.mergedOutputDir}/${configPaths.appFeaturesFile}"
            CLog.d(TAG, "检查源文件: $appFeaturesSource")
            if (File(appFeaturesSource).exists()) {
                CLog.i(TAG, "源文件存在，准备复制 app-features.xml")
                moduleDirs.forEach { dir ->
                    shellCmd.append("cp \"$appFeaturesSource\" \"$dir/\" && ")
                }
                hasFilesToCopy = true
            } else {
                CLog.w(TAG, "源文件不存在: $appFeaturesSource")
            }

            // 复制oplus-feature.xml
            val oplusFeaturesSource =
                "${configPaths.mergedOutputDir}/${configPaths.oplusFeaturesFile}"
            CLog.d(TAG, "检查源文件: $oplusFeaturesSource")
            if (File(oplusFeaturesSource).exists()) {
                CLog.i(TAG, "源文件存在，准备复制 oplus-feature.xml")
                moduleDirs.forEach { dir ->
                    shellCmd.append("cp \"$oplusFeaturesSource\" \"$dir/\" && ")
                }
                hasFilesToCopy = true
            } else {
                CLog.w(TAG, "源文件不存在: $oplusFeaturesSource")
            }

            if (!hasFilesToCopy) {
                CLog.e(TAG, "没有找到任何需要复制的文件")
                return false
            }

            // 移除最后的 &&
            val finalCmd = shellCmd.toString().removeSuffix(" && ")
            CLog.d(TAG, "执行Shell命令: $finalCmd")

            if (finalCmd.isNotEmpty()) {
                val result = CSU.runWithSu(finalCmd).output
                CLog.d(TAG, "Shell命令执行结果: $result")
            } else {
                CLog.w(TAG, "没有Shell命令需要执行")
            }

            // 设置模块目录权限为644
            setModulePermissions(moduleBase)

            // 验证复制是否成功
            var successCount = 0
            moduleDirs.forEach { dir ->
                val appExists = CSU.fileExists("$dir/${configPaths.appFeaturesFile}")
                val oplusExists = CSU.fileExists("$dir/${configPaths.oplusFeaturesFile}")
                CLog.d(
                    TAG,
                    "目录 $dir: app-features存在=$appExists, oplus-feature存在=$oplusExists"
                )
                if (appExists || oplusExists) {
                    successCount++
                }
            }

            val success = successCount > 0
            if (success) {
                CLog.i(TAG, "配置文件复制到模块目录成功 ($successCount/${moduleDirs.size} 个目录)")
            } else {
                CLog.e(TAG, "配置文件复制到模块目录失败，所有目录都没有文件")
            }

            return success

        } catch (e: Exception) {
            CLog.e(TAG, "复制配置到模块目录时发生异常", e)
            return false
        }
    }

    /**
     * 设置模块目录权限为644
     * 对 /模块路径/anymount/ 和 /模块路径/my_product/ 目录及其子目录、文件设置权限
     */
    private fun setModulePermissions(moduleBase: String) {
        try {
            CLog.i(TAG, "开始设置模块目录权限")

            val targetDirs = listOf(
                "$moduleBase/anymount",
                "$moduleBase/my_product"
            )

            val permissionCmd = StringBuilder()

            targetDirs.forEach { dir ->
                if (CSU.dirExists(dir)) {
                    CLog.d(TAG, "设置目录权限: $dir")
                    // 设置目录及其所有子目录和文件的权限为644
                    permissionCmd.append("chmod -R 644 \"$dir\" && ")
                } else {
                    CLog.d(TAG, "目录不存在，跳过权限设置: $dir")
                }
            }

            if (permissionCmd.isNotEmpty()) {
                // 移除最后的 &&
                val finalPermissionCmd = permissionCmd.toString().removeSuffix(" && ")
                CLog.d(TAG, "执行权限设置命令: $finalPermissionCmd")

                val result = CSU.runWithSu(finalPermissionCmd).output
                CLog.d(TAG, "权限设置命令执行结果: $result")
                CLog.i(TAG, "模块目录权限设置完成")
            } else {
                CLog.w(TAG, "没有需要设置权限的目录")
            }

        } catch (e: Exception) {
            CLog.e(TAG, "设置模块目录权限时发生异常", e)
        }
    }

    fun copySystemConfig() {
        CLog.i(TAG, "开始复制系统原本配置")
        try {
            CSU.runWithSu("""
                busybox ash /data/adb/modules/ColorOSFeaturesEnhance/copy.sh ${Os.getuid()}
            """.trimIndent()).apply {
                CLog.d(TAG, "复制系统原本配置结果: $exitCode")
            }
        } catch (e: Exception) {
            CLog.e(TAG, "复制系统原本配置时发生异常", e)
        }
    }
}