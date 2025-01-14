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
import com.frostwire.jlibtorrent.swig.*
import com.frostwire.util.Logger

import java.io.File
import java.util.*
import java.util.concurrent.CountDownLatch

/**
 * @author gubatron
 * @author aldenml
 */
class BTEngine private constructor() : SessionManager(false) {
    private val innerListener: InnerListener
    private val restoreDownloadsQueue: Queue<RestoreDownloadTask>
    var listener: BTEngineListener? = null

    init {
        this.innerListener = InnerListener()
        this.restoreDownloadsQueue = LinkedList()
    }

    override fun start() {
        val params = loadSettings()
        val sp = params.settings().swig()
        sp.set_str(settings_pack.string_types.listen_interfaces.swigValue(), ctx!!.interfaces)
        sp.set_int(settings_pack.int_types.max_retry_port_bind.swigValue(), ctx!!.retries)
        sp.set_str(settings_pack.string_types.dht_bootstrap_nodes.swigValue(), dhtBootstrapNodes())
        sp.set_int(settings_pack.int_types.active_limit.swigValue(), 2000)
        sp.set_int(settings_pack.int_types.stop_tracker_timeout.swigValue(), 0)
        sp.set_int(settings_pack.int_types.alert_queue_size.swigValue(), 5000)
        sp.set_bool(settings_pack.bool_types.enable_dht.swigValue(), ctx!!.enableDht)
        if (ctx!!.optimizeMemory) {
            sp.set_bool(settings_pack.bool_types.enable_ip_notifier.swigValue(), false)
        }
        // NOTE: generate_fingerprint needs a tag number between 0 and 19, otherwise it returns an
        // invalid character that makes the app crash on android
        val fwFingerPrint = libtorrent.generate_fingerprint(
            "FW",
            ctx!!.version[0],
            ctx!!.version[1], ctx!!.version[2], ctx!!.version[3] % 10
        )
        sp.set_str(settings_pack.string_types.peer_fingerprint.swigValue(), fwFingerPrint)
        val userAgent = String.format(
            Locale.ENGLISH,
            "CataclysmLauncher/%d.%d.%d libtorrent/%s",
            ctx!!.version[0],
            ctx!!.version[1],
            ctx!!.version[2],
            libtorrent.version()
        )
        sp.set_str(settings_pack.string_types.user_agent.swigValue(), userAgent)
        LOG.info("Peer Fingerprint: " + sp.get_str(settings_pack.string_types.peer_fingerprint.swigValue()))
        LOG.info("User Agent: " + sp.get_str(settings_pack.string_types.user_agent.swigValue()))

        try {
            super.start(params)
        } catch (t: Throwable) {
            LOG.error(t.message, t)
        }
    }

    override fun stop() {
        super.stop()
        if (ctx == null) {
            onCtxSetupComplete()
        }
    }

    override fun onBeforeStart() {
        addListener(innerListener)
    }

    override fun onAfterStart() {
        fireStarted()
    }

    override fun onBeforeStop() {
        removeListener(innerListener)
        saveSettings()
    }

    override fun onAfterStop() {
        fireStopped()
    }

    override fun moveStorage(dataDir: File) {
        if (swig() == null) {
            return
        }
        ctx!!.dataDir = dataDir // this will be removed when we start using platform
        super.moveStorage(dataDir)
    }

    private fun loadSettings(): SessionParams {
        try {
            val f = settingsFile()
            if (f.exists()) {
                val data = f.readBytes()
                val buffer = Vectors.bytes2byte_vector(data)
                val n = bdecode_node()
                val ec = error_code()
                val ret = bdecode_node.bdecode(buffer, n, ec)
                if (ret == 0) {
                    val stateVersion = n.dict_find_string_value_s(STATE_VERSION_KEY)
                    if (STATE_VERSION_VALUE != stateVersion) {
                        return defaultParams()
                    }
                    val params = libtorrent.read_session_params(n)
                    buffer.clear() // prevents GC
                    return SessionParams(params)
                } else {
                    LOG.error("Can't decode session state data: " + ec.message())
                    return defaultParams()
                }
            } else {
                return defaultParams()
            }
        } catch (e: Throwable) {
            LOG.error("Error loading session state", e)
            return defaultParams()
        }
    }

    private fun defaultParams(): SessionParams {
        val sp = defaultSettings()
        return SessionParams(sp)
    }

    override fun onApplySettings(sp: SettingsPack) {
        saveSettings()
    }

