package me.pavanvo.appfx

import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.stage.Modality
import javafx.stage.Stage
import java.net.URL

abstract class ModalDialog(private val parent: Stage) : Stage() {

    init {
        initModality(Modality.APPLICATION_MODAL)
        initOwner(parent)
    }

    open fun <T : CustomController> load(fxml: URL, title: String): T? {
        toCentr(parent, this, null)
        return try {
            val fxmlLoader = FXMLLoader(fxml)
            val scene = Scene(fxmlLoader.load())
            this.title = title
            this.scene = scene
            val controller = fxmlLoader.getController<T>()
            controller.loaded(this)
            controller
        } catch (ex: Exception) {
            println("An error occurred while load \"${fxml}\"")
            null
        }
    }

    companion object {
        fun toCentr(parent: Stage, stage: Stage?, alert: Alert?) {
            // Calculate the center position of the parent Stage
            val centerXPosition: Double = parent.x + parent.width / 2.0
            val centerYPosition: Double = parent.y + parent.height / 2.0
            // Relocate the pop-up Stage
            stage?.setOnShown {
                stage.x = centerXPosition - stage.width / 2.0
                stage.y = centerYPosition - stage.height / 2.0
            }
            alert?.setOnShown {
                alert.x = centerXPosition - alert.width / 2.0
                alert.y = centerYPosition - alert.height / 2.0
            }
        }
    }
}