/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2015, FrostWire(R). All rights reserved.
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
package com.frostwire.bittorrent

import com.frostwire.jlibtorrent.PiecesTracker
import com.frostwire.jlibtorrent.Priority
import com.frostwire.jlibtorrent.TorrentHandle
import com.frostwire.transfers.TransferItem
import java.io.File

/**
 * @author gubatron
 * @author aldenml
 */
class BTDownloadItem(
    private val th: TorrentHandle,
    private val index: Int,
    filePath: String,
    override val size: Long,
    private val piecesTracker: PiecesTracker?,
    override val name: String = ""
) :
    TransferItem {
    override val file: File = File(th.savePath(), filePath)
    override val displayName: String = file.name

    override val isSkipped: Boolean
        get() = th.filePriority(index) == Priority.IGNORE

    override val downloaded: Long
        get() {
            if (!th.isValid) {
                return 0
            }
            val progress = th.fileProgress(TorrentHandle.FileProgressFlags.PIECE_GRANULARITY)
            return progress[index]
        }

    override val progress: Int
        get() {
            if (!th.isValid || size == 0L) { // edge cases
                return 0
            }
            val progress: Int
            val downloaded = downloaded
            progress = if (downloaded == size) {
                100
            } else {
                ((this.downloaded * 100).toFloat() / size.toFloat()).toInt()
            }
            return progress
        }

    override val isComplete: Boolean
        get() = downloaded == size

    val sequentialDownloaded: Long
        get() = piecesTracker?.getSequentialDownloadedBytes(index) ?: 0
}