    override fun saveState(): ByteArray {
        if (swig() == null) {
            return byteArrayOf()
        }
        val e = entry()
        swig().save_state(e)
        e[STATE_VERSION_KEY] = STATE_VERSION_VALUE
        return Vectors.byte_vector2bytes(e.bencode())
    }

    private fun saveSettings() {
        if (swig() == null) {
            return
        }
        try {
            val data = saveState()
            settingsFile().writeBytes(data)
        } catch (e: Throwable) {
            LOG.error("Error saving session state", e)
        }
    }

    fun revertToDefaultConfiguration() {
        if (swig() == null) {
            return
        }
        val sp = defaultSettings()
        applySettings(sp)
    }

    fun download(torrent: File, saveDir: File?, selection: BooleanArray?) {
        @Suppress("NAME_SHADOWING")
        var saveDir = saveDir
        @Suppress("NAME_SHADOWING")
        var selection = selection
        if (swig() == null) {
            return
        }
        saveDir = setupSaveDir(saveDir)
        if (saveDir == null) {
            return
        }
        val ti = TorrentInfo(torrent)
        if (selection == null) {
            selection = BooleanArray(ti.numFiles())
            Arrays.fill(selection, true)
        }
        val priorities: Array<Priority>
        val th = find(ti.infoHash())
        val exists = th != null
        priorities = if (th != null) {
            th.filePriorities()
        } else {
            Priority.array(Priority.IGNORE, ti.numFiles())
        }
        var changed = false
        for (i in selection.indices) {
            if (selection[i] && priorities[i] == Priority.IGNORE) {
                priorities[i] = Priority.NORMAL
                changed = true
            }
        }
        if (!changed) { // nothing to do
            return
        }
        download(ti, saveDir, priorities, null, null)
        if (!exists) {
            saveResumeTorrent(ti)
        }
    }

    fun download(ti: TorrentInfo, saveDir: File?, selection: BooleanArray?, peers: List<TcpEndpoint?>?) {
        download(ti, saveDir, selection, peers, false)
    }

