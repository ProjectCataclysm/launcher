package cataclysm.launcher.ui;

import cataclysm.launcher.ui.controls.LauncherBackground;
import cataclysm.launcher.ui.controls.WindowHeader;
import cataclysm.launcher.ui.forms.dialog.ModalDialog;
import cataclysm.launcher.ui.forms.initialization.InitializeForm;
import cataclysm.launcher.ui.forms.login.LoginForm;
import cataclysm.launcher.ui.forms.main.MainForm;
import cataclysm.launcher.utils.Account;
import cataclysm.launcher.utils.PlatformHelper;
import cataclysm.launcher.utils.Settings;
import cataclysm.launcher.utils.logging.Log;
import cataclysm.launcher.utils.reporter.CrashReporter;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import ru.knoblul.winjfx.MinimizeAnimationListener;
import ru.knoblul.winjfx.Taskbar;
import ru.knoblul.winjfx.TrayIcon;
import ru.knoblul.winjfx.WindowHandleAccessor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * <br><br>REVOM ENGINE / ProjectCataclysm
 * <br>Created: 02.10.2020 18:25
 *
 * @author Knoblul
 */
public class Launcher extends Application {
	public static Launcher instance;

	private Path launcherDirPath;

	private Settings settings;
	private Account account;

	private Stage primaryStage;
	private Taskbar taskbar;
	private TrayIcon trayIcon;

	private LauncherBackground background;

	private Pane currentForm;

	private MainForm mainForm;
	private LoginForm loginForm;
	private InitializeForm initializeForm;

	public Settings getSettings() {
		return settings;
	}

	public Account getAccount() {
		return account;
	}

	public Taskbar getTaskbar() {
		return taskbar;
	}

	public TrayIcon getTrayIcon() {
		return trayIcon;
	}

	public LauncherBackground getBackground() {
		return background;
	}

	public LoginForm getLoginForm() {
		return loginForm;
	}

	public MainForm getMainForm() {
		return mainForm;
	}

	public InitializeForm getInitializeForm() {
		return initializeForm;
	}

	public Launcher() {
		instance = this;
	}

	private void setupIO() {
		launcherDirPath = PlatformHelper.getLauncherDirectoryPath();
		settings = new Settings(launcherDirPath);
		settings.load();
		account = new Account(launcherDirPath);
	}

	private void setupTrayIcon() {
		trayIcon = new TrayIcon(new Image("/icons/icon_16.png"));
		trayIcon.setTooltip("Project Cataclysm Лаунчер");
		trayIcon.setOnAction(event -> Platform.runLater(this::showStage));

		List<MenuItem> items = trayIcon.getContextMenu().getItems();

		MenuItem showLauncher = new MenuItem("Показать лаунчер");
		showLauncher.setOnAction(event -> Platform.runLater(this::showStage));
		items.add(showLauncher);

		MenuItem exit = new MenuItem("Закрыть лаунчер");
		exit.setOnAction(event -> close());
		items.add(exit);
	}

	private void setupUI() {
		BorderPane launcherPane = new BorderPane();
		launcherPane.setBackground(null);
		launcherPane.getStyleClass().add("launcher");

		background = new LauncherBackground("/icons/backgrounds/%d.jpg", 5);
		background.getStyleClass().add("launcher-background");

		WindowHeader topPane = new WindowHeader(getPrimaryStage(), true);
		launcherPane.setTop(topPane);

		mainForm = new MainForm(this);
		loginForm = new LoginForm(this);
		initializeForm = new InitializeForm(this);

		StackPane formWrapper = new StackPane(mainForm, initializeForm, loginForm);
		Rectangle clip = new Rectangle();
		clip.widthProperty().bind(formWrapper.widthProperty());
		clip.heightProperty().bind(formWrapper.heightProperty());
		formWrapper.setClip(clip);
		launcherPane.setCenter(formWrapper);

		loginForm.setVisible(false);
		loginForm.setOpacity(0);
		mainForm.setVisible(false);
		mainForm.setOpacity(0);
		currentForm = initializeForm;

		background.createBlurEffect(() -> topPane);
		background.getChildren().add(launcherPane);
		StackPane launcherShadow = new StackPane(background);
		launcherShadow.getStyleClass().add("launcher-shadow");

		Scene scene = new Scene(launcherShadow, 800, 550);
		scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
		scene.setFill(Color.TRANSPARENT);
		primaryStage.setScene(scene);
		primaryStage.initStyle(StageStyle.TRANSPARENT);

		primaryStage.setTitle("ProjectCataclysm Лаунчер");
		primaryStage.getIcons().addAll(new Image("/icons/icon_16.png"), new Image("/icons/icon_32.png"),
				new Image("/icons/icon_64.png"));

		primaryStage.setOnCloseRequest(event -> hide());
		Platform.setImplicitExit(false);

		background.buildBlurEffects();

		WindowHandleAccessor windowHandleAccessor = WindowHandleAccessor.create(primaryStage);
		taskbar = new Taskbar(windowHandleAccessor);
		setupTrayIcon();

		primaryStage.iconifiedProperty().addListener(new MinimizeAnimationListener(primaryStage, windowHandleAccessor));

		showStage();

		if (shouldLaunchReporter()) {
			try {
				initializeForm.setOpacity(0);
				initCrashReporter();
			} catch (IOException e) {
				Log.warn(e, "Failed to init crash reporter");
			}
		}

		initializeForm.setOpacity(1);
		initializeForm.beginInitialization();
	}

