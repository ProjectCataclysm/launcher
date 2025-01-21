package me.pavanvo.appfx

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.geometry.Rectangle2D
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.scene.paint.Color
import javafx.stage.Screen
import javafx.stage.Stage
import javafx.stage.StageStyle
import java.net.URL

open class BorderlessApp(private val view: URL,
                         private val title: String
) : Application() {

    override fun start(stage: Stage) {
        stage.title = title
        loadScene(stage, view)
        stage.initStyle(StageStyle.TRANSPARENT)
        stage.opacity = 0.0
        stage.show()
        stage.centerOnScreen()
        stage.opacity = 1.0
    }

    companion object {

        fun loadScene(stage: Stage, view: URL): Scene {
            val fxmlLoader = FXMLLoader(view)
            val scene = Scene(fxmlLoader.load())
            val controller = fxmlLoader.getController<CustomController>()

            scene.fill = Color.TRANSPARENT

            stage.scene = scene
            draggableStage(scene.root, stage, null)

            controller?.loaded(stage)

            return scene
        }

        fun draggableStage(node: Node, stage: Stage?, alert: Alert?) {
            val xOffset = doubleArrayOf(0.0)
            val yOffset = doubleArrayOf(0.0)

            node.onMousePressed = null
            node.setOnMousePressed { event ->
                stage?.run {
                    xOffset[0] = stage.x - event.screenX
                    yOffset[0] = stage.y - event.screenY
                }
                alert?.run {
                    xOffset[0] = alert.x - event.screenX
                    yOffset[0] = alert.y - event.screenY
                }
            }

            node.onMouseDragged = null
            node.setOnMouseDragged { event ->
                stage?.run {
                    stage.x = event.screenX + xOffset[0]
                    stage.y = event.screenY + yOffset[0]
                }
                alert?.run {
                    alert.x = event.screenX + xOffset[0]
                    alert.y = event.screenY + yOffset[0]
                }
            }
        }
    }
}