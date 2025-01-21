package ru.cataclysm.controllers

import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.stage.DirectoryChooser
import javafx.stage.Stage
import javafx.util.StringConverter
import me.pavanvo.appfx.CustomController
import me.pavanvo.events.Event
import ru.cataclysm.Launcher
import ru.cataclysm.helpers.ClientBranchConverter
import ru.cataclysm.helpers.PlatformHelper
import ru.cataclysm.helpers.MemoryConverter
import ru.cataclysm.services.Settings
import ru.cataclysm.services.assets.AssetsService
import kotlin.io.path.isDirectory

class SettingsController : CustomController() {

    lateinit var updateBranchChoiceBox: ChoiceBox<Settings.ClientBranch>
    lateinit var changeDirectoryButton: Button
    lateinit var createReportButton: Button
    lateinit var memoryLimitChoiceBox: ChoiceBox<Int>
    lateinit var directoryInput: TextField
    lateinit var done : Event

    val pathChooser = DirectoryChooser()


    override fun loaded(stage: Stage) {
        super.loaded(stage)
        initUpdateBranchChoiceBox()
        initGamePath()
        initMemoryChoiceBox()
        initReportButton()
    }

    private fun initReportButton() {
        val reportTip = Tooltip(
            ("Создает архив, в который складывает всю необходимую информацию для "
                    + "диагностики и исправления ошибок. Папка, содержащая созданный архив будет открыта. "
                    + "Данный архив вы можете отправить нам, чтобы мы могли решить возникшую у вас проблему.")
        )
        reportTip.isWrapText = true
        reportTip.maxWidth = 400.0
        Tooltip.install(createReportButton, reportTip)
    }

    private fun initUpdateBranchChoiceBox() {
        updateBranchChoiceBox.items = FXCollections.observableArrayList(Settings.ClientBranch.entries)
        updateBranchChoiceBox.converter = ClientBranchConverter
        updateBranchChoiceBox.selectionModel.select(Settings.clientBranch)
        updateBranchChoiceBox.selectionModel.selectedItemProperty()
            .addListener { _, _, newValue -> updateBranch_Selected(newValue) }
    }

    private fun updateBranch_Selected(v: Settings.ClientBranch?) {
        Settings.clientBranch = v!!
        AssetsService.checkForUpdates()
    }

    private fun initMemoryChoiceBox() {
        val values: MutableList<Int> = mutableListOf()
        values.add(0)

        val maxMemory: Int = PlatformHelper.maxMemory
        val selectedValue: Int = Settings.limitMemoryMegabytes
        var selectedIndex = 0

        // степени двойки, начиная от 1024, заканчивая 32768
        for (i in 10..15) {
            val value = 1 shl i
            if (value > maxMemory) break

            values.add(value)

            if (value == selectedValue) selectedIndex = i - 9
        }

        memoryLimitChoiceBox.items = FXCollections.observableArrayList(values)
        memoryLimitChoiceBox.converter = MemoryConverter
        memoryLimitChoiceBox.selectionModel.select(selectedIndex)
        memoryLimitChoiceBox.selectionModel.selectedItemProperty()
            .addListener { _, _, newValue -> memoryLimit_Selected(newValue) }
    }

    private fun initGamePath() {
        directoryInput.text = Settings.gameDirectoryPath.toString()

        pathChooser.title = "Выбор папки с игрой"
        if ((Settings.gameDirectoryPath.isDirectory())) {
            pathChooser.initialDirectory = Settings.gameDirectoryPath.toFile()
        }
    }

    @FXML
    private fun changeDirectoryButton_Click() {
        val file = pathChooser.showDialog(scene.window)
        if (file != null) {
            directoryInput.text = file.absolutePath
            Settings.gameDirectoryPath = file.toPath()
            pathChooser.initialDirectory = file
        }
    }

    private fun memoryLimit_Selected(value: Int) {
        Settings.limitMemoryMegabytes = value
    }

    @FXML
    private fun createReportButton_Click() {
        Launcher.createReport(0)
    }

    @FXML
    private fun doneButton_Click() {
        done()
    }
}