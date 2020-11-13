package cataclysm.launcher.ui.forms.main.game;

import cataclysm.launcher.ui.Launcher;
import cataclysm.launcher.ui.controls.IconButton;
import cataclysm.launcher.ui.controls.LargeButton;
import cataclysm.launcher.ui.forms.main.MainForm;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIconView;
import javafx.animation.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import ru.knoblul.winjfx.TrayIcon;

/**
 * <br><br>REVOM ENGINE / ProjectCataclysm
 * <br>Created: 14.10.2020 15:41
 *
 * @author Knoblul
 */
public class StatusPane extends StackPane {
	private final ParallelTransition downloadingSwipeAnimation;
	private boolean downloadVisible;

	public StatusPane(Launcher launcher, MainForm mainForm) {
		getStyleClass().add("bottom-content");

		Button playButton = new LargeButton("Играть");
		playButton.getStyleClass().add("play-button");

		Button settingsButton = new IconButton("Настройки", new MaterialDesignIconView(MaterialDesignIcon.SETTINGS));
		settingsButton.setOnAction(event -> mainForm.toggleSettingsPane());
		settingsButton.getStyleClass().add("settings-button");

		Button logoutButton = new IconButton("Выход", new MaterialDesignIconView(MaterialDesignIcon.LOGOUT));
		logoutButton.getStyleClass().add("logout-button");
		logoutButton.setOnAction(event -> {
//			if (launcher.showModal(false, "Подтвердите", "Вы уверены что хотите выйти из аккаунта?")) {
//				launcher.logout();
//			}
			launcher.getTrayIcon().showMessage("Hello", "Lorem ipsum dolor sit amet", TrayIcon.MessageType.INFO);
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

		BorderPane downloadingPane = new BorderPane();
		downloadingPane.getStyleClass().addAll("padded-pane", "downloading-pane");
		downloadingPane.setBackground(new Background(new BackgroundFill(new Color(0, 0, 0, 0.35), null, null)));
		downloadingPane.setCenter(new Label("IF YOU READ THIS VI VON"));
		downloadingPane.setManaged(false);
		downloadingPane.layoutYProperty().bind(mainPane.heightProperty());
		downloadingPane.setOnMouseClicked(event -> toggleDownloadingPane());
		mainPane.layoutBoundsProperty().addListener((observable, oldValue, newValue) ->
				downloadingPane.resize(mainPane.getWidth(), mainPane.getHeight()));

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

		TranslateTransition translateLoading = new TranslateTransition(swipeDuration, downloadingPane);
		translateLoading.toYProperty().bind(mainPane.heightProperty().negate());
		translateLoading.setFromY(0);
		translateLoading.setInterpolator(Interpolator.EASE_BOTH);

		FadeTransition fadeLoading = new FadeTransition(swipeDuration.multiply(3), downloadingPane);
		fadeLoading.setFromValue(0);
		fadeLoading.setToValue(1);
		fadeLoading.setInterpolator(Interpolator.EASE_BOTH);
		downloadingPane.setOpacity(0);

		downloadingSwipeAnimation = new ParallelTransition(
				//translateMain,
				scaleMain,
				fadeMain,
				translateLoading,
				fadeLoading
		);
		downloadingSwipeAnimation.setInterpolator(Interpolator.EASE_BOTH);
		playButton.setOnAction(event -> toggleDownloadingPane());

		Rectangle clip = new Rectangle();
		clip.widthProperty().bind(mainPane.widthProperty());
		clip.heightProperty().bind(mainPane.heightProperty());
		setClip(clip);
		getChildren().addAll(mainPane, downloadingPane);
	}

	public boolean isDownloadVisible() {
		return downloadVisible;
	}

	public void toggleDownloadingPane() {
		downloadingSwipeAnimation.setRate(downloadVisible ? -1 : 1);
		downloadingSwipeAnimation.play();
		downloadVisible = !downloadVisible;
	}
}

