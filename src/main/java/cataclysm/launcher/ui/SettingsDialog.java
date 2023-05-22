package cataclysm.launcher.ui;

import cataclysm.launcher.LauncherApplication;
import cataclysm.launcher.utils.HttpClientWrapper;
import cataclysm.launcher.utils.LauncherConfig;
import cataclysm.launcher.utils.PlatformHelper;
import com.google.common.collect.Lists;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import proguard.annotation.Keep;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * <br><br>ProjectCataclysm
 * <br>Created: 06.08.2022 15:27
 *
 * @author Knoblul
 */
public class SettingsDialog extends VBox {
	private SettingsDialog(LauncherConfig config) {
		getStyleClass().add("settings");

		GridPane gp = new GridPane();
		gp.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		ColumnConstraints mainConstraint = new ColumnConstraints();
		mainConstraint.setHgrow(Priority.ALWAYS);
		mainConstraint.setMaxWidth(250);
		gp.getColumnConstraints().add(0, mainConstraint);
		ColumnConstraints secondaryConstraint = new ColumnConstraints();
		secondaryConstraint.setFillWidth(true);
		gp.getColumnConstraints().add(1, secondaryConstraint);
		gp.getStyleClass().addAll("options", "spaced-wrapper");

		gamePathOption(config, gp);
		memoryOption(config, gp);
		forwardCompatRender(config, gp);
		createReportButton(gp);

		VBox.setVgrow(gp, Priority.ALWAYS);

		getChildren().add(gp);

		Button okButton = new Button("Сохранить");
		okButton.getStyleClass().add("ok-button");
		okButton.setOnMouseClicked(event -> ((Stage) getScene().getWindow()).close());
		HBox e = new HBox(okButton);
		e.getStyleClass().add("controls");
		getChildren().add(e);
	}

	private void createReportButton(GridPane gp) {
		Button button = new Button("Создать отчет");
		Tooltip t = new Tooltip("Создает архив, в который складывает всю необходимую информацию для "
				+ "диагностики и исправления ошибок. Папка, содержащая созданный архив будет открыта. "
				+ "Данный архив вы можете отправить нам, чтобы мы могли решить возникшую у вас проблему.");
		t.setWrapText(true);
		t.setMaxWidth(300);
		Tooltip.install(button, t);
		button.setOnAction(__ -> DialogUtils.createReportArchive(0));
		gp.add(button, 0, 4);
	}

	private void forwardCompatRender(LauncherConfig config, GridPane gp) {
		Label label = new Label("Разрешить использование OpenGL Core Profile");
		label.setWrapText(true);
		gp.add(label, 0, 3);

		CheckBox checkBox = new CheckBox();
		checkBox.setSelected(config.forwardCompatRender);
		checkBox.setOnMouseClicked(event -> config.forwardCompatRender = checkBox.isSelected());
		gp.add(checkBox, 1, 3);
	}

	private void memoryOption(LauncherConfig config, GridPane gp) {
		Label label = new Label("Ограничение памяти");
		gp.add(label, 0, 1);

		List<Integer> values = Lists.newArrayList();
		values.add(0);

		int maxMemory = PlatformHelper.getMaxMemory();
		int selectedValue = config.limitMemoryMegabytes;
		int selectedIndex = 0;

		// степени двойки, начиная от 1024, заканчивая 32768
		for (int i = 10; i <= 15; i++) {
			int value = 1 << i;
			if (value > maxMemory) {
				break;
			}

			values.add(value);

			if (value == selectedValue) {
				selectedIndex = i - 9;
			}
		}

		ComboBox<Integer> memoryCombo = new ComboBox<>(FXCollections.observableArrayList(values));
		memoryCombo.setMaxWidth(Double.MAX_VALUE);
		memoryCombo.setCellFactory(param -> new MemoryListCell());
		memoryCombo.setButtonCell(new MemoryListCell());
		memoryCombo.getSelectionModel().select(selectedIndex);
		memoryCombo.setOnAction(event -> config.limitMemoryMegabytes = Objects.requireNonNull(
				memoryCombo.getSelectionModel().getSelectedItem()));

		gp.add(memoryCombo, 1, 1);
	}

	private void gamePathOption(LauncherConfig config, GridPane gp) {
		Label title = new Label("Путь до папки с игрой");
		TextField pathField = new TextField();
		pathField.setFocusTraversable(false);
		pathField.setText(config.gameDirectoryPath.toString());
		pathField.setEditable(false);
		pathField.setMaxWidth(Double.MAX_VALUE);

		VBox innerGP = new VBox(title, pathField);
		innerGP.getStyleClass().addAll("spaced-wrapper", "game-path-wrapper");
		gp.add(innerGP, 0, 0);

		DirectoryChooser pathChooser = new DirectoryChooser();
		pathChooser.setTitle("Выбор папки с игрой");
		if (Files.isDirectory(config.gameDirectoryPath)) {
			pathChooser.setInitialDirectory(config.gameDirectoryPath.toFile());
		}
		Button browseButton = new Button("Выбрать...");
		browseButton.setOnMouseClicked(event -> {
			File file = pathChooser.showDialog(getScene().getWindow());
			if (file != null) {
				pathField.setText(file.getAbsolutePath());
				config.gameDirectoryPath = file.toPath();
			}
		});
		Button openButton = new Button("Открыть");
		openButton.setOnMouseClicked(event -> HttpClientWrapper.browse(config.gameDirectoryPath.toUri()));
		HBox box = new HBox(browseButton, openButton);
		box.setMaxWidth(Double.MAX_VALUE);
		box.getStyleClass().addAll("spaced-wrapper", "game-path-buttons");
		gp.add(box, 1, 0);
	}

	public static void show(LauncherApplication application) {
		LauncherConfig config = application.getConfig();

		Stage stage = new Stage();
		stage.setResizable(false);
		stage.setOnHiding(event -> config.save());
		stage.setScene(StageUtils.createScene(new SettingsDialog(config), 500, 400));
		StageUtils.setupIcons(stage);
		stage.setTitle("Настройки");
		stage.initModality(Modality.WINDOW_MODAL);
		stage.initOwner(application.getPrimaryStage());
		stage.show();
	}

	private static final class MemoryListCell extends ListCell<Integer> {
		@Keep
		@Override
		protected void updateItem(Integer item, boolean empty) {
			super.updateItem(item, empty);
			if (item != null) {
				if (item == 0) {
					setText("<по умолчанию>");
				} else {
					setText(String.format(Locale.ROOT, "%dMB (%.1fGB)", item, item / 1024.0));
				}
			}
		}
	}
}
