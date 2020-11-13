package cataclysm.launcher.ui.forms.main.settings;

import cataclysm.launcher.ui.controls.CustomTextField;
import javafx.scene.Node;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;

/**
 * <br><br>REVOM ENGINE / ProjectCataclysm
 * <br>Created: 04.10.2020 6:16
 *
 * @author Knoblul
 */
class MemorySlider extends Slider {
	final TextField sliderField;

	private final StackPane trackAlt;
	private final StackPane progress;

	MemorySlider() {
		getStyleClass().add("memory-slider");

		trackAlt = new StackPane();
		trackAlt.getStyleClass().add("track-alt");
		trackAlt.setManaged(false);

		progress = new StackPane();
		progress.getStyleClass().add("progress");
		progress.setManaged(false);

		setMaxWidth(Double.MAX_VALUE);
		setShowTickLabels(true);
		setShowTickMarks(true);
		setMajorTickUnit(256);

		sliderField = new CustomTextField();
		sliderField.setPrefWidth(50);
		valueProperty().addListener((observable, oldValue, newValue) -> updateField());
		sliderField.focusedProperty().addListener((observable, oldValue, newValue) -> {
			try {
				setValue(Double.parseDouble(sliderField.getText()));
			} catch (Exception ignored) {

			}
			updateField();
		});
	}

	private void updateField() {
		sliderField.setText(String.valueOf(getMemoryNumber()));
	}

	@Override
	protected void layoutChildren() {
		super.layoutChildren();

		Node track = lookup(".track");
		Node thumb = lookup(".thumb");
		if (track instanceof StackPane && thumb instanceof StackPane) {
			StackPane thumbPane = (StackPane) thumb;
			StackPane sliderPane = (StackPane) track;
			if (!sliderPane.getChildren().contains(progress) ||
					!sliderPane.getChildren().contains(trackAlt)) {
				sliderPane.getChildren().setAll(trackAlt, progress);
			}

			double x = thumbPane.getWidth()*0.5;
			double y = 0;
			double w = sliderPane.getWidth()-thumbPane.getWidth();
			double h = sliderPane.getHeight();
			double t = (getValue() - getMin()) / (getMax() - getMin());
			trackAlt.resizeRelocate(x, y, w, h);
			progress.resizeRelocate(x, y, w*t, h);
		}
	}

	public int getMemoryNumber() {
		return (int) getValue();
	}
}
