package ru.cataclysm.modals

import javafx.fxml.FXMLLoader
import javafx.scene.control.Alert
import javafx.scene.layout.VBox
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.StageStyle
import me.pavanvo.appfx.BorderlessApp
import me.pavanvo.appfx.CustomController
import ru.cataclysm.controllers.ErrorController
import ru.cataclysm.helpers.Constants
import java.net.URL

class ErrorDialog(
    private val parent: Stage,
    message: String,
    exception: Throwable?
) : Alert(AlertType.ERROR){


    fun <T : CustomController> load(fxml: URL, title: String): T? {
        BorderlessApp.draggableStage(dialogPane, null, this)
        return try {
            val fxmlLoader = FXMLLoader(fxml)
            this.title = title
            dialogPane.content = fxmlLoader.load<VBox>()
            val controller = fxmlLoader.getController<T>()
            controller.loaded(dialogPane.scene.window as Stage)
            controller
        } catch (ex: Exception) {
            println("An error occurred while load \"${fxml}\"")
            null
        }
    }

    init {
        width = 500.0; height = 400.0
        initModality(Modality.APPLICATION_MODAL)
        initStyle(StageStyle.UNDECORATED)
        initOwner(parent)

        val controller = load<ErrorController>(Constants.View.ERROR, "Ошибка")

        dialogPane.stylesheets.add(Constants.Styles.alert.toString())
        if (exception != null) {
            headerText = message
            controller!!.stackTrace.text = exception.stackTraceToString()
        } else {
            headerText = message
            controller!!.stackTrace.text = "Неизвестная ошибка"
        }
    }
}