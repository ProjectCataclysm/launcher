package ru.cataclysm.controllers

import javafx.event.EventHandler
import javafx.stage.Stage
import me.pavanvo.appfx.CustomController
import ru.cataclysm.Launcher
import ru.cataclysm.services.account.AccountService

class PreloaderController : CustomController() {
    override fun loaded(stage: Stage) {
        super.loaded(stage)
        stage.onShown = EventHandler { onShown() }
    }

    private fun onShown() {
        try {
            AccountService.validateSession()
        } catch (ex: Exception) {
            Launcher.showError("Не удалось восстановить сессию", ex)
        }

        if (AccountService.session != null) {
            Launcher.switchToMain()
        } else {
            Launcher.switchToLogin()
        }
    }
}