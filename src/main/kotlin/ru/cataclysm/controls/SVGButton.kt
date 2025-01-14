package ru.cataclysm.controls

import javafx.fxml.FXMLLoader
import javafx.geometry.Orientation
import javafx.scene.control.Button
import javafx.scene.control.Separator
import javafx.scene.layout.HBox
import javafx.scene.paint.Color
import javafx.scene.shape.SVGPath
import ru.cataclysm.helpers.Constants

class SVGButton : Button() {
    private var svgPath: SVGPath = SVGPath()
    private val separator0: Separator = Separator(Orientation.VERTICAL)
    private val separator1: Separator = Separator(Orientation.VERTICAL)

    init {
        separator0.isVisible = false; separator1.isVisible = false
        separator0.prefWidth = 0.0; separator1.prefWidth = 0.0
        textFillProperty().addListener { _, _, newValue ->
            if (svgPath.fill != Color.TRANSPARENT) svgPath.fill = newValue
            if (svgPath.stroke != Color.TRANSPARENT) svgPath.stroke = newValue
        }
        textProperty().addListener { _, _, newValue -> text(newValue) }
    }

    var svgId: String = ""
        set(value) {
            field = value
            image(field)
        }

    private fun image(id: String?) {
        if (!id.isNullOrEmpty()) {
            val fxmlLoader = FXMLLoader(Constants.Controls.getControl(id))
            svgPath = fxmlLoader.load()
//            svgPath.resize()
            text(text)
        }
    }

    private fun text(value: String?) {
        if (!value.isNullOrEmpty()) {
            val separatorWidth = (prefHeight - svgPath.getWidth()) / 2
            separator0.prefWidth = separatorWidth
            separator1.prefWidth = separatorWidth
            graphic = HBox(separator0, svgPath, separator1)
        } else {
            graphic = svgPath
        }
    }

    private fun SVGPath.getWidth(): Double {
        val originalHeight = this.prefHeight(this@SVGButton.prefHeight)
        val originalWidth = this.prefWidth(this@SVGButton.prefHeight)
        val original = if (originalHeight > originalWidth) originalHeight else originalWidth
        return original
    }

    private fun SVGPath.resize() {
        val scale = this@SVGButton.prefHeight / this.getWidth()
        this.scaleX = scale
        this.scaleY = scale
    }
}