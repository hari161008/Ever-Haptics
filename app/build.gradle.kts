plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

fun getVersionName(): String {
fun getVersionName(): String {
    return (System.getenv("GITHUB_REF_NAME") ?: "6.0.1").removePrefix("v")
}

fun getVersionCode(): Int {
    return System.getenv("GITHUB_RUN_NUMBER")?.toInt() ?: 7
}
}

android {
    namespace = "com.hapticks.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.coolappstore.everhaptics.by.svhp"
        minSdk = 33
        targetSdk = 36
        versionCode = getVersionCode()
        versionName = getVersionName()

        vectorDrawables {
            useSupportLibrary = true
        }

        // Pure Kotlin/Compose app (no native code of its own): ship every ABI so one APK installs on
        // 32-bit (armeabi-v7a, x86) and 64-bit (arm64-v8a, x86_64) devices alike.
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
    }

    signingConfigs {
        create("release") {
            val ksFile = rootProject.file("app/keystore.jks")
            val storePass = System.getenv("KEYSTORE_PASSWORD") ?: "everhapticspass"
            val alias = System.getenv("KEY_ALIAS") ?: "everhaptics"
            val keyPass = System.getenv("KEY_PASSWORD") ?: storePass

            if (ksFile.exists()) {
                storeFile = ksFile
                storePassword = storePass
                keyAlias = alias
                keyPassword = keyPass
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            isJniDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    dependenciesInfo {
        includeInApk = false  
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/java")
            kotlin.srcDirs("src/main/kotlin")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.graphics.shapes)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.google.material)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)


    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}