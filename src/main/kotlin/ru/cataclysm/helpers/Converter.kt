package ru.cataclysm.helpers

import javafx.util.StringConverter
import java.text.DecimalFormat
import java.util.*
import kotlin.math.log10
import kotlin.math.pow

object Converter {
    fun readableSize(size: Long): String {
        if (size <= 0) return "0"
        val units = arrayOf("B", "kB", "MB", "GB", "TB", "PB", "EB")
        val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
        return DecimalFormat("#,##0.#").format(size /
                    1024.0.pow(digitGroups.toDouble())) + " " + units[digitGroups]
    }

    /**
     * Returns the input value rounded up to the next highest power of two.
     */
    fun roundUpToPowerOfTwo(value: Int): Int {
        var result = value - 1
        result = result or (result shr 1)
        result = result or (result shr 2)
        result = result or (result shr 4)
        result = result or (result shr 8)
        result = result or (result shr 16)
        return result + 1
    }
}

object MemoryConverter : StringConverter<Int>() {
    override fun toString(item: Int?): String {
        item?: return ""
        return if (item == 0) "<по умолчанию>"
        else String.format(Locale.ROOT, "%dMB (%.1fGB)", item, item / 1024.0)
    }
    // not needed
    override fun fromString(item: String?): Int = 0
}