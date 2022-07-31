package cataclysm.io.sanitation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import cataclysm.utils.HttpHelper;
import cataclysm.utils.PlatformHelper;

/**
 * 
 * 
 * <br>
 * <br>
 * <i>Created 20 ���. 2018 �. / 16:08:01</i><br>
 * SME REDUX / NOT FOR FREE USE!
 * 
 * @author Knoblul
 */
public class ResourceMaster {
	private final Map<String, String> hashes = Maps.newHashMap();
	private final List<String> protectedFolders = Lists.newArrayList();
	private final List<Resource> resources = Lists.newArrayList();

	private Resource parseResource(JSONObject node) {
		return new Resource(
			(String) node.get("local"),
			(String) node.get("remote"),
			(Boolean) node.get("hashed"),
			(Boolean) node.get("archive")
		);
	}

	@SuppressWarnings("unchecked")
	public void retrieveDownloadInformation() throws IOException {
		resources.clear();
		protectedFolders.clear();

		JSONParser parser = new JSONParser();
		try (InputStreamReader reader = new InputStreamReader(
				HttpHelper.openStream(HttpHelper.clientURL("deploy.json")))) {
			JSONObject root = (JSONObject) parser.parse(reader);

			JSONArray pfnodes = (JSONArray) root.get("protected-folders");
			pfnodes.forEach(t -> protectedFolders.add((String) t));

			JSONArray rnodesdef = (JSONArray) root.get("resources-default");
			rnodesdef.forEach(t -> resources.add(parseResource((JSONObject) t)));

			JSONArray rnodesos = (JSONArray) root.get("resources-" + PlatformHelper.getOsArchIdentifier());
			rnodesos.forEach(t -> resources.add(parseResource((JSONObject) t)));
		} catch (ParseException e) {
			throw new IOException("Failed to parse deploy.json", e);
		}
	}

	public void retrieveHashes() throws IOException {
		hashes.clear();
		for (Resource resource : resources) {
			if (resource.isHashed()) {
				try (InputStream in = HttpHelper.openStream(HttpHelper.clientURL(resource.getRemote() + ".hash"));
						BufferedReader reader = new BufferedReader(new InputStreamReader(in, Charsets.UTF_8))) {
					String ln;
					while ((ln = reader.readLine()) != null) {
						List<String> spl = Splitter.on('=').trimResults().splitToList(ln);
						hashes.put(spl.get(0), spl.get(1));
					}
				}
			}
		}
	}

	public List<Resource> getFolderDependedResources(String folder) {
		List<Resource> result = Lists.newArrayList();
		for (Resource resource : resources) {
			if (resource.getLocal().equals(folder)) {
				result.add(resource);
			}
		}
		return result;
	}

	public List<Resource> getResources() {
		return resources;
	}

	public Map<String, String> getHashes() {
		return hashes;
	}

	public List<String> getProtectedFolders() {
		return protectedFolders;
	}
}
