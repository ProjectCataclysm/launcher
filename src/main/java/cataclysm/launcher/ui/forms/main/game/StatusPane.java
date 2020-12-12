package cataclysm.launcher.ui.forms.main.game;

import cataclysm.launcher.ui.Launcher;
import cataclysm.launcher.ui.controls.IconButton;
import cataclysm.launcher.ui.controls.LabelField;
import cataclysm.launcher.ui.controls.LargeButton;
import cataclysm.launcher.ui.forms.dialog.ModalDialog;
import cataclysm.launcher.ui.forms.launch.LaunchForm;
import cataclysm.launcher.ui.forms.main.MainForm;
import com.google.common.base.Throwables;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIconView;
import javafx.animation.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * <br><br>REVOM ENGINE / ProjectCataclysm
 * <br>Created: 14.10.2020 15:41
 *
 * @author Knoblul
 */
public class StatusPane extends StackPane {
	private final Launcher launcher;
	private final Button playButton;
	private final ParallelTransition launchFormSwipeAnimation;

	public StatusPane(Launcher launcher, MainForm mainForm) {
		this.launcher = launcher;

		getStyleClass().add("bottom-content");

		playButton = new LargeButton("Играть");
		playButton.getStyleClass().add("play-button");

		Button settingsButton = new IconButton("Настройки", new MaterialDesignIconView(MaterialDesignIcon.SETTINGS));
		settingsButton.setOnAction(event -> mainForm.toggleSettingsPane());
		settingsButton.getStyleClass().add("settings-button");

		Button logoutButton = new IconButton("Выход", new MaterialDesignIconView(MaterialDesignIcon.LOGOUT));
		logoutButton.getStyleClass().add("logout-button");
		logoutButton.setOnAction(event -> {
			if (launcher.showModal(false, "Подтвердите", "Вы уверены что хотите выйти из аккаунта?")
					== ModalDialog.YES_OPTION) {
				launcher.logout();
			}
//			launcher.getTrayIcon().showMessage("Hello", "Lorem ipsum dolor sit amet", TrayIcon.MessageType.INFO);
		});

		VBox vbox = new VBox(settingsButton, logoutButton);
		vbox.getStyleClass().add("user-actions");

		BorderPane mainPane = new BorderPane();
		Label userNameLabel = new Label();
		userNameLabel.textProperty().bind(launcher.getAccount().usernameProperty());
		userNameLabel.getStyleClass().add("username");

		mainPane.getStyleClass().add("padded-pane");
		mainPane.setLeft(vbox);
		mainPane.setCenter(userNameLabel);
		mainPane.setRight(playButton);

		LaunchForm launchForm = new LaunchForm(launcher, this);
		launchForm.setManaged(false);
		launchForm.layoutYProperty().bind(mainPane.heightProperty());
		mainPane.layoutBoundsProperty().addListener((observable, oldValue, newValue) ->
				launchForm.resize(mainPane.getWidth(), mainPane.getHeight()));

		Duration swipeDuration = Duration.seconds(0.5);

//		TranslateTransition translateMain = new TranslateTransition(swipeDuration, mainPane);
//		translateMain.toYProperty().bind(mainPane.heightProperty().negate());
//		translateMain.setFromY(0);
//		translateMain.setInterpolator(Interpolator.EASE_BOTH);

		ScaleTransition scaleMain = new ScaleTransition(swipeDuration, mainPane);
		scaleMain.setFromX(1);
		scaleMain.setFromY(1);
		scaleMain.setToX(0.9);
		scaleMain.setToY(0.9);

		FadeTransition fadeMain = new FadeTransition(swipeDuration.multiply(0.5), mainPane);
		fadeMain.setFromValue(1);
		fadeMain.setToValue(0);
		fadeMain.setInterpolator(Interpolator.EASE_BOTH);

		TranslateTransition translateLaunch = new TranslateTransition(swipeDuration, launchForm);
		translateLaunch.toYProperty().bind(mainPane.heightProperty().negate());
		translateLaunch.setFromY(0);
		translateLaunch.setInterpolator(Interpolator.EASE_BOTH);

		FadeTransition fadeLaunch = new FadeTransition(swipeDuration.multiply(3), launchForm);
		fadeLaunch.setFromValue(0);
		fadeLaunch.setToValue(1);
		fadeLaunch.setInterpolator(Interpolator.EASE_BOTH);
		launchForm.setOpacity(0);

		launchFormSwipeAnimation = new ParallelTransition(
				//translateMain,
				scaleMain,
				fadeMain,
				translateLaunch,
				fadeLaunch
		);
		launchFormSwipeAnimation.setInterpolator(Interpolator.EASE_BOTH);
		playButton.setOnAction(event -> launchForm.launch());

		Rectangle clip = new Rectangle();
		clip.widthProperty().bind(mainPane.widthProperty());
		clip.heightProperty().bind(mainPane.heightProperty());
		setClip(clip);
		getChildren().addAll(mainPane, launchForm);
	}

	public void setLaunchFormVisible(boolean launchFormVisible) {
		launchFormSwipeAnimation.setRate(launchFormVisible ? 1 : -1);
		launchFormSwipeAnimation.play();
	}

	public void handleLaunchTaskChainCompleted(Throwable cause) {
		setLaunchFormVisible(false);
		if (cause != null) {
			LabelField text = new LabelField(Throwables.getStackTraceAsString(cause));
			text.setWrapText(false);
			text.getStyleClass().add("exception-text");
			text.setMaxHeight(300);

			Label title = new Label("Не удалось запустить игру:");
			title.getStyleClass().add("exception-title");

			launcher.showModal(true, "Ошибка", new VBox(title, text));
			playButton.setText("Играть");
		} else {
			playButton.setText("Завершить игру");
		}
	}
}
