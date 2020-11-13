package cataclysm.launcher.ui.forms.main.info;

import cataclysm.launcher.ui.controls.IconButton;
import cataclysm.launcher.ui.controls.LabelField;
import cataclysm.launcher.utils.AsyncTasks;
import cataclysm.launcher.utils.HttpHelper;
import cataclysm.launcher.utils.PlatformHelper;
import cataclysm.launcher.utils.logging.Log;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIconView;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;

/**
 * <br><br>REVOM ENGINE / ProjectCataclysm
 * <br>Created: 03.10.2020 18:36
 *
 * @author Knoblul
 */
public class ServerInfoPane extends BorderPane {
	private final ProgressIndicator patchNoteLoadingIndicator;
	private final StackPane patchNoteWrapper;
	private final LabelField errorLabel;
	private final TextArea patchNote;

	private final Label versionLabel;
	private final Label statusLabel;
	private final Label onlinePlayers;

	public ServerInfoPane() {
		getStyleClass().addAll("server-info", "content-card");

		ImageView logo = new ImageView("/icons/logo.png");
		logo.setFitWidth(200);
		logo.setPreserveRatio(true);
		BorderPane logoWrapper = new BorderPane(logo);
		logoWrapper.getStyleClass().add("cataclysm-logo");

		Button discord = new IconButton(new MaterialDesignIconView(MaterialDesignIcon.DISCORD));
		discord.setOnAction(event -> PlatformHelper.browseURL("https://discord.gg/5UTs7R8"));

		Button vk = new IconButton(new MaterialDesignIconView(MaterialDesignIcon.VK));
		vk.setOnAction(event -> PlatformHelper.browseURL("https://vk.com/project_cataclysm"));

		Button youtube = new IconButton(new MaterialDesignIconView(MaterialDesignIcon.YOUTUBE_PLAY));
		youtube.setOnAction(event -> PlatformHelper.browseURL("https://www.youtube.com/channel/UC_gYULJ5ouaXCIxC7kKETjg"));

		HBox socials = new HBox(discord, vk, youtube);
		socials.getStyleClass().add("socials");

		VBox topWrapper = new VBox(logoWrapper, socials);
		topWrapper.getStyleClass().add("top-wrapper");

		setTop(topWrapper);

		patchNote = new LabelField("Патчноут от 04.10.2020\n\n- Исправлено че то там lorem ipsum dolor sit amet omegalul kekw\n- Добавлено че то там lorem ipsum dolor sit amet omegalul kekw\n- Вырезано че то там\n- Исправлено че то там\n- Добавлено че то там lorem ipsum dolor sit amet omegalul kekw\n- Вырезано че то там\n- Исправлено че то там\n- Добавлено че то там\n- Вырезано че то там\n- Исправлено че то там\n- Добавлено че то там lorem ipsum dolor sit amet omegalul kekw\n- Вырезано че то там\n- Исправлено че то там\n- Добавлено че то там\n- Вырезано че то там\n- Исправлено че то там\n- Добавлено че то там\n- Вырезано че то там\n- Исправлено че то там\n- Добавлено че то там\n- Вырезано че то там\n- Исправлено че то там\n- Добавлено че то там\n- Вырезано че то там\n- Исправлено че то там\n- Добавлено че то там\n- Вырезано че то там\n- Исправлено че то там\n- Добавлено че то там\n- Вырезано че то там\n- Исправлено че то там\n- Добавлено че то там\n- Вырезано че то там\n- Исправлено че то там\n- Добавлено че то там\n- Вырезано че то там\n- Исправлено че то там\n- Добавлено че то там\n- Вырезано че то там\n- Исправлено че то там\n- Добавлено че то там\n- Вырезано че то там\n- Исправлено че то там\n- Добавлено че то там\n- Вырезано че то там");
		patchNote.getStyleClass().add("patch-note");

		patchNoteLoadingIndicator = new ProgressIndicator();
		patchNoteLoadingIndicator.setMaxSize(32, 32);
		patchNoteWrapper = new StackPane();

		errorLabel = new LabelField();
		errorLabel.getStyleClass().add("error-modal-label");

		setCenter(patchNoteWrapper);

		versionLabel = new Label("N/A", new MaterialDesignIconView(MaterialDesignIcon.LABEL));
		versionLabel.getStyleClass().add("game-version");
		versionLabel.setMaxWidth(Double.MAX_VALUE);
		statusLabel = new Label("Оффлайн", new MaterialDesignIconView(MaterialDesignIcon.INFORMATION));
		statusLabel.setTextFill(Color.GRAY);
		statusLabel.getStyleClass().add("server-status");
		statusLabel.getStyleClass().add("online");
		statusLabel.getStyleClass().add("offline");
		statusLabel.getStyleClass().add("maintenance");
		onlinePlayers = new Label("N/A", new MaterialDesignIconView(MaterialDesignIcon.ACCOUNT));
		onlinePlayers.getStyleClass().add("server-player-count");

		HBox.setHgrow(versionLabel, Priority.ALWAYS);
		HBox infoWrapper = new HBox(versionLabel, statusLabel, onlinePlayers);
		infoWrapper.getStyleClass().add("info-wrapper");
		infoWrapper.setMaxHeight(USE_PREF_SIZE);
		setBottom(infoWrapper);

		setPrefWidth(320);
		setMaxWidth(USE_PREF_SIZE);
		setMinWidth(USE_PREF_SIZE);
	}

	public void startServerPingTimer() {
		Timer pingTimer = new Timer("Server Pinger", true);
		pingTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				try {
					String pingResultFile = HttpHelper.LAUNCHER_URL + "/server_ping.json";
					JSONObject response = HttpHelper.executeJsonObjectRequest(pingResultFile, null);
					Platform.runLater(() -> {
						versionLabel.setText((String) response.get("version"));
						onlinePlayers.setText(response.get("players").toString());

						String status = (String) response.get("status");
						if (status.equals("online")) {
							statusLabel.setText("Онлайн");
							statusLabel.setTextFill(Color.GREEN);
						} else if (status.equals("maintenance")) {
							statusLabel.setText("Тех. работы");
							statusLabel.setTextFill(Color.ORANGE);
						} else {
							statusLabel.setText("Оффлайн");
							statusLabel.setTextFill(Color.GRAY);
						}
					});
					response.get("version");
				} catch (IOException e) {
					Log.warn(e, "Failed to get server info");
				}
			}
		}, 0, 60000);
	}

	public void updatePatchNote(CompletableFuture<JSONObject> future) {
		Log.msg("Updating patch note...");
		patchNoteWrapper.getChildren().setAll(patchNoteLoadingIndicator);

		AsyncTasks.whenComplete(future, (response, exception) -> {
			Runnable fxTask;
			if (exception == null) {
				fxTask = () -> {
					patchNote.setText((String) response.get("patchNote"));
					patchNoteWrapper.getChildren().setAll(patchNote);
				};
			} else {
				fxTask = () -> {
					Log.warn(exception, "Failed to get patch-note");
					errorLabel.setText("Не удалось получить патчноут: \n\n" + exception.toString());
					patchNoteWrapper.getChildren().setAll(errorLabel);
				};
			}

			Platform.runLater(fxTask);
		});
	}
}
