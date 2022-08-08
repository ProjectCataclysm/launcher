package cataclysm.launcher.ui;

import javafx.scene.image.Image;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;


/**
 * Created 28 . 2018 . / 18:09:34
 * @author Knoblul
 */
public class Icons {
	public static Image readImage(String name) throws IOException {
		String file = "/" + name;
		try (InputStream in = Icons.class.getResourceAsStream(file)) {
			if (in == null) {
				throw new FileNotFoundException(file);
			}
			return new Image(in);
		}
	}
}
