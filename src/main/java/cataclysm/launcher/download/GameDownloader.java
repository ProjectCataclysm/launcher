public class GameDownloader {
    private final TorrentManager torrentManager;
    private final Path gamePath;
    private volatile boolean isDownloading = false;
    private DownloadProgressListener listener;
    private CompletableFuture<Void> downloadFuture;

    public GameDownloader(Path gamePath, String torrentUrl) {
        this.gamePath = gamePath;
        this.torrentManager = new TorrentManager(
            gamePath.resolve("game.torrent"),
            torrentUrl
        );
    }

    public synchronized void startDownload(DownloadProgressListener listener) {
        if (isDownloading) return;
        
        this.listener = listener;
        isDownloading = true;
        
        downloadFuture = CompletableFuture.runAsync(() -> {
            try {
                startTorrentDownload();
            } catch (Exception e) {
                listener.onError(new ErrorHandler(
                    "Ошибка загрузки игры",
                    "Не удалось запустить загрузку игры",
                    e
                ));
            }
        });

        // Запускаем проверку обновлений торрент-файла
        torrentManager.startDownload(this::restartDownload);
    }

    private synchronized void restartDownload() {
        if (!isDownloading) return;

        listener.onError(new ErrorHandler(
            "Обнаружена новая версия игры",
            "Перезапуск загрузки для получения актуальной версии",
            null
        ));

        // Останавливаем текущую загрузку
        stopCurrentDownload();

        // Запускаем новую загрузку
        try {
            startTorrentDownload();
        } catch (Exception e) {
            listener.onError(new ErrorHandler(
                "Ошибка перезапуска загрузки",
                "Не удалось перезапустить загрузку игры",
                e
            ));
        }
    }

    private void startTorrentDownload() throws Exception {
        // Здесь реализация запуска торрент-загрузки
        // Используя выбранную торрент-библиотеку
    }

    private void stopCurrentDownload() {
        // Здесь реализация остановки текущей торрент-загрузки
    }

    public synchronized void stopDownload() {
        if (!isDownloading) return;
        
        isDownloading = false;
        torrentManager.stopDownload();
        stopCurrentDownload();
        
        if (downloadFuture != null) {
            downloadFuture.cancel(true);
        }
    }
} 