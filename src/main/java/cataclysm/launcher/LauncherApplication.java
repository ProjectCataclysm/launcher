package cataclysm.launcher;

import cataclysm.launcher.account.AccountManager;
import cataclysm.launcher.launch.TrayManager;
import cataclysm.launcher.selfupdate.AutoUpdater;
import cataclysm.launcher.selfupdate.LauncherRestartedException;
import cataclysm.launcher.ui.*;
import cataclysm.launcher.utils.LauncherConfig;
import cataclysm.launcher.utils.LauncherConstants;
import cataclysm.launcher.utils.Log;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.*;

/**
 * Created 27 сент. 2018 г. / 21:10:13
 *
 * @author Knoblul
 */
public class LauncherApplication {
	private static LauncherApplication instance;

	private final LauncherConfig config = new LauncherConfig();
	private final AccountManager accountManager = new AccountManager();
	private final ScheduledExecutorService mainExecutor;
	private final ScheduledExecutorService parallelExecutor;

	private Stage primaryStage;
	private LauncherOverlayManager overlays;
	private SessionInfoPane sessionInfoPane;
	private Button startButton;
	private TrayManager trayManager;

	public LauncherApplication() {
		instance = this;
		mainExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
			Thread t = new Thread(r);
			t.setName("Main Task Executor");
			t.setDaemon(true);
			return t;
		});
		parallelExecutor = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors(), r -> {
			Thread t = new Thread(r);
			t.setName("Parallel Task Executor");
			t.setDaemon(true);
			return t;
		});
	}

	public static LauncherApplication getInstance() {
		return instance;
	}

	public ScheduledExecutorService getMainExecutor() {
		return mainExecutor;
	}

	public ScheduledExecutorService getParallelExecutor() {
		return parallelExecutor;
	}

	public Executor getDelayedExecutor(int delaySeconds) {
		return r -> mainExecutor.schedule(r, delaySeconds, TimeUnit.SECONDS);
	}

	public LauncherConfig getConfig() {
		return config;
	}

	public AccountManager getAccountManager() {
		return accountManager;
	}

	public LauncherOverlayManager getOverlays() {
		return overlays;
	}

	public SessionInfoPane getSessionInfoPane() {
		return sessionInfoPane;
	}

	public Stage getPrimaryStage() {
		return primaryStage;
	}
	//	public void launch() {
//		Thread t = new Thread(() -> {
//		executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() - 1,
//				new ThreadFactoryBuilder().setNameFormat("Parallel Executor #%d").build());
//			try {
//				gameLauncher.startGame();
//			} finally {
//				//noinspection UnstableApiUsage
//				MoreExecutors.shutdownAndAwaitTermination(executor, 10, TimeUnit.SECONDS);
//			}
//		});
//		t.setDaemon(true);
//		t.setName("Game Launcher");
//		t.start();

