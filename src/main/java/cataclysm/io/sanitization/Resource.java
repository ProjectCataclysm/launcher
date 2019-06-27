package cataclysm.io.sanitization;

/**
 * 
 * <br><br><i>Created 27 июн. 2019 г. / 18:35:22</i><br>
 * SME REDUX / NOT FOR FREE USE!
 * @author Knoblul
 */
public class Resource {
	private String local;
	private String remote;
	private boolean hashed;
	private boolean isFolder;
	
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
