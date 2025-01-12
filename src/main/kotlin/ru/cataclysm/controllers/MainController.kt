package ru.cataclysm.controllers

import javafx.application.Platform
import javafx.scene.control.TabPane
import javafx.scene.text.Text
import javafx.stage.Stage
import me.pavanvo.appfx.CustomController
import ru.cataclysm.helpers.Constants
import ru.cataclysm.helpers.Constants.App.version
import ru.cataclysm.services.Settings
import ru.cataclysm.services.assets.AssetsService

class MainController : CustomController() {

    lateinit var tabs: TabPane
    lateinit var versionLabel: Text

    lateinit var mainTabController: MainTabController
    lateinit var settingsController: SettingsController
    lateinit var sidebarController: SidebarController

    override fun loaded(stage: Stage) {
        super.loaded(stage)
        stage.icons.clear()
        stage.icons.add(Constants.Image.icon)

        versionLabel.text = version
        sidebarController.loaded(stage)
        mainTabController.loaded(stage)
        settingsController.loaded(stage)

        sidebarController.onPlay += {
            if (tabs.selectionModel.selectedIndex == 1) Settings.save()
            tabs.selectionModel.select(tabs.tabs[0])
        }
        sidebarController.onSettings += { tabs.selectionModel.select(tabs.tabs[1]) }
    }

    fun buttonMinimize_Click() {
        stage.isIconified = true
    }

    fun buttonClose_Click() {
        try {
            AssetsService.stop()
        } finally {
            Platform.exit()
        }
    }
}