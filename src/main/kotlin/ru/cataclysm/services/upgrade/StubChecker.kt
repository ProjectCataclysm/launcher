package ru.cataclysm.services.upgrade

import ru.cataclysm.services.Log
import ru.cataclysm.services.Settings
import java.io.IOException
import java.net.URISyntaxException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption


class LauncherRestartedException : java.lang.RuntimeException()

/**
 * // XXX НЕ ИСПОЛЬЗОВАТЬ СТОРОННИЕ БИБЛИОТЕКИ В ЭТОМ КЛАССЕ!
 *
 * <br></br><br></br>ProjectCataclysm
 * <br></br>Created: 05.08.2022 16:41
 *
 * @author Knoblul
 */
object StubChecker {
    fun currentJarPath(): Path? {
        try {
            val path = Paths.get(StubChecker::class.java.protectionDomain.codeSource.location.toURI())
            val fn = path.fileName.toString().lowercase()
            if (Files.isDirectory(path) || !fn.endsWith(".jar") && !fn.endsWith(".exe")) {
                return null
            }

            return path
        } catch (e: URISyntaxException) {
            throw RuntimeException(e)
        }
    }

    fun stubJarPath(): Path {
        return Settings.configFile
    }

    private fun checkLocation(): Path? {
        val jar = currentJarPath()
        val stubJar = stubJarPath()
        val updateJar: Path = UpgradeService.updateJarPath()

        Log.msg("JAR: $jar")
        Log.msg("STUB: $stubJar")
        Log.msg("UPD: $updateJar")

        // Если запуск из IDE, то игнорим всю логику
        // Если запуск из exe-файла, то смотрим есть ли файл стаба на компе, если есть запускаем через него
        //  если нет, то AutoUpdater должен его скачать
        if (jar == null || jar.fileName.toString().lowercase().endsWith(".exe")
            && !Files.isRegularFile(stubJar)
        ) {
            return null
        }

        if (jar == updateJar || !Files.isRegularFile(stubJar)) {
            try {
                Log.msg("Copying self into $stubJar")
                Files.copy(jar, stubJar, StandardCopyOption.REPLACE_EXISTING)
            } catch (e: IOException) {
                throw RuntimeException("Failed to copy $jar into $jar", e)
            }
        }

        return if (jar != stubJar) stubJar else null
    }

    fun restart(path: Path) {
        try {
            //LauncherLock.unlock()
            val command: MutableList<String> = ArrayList()
            command.add(if (Settings.IS_INSTALLATION) "java/bin/java" else "java")
            command.add("-Xmx256m") // Так как лаунчер по кд висит в фоне, ему надо как можно минимум памяти задать
            //			command.add("-cp");
//			String libPath = LauncherConfig.LAUNCHER_DIR_PATH.resolve("lib").toAbsolutePath().toString();
//			command.add(path.toAbsolutePath() + File.pathSeparator + libPath + File.separator + "*");
            command.add("-jar")
            command.add(path.toAbsolutePath().toString())
            command.add("--restarted")
            ProcessBuilder(command).start()
        } catch (e: IOException) {
            throw RuntimeException("Failed to restart launcher as $path", e)
        }

        throw LauncherRestartedException()
    }

    fun check(args: Array<String>) {
        if (args.size > 0 && args[0] != "--restarted") {
            // Если в параметр был передан файл с окночнанием .jar, то удаляем
            // этот файл
            // нужно для удаления родительского джарника, напр. при запуске
            // лаунчера через обновляющий джар
            val src = Paths.get(args[0])
            if (src.fileName.endsWith(".jar")) {
                try {
                    Files.deleteIfExists(src)
                } catch (e: IOException) {
                    Log.err(e, "Failed to delete jar")
                }
            }
        }

        val location = checkLocation()
        if (location != null) {
            Log.msg("Restarting as $location")
            // перезапускаем лаунчер в другом джаре
            restart(location)
        }
    }
}

