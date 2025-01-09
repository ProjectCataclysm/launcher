package ru.cataclysm.modals

import javafx.fxml.FXMLLoader
import javafx.scene.control.Alert
import javafx.scene.control.TextArea
import javafx.scene.layout.VBox
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.StageStyle
import me.pavanvo.appfx.BorderlessApp
import ru.cataclysm.helpers.Constants

class ErrorDialog(
    private val parent: Stage,
    message: String,
    exception: Throwable?
) : Alert(AlertType.ERROR){

    init {
        width = 500.0; height = 400.0
        initModality(Modality.APPLICATION_MODAL)
        initStyle(StageStyle.UNDECORATED)
        initOwner(parent)

        BorderlessApp.draggableStage(dialogPane, null, this)

        val fxmlLoader = FXMLLoader(Constants.View.ERROR)
        this.title = "Ошибка"
        dialogPane.content = fxmlLoader.load<VBox>()

        val stackTrace = dialogPane.lookup("#stackTrace") as TextArea
        dialogPane.stylesheets.add(Constants.Styles.alert.toString())
        if (exception != null) {
            headerText = message
            stackTrace.text = exception.stackTraceToString()
        } else {
            headerText = message
            stackTrace.text = "Неизвестная ошибка"
        }
    }
}