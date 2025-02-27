package me.pavanvo.appfx


import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.input.MouseEvent
import javafx.stage.Stage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.actor

abstract class CustomController {

    lateinit var scene: Scene
    lateinit var stage: Stage

    open fun loaded(stage: Stage) {
        this.stage = stage
        scene = stage.scene
        applySettings(false)
    }

    open fun applySettings(changed: Boolean) {

    }

    open fun Node.onAction(action: suspend (MouseEvent) -> Unit) {
        // launch one actor to handle all events on this node
        val eventActor = GlobalScope.actor<MouseEvent>(Dispatchers.Main) {
            for (event in channel) action(event) // pass event to action
        }
        // install a listener to offer events to this actor
        onMouseClicked = EventHandler { event ->
            eventActor.trySend(event)
        }
    }
}