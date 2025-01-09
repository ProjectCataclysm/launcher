package ru.cataclysm.helpers

import javafx.scene.image.Image
import ru.cataclysm.Launcher
import java.io.File
import java.net.URL
import java.time.format.DateTimeFormatter
import java.util.*
import javax.imageio.ImageIO

object Constants {
    var DATETIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd_MM_yyyy_HH_mm_ss", Locale.ROOT)

    const val WORK_SITE_URL: String = "project-cataclysm.ru"
    const val API_URL: String = "$WORK_SITE_URL/api"
    const val LAUNCHER_URL: String = "files.$WORK_SITE_URL/files/launcher"
    const val CLIENT_URL: String = "files.$WORK_SITE_URL/files/client"
    const val TORRENT_URL: String = "http://itorrents.org/torrent/6DF63688C25658D3FB05DB5BD3C5B42A7BA035A8.torrent"

    object Path {
        var jarLocation: String = javaClass.protectionDomain.codeSource.location.path
        val jarDirectory: File = File(jarLocation).parentFile

        const val BASE = "ru/cataclysm/"
        const val VIEWS = "${BASE}views/"
        const val IMAGES = "${BASE}images/"
        const val ICONS = "${IMAGES}icons/"
        const val CONTROLS = "${BASE}controls/"
        const val STYLES = "${BASE}styles/"
    }

    object View {
        val PRELOADER: URL = javaClass.classLoader.getResource("${Path.VIEWS}Preloader.fxml")!!
        val MAIN: URL = javaClass.classLoader.getResource("${Path.VIEWS}Main.fxml")!!
        val REGISTER: URL = javaClass.classLoader.getResource("${Path.VIEWS}${App.REGISTER}.fxml")!!
        val LOGIN: URL = javaClass.classLoader.getResource("${Path.VIEWS}${App.LOGIN}.fxml")!!
        val ERROR: URL = javaClass.classLoader.getResource("${Path.VIEWS}Error.fxml")!!
        val LOADING: URL = javaClass.classLoader.getResource("${Path.VIEWS}Loading.fxml")!!
    }

    object App {
        const val NAME = "Project Cataclysm"
        const val REGISTER = "Register"
        const val LOGIN = "Login"
        const val SETTINGS = "Settings"
        val version: String
            get() {
                val default: String? = Launcher::class.java.getPackage().implementationVersion
                return default ?: "debug"
            }
    }

    object Image {
        val icon = Image(javaClass.classLoader.getResourceAsStream("${Path.ICONS}icon_64.png"))
        val tray = ImageIO.read(javaClass.classLoader.getResourceAsStream("${Path.ICONS}icon_16.png"))
    }

    object Controls {
        fun getControl(id: String): URL {
            return javaClass.classLoader.getResource("${Path.CONTROLS}$id.fxml")!!
        }
    }

    object Styles {
        val alert: URL = javaClass.classLoader.getResource("${Path.STYLES}alert.css")!!
    }
}