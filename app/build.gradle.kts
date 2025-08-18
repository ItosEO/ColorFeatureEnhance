import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    kotlin("plugin.serialization") version "1.9.10"
}

val ksFile = rootProject.file("signing.properties")
val props = Properties()
if (ksFile.canRead()) {
    props.load(FileInputStream(ksFile))
    android.signingConfigs.create("sign").apply {
        storeFile = file(props["KEYSTORE_FILE"] as String)
        storePassword = props["KEYSTORE_PASSWORD"] as String
        keyAlias = props["KEYSTORE_ALIAS"] as String
        keyPassword = props["KEYSTORE_ALIAS_PASSWORD"] as String
    }
} else {
    android.signingConfigs.create("sign").apply {
        storeFile = android.signingConfigs.getByName("debug").storeFile
        storePassword = android.signingConfigs.getByName("debug").storePassword
        keyAlias = android.signingConfigs.getByName("debug").keyAlias
        keyPassword = android.signingConfigs.getByName("debug").keyPassword
    }
}

android {
    namespace = "com.itosfish.colorfeatureenhance"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.itosfish.colorfeatureenhance"
        minSdk = 34
        targetSdk = 36
        versionCode = 25081801
        versionName = "0.84"

        ndk {
            // 设置支持的SO库架构
            abiFilters.addAll(listOf("arm64-v8a"))
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("sign")
        }
        debug {
            signingConfig = signingConfigs.getByName("sign")
        }
    }
    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
        dex {
            useLegacyPackaging = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    applicationVariants.all {
        outputs.all {
            (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName = "ColorFeatureEnhance-v${versionName}-${name}.apk"
            assembleProvider.get().doLast {
                val outDir = File(rootDir, "out")
                val mappingDir = File(outDir, "mapping").absolutePath
                val apkDir = File(outDir, "apk").absolutePath

                if (buildType.isMinifyEnabled) {
                    copy {
                        from(mappingFileProvider.get())
                        into(mappingDir)
                        rename { _ -> "mapping-${versionName}.txt" }
                    }
                    copy {
                        from(outputFile)
                        into(apkDir)
                    }
                }
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.material)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    // 网络请求依赖，用于云端配置下载
    implementation(libs.okhttp)

//    implementation(libs.androidx.material.icons.extended)
}
