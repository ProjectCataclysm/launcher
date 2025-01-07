package ru.cataclysm.controllers

import javafx.scene.control.TextArea
import me.pavanvo.appfx.CustomController

class ErrorController : CustomController() {
    lateinit var stackTrace: TextArea
}