package ru.cataclysm.modals

import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.StageStyle
import kotlinx.coroutines.launch
import me.pavanvo.appfx.BorderlessApp
import ru.cataclysm.helpers.Constants.View.LOADING
import ru.cataclysm.scopeFX

class LoadingDialog(
    parent: Stage,
    title: String,
    header: String,
) : Stage() {

    private val progressBar: ProgressBar
    private val label: Label

    init {
        initStyle(StageStyle.UNDECORATED)
        initModality(Modality.APPLICATION_MODAL)
        initOwner(parent)

        val fxmlLoader = FXMLLoader(LOADING)
        val scene = Scene(fxmlLoader.load())
        this.title = title
        this.scene = scene

        BorderlessApp.draggableStage(scene.root, this, null)

        progressBar = scene.lookup("#progressBar") as ProgressBar
        label = scene.lookup("#header") as Label
        label.text = header
    }

    fun setProgress(value: Double) = scopeFX.launch {
        progressBar.progress = value
    }

    fun setHeader(value: String) = scopeFX.launch {
        label.text = value
    }
}