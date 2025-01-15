package cataclysm.launcher.ui;

import cataclysm.launcher.LauncherApplication;
import cataclysm.launcher.account.Session;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.awt.*;

/**
 * <br><br>ProjectCataclysm
 * <br>Created: 07.08.2022 17:51
 *
 * @author Knoblul
 */
public class SessionInfoPane extends HBox {
	private final Label username;

	public SessionInfoPane(LauncherApplication application) {
		getStyleClass().add("account-info");
		username = new Label();
		username.getStyleClass().addAll("username", "bold-font");
		ImageView icon = new ImageView("/icons/logout.png");
		Button quitButton = new Button(null, icon);
		Tooltip.install(quitButton, new Tooltip("Выход из аккаунта"));
		icon.fitWidthProperty().bind(quitButton.widthProperty());
		icon.fitHeightProperty().bind(quitButton.heightProperty());
		icon.setPreserveRatio(true);
		quitButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
		quitButton.getStyleClass().add("quit-button");
		HoverAnimation.install(quitButton);

		quitButton.setOnAction(__ -> {
			Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
			alert.setTitle("Выход из аккаунта");
			alert.setHeaderText(null);
			alert.setContentText("Вы уверены, что хотите выйти из аккаунта?");
			alert.getButtonTypes().setAll(new ButtonType("Да", ButtonBar.ButtonData.YES), new ButtonType("Нет", ButtonBar.ButtonData.NO));
			StageUtils.setupIcons((Stage) alert.getDialogPane().getScene().getWindow());
			alert.showAndWait().ifPresent(value -> {
				if (value.getButtonData() == ButtonBar.ButtonData.YES) {
					application.getAccountManager().logout();
					init(null);
					application.getOverlays().getLogin().reset();
					application.getOverlays().getLogin().show();
				}
			});
		});

		getChildren().addAll(username, quitButton);
		AnchorPane.setLeftAnchor(this, 1.0);
		AnchorPane.setTopAnchor(this, 1.0);
		AnchorPane.setRightAnchor(this, 1.0);
		setVisible(false);
	}

	public void init(Session session) {
		if (session == null) {
			setVisible(false);
		} else {
			username.setText(session.getProfile().getUsername());
			setVisible(true);
		}
	}
}
