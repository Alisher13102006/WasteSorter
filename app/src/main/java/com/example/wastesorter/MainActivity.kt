package com.example.wastesorter

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Ниже этой уверенности приложение честно говорит «не уверен». */
private const val MIN_CONFIDENCE = 0.5f

class MainActivity : ComponentActivity() {
    private lateinit var classifier: WasteClassifier

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        classifier = WasteClassifier(this)
        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                Surface(Modifier.fillMaxSize()) { SorterScreen(classifier) }
            }
        }
    }

    override fun onDestroy() {
        classifier.close()
        super.onDestroy()
    }
}

@Composable
fun SorterScreen(classifier: WasteClassifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var photo by remember { mutableStateOf<Bitmap?>(null) }
    var result by remember { mutableStateOf<Prediction?>(null) }
    var busy by remember { mutableStateOf(false) }

    fun analyze(bmp: Bitmap) {
        photo = bmp
        result = null
        busy = true
        scope.launch {
            result = withContext(Dispatchers.Default) { classifier.classify(bmp) }
            busy = false
        }
    }

    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
        bmp?.let(::analyze)
    }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { u -> loadBitmap(context, u)?.let(::analyze) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .safeDrawingPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Сортировка мусора", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text(
            "Сфотографируйте предмет — модель определит: пластик, стекло или бумага.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            val bmp = photo
            if (bmp != null) {
                Image(bmp.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Text("📷", fontSize = 64.sp)
            }
            if (busy) CircularProgressIndicator()
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { takePhoto.launch(null) }, Modifier.weight(1f)) { Text("Камера") }
            OutlinedButton(
                onClick = {
                    pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                Modifier.weight(1f),
            ) { Text("Из галереи") }
        }

        result?.let { ResultCard(it) }
    }
}

@Composable
fun ResultCard(r: Prediction) {
    val sure = r.confidence >= MIN_CONFIDENCE
    val info = infoFor(r.label)
    val accent = if (sure) info.color else Color.Gray

    Card(shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(accent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) { Text(if (sure) info.emoji else "🤔", fontSize = 26.sp) }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        if (sure) info.title else "Не уверен",
                        fontSize = 22.sp, fontWeight = FontWeight.Bold, color = accent,
                    )
                    Text(
                        if (sure) "${info.bin} · ${(r.confidence * 100).toInt()}%"
                        else "Похоже на «${info.title}» (${(r.confidence * 100).toInt()}%). Снимите ближе, на ровном фоне.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            r.all.forEach { (label, p) ->
                val i = infoFor(label)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(i.title, Modifier.width(72.dp), style = MaterialTheme.typography.bodySmall)
                    LinearProgressIndicator(
                        progress = { p },
                        modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = i.color,
                    )
                    Text("${(p * 100).toInt()}%", Modifier.width(44.dp), style = MaterialTheme.typography.bodySmall)
                }
            }

            if (sure) {
                HorizontalDivider()
                Text("Как сдать", fontWeight = FontWeight.SemiBold)
                Text(info.howTo, style = MaterialTheme.typography.bodyMedium)
                Text("Что из этого сделают", fontWeight = FontWeight.SemiBold)
                info.becomes.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
}

/** Загружает фото из галереи уменьшенным (модели хватает 224×224) и с учётом поворота. */
private fun loadBitmap(context: android.content.Context, uri: Uri): Bitmap? = runCatching {
    if (Build.VERSION.SDK_INT >= 28) {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val scale = maxOf(1, minOf(info.size.width, info.size.height) / 512)
            decoder.setTargetSize(info.size.width / scale, info.size.height / scale)
        }
    } else {
        context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it) }
    }
}.getOrNull()
