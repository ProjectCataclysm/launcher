package cataclysm.utils;

import java.awt.Image;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

import com.google.common.collect.Lists;

import cataclysm.ui.DialogUtils;

/**
 * Created 28 сент. 2018 г. / 18:09:34 
 * @author Knoblul
 */
public class Icons {
	public static Image readImage(String name) throws IOException {
		String file = "/icons/" + name;
		try (InputStream in = Icons.class.getResourceAsStream(file)) {
			if (in == null) {
				throw new FileNotFoundException(file);
			}
			return ImageIO.read(in);
		}
	}

	public static void setupIcons(JFrame frame) {
		try {
			Image icon16 = readImage("icon_16.png");
			Image icon32 = readImage("icon_32.png");
			frame.setIconImages(Lists.newArrayList(icon16, icon32));
		} catch (IOException e) {
			DialogUtils.showError(e.getLocalizedMessage(), e);
		}
	}
}
