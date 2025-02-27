package cataclysm.launcher.assets;

/**
 * <br><br>ProjectCataclysm
 * <br>Created: 27.06.2019 18:35
 *
 * @author Knoblul
 */
public class AssetInfo {
	private final String local;
	private final String remote;
	private final boolean archive;
	private final boolean hashed;

	public AssetInfo(String local, String remote, boolean archive, boolean hashed) {
		this.local = local;
		this.remote = remote;
		this.archive = archive;
		this.hashed = hashed;
	}

	public String getLocal() {
		return local;
	}
	
	public String getRemote() {
		return remote;
	}

	public boolean isArchive() {
		return archive;
	}

	public boolean isHashed() {
		return hashed;
	}
}
