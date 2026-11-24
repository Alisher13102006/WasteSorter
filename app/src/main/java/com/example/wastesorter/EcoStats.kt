package com.example.wastesorter

import android.content.Context

/**
 * 1.2.0: «Эко-статистика» — сколько предметов каждого вида пользователь отсортировал.
 * Счётчики хранятся только на устройстве (SharedPreferences).
 */
class EcoStats(private val counts: MutableMap<String, Int> = mutableMapOf()) {

    val total: Int get() = counts.values.sum()

    fun add(label: String) { counts[label] = (counts[label] ?: 0) + 1 }

    fun count(label: String): Int = counts[label] ?: 0

    /** Доля вида отхода в процентах. Hotfix 1.2.1: пустая статистика -> 0 %, а не деление на ноль. */
    fun percent(label: String): Int = if (total == 0) 0 else count(label) * 100 / total

    companion object {
        private const val PREFS = "eco_stats"

        fun load(context: Context): EcoStats {
            val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            return EcoStats(WASTE_INFO.keys.associateWith { p.getInt(it, 0) }.filterValues { it > 0 }.toMutableMap())
        }

        fun save(context: Context, stats: EcoStats) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().apply {
                WASTE_INFO.keys.forEach { putInt(it, stats.count(it)) }
            }.apply()
        }
    }
}
