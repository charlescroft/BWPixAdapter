plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.bwpixadapter.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.bwpixadapter.app"
        minSdk = 21
        // targetSdk must stay < 23 so Android allows loading the legacy PPPP
        // .so files which contain text relocations (same reason the original
        // targetSdk=10 app works on this device).
        targetSdk = 22
        versionCode = 1
        versionName = "1.0.0"

        ndk {
            // armeabi is the only ABI the PPPP SDK ships; force it so the
            // device runs the app in 32-bit mode and loads the native libs.
            abiFilters += "armeabi"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // 开源发布：使用 debug 密钥签名，方便用户直接安装测试
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    packagingOptions {
        jniLibs {
            // The PPPP SDK predates extractNativeLibs=false; we must extract
            // the .so files to disk or the loader can't find them.
            useLegacyPackaging = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }

    lint {
        // 本应用刻意使用 targetSdk=22 以加载含 text relocations 的 32 位 PPPP 原生库，
        // 侧载使用（非 Google Play 分发），因此禁用该 Play 商店 targetSdk 检查。
        disable += "ExpiredTargetSdkVersion"
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.navigation.compose)
    implementation(libs.okhttp)
}