	private boolean shouldLaunchReporter() {
		return getParameters().getRaw().contains("--reporter") || CrashReporter.deleteCrashReportTrace(settings.gameDirPath);
	}

	private void hide() {
//		primaryStage.hide();
		close();
	}

	@Override
	public void start(Stage primaryStage) {
		try {
			Log.msg("Starting launcher application");
			this.primaryStage = primaryStage;
			setupIO();
			setupUI();
		} catch (Throwable e) {
			try {
				close();
			} catch (Throwable x) {
				x.printStackTrace();
			}

			throw e;
		}
	}

	@Override
	public void stop() throws Exception {
		super.stop();
		trayIcon.destroy();
		taskbar.destroy();
	}

	public Path getLauncherDirPath() {
		return launcherDirPath;
	}

	public void performTransition(Pane targetForm) {
		performTransition(targetForm, null);
	}

	public void performTransition(Pane targetForm, Runnable onTransitionFinished) {
		Duration duration = Duration.seconds(0.75);

		ParallelTransition transition;
		if (targetForm == loginForm) {
			FadeTransition loginFade = new FadeTransition(duration, loginForm);
			loginFade.setFromValue(0);
			loginFade.setToValue(1);
			loginFade.setInterpolator(Interpolator.EASE_BOTH);

			ScaleTransition loginScale = new ScaleTransition(duration, loginForm);
			loginScale.setFromX(1.5);
			loginScale.setFromY(1.5);
			loginScale.setToX(1);
			loginScale.setToY(1);
			loginScale.setInterpolator(Interpolator.EASE_BOTH);

			FadeTransition currentFade = new FadeTransition(duration, currentForm);
			currentFade.setFromValue(1);
			currentFade.setToValue(0);
			currentFade.setInterpolator(Interpolator.EASE_BOTH);

			transition = new ParallelTransition(loginFade, loginScale, currentFade);
			transition.setInterpolator(Interpolator.EASE_BOTH);
		} else {
			FadeTransition targetFade = new FadeTransition(duration, targetForm);
			targetFade.setFromValue(0);
			targetFade.setToValue(1);
			targetFade.setInterpolator(Interpolator.EASE_BOTH);

			FadeTransition currentFade = new FadeTransition(duration.multiply(0.5), currentForm);
			currentFade.setFromValue(1);
			currentFade.setToValue(0);
			currentFade.setInterpolator(Interpolator.EASE_BOTH);

			ScaleTransition currentScale = new ScaleTransition(duration, currentForm);
			currentScale.setFromX(1);
			currentScale.setFromY(1);
			currentScale.setToX(1.5);
			currentScale.setToY(1.5);
			currentScale.setInterpolator(Interpolator.EASE_BOTH);

			transition = new ParallelTransition(targetFade, currentFade, currentScale);
			transition.setInterpolator(Interpolator.EASE_BOTH);
		}

		Platform.runLater(transition::play);

		Pane finalCurrentForm = currentForm;
		transition.setOnFinished(event -> {
			finalCurrentForm.setVisible(false);
			targetForm.setVisible(true);
			Optional.ofNullable(onTransitionFinished).ifPresent(Runnable::run);

			if (targetForm == mainForm) {
				mainForm.handleShown();
			}
		});

		currentForm.setVisible(true);
		targetForm.setVisible(true);

		currentForm = targetForm;
	}

	public int showModal(boolean infoDialog, String title, String message) {
		ModalDialog modalDialog = new ModalDialog(this, infoDialog);
		modalDialog.setTitle(title);
		modalDialog.setMessage(message);
		return modalDialog.show();
	}

	public int showModal(boolean infoDialog, String title, Node message) {
		ModalDialog modalDialog = new ModalDialog(this, infoDialog);
		modalDialog.setTitle(title);
		modalDialog.setMessage(message);
		return modalDialog.show();
	}

//	public void toggleGameLoadingPane() {
//		if (mainForm.isSettingsPaneVisible()) {
//			toggleSettingsPane();
//		}
//
//		mainForm.toggleGameLoadingPane();
//	}

//	public void toggleSettingsPane() {
//		mainForm.toggleSettingsPane();
//		// settings.save()
//	}

	public Stage getPrimaryStage() {
		return primaryStage;
	}

	public void showStage() {
		if (primaryStage.isIconified()) {
			primaryStage.setIconified(false);
		}

		primaryStage.show();
		primaryStage.requestFocus();
	}

	public void initCrashReporter() throws IOException {
		Log.msg("Initailizing crash-reporter...");

		if (!initializeForm.checkLoginHolderForReporter()) {
			throw new IOException("Login required");
		}

		CrashReporter.showCrashReporterDialog(this);
	}

	public void close() {
		Log.msg("Shutting down...");
		primaryStage.close();
		Platform.exit();
	}

	public void logout() {
		loginForm.clear();
		performTransition(loginForm, () -> {
			if (mainForm.isSettingsPaneVisible()) {
				mainForm.toggleSettingsPane();
			}
		});

		try {
			account.logout();
		} catch (IOException e) {
			Log.warn(e, "Failed to logout");
		}
	}
}
