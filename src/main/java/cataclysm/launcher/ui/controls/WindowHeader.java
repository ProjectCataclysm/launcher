package cataclysm.launcher.ui.controls;

import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIconView;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/**
 * <br><br>REVOM ENGINE / ProjectCataclysm
 * <br>Created: 06.10.2020 23:44
 *
 * @author Knoblul
 */
public class WindowHeader extends HBox {
	private double xOffset;
	private double yOffset;

	public WindowHeader(Stage stage, boolean applicationHeader) {
		Label title = new Label();
		if (applicationHeader) {
			title.setGraphic(new ImageView("/icons/icon_16.png"));
		}

		title.textProperty().bind(stage.titleProperty());
		title.setMaxWidth(Double.MAX_VALUE);
		title.getStyleClass().addAll("window-title");
		HBox.setHgrow(title, Priority.ALWAYS);
		getChildren().add(title);

		if (applicationHeader) {
			Button minimizeButton = new IconButton(new MaterialDesignIconView(MaterialDesignIcon.WINDOW_MINIMIZE));
			minimizeButton.setOnAction(event -> stage.setIconified(true));
			getChildren().add(minimizeButton);
		}

		Button closeButton = new IconButton(new MaterialDesignIconView(MaterialDesignIcon.WINDOW_CLOSE));
		closeButton.setOnAction(event -> stage.getOnCloseRequest()
				.handle(new WindowEvent(stage,	WindowEvent.WINDOW_CLOSE_REQUEST)));

		getChildren().add(closeButton);
		getStyleClass().add("window-controls");

		if (applicationHeader) {
			setOnMousePressed(event -> {
				xOffset = event.getSceneX();
				yOffset = event.getSceneY();
			});

			setOnMouseDragged(event -> {
				stage.setX(event.getScreenX() - xOffset);
				stage.setY(event.getScreenY() - yOffset);
			});
		}
	}
}
