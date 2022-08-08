package cataclysm.launcher.assets;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

/**
 * Created 21 окт. 2018 г. / 19:56:59
 * @author Knoblul
 */
public class AssetDifferenceComputer {
	private static CompletableFuture<Void> findModifiedFiles(Executor executor, AssetInfo asset, Path rootPath,
	                                                         Path filePath, Map<String, String> hashes,
	                                                         DifferenceSink sink) {
		if (Files.isDirectory(filePath)) {
			try {
				List<CompletableFuture<Void>> futures = Lists.newArrayList();
				try (DirectoryStream<Path> ds = Files.newDirectoryStream(filePath)) {
					for (Path path : ds) {
						futures.add(findModifiedFiles(executor, asset, rootPath, path, hashes, sink));
					}
				}
				return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
			} catch (IOException e) {
				sink.acceptWrapped(new Difference(DifferenceType.ERROR, asset, filePath,
						new DirectoryListingException("Failed to iterate trough directory " + filePath, e)));
				return CompletableFuture.completedFuture(null);
			}
		} else {
			return CompletableFuture.runAsync(() -> {
				try {
					String hash = ChecksumUtil.computeChecksum(filePath);
					String fn = rootPath.relativize(filePath).toString().replace(File.separatorChar, '/');
					if (hashes.get(fn) == null || !hashes.get(fn).equals(hash)) {
						sink.acceptWrapped(new Difference(DifferenceType.CHANGED, asset, filePath, null));
					}
				} catch (Throwable e) {
					sink.acceptWrapped(new Difference(DifferenceType.ERROR, asset, filePath,
							new DifferenceComputationException("Failed to compute checksum for file " + filePath, e)));
				}
			}, executor);
		}
	}

	private static CompletableFuture<Void> findMissingFiles(Executor executor, AssetInfo asset, Path rootPath,
	                                                        Path filePath, Map<String, String> hashes,
	                                                        DifferenceSink sink) {
		return CompletableFuture.runAsync(() -> {
			List<Path> paths = Lists.newArrayList();
			String path = rootPath.relativize(filePath).toString().replace(File.separatorChar, '/');
			for (String fn : hashes.keySet()) {
				if (fn.startsWith(path)) {
					paths.add(rootPath.resolve(fn));
				}
			}

			for (Path p : paths) {
				if (!Files.isRegularFile(p)) {
					sink.acceptWrapped(new Difference(DifferenceType.REMOVED, asset, filePath, null));
				}
			}
		}, executor);
	}

	private static CompletableFuture<Void> findExtraFiles(Executor executor, AssetInfo asset, Path rootPath, Path filePath,
	                                   Map<String, String> hashes, DifferenceSink sink) {
		if (Files.isDirectory(filePath)) {
			// пробегаемся по всем файлам в директории
			int fileNum = 0;
			try (DirectoryStream<Path> ds = Files.newDirectoryStream(filePath)) {
				List<CompletableFuture<Void>> futures = Lists.newArrayList();
				for (Path path : ds) {
					futures.add(findExtraFiles(executor, asset, rootPath, path, hashes, sink));
					fileNum++;
				}
				return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
			} catch (IOException e) {
				sink.acceptWrapped(new Difference(DifferenceType.ERROR, asset, filePath,
						new DirectoryListingException("Failed to list directory files in " + filePath, e)));
			}

			//  пустые директории
			if (fileNum == 0) {
				sink.acceptWrapped(new Difference(DifferenceType.ADDED, asset, filePath, null));
			}
		} else if (Files.isRegularFile(filePath)) {
			// получаем имя файла
			String fn = rootPath.relativize(filePath).toString().replace(File.separatorChar, '/');

			// проверяем, есть ли имя в хешах
			if (!hashes.containsKey(fn)) {
				sink.acceptWrapped(new Difference(DifferenceType.ADDED, asset, filePath, null));
			}
		}

		return CompletableFuture.completedFuture(null);
	}

