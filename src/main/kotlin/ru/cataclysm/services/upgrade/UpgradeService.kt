package ru.cataclysm.services.upgrade

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
    fun updateJarPath(): Path {
        return Settings.LAUNCHER_DIR_PATH.resolve("launcher_update.jar")
    }

    private fun updateLauncher() {
        try {
            val updateJar = updateJarPath()
            Log.msg("Downloading launcher update...")
            val url = "https://${Constants.LAUNCHER_URL}/launcher.jar"
            RequestHelper.get(url).use { response ->
                response.body.source().use { source ->
                    Files.newOutputStream(updateJar).use { out ->

                    }
                }
            }
            StubChecker.restart(updateJar)
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

    private fun relaunchExeAsStub(jar: Path?) {
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
        val jar = StubChecker.currentJarPath()

        // если лаунчер обёрнут в exe, то перезапускаем его в jar
        relaunchExeAsStub(jar)

        // проверяем наличие обновлений
        checkForUpdates()
    }
}