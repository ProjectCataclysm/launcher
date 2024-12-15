package ru.cataclysm

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage

class HelloApplication : Application() {
    override fun start(stage: Stage) {
//        val fxmlLoader = FXMLLoader(HelloApplication::class.java.getResource("Checking.fxml"))
//        val fxmlLoader = FXMLLoader(HelloApplication::class.java.getResource("Login.fxml"))
        val fxmlLoader = FXMLLoader(HelloApplication::class.java.getResource("Main.fxml"))
//        val fxmlLoader = FXMLLoader(HelloApplication::class.java.getResource("Register.fxml"))
        val scene = Scene(fxmlLoader.load(), 600.0, 600.0)
        stage.title = "Hello!"
        stage.scene = scene
        stage.show()
    }
}

fun main() {
    Application.launch(HelloApplication::class.java)
}