import org.libtorrent4j.*;
import org.libtorrent4j.alerts.AddTorrentAlert;
import org.libtorrent4j.alerts.Alert;
import org.libtorrent4j.alerts.AlertType;
import org.libtorrent4j.alerts.BlockFinishedAlert;

import java.io.File;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;

/**
 * <br><br>ProjectCataclysm
 * <br>Created: 23.11.2022 21:23
 *
 * @author Knoblul
 */
public class TorrentTest {
	public static void main(String[] args) throws InterruptedException {
		File torrentFile = new File("C:/users/mihail/desktop/torrent_test.torrent");


		System.out.println("Using libtorrent version: " + LibTorrent.version());

		final SessionManager s = new SessionManager();

		final CountDownLatch signal = new CountDownLatch(1);

		s.addListener(new AlertListener() {
			@Override
			public int[] types() {
				return null;
			}

			@Override
			public void alert(Alert<?> alert) {
				AlertType type = alert.type();

				switch (type) {
					case ADD_TORRENT:
						System.out.println("Torrent added");
						((AddTorrentAlert) alert).handle().resume();
						break;
					case BLOCK_FINISHED:
						BlockFinishedAlert a = (BlockFinishedAlert) alert;
						int p = (int) (a.handle().status().progress() * 100);
						System.out.println("Progress: " + p + " for torrent name: " + a.torrentName());
						System.out.println(s.stats().totalDownload());
						break;
					case TORRENT_FINISHED:
						System.out.println("Torrent finished");
						signal.countDown();
						break;
				}
			}
		});

		s.start();

		TorrentInfo ti = new TorrentInfo(torrentFile);
		s.download(ti, torrentFile.getParentFile());

		signal.await();

		s.stop();
	}
}
