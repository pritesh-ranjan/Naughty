import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
}

val versionPropsFile = rootProject.file("version.properties")
val versionProps = Properties().apply {
    if (versionPropsFile.exists()) {
        versionPropsFile.inputStream().use { load(it) }
    }
}
val appVersionCode = versionProps.getProperty("VERSION_CODE", "2").toInt()
val appVersionName: String = versionProps.getProperty("VERSION_NAME", "2.0")

android {
    namespace = "com.example.naughty"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.example.naughty"
        minSdk = 24
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName
    }

    signingConfigs {
        create("release") {
            val keystorePropsFile = rootProject.file("keystore.properties")
            val keystoreProps = Properties().apply {
                if (keystorePropsFile.exists()) {
                    keystorePropsFile.inputStream().use { load(it) }
                }
            }
            val storeFilePath = keystoreProps.getProperty("storeFile")
                ?: System.getenv("KEYSTORE_PATH")
                ?: System.getenv("SIGNING_KEYSTORE_PATH")
            val storePass = keystoreProps.getProperty("storePassword")
                ?: System.getenv("KEYSTORE_PASSWORD")
                ?: System.getenv("SIGNING_KEYSTORE_PASSWORD")
            val keyAliasVal = keystoreProps.getProperty("keyAlias")
                ?: System.getenv("KEY_ALIAS")
                ?: System.getenv("SIGNING_KEY_ALIAS")
            val keyPass = keystoreProps.getProperty("keyPassword")
                ?: System.getenv("KEY_PASSWORD")
                ?: System.getenv("SIGNING_KEY_PASSWORD")

            if (!storeFilePath.isNullOrBlank() && !storePass.isNullOrBlank() && !keyAliasVal.isNullOrBlank() && !keyPass.isNullOrBlank()) {
                val resolvedKeystore = file(storeFilePath)
                if (resolvedKeystore.exists()) {
                    storeFile = resolvedKeystore
                    storePassword = storePass
                    keyAlias = keyAliasVal
                    keyPassword = keyPass
                    enableV1Signing = true
                    enableV2Signing = true
                    enableV3Signing = true
                } else {
                    println("⚠️ Warning: Keystore file not found at $storeFilePath. Falling back to debug signing.")
                    initWith(getByName("debug"))
                }
            } else {
                initWith(getByName("debug"))
            }
        }
    }

    buildTypes {
        release {
            isDebuggable = project.findProperty("debuggableRelease") == "true"
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = false
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.lifecycle.service)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation("androidx.compose.material:material-icons-extended")
  implementation(libs.androidx.compose.animation)
  implementation(libs.androidx.compose.foundation)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation 3
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)

  // Room
  implementation(libs.androidx.room.runtime)
  ksp(libs.androidx.room.compiler)

  // Markwon
  implementation(libs.markwon.core)
  implementation(libs.markwon.ext.strikethrough)
  implementation(libs.markwon.ext.tables)

  // DataStore
  implementation(libs.androidx.datastore.preferences)

  // Google ML Kit Text Recognition (Local on-device)
  implementation(libs.mlkit.text.recognition)

  // Biometric & Device Credential Authentication
  implementation(libs.androidx.biometric)

  // Audx RNNoise Audio Noise Suppression
  implementation("com.github.rizukirr:audx-android:v3.0.0")
}
