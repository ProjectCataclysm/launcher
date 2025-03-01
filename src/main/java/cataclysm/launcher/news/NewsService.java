public class NewsService {
    private static final String NEWS_URL = "https://project-cataclysm.ru/api/news";

    public List<NewsItem> getNews() throws IOException {
        try (HttpClientWrapper.HttpResponse response = HttpClientWrapper.get(NEWS_URL)) {
            JSONArray newsArray = (JSONArray) new JSONParser().parse(response.getBody().charStream());
            List<NewsItem> news = new ArrayList<>();
            
            for (Object obj : newsArray) {
                JSONObject newsObj = (JSONObject) obj;
                news.add(new NewsItem(
                    (String) newsObj.get("title"),
                    formatDate((String) newsObj.get("date")),
                    (String) newsObj.get("text"),
                    (String) newsObj.get("image"),
                    (String) newsObj.get("link")
                ));
            }
            
            return news;
        } catch (ParseException e) {
            throw new IOException("Failed to parse news JSON", e);
        }
    }

    private String formatDate(String isoDate) {
        try {
            LocalDate date = LocalDate.parse(isoDate);
            return date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        } catch (Exception e) {
            return isoDate;
        }
    }
} 