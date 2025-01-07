/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2016, FrostWire(R). All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.frostwire.platform

/**
 * @author gubatron
 * @author aldenml
 */
interface AppSettings {
    fun string(key: String?): String?

    fun string(key: String?, value: String?)

    fun int32(key: String?): Int

    fun int32(key: String?, value: Int)

    fun int64(key: String?): Long

    fun int64(key: String?, value: Long)

    fun bool(key: String?): Boolean

    fun bool(key: String?, value: Boolean)
}
