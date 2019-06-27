package cataclysm.io.sanitization;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import com.google.common.collect.Lists;

import cataclysm.utils.HttpHelper;

/**
 * 
 * 
 * <br><br><i>Created 20 окт. 2018 г. / 16:08:01</i><br>
 * SME REDUX / NOT FOR FREE USE!
 * @author Knoblul
 */
public class ResourceRetriever {
	public List<Resource> retrieveResources() {
		List<Resource> resources = Lists.newArrayList();
		
		resources.add(new Resource("config.zip", false));
		resources.add(new Resource("libs.zip", true));
		resources.add(new Resource("texturepacks.zip", true));
		resources.add(new Resource("mods.zip", true));
		resources.add(new Resource("sme_client.jar", true));
		resources.add(new Resource("packed.rvmp", true));
		
		try (InputStream in = HttpHelper.openStream(HttpHelper.clientURL("packed.rvmp"));
				BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
			String ln;
			while ((ln = reader.readLine()) != null) {
				resources.add(new Resource(ln, true));
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		return resources;
	}
}
