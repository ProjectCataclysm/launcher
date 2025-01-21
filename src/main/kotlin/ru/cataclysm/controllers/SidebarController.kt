package ru.cataclysm.controllers

import javafx.fxml.FXML
import javafx.geometry.Pos
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.Stage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.pavanvo.appfx.CustomController
import me.pavanvo.events.Event
import ru.cataclysm.Launcher
import ru.cataclysm.controls.SVGButton
import ru.cataclysm.controls.SVGLabel
import ru.cataclysm.helpers.Constants
import ru.cataclysm.scopeFX
import ru.cataclysm.services.Settings
import ru.cataclysm.services.account.AccountService
import ru.cataclysm.services.assets.AssetsService


class SidebarController : CustomController() {

    lateinit var playBlock: HBox
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

        for (v in Settings.ClientBranch.entries) {
            val button = SVGButton()
            button.id = "play-${v.name}"
//            button.svgId = "play-${v.name}"
            button.text = v.title
            button.setPrefSize(170.0, 35.0)
            button.alignment = Pos.CENTER_LEFT
            button.stylesheets.add(Constants.Styles.button.toString())
            playDropDown.children.add(button)
        }

        playDropDown.isVisible = false
    }


    @FXML
    private fun playButton_Click() {
        playDropDown.isVisible = !playDropDown.isVisible
        dropdown.rotate = if (dropdown.rotate == 0.0) 180.0 else 0.0
        if (playDropDown.isVisible) playBlock.style = "-fx-background-color: #232323;"
        else playBlock.style = ""
    }

    @FXML
    private fun playServer1Button_Click() = onPlay()

    @FXML
    private fun playServer2Button_Click() = onPlay()

    @FXML
    private fun playLastServer_Click() = onPlay()

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