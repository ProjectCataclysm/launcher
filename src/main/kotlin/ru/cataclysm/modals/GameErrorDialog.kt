package ru.cataclysm.modals

import javafx.scene.control.Alert
import javafx.scene.control.ButtonBar
import javafx.scene.control.ButtonType
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.StageStyle
import me.pavanvo.appfx.BorderlessApp
import ru.cataclysm.helpers.Constants
import ru.cataclysm.services.Log

class GameErrorDialog(
    parent: Stage,
    exitCode: Int
) : Alert(AlertType.ERROR) {


    init {
        width = 500.0; height = 400.0
        initModality(Modality.APPLICATION_MODAL)
        initStyle(StageStyle.UNDECORATED)
        initOwner(parent)

        BorderlessApp.draggableStage(dialogPane, null, this)

        val header = String.format("Игра была аварийно завершена с кодом %d (0x%X)", exitCode, exitCode)
        val message = " Вы желаете сформировать архив с отчетом о данной ошибке? Архив можно будет отправить нам, чтобы мы исправили вашу проблему."
        Log.err("$header\r\n$message")

        dialogPane.stylesheets.add(Constants.Styles.alert.toString())
        title = "Ошибка"
        headerText = header
        contentText = message
        buttonTypes.setAll(
            ButtonType("Сформировать", ButtonBar.ButtonData.YES),
            ButtonType("Отмена", ButtonBar.ButtonData.NO)
        )
    }
}