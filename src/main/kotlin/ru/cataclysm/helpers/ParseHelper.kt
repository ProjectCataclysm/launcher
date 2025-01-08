package ru.cataclysm.helpers

import ru.cataclysm.helpers.Constants.App.version

object ParseHelper {
    fun version2IntArray(): IntArray {
        val result = mutableListOf<Int>()
        val array = version.split('.', '-')
        try {
            for (v in array) result.add(v.toInt())
        } catch (ex: NumberFormatException) {
            return intArrayOf(0, 0, 0, 0)
        }
        return result.toIntArray()
    }

    fun versionCompare(v1: String, v2: String): Int {
        val separators = arrayOf('.', '-')
        // vnum stores each numeric part of version
        var vnum1 = 0
        var vnum2 = 0

        // loop until both String are processed
        var i = 0
        var j = 0
        while ((i < v1.length || j < v2.length)) {
            // Storing numeric part of
            // version 1 in vnum1
            while (i < v1.length && !separators.contains(v1[i])) {
                vnum1 = (vnum1 * 10 + (v1[i].code - '0'.code))
                i++
            }

            // storing numeric part
            // of version 2 in vnum2
            while (j < v2.length && !separators.contains(v2[j])) {
                vnum2 = (vnum2 * 10 + (v2[j].code - '0'.code))
                j++
            }

            if (vnum1 > vnum2) return 1
            if (vnum2 > vnum1) return -1

            // if equal, reset variables and
            // go for next numeric part
            vnum2 = 0
            vnum1 = vnum2
            i++
            j++
        }
        return 0
    }
}