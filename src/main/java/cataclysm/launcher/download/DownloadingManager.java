private static final int MAX_RETRY_ATTEMPTS = 3;
private static final int RETRY_DELAY_MS = 1000;

private volatile boolean isCancelled = false;

private void downloadFileWithRetry(Path localFilePath, AssetInfo asset, DownloadProgressListener listener) throws IOException {
    int attempts = 0;
    Exception lastException = null;

    while (attempts < MAX_RETRY_ATTEMPTS) {
        try {
            downloadFile(localFilePath, asset, listener);
            return;
        } catch (IOException e) {
            lastException = e;
            attempts++;
            
            if (attempts < MAX_RETRY_ATTEMPTS) {
                listener.onError(new ErrorHandler(
                    "Ошибка загрузки, попытка " + (attempts + 1) + " из " + MAX_RETRY_ATTEMPTS,
                    "Не удалось загрузить файл " + asset.getRemote(),
                    e
                ));
                
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Загрузка прервана", ie);
                }
            }
        }
    }

    throw new IOException("Не удалось загрузить файл после " + MAX_RETRY_ATTEMPTS + " попыток", lastException);
}

public interface DownloadProgressListener {
    void onError(ErrorHandler error);
    void onRetry(AssetInfo asset, int attempt, int maxAttempts);
    default void onStatusChange(String status) {}
    default void onProgress(double progress) {}
}

public void cancelDownload() {
    isCancelled = true;
}

private void checkCancellation() throws IOException {
    if (isCancelled) {
        throw new DownloadCancelledException("Загрузка отменена пользователем");
    }
}

public static class DownloadCancelledException extends IOException {
    public DownloadCancelledException(String message) {
        super(message);
    }
} 