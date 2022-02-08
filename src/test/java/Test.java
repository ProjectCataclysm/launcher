import com.sun.deploy.util.StringUtils;

import java.util.Random;
import java.util.UUID;

/**
 * <br><br>REVOM ENGINE / ProjectCataclysm
 * <br>Created: 15.11.2021 14:17
 *
 * @author Knoblul
 */
public class Test {

	public static class TheClass {
		String name;

		public String getName() {
			return name;
		}
	}

	public static void main(String[] args) {
		TheClass cl = new TheClass() {
			@Override
			public String getName() {
				return super.getName() + System.currentTimeMillis();
			}
		};

		System.out.println(cl.getClass().isAssignableFrom(TheClass.class));
	}
}
