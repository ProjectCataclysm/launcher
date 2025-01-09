package ru.cataclysm.services.upgrade

import me.pavanvo.events.Event1
import okhttp3.ResponseBody
import okio.BufferedSink
import okio.buffer
import okio.sink
import ru.cataclysm.helpers.Constants
import ru.cataclysm.helpers.Constants.App.version
import ru.cataclysm.helpers.ParseHelper
import ru.cataclysm.helpers.RequestHelper
import ru.cataclysm.services.Log
import ru.cataclysm.services.Settings
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path


object UpgradeService {

    val updateJarPath: Path
        get() = Settings.LAUNCHER_DIR_PATH.resolve("launcher_update.jar")

    val onProgress: Event1<Double> = Event1()
    val onMessage: Event1<String> = Event1()

    private fun updateLauncher() {
        try {
            val updateJar = updateJarPath.toFile()
            Log.msg("Downloading launcher update...")
            onMessage("Скачиваем обновление...")

            val url = "https://${Constants.LAUNCHER_URL}/launcher.jar"
            RequestHelper.get(url).use { response ->
                val body: ResponseBody = response.body
                val contentLength = body.contentLength()
                val source = body.source()

                val sink: BufferedSink = updateJar.sink().buffer()
                val sinkBuffer = sink.buffer

                var totalBytesRead = 0.0
                val bufferSize = 8 * 1024
                var bytesRead: Long
                while ((source.read(sinkBuffer, bufferSize.toLong()).also { bytesRead = it }) != -1L) {
                    sink.emit()
                    totalBytesRead += bytesRead
                    var progress = (totalBytesRead / contentLength)
                    if (progress > 1) progress = 1.0
                    onProgress(progress)
                }
                sink.flush()
                sink.close()
                source.close()
            }
            StubChecker.restart(updateJarPath)
        } catch (e: IOException) {
            throw RuntimeException("Failed to update launcher", e)
        }
    }

    private fun checkForUpdates() {
        val url = "https://${Constants.LAUNCHER_URL}/version.txt"
        try {
            RequestHelper.get(url).use { response ->
                val newVersion = response.body.string().trim()
                if (ParseHelper.versionCompare(newVersion, version) > 0) {
                    Log.msg("Launcher outdated!")
                    // обновляем лаунчер
                    updateLauncher()
                }
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to check version", e)
        }
    }

    private fun relaunchExeAsStub() {
        val jar = StubChecker.currentJarPath()
        val stub = StubChecker.stubJarPath()
        if (jar != null && jar.fileName.toString().lowercase().endsWith(".exe")) {
            if (!Files.isRegularFile(stub)) {
                Log.err("Launcher not downloaded as jar!")
                updateLauncher()
            }
        }
    }

    fun updateEnabled(): Boolean {
        // проверяем, запущены ли мы в IDE?
        return StubChecker.currentJarPath() != null
    }

    fun check() {
        // если лаунчер обёрнут в exe, то перезапускаем его в jar
        relaunchExeAsStub()

        // проверяем наличие обновлений
        checkForUpdates()
        //StubChecker.restart(updateJarPath)
    }
}