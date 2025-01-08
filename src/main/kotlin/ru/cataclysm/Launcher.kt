package ru.cataclysm

import javafx.application.Application
import javafx.application.Platform
import javafx.scene.Scene
import javafx.scene.control.ButtonBar
import javafx.stage.Stage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import me.pavanvo.appfx.BorderlessApp
import me.pavanvo.appfx.BorderlessApp.Companion.draggableStage
import ru.cataclysm.helpers.Constants
import ru.cataclysm.helpers.ReportHelper
import ru.cataclysm.modals.ErrorDialog
import ru.cataclysm.modals.GameErrorDialog
import ru.cataclysm.services.Log

object Launcher {
    lateinit var stage: Stage

    @Volatile
    var mainScene: Scene? = null
    fun switchToMain() = synchronized(this) {
        mainScene ?: BorderlessApp.loadScene(stage, Constants.View.MAIN).also {
            mainScene = it
            draggableStage(mainScene!!.lookup("#tabs"), stage, null)
        }
        stage.scene = mainScene
        stage.title = Constants.App.NAME
    }

    @Volatile
    var loginScene: Scene? = null
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
                ReportHelper.createReportArchive(stage, exitCode)
        }
    }
}

class Preloader : BorderlessApp(Constants.View.PRELOADER, Constants.App.NAME){
    override fun start(stage: Stage) {
        Launcher.stage = stage
        super.start(stage)
    }
}


val scopeIO = CoroutineScope(Dispatchers.IO)
val scopeFX = CoroutineScope(Dispatchers.JavaFx)

fun main(args: Array<String>) {
    Log.configureLogging()
    Log.msg("Starting launcher...")
    Platform.setImplicitExit(false)
    Application.launch(Preloader::class.java)
}