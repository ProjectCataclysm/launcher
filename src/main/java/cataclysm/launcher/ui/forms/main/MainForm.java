package cataclysm.launcher.ui.forms.main;

import cataclysm.launcher.ui.Launcher;
import cataclysm.launcher.ui.forms.main.game.StatusPane;
import cataclysm.launcher.ui.forms.main.info.ServerInfoPane;
import cataclysm.launcher.ui.forms.main.news.NewsPane;
import cataclysm.launcher.ui.forms.main.settings.SettingsPane;
import cataclysm.launcher.utils.AsyncTasks;
import cataclysm.launcher.utils.HttpHelper;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.layout.*;
import javafx.util.Duration;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * <br><br>REVOM ENGINE / ProjectCataclysm
 * <br>Created: 07.10.2020 3:40
 *
 * @author Knoblul
 */
public class MainForm extends BorderPane {
	private final Launcher launcher;

	private boolean wasShown;

	private NewsPane newsPane;
	private ServerInfoPane serverInfoPane;

	private ParallelTransition settingsSwipeAnimation;
	private boolean settingsPaneVisible;

	public MainForm(Launcher launcher) {
		this.launcher = launcher;
		setCenter(createCenterPane());
		setBottom(createBottomPane());
	}

	private Pane createCenterPane() {
		StackPane centerPane = new StackPane();

		newsPane = new NewsPane();
		serverInfoPane = new ServerInfoPane();
		HBox.setHgrow(newsPane, Priority.ALWAYS);
		HBox mainPane = new HBox(newsPane, serverInfoPane);
		mainPane.getStyleClass().addAll("padded-pane", "center-content");

		SettingsPane settingsPane = new SettingsPane(launcher, launcher.getSettings());
		settingsPane.getBackButton().setOnAction(event -> toggleSettingsPane());
		settingsPane.setManaged(false);
		settingsPane.layoutXProperty().bind(mainPane.widthProperty().negate());

		mainPane.layoutBoundsProperty().addListener((observable, oldValue, newValue) ->
				settingsPane.resize(mainPane.getWidth(), mainPane.getHeight()));

		Duration swipeDuration = Duration.seconds(0.35);

		TranslateTransition translateMain = new TranslateTransition(swipeDuration, mainPane);
		translateMain.toXProperty().bind(mainPane.widthProperty());
		translateMain.setFromX(0);
		translateMain.setInterpolator(Interpolator.EASE_BOTH);

		FadeTransition fadeMain = new FadeTransition(swipeDuration, mainPane);
		fadeMain.setFromValue(1);
		fadeMain.setToValue(0);
		fadeMain.setInterpolator(Interpolator.EASE_BOTH);

		TranslateTransition translateSettings = new TranslateTransition(swipeDuration, settingsPane);
		translateSettings.toXProperty().bind(mainPane.widthProperty());
		translateSettings.setFromX(0);
		translateSettings.setInterpolator(Interpolator.EASE_BOTH);

		FadeTransition fadeSettings = new FadeTransition(swipeDuration, settingsPane);
		fadeSettings.setFromValue(0);
		fadeSettings.setToValue(1);
		fadeSettings.setInterpolator(Interpolator.EASE_BOTH);
		settingsPane.setOpacity(0);

		settingsSwipeAnimation = new ParallelTransition(
				translateMain,
				fadeMain,
				translateSettings,
				fadeSettings
		);
		settingsSwipeAnimation.setInterpolator(Interpolator.EASE_BOTH);
		centerPane.getChildren().addAll(mainPane, settingsPane);
		launcher.getBackground().createBlurEffect(() -> settingsPane);
		return centerPane;
	}

	private Pane createBottomPane() {
		StatusPane bottomPane = new StatusPane(launcher, this);
		launcher.getBackground().createBlurEffect(() -> bottomPane);
		return bottomPane;
	}

	public boolean isSettingsPaneVisible() {
		return settingsPaneVisible;
	}

	public void toggleSettingsPane() {
		settingsSwipeAnimation.setRate(settingsPaneVisible ? -1 : 1);
		settingsSwipeAnimation.play();
		settingsPaneVisible = !settingsPaneVisible;
	}

	public void handleShown() {
		if (!wasShown) {

			CompletableFuture<JSONObject> future = CompletableFuture.supplyAsync(() -> {
				try {
					return HttpHelper.executeJsonObjectRequest(HttpHelper.API_URL + "/news", null);
				} catch (IOException e) {
					throw new CompletionException(e);
				}
			}, AsyncTasks.executor);
			newsPane.updateNews(future);
			serverInfoPane.updatePatchNote(future);
			serverInfoPane.startServerPingTimer();

			wasShown = true;
		}
	}
}
