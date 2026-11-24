package com.example.wastesorter

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/** Результат распознавания: метка класса (glass/paper/plastic) и вероятности всех классов. */
data class Prediction(val label: String, val confidence: Float, val all: List<Pair<String, Float>>)

/**
 * Обёртка над моделью TensorFlow Lite, обученной на Python (~/obrabotka/export_tflite.py).
 * Вход: [1, 224, 224, 3] float32, пиксели RGB 0..255 (нормализация внутри модели).
 * Выход: [1, N] вероятности классов в порядке labels.txt.
 */
class WasteClassifier(context: Context) : AutoCloseable {

    private val interpreter = Interpreter(loadModel(context, MODEL_FILE), Interpreter.Options().setNumThreads(4))
    val labels: List<String> = context.assets.open(LABELS_FILE).bufferedReader()
        .readLines().map { it.trim() }.filter { it.isNotEmpty() }

    private val inputSize: Int = interpreter.getInputTensor(0).shape()[1]

    fun classify(source: Bitmap): Prediction {
        val bitmap = Bitmap.createScaledBitmap(centerCrop(source), inputSize, inputSize, true)
        val input = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3).order(ByteOrder.nativeOrder())
        val pixels = IntArray(inputSize * inputSize)
        bitmap.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)
        for (p in pixels) {
            input.putFloat(((p shr 16) and 0xFF).toFloat())   // R
            input.putFloat(((p shr 8) and 0xFF).toFloat())    // G
            input.putFloat((p and 0xFF).toFloat())            // B
        }
        input.rewind()

        val output = Array(1) { FloatArray(labels.size) }
        interpreter.run(input, output)
        val probs = output[0]
        val all = labels.zip(probs.toList()).sortedByDescending { it.second }
        return Prediction(all.first().first, all.first().second, all)
    }

    override fun close() = interpreter.close()

    /** Квадратный центр кадра — так же, как квадратные фото в датасете. */
    private fun centerCrop(b: Bitmap): Bitmap {
        val side = minOf(b.width, b.height)
        return Bitmap.createBitmap(b, (b.width - side) / 2, (b.height - side) / 2, side, side)
    }

    companion object {
        const val MODEL_FILE = "waste_model.tflite"
        const val LABELS_FILE = "labels.txt"

        private fun loadModel(context: Context, name: String): MappedByteBuffer {
            context.assets.openFd(name).use { fd ->
                FileInputStream(fd.fileDescriptor).channel.use { ch ->
                    return ch.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
                }
            }
        }
    }
}
