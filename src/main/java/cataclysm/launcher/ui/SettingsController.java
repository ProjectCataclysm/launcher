public class SettingsController {
    @FXML
    private ComboBox<BranchConfig> branchComboBox;
    
    private BranchManager branchManager;
    
    @FXML
    private void initialize() {
        branchManager = new BranchManager(LauncherConfig.LAUNCHER_DIR_PATH);
        
        // Настраиваем ComboBox
        branchComboBox.setItems(FXCollections.observableArrayList(
            branchManager.getAvailableBranches()
        ));
        
        branchComboBox.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(BranchConfig branch, boolean empty) {
                super.updateItem(branch, empty);
                if (empty || branch == null) {
                    setText(null);
                } else {
                    setText(branch.getDisplayName());
                }
            }
        });
        
        branchComboBox.setValue(branchManager.getCurrentBranch());
    }
    
    @FXML
    private void onBranchChange() {
        BranchConfig selectedBranch = branchComboBox.getValue();
        if (selectedBranch == null) return;

        branchManager.switchBranch(selectedBranch.getName(), new BranchManager.BranchSwitchCallback() {
            @Override
            public void onRequireDownload() {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Требуется загрузка");
                    alert.setHeaderText("Необходима загрузка файлов игры");
                    alert.setContentText("Для выбранной ветки требуется загрузить файлы игры. Продолжить?");
                    
                    if (alert.showAndWait().orElse(null) == ButtonType.OK) {
                        startGameDownload();
                    } else {
                        // Возвращаем предыдущую ветку
                        branchComboBox.setValue(branchManager.getCurrentBranch());
                    }
                });
            }

            @Override
            public void onRequireUpdate(int modifiedFilesCount) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Требуется обновление");
                    alert.setHeaderText("Необходимо обновить файлы игры");
                    alert.setContentText("Требуется обновить " + modifiedFilesCount + " файлов. Продолжить?");
                    
                    if (alert.showAndWait().orElse(null) == ButtonType.OK) {
                        startGameUpdate();
                    } else {
                        branchComboBox.setValue(branchManager.getCurrentBranch());
                    }
                });
            }

            @Override
            public void onComplete() {
                Platform.runLater(() -> {
                    showMessage("Ветка успешно изменена", "Переключение на ветку " + 
                        selectedBranch.getDisplayName() + " выполнено успешно.");
                });
            }

            @Override
            public void onError(ErrorHandler error) {
                Platform.runLater(() -> {
                    showError(error);
                    branchComboBox.setValue(branchManager.getCurrentBranch());
                });
            }
        });
    }
} 