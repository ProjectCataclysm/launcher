public class NewsItem {
    private final String title;
    private final String date;
    private final String text;
    private final String imageUrl;
    private final String link;

    public NewsItem(String title, String date, String text, String imageUrl, String link) {
        this.title = title;
        this.date = date;
        this.text = text;
        this.imageUrl = imageUrl;
        this.link = link;
    }

    // Геттеры
    public String getTitle() { return title; }
    public String getDate() { return date; }
    public String getText() { return text; }
    public String getImageUrl() { return imageUrl; }
    public String getLink() { return link; }
} 