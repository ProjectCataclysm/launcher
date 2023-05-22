package cataclysm.launcher.download.santation;

import cataclysm.launcher.assets.AssetDifferenceComputer;
import cataclysm.launcher.assets.AssetInfo;
import cataclysm.launcher.assets.AssetInfoContainer;
import com.google.common.collect.Sets;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * <br><br>ProjectCataclysm
 * <br>Created: 16.10.2018 21:06
 *
 * @author Knoblul
 */
public class SanitationManager  {
	private boolean handleDifference(AssetInfoContainer assets, Set<AssetInfo> downloadSet, Path rootPath,
	                                 AssetDifferenceComputer.Difference diff) {
		if (diff.getFilePath().equals(rootPath)) {
			downloadSet.addAll(assets.getAssets());
			return false;
		}

		if (diff.getType() == AssetDifferenceComputer.DifferenceType.ADDED) {
			try {
				Files.delete(diff.getFilePath());
				return true;
			} catch (IOException e) {
				throw new RuntimeException("Failed to delete " + diff.getFilePath(), e);
			}
		}

		downloadSet.add(Objects.requireNonNull(diff.getAsset()));
		return true;
	}

	public CompletableFuture<Set<AssetInfo>> sanitize(AssetInfoContainer assets, Executor executor, Path rootPath) {
		Set<AssetInfo> downloadSet = Sets.newHashSet();
		return AssetDifferenceComputer.findDifferences(assets, executor, rootPath,
				diff -> handleDifference(assets, downloadSet, rootPath, diff)).thenApply(__ -> downloadSet);
	}
}
