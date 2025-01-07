package ru.cataclysm.controllers

import javafx.application.Platform
import javafx.scene.control.Button
import javafx.scene.text.Text
import javafx.stage.Stage
import kotlinx.coroutines.launch
import me.pavanvo.appfx.CustomController
import ru.cataclysm.controls.SVGLabel
import ru.cataclysm.scopeFX
import ru.cataclysm.services.assets.AssetsService
import ru.cataclysm.services.launch.GameService
import ru.cataclysm.services.launch.TrayService

class MainTabController : CustomController() {
    lateinit var playersLabel: SVGLabel

    lateinit var playersOnline: Text
    lateinit var playButton: Button

    lateinit var updateController: UpdateController

    override fun loaded(stage: Stage) {
        super.loaded(stage)

        updateController.loaded(stage)

        TrayService.onAction += { scopeFX.launch { stage.show() } }
        GameService.onStartGameAvailable += ::startGameAvailable
    }

    fun playButton_Click() {
        GameService.startGame()
        TrayService.install()
        stage.hide()
    }

    fun startGameAvailable(available: Boolean) {
        if (available) {
            stage.setOnCloseRequest {
                AssetsService.stop()
                TrayService.uninstall()
                Platform.exit()
            }
            stage.setOnShown { AssetsService.checkForUpdates() }
            if (stage.isShowing) AssetsService.checkForUpdates() else stage.show()
            playButton.isDisable = false
            playButton.text = "Запуск"
        } else {
            stage.setOnCloseRequest { TrayService.install() }
            stage.onShown = null
            stage.hide()
            playButton.isDisable = true
            playButton.text = "Игра запущена"
        }
    }
}