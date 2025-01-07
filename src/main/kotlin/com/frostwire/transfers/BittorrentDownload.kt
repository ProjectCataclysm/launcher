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
package com.frostwire.transfers

import java.io.File

/**
 * @author gubatron
 * @author aldenml
 */
interface BittorrentDownload : Transfer {
    val infoHash: String?

    /**
     * Generates a magnet URI using the current information in
     * the torrent. If the underlying torrent handle is invalid,
     * null is returned.
     *
     * @return
     */
    fun magnetUri(): String?

    val connectedPeers: Int

    val totalPeers: Int

    val connectedSeeds: Int

    val totalSeeds: Int

    /**
     * For multi files torrents, returns the folder containing the files (savePath/torrentName)
     * For single file torrents, returns the path to the single file of the torrent (savePath/singleFile)
     *
     * @return
     */
    val contentSavePath: File?

    val isPaused: Boolean

    val isSeeding: Boolean

    val isFinished: Boolean

    fun pause()

    fun resume()

    fun remove(deleteTorrent: Boolean, deleteData: Boolean)

    /**
     * Adds up the number of bytes per file extension and returns
     * the winning file extension for the torrent.
     *
     *
     * If the files are not known, then it returns "torrent"
     *
     * @return
     */
    val predominantFileExtension: String?
}
