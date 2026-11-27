import { connection, socket } from '@kit.NetworkKit';

/**
 * 局域网 Daphile/LMS 发现：取本机 IPv4 所在网段（并兼容当前服务器所在网段），
 * 并发探测 9000 端口，返回端口开放的主机地址。
 *
 * 采用固定并发池 + 硬性超时：即使底层 connect 的 timeout 不生效，
 * 也会在 HARD_TIMEOUT 毫秒后强制关闭 socket，避免个别不可达主机拖慢整体扫描。
 */
export class LanDiscovery {
  private static readonly PORT: number = 9000;
  private static readonly CONNECT_TIMEOUT: number = 400;
  private static readonly HARD_TIMEOUT: number = 800;
  private static readonly CONCURRENCY: number = 64;

  private static isIPv4(value: string): boolean {
    const parts = value.split('.');
    if (parts.length !== 4) {
      return false;
    }
    for (const part of parts) {
      if (part.length === 0) {
        return false;
      }
      const number = Number.parseInt(part);
      if (Number.isNaN(number) || number < 0 || number > 255) {
        return false;
      }
    }
    return true;
  }

  private static prefixOf(host: string): string {
    const parts = host.split('.');
    if (parts.length !== 4) {
      return '';
    }
    return `${parts[0]}.${parts[1]}.${parts[2]}`;
  }

  private static addPrefix(prefixes: string[], prefix: string): void {
    if (prefix.length > 0 && prefixes.indexOf(prefix) < 0 && prefixes.length < 3) {
      prefixes.push(prefix);
    }
  }

  /** 本机所有 IPv4 地址所在的 /24 网段。 */
  private static async localPrefixes(): Promise<string[]> {
    const prefixes: string[] = [];
    try {
      const handle = await connection.getDefaultNet();
      const properties = await connection.getConnectionProperties(handle);
      for (const link of properties.linkAddresses) {
        if (link.address.family === 2) {
          continue;
        }
        const ip = link.address.address;
        if (!LanDiscovery.isIPv4(ip) || ip.startsWith('127.')) {
          continue;
        }
        LanDiscovery.addPrefix(prefixes, LanDiscovery.prefixOf(ip));
      }
    } catch (error) {
      // 无网络信息权限或当前无默认网络时忽略，后续使用提示网段
    }
    return prefixes;
  }

  /** 探测单台主机的 9000 端口，成功返回 true。 */
  private static probe(host: string): Promise<boolean> {
    return new Promise<boolean>((resolve: (value: boolean) => void) => {
      const tcp = socket.constructTCPSocketInstance();
      let settled = false;
      const finish = (reachable: boolean): void => {
        if (settled) {
          return;
        }
        settled = true;
        try {
          tcp.close();
        } catch (error) {
          // 关闭失败不影响探测结果
        }
        resolve(reachable);
      };
      const timer = setTimeout(() => finish(false), LanDiscovery.HARD_TIMEOUT);
      tcp.connect({
        address: { address: host, port: LanDiscovery.PORT, family: 1 },
        timeout: LanDiscovery.CONNECT_TIMEOUT
      }).then(() => {
        clearTimeout(timer);
        finish(true);
      }).catch(() => {
        clearTimeout(timer);
        finish(false);
      });
    });
  }

  /**
   * 扫描局域网。hintHost 为当前服务器地址（可为空），其所在网段会一并扫描，
   * 便于模拟器 NAT / 跨网段场景下仍能发现已连接过的服务器。
   */
  static async scan(hintHost: string): Promise<string[]> {
    let prefixes = await LanDiscovery.localPrefixes();
    if (LanDiscovery.isIPv4(hintHost)) {
      const hintPrefix = LanDiscovery.prefixOf(hintHost);
      const ordered = [hintPrefix];
      for (const prefix of prefixes) {
        if (prefix !== hintPrefix) {
          ordered.push(prefix);
        }
      }
      prefixes = ordered.slice(0, 3);
    }
    const hosts: string[] = [];
    for (const prefix of prefixes) {
      for (let i = 1; i <= 254; i++) {
        hosts.push(`${prefix}.${i}`);
      }
    }
    if (hosts.length === 0) {
      return [];
    }
    const startedAt = Date.now();
    console.info(`[LanDiscovery] prefixes=${prefixes.join(',')} hosts=${hosts.length}`);

    const found: string[] = [];
    let cursor = 0;
    const workerCount = Math.min(LanDiscovery.CONCURRENCY, hosts.length);
    const workers: Promise<void>[] = [];
    for (let worker = 0; worker < workerCount; worker++) {
      workers.push((async (): Promise<void> => {
        while (cursor < hosts.length) {
          const index = cursor;
          cursor++;
          const host = hosts[index];
          const reachable = await LanDiscovery.probe(host);
          if (reachable) {
            found.push(host);
          } else if (host === hintHost) {
            console.warn(`[LanDiscovery] hint host ${host} port ${LanDiscovery.PORT} not reachable`);
          }
        }
      })());
    }
    await Promise.all(workers);

    found.sort((left: string, right: string) => left.localeCompare(right));
    console.info(`[LanDiscovery] found=${found.join(',')} cost=${Date.now() - startedAt}ms`);
    return found;
  }
}
