import { socket } from '@kit.NetworkKit';

/**
 * 网络唤醒：魔术包 = 6 字节 0xFF + MAC 重复 16 次。
 * 同时发往全局广播与主机所在 /24 网段广播，端口 9 与 7，连发三轮。
 */
export class WolClient {
  static normalizeMac(raw: string): string {
    let hex = '';
    for (const ch of raw) {
      if ((ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f') || (ch >= 'A' && ch <= 'F')) {
        hex += ch.toUpperCase();
      }
    }
    if (hex.length !== 12) {
      return '';
    }
    const parts: string[] = [];
    for (let i = 0; i < 12; i += 2) {
      parts.push(hex.substring(i, i + 2));
    }
    return parts.join(':');
  }

  private static macBytes(macAddress: string): Uint8Array | null {
    const normalized = WolClient.normalizeMac(macAddress);
    if (normalized.length === 0) {
      return null;
    }
    const hex = normalized.replace(/:/g, '');
    const bytes = new Uint8Array(6);
    for (let i = 0; i < 6; i++) {
      bytes[i] = Number.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
    }
    return bytes;
  }

  private static subnetBroadcast(host: string): string {
    const parts = host.split('.');
    if (parts.length !== 4) {
      return '';
    }
    for (const part of parts) {
      const value = Number.parseInt(part);
      if (Number.isNaN(value) || value < 0 || value > 255) {
        return '';
      }
    }
    return `${parts[0]}.${parts[1]}.${parts[2]}.255`;
  }

  private static delay(ms: number): Promise<void> {
    return new Promise<void>((resolve) => {
      setTimeout(() => resolve(), ms);
    });
  }

  static async wake(macAddress: string, host: string): Promise<boolean> {
    const mac = WolClient.macBytes(macAddress);
    if (mac === null) {
      return false;
    }
    const packet = new Uint8Array(6 + 16 * 6);
    for (let i = 0; i < 6; i++) {
      packet[i] = 0xFF;
    }
    let offset = 6;
    for (let repeat = 0; repeat < 16; repeat++) {
      for (let i = 0; i < 6; i++) {
        packet[offset] = mac[i];
        offset++;
      }
    }

    const addresses: string[] = ['255.255.255.255'];
    const subnet = WolClient.subnetBroadcast(host);
    if (subnet.length > 0) {
      addresses.push(subnet);
    }

    const udp = socket.constructUDPSocketInstance();
    let sent = false;
    try {
      await udp.bind({ address: '0.0.0.0', port: 0 });
      await udp.setExtraOptions({ broadcast: true });
      for (let round = 0; round < 3; round++) {
        for (const address of addresses) {
          for (const port of [9, 7]) {
            try {
              await udp.send({
                data: packet.buffer as ArrayBuffer,
                address: { address: address, port: port, family: 1 }
              });
              sent = true;
            } catch (error) {
              // 单个广播地址失败不影响其他目标
            }
          }
        }
        if (round < 2) {
          await WolClient.delay(300);
        }
      }
    } catch (error) {
      return false;
    } finally {
      try {
        udp.close();
      } catch (error) {
        // 忽略关闭异常
      }
    }
    return sent;
  }
}
