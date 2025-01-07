package ru.cataclysm.services.launch

import me.pavanvo.events.Event1
import ru.cataclysm.helpers.Constants
import ru.cataclysm.services.Log
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.event.ActionEvent

/**
 * <br></br><br></br>ProjectCataclysm
 * <br></br>Created: 10.09.2022 23:30
 *
 * @author Knoblul
 */
object TrayService {
    private var tray: SystemTray? = null
    private var trayIcon: TrayIcon? = null

    val onAction: Event1<ActionEvent> = Event1()

    init {
        if (!SystemTray.isSupported()) {
            Log.err("Tray icon is not supported")
        }
        tray = SystemTray.getSystemTray()
        trayIcon = TrayIcon(Constants.Image.tray, "Project Cataclysm Launcher")
        trayIcon!!.addActionListener { it: ActionEvent ->
            onAction(it)
            uninstall()
        }
    }

    fun install() {
        try {
            tray!!.add(trayIcon!!)
        } catch (e: Exception) {
            Log.err(e, "Failed to create tray icon")
        }
    }

    fun uninstall() {
        tray!!.remove(trayIcon!!)
    }
}