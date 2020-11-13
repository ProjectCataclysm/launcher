package cataclysm.launcher.ui.forms.main.settings;

import cataclysm.launcher.ui.controls.CustomTextField;
import com.google.common.base.Strings;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextFormatter;
import javafx.util.StringConverter;

import java.util.Optional;

/**
 * <br><br>REVOM ENGINE / ProjectCataclysm
 * <br>Created: 06.10.2020 6:24
 *
 * @author Knoblul
 */
class LoadingSpeedField extends CustomTextField {
	final CheckBox checkBox;

	LoadingSpeedField() {
		getStyleClass().add("loading-speed-field");

		setPromptText("КБ/c");

		StringConverter<Integer> stringConverter = new StringConverter<Integer>() {
			@Override
			public Integer fromString(String string) {
				return Strings.isNullOrEmpty(string) ? null : Integer.parseInt(string);
			}

			@Override
			public String toString(Integer object) {
				return Optional.ofNullable(object).map(Object::toString).orElse("");
			}
		};
		setTextFormatter(new TextFormatter<>(stringConverter, null, this::formatField));

		checkBox = new CheckBox("Без огр.");
		disableProperty().bind(checkBox.selectedProperty());
	}

	private TextFormatter.Change formatField(TextFormatter.Change change) {
		return Strings.isNullOrEmpty(change.getText()) || change.getText().matches("\\d+") ? change : null;
	}
}