	private static CompletableFuture<Void> findDifferencesImpl(AssetInfoContainer assets, Executor executor,
	                                                           Path rootPath, DifferenceSink sink) {
		// получаем игровую директорию
		if (!Files.isDirectory(rootPath)) {
			// если директория отсутствует, то создаём её и добавляем ВСЕ ресурсы
			// на скачивание
			sink.acceptWrapped(new Difference(DifferenceType.REMOVED, null, rootPath, null));

			try {
				Files.createDirectories(rootPath);
			} catch (IOException e) {
				throw new RuntimeException("Failed to create root directory", e);
			}

			return CompletableFuture.completedFuture(null);
		}

		Set<AssetInfo> contents = assets.getAssets();
		Map<String, String> hashes = assets.getHashes();
		List<CompletableFuture<Void>> futures = Lists.newArrayList();

		// удаляем все посторонние файлы из защищёных директорий
		for (String folder : assets.getProtectedFolders()) {
			futures.add(findExtraFiles(executor, null, rootPath, rootPath.resolve(folder), hashes, sink));
		}

		// санитизируем папку с игрой, сравниваем хеши файлов с hashes
		for (AssetInfo asset : contents) {
			Path localPath = rootPath.resolve(asset.getLocal());
			if (asset.isArchive() && !Files.isDirectory(localPath)
					|| !asset.isArchive() && !Files.isRegularFile(localPath)) {
				// если локальный файл не существует добавляем ресурс на
				// скачивание
				sink.acceptWrapped(new Difference(DifferenceType.REMOVED, asset, localPath, null));
				continue;
			}

			if (asset.isHashed()) {
				// ВАЖНО: не сканируем рут!
				if (rootPath.equals(localPath)) {
					continue;
				}

				// если недостаёт файлов или файлы изменены - то добавляем
				// ресурс на скачивание
				futures.add(findMissingFiles(executor, asset, rootPath, localPath, hashes, sink));
				futures.add(findModifiedFiles(executor, asset, rootPath, localPath, hashes, sink));
			}
		}

		return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
	}

	public static CompletableFuture<Void> findDifferences(AssetInfoContainer assets, Executor executor, Path rootPath, DifferenceSink sink) {
		return CompletableFuture.runAsync(() -> {
				}, executor)
				.thenCompose(__ -> findDifferencesImpl(assets, executor, rootPath, sink))
				.handle((result, cause) -> {
					if (cause != null) {
						if (cause instanceof StopComputationException
								|| cause.getCause() instanceof StopComputationException) {
							return null;
						}

						if (cause instanceof CompletionException) {
							throw (CompletionException) cause;
						} else {
							throw new CompletionException(cause);
						}
					}

					return null;
				});
	}

	public enum DifferenceType {
		ERROR("!"),
		REMOVED("-"),
		ADDED("+"),
		CHANGED("*"),

		;

		private final String legend;

		DifferenceType(String legend) {
			this.legend = legend;
		}

		@Override
		public String toString() {
			return legend;
		}
	}

	public static class DifferenceComputationException extends Exception {
		public DifferenceComputationException(String message, Throwable cause) {
			super(message, cause);
		}
	}

	public static final class DirectoryListingException extends DifferenceComputationException {
		public DirectoryListingException(String message, Throwable cause) {
			super(message, cause);
		}
	}

	public static final class Difference {
		private final DifferenceType type;
		private final AssetInfo asset;
		private final Path filePath;
		private final @Nullable DifferenceComputationException failCause;

		public Difference(DifferenceType type, AssetInfo asset, Path filePath,
		                  @Nullable DifferenceComputationException failCause) {
			this.type = type;
			this.asset = asset;
			this.filePath = filePath;
			this.failCause = failCause;
		}

		public DifferenceType getType() {
			return type;
		}

		public @Nullable AssetInfo getAsset() {
			return asset;
		}

		public Path getFilePath() {
			return filePath;
		}

		public @Nullable DifferenceComputationException getFailCause() {
			return failCause;
		}

		@Override
		public String toString() {
			return "{" + "type=" + type+ ", path=" + filePath + ", cause="
					+ failCause + '}';
		}
	}

	@FunctionalInterface
	public interface DifferenceSink {
		boolean accept(Difference difference);

		default void acceptWrapped(Difference difference) {
			if (!accept(difference)) {
				throw new StopComputationException();
			}
		}
	}

	private static final class StopComputationException extends RuntimeException {

	}
}