    fun download(
        ti: TorrentInfo,
        saveDir: File?,
        selection: BooleanArray?,
        peers: List<TcpEndpoint?>?,
        dontSaveTorrentFile: Boolean
    ) {
        @Suppress("NAME_SHADOWING")
        var saveDir = saveDir
        @Suppress("NAME_SHADOWING")
        var selection = selection
        if (swig() == null) {
            return
        }
        saveDir = setupSaveDir(saveDir)
        if (saveDir == null) {
            return
        }
        if (selection == null) {
            selection = BooleanArray(ti.numFiles())
            Arrays.fill(selection, true)
        }
        var priorities: Array<Priority>? = null
        val th = find(ti.infoHash())
        val torrentHandleExists = th != null
        if (torrentHandleExists) {
            try {
                priorities = th!!.filePriorities()
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        } else {
            priorities = Priority.array(Priority.IGNORE, ti.numFiles())
        }
        if (priorities != null) {
            for (i in selection.indices) {
                if (selection[i] && i < priorities.size && priorities[i] == Priority.IGNORE) {
                    priorities[i] = Priority.NORMAL
                }
            }
        }
        download(ti, saveDir, priorities, null, peers)

        saveResumeTorrent(ti)
        if (!dontSaveTorrentFile) {
            saveTorrent(ti)
        }
    }

    fun resumeDataFile(torrentInfo: TorrentInfo): File {
        return resumeDataFile(torrentInfo.infoHash().toString())
    }

    fun restoreDownloads() {
        if (swig() == null) {
            return
        }
        if (ctx!!.homeDir == null || !ctx!!.homeDir!!.exists()) {
            LOG.warn("Wrong setup with BTEngine home dir")
            return
        }
        val torrents = ctx!!.homeDir!!.listFiles { _: File?, name: String? ->
            name != null && name.substringAfterLast('.', "") == "torrent"
        }
        if (torrents != null) {
            for (t in torrents) {
                try {
                    val infoHash = File(t.name).nameWithoutExtension
                    val resumeFile = resumeDataFile(infoHash)
                    val savePath = readSavePath(infoHash)
                    if (setupSaveDir(savePath) == null) {
                        LOG.warn("Can't create data dir or mount point is not accessible")
                        continue
                    }
                    restoreDownloadsQueue.add(RestoreDownloadTask(t, null, null, resumeFile))
                } catch (e: Throwable) {
                    LOG.error("Error restoring torrent download: $t", e)
                }
            }
        }
        migrateVuzeDownloads()
        runNextRestoreDownloadTask()
    }

    fun settingsFile(): File {
        return File(ctx!!.homeDir, "settings.dat")
    }

    fun resumeTorrentFile(infoHash: String?): File {
        return File(ctx!!.homeDir, "$infoHash.torrent")
    }

    fun torrentFile(name: String): File {
        return File(ctx!!.torrentsDir, "$name.torrent")
    }

    fun resumeDataFile(infoHash: String?): File {
        return File(ctx!!.homeDir, "$infoHash.resume")
    }

    fun readTorrentPath(infoHash: String?): File? {
        var torrent: File? = null
        try {
            val bytes = resumeTorrentFile(infoHash).readBytes()
            val e = entry.bdecode(Vectors.bytes2byte_vector(bytes))
            torrent = File(e.dict()[TORRENT_ORIG_PATH_KEY].string())
        } catch (e: Throwable) {
            // can't recover original torrent path
        }
        return torrent
    }

    fun readSavePath(infoHash: String?): File? {
        var savePath: File? = null
        try {
            val bytes = resumeDataFile(infoHash).readBytes()
            val e = entry.bdecode(Vectors.bytes2byte_vector(bytes))
            savePath = File(e.dict()["save_path"].string())
        } catch (e: Throwable) {
            // can't recover original torrent path
        }
        return savePath
    }

    private fun saveTorrent(ti: TorrentInfo) {
        val torrentFile: File
        try {
            val name = getEscapedFilename(ti)
            torrentFile = torrentFile(name)
            val arr = ti.toEntry().bencode()
            val fs = com.frostwire.platform.Platforms.get().fileSystem()
            fs.write(torrentFile, arr)
        } catch (e: Throwable) {
            LOG.warn("Error saving torrent info to file", e)
        }
    }

    private fun saveResumeTorrent(ti: TorrentInfo) {
        try {
            val name = getEscapedFilename(ti)
            val e = ti.toEntry().swig()
            e.dict()[TORRENT_ORIG_PATH_KEY] = entry(torrentFile(name).absolutePath)
            val bytes = Vectors.byte_vector2bytes(e.bencode())
            resumeTorrentFile(ti.infoHash().toString()).writeBytes(bytes)
        } catch (e: Throwable) {
            LOG.warn("Error saving resume torrent", e)
        }
    }

    private fun getEscapedFilename(ti: TorrentInfo): String {
        var name = ti.name()
        if (name == null || name.isEmpty()) {
            name = ti.infoHash().toString()
        }
        return escapeFilename(name)
    }

    private fun fireStarted() {
        if (listener != null) {
            listener!!.started(this)
        }
    }

    private fun fireStopped() {
        if (listener != null) {
            listener!!.stopped(this)
        }
    }

    private fun fireDownloadAdded(alert: TorrentAlert<*>) {
        try {
            val th = find(alert.handle().infoHash())
            if (th != null) {
                val dl = BTDownload(this, th)
                if (listener != null) {
                    listener!!.downloadAdded(this, dl)
                }
            } else {
                LOG.info("torrent was not successfully added")
            }
        } catch (e: Throwable) {
            LOG.error("Unable to create and/or notify the new download", e)
        }
    }

    private fun fireDownloadUpdate(th: TorrentHandle) {
        try {
            val dl = BTDownload(this, th)
            if (listener != null) {
                listener!!.downloadUpdate(this, dl)
            }
        } catch (e: Throwable) {
            LOG.error("Unable to notify update the a download", e)
        }
    }

    private fun onListenSucceeded(alert: ListenSucceededAlert) {
        try {
            val endp = alert.address().toString() + ":" + alert.port()
            val s = "endpoint: " + endp + " type:" + alert.socketType()
            LOG.info("Listen succeeded on $s")
        } catch (e: Throwable) {
            LOG.error("Error adding listen endpoint to internal list", e)
        }
    }

    private fun onListenFailed(alert: ListenFailedAlert) {
        val endp = alert.address().toString() + ":" + alert.port()
        val s = "endpoint: " + endp + " type:" + alert.socketType()
        val error = alert.error()
        val message = error.message() + "/value=" + error.value()
        LOG.info("Listen failed on $s (error: $message)")
    }

    private fun migrateVuzeDownloads() {
        try {
            val dir = File(ctx!!.homeDir!!.parent, "azureus")
            val file = File(dir, "downloads.config")
            if (file.exists()) {
                val configEntry = Entry.bdecode(file)
                val downloads = configEntry.dictionary()["downloads"]!!.list()
                for (d in downloads) {
                    try {
                        val map = d.dictionary()
                        val saveDir = File(map["save_dir"]!!.string())
                        val torrent = File(map["torrent"]!!.string())
                        val filePriorities = map["file_priorities"]!!.list()
                        val priorities = Priority.array(Priority.IGNORE, filePriorities.size)
                        for (i in filePriorities.indices) {
                            val p = filePriorities[i].integer()
                            if (p != 0L) {
                                priorities[i] = Priority.NORMAL
                            }
                        }
                        if (torrent.exists() && saveDir.exists()) {
                            LOG.info("Restored old vuze download: $torrent")
                            restoreDownloadsQueue.add(RestoreDownloadTask(torrent, saveDir, priorities, null))
                            saveResumeTorrent(TorrentInfo(torrent))
                        }
                    } catch (e: Throwable) {
                        LOG.error("Error restoring vuze torrent download", e)
                    }
                }
                file.delete()
            }
        } catch (e: Throwable) {
            LOG.error("Error migrating old vuze downloads", e)
        }
    }

    private fun setupSaveDir(saveDir: File?): File? {
        var result: File? = null
        if (saveDir == null) {
            if (ctx!!.dataDir != null) {
                result = ctx!!.dataDir
            } else {
                LOG.warn("Unable to setup save dir path, review your logic, both saveDir and ctx.dataDir are null.")
            }
        } else {
            result = saveDir
        }
        val fs = com.frostwire.platform.Platforms.get().fileSystem()
        if (result != null && !fs.isDirectory(result) && !fs.mkdirs(result)) {
            result = null
            LOG.warn("Failed to create save dir to download")
        }
        if (result != null && !fs.canWrite(result)) {
            result = null
            LOG.warn("Failed to setup save dir with write access")
        }
        return result
    }

    private fun runNextRestoreDownloadTask() {
        var task: RestoreDownloadTask? = null
        try {
            if (!restoreDownloadsQueue.isEmpty()) {
                task = restoreDownloadsQueue.poll()
            }
        } catch (t: Throwable) {
            // on Android, LinkedList's .poll() implementation throws a NoSuchElementException
        }
        task?.run()
    }

    private fun download(
        ti: TorrentInfo,
        saveDir: File,
        priorities: Array<Priority>?,
        resumeFile: File?,
        peers: List<TcpEndpoint?>?
    ) {
        var th = find(ti.infoHash())
        if (th != null) {
            // found a download with the same hash, just adjust the priorities if needed
            if (priorities != null) {
                require(ti.numFiles() == priorities.size) { "The priorities length should be equals to the number of files" }
                th.prioritizeFiles(priorities)
                fireDownloadUpdate(th)
                th.resume()
            } else {
                // did they just add the entire torrent (therefore not selecting any priorities)
                val wholeTorrentPriorities = Priority.array(Priority.NORMAL, ti.numFiles())
                th.prioritizeFiles(wholeTorrentPriorities)
                fireDownloadUpdate(th)
                th.resume()
            }
        } else { // new download
            download(ti, saveDir, resumeFile, priorities, peers)
            th = find(ti.infoHash())
            if (th != null) {
                fireDownloadUpdate(th)
            }
        }
    }

    private fun onExternalIpAlert(alert: ExternalIpAlert) {
        try {
            // libtorrent perform all kind of tests
            // to avoid non usable addresses
            val address = alert.externalAddress().toString()
            LOG.info("External IP: $address")
        } catch (e: Throwable) {
            LOG.error("Error saving reported external ip", e)
        }
    }

    private fun onFastresumeRejected(alert: FastresumeRejectedAlert) {
        try {
            LOG.warn(
                "Failed to load fastresume data, path: " + alert.filePath() +
                        ", operation: " + alert.operation() + ", error: " + alert.error().message()
            )
        } catch (e: Throwable) {
            LOG.error("Error logging fastresume rejected alert", e)
        }
    }

    private fun onDhtBootstrap() {
        //long nodes = stats().dhtNodes();
        //LOG.info("DHT bootstrap, total nodes=" + nodes);
    }

    private fun printAlert(alert: Alert<*>) {
        println("Log: $alert")
    }

    private object Loader {
        val INSTANCE: BTEngine = BTEngine()
    }

    private inner class InnerListener : AlertListener {
        override fun types(): IntArray {
            return INNER_LISTENER_TYPES
        }

        override fun alert(alert: Alert<*>) {
            val type = alert.type()
            when (type) {
                AlertType.ADD_TORRENT -> {
                    val torrentAlert = alert as TorrentAlert<*>
                    fireDownloadAdded(torrentAlert)
                    runNextRestoreDownloadTask()
                }

                AlertType.LISTEN_SUCCEEDED -> onListenSucceeded(alert as ListenSucceededAlert)
                AlertType.LISTEN_FAILED -> onListenFailed(alert as ListenFailedAlert)
                AlertType.EXTERNAL_IP -> onExternalIpAlert(alert as ExternalIpAlert)
                AlertType.FASTRESUME_REJECTED -> onFastresumeRejected(alert as FastresumeRejectedAlert)
                AlertType.DHT_BOOTSTRAP -> onDhtBootstrap()
                AlertType.TORRENT_LOG, AlertType.PEER_LOG, AlertType.LOG -> printAlert(alert)
                else -> { }
            }
        }
    }

    private inner class RestoreDownloadTask(
        private val torrent: File,
        private val saveDir: File?,
        private val priorities: Array<Priority>?,
        private val resume: File?
    ) :
        Runnable {
        override fun run() {
            try {
                download(TorrentInfo(torrent), saveDir, resume, priorities, null)
            } catch (e: Throwable) {
                LOG.error("Unable to restore download from previous session. (" + torrent.absolutePath + ")", e)
            }
        }
    }

    companion object {
        private val LOG: Logger = Logger.getLogger(BTEngine::class.java)
        private val INNER_LISTENER_TYPES = intArrayOf(
            AlertType.ADD_TORRENT.swig(),
            AlertType.LISTEN_SUCCEEDED.swig(),
            AlertType.LISTEN_FAILED.swig(),
            AlertType.EXTERNAL_IP.swig(),
            AlertType.FASTRESUME_REJECTED.swig(),
            AlertType.DHT_BOOTSTRAP.swig(),
            AlertType.TORRENT_LOG.swig(),
            AlertType.PEER_LOG.swig(),
            AlertType.LOG.swig()
        )
        private const val TORRENT_ORIG_PATH_KEY = "torrent_orig_path"
        private const val STATE_VERSION_KEY = "state_version"

        // this constant only changes when the libtorrent settings_pack ABI is
        // incompatible with the previous version, it should only happen from
        // time to time, not in every version
        private const val STATE_VERSION_VALUE = "1.2.0.6"
        private val ctxSetupLatch = CountDownLatch(1)
        var ctx: BTContext? = null
        val instance: BTEngine
            get() {
                if (ctx == null) {
                    try {
                        ctxSetupLatch.await()
                    } catch (e: InterruptedException) {
                        LOG.error(e.message, e)
                    }
                    check(!(ctx == null && Loader.INSTANCE.isRunning)) { "BTContext can't be null" }
                }
                return Loader.INSTANCE
            }

        fun onCtxSetupComplete() {
            ctxSetupLatch.countDown()
        }

        // this is here until we have a properly done OS utils.
        private fun escapeFilename(s: String): String {
            return s.replace("[\\\\/:*?\"<>|\\[\\]]+".toRegex(), "_")
        }

        private fun dhtBootstrapNodes(): String {
            return "dht.libtorrent.org:25401" + "," +
                    "router.bittorrent.com:6881" + "," +
                    "dht.transmissionbt.com:6881" + "," +  // for DHT IPv6
                    "router.silotis.us:6881"
        }

        private fun defaultSettings(): SettingsPack {
            val sp = SettingsPack()
            // sp.validateHttpsTrackers(false)
            if (ctx!!.optimizeMemory) {
                val maxQueuedDiskBytes = sp.maxQueuedDiskBytes()
                sp.maxQueuedDiskBytes(maxQueuedDiskBytes / 2)
                val sendBufferWatermark = sp.sendBufferWatermark()
                sp.sendBufferWatermark(sendBufferWatermark / 2)
                sp.cacheSize(256)
                sp.activeDownloads(4)
                sp.activeSeeds(4)
                sp.maxPeerlistSize(200)
                sp.tickInterval(1000)
                sp.inactivityTimeout(60)
                sp.seedingOutgoingConnections(false)
                sp.connectionsLimit(200)
            } else {
                sp.activeDownloads(10)
                sp.activeSeeds(10)
            }
            return sp
        }
    }
}
