package ru.cataclysm.controllers

import com.frostwire.jlibtorrent.SessionStats
import com.frostwire.jlibtorrent.TorrentStatus
import javafx.scene.control.ProgressBar
import javafx.scene.text.Text
import javafx.stage.Stage
import kotlinx.coroutines.launch
import me.pavanvo.appfx.CustomController
import ru.cataclysm.controls.SVGButton
import ru.cataclysm.controls.SVGLabel
import ru.cataclysm.helpers.Converter.readableSize
import ru.cataclysm.scopeFX
import ru.cataclysm.services.assets.AssetsService
import ru.cataclysm.services.assets.CheckResult


class UpdateController : CustomController() {
    lateinit var updateState: SVGLabel
    lateinit var continueUpdateButton: SVGButton
    lateinit var pauseUpdateButton: SVGButton
    lateinit var downloadedSizeLabel: Text
    lateinit var downloadSpeedLabel: Text
    lateinit var updateProgressBar: ProgressBar


    override fun loaded(stage: Stage) {
        super.loaded(stage)
        // Service Events
        AssetsService.onProgressUpdated += ::updateProgress
        AssetsService.onStateChanged += ::stateChanged
        AssetsService.onChecked += ::checked
        AssetsService.onTraffic += ::updateSpeed
        AssetsService.onDownload += { AssetsService.verify() }

        pauseUpdateButton.isDisable = true
        AssetsService.checkForUpdates()
        AssetsService.startBittorrentCore()
    }

    private fun stateChanged(state: TorrentStatus.State) = scopeFX.launch {
        showProgress()
        when (state) {
            TorrentStatus.State.CHECKING_FILES -> {
                updateState.text = "Проверка файлов";
                updateProgress(0F)
            }

            TorrentStatus.State.DOWNLOADING_METADATA -> {
                updateState.text = "Загрузка данных";
                updateProgress(0F)
            }
            TorrentStatus.State.DOWNLOADING -> {
                updateState.text = "Загрузка"
            }
            TorrentStatus.State.FINISHED -> {
                updateState.text = "Завершено";
                updateProgress(1F)
            }
            TorrentStatus.State.SEEDING -> {
                updateState.text = "Раздаётся";
                updateProgress(1F)
            }
            TorrentStatus.State.CHECKING_RESUME_DATA -> {
                updateState.text = "Восстановление";
                updateProgress(0F)
            }
            TorrentStatus.State.UNKNOWN -> updateState.text = "Неизвестно"
        }
    }

    private fun checked(state: CheckResult) = scopeFX.launch {
        hideProgress()
        when (state) {
            CheckResult.DOWNLOAD_AVAILABLE -> {
                updateState.text = "Доступна загрузка"
            }

            CheckResult.IN_PROGRESS -> {
                updateState.text = "Проверка обновления"
            }

            CheckResult.UPDATE_AVAILABLE -> {
                updateState.text = "Доступно обновление";
            }

            CheckResult.READY -> {
                updateButton_Click()
            }

            CheckResult.FAIL -> {
                updateState.text = "Не удалось обновить"
            }
        }
    }

    private fun updateProgress(progress: Float) = scopeFX.launch {
        updateProgressBar.progress = progress.toDouble()
        val current = (AssetsService.totalSize * progress).toLong()
        downloadedSizeLabel.text = "${readableSize(current)} / ${readableSize(AssetsService.totalSize)}"
    }

    private fun updateSpeed(stats: SessionStats) = scopeFX.launch {
        downloadSpeedLabel.text = "${readableSize(stats.downloadRate())}/s /" +
                " ${readableSize(stats.uploadRate())}/s"
    }

    fun updateButton_Click() {
        AssetsService.resume()
        continueUpdateButton.isDisable = true
        pauseUpdateButton.isDisable = false
    }

    fun pauseButton_Click() {
        AssetsService.pause()
        continueUpdateButton.isDisable = false
        pauseUpdateButton.isDisable = true
    }

    private fun hideProgress() {
        updateProgressBar.isVisible = false
        downloadedSizeLabel.isVisible = false
        downloadSpeedLabel.isVisible = false
    }

    private fun showProgress() {
        updateProgressBar.isVisible = true
        downloadedSizeLabel.isVisible = true
        downloadSpeedLabel.isVisible = true
    }
}