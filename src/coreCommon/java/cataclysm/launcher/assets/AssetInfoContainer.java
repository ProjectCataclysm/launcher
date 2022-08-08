package cataclysm.launcher.assets;

import java.util.Map;
import java.util.Set;

/**
 * <br><br>REVOM ENGINE / ProjectCataclysm
 * <br>Created: 20.11.2018 16:08
 *
 * @author Knoblul
 */
public class AssetInfoContainer {
	private final Set<String> protectedFolders;
	private final Set<AssetInfo> assets;
	private final Map<String, String> hashes;

	public AssetInfoContainer(Set<String> protectedFolders, Set<AssetInfo> assets, Map<String, String> hashes) {
		this.protectedFolders = protectedFolders;
		this.assets = assets;
		this.hashes = hashes;
	}

	public Set<AssetInfo> getAssets() {
		return assets;
	}

	public Set<String> getProtectedFolders() {
		return protectedFolders;
	}

	public Map<String, String> getHashes() {
		return hashes;
	}
}
