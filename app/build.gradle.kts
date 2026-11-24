import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// ЛР8 задание 13: Firebase включается, только когда в app/ лежит google-services.json
// (скачивается из Firebase Console, в git не коммитится). Без него сборка работает, крэши видны в Logcat.
val firebaseEnabled = file("google-services.json").exists()
if (firebaseEnabled) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
}

// ЛР8 задание 2: SemVer. MAJOR — несовместимые изменения (новая модель с другими классами,
// удаление функций, повышение minSdk); MINOR — новая функция; PATCH — только исправления.
// versionCode = MAJOR*10000 + MINOR*100 + PATCH: всегда растёт, 1.2.1 -> 10201.
val versionMajor = 1
val versionMinor = 2
val versionPatch = 0
val semVer = "$versionMajor.$versionMinor.$versionPatch"
val semVerCode = versionMajor * 10000 + versionMinor * 100 + versionPatch

// ЛР8 задание 3: секреты не в коде и не в git.
// Порядок поиска: переменная окружения (CI Secrets) -> local.properties -> keystore.properties.
val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
val keystoreProps = Properties().apply {
    rootProject.file("keystore.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
fun secret(name: String, default: String = ""): String =
    System.getenv(name) ?: localProps.getProperty(name) ?: keystoreProps.getProperty(name) ?: default

android {
    // namespace — пакет кода (R, BuildConfig); applicationId — ID в магазине. Они могут различаться.
    namespace = "com.example.wastesorter"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        // com.example.* Google Play не принимает — нужен уникальный ID (обратный домен)
        applicationId = "kz.wastesorter.app"
        minSdk = 24
        targetSdk = 37
        versionCode = semVerCode
        versionName = semVer

        buildConfigField("String", "API_KEY", "\"${secret("API_KEY")}\"")
        buildConfigField("boolean", "FIREBASE_ENABLED", firebaseEnabled.toString())
    }

    // ЛР8 задание 5: подпись release. Keystore лежит ВНЕ репозитория,
    // путь и пароли — в keystore.properties (в .gitignore) или в CI Secrets.
    signingConfigs {
        create("release") {
            val path = secret("RELEASE_STORE_FILE")
            if (path.isNotEmpty()) {
                storeFile = file(path)
                storePassword = secret("RELEASE_STORE_PASSWORD")
                keyAlias = secret("RELEASE_KEY_ALIAS")
                keyPassword = secret("RELEASE_KEY_PASSWORD")
            }
        }
    }

    // ЛР8 задание 3: debug / staging / release — свой applicationId, адрес API, имя и иконка
    // (иконки и имена: src/debug/res, src/staging/res; release берёт src/main/res).
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            buildConfigField("String", "API_BASE_URL", "\"https://dev.api.wastesorter.kz/\"")
        }
        create("staging") {
            initWith(getByName("release"))
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
            buildConfigField("String", "API_BASE_URL", "\"https://staging.api.wastesorter.kz/\"")
            // ставится рядом с release и подписывается debug-ключом: боевой keystore не нужен
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
        release {
            buildConfigField("String", "API_BASE_URL", "\"https://api.wastesorter.kz/\"")
            // ЛР8 задание 4: R8 (минификация, обфускация, оптимизация) + удаление неиспользуемых ресурсов.
            // Для сравнения размера: ./gradlew assembleRelease -Pr8=false
            val r8 = (project.findProperty("r8") ?: "true") == "true"
            isMinifyEnabled = r8
            isShrinkResources = r8
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "src/main/keepRules/rules.keep")
            val release = signingConfigs.getByName("release")
            signingConfig = if (release.storeFile != null) release else null
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    // .tflite нельзя сжимать в APK — иначе модель не отобразится в память
    androidResources {
        noCompress += "tflite"
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
    lint {
        abortOnError = false
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.litert)                       // TensorFlow Lite (LiteRT)

    // ЛР8 задания 13–14: Crashlytics, Analytics, Remote Config (feature flags), in-app updates
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.config)
    implementation(libs.play.app.update)

    testImplementation(libs.junit)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
