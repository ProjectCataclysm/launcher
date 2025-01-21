package ru.cataclysm.services

import ru.cataclysm.helpers.Converter
import ru.cataclysm.helpers.PlatformHelper
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

/**
 * <br></br><br></br>ProjectCataclysm
 * <br></br>Created: 06.08.2022 13:32
 *
 * @author Knoblul
 */
object Settings {
    private val CLIENT_BRANCH_NAME_MAP: MutableMap<String, ClientBranch> = mutableMapOf()

    val IS_INSTALLATION: Boolean = Files.isRegularFile(Paths.get(".setup"))
    var LAUNCHER_DIR_PATH: Path = getLauncherPath()

    var gameDirectoryPath: Path = if (IS_INSTALLATION && LAUNCHER_DIR_PATH.parent != null)
        LAUNCHER_DIR_PATH.parent.resolve("client")
    else PlatformHelper.defaultGameDirectory

    var limitMemoryMegabytes: Int = 0

    val configFile: Path = LAUNCHER_DIR_PATH.resolve("launcher.cfg")

    var clientBranch: ClientBranch = ClientBranch.PRODUCTION

    init {
        Log.msg("Config file %s", configFile)
        load()

        for (v in ClientBranch.entries) {
            CLIENT_BRANCH_NAME_MAP[v.name.lowercase()] = v
        }
    }

    val currentGameDirectoryPath: Path
        get() = gameDirectoryPath.resolve(clientBranch.id!!)

    val torrentFile: File
        get() = currentGameDirectoryPath.resolve("game.torrent").toFile()

    private fun load() {
        try {
            Files.newInputStream(configFile).use { `in` ->
                Log.msg("Loading config file...")
                val props = Properties()
                props.load(`in`)

                gameDirectoryPath = Paths.get(
                    props.getProperty("gameDirectory", gameDirectoryPath.toString()))
                limitMemoryMegabytes = Converter.roundUpToPowerOfTwo(
                    props.getProperty("limitMemory", limitMemoryMegabytes.toString()).toInt()
                )
                clientBranch = CLIENT_BRANCH_NAME_MAP.getOrDefault(
                    props.getProperty("clientUpdateBranch").orEmpty().lowercase(Locale.ROOT), ClientBranch.PRODUCTION
                )
            }
        } catch (e: FileNotFoundException) {
            save()
        } catch (e: NoSuchFileException) {
            save()
        } catch (e: Exception) {
            Log.err(e, "Can't load config file")
            save()
        }

        if (limitMemoryMegabytes != 0 && limitMemoryMegabytes < 1024
            || limitMemoryMegabytes > PlatformHelper.maxMemory
        ) {
            limitMemoryMegabytes = 0
        }
    }

    fun save() {
        try {
            Files.newOutputStream(configFile).use { out ->
                Log.msg("Saving config file...")
                val props = Properties()
                props.setProperty("gameDirectory", gameDirectoryPath.toAbsolutePath().toString())
                props.setProperty("limitMemory", limitMemoryMegabytes.toString())
                props.setProperty("clientUpdateBranch", clientBranch.name)
                props.store(out, "")
            }
        } catch (e: IOException) {
            Log.err(e, "Can't save config file")
        }
    }

    private fun getLauncherPath(): Path {
        return if (IS_INSTALLATION) Paths.get("").toAbsolutePath()
        else try {
            Files.createDirectories(Paths.get(System.getProperty("user.home"), "ProjectCataclysm"))
        } catch (e: IOException) {
            throw RuntimeException("Failed to create work directory", e)
        }
    }

    enum class ClientBranch(val value: String, val title: String, val id: String?) {
        PRODUCTION("production", "Основной клиент", "branch_main"),
        TEST("textArea", "Тестовый клиент", "branch_test"),
    }
}