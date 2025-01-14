package ru.cataclysm.helpers

import okio.IOException
import ru.cataclysm.services.Log
import ru.cataclysm.services.upgrade.StubChecker
import java.lang.management.ManagementFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import javax.management.ObjectName
import kotlin.math.min


/**
 * Created 16 ���. 2018 �. / 22:09:12
 *
 * @author Knoblul
 */
object PlatformHelper {
    enum class Platform {
        WINDOWS,
        MAC,
        LINUX
    }

    private val JLIBTORRENT_VERSION: String = "1.2.13.0"

    val platform: Platform = readPlatform()
    val platformIdentifier: String = readPlatformID()
    private val IS_64BIT: Boolean = platformIdentifier.contains("64")
    private val MAX_MEMORY: Int = readMaxMemory()

    val defaultGameDirectory: Path
        get() {
            val path = ".project-cataclysm/"
            return when (platform) {
                Platform.WINDOWS -> Paths.get(
                    System.getenv("APPDATA"), path
                )

                Platform.MAC -> Paths.get(
                    System.getProperty("user.home", ""),
                    "Library/Application Support/$path"
                )

                else -> Paths.get(System.getProperty("user.home", ""), path)
            }
        }

    fun is64bit(): Boolean {
        return IS_64BIT
    }

    val maxMemory: Int
        get() =// Ограничиваем в 32 гб
            min(
                (32 * 1024).toDouble(),
                (if (MAX_MEMORY != 0) MAX_MEMORY else (if (IS_64BIT) 16 * 1024 else 2048)).toDouble()
            ).toInt()

    private fun readPlatform(): Platform {
        val osName = System.getProperty("os.name")
        return if (osName.startsWith("Windows")) {
            Platform.WINDOWS
        } else if (osName.startsWith("Linux") || osName.startsWith("FreeBSD") || osName.startsWith("SunOS")
            || osName.startsWith("Unix")
        ) {
            Platform.LINUX
        } else if (osName.startsWith("Mac OS X") || osName.startsWith("Darwin")) {
            Platform.MAC
        } else {
            throw IllegalArgumentException("Unknown platform: $osName")
        }
    }

    private fun readPlatformID(): String {
        val arch = System.getProperty("os.arch")
        return when (platform) {
            Platform.LINUX -> if (arch.startsWith("arm") || arch.startsWith("aarch64"))
                "linux-" + (if (arch.contains("64") || arch.contains("armv8")) "arm64" else "arm32")
            else "linux"

            Platform.MAC -> "macos"
            Platform.WINDOWS -> if (System.getenv("ProgramFiles(x86)") != null) "win64" else "win32"
        }
    }

    private fun readMaxMemory(): Int {
        try {
            val mBeanServer = ManagementFactory.getPlatformMBeanServer()
            val attribute = mBeanServer.getAttribute(
                ObjectName("java.lang", "type", "OperatingSystem"),
                "TotalPhysicalMemorySize"
            )
            require(attribute is Long) { "No such attribute: TotalPhysicalMemorySize" }

            return Converter.roundUpToPowerOfTwo((attribute / 1024 / 1024).toInt())
        } catch (t: Throwable) {
            Log.err(t, "Failed to determine max physical memory size")
        }
        return 0
    }

    private fun getTorrentNativeLibName(): String {
        return when (platform) {
            Platform.WINDOWS -> if (IS_64BIT) "x86_64/libjlibtorrent-$JLIBTORRENT_VERSION.dll" else "x86/libjlibtorrent-$JLIBTORRENT_VERSION.dll"
            Platform.LINUX -> if (IS_64BIT) "x86/libjlibtorrent-$JLIBTORRENT_VERSION.so" else "x86_64/libjlibtorrent-$JLIBTORRENT_VERSION.so"
            Platform.MAC -> "x86_64/libjlibtorrent-$JLIBTORRENT_VERSION.dylib";
        }
    }

    fun injectLibtorrent() {
        val libName = getTorrentNativeLibName()
        val libFilePath = Paths.get("libs", "natives", libName).toAbsolutePath()

        // copy only if started from jar/exe
        if (StubChecker.currentJarPath() != null) {
            try {
                Files.createDirectories(libFilePath.parent)

                Files.copy(
                    PlatformHelper.javaClass.getResourceAsStream("/natives/$libName")!!,
                    libFilePath,
                    StandardCopyOption.REPLACE_EXISTING
                )
            } catch (e: IOException) {
                throw RuntimeException("Failed to extract jlibtorrent native library", e)
            }

            if (!Files.isExecutable(libFilePath)) {
                throw RuntimeException("jlibtorrent native $libFilePath is not executable")
            }
        }

        System.setProperty("jlibtorrent.jni.path", libFilePath.toString())
    }
}