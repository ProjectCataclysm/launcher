package cataclysm.launcher.ui;

import cataclysm.launcher.LauncherApplication;
import cataclysm.launcher.account.Session;
import cataclysm.launcher.utils.HttpClientWrapper;
import cataclysm.launcher.utils.LauncherConfig;
import cataclysm.launcher.utils.PlatformHelper;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import proguard.annotation.Keep;

import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Function;

/**
 * <br><br>ProjectCataclysm
 * <br>Created: 06.08.2022 15:27
 *
 * @author Knoblul
 */
public class SettingsDialog extends VBox {
	private ComboBox<LauncherConfig.ClientBranch> clientBranchComboBox;

	private SettingsDialog(LauncherApplication application) {
		LauncherConfig config = application.getConfig();

		getStyleClass().add("settings");

		GridPane gp = new GridPane();
		gp.getStyleClass().addAll("options", "spaced-wrapper");
		gp.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
		ColumnConstraints mainConstraint = new ColumnConstraints();
		mainConstraint.setHgrow(Priority.ALWAYS);
		mainConstraint.setMaxWidth(250);
		gp.getColumnConstraints().add(0, mainConstraint);
		ColumnConstraints secondaryConstraint = new ColumnConstraints();
		secondaryConstraint.setFillWidth(true);
		gp.getColumnConstraints().add(1, secondaryConstraint);

		gamePathOption(config, gp);
		memoryOption(config, gp);
		clientBranchOption(application, gp);

		VBox.setVgrow(gp, Priority.ALWAYS);

		getChildren().add(gp);

		Button okButton = new Button("Сохранить");
		okButton.getStyleClass().add("ok-button");
		okButton.setOnMouseClicked(event -> ((Stage) getScene().getWindow()).close());
		BorderPane bp2 = new BorderPane();
		bp2.getStyleClass().add("controls");
		bp2.setLeft(createReportButton());
		bp2.setRight(okButton);
		getChildren().add(bp2);
	}

	private Button createReportButton() {
		Button button = new Button("Создать отчет");
		Tooltip t = new Tooltip("Создает архив, в который складывает всю необходимую информацию для "
			+ "диагностики и исправления ошибок. Папка, содержащая созданный архив будет открыта. "
			+ "Данный архив вы можете отправить нам, чтобы мы могли решить возникшую у вас проблему.");
		t.setWrapText(true);
		t.setMaxWidth(300);
		Tooltip.install(button, t);
		button.setOnAction(__ -> DialogUtils.createReportArchive(0));
		return button;
	}

	private void clientBranchOption(LauncherApplication application, GridPane gp) {
		Set<LauncherConfig.ClientBranch> accessibleBranches = Sets.newHashSet();
		Set<String> tickets = Optional.ofNullable(application.getAccountManager().getSession())
			.map(Session::getTickets)
			.orElse(Collections.emptySet());

		accessibleBranches.add(LauncherConfig.ClientBranch.PRODUCTION);
		if (tickets.contains("e:test")) {
			accessibleBranches.add(LauncherConfig.ClientBranch.TEST);
		}

		Label label = new Label("Ветка обновлений");
		gp.add(label, 0, 2);

		Function<LauncherConfig.ClientBranch, LauncherConfig.ClientBranch> convFun =
			b -> accessibleBranches.contains(b) ? b : LauncherConfig.ClientBranch.PRODUCTION;

		ObservableList<LauncherConfig.ClientBranch> optionList = FXCollections.observableArrayList(accessibleBranches);
		optionList.sort(Comparator.comparingInt(Enum::ordinal));
		clientBranchComboBox = new ComboBox<>(optionList);
		clientBranchComboBox.setMaxWidth(Double.MAX_VALUE);
		clientBranchComboBox.setCellFactory(param -> new ListCell<LauncherConfig.ClientBranch>() {
			@Override
			protected void updateItem(LauncherConfig.ClientBranch item, boolean empty) {
				super.updateItem(item, empty);

				if (item == null) {
					item = LauncherConfig.ClientBranch.PRODUCTION;
				}

				setText(item.getTitle());
			}
		});
		clientBranchComboBox.setButtonCell(clientBranchComboBox.getCellFactory().call(null));
		clientBranchComboBox.getSelectionModel().select(convFun.apply(application.getConfig().clientBranch));
		clientBranchComboBox.setDisable(accessibleBranches.size() == 1);
		clientBranchComboBox.setOnAction(event -> application.getConfig().clientBranch
			= convFun.apply(clientBranchComboBox.getSelectionModel().getSelectedItem()));

		gp.add(clientBranchComboBox, 1, 2);
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
		Stage stage = new Stage();
		stage.setResizable(false);
		stage.setOnHiding(event -> application.getConfig().save());
		stage.setScene(StageUtils.createScene(new SettingsDialog(application), 500, 400));
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
