package ru.cataclysm.controllers

import javafx.fxml.FXML
import javafx.scene.layout.VBox
import javafx.stage.Stage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.pavanvo.appfx.CustomController
import me.pavanvo.events.Event
import ru.cataclysm.Launcher
import ru.cataclysm.controls.SVGButton
import ru.cataclysm.controls.SVGLabel
import ru.cataclysm.scopeFX
import ru.cataclysm.services.account.AccountService
import ru.cataclysm.services.assets.AssetsService


class SidebarController : CustomController() {

    lateinit var dropdown: SVGLabel
    lateinit var playDropDown: VBox
    lateinit var accountButton: SVGButton
    lateinit var logoutButton: SVGButton

    val onSettings = Event()
    val onPlay = Event()

    override fun loaded(stage: Stage) {
        super.loaded(stage)
        accountButton.text = AccountService.session!!.user.profile.username
        accountButton.focusedProperty().addListener { _, _, newValue ->
            if (!newValue) scopeFX.launch { delay(1000); logoutButton.isVisible = false }
        }
    }


    @FXML
    private fun playBlock_Click() {
        playDropDown.isVisible = !playDropDown.isVisible
        dropdown.rotate = if (dropdown.rotate == 0.0) 180.0 else 0.0
    }

    @FXML
    private fun playServer1Button_Click() = onPlay()

    @FXML
    private fun playServer2Button_Click() = onPlay()

    @FXML
    private fun supportButton_Click() {

    }

    @FXML
    private fun websiteButton_Click() {

    }

    @FXML
    private fun discordButton_Click() {

    }

    @FXML
    private fun settingsButton_Click() = onSettings()

    @FXML
    private fun accountButton_Click() {
        logoutButton.isVisible = !logoutButton.isVisible
    }

    @FXML
    private fun logoutButton_Click() {
        AssetsService.stop()
        AccountService.logout()
        Launcher.switchToLogin()
    }
}