package com.example.wastesorter

import androidx.compose.ui.graphics.Color

/** Описание каждого вида мусора (тексты из Python-проекта, RECYCLING в 7.py). */
data class WasteInfo(
    val title: String,
    val emoji: String,
    val color: Color,
    val bin: String,
    val howTo: String,
    val becomes: List<String>,
)

val WASTE_INFO: Map<String, WasteInfo> = mapOf(
    "plastic" to WasteInfo(
        title = "Пластик",
        emoji = "🧴",
        color = Color(0xFFF5B800),
        bin = "Жёлтый контейнер",
        howTo = "Промойте, снимите крышку и этикетку, сомните бутылку и сдайте в пункт приёма " +
            "вторсырья. Смотрите маркировку на дне: PET (1), HDPE (2), PP (5).",
        becomes = listOf(
            "новые пластиковые бутылки и контейнеры",
            "флис и синтетическая ткань для одежды",
            "тротуарная плитка и садовая мебель",
        ),
    ),
    "glass" to WasteInfo(
        title = "Стекло",
        emoji = "🍾",
        color = Color(0xFF2E9D57),
        bin = "Зелёный контейнер",
        howTo = "Промойте, разделите по цвету (прозрачное, зелёное, коричневое) и сдайте в " +
            "контейнер для стекла. Стекло можно переплавлять бесконечно.",
        becomes = listOf(
            "новые стеклянные бутылки и банки",
            "стекловата (утеплитель для домов)",
            "добавка в асфальт и строительные материалы",
        ),
    ),
    "paper" to WasteInfo(
        title = "Бумага",
        emoji = "📦",
        color = Color(0xFF2F6FD6),
        bin = "Синий контейнер",
        howTo = "Сдавайте сухой и чистой, без жирных пятен, скотча и скрепок. " +
            "Картон сложите и свяжите в пачки.",
        becomes = listOf(
            "новая офисная и туалетная бумага",
            "картонные коробки и упаковка",
            "эковата (утеплитель из целлюлозы)",
        ),
    ),
)

fun infoFor(label: String): WasteInfo =
    WASTE_INFO[label] ?: WasteInfo(label, "❓", Color.Gray, "—", "", emptyList())
