package cataclysm.launcher.ui.controls;

import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.util.Duration;

/**
 * <br><br>REVOM ENGINE / ProjectCataclysm
 * <br>Created: 08.10.2020 8:23
 *
 * @author Knoblul
 */
public class LargeButton extends Button {
	private final ChangeListener<String> upperCaseChangeListener;
	private final ScaleTransition scale;
	private boolean active;

	public LargeButton(String text, Node graphic) {
		super(text, graphic);
		scale = new ScaleTransition(Duration.millis(300), this);
		scale.setInterpolator(Interpolator.EASE_BOTH);
		scale.setFromX(1);
		scale.setFromY(1);
		scale.setToX(1.05);
		scale.setToY(1.05);

		hoverProperty().addListener((observable, oldValue, newValue) -> updateState(newValue));
		pressedProperty().addListener((observable, oldValue, newValue) -> updateState(false));
		textProperty().addListener(upperCaseChangeListener = (observable, oldValue, newValue) -> updateButtonText());
		updateButtonText();
	}

	public LargeButton(String text) {
		this(text, null);
	}

	public LargeButton() {
		this(null);
	}

	private void updateButtonText() {
		textProperty().removeListener(upperCaseChangeListener);
		if (getText() != null) {
			setText(getText().toUpperCase());
		}
		textProperty().addListener(upperCaseChangeListener);
	}

	private void updateState(boolean active) {
		if (this.active != active) {
			this.active = active;
			scale.setRate(active ? 1 : -1);
			scale.play();
		}
	}
}
