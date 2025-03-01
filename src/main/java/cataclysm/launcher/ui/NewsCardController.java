public class NewsCardController {
    @FXML private ImageView newsImage;
    @FXML private Label dateLabel;
    @FXML private Label titleLabel;
    @FXML private Text contentText;
    @FXML private Hyperlink readMoreLink;

    public void setNewsItem(NewsItem news) {
        // Загружаем изображение асинхронно
        CompletableFuture.runAsync(() -> {
            try {
                Image image = new Image(news.getImageUrl(), true);
                Platform.runLater(() -> newsImage.setImage(image));
            } catch (Exception e) {
                Log.err(e, "Failed to load news image: " + news.getImageUrl());
            }
        });

        dateLabel.setText("[" + news.getDate() + "]");
        titleLabel.setText(news.getTitle());
        contentText.setText(news.getText());
        
        readMoreLink.setOnAction(event -> {
            try {
                PlatformHelper.openUrl(news.getLink());
            } catch (Exception e) {
                Log.err(e, "Failed to open news link: " + news.getLink());
            }
        });
    }
} 