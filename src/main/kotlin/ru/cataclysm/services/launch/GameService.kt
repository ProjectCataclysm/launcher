package ru.cataclysm.services.launch

import kotlinx.coroutines.launch
import me.pavanvo.events.Event1
import ru.cataclysm.Launcher
import ru.cataclysm.helpers.PlatformHelper
import ru.cataclysm.scopeFX
import ru.cataclysm.scopeIO
import ru.cataclysm.services.Log
import ru.cataclysm.services.Settings
import ru.cataclysm.services.account.AccountService
import ru.cataclysm.services.account.Session
import ru.cataclysm.services.assets.AssetsService
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * <br><br>ProjectCataclysm
 * <br>Created: 31.07.2022 19:06
 *
 * @author Knoblul
 */
object GameService {
    private val launched = AtomicBoolean()

    val onStartGameAvailable: Event1<Boolean> = Event1()

    private fun startGame(session: Session): Int {
        val gameDirPath: Path = Settings.currentGameDirectoryPath

        // Может быть проблема при закрытии игры через диспетчер задач, код завершения 1
        val command: MutableList<String> = mutableListOf()

        var customJava = true
        if (PlatformHelper.platform === PlatformHelper.Platform.WINDOWS) {
            val javaPath = gameDirPath.resolve(Paths.get("jre8", "bin", "java.exe"))
            if (Files.isExecutable(javaPath)) {
                command.add(javaPath.toAbsolutePath().toString())
                customJava = false
            }
        }

        // пример аргументов IntelliJ при запуске:
        //Command Line: exit -Xms128m -Xmx750m -XX:ReservedCodeCacheSize=512m -XX:+IgnoreUnrecognizedVMOptions -XX:+UseG1GC -XX:SoftRefLRUPolicyMSPerMB=50 -XX:CICompilerCount=2 -XX:+HeapDumpOnOutOfMemoryError -XX:-OmitStackTraceInFastThrow -ea -Dsun.io.useCanonCaches=false -Djdk.http.auth.tunneling.disabledSchemes="" -Djdk.attach.allowAttachSelf=true -Djdk.module.illegalAccess.silent=true -Dkotlinx.coroutines.debug=off -Xmx8192m -Djb.vmOptionsFile=C:\Users\Mihail\AppData\Roaming\JetBrains\IdeaIC2021.3\idea64.exe.vmoptions -Djava.system.class.loader=com.intellij.util.lang.PathClassLoader -Didea.vendor.NAME=JetBrains -Didea.paths.selector=IdeaIC2021.3 -Didea.platform.prefix=Idea -Didea.jre.check=true -Dsplash=true -Dide.native.launcher=true -XX:ErrorFile=C:\Users\Mihail\java_error_in_idea64_%p.log -XX:HeapDumpPath=C:\Users\Mihail\java_error_in_idea64.hprof

        // для репортов JVM
        Files.createDirectories(gameDirPath.resolve("crashes"))

        if (customJava) {
            command.add("java")
        }

        command.add("-Dfile.encoding=UTF-8")

        // логгинг вылетов
        command.add("-XX:ErrorFile=crashes/fatal_pid_%p.log")
        command.add("-XX:HeapDumpPath=crashes/heapdump_pid_%p.hprof")

        // оптимизация
        command.add("-XX:+UnlockExperimentalVMOptions")
        command.add("-XX:+UseG1GC")
        command.add("-XX:G1NewSizePercent=20")
        command.add("-XX:G1ReservePercent=20")
        command.add("-XX:MaxGCPauseMillis=50")
        command.add("-XX:G1HeapRegionSize=32M")

        // показывать стектрейсы из JVM
        // убрал из-за падения производительности
//		command.add("-XX:-OmitStackTraceInFastThrow");

        // создаем хипдамп при недостатке памяти
        command.add("-XX:+HeapDumpOnOutOfMemoryError")

        // макс. размер стека вызова
        command.add("-Xss1M")

        // антиинжект
        command.add("-XX:+DisableAttachMechanism")

        // память
        command.add("-Xms256m")
        if (Settings.limitMemoryMegabytes > 0) {
            command.add("-Xmx" + (Settings.limitMemoryMegabytes) + "m")
        } else if (!PlatformHelper.is64bit()) {
            command.add("-Xmx1024m")
        }

        if (PlatformHelper.platform === PlatformHelper.Platform.MAC) {
            command.add("-XstartOnFirstThread")
        }

        // вырубаем Ipv6 сокеты по дефолту для ускорения работы нетворкинга
        command.add("-Djava.net.preferIPv4Stack=true")

        // либрарипатх
        command.add("-Djava.library.path=")

        // класспатх
        command.add("-cp")

        val classPath: MutableList<String> = mutableListOf()
        val binPath = gameDirPath.resolve("bin")
        Files.newDirectoryStream(binPath).use { ds ->
            for (path in ds) {
                val relativePath = gameDirPath.relativize(path)
                classPath.add(relativePath.toString())
            }
        }
        val binNativesPath = binPath.resolve("natives")
        Files.newDirectoryStream(binNativesPath).use { ds ->
            for (path in ds) {
                val relativePath = gameDirPath.relativize(path)
                classPath.add(relativePath.toString())
            }
        }
        classPath.sort()
        classPath.add(0, "core.jar")

        command.add(java.lang.String.join(File.pathSeparator, classPath))
        command.add("start.StartClient")

        command.add("--session")
        command.add(buildSessionString(session))

        if (Settings.clientBranch !== Settings.ClientBranch.PRODUCTION) {
            command.add("--updateBranch")
            command.add(Settings.clientBranch.id!!)
        }

        val pb = ProcessBuilder(command)
        pb.directory(gameDirPath.toFile())

        pb.inheritIO()

        val process = pb.start()
        return process.waitFor()
    }

    private fun buildSessionString(session: Session): String {
        return Base64.getEncoder()
            .withoutPadding()
            .encodeToString(session.toJson().toByteArray(StandardCharsets.UTF_8))
    }

    fun startGame() {
        check(!launched.get()) { "Игра уже запущена!" }
        checkNotNull(AccountService.session) { "Вход в аккаунт не был выполнен!" }
        // Sanitizing
        AssetsService.deleteUselessFiles()

        Log.msg("Starting game process...")
        scopeFX.launch {
            TrayService.install()
            onStartGameAvailable(false)
        }

        launched.set(true)
        scopeIO.launch {
            var exitCode = 0
            try {
                exitCode = startGame(AccountService.session!!)
            } catch (e: InterruptedException) {
                scopeFX.launch { Launcher.showError("Ошибка: доступ к процессу игры был прерван", e) }
            } catch (e: IOException) {
                scopeFX.launch { Launcher.showError("Ошибка запуска процесса игры", e) }
            } finally {
                launched.set(false)
            }

            val finalExitCode = exitCode
            scopeFX.launch {
                onStartGameAvailable(true)
                if (finalExitCode != 0) {
                    if (finalExitCode == -1337) {
                        Launcher.showError(
                            "Файлы игры устарели."
                                    + " Необходимо перезапустить игру, чтобы скачать последнюю версию.", null, false
                        )
                    } else {
                        Launcher.showGameError(finalExitCode)
                    }
                }
            }
        }
    }
}