package ru.cataclysm.modals

import javafx.stage.Stage
import javafx.stage.StageStyle
import kotlinx.coroutines.launch
import me.pavanvo.appfx.BorderlessApp
import me.pavanvo.appfx.ModalDialog
import me.pavanvo.events.Event1
import ru.cataclysm.controllers.ReportController
import ru.cataclysm.helpers.Constants.View.REPORT
import ru.cataclysm.scopeFX

class ReportDialog(private val parent: Stage) : ModalDialog(parent) {
    val onProgress: Event1<Double> = Event1()

    init {
        initStyle(StageStyle.UNDECORATED)
        val controller = load<ReportController>(REPORT, "Отчёт")
        BorderlessApp.draggableStage(scene.root, this, null)
        onProgress += {
            value ->
            scopeFX.launch {  controller!!.progressBar.progress = value }
        }
    }
}