package ru.cataclysm.controllers

import javafx.application.Platform
import javafx.fxml.FXML
import javafx.stage.Stage
import me.pavanvo.appfx.CustomController
import ru.cataclysm.Launcher

class RegisterController : CustomController() {
    override fun loaded(stage: Stage) {
        super.loaded(stage)

    }


    @FXML
    fun buttonRegister_Click() {

    }

    @FXML
    fun buttonLogin_Click() {
        Launcher.switchToLogin()
    }

    @FXML
    fun buttonMinimize_Click() {
        stage.isIconified = true
    }

    @FXML
    fun buttonClose_Click() {
        Platform.exit()
    }
}