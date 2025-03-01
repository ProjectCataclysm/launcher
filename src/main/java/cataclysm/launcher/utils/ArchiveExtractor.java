public class ArchiveExtractor {
    private static final int BUFFER_SIZE = 8192;

    public static void extractZip(Path zipFile, Path targetDir) throws IOException {
        try (ZipFile zip = new ZipFile(zipFile.toFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                Path entryPath = targetDir.resolve(entry.getName());

                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    try (InputStream in = zip.getInputStream(entry);
                         OutputStream out = Files.newOutputStream(entryPath)) {
                        byte[] buffer = new byte[BUFFER_SIZE];
                        int len;
                        while ((len = in.read(buffer)) > 0) {
                            out.write(buffer, 0, len);
                        }
                    }
                    
                    // Устанавливаем права на выполнение для исполняемых файлов на Unix-системах
                    if (PlatformHelper.isUnix() && isExecutable(entry.getName())) {
                        entryPath.toFile().setExecutable(true, true);
                    }
                }
            }
        }
    }

    private static boolean isExecutable(String fileName) {
        return fileName.endsWith(".sh") || 
               fileName.endsWith("/bin/java") ||
               fileName.endsWith("/bin/javaw");
    }
} 