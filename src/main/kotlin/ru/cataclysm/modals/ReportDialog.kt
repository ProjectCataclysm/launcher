package ru.cataclysm.modals

import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.ProgressBar
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.StageStyle
import kotlinx.coroutines.launch
import me.pavanvo.appfx.BorderlessApp
import me.pavanvo.events.Event1
import ru.cataclysm.helpers.Constants.View.REPORT
import ru.cataclysm.scopeFX

class ReportDialog(private val parent: Stage) : Stage() {
    val onProgress: Event1<Double> = Event1()

    init {
        initStyle(StageStyle.UNDECORATED)
        initModality(Modality.APPLICATION_MODAL)
        initOwner(parent)

        val fxmlLoader = FXMLLoader(REPORT)
        val scene = Scene(fxmlLoader.load())
        this.title = "Отчёт"
        this.scene = scene

        BorderlessApp.draggableStage(scene.root, this, null)

        val progressBar = scene.lookup("#progressBar") as ProgressBar
        onProgress += {
            value ->
            scopeFX.launch {  progressBar.progress = value }
        }
    }
}