package ru.cataclysm.services.assets

import com.frostwire.jlibtorrent.AlertListener
import com.frostwire.jlibtorrent.alerts.*

class TorrentListener(
    val service: AssetsService
) : AlertListener {

    override fun types(): IntArray {
        return intArrayOf(
            AlertType.ADD_TORRENT.swig(),
            AlertType.BLOCK_FINISHED.swig(),
            AlertType.PIECE_FINISHED.swig(),
            AlertType.STATE_CHANGED.swig(),
            AlertType.TORRENT_FINISHED.swig(),
//            AlertType.TORRENT_ERROR.swig(),
//            AlertType.TORRENT_LOG.swig(),
        )
    }


    override fun alert(alert: Alert<*>) {
        val type: AlertType = alert.type()

        when (type) {
            AlertType.STATS -> { }

            AlertType.ADD_TORRENT -> {
                val a: AddTorrentAlert = alert as AddTorrentAlert
                a.handle().resume()
            }

            AlertType.BLOCK_FINISHED -> {
                val a: BlockFinishedAlert = alert as BlockFinishedAlert
                service.onProgressUpdated(a.handle().status().progress())
            }

            AlertType.PIECE_FINISHED -> {
                val a: PieceFinishedAlert = alert as PieceFinishedAlert
                service.onProgressUpdated(a.handle().status().progress())
            }

            AlertType.STATE_CHANGED -> {
                val a: StateChangedAlert = alert as StateChangedAlert
                service.onStateChanged(a.state)
            }

            AlertType.TORRENT_FINISHED -> {
                service.onCompleted()
            }

            AlertType.TORRENT_ERROR -> {
                val a: TorrentErrorAlert = alert as TorrentErrorAlert
                println("TORRENT_ERROR: file ${a.filename()}, code: ${a.error()}")
            }

            AlertType.TORRENT_LOG -> {
                val a: TorrentLogAlert = alert as TorrentLogAlert
                println("TORRENT LOG: " + a.logMessage())
            }

            AlertType.TORRENT_REMOVED -> println("TORRENT_REMOVED")
            AlertType.TORRENT_DELETED -> println("TORRENT_DELETED")

            AlertType.TORRENT_PAUSED -> println("TORRENT_PAUSED")
            AlertType.TORRENT_RESUMED -> println("TORRENT_RESUMED")
            AlertType.SAVE_RESUME_DATA -> println("SAVE_RESUME_DATA")
            AlertType.SAVE_RESUME_DATA_FAILED -> println("SAVE_RESUME_DATA_FAILED")
            AlertType.FASTRESUME_REJECTED -> println("FASTRESUME_REJECTED")

            AlertType.TORRENT_CHECKED -> println("TORRENT_CHECKED")

            AlertType.TORRENT_NEED_CERT -> println("TORRENT_NEED_CERT")
            AlertType.INCOMING_CONNECTION -> println("INCOMING_CONNECTION")


            AlertType.METADATA_RECEIVED -> println("METADATA_RECEIVED")
            AlertType.METADATA_FAILED -> println("METADATA_FAILED")

            AlertType.FILE_COMPLETED -> println("FILE_COMPLETED")
            AlertType.FILE_RENAMED -> println("FILE_RENAMED")
            AlertType.FILE_RENAME_FAILED -> println("FILE_RENAME_FAILED")
            AlertType.FILE_ERROR -> println("FILE_ERROR")

            AlertType.HASH_FAILED -> println("HASH_FAILED")

            AlertType.PORTMAP -> println("PORTMAP")
            AlertType.PORTMAP_ERROR -> println("PORTMAP_ERROR")
            AlertType.PORTMAP_LOG -> {
                val a: PortmapLogAlert = alert as PortmapLogAlert
                println("PORTMAP_LOG: ${a.logMessage()}")
            }

            AlertType.TRACKER_ANNOUNCE -> println("TRACKER_ANNOUNCE")
            AlertType.TRACKER_REPLY -> println("TRACKER_REPLY")
            AlertType.TRACKER_WARNING -> println("TRACKER_WARNING")
            AlertType.TRACKER_ERROR -> println("TRACKER_ERROR")

            AlertType.DHT_REPLY -> println("DHT_REPLY")
            AlertType.DHT_BOOTSTRAP -> println("DHT_BOOTSTRAP")
            AlertType.DHT_GET_PEERS -> println("DHT_GET_PEERS")
            AlertType.DHT_ANNOUNCE -> println("DHT_ANNOUNCE")
            AlertType.DHT_ERROR -> println("DHT_ERROR")
            AlertType.DHT_PUT -> println("DHT_PUT")
            AlertType.DHT_MUTABLE_ITEM -> println("DHT_MUTABLE_ITEM")
            AlertType.DHT_IMMUTABLE_ITEM -> println("DHT_IMMUTABLE_ITEM")
            AlertType.DHT_OUTGOING_GET_PEERS -> println("DHT_OUTGOING_GET_PEERS")
            AlertType.DHT_STATS -> println("DHT_STATS")
            AlertType.DHT_LOG -> {
                val a: DhtLogAlert = alert as DhtLogAlert
                println("DHT_LOG: ${a.logMessage()}")
            }
            AlertType.DHT_PKT -> {
                val a: DhtPktAlert = alert as DhtPktAlert
                println("DHT_PKT: ${a.node()}")
            }
            AlertType.DHT_GET_PEERS_REPLY -> println("DHT_GET_PEERS_REPLY")
            AlertType.DHT_DIRECT_RESPONSE -> println("DHT_DIRECT_RESPONSE")
            AlertType.DHT_SAMPLE_INFOHASHES -> println("DHT_SAMPLE_INFOHASHES")
            AlertType.DHT_LIVE_NODES -> println("DHT_LIVE_NODES")

            AlertType.EXTERNAL_IP -> println("EXTERNAL_IP")
            AlertType.LISTEN_SUCCEEDED -> println("LISTEN_SUCCEEDED")
            AlertType.STATE_UPDATE -> println("STATE_UPDATE")
            AlertType.SESSION_STATS -> println("SESSION_STATS")

            AlertType.SCRAPE_REPLY -> println("SCRAPE_REPLY")
            AlertType.SCRAPE_FAILED -> println("SCRAPE_FAILED")

            AlertType.LSD_PEER -> println("LSD_PEER")
            AlertType.PEER_BLOCKED -> println("PEER_BLOCKED")
            AlertType.PERFORMANCE -> println("PERFORMANCE")

            AlertType.READ_PIECE -> println("READ_PIECE")

            AlertType.STORAGE_MOVED -> println("STORAGE_MOVED")
            AlertType.TORRENT_DELETE_FAILED -> println("TORRENT_DELETE_FAILED")
            AlertType.URL_SEED -> println("URL_SEED")
            AlertType.INVALID_REQUEST -> println("INVALID_REQUEST")
            AlertType.LISTEN_FAILED -> println("LISTEN_FAILED")

            AlertType.PEER_BAN -> println("PEER_BAN")
            AlertType.PEER_CONNECT -> println("PEER_CONNECT")
            AlertType.PEER_DISCONNECTED -> println("PEER_DISCONNECTED")
            AlertType.PEER_ERROR -> println("PEER_ERROR")
            AlertType.PEER_SNUBBED -> println("PEER_SNUBBED")
            AlertType.PEER_UNSNUBBED -> println("PEER_UNSNUBBED")

            AlertType.REQUEST_DROPPED -> println("REQUEST_DROPPED")
            AlertType.UDP_ERROR -> println("UDP_ERROR")

            AlertType.BLOCK_DOWNLOADING -> println("BLOCK_DOWNLOADING")
            AlertType.BLOCK_TIMEOUT -> println("BLOCK_TIMEOUT")

            AlertType.CACHE_FLUSHED -> println("CACHE_FLUSHED")


            AlertType.STORAGE_MOVED_FAILED -> println("TORRENT_NEED_CERT")
            AlertType.TRACKERID -> println("TORRENT_NEED_CERT")
            AlertType.UNWANTED_BLOCK -> println("TORRENT_NEED_CERT")

            AlertType.I2P -> println("I2P")

            AlertType.LOG -> println("LOG")
            AlertType.PEER_LOG -> println("PEER_LOG")
            AlertType.LSD_ERROR -> println("LSD_ERROR")

            AlertType.INCOMING_REQUEST -> println("INCOMING_REQUEST")

            AlertType.PICKER_LOG -> println("PICKER_LOG")
            AlertType.SESSION_ERROR -> println("SESSION_ERROR")

            AlertType.SESSION_STATS_HEADER -> println("SESSION_STATS_HEADER")


            AlertType.BLOCK_UPLOADED -> println("BLOCK_UPLOADED")
            AlertType.ALERTS_DROPPED -> println("ALERTS_DROPPED")

            AlertType.UNKNOWN -> println("UNKNOWN")
            AlertType.SOCKS5_ALERT -> println("SOCKS5_ALERT")
        }
    }
}