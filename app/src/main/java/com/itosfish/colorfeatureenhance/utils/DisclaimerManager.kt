package com.itosfish.colorfeatureenhance.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.itosfish.colorfeatureenhance.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 免责声明管理器
 * 负责管理首次启动状态检测、免责声明文本读取和用户同意状态存储
 */
class DisclaimerManager private constructor(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )
    
    /**
     * 检查是否需要显示免责声明
     * @return true 如果需要显示免责声明，false 如果用户已经同意过
     */
    fun shouldShowDisclaimer(): Boolean {
        return !prefs.getBoolean(KEY_DISCLAIMER_ACCEPTED, false)
    }
    
    /**
     * 标记用户已同意免责声明
     */
    fun markDisclaimerAccepted() {
        prefs.edit(commit = true) {
            putBoolean(KEY_DISCLAIMER_ACCEPTED, true)
            putLong(KEY_ACCEPTANCE_TIME, System.currentTimeMillis())
        }
        CLog.i(TAG, "用户已同意免责声明")
    }
    
    /**
     * 获取免责声明文本内容
     * 从项目根目录的 DISCLAIMER 文件读取
     */
    suspend fun getDisclaimerText(): String = withContext(Dispatchers.IO) {
        try {
            // 尝试从 assets 目录读取 DISCLAIMER 文件
            val context = MainActivity.app
            try {
                val inputStream = context.assets.open("DISCLAIMER")
                val text = inputStream.bufferedReader().use { it.readText() }
                CLog.d(TAG, "从 assets 目录读取免责声明文件")
                return@withContext text
            } catch (e: Exception) {
                CLog.d(TAG, "assets 目录中没有 DISCLAIMER 文件")
            }

            // 尝试从应用内部存储读取 DISCLAIMER 文件
            val internalFile = File(context.filesDir, "DISCLAIMER")
            if (internalFile.exists()) {
                CLog.d(TAG, "从内部存储读取免责声明文件")
                return@withContext internalFile.readText()
            }

            // 尝试从外部存储读取 DISCLAIMER 文件
            val externalFile = File(context.getExternalFilesDir(null), "DISCLAIMER")
            if (externalFile.exists()) {
                CLog.d(TAG, "从外部存储读取免责声明文件")
                return@withContext externalFile.readText()
            }

            // 如果文件不存在，返回默认文本
            CLog.w(TAG, "DISCLAIMER 文件不存在，使用默认文本")
            return@withContext getDefaultDisclaimerText()

        } catch (e: Exception) {
            CLog.e(TAG, "读取免责声明文件失败", e)
            return@withContext getDefaultDisclaimerText()
        }
    }
    
    /**
     * 获取默认免责声明文本（当文件读取失败时使用）
     */
    private fun getDefaultDisclaimerText(): String {
        return """
以下免责声明适用于「ColorFeatureEnhance」软件（以下简称"本软件"）。在安装、使用或传播本软件之前，请务必仔细阅读并充分理解本免责声明的全部内容。一旦您安装或使用本软件，即视为您已阅读、理解并同意受本免责声明的所有条款约束。

1. 风险自负
本软件需要 Root 权限并对系统配置进行深度调整，可能影响设备稳定性与安全性。您应自行评估并承担因使用本软件（包括但不限于安装、运行、配置或卸载）而导致的任何风险与损失（如系统崩溃、数据丢失、设备变砖、保修失效等）。开发者不对任何直接或间接损害承担责任。

2. 无担保条款
本软件以"按现状"提供，不附带任何明示或暗示的担保，包括但不限于适销性、特定用途适用性及非侵权性。开发者不保证本软件的功能能满足您的需求，也不保证其运行的时效性、连续性、准确性或安全性。

3. 非官方/非关联声明
本软件系个人/第三方开发项目，与 Google、Android、ColorOS 及其任何关联公司、子公司、开发团队或合作伙伴无任何关联或合作关系。所涉商标、产品或服务名称仅作为识别之用，其所有权归各自合法持有人所有。

4. 用户数据与隐私
本软件在本地处理配置文件，不会主动收集或上传任何个人隐私数据。若您通过使用本软件导入或修改第三方配置文件，请确保已获得相应授权并遵守相关法律法规。因配置文件内容或使用方式引发的任何争议或责任，由用户自行承担。

5. 开源与修改
本软件可能包含或依赖开源组件，相关许可证详见项目仓库或随附文档。您可在遵守相应开源协议和本免责声明的前提下自由下载、修改、编译及传播本软件，但所有衍生版本必须保留本免责声明，并明确注明修改来源与作者。

6. 更新与终止
开发者保留在任何时间修改、暂停、删除或终止本软件（或其任何部分）及相关服务的权利，恕不另行通知。开发者亦有权在必要时更新本免责声明，更新后的条款一经公布即自动替代原有条款。

7. 适用法律与争议解决
本免责声明的订立、执行与解释及争议解决均应适用开发者所在地的现行法律。如双方就本免责声明内容或其执行发生任何争议，应首先友好协商解决；协商不成时，可向开发者所在地有管辖权的法院提起诉讼。

若您不同意本免责声明的任何条款，请立即停止安装或使用本软件。
        """.trimIndent()
    }
    
    /**
     * 重置免责声明状态（用于测试）
     */
    fun resetDisclaimerStatus() {
        prefs.edit(commit = true) {
            remove(KEY_DISCLAIMER_ACCEPTED)
            remove(KEY_ACCEPTANCE_TIME)
        }
        CLog.i(TAG, "免责声明状态已重置")
    }
    
    /**
     * 获取用户同意时间
     */
    fun getAcceptanceTime(): Long {
        return prefs.getLong(KEY_ACCEPTANCE_TIME, 0L)
    }
    
    companion object {
        private const val TAG = "DisclaimerManager"
        private const val PREFS_NAME = "disclaimer_prefs"
        private const val KEY_DISCLAIMER_ACCEPTED = "disclaimer_accepted"
        private const val KEY_ACCEPTANCE_TIME = "acceptance_time"
        
        @Volatile
        private var INSTANCE: DisclaimerManager? = null
        
        fun getInstance(context: Context): DisclaimerManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DisclaimerManager(context.applicationContext).also { 
                    INSTANCE = it 
                }
            }
        }
    }
}
