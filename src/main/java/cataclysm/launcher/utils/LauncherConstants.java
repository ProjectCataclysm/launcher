package cataclysm.launcher.utils;

import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

/**
 * <br><br>REVOM ENGINE / ProjectCataclysm
 * <br>Created: 08.08.2022 15:02
 *
 * @author Knoblul
 */
public class LauncherConstants {
	public static final String WORK_SITE_URL = "project-cataclysm.ru";
	public static final String API_URL = WORK_SITE_URL + "/api";
	public static final String LAUNCHER_URL = "files." + WORK_SITE_URL + "/files/launcher";
	public static final String CLIENT_URL = "files." + WORK_SITE_URL + "/files/client";

	public static boolean isUnreachableException(Throwable cause) {
		return cause instanceof TimeoutException || cause instanceof SocketTimeoutException
				|| cause instanceof SocketException;
	}
}
