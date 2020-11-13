package cataclysm.launcher.ui.forms.loading;

import cataclysm.launcher.ui.Launcher;
import cataclysm.launcher.utils.Account;
import cataclysm.launcher.utils.AsyncTasks;
import cataclysm.launcher.utils.Updater;
import cataclysm.launcher.utils.exception.LauncherUpToDateException;
import cataclysm.launcher.utils.logging.Log;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * <br><br>REVOM ENGINE / ProjectCataclysm
 * <br>Created: 07.10.2020 3:54
 *
 * @author Knoblul
 */
public class LoadingForm extends BorderPane {
	private final Launcher launcher;
	private final Label subTitle;
	private final ProgressBar progressBar;

	public LoadingForm(Launcher launcher) {
		getStyleClass().add("loading-form");

		this.launcher = launcher;

		Label title = new Label("ЗАГРУЗКА");
		title.getStyleClass().add("title");

		subTitle = new Label();
		subTitle.getStyleClass().add("sub-title");
		subTitle.setPrefHeight(50);
		subTitle.setMaxHeight(USE_PREF_SIZE);
		subTitle.setMinHeight(USE_PREF_SIZE);

		progressBar = new ProgressBar();
		progressBar.setMaxWidth(200);

		VBox center = new VBox(title, subTitle, progressBar);
		center.getStyleClass().addAll("center-pane", "padded-pane");
		center.setMaxWidth(350);
		center.setMaxHeight(USE_PREF_SIZE);

		setCenter(center);
	}

	public void beginLoading() {
		CompletableFuture<Void> versionCheckFuture = CompletableFuture
				.runAsync(this::checkForUpdates, AsyncTasks.executor)
				.thenRunAsync(this::updateLauncher, AsyncTasks.executor);

		AsyncTasks.whenComplete(versionCheckFuture, (result, exception) -> {
			if (!(exception instanceof LauncherUpToDateException) && exception != null) {
				Log.err(exception, "Launcher update failed");
				Platform.runLater(() -> launcher.showModal(true, "Ошибка", exception.toString()));
			}

			CompletableFuture.runAsync(this::checkLoginHolder, AsyncTasks.executor);
		});
	}

	private void checkForUpdates() {
		try {
			Platform.runLater(() -> subTitle.setText("Проверка версии лаунчера..."));
			Updater.checkVersion(launcher.getLauncherDirPath());
		} catch (IOException e) {
			Log.err("Version check failed");
			Platform.runLater(() -> launcher.showModal(true, "Обновление лаунчера",
					"Не удалось проверить наличие обновлений: " + e.toString()));
		}
	}

	private void updateLauncher() {
		Platform.runLater(() -> subTitle.setText("Обновление лаунчера..."));
		try {
			Updater.updateLauncher(launcher.getLauncherDirPath());
		} catch (IOException e) {
			Log.err(e, "Launcher update failed");
			Platform.runLater(() -> launcher.showModal(true, "Обновление лаунчера",
					"Не удалось обновить лаунчер: " + e.toString()));
		}
	}

	private void checkLoginHolder() {
		Platform.runLater(() -> progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS));

		Account account = launcher.getAccount();
		try {
			Platform.runLater(() -> subTitle.setText("Проверка аккаунта..."));
			account.validate();
			Platform.runLater(() -> launcher.performTransition(launcher.getMainForm()));
		} catch (IOException e) {
			Log.err(e, "Session validation failed");
			Platform.runLater(() -> launcher.performTransition(launcher.getLoginForm()));
		}
	}

	public boolean checkLoginHolderForReporter() {
		Account account = launcher.getAccount();
		try {
			account.validate();
			return true;
		} catch (IOException e) {
			Log.err(e, "Session validation failed");
			return false;
		}
	}
}
