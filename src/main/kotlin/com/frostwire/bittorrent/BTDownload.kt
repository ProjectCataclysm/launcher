/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2022, FrostWire(R). All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.frostwire.bittorrent

import com.frostwire.jlibtorrent.*
import com.frostwire.jlibtorrent.alerts.*
import com.frostwire.jlibtorrent.swig.add_torrent_params
import com.frostwire.jlibtorrent.swig.entry
import com.frostwire.jlibtorrent.swig.string_entry_map
import com.frostwire.util.Logger
import java.io.File
import java.util.*
import kotlin.math.min

/**
 * @author gubatron
 * @author aldenml
 */
class BTDownload(private val engine: BTEngine, val torrentHandle: TorrentHandle) :
    com.frostwire.transfers.BittorrentDownload {

    /**
     * If we have a torrent which is downloaded as a folder, this will return the parent of that folder.
     * (e.g. default save location for torrents)
     *
     *
     * If you want to have the folder where the torrent's data files are located you
     * want to use getContentSavePath().
     */
    override val savePath: File = File(torrentHandle.savePath())
    override val created: Date = Date(torrentHandle.status().addedTime())
    private val piecesTracker: PiecesTracker?
    private val parts: File?
    private val extra: MutableMap<String, String?>
    private val innerListener: BTDownload.InnerListener
    var listener: BTDownloadListener? = null
    private var incompleteFilesToRemove: Set<File>? = null
    private var lastSaveResumeTime: Long = 0
    override var predominantFileExtension: String? = null
        get() {
            if (field == null) {
                val torrentInfo = torrentHandle.torrentFile()
                if (torrentInfo != null) {
                    val files = torrentInfo.files()
                    val extensionByteSums: MutableMap<String, Long> =
                        HashMap()
                    val numFiles = files.numFiles()
                    files.paths()
                    for (i in 0..<numFiles) {
                        val path = files.filePath(i)
                        val extension = path.substringAfterLast('.', "")
                        if ("" == extension) {
                            // skip folders
                            continue
                        }
                        if (extensionByteSums.containsKey(extension)) {
                            val bytes = extensionByteSums[extension]
                            extensionByteSums[extension] = bytes!! + files.fileSize(i)
                        } else {
                            extensionByteSums[extension] = files.fileSize(i)
                        }
                    }
                    var extensionCandidate: String? = null
                    val exts: Set<String> = extensionByteSums.keys
                    for (ext in exts) {
                        if (extensionCandidate == null) {
                            extensionCandidate = ext
                        } else {
                            val extSum = extensionByteSums[ext]
                            val extSumCandidate = extensionByteSums[extensionCandidate]
                            if (extSum != null && extSumCandidate != null && extSum > extSumCandidate) {
                                extensionCandidate = ext
                            }
                        }
                    }
                    field = extensionCandidate
                }
            }
            return field
        }
        private set

    init {
        val ti = torrentHandle.torrentFile()
        this.piecesTracker = if (ti != null) PiecesTracker(ti) else null
        this.parts = if (ti != null) File(savePath, "." + ti.infoHash() + ".parts") else null
        this.extra = createExtra()
        this.innerListener = InnerListener()
        engine.addListener(innerListener)
    }

    fun getExtra(): Map<String, String?> {
        return extra
    }

    override val name: String?
        get() {
            return torrentHandle.name()
        }

    override val displayName: String?
        get() {
            val priorities = torrentHandle.filePriorities()
            var count = 0
            var index = 0
            for (i in priorities.indices) {
                if (Priority.IGNORE != priorities[i]) {
                    count++
                    index = i
                }
            }
            return if (count != 1) torrentHandle.name() else
                File(torrentHandle.torrentFile().files().filePath(index)).nameWithoutExtension
        }

    override val size: Double
        get() {
            // TODO: jlibtorrent's TorrentInfo is returning a long, should be a double (int_64)
            val ti = torrentHandle.torrentFile()
            return (ti?.totalSize() ?: 0).toDouble()
        }

    override val isPaused: Boolean
        get() = torrentHandle.isValid && (isPaused(
            torrentHandle.status()
        ) || engine.isPaused || !engine.isRunning)

    override val isSeeding: Boolean
        get() = torrentHandle.isValid && torrentHandle.status().isSeeding

    override val isFinished: Boolean
        get() = isFinished(false)

    fun isFinished(force: Boolean): Boolean {
        return torrentHandle.isValid && torrentHandle.status(force).isFinished
    }

    override val state: com.frostwire.transfers.TransferState
        get() {
            if (!engine.isRunning) {
                return com.frostwire.transfers.TransferState.STOPPED
            }
            if (engine.isPaused) {
                return com.frostwire.transfers.TransferState.PAUSED
            }
            if (!torrentHandle.isValid) {
                return com.frostwire.transfers.TransferState.ERROR
            }
            val status = torrentHandle.status()
            val isPaused = isPaused(status)
            if (isPaused && status.isFinished) {
                return com.frostwire.transfers.TransferState.FINISHED
            }
            if (isPaused && !status.isFinished) {
                return com.frostwire.transfers.TransferState.PAUSED
            }
            if (!isPaused && status.isFinished) { // see the docs of isFinished
                return com.frostwire.transfers.TransferState.SEEDING
            }
            val state = status.state()
            return when (state) {
                TorrentStatus.State.CHECKING_FILES, TorrentStatus.State.CHECKING_RESUME_DATA -> com.frostwire.transfers.TransferState.CHECKING
                TorrentStatus.State.DOWNLOADING_METADATA -> com.frostwire.transfers.TransferState.DOWNLOADING_METADATA
                TorrentStatus.State.DOWNLOADING -> com.frostwire.transfers.TransferState.DOWNLOADING
                TorrentStatus.State.FINISHED -> com.frostwire.transfers.TransferState.FINISHED
                TorrentStatus.State.SEEDING -> com.frostwire.transfers.TransferState.SEEDING
                else -> com.frostwire.transfers.TransferState.UNKNOWN
            }
        }

    override fun previewFile(): File? {
        return null
    }

    override val progress: Int
        get() {
            if (!torrentHandle.isValid) {
                return 0
            }
            val ts = torrentHandle.status()
                ?: // this can't never happens
                return 0
            val fp = ts.progress()
            val state = ts.state()
            if (java.lang.Float.compare(fp, 1f) == 0 && state != TorrentStatus.State.CHECKING_FILES) {
                return 100
            }
            val p = (fp * 100).toInt()
            if (p > 0 && state != TorrentStatus.State.CHECKING_FILES) {
                return min(p.toDouble(), 100.0).toInt()
            }
            return 0
        }

    override val isComplete: Boolean
        get() = progress == 100

    override val bytesReceived: Long
        get() = if (torrentHandle.isValid) torrentHandle.status().totalDone() else 0

    val totalBytesReceived: Long
        get() = if (torrentHandle.isValid) torrentHandle.status().allTimeDownload() else 0

    override val bytesSent: Long
        get() = if (torrentHandle.isValid) torrentHandle.status().totalUpload() else 0

    val totalBytesSent: Long
        get() = if (torrentHandle.isValid) torrentHandle.status().allTimeUpload() else 0

    override val downloadSpeed: Long
        get() = (if (!torrentHandle.isValid || isFinished || isPaused || isSeeding) 0 else torrentHandle.status()
            .downloadPayloadRate()).toLong()

    override val uploadSpeed: Long
        get() = (if (!torrentHandle.isValid || (isFinished && !isSeeding) || isPaused) 0 else torrentHandle.status()
            .uploadPayloadRate()).toLong()

    override val isDownloading: Boolean
        get() = downloadSpeed > 0

    override val connectedPeers: Int
        get() = if (torrentHandle.isValid) torrentHandle.status().numPeers() else 0

    override val totalPeers: Int
        get() = if (torrentHandle.isValid) torrentHandle.status().listPeers() else 0

    override val connectedSeeds: Int
        get() = if (torrentHandle.isValid) torrentHandle.status().numSeeds() else 0

    override val totalSeeds: Int
        get() = if (torrentHandle.isValid) torrentHandle.status().listSeeds() else 0

    override val contentSavePath: File?
        get() {
            try {
                if (!torrentHandle.isValid) {
                    return null
                }
                val ti = torrentHandle.torrentFile()
                if (ti?.swig() != null) {
                    return File(
                        savePath.absolutePath,
                        if (ti.numFiles() > 1) torrentHandle.name() else ti.files().filePath(0)
                    )
                }
            } catch (e: Throwable) {
                LOG.warn("Could not retrieve download content save path", e)
            }
            return null
        }

    override val infoHash: String?
        get() {
            try {
                return torrentHandle.infoHash().toString().lowercase(Locale.getDefault())
            } catch (t: Throwable) {
                LOG.error(t.message, t)
                return null
            }
        }

    override val eTA: Long
        get() {
            if (!torrentHandle.isValid) {
                return 0
            }
            val ti = torrentHandle.torrentFile() ?: return 0
            val status = torrentHandle.status()
            val left = ti.totalSize() - status.totalDone()
            val rate = status.downloadPayloadRate().toLong()
            if (left <= 0) {
                return 0
            }
            if (rate <= 0) {
                return -1
            }
            return left / rate
        }

    override fun pause() {
        if (!torrentHandle.isValid) {
            return
        }
        extra[WAS_PAUSED_EXTRA_KEY] = java.lang.Boolean.TRUE.toString()
        torrentHandle.unsetFlags(TorrentFlags.AUTO_MANAGED)
        torrentHandle.pause()
        doResumeData(true)
    }

    override fun resume() {
        if (!torrentHandle.isValid) {
            return
        }
        extra[WAS_PAUSED_EXTRA_KEY] = java.lang.Boolean.FALSE.toString()
        torrentHandle.setFlags(TorrentFlags.AUTO_MANAGED)
        torrentHandle.resume()
        doResumeData(true)
    }

    fun remove() {
        remove(false, false)
    }

    override fun remove(deleteData: Boolean) {
        remove(false, deleteData)
    }

    override fun remove(deleteTorrent: Boolean, deleteData: Boolean) {
        val infoHash = this.infoHash
        incompleteFilesToRemove = incompleteFiles
        if (torrentHandle.isValid) {
            if (deleteData) {
                engine.remove(torrentHandle, SessionHandle.DELETE_FILES)
            } else {
                engine.remove(torrentHandle)
            }
        }
        if (deleteTorrent) {
            val torrent = engine.readTorrentPath(infoHash)
            if (torrent != null) {
                com.frostwire.platform.Platforms.get().fileSystem().delete(torrent)
            }
        }
        engine.resumeDataFile(infoHash).delete()
        engine.resumeTorrentFile(infoHash).delete()
    }

    private fun torrentFinished() {
        if (listener != null) {
            try {
                LOG.info("BTDownload::torrentFinished", true)
                listener!!.finished(this)
            } catch (e: Throwable) {
                LOG.error("Error calling listener (finished)", e)
            }
        }
        doResumeData(true)
    }

    private fun torrentRemoved() {
        engine.removeListener(innerListener)
        parts?.delete()
        if (listener != null) {
            try {
                listener!!.removed(this, incompleteFilesToRemove)
            } catch (e: Throwable) {
                LOG.error("Error calling listener (removed)", e)
            }
        }
    }

    private fun torrentChecked() {
        try {
            if (torrentHandle.isValid) {
                // trigger items calculation
                items
            }
        } catch (e: Throwable) {
            LOG.warn("Error handling torrent checked logic", e)
        }
    }

    private fun pieceFinished(alert: PieceFinishedAlert) {
        try {
            piecesTracker?.setComplete(alert.pieceIndex(), true)
        } catch (e: Throwable) {
            LOG.warn("Error handling piece finished logic", e)
        }
    }

    val isPartial: Boolean
        get() {
            if (torrentHandle.isValid) {
                val priorities = torrentHandle.filePriorities()
                for (p in priorities) {
                    if (Priority.IGNORE == p) {
                        return true
                    }
                }
            }
            return false
        }

    override fun magnetUri(): String? {
        return torrentHandle.makeMagnetUri()
    }

    var downloadRateLimit: Int
        get() = torrentHandle.downloadLimit
        set(limit) {
            torrentHandle.downloadLimit = limit
        }

    var uploadRateLimit: Int
        get() = torrentHandle.uploadLimit
        set(limit) {
            torrentHandle.uploadLimit = limit
        }

    fun requestTrackerAnnounce() {
        torrentHandle.forceReannounce()
    }

    fun requestTrackerScrape() {
        torrentHandle.scrapeTracker()
    }

    fun trackers(): Set<String> {
        if (!torrentHandle.isValid) {
            return HashSet()
        }
        val trackers = torrentHandle.trackers()
        val urls: MutableSet<String> = HashSet(trackers.size)
        for (e in trackers) {
            urls.add(e.url())
        }
        return urls
    }

    fun trackers(trackers: Set<String>) {
        val list: MutableList<AnnounceEntry> = ArrayList(trackers.size)
        for (url in trackers) {
            list.add(AnnounceEntry(url))
        }
        torrentHandle.replaceTrackers(list)
        doResumeData(true)
    }

    override val items: List<com.frostwire.transfers.TransferItem>
        get() {
            val items = ArrayList<com.frostwire.transfers.TransferItem>()
            if (torrentHandle.isValid) {
                val ti = torrentHandle.torrentFile()
                if (ti != null && ti.isValid) {
                    val fs = ti.files()
                    val numFiles = ti.numFiles()
                    for (i in 0..<numFiles) {
                        val item = BTDownloadItem(torrentHandle, i, fs.filePath(i),
                                fs.fileSize(i), piecesTracker)
                        items.add(item)
                    }
                    if (piecesTracker != null) {
                        val numPieces = ti.numPieces()
                        // perform piece complete check
                        for (i in 0..<numPieces) {
                            if (torrentHandle.havePiece(i)) {
                                piecesTracker.setComplete(i, true)
                            }
                        }
                    }
                }
            }
            return items
        }

    val torrentFile: File?
        get() = engine.readTorrentPath(this.infoHash)

    val incompleteFiles: Set<File>
        get() {
            val s: MutableSet<File> = HashSet()
            try {
                if (!torrentHandle.isValid) {
                    return s
                }
                val progress =
                    torrentHandle.fileProgress(TorrentHandle.FileProgressFlags.PIECE_GRANULARITY)
                val ti = torrentHandle.torrentFile()
                    ?: // still downloading the info (from magnet)
                    return s
                val fs = ti.files()
                val prefix = savePath.absolutePath
                val createdTime = created.time
                for (i in progress.indices) {
                    val fePath = fs.filePath(i)
                    val feSize = fs.fileSize(i)
                    if (progress[i] < feSize) {
                        // lets see if indeed the file is incomplete
                        val f = File(prefix, fePath)
                        if (!f.exists()) {
                            continue  // nothing to do here
                        }
                        if (f.lastModified() >= createdTime) {
                            // we have a file modified (supposedly) by this transfer
                            s.add(f)
                        }
                    }
                }
            } catch (e: Throwable) {
                LOG.error("Error calculating the incomplete files set", e)
            }
            return s
        }

    var isSequentialDownload: Boolean
        get() = torrentHandle.isValid && torrentHandle.status().flags().and_(TorrentFlags.SEQUENTIAL_DOWNLOAD)
            .eq(TorrentFlags.SEQUENTIAL_DOWNLOAD)
        set(sequential) {
            if (!torrentHandle.isValid) {
                println("BTDownload::setSequentialDownload( $sequential) aborted. Torrent Handle Invalid.")
                return
            }
            if (sequential) {
                torrentHandle.setFlags(TorrentFlags.SEQUENTIAL_DOWNLOAD)
            } else {
                torrentHandle.unsetFlags(TorrentFlags.SEQUENTIAL_DOWNLOAD)
            }
        }

    fun partsFile(): File? {
        return parts
    }

    private fun serializeResumeData(alert: SaveResumeDataAlert) {
        try {
            if (torrentHandle.isValid) {
                val infoHash = torrentHandle.infoHash().toString()
                val file = engine.resumeDataFile(infoHash)
                val e = add_torrent_params.write_resume_data(alert.swig().params)
                e.dict()[EXTRA_DATA_KEY] = Entry.fromMap(extra).swig()
                file.writeBytes(Vectors.byte_vector2bytes(e.bencode()))
            }
        } catch (e: Throwable) {
            LOG.warn("Error saving resume data", e)
        }
    }

    private fun doResumeData(force: Boolean) {
        val now = System.currentTimeMillis()
        if (force || (now - lastSaveResumeTime) >= SAVE_RESUME_RESOLUTION_MILLIS) {
            lastSaveResumeTime = now
        } else {
            // skip, too fast, see SAVE_RESUME_RESOLUTION_MILLIS
            return
        }
        try {
            if (torrentHandle.isValid) {
                torrentHandle.saveResumeData(TorrentHandle.SAVE_INFO_DICT)
            }
        } catch (e: Throwable) {
            LOG.warn("Error triggering resume data", e)
        }
    }

    private fun createExtra(): MutableMap<String, String?> {
        val map: MutableMap<String, String?> = HashMap()
        try {
            val infoHash = infoHash
            val file = engine.resumeDataFile(infoHash)
            if (file.exists()) {
                val e = entry.bdecode(Vectors.bytes2byte_vector(file.readBytes()))
                val d = e.dict()
                if (d.has_key(EXTRA_DATA_KEY)) {
                    readExtra(d[EXTRA_DATA_KEY].dict(), map)
                }
            }
        } catch (e: Throwable) {
            LOG.error("Error reading extra data from resume file", e)
        }
        return map
    }

    private fun readExtra(dict: string_entry_map, map: MutableMap<String, String?>) {
        val keys = dict.keys()
        val size = keys.size().toInt()
        for (i in 0..<size) {
            val k = keys[i]
            val e = dict[k]
            if (e.type() == entry.data_type.string_t) {
                map[k] = e.string()
            }
        }
    }

    fun wasPaused(): Boolean {
        var flag = false
        if (extra.containsKey(WAS_PAUSED_EXTRA_KEY)) {
            try {
                flag = extra[WAS_PAUSED_EXTRA_KEY].toBoolean()
            } catch (e: Throwable) {
                // ignore
            }
        }
        return flag
    }

    private inner class InnerListener : AlertListener {
        override fun types(): IntArray {
            return ALERT_TYPES
        }

        override fun alert(alert: Alert<*>) {
            if (alert !is TorrentAlert<*>) {
                return
            }
            if (!alert.handle().swig().op_eq(torrentHandle.swig())) {
                return
            }
            val type = alert.type()
            when (type) {
                AlertType.TORRENT_FINISHED -> torrentFinished()
                AlertType.TORRENT_REMOVED -> torrentRemoved()
                AlertType.TORRENT_CHECKED -> torrentChecked()
                AlertType.SAVE_RESUME_DATA -> serializeResumeData(alert as SaveResumeDataAlert)
                AlertType.PIECE_FINISHED -> {
                    pieceFinished(alert as PieceFinishedAlert)
                    doResumeData(false)
                }

                AlertType.STORAGE_MOVED -> doResumeData(true)
                else -> {}
            }
        }
    }

    companion object {
        private val LOG: Logger = Logger.getLogger(BTDownload::class.java)
        private const val SAVE_RESUME_RESOLUTION_MILLIS: Long = 10000
        private val ALERT_TYPES = intArrayOf(
            AlertType.TORRENT_FINISHED.swig(),
            AlertType.TORRENT_REMOVED.swig(),
            AlertType.TORRENT_CHECKED.swig(),
            AlertType.SAVE_RESUME_DATA.swig(),
            AlertType.PIECE_FINISHED.swig(),
            AlertType.STORAGE_MOVED.swig()
        )
        private const val EXTRA_DATA_KEY = "extra_data"
        private const val WAS_PAUSED_EXTRA_KEY = "was_paused"
        private fun isPaused(s: TorrentStatus): Boolean {
            return s.flags().and_(TorrentFlags.PAUSED).nonZero()
        }
    }
}
