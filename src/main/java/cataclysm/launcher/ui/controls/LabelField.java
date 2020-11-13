package cataclysm.launcher.ui.controls;

import com.google.common.base.Strings;
import javafx.css.PseudoClass;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;

/**
 * <br><br>REVOM ENGINE / ProjectCataclysm
 * <br>Created: 04.10.2020 3:06
 *
 * @author Knoblul
 */
public class LabelField extends TextArea {
	public LabelField(String text) {
		super(text);
		setEditable(false);
		setWrapText(true);
		getStyleClass().add("label-field");

		PseudoClass empty = PseudoClass.getPseudoClass("empty");
		pseudoClassStateChanged(empty, true);
		textProperty().addListener((obs, oldText, newText) -> pseudoClassStateChanged(empty, Strings.isNullOrEmpty(newText)));

		MenuItem copy = new MenuItem("Копировать");
		copy.setOnAction(event -> copy());

		MenuItem selectAll = new MenuItem("Выделить все");
		selectAll.setOnAction(event -> selectAll());

		ContextMenu contextMenu = new ContextMenu(copy, selectAll);
		contextMenu.setOnShown(event -> {
			copy.setDisable(getSelectedText().isEmpty());
			selectAll.setDisable(getSelectedText().equals(getText()));
		});
		setContextMenu(contextMenu);
	}

	public LabelField() {
		this(null);
	}
}
