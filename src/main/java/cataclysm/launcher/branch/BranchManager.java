public class BranchManager {
    private static final String BRANCH_CONFIG_FILE = "branch.cfg";
    private final Map<String, BranchConfig> branches;
    private BranchConfig currentBranch;
    private final Path configPath;
    private final GameDownloader gameDownloader;
    private final DownloadingManager downloadingManager;

    public BranchManager(Path launcherDir) {
        this.configPath = launcherDir.resolve(BRANCH_CONFIG_FILE);
        this.branches = new HashMap<>();
        
        // Инициализация доступных веток
        initializeBranches();
        
        // Загрузка сохраненной ветки или использование production по умолчанию
        loadSavedBranch();
        
        this.gameDownloader = new GameDownloader(
            Paths.get(currentBranch.getGamePath()),
            currentBranch.getTorrentUrl()
        );
        
        this.downloadingManager = new DownloadingManager();
        this.downloadingManager.setClientBranch(LauncherConfig.ClientBranch.valueOf(currentBranch.getName()));
    }

    private void initializeBranches() {
        // Добавляем production ветку
        branches.put("PRODUCTION", new BranchConfig(
            "PRODUCTION",
            "Релиз",
            "https://project-cataclysm.ru/production/game.torrent",
            "game/production",
            "https://project-cataclysm.ru/production/assets"
        ));

        // Добавляем development ветку
        branches.put("DEVELOPMENT", new BranchConfig(
            "DEVELOPMENT",
            "Разработка",
            "https://project-cataclysm.ru/development/game.torrent",
            "game/development",
            "https://project-cataclysm.ru/development/assets"
        ));
    }

    private void loadSavedBranch() {
        try {
            if (Files.exists(configPath)) {
                String branchName = Files.readString(configPath).trim();
                currentBranch = branches.getOrDefault(branchName, branches.get("PRODUCTION"));
            } else {
                currentBranch = branches.get("PRODUCTION");
            }
        } catch (IOException e) {
            Log.err(e, "Failed to load branch config");
            currentBranch = branches.get("PRODUCTION");
        }
    }

    public void switchBranch(String branchName, BranchSwitchCallback callback) {
        BranchConfig newBranch = branches.get(branchName);
        if (newBranch == null || newBranch == currentBranch) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                // Останавливаем текущие загрузки
                gameDownloader.stopDownload();
                
                // Сохраняем выбор ветки
                Files.writeString(configPath, branchName);
                
                // Обновляем текущую ветку
                currentBranch = newBranch;
                
                // Обновляем настройки загрузчиков
                gameDownloader.updateConfig(
                    Paths.get(currentBranch.getGamePath()),
                    currentBranch.getTorrentUrl()
                );
                
                downloadingManager.setClientBranch(
                    LauncherConfig.ClientBranch.valueOf(currentBranch.getName())
                );

                // Проверяем наличие файлов новой ветки
                checkBranchFiles(callback);
                
            } catch (Exception e) {
                callback.onError(new ErrorHandler(
                    "Ошибка при переключении ветки",
                    "Не удалось переключиться на ветку " + newBranch.getDisplayName(),
                    e
                ));
            }
        });
    }

    private void checkBranchFiles(BranchSwitchCallback callback) throws Exception {
        // Проверяем наличие файлов игры
        Path gamePath = Paths.get(currentBranch.getGamePath());
        if (!Files.exists(gamePath) || Files.list(gamePath).count() == 0) {
            callback.onRequireDownload();
        } else {
            // Проверяем актуальность файлов
            AssetInfoContainer assets = downloadingManager.loadAssetContainer();
            new SanitationManager()
                .sanitize(assets, Executors.newFixedThreadPool(4), gamePath)
                .thenAccept(modifiedAssets -> {
                    if (!modifiedAssets.isEmpty()) {
                        callback.onRequireUpdate(modifiedAssets.size());
                    } else {
                        callback.onComplete();
                    }
                });
        }
    }

    public BranchConfig getCurrentBranch() {
        return currentBranch;
    }

    public List<BranchConfig> getAvailableBranches() {
        return new ArrayList<>(branches.values());
    }

    public interface BranchSwitchCallback {
        void onRequireDownload();
        void onRequireUpdate(int modifiedFilesCount);
        void onComplete();
        void onError(ErrorHandler error);
    }
} 