package ru.cataclysm

import javafx.application.Application
import javafx.application.Platform
import javafx.event.EventHandler
import javafx.scene.Scene
import javafx.scene.control.ButtonBar
import javafx.stage.Stage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import me.pavanvo.appfx.BorderlessApp
import me.pavanvo.appfx.BorderlessApp.Companion.draggableStage
import ru.cataclysm.helpers.Constants
import ru.cataclysm.modals.ErrorDialog
import ru.cataclysm.modals.GameErrorDialog
import ru.cataclysm.modals.LoadingDialog
import ru.cataclysm.services.Log
import ru.cataclysm.services.Report
import ru.cataclysm.services.account.AccountService
import ru.cataclysm.services.upgrade.LauncherRestartedException
import ru.cataclysm.services.upgrade.StubChecker
import ru.cataclysm.services.upgrade.UpgradeService
import java.io.IOException
import kotlin.system.exitProcess

object Launcher {
    lateinit var stage: Stage

    @Volatile
    private var mainScene: Scene? = null
    fun switchToMain() = synchronized(this) {
        mainScene ?: BorderlessApp.loadScene(stage, Constants.View.MAIN).also {
            mainScene = it
            draggableStage(mainScene!!.lookup("#tabs"), stage, null)
        }
        stage.scene = mainScene
        stage.title = Constants.App.NAME
    }

    @Volatile
    private var loginScene: Scene? = null
    fun switchToLogin() = synchronized(this) {
        loginScene ?: BorderlessApp.loadScene(stage, Constants.View.LOGIN).also { loginScene = it }
        stage.scene = loginScene
        stage.title = Constants.App.LOGIN
    }

    @Volatile
    private var registerScene: Scene? = null
    fun switchToRegister() = synchronized(this) {
        registerScene ?: BorderlessApp.loadScene(stage, Constants.View.REGISTER).also { registerScene = it }
        stage.scene = registerScene
        stage.title = Constants.App.REGISTER
    }

    fun showError(message: String, exception: Throwable?, log: Boolean = true) {
        if (log) Log.err(exception, message)
        val errorMessage = ErrorDialog(stage, message, exception)
        errorMessage.showAndWait()
    }

    fun showGameError(exitCode: Int) {
        val errorMessage = GameErrorDialog(stage, exitCode)
        errorMessage.showAndWait().ifPresent { selection ->
            if (selection.buttonData == ButtonBar.ButtonData.YES)
                createReport(exitCode)
        }
    }

    fun createReport(exitCode: Int) {
        val loading = LoadingDialog(stage, "Отчёт", "Создание архива...")
        loading.show()
        Report.onProgress += loading::setProgress

        scopeIO.launch {
            try {
                Report.createReportArchive(exitCode)
            } catch (e: IOException) {
                scopeFX.launch {
                    showError("Не удалось сформировать архив", e)
                }
            } finally {
                Report.onProgress -= loading::setProgress
                scopeFX.launch { loading.hide() }
            }
        }
    }

    fun checkForUpgrade() {
        val loading = LoadingDialog(stage, "Обновление", "Проверяем наличие обновлений")
        loading.show()
        UpgradeService.onProgress += loading::setProgress
        UpgradeService.onMessage += loading::setHeader

        scopeIO.launch {
            try {
                UpgradeService.check()
            } catch (e: LauncherRestartedException) {
                exitProcess(0)
            } catch (e: IOException) {
                scopeFX.launch {
                    showError("Не удалось обновить лаунчер", e)
                }
            } finally {
                UpgradeService.onProgress -= loading::setProgress
                UpgradeService.onMessage -= loading::setHeader
                scopeFX.launch { loading.hide() }
            }
        }
    }
}

class Preloader : BorderlessApp(Constants.View.PRELOADER, Constants.App.NAME){
    override fun start(stage: Stage) {
        Launcher.stage = stage
        stage.onShown = EventHandler { onShown() }

        super.start(stage)
    }

    private fun onShown() {
        try {
            AccountService.validateSession()
        } catch (ex: Exception) {
            Launcher.showError("Не удалось восстановить сессию", ex)
        }

        if (AccountService.session != null) {
            Launcher.checkForUpgrade()
            Launcher.switchToMain()
        } else {
            Launcher.switchToLogin()
        }
        Launcher.stage.onShown = null
    }
}


val scopeIO = CoroutineScope(Dispatchers.IO)
val scopeFX = CoroutineScope(Dispatchers.JavaFx)

fun main(args: Array<String>) {
    Log.configureLogging()
    Log.msg("Starting launcher...")

    try {
        StubChecker.check(args)
    } catch (e: LauncherRestartedException) {
        exitProcess(0)
    } catch (t: Throwable) {
//        AwtErrorDialog.showError(t.toString(), t)
//        return
    }

    Platform.setImplicitExit(false)

    Application.launch(Preloader::class.java)
}