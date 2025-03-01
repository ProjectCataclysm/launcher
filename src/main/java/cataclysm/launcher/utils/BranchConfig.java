public class BranchConfig {
    private final String name;
    private final String displayName;
    private final String torrentUrl;
    private final String gamePath;
    private final String assetsUrl;

    public BranchConfig(String name, String displayName, String torrentUrl, String gamePath, String assetsUrl) {
        this.name = name;
        this.displayName = displayName;
        this.torrentUrl = torrentUrl;
        this.gamePath = gamePath;
        this.assetsUrl = assetsUrl;
    }

    // Геттеры
    public String getName() { return name; }
    public String getDisplayName() { return displayName; }
    public String getTorrentUrl() { return torrentUrl; }
    public String getGamePath() { return gamePath; }
    public String getAssetsUrl() { return assetsUrl; }
} 