package ru.cataclysm.services.assets

import com.frostwire.bittorrent.BTContext
import com.frostwire.bittorrent.BTEngine
import com.frostwire.jlibtorrent.SessionStats
import com.frostwire.jlibtorrent.TorrentInfo
import com.frostwire.jlibtorrent.TorrentStatus
import kotlinx.coroutines.launch
import me.pavanvo.events.Event
import me.pavanvo.events.Event1
import ru.cataclysm.helpers.Constants.TORRENT_URL
import ru.cataclysm.helpers.NetworkHelper
import ru.cataclysm.helpers.ParseHelper
import ru.cataclysm.helpers.RequestHelper
import ru.cataclysm.scopeIO
import ru.cataclysm.services.Log
import ru.cataclysm.services.Settings
import java.io.File
import java.util.*

enum class CheckedState {
    IN_PROGRESS,
    DOWNLOAD_AVAILABLE,
    UPDATE_AVAILABLE,
    READY,
    FAIL
}

object AssetsService {
    private var timer: Timer = Timer()
    private var timerTask: TimerTask = object : TimerTask() {
        override fun run() {
            if (BTEngine.instance.isRunning) onTraffic(BTEngine.instance.stats())
        }
    }

    var onProgressUpdated: Event1<Float> = Event1()
    var onStateChanged: Event1<TorrentStatus.State> = Event1()
    var onChecked: Event1<CheckedState> = Event1()
    var onDownloadStarted: Event1<TorrentInfo> = Event1()
    var onTraffic: Event1<SessionStats> = Event1()
    var onCompleted: Event = Event()

    fun startBittorrentCore() = scopeIO.launch {
        val homeDir = File(Settings.currentGameDirectoryPath.toString())
        if (!homeDir.exists()) homeDir.mkdirs()

        val ctx = BTContext()
        ctx.version = ParseHelper.version2IntArray()
        ctx.homeDir = homeDir
        ctx.torrentsDir = File(homeDir.absolutePath + File.separator + "torrents")
        ctx.dataDir = File(homeDir.absolutePath + File.separator + "data")
        ctx.interfaces = NetworkHelper.endpoint
        ctx.retries = 15
        ctx.enableDht = true
        BTEngine.ctx = ctx
        BTEngine.onCtxSetupComplete()
        onCompleted += ::deleteUselessFiles

        val listener = TorrentListener(this@AssetsService)
        BTEngine.instance.addListener(listener)
        BTEngine.instance.start()
    }

    fun checkForUpdates() = scopeIO.launch {
        onChecked(CheckedState.IN_PROGRESS)
        try {
            // fetch torrent
            val bytes = RequestHelper.get(TORRENT_URL).use { it.response.body?.bytes() }
            if (bytes != null) {
                val newTorrent = TorrentInfo.bdecode(bytes)

                if (Settings.torrentFile.exists()) {
                    // compare with existing
                    val torrentInfo = TorrentInfo(Settings.torrentFile)
                    if (newTorrent.creationDate() > torrentInfo.creationDate()) {
                        onChecked(CheckedState.UPDATE_AVAILABLE)
                        Settings.torrentFile.writeBytes(bytes)
                    }
                    // torrent files are same
                    if (newTorrent.infoHash() == torrentInfo.infoHash()) {
                        // all files already downloaded
                        if (quickVerify()) onChecked(CheckedState.READY)
                        // need to download something
                        else onChecked(CheckedState.DOWNLOAD_AVAILABLE)
                    }
                } else {
                    onChecked(CheckedState.DOWNLOAD_AVAILABLE)
                    Settings.torrentFile.writeBytes(bytes)
                }

            } else
                onChecked(CheckedState.FAIL)
        } catch (ex: Exception) {
            Log.err(ex, "Unable to fetch torrent file")
            onChecked(CheckedState.FAIL)
        }
    }

    fun pause() {
        if (BTEngine.instance.isRunning)
            BTEngine.instance.pause()
    }

    fun resume() {
        if (BTEngine.instance.isPaused)
            BTEngine.instance.resume()
        else download()
    }

    fun stop() {
        timer.cancel()
        BTEngine.instance.stop()
    }

    private fun download() = scopeIO.launch {
        val torrentInfo = TorrentInfo(Settings.torrentFile)
        onDownloadStarted(torrentInfo)

        val resumeFile = BTEngine.instance.resumeDataFile(torrentInfo)
        if (resumeFile.exists()) {
            onStateChanged(TorrentStatus.State.CHECKING_RESUME_DATA)
            BTEngine.instance.download(
                torrentInfo, Settings.currentGameDirectoryPath.toFile(),
                resumeFile, null, null
            )
        } else {
            BTEngine.instance.download(torrentInfo, Settings.currentGameDirectoryPath.toFile())
        }
        timer.schedule(timerTask, 0, 1000)
    }

    private fun deleteUselessFiles() = scopeIO.launch {
        val ti = TorrentInfo(Settings.torrentFile)
        val torrentFiles = getTorrentFiles()

        val files = Settings.currentGameDirectoryPath.toFile().walkTopDown()
        for (file in files) {
            if (file.isDirectory) continue
            if (file == Settings.torrentFile) continue
            if (file == BTEngine.instance.settingsFile()) continue
            if (file == BTEngine.instance.resumeDataFile(ti.infoHash().toString())) continue
            if (torrentFiles.contains(file.absolutePath)) continue

            file.delete()
        }
    }

    private fun quickVerify(): Boolean {
        val torrentFiles = getTorrentFiles()

        for (filename in torrentFiles) {
            val file = File(filename)
            if (!file.exists()) return false
        }

        return true
    }

    private fun getTorrentFiles(): MutableList<String> {
        val ti = TorrentInfo(Settings.torrentFile)
        val torrentStorage = ti.files()
        val count = torrentStorage.numFiles()

        val torrentFiles = mutableListOf<String>()

        for (i in 0..<count) {
            torrentFiles.add(torrentStorage.filePath(i, Settings.currentGameDirectoryPath.toString()))
        }

        return torrentFiles
    }
}