//	}

	public void start(Stage primaryStage) {
		// Загружаем иконки окна
		StageUtils.setupIcons(primaryStage);
		this.primaryStage = primaryStage;

		sessionInfoPane = new SessionInfoPane(this);
		startButton = createStartGameButton();
		AnchorPane launcherNode = new AnchorPane();
		launcherNode.getStyleClass().add("launcher");
		launcherNode.getChildren().addAll(createSettingsButton(), sessionInfoPane, startButton);

		overlays = new LauncherOverlayManager(launcherNode);

		primaryStage.setResizable(false);
		primaryStage.setScene(StageUtils.createScene(new StackPane(launcherNode, overlays), 400, 400));
		primaryStage.setTitle("ProjectCataclysm Launcher " + AutoUpdater.VERSION);

		primaryStage.setOnCloseRequest(event -> closeApplication());
		primaryStage.setOnShown(event -> checkForUpdates(0));
		primaryStage.show();

		trayManager = new TrayManager();

		Platform.setImplicitExit(false);
	}

	public TrayManager getTrayManager() {
		return trayManager;
	}

	private void closeApplication() {
		Platform.exit();
		trayManager.uninstall();
	}

	private void checkForUpdates(int delay) {
		if (!AutoUpdater.updateEnabled()) {
			validateLogin(2);
			return;
		}

		CompletableFuture.runAsync(() -> overlays.getLoading().showLoading("Проверяем наличие обновлений", 0), Platform::runLater)
				.thenRunAsync(() -> AutoUpdater.check(overlays.getLoading()), getDelayedExecutor(delay))
				.handle((result, cause) -> handleUpdateCheckResult(cause))
				.whenComplete(Log::logFutureErrors);
	}

	private void validateLogin(int delay) {
		CompletableFuture.runAsync(() -> overlays.getLoading().showLoading("Проверка аккаунта...", 0), Platform::runLater)
				.thenRunAsync(accountManager::validateSession, getDelayedExecutor(delay))
				.handleAsync((result, cause) -> handleValidateResult(cause), Platform::runLater)
				.whenComplete(Log::logFutureErrors);
	}

	private Void handleValidateResult(Throwable cause) {
		Throwable realCause = cause;
		if (cause instanceof CompletionException && cause.getCause() != null) {
			realCause = cause.getCause();
		}

		if (LauncherConstants.isUnreachableException(realCause)) {
			overlays.getLoading().showError("Failed to validate account due to unreachable services",
					"Не удалось войти в аккаунт", cause, () -> validateLogin(2));
		} else if (realCause != null || accountManager.getSession() == null) {
			if (realCause != null) {
				Log.err(realCause, "Failed to validate account");
			}

			overlays.getLogin().show();
		} else {
			sessionInfoPane.init(accountManager.getSession());
			overlays.getLoading().hide();
		}

		return null;
	}

	private Void handleUpdateCheckResult(Throwable cause) {
		Throwable realCause = cause;
		if (cause instanceof CompletionException && cause.getCause() != null) {
			realCause = cause.getCause();
		}

		if (realCause == null) {
			validateLogin(0);
			return null;
		}

		if (realCause instanceof LauncherRestartedException) {
//			Platform.exit();
			System.exit(0);
			return null;
		}

		Platform.runLater(() -> overlays.getLoading()
				.showError("Failed to check for launcher updates", "Не удалось проверить наличие обновлений лаунчера",
						cause, () -> checkForUpdates(2)));
		return null;
	}

	@NotNull
	private Button createSettingsButton() {
		Button button = new Button();
		button.setOnMouseClicked(event -> SettingsDialog.show(this));
		Tooltip.install(button, new Tooltip("Настройки..."));
		button.getStyleClass().add("settings-button");
		button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

		HoverAnimation.install(button);
		AnchorPane.setLeftAnchor(button, 1.0);
		AnchorPane.setRightAnchor(button, 1.0);
		AnchorPane.setTopAnchor(button, 1.0);
		AnchorPane.setBottomAnchor(button, 1.0);

		return button;
	}

	private Button createStartGameButton() {
		Button button = new Button("Запуск");
		button.setOnAction(__ -> overlays.getGameLoading().startGamePressed());
		button.getStyleClass().addAll("bold-font", "start-game-button");
		button.setMaxWidth(Double.MAX_VALUE);
		AnchorPane.setBottomAnchor(button, 1.0);
		AnchorPane.setRightAnchor(button, 1.0);
		AnchorPane.setLeftAnchor(button, 1.0);
		return button;
	}

	public void setStartGameAvailable(boolean available) {
		if (available) {
			primaryStage.setOnCloseRequest(__ -> closeApplication());
			primaryStage.setOnShown(event -> checkForUpdates(0));
			if (primaryStage.isShowing()) {
				checkForUpdates(0);
			} else {
				primaryStage.show();
			}
			overlays.hideAll();
			startButton.setDisable(false);
			startButton.setText("Запуск");
		} else {
			primaryStage.setOnCloseRequest(__ -> trayManager.install());
			primaryStage.setOnShown(null);
			primaryStage.hide();
			overlays.hideAll();
			startButton.setDisable(true);
			startButton.setText("Игра запущена");
		}
	}

//	public enum State {
//		SELF_UPDATE_CHECK,
//		SELF_UPDATING,
//
//		LOGIN_VALIDATION,
//		LOGIN_UNAUTHORIZED,
//		LOGIN_SUBMIT,
//		LOGIN_INCORRECT,
//		LOGIN_OK,
//
//		GAME_UPDATE_CHECK,
//		GAME_DOWNLOADING,
//		GAME_PLAYING,
//	}
}
