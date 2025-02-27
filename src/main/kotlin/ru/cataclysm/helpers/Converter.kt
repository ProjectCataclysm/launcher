package ru.cataclysm.helpers

import javafx.util.StringConverter
import ru.cataclysm.services.Settings
import java.text.DecimalFormat
import java.util.*
import kotlin.math.log10
import kotlin.math.pow

object Converter {
    private val group = log10(1024.0)

    fun readableSize(size: Long): String {
        if (size <= 0) return "0"
        val units = arrayOf("B", "kB", "MB", "GB", "TB")
        val digitGroups = (log10(size.toDouble()) / group).toInt()
        val sizeName = units[digitGroups]
        val pattern: String = when(sizeName) {
            "MB" -> "#,##0.##"
            "GB" -> "#,##0.###"
            "TB" -> "#,##0.###"
            else -> "#,##0.#"
        }
        return DecimalFormat(pattern).format(size /
                    1024.0.pow(digitGroups.toDouble())) + " " + sizeName
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

object ClientBranchConverter : StringConverter<Settings.ClientBranch>() {
    override fun toString(item: Settings.ClientBranch?): String {
        return item?.title ?: ""
    }

    // not needed
    override fun fromString(item: String?): Settings.ClientBranch = Settings.ClientBranch.PRODUCTION
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