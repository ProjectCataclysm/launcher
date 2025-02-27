package ru.cataclysm.controllers

import javafx.application.Platform
import javafx.scene.control.Button
import javafx.scene.control.Hyperlink
import javafx.scene.control.PasswordField
import javafx.scene.control.TextField
import javafx.scene.text.Text
import javafx.stage.Stage
import kotlinx.coroutines.launch
import me.pavanvo.appfx.CustomController
import ru.cataclysm.Launcher
import ru.cataclysm.helpers.ApiException
import ru.cataclysm.helpers.Constants
import ru.cataclysm.scopeFX
import ru.cataclysm.scopeIO
import ru.cataclysm.services.Log
import ru.cataclysm.services.account.AccountService

class LoginController : CustomController() {
    lateinit var loginInput: TextField
    lateinit var passwordInput: PasswordField
    lateinit var forgetPasswordLink: Text
    lateinit var incorrectLoginDataLabel: Text
    lateinit var loginButton: Button
    lateinit var redirectToRegisterLink: Hyperlink

    override fun loaded(stage: Stage) {
        super.loaded(stage)
        stage.icons.clear()
        stage.icons.add(Constants.Image.icon)

        incorrectLoginDataLabel.isVisible = false
        forgetPasswordLink.isVisible = false
    }

    fun buttonLogin_Click() = scopeIO.launch {
        try {
            AccountService.authorize(loginInput.text, passwordInput.text)
        } catch (ex: ApiException) {
            scopeFX.launch {
                incorrectLoginDataLabel.isVisible = true
                forgetPasswordLink.isVisible = true
            }
        } catch (ex: Exception) {
            Log.err(ex, "Failed to login")
            //TODO("Show Error")
        }
        finally {
            scopeFX.launch {
                if (AccountService.session != null)
                    Launcher.switchToMain()
            }
        }
    }

    fun buttonRegister_Click() {
       Launcher.switchToRegister()
    }

    fun buttonMinimize_Click() {
        stage.isIconified = true
    }

    fun buttonClose_Click() {
        Platform.exit()
    }
}