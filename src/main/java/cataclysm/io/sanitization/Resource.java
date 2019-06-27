package cataclysm.io.sanitization;

import com.google.common.base.Objects;

/**
 * 
 * 
 * <br><br><i>Created 20 окт. 2018 г. / 16:03:07</i><br>
 * SME REDUX / NOT FOR FREE USE!
 * @author Knoblul
 */
public class Resource {
	private String file;
	private String shaFile;
	private String unpackDir;
	private boolean archive;
	private boolean sanitize;

	public Resource(String file, boolean sanitize) {
		this.file = file;
		this.shaFile = file + ".sha";
		this.archive = file.endsWith(".zip");
		this.sanitize = sanitize;
		
		if (isArchive()) {
			int idx = file.lastIndexOf('.');
			if (idx != -1) {
				String fn = file.substring(0, idx);
				unpackDir = fn;
			}
		}
	}

	public String getFile() {
		return file;
	}
	
	public String getShaFile() {
		return shaFile;
	}
	
	public boolean needSanitize() {
		return sanitize;
	}
	
	public boolean isArchive() {
		return archive;
	}

	public String getUnpackDir() {
		return unpackDir;
	}
	
	@Override
	public String toString() {
		return getFile();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Resource) {
			Resource ri = (Resource) obj;
			return Objects.equal(file, ri.file) && Objects.equal(sanitize, ri.sanitize);
		}
		
		return false;
	}
	
	@Override
	public int hashCode() {
		return Objects.hashCode(file, sanitize);
	}
}
