package ru.cataclysm.services

import me.pavanvo.events.Event1
import ru.cataclysm.helpers.Constants.DATETIME_FORMATTER
import ru.cataclysm.helpers.PlatformHelper
import java.awt.Desktop
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.time.LocalDateTime
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object Report {

    @Throws(IOException::class)
    fun createReportArchive(exitCode: Int, onProgress: Event1<Double>) {
        val launcherDir: Path = Settings.LAUNCHER_DIR_PATH
        val reportsPath = Files.createDirectories(Settings.currentGameDirectoryPath.resolve("reports"))
        val archivePath = reportsPath.resolve(
            "crash_report_" + LocalDateTime.now().format(DATETIME_FORMATTER) + ".zip"
        )

        onProgress(0.0)
        Log.msg("Creating report file " + archivePath.fileName)

        val filesToDelete: MutableSet<Path> = mutableSetOf()
        ZipOutputStream(Files.newOutputStream(archivePath)).use { out ->
            if (exitCode != 0) {
                out.putNextEntry(ZipEntry("exit_code.txt"))
                out.write(exitCode.toString().toByteArray(StandardCharsets.UTF_8))
                out.closeEntry()
            }
            onProgress(0.1)
            // папка logs из папки с лаунчером
            try {
                Files.newDirectoryStream(launcherDir.resolve("logs")).use { stream ->
                    for (path in stream) {
                        newEntry(path, "launcher/logs", out)
                    }
                }
            } catch (ignored: NoSuchFileException) {
            } catch (ignored: FileNotFoundException) {
            }

            onProgress(0.3)
            // файл настроек лаунчера
            val launcherCfgPath = launcherDir.resolve("launcher.cfg")
            if (Files.isRegularFile(launcherCfgPath)) {
                newEntry(launcherCfgPath, "launcher", out)
            }

            // папка logs из папки с игрой
            try {
                Files.newDirectoryStream(Settings.currentGameDirectoryPath.resolve("logs")).use { stream ->
                    for (path in stream) {
                        newEntry(path, "client/logs", out)
                    }
                }
            } catch (ignored: NoSuchFileException) {
            } catch (ignored: FileNotFoundException) {
            }

            onProgress(0.6)
            // папка crashes из папки с игрой
            try {
                Files.newDirectoryStream(Settings.currentGameDirectoryPath.resolve("crashes")).use { stream ->
                    for (path in stream) {
                        if (newEntry(path, "client/crashes", out)) {
                            filesToDelete.add(path)
                        }
                    }
                }
            } catch (ignored: NoSuchFileException) {
            } catch (ignored: FileNotFoundException) {
            }

            // файл настроек игры
            val settingsTxtPath = Paths.get(System.getProperty("user.home"))
                .resolve(Paths.get("ProjectCataclysm", "settings.txt"))
            if (Files.isRegularFile(settingsTxtPath)) {
                newEntry(settingsTxtPath, "client", out)
            }

            onProgress(0.8)
            Files.newDirectoryStream(Settings.currentGameDirectoryPath, "*.txt").use { stream ->
                for (path in stream) {
                    if (newEntry(path, "client/crashes", out)) {
                        filesToDelete.add(path)
                    }
                }
            }

            if (PlatformHelper.platform === PlatformHelper.Platform.WINDOWS) {
                try {
                    val dxDiagPath = launcherDir.resolve("dxdiag.txt").toAbsolutePath()
                    val commandLine: MutableList<String> = mutableListOf()
                    commandLine.add("dxdiag.exe")
                    commandLine.add("/dontskip")
                    commandLine.add("/whql:off")
                    commandLine.add("/t")
                    commandLine.add(dxDiagPath.toString())
                    val pb = ProcessBuilder(commandLine)
                    pb.start().waitFor()
                    if (Files.isRegularFile(dxDiagPath)) {
                        newEntry(dxDiagPath, null, out)
                    }

                    Files.deleteIfExists(dxDiagPath)
                } catch (e: Exception) {
                    Log.err(e, "DxDiag failed")
                }
            }
        }

//        for (path in filesToDelete) {
//            path.deleteIfExists()
//        }

        if (PlatformHelper.platform === PlatformHelper.Platform.WINDOWS) {
            Runtime.getRuntime().exec("explorer.exe /select,$archivePath")
        } else {
            Desktop.getDesktop().open(archivePath.toFile())
        }
        onProgress(1.0)
    }

    @Throws(IOException::class)
    private fun newEntry(path: Path, entryDir: String?, out: ZipOutputStream): Boolean {
        val attributes = Files.readAttributes(
            path,
            BasicFileAttributes::class.java
        )
        if (!attributes.isRegularFile) {
            return false
        }

        val fn = path.fileName.toString()
        if (!fn.endsWith(".txt") && !fn.endsWith(".log") && !fn.endsWith(".hrpof") && (fn != "launcher.cfg")) {
            return false
        }

        Log.msg("> $path")

        val e = ZipEntry(if (entryDir != null) ("$entryDir/$fn") else fn)
        e.setLastModifiedTime(attributes.lastModifiedTime())
        e.setLastAccessTime(attributes.lastAccessTime())
        e.setCreationTime(attributes.creationTime())

        out.putNextEntry(e)
        Files.copy(path, out)
        out.closeEntry()

        return true
    }
}