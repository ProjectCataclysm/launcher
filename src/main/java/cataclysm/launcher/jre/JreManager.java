public class JreManager {
    private static final String JRE_ASSET_PREFIX = "jre8";
    private final Path launcherDir;
    private final DownloadingManager downloadManager;
    private final DownloadProgressListener progressListener;

    public JreManager(Path launcherDir, DownloadProgressListener progressListener) {
        this.launcherDir = launcherDir;
        this.downloadManager = new DownloadingManager();
        this.progressListener = progressListener;
    }

    public CompletableFuture<Path> ensureJreInstalled() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Path jrePath = launcherDir.resolve("java");
                
                // Проверяем наличие JRE
                if (isJreInstalled(jrePath)) {
                    return jrePath;
                }

                // Загружаем информацию об ассетах
                AssetInfoContainer assets = downloadManager.loadAssetContainer();
                
                // Ищем JRE в ассетах
                AssetInfo jreAsset = findJreAsset(assets);
                if (jreAsset == null) {
                    throw new RuntimeException("JRE asset not found in assets.json");
                }

                // Скачиваем и распаковываем JRE
                downloadAndExtractJre(jreAsset, jrePath);

                return jrePath;
            } catch (Exception e) {
                throw new CompletionException("Failed to install JRE", e);
            }
        });
    }

    private boolean isJreInstalled(Path jrePath) {
        Path javaExecutable = jrePath.resolve(PlatformHelper.isWindows() ? 
            "bin/java.exe" : "bin/java");
        return Files.isExecutable(javaExecutable);
    }

    private AssetInfo findJreAsset(AssetInfoContainer assets) {
        String platformSuffix = getPlatformJreSuffix();
        return assets.getAssets().stream()
                .filter(asset -> asset.getLocal().startsWith(JRE_ASSET_PREFIX) &&
                               asset.getLocal().contains(platformSuffix))
                .findFirst()
                .orElse(null);
    }

    private String getPlatformJreSuffix() {
        if (PlatformHelper.isWindows()) {
            return "windows";
        } else if (PlatformHelper.isMac()) {
            return "macos";
        } else if (PlatformHelper.isLinux()) {
            return "linux";
        }
        throw new RuntimeException("Unsupported platform");
    }

    private void downloadAndExtractJre(AssetInfo jreAsset, Path targetPath) throws IOException {
        progressListener.onStatusChange("Загрузка Java Runtime Environment...");

        // Создаем временную директорию для загрузки
        Path tempDir = Files.createTempDirectory("jre_download");
        try {
            // Скачиваем архив
            Path archivePath = tempDir.resolve("jre.zip");
            downloadManager.downloadAssets(
                Collections.singleton(jreAsset),
                tempDir,
                progressListener
            );

            // Распаковываем архив
            progressListener.onStatusChange("Распаковка Java Runtime Environment...");
            ArchiveExtractor.extractZip(archivePath, targetPath);

            // Проверяем успешность установки
            if (!isJreInstalled(targetPath)) {
                throw new RuntimeException("JRE installation failed - executable not found");
            }
        } finally {
            // Очищаем временные файлы
            FileUtils.deleteDirectory(tempDir);
        }
    }
} 