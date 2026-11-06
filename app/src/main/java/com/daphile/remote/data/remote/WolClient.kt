package com.daphile.remote.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * 网络唤醒（Wake-on-LAN）。
 *
 * 向局域网广播魔术包（6 字节 0xFF + MAC 重复 16 次），
 * 同时发往全局广播与主机所在网段的广播地址，端口 9 与 7，
 * 连续发送多轮以提高唤醒成功率。
 */
object WolClient {

    /** 把用户输入的 MAC 规范成 AA:BB:CC:DD:EE:FF；格式非法时返回 null。 */
    fun normalizeMac(raw: String): String? {
        val hex = raw.filter { it.isLetterOrDigit() }
        if (hex.length != 12) return null
        if (hex.any { !(it.isDigit() || it.lowercaseChar() in 'a'..'f') }) return null
        return hex.chunked(2).joinToString(":") { it.uppercase() }
    }

    /** 发送魔术包；返回是否至少成功发出一次（不代表主机已开机）。 */
    suspend fun wake(macAddress: String, host: String): Boolean = withContext(Dispatchers.IO) {
        val mac = macBytes(macAddress) ?: return@withContext false
        val packet = ByteArray(6 + 16 * mac.size)
        for (index in 0 until 6) packet[index] = 0xFF.toByte()
        var offset = 6
        repeat(16) {
            mac.forEach { byte -> packet[offset++] = byte }
        }

        val targets = buildList {
            add(InetAddress.getByName("255.255.255.255") to 9)
            add(InetAddress.getByName("255.255.255.255") to 7)
            subnetBroadcast(host)?.let { broadcast ->
                add(broadcast to 9)
                add(broadcast to 7)
            }
        }

        var sent = false
        DatagramSocket().use { socket ->
            socket.broadcast = true
            repeat(3) { round ->
                targets.forEach { (address, port) ->
                    runCatching {
                        socket.send(DatagramPacket(packet, packet.size, address, port))
                    }.onSuccess { sent = true }
                }
                if (round < 2) delay(300)
            }
        }
        sent
    }

    private fun macBytes(macAddress: String): ByteArray? =
        normalizeMac(macAddress)
            ?.replace(":", "")
            ?.chunked(2)
            ?.map { it.toInt(16).toByte() }
            ?.toByteArray()

    /** 主机为 IPv4 字面量时，按 /24 推算广播地址（如 192.168.1.255）。 */
    private fun subnetBroadcast(host: String): InetAddress? = runCatching {
        val parts = host.split(".")
        if (parts.size != 4) return@runCatching null
        if (parts.any { it.toIntOrNull() !in 0..255 }) return@runCatching null
        InetAddress.getByName("${parts[0]}.${parts[1]}.${parts[2]}.255")
    }.getOrNull()
}
