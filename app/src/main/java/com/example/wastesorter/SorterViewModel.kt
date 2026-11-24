package com.example.wastesorter

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Ниже этой уверенности приложение честно говорит «не уверен». */
const val MIN_CONFIDENCE = 0.5f

/**
 * 1.1.0, исправление: раньше фото и результат хранились в remember {} внутри Compose
 * и пропадали при повороте экрана и смене темы (Activity пересоздаётся).
 * ViewModel переживает пересоздание, а модель TFLite больше не грузится заново при каждом повороте.
 */
class SorterViewModel(app: Application) : AndroidViewModel(app) {
    private val classifier by lazy { WasteClassifier(app) }

    var photo by mutableStateOf<Bitmap?>(null); private set
    var result by mutableStateOf<Prediction?>(null); private set
    var busy by mutableStateOf(false); private set

    // 1.2.0: эко-статистика
    var stats by mutableStateOf(EcoStats.load(app)); private set

    fun analyze(bmp: Bitmap) {
        photo = bmp
        result = null
        busy = true
        viewModelScope.launch {
            result = withContext(Dispatchers.Default) {
                runCatching { classifier.classify(bmp) }.onFailure(Monitoring::nonFatal).getOrNull()
            }
            result?.let {
                if (it.confidence >= MIN_CONFIDENCE) {
                    stats = EcoStats(WASTE_INFO.keys.associateWith(stats::count).filterValues { c -> c > 0 }.toMutableMap()).apply { add(it.label) }
                    EcoStats.save(getApplication(), stats)
                }
                Monitoring.event("classify", mapOf("label" to it.label, "sure" to (it.confidence >= MIN_CONFIDENCE).toString()))
            }
            busy = false
        }
    }

    override fun onCleared() = classifier.close()
}

/** 1.1.0: текст подсказки, которым можно поделиться в мессенджере. */
fun shareText(r: Prediction): String {
    val info = infoFor(r.label)
    return buildString {
        append("${info.emoji} ${info.title} → ${info.bin}\n")
        append(info.howTo)
        append("\n\nОпределено приложением «Сортировка мусора»")
    }
}

fun shareResult(context: Context, r: Prediction) {
    Monitoring.event("share_result", mapOf("label" to r.label))
    context.startActivity(Intent.createChooser(
        Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, shareText(r)), "Поделиться"))
}
