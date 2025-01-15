package cataclysm.launcher.download.torrent;

//import com.frostwire.jlibtorrent.*;
//import com.frostwire.jlibtorrent.alerts.AddTorrentAlert;
//import com.frostwire.jlibtorrent.alerts.Alert;
//import com.frostwire.jlibtorrent.alerts.AlertType;
//import com.frostwire.jlibtorrent.alerts.BlockFinishedAlert;
//import com.frostwire.jlibtorrent.swig.libtorrent;
//import com.frostwire.jlibtorrent.swig.settings_pack;

/**
 * <br><br>ProjectCataclysm
 * <br>Created: 24.11.2022 9:28
 *
 * @author Knoblul
 */
public class TorrentDispatcher {
//	public static void main(String[] args) throws Exception {
//		File torrentFile = new File("C:/users/mihail/desktop/test.torrent");
//
//		System.out.println("Using libtorrent version: " + LibTorrent.version());
//
//		settings_pack settings = libtorrent.default_settings();
////		settings.set_int();
//
//		SessionManager s = new SessionManager();
//		s.start(new SessionParams(new SettingsPack(settings)));
//
//		CountDownLatch signal = new CountDownLatch(1);
//
//		s.addListener(new AlertListener() {
//			@Override
//			public int[] types() {
//				return null;
//			}
//
//			@Override
//			public void alert(Alert<?> alert) {
//				AlertType type = alert.type();
//
//				switch (type) {
//					case ADD_TORRENT:
//						System.out.println("Torrent added");
//						((AddTorrentAlert) alert).handle().resume();
//						break;
//					case BLOCK_FINISHED:
//						BlockFinishedAlert a = (BlockFinishedAlert) alert;
//						int p = (int) (a.handle().status().progress() * 100);
//						System.out.println("Progress: " + p + " for torrent name: " + a.torrentName());
//						System.out.println(s.stats().totalDownload());
//						break;
//					case TORRENT_FINISHED:
//						System.out.println("Torrent finished");
//						signal.countDown();
//						break;
//					default:
//						System.out.println("status " + alert);
//						break;
//				}
//			}
//		});
//
//		s.start();
//
//		TorrentInfo ti = new TorrentInfo(torrentFile);
//		s.download(ti, torrentFile.getParentFile());
//
//		signal.await();
//
//		s.stop();
//	}
}
