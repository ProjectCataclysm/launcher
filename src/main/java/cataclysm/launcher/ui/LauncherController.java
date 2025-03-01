private Label statusLabel;
private Button detailsButton;
private ErrorHandler lastError;
private GameDownloader gameDownloader;
private JreManager jreManager;

@FXML private VBox newsContainer;
private final NewsService newsService;

private void showError(ErrorHandler error) {
    lastError = error;
    statusLabel.setText(error.getShortMessage());
    detailsButton.setVisible(true);
}

@FXML
private void showErrorDetails() {
    if (lastError != null) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Подробности ошибки");
        alert.setHeaderText(lastError.getShortMessage());
        
        TextArea textArea = new TextArea(lastError.getFullStackTrace());
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        
        alert.getDialogPane().setExpandableContent(new VBox(textArea));
        alert.showAndWait();
    }
}

private void resetError() {
    lastError = null;
    detailsButton.setVisible(false);
}

@Override
public void onError(ErrorHandler error) {
    Platform.runLater(() -> showError(error));
}

@FXML
private void initialize() {
    gameDownloader = new GameDownloader(
        LauncherConfig.GAME_DIR_PATH,
        LauncherConfig.TORRENT_URL
    );
    
    jreManager = new JreManager(
        LauncherConfig.LAUNCHER_DIR_PATH,
        new DownloadProgressListener() {
            @Override
            public void onStatusChange(String status) {
                Platform.runLater(() -> updateStatus(status));
            }

            @Override
            public void onProgress(double progress) {
                Platform.runLater(() -> updateProgress(progress));
            }

            @Override
            public void onError(ErrorHandler error) {
                Platform.runLater(() -> showError(error));
            }
        }
    );

    loadNews();
}

private void loadNews() {
    newsContainer.getChildren().clear();
    
    CompletableFuture.supplyAsync(() -> newsService.getNews())
        .thenAccept(newsList -> Platform.runLater(() -> {
            for (NewsItem news : newsList) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/NewsCard.fxml"));
                    Node newsCard = loader.load();
                    NewsCardController controller = loader.getController();
                    controller.setNewsItem(news);
                    newsContainer.getChildren().add(newsCard);
                } catch (IOException e) {
                    Log.err(e, "Failed to load news card");
                }
            }
        }))
        .exceptionally(throwable -> {
            Platform.runLater(() -> showError(new ErrorHandler(
                "Ошибка загрузки новостей",
                "Не удалось загрузить новости",
                throwable
            )));
            return null;
        });
}

private void startGameDownload() {
    gameDownloader.startDownload(new DownloadProgressListener() {
        @Override
        public void onProgress(double progress) {
            Platform.runLater(() -> updateProgress(progress));
        }

        @Override
        public void onError(ErrorHandler error) {
            Platform.runLater(() -> showError(error));
        }

        @Override
        public void onComplete() {
            Platform.runLater(() -> downloadComplete());
        }
    });
}

private void stopGameDownload() {
    gameDownloader.stopDownload();
}

private void startGame() {
    // Проверяем наличие JRE перед запуском
    jreManager.ensureJreInstalled()
        .thenAccept(jrePath -> {
            // Используем установленную JRE для запуска игры
            GameLauncher.launch(jrePath, ...);
        })
        .exceptionally(throwable -> {
            Platform.runLater(() -> showError(new ErrorHandler(
                "Ошибка подготовки Java Runtime",
                "Не удалось установить Java Runtime Environment",
                throwable
            )));
            return null;
        });
} 