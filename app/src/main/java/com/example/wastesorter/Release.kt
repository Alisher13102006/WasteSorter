package com.example.wastesorter

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings

/** Логи с тегом WasteSorter. d/i вырезаются R8 в release (rules.keep). */
object AppLog {
    private const val TAG = "WasteSorter"
    fun d(msg: String) { Log.d(TAG, msg) }
    fun i(msg: String) { Log.i(TAG, msg) }
    fun w(msg: String) { Log.w(TAG, msg) }
    fun e(msg: String, t: Throwable? = null) { Log.e(TAG, msg, t) }
}

/**
 * ЛР №8 «Публикация и сопровождение».
 * Задание 13 — мониторинг: Crashlytics + Analytics.
 * Задание 14 — remote config, feature flags, принудительное обновление (in-app updates).
 *
 * Firebase работает, только если в app/ положить google-services.json (BuildConfig.FIREBASE_ENABLED).
 * Без него все вызовы безопасно сводятся к Logcat — приложение собирается и запускается.
 */
object Monitoring {
    private var analytics: FirebaseAnalytics? = null

    private fun ready(context: Context) =
        BuildConfig.FIREBASE_ENABLED && FirebaseApp.getApps(context).isNotEmpty()

    fun init(context: Context) {
        if (!ready(context)) {
            AppLog.i("Firebase выключен: нет google-services.json, крэши только в Logcat")
            return
        }
        analytics = FirebaseAnalytics.getInstance(context)
        FirebaseCrashlytics.getInstance().apply {
            // в debug не засоряем консоль, в staging/release собираем
            isCrashlyticsCollectionEnabled = !BuildConfig.DEBUG
            setCustomKey("build_type", BuildConfig.BUILD_TYPE)
            setCustomKey("api", BuildConfig.API_BASE_URL)
        }
    }

    fun event(name: String, params: Map<String, String> = emptyMap()) {
        AppLog.d("analytics: $name $params")
        analytics?.logEvent(name, Bundle().apply { params.forEach { (k, v) -> putString(k, v) } })
    }

    /** Нефатальная ошибка: приложение живо, но в Crashlytics она видна. */
    fun nonFatal(t: Throwable) {
        AppLog.e("non-fatal", t)
        if (analytics != null) FirebaseCrashlytics.getInstance().recordException(t)
    }

    /** Тестовый крэш: необработанное исключение, его стектрейс расшифровывается по mapping.txt. */
    fun testCrash(): Nothing {
        if (analytics != null) FirebaseCrashlytics.getInstance().log("test crash button")
        throw RuntimeException("Test Crash (ЛР8, задание 13), version ${BuildConfig.VERSION_NAME}")
    }
}

/**
 * Задание 14: feature flags через Remote Config.
 * Новую функцию можно выключить у всех пользователей без нового релиза —
 * это замена «отката», которого в магазинах нет.
 */
object FeatureFlags {
    const val SHARE_RESULT = "share_result_enabled"        // функция версии 1.1.0
    const val MIN_VERSION_CODE = "min_supported_version"   // ниже — принудительное обновление

    private val defaults = mapOf<String, Any>(
        SHARE_RESULT to true,
        MIN_VERSION_CODE to 0L,
    )
    private var rc: FirebaseRemoteConfig? = null

    fun init(context: Context, onUpdated: () -> Unit = {}) {
        if (!BuildConfig.FIREBASE_ENABLED || FirebaseApp.getApps(context).isEmpty()) return
        rc = FirebaseRemoteConfig.getInstance().apply {
            setConfigSettingsAsync(remoteConfigSettings {
                minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) 0 else 3600
            })
            setDefaultsAsync(defaults)
            fetchAndActivate().addOnCompleteListener { onUpdated() }
        }
    }

    fun isEnabled(key: String): Boolean = rc?.getBoolean(key) ?: (defaults[key] as Boolean)

    fun long(key: String): Long = rc?.getLong(key) ?: (defaults[key] as Long)
}

/**
 * Задание 14: принудительное обновление.
 * Если versionCode ниже min_supported_version из Remote Config — запускается IMMEDIATE in-app update
 * (работает, когда приложение установлено из Google Play), иначе FLEXIBLE при наличии обновления.
 */
object UpdateChecker {
    const val REQUEST_CODE = 801

    fun mustUpdate(current: Int = BuildConfig.VERSION_CODE, min: Long = FeatureFlags.long(FeatureFlags.MIN_VERSION_CODE)) =
        current < min

    fun check(activity: Activity) {
        val manager = AppUpdateManagerFactory.create(activity)
        manager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE) return@addOnSuccessListener
            val type = if (mustUpdate()) AppUpdateType.IMMEDIATE else AppUpdateType.FLEXIBLE
            if (info.isUpdateTypeAllowed(type)) {
                manager.startUpdateFlowForResult(info, activity, AppUpdateOptions.defaultOptions(type), REQUEST_CODE)
            }
        }.addOnFailureListener { AppLog.w("in-app update недоступен (не из Play): ${it.message}") }
    }
}
