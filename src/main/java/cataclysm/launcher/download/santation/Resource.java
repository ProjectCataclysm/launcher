package cataclysm.launcher.download.santation;

/**
 * <br><br>REVOM ENGINE / ProjectCataclysm
 * <br>Created: 27 июн. 2019 г. / 18:35:22
 *
 * @author Knoblul
 */
public class Resource {
	private final String local;
	private final String remote;
	private final boolean hashed;
	private final boolean isFolder;
	
	public Resource(String local, String remote, boolean hashed, boolean isFolder) {
		this.local = local;
		this.remote = remote;
		this.hashed = hashed;
		this.isFolder = isFolder;
	}
	
	
	public String getLocal() {
		return local;
	}
	
	public String getRemote() {
		return remote;
	}
	
	public boolean isHashed() {
		return hashed;
	}
	
	public boolean isFolder() {
		return isFolder;
	}
}
