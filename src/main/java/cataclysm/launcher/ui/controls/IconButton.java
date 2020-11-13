package cataclysm.launcher.ui.controls;

import javafx.animation.FadeTransition;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.util.Duration;

import java.util.Optional;

/**
 * <br><br>REVOM ENGINE / ProjectCataclysm
 * <br>Created: 08.10.2020 8:25
 *
 * @author Knoblul
 */
public class IconButton extends Button {
	private final FadeTransition fade;

	public IconButton(String text, Node graphic) {
		super(text, graphic);
		getStyleClass().add("icon-button");

		fade = new FadeTransition(Duration.millis(300), this);
		fade.setFromValue(0.65);
		fade.setToValue(1);

		setOnMouseEntered(event -> updateState(true));
		setOnMouseExited(event -> updateState(false));
		graphicProperty().addListener((observable, oldValue, newValue) -> Optional.ofNullable(newValue)
				.ifPresent(g -> g.opacityProperty().bind(opacityProperty())));
		updateState(false);
	}

	public IconButton(String text) {
		this(text, null);
	}

	public IconButton(Node graphic) {
		this(null, graphic);
	}

	public IconButton() {
		this(null,null);
	}

	protected void updateState(boolean active) {
		fade.setRate(active ? 1 : -1);
		fade.play();
	}
}
