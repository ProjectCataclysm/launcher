/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2020, FrostWire(R). All rights reserved.
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
package com.frostwire.transfers

import java.io.File
import java.util.*

/**
 * @author gubatron
 * @author aldenml
 */
interface Transfer {
    val name: String?

    val displayName: String?

    val savePath: File

    fun previewFile(): File?

    val size: Double

    val created: Date

    val state: TransferState

    val bytesReceived: Long

    val bytesSent: Long

    val downloadSpeed: Long

    val uploadSpeed: Long

    val isDownloading: Boolean

    val eTA: Long

    /**
     * [0..100]
     *
     * @return
     */
    val progress: Int

    val isComplete: Boolean

    val items: List<TransferItem>

    fun remove(deleteData: Boolean)
}
