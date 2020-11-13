package cataclysm.launcher.ui.forms.main.settings;

import cataclysm.launcher.ui.Launcher;
import cataclysm.launcher.ui.controls.CustomTextField;
import cataclysm.launcher.ui.controls.IconButton;
import cataclysm.launcher.ui.controls.LargeButton;
import cataclysm.launcher.utils.PlatformHelper;
import cataclysm.launcher.utils.Settings;
import com.google.common.base.Strings;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIconView;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.DirectoryChooser;
import javafx.util.StringConverter;

import java.io.File;

/**
 * <br><br>REVOM ENGINE / ProjectCataclysm
 * <br>Created: 04.10.2020 4:32
 *
 * @author Knoblul
 */
public class SettingsPane extends BorderPane {
	private final Button backButton;

	public SettingsPane(Launcher launcher, Settings settings) {
		getStyleClass().addAll("padded-pane", "settings-pane");

		Label titleLabel = new Label("Настройки");
		titleLabel.getStyleClass().add("title");
		titleLabel.setMaxWidth(Double.MAX_VALUE);
		setTop(titleLabel);

		MemorySlider memorySlider = new MemorySlider();
		memorySlider.setMin(512);
		memorySlider.setMax(PlatformHelper.getMaximumMemoryMB());
		memorySlider.setValue(settings.allocatedMemory);
		memorySlider.valueProperty().addListener((observable, oldValue, newValue) -> {
			settings.allocatedMemory = (int) memorySlider.getValue();
			settings.save();
		});
		Tooltip.install(memorySlider, new Tooltip("Сколько будет выделено памяти игре."));

		LoadingSpeedField loadingSpeed = new LoadingSpeedField();
		loadingSpeed.textProperty().addListener((observable, oldValue, newValue) -> {
			settings.downloadSpeedLimit = Strings.isNullOrEmpty(loadingSpeed.getText()) ? 0
					: Integer.parseInt(loadingSpeed.getText());
			settings.save();
		});

		loadingSpeed.setText(settings.downloadSpeedLimit == 0 ? "" : Integer.toString(settings.downloadSpeedLimit));
		loadingSpeed.checkBox.setSelected(settings.downloadSpeedLimit == 0);
		loadingSpeed.checkBox.setOnAction(event -> {
			settings.downloadSpeedLimit = loadingSpeed.checkBox.isSelected() || Strings.isNullOrEmpty(loadingSpeed.getText()) ?
					0 : Integer.parseInt(loadingSpeed.getText());
			settings.save();
		});
		Tooltip.install(loadingSpeed, new Tooltip("Ограничивает скорость скачивания обновлений игры."));

		CheckBox trayOnClose = new CheckBox();
		trayOnClose.setSelected(settings.hideToTrayOnClose);
		trayOnClose.setOnAction(event -> {
			settings.hideToTrayOnClose = trayOnClose.isSelected();
			settings.save();
		});
		Tooltip.install(trayOnClose, new Tooltip("Будет ли лаунчер сворачиваться область уведомлений при закрытии на крестик."));

		ComboBox<Settings.LaunchVisibilityAction> launchVisibilityAction = new ComboBox<>(FXCollections
				.observableArrayList(Settings.LaunchVisibilityAction.values()));
		launchVisibilityAction.setConverter(new StringConverter<Settings.LaunchVisibilityAction>() {
			@Override
			public String toString(Settings.LaunchVisibilityAction object) {
				return object.getName();
			}

			@Override
			public Settings.LaunchVisibilityAction fromString(String string) {
				return Settings.LaunchVisibilityAction.find(string);
			}
		});
		launchVisibilityAction.setCellFactory(view -> new ListCell<Settings.LaunchVisibilityAction>() {
			@Override
			protected void updateItem(Settings.LaunchVisibilityAction item, boolean empty) {
				super.updateItem(item, empty);
				if (item != null && !empty) {
					setText(item.getName());
				}
			}
		});
		launchVisibilityAction.setValue(settings.launchVisibilityAction);
		launchVisibilityAction.setOnAction(event -> {
			settings.launchVisibilityAction = launchVisibilityAction.getValue();
			settings.save();
		});
		Tooltip.install(launchVisibilityAction, new Tooltip("Как будет вести себя лаунчер после запуска игры."));

		ComboBox<Settings.ReporterPolicy> reporterPolicy = new ComboBox<>(FXCollections
				.observableArrayList(Settings.ReporterPolicy.values()));
		reporterPolicy.setConverter(new StringConverter<Settings.ReporterPolicy>() {
			@Override
			public String toString(Settings.ReporterPolicy object) {
				return object.getName();
			}

			@Override
			public Settings.ReporterPolicy fromString(String string) {
				return Settings.ReporterPolicy.find(string);
			}
		});
		reporterPolicy.setCellFactory(view -> new ListCell<Settings.ReporterPolicy>() {
			@Override
			protected void updateItem(Settings.ReporterPolicy item, boolean empty) {
				super.updateItem(item, empty);
				if (item != null && !empty) {
					setText(item.getName());
				}
			}
		});
		reporterPolicy.setValue(settings.reporterPolicy);
		reporterPolicy.setOnAction(event -> {
			settings.reporterPolicy = reporterPolicy.getValue();
			settings.save();
		});
		Tooltip.install(reporterPolicy, new Tooltip("Отправляя краш-репорты Вы помогаете нам решать различные проблемы с игрой."));

		GridPane settingsOptions = new GridPane();
		settingsOptions.getStyleClass().add("settings-options");

		Label label = new Label("Выделяемая оперативная память (МБ):");
		label.getStyleClass().add("setting-name");
		settingsOptions.add(label, 0, 0);
		settingsOptions.add(memorySlider, 1, 0);
		settingsOptions.add(memorySlider.sliderField, 2,0);

		label = new Label("Ограничение скорости загрузки:");
		label.getStyleClass().add("setting-name");
		settingsOptions.add(label, 0, 1);
		settingsOptions.add(loadingSpeed, 1, 1);
		settingsOptions.add(loadingSpeed.checkBox, 2, 1);

		label = new Label("Сворачивать лаунчер в область уведомлений при нажатии «Закрыть»:");
		label.getStyleClass().add("setting-name");
		settingsOptions.add(label, 0, 2);
		settingsOptions.add(trayOnClose, 1, 2, 2, 1);

		label = new Label("Действие после запуска игры:");
		label.getStyleClass().add("setting-name");
		settingsOptions.add(label, 0, 3);
		settingsOptions.add(launchVisibilityAction, 1, 3, 2, 1);

		label = new Label("Политика отправки сообщений об ошибках:");
		label.getStyleClass().add("setting-name");
		settingsOptions.add(label, 0, 4);
		settingsOptions.add(reporterPolicy, 1, 4, 2, 1);

		if (PlatformHelper.getOS() != PlatformHelper.PlatformOS.WINDOWS) {
			DirectoryChooser gameDirChooser = new DirectoryChooser();

			CustomTextField textField = new CustomTextField();
			textField.setEditable(false);
			textField.setText(settings.gameDirPath.toAbsolutePath().normalize().toString());

			LargeButton browse = new LargeButton("Выбор...");
			browse.getStyleClass().add("browse-game-dir-button");
			browse.setOnAction(event -> {
				gameDirChooser.setInitialDirectory(settings.gameDirPath.toAbsolutePath().toFile());
				File selectedGameDir = gameDirChooser.showDialog(launcher.getPrimaryStage());
				if (selectedGameDir != null && !selectedGameDir.toPath().equals(settings.gameDirPath)) {
					settings.gameDirPath = selectedGameDir.toPath();
					textField.setText(settings.gameDirPath.toAbsolutePath().normalize().toString());
					settings.save();
				}
			});

			label = new Label("Папка с игрой:");
			label.getStyleClass().add("setting-name");
			settingsOptions.add(label, 0, 5);
			settingsOptions.add(textField, 1, 5);
			settingsOptions.add(browse, 2, 5);
		}

		ColumnConstraints cc = new ColumnConstraints();
		cc.setHgrow(Priority.ALWAYS);
		settingsOptions.getColumnConstraints().addAll(new ColumnConstraints(), cc);
		ScrollPane scrollPane = new ScrollPane(settingsOptions);
		scrollPane.setFitToWidth(true);
		scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
		setCenter(scrollPane);

		backButton = new IconButton(new MaterialDesignIconView(MaterialDesignIcon.CHEVRON_RIGHT));
		backButton.getStyleClass().add("back-button");
		Tooltip.install(backButton, new Tooltip("Назад к новостям"));
		setRight(new BorderPane(backButton));
	}

	public Button getBackButton() {
		return backButton;
	}
}
