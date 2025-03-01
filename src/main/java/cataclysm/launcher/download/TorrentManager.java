public class TorrentManager {
    private static final long CHECK_INTERVAL = 60_000; // 1 минута в миллисекундах
    private final Path localTorrentPath;
    private final String remoteTorrentUrl;
    private volatile boolean isDownloading = false;
    private String currentTorrentHash;
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> checkTask;
    
    public TorrentManager(Path localTorrentPath, String remoteTorrentUrl) {
        this.localTorrentPath = localTorrentPath;
        this.remoteTorrentUrl = remoteTorrentUrl;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "TorrentChecker");
            t.setDaemon(true);
            return t;
        });
    }

    public void startDownload(TorrentUpdateCallback callback) {
        isDownloading = true;
        currentTorrentHash = downloadAndGetHash();
        
        // Запускаем периодическую проверку
        checkTask = scheduler.scheduleAtFixedRate(() -> {
            try {
                if (!isDownloading) return;
                
                String newHash = getRemoteTorrentHash();
                if (!newHash.equals(currentTorrentHash)) {
                    currentTorrentHash = newHash;
                    callback.onTorrentUpdate();
                }
            } catch (Exception e) {
                Log.err(e, "Failed to check torrent updates");
            }
        }, CHECK_INTERVAL, CHECK_INTERVAL, TimeUnit.MILLISECONDS);
    }

    public void stopDownload() {
        isDownloading = false;
        if (checkTask != null) {
            checkTask.cancel(false);
        }
    }

    private String downloadAndGetHash() throws IOException {
        // Загружаем торрент-файл
        try (HttpClientWrapper.HttpResponse response = HttpClientWrapper.get(remoteTorrentUrl)) {
            Files.createDirectories(localTorrentPath.getParent());
            try (InputStream in = response.getBody().byteStream()) {
                Files.copy(in, localTorrentPath, StandardCopyOption.REPLACE_EXISTING);
            }
            return getFileHash(localTorrentPath);
        }
    }

    private String getRemoteTorrentHash() throws IOException {
        try (HttpClientWrapper.HttpResponse response = HttpClientWrapper.get(remoteTorrentUrl)) {
            try (InputStream in = response.getBody().byteStream()) {
                Path tempFile = Files.createTempFile("torrent", ".tmp");
                try {
                    Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
                    return getFileHash(tempFile);
                } finally {
                    Files.deleteIfExists(tempFile);
                }
            }
        }
    }

    private String getFileHash(Path file) throws IOException {
        return ChecksumUtil.computeChecksum(file);
    }

    public interface TorrentUpdateCallback {
        void onTorrentUpdate();
    }
} 