package ru.cataclysm.helpers

import java.net.DatagramSocket
import java.net.InetAddress
import java.net.ServerSocket

object NetworkHelper {

    val defaultIP: InetAddress
        get() {
            DatagramSocket().use { s ->
                s.connect(InetAddress.getByAddress(byteArrayOf(1, 1, 1, 1)), 80)
                return s.localAddress
            }
        }

    val endpoint: String
        get() {
            ServerSocket(0).use { s ->
                return "${defaultIP.hostAddress}:${s.localPort}"
            }
        }
}