import { http } from '@kit.NetworkKit';
import { util } from '@kit.ArkTS';
import { BusinessError } from '@kit.BasicServicesKit';
import {
  Album, Artist, BrowserItem, Genre, HostPowerAction, LmsError, LmsException,
  Paged, Player, PlaylistSummary, PluginEntry, ScanStatus, SearchResults,
  ServerConfig, ServerInfo, Track, YearItem, daphileBaseUrl, serverBaseUrl
} from '../model/Models';
import { JsonMap, LmsParser, readMap } from './LmsParser';

interface JsonRpcError {
  message?: string;
}

export class LmsClient {
  private static readonly JSON_HEADER: string = 'application/json; charset=utf-8';

  private basicAuth(server: ServerConfig): string {
    if (server.username.length === 0) {
      return '';
    }
    const raw = `${server.username}:${server.password}`;
    const bytes = new util.TextEncoder().encodeInto(raw);
    const encoded = new util.Base64Helper().encodeToStringSync(bytes);
    return `Basic ${encoded}`;
  }

  private async withTimeout<T>(promise: Promise<T>, timeoutMs: number): Promise<T> {
    return new Promise<T>((resolve, reject) => {
      let finished = false;
      const timer = setTimeout(() => {
        if (!finished) {
          finished = true;
          reject(new LmsException(LmsError.TIMEOUT, '连接超时，请重试'));
        }
      }, timeoutMs);
      promise.then((value: T) => {
        if (!finished) {
          finished = true;
          clearTimeout(timer);
          resolve(value);
        }
      }).catch((error: Error) => {
        if (!finished) {
          finished = true;
          clearTimeout(timer);
          reject(error);
        }
      });
    });
  }

  async request(server: ServerConfig, playerId: string, command: string[]): Promise<JsonMap> {
    const payload: JsonMap = {
      'id': 1,
      'method': 'slim.request',
      'params': [playerId, command] as Object[]
    };
    const bodyText = JSON.stringify(payload);
    const header: Record<string, string> = {
      'Content-Type': LmsClient.JSON_HEADER
    };
    const auth = this.basicAuth(server);
    if (auth.length > 0) {
      header['Authorization'] = auth;
    }

    const task = http.createHttp();
    const requestPromise: Promise<JsonMap> = new Promise<JsonMap>((resolve, reject) => {
      task.request(`${serverBaseUrl(server)}/jsonrpc.js`, {
        method: http.RequestMethod.POST,
        header: header,
        extraData: bodyText,
        connectTimeout: 5000,
        readTimeout: 12000,
        expectDataType: http.HttpDataType.STRING
      }).then((response: http.HttpResponse) => {
        try {
          if (response.responseCode === 401 || response.responseCode === 403) {
            reject(new LmsException(LmsError.AUTH, '用户名或密码错误'));
            return;
          }
          if (response.responseCode < 200 || response.responseCode >= 300) {
            reject(new LmsException(LmsError.NETWORK, `服务器返回 HTTP ${response.responseCode}`));
            return;
          }
          let body = '';
          if (typeof response.result === 'string') {
            body = response.result as string;
          } else {
            body = JSON.stringify(response.result);
          }
          if (body.trim().length === 0) {
            reject(new LmsException(LmsError.NETWORK, '服务器未返回数据'));
            return;
          }
          if (body.trim().startsWith('<')) {
            reject(new LmsException(LmsError.AUTH, '需要登录 Daphile'));
            return;
          }
          let root: JsonMap;
          try {
            root = JSON.parse(body) as JsonMap;
          } catch (error) {
            reject(new LmsException(LmsError.SERVER, '服务器返回了无法解析的数据'));
            return;
          }
          const jsonError = readMap(root, 'error');
          if (jsonError !== null) {
            const message = jsonError['message'];
            reject(new LmsException(
              LmsError.SERVER,
              message !== undefined ? message.toString() : '服务器返回错误'
            ));
            return;
          }
          const result = readMap(root, 'result');
          if (result === null) {
            resolve({});
            return;
          }
          const rawError = result['_error'];
          const fallbackError = result['error'];
          const resultError = rawError !== undefined && rawError !== null
            ? rawError.toString()
            : (fallbackError !== undefined && fallbackError !== null ? fallbackError.toString() : '');
          if (resultError.length > 0) {
            const kind = resultError.toLowerCase().includes('invalid player')
              ? LmsError.PLAYER : LmsError.SERVER;
            reject(new LmsException(kind, resultError));
            return;
          }
          resolve(result);
        } catch (error) {
          reject(error as Error);
        } finally {
          task.destroy();
        }
      }).catch((error: BusinessError) => {
        task.destroy();
        reject(new LmsException(LmsError.NETWORK, '无法连接服务器，请检查 IP 与端口'));
      });
    });

    return this.withTimeout(requestPromise, 18000);
  }

  async serverStatus(server: ServerConfig): Promise<ServerInfo> {
    return LmsParser.parseServerInfo(await this.request(server, '', ['serverstatus', '0', '999']));
  }

  async players(server: ServerConfig): Promise<Player[]> {
    return LmsParser.parsePlayers(await this.request(server, '', ['players', '0', '200']));
  }

  async quickStatus(server: ServerConfig, playerId: string): Promise<JsonMap> {
    return this.request(server, playerId, ['status', '-', '2', `tags:${LmsParser.TRACK_TAGS}`]);
  }

  async queueStatus(server: ServerConfig, playerId: string, limit: number): Promise<JsonMap> {
    const safeLimit = Math.min(1000, Math.max(1, limit));
    return this.request(server, playerId,
      ['status', '0', safeLimit.toString(), `tags:${LmsParser.TRACK_TAGS}`]);
  }

  async playerBrief(server: ServerConfig, base: Player): Promise<Player> {
    const status = await this.quickStatus(server, base.id);
    if (Object.keys(status).length === 0) {
      throw new LmsException(LmsError.PLAYER, '播放器不可达');
    }
    return LmsParser.parsePlayerStatus(base, status);
  }

  async albums(server: ServerConfig, start: number, count: number, sort: string,
    artistId: string = '', genreId: string = '', year: string = ''): Promise<Paged<Album>> {
    const command: string[] = ['albums', start.toString(), count.toString(), 'tags:layjq', `sort:${sort}`];
    if (artistId.length > 0) {
      command.push(`artist_id:${artistId}`);
    }
    if (genreId.length > 0) {
      command.push(`genre_id:${genreId}`);
    }
    if (year.length > 0) {
      command.push(`year:${year}`);
    }
    return LmsParser.parseAlbums(await this.request(server, '', command), start, count);
  }

  async artists(server: ServerConfig, start: number, count: number): Promise<Paged<Artist>> {
    return LmsParser.parseArtists(
      await this.request(server, '', ['artists', start.toString(), count.toString()]), start, count);
  }

  async genres(server: ServerConfig, start: number, count: number): Promise<Paged<Genre>> {
    return LmsParser.parseGenres(
      await this.request(server, '', ['genres', start.toString(), count.toString()]), start, count);
  }

  async years(server: ServerConfig, start: number, count: number): Promise<Paged<YearItem>> {
    return LmsParser.parseYears(
      await this.request(server, '', ['years', start.toString(), count.toString()]), start, count);
  }

  async playlists(server: ServerConfig, start: number, count: number): Promise<Paged<PlaylistSummary>> {
    return LmsParser.parsePlaylists(
      await this.request(server, '', ['playlists', start.toString(), count.toString()]), start, count);
  }

  async albumInfo(server: ServerConfig, albumId: string): Promise<Album | null> {
    const page = LmsParser.parseAlbums(
      await this.request(server, '', ['albums', '0', '1', `album_id:${albumId}`, 'tags:layjq']));
    return page.items.length > 0 ? page.items[0] : null;
  }

  async albumTracks(server: ServerConfig, albumId: string): Promise<Paged<Track>> {
    return LmsParser.parseTracks(await this.request(server, '', [
      'tracks', '0', '500', `album_id:${albumId}`,
      `tags:${LmsParser.TRACK_TAGS}`, 'sort:tracknum'
    ]));
  }

  async playlistTracks(server: ServerConfig, playlistId: string): Promise<Paged<Track>> {
    return LmsParser.parseTracks(await this.request(server, '', [
      'playlists', 'tracks', '0', '1000', `playlist_id:${playlistId}`,
      `tags:${LmsParser.TRACK_TAGS}`
    ]));
  }

  async songs(server: ServerConfig, start: number, count: number): Promise<Paged<Track>> {
    return LmsParser.parseTracks(await this.request(server, '', [
      'titles', start.toString(), count.toString(), `tags:${LmsParser.TRACK_TAGS}`
    ]), start, count);
  }

  async randomTitles(server: ServerConfig, count: number): Promise<Paged<Track>> {
    const safe = Math.min(500, Math.max(1, count));
    return LmsParser.parseTracks(await this.request(server, '', [
      'titles', '0', safe.toString(), 'sort:random', `tags:${LmsParser.TRACK_TAGS}`
    ]));
  }

  /**
   * 真随机抽取：LMS 的 sort:random 在同一会话内顺序固定，
   * 这里先取总数，再多起点采样、去重洗牌。
   */
  async randomTracks(server: ServerConfig, count: number): Promise<Track[]> {
    const target = Math.min(500, Math.max(1, count));
    const totalPage = await this.songs(server, 0, 0);
    const total = totalPage.total;
    if (total <= 0) {
      return [];
    }
    if (total <= target) {
      const all = await this.songs(server, 0, total);
      return this.shuffle(all.items);
    }
    const chunk = Math.max(1, Math.trunc((target + 11) / 12));
    const pool: Map<string, Track> = new Map<string, Track>();
    let attempts = 0;
    while (pool.size < target && attempts < 48) {
      const start = Math.floor(Math.random() * Math.max(1, total - chunk + 1));
      const page = await this.songs(server, start, chunk);
      for (const track of page.items) {
        const key = this.randomKey(track);
        if (!pool.has(key)) {
          pool.set(key, track);
        }
      }
      attempts++;
    }
    if (pool.size < target) {
      const fallback = await this.randomTitles(server, target * 2);
      for (const track of fallback.items) {
        if (pool.size >= target) {
          break;
        }
        const key = this.randomKey(track);
        if (!pool.has(key)) {
          pool.set(key, track);
        }
      }
    }
    return this.shuffle(Array.from(pool.values())).slice(0, target);
  }

  async randomAlbum(server: ServerConfig): Promise<Album | null> {
    const page = await this.albums(server, 0, 0, 'album');
    const total = page.total;
    if (total <= 0) {
      return null;
    }
    const result = await this.albums(server, Math.floor(Math.random() * total), 1, 'album');
    return result.items.length > 0 ? result.items[0] : null;
  }

  private randomKey(track: Track): string {
    if (track.id.length > 0) {
      return track.id;
    }
    if (track.url.length > 0) {
      return track.url;
    }
    return `${track.album}|${track.title}`;
  }

  private shuffle(tracks: Track[]): Track[] {
    const copy = tracks.slice();
    for (let i = copy.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      const temp = copy[i];
      copy[i] = copy[j];
      copy[j] = temp;
    }
    return copy;
  }

  async musicFolder(server: ServerConfig, parentId: string, start: number, count: number): Promise<Paged<BrowserItem>> {
    const command: string[] = ['musicfolder', start.toString(), count.toString(), 'tags:ucd'];
    if (parentId.length > 0 && parentId !== '0') {
      command.push(`folder_id:${parentId}`);
    }
    const result = await this.request(server, '', command);
    const items = LmsParser.parseBrowserItems(result);
    const loop = result['item_loop'];
    const folderLoop = result['folder_loop'];
    let rawCount = items.length;
    if (loop !== undefined && loop !== null && Array.isArray(loop)) {
      rawCount = (loop as Object[]).length;
    } else if (folderLoop !== undefined && folderLoop !== null && Array.isArray(folderLoop)) {
      rawCount = (folderLoop as Object[]).length;
    }
    const page = LmsParser.pageInfo(result, rawCount, start, count);
    return { total: page.total, items: items, hasMore: page.hasMore };
  }

  async songInfo(server: ServerConfig, trackId: string): Promise<JsonMap> {
    return LmsParser.parseSongInfo(
      await this.request(server, '', ['songinfo', '0', '100', `track_id:${trackId}`, 'tags:wa']));
  }

  async scanProgress(server: ServerConfig): Promise<ScanStatus> {
    return LmsParser.parseScanStatus(await this.request(server, '', ['rescanprogress']));
  }

  async startScan(server: ServerConfig, full: boolean): Promise<void> {
    await this.request(server, '', [full ? 'wipecache' : 'rescan']);
  }

  async search(server: ServerConfig, query: string): Promise<SearchResults> {
    const meta = LmsParser.parseSearch(query,
      await this.request(server, '', ['search', '0', '30', `term:${query}`]));
    try {
      const rich = LmsParser.parseTracks(await this.request(server, '', [
        'titles', '0', '30', `search:${query}`, `tags:${LmsParser.TRACK_TAGS}`
      ])).items;
      if (rich.length > 0) {
        meta.songs = rich;
      }
    } catch (error) {
      // 搜索接口不返回音质字段时保留基础结果
    }
    return meta;
  }

  async favorites(server: ServerConfig, parentId: string = '', playerId: string = ''): Promise<BrowserItem[]> {
    const command: string[] = ['favorites', 'items', '0', '200', 'want_url:1'];
    if (parentId.length > 0 && parentId !== '0') {
      command.push(`item_id:${parentId}`);
    }
    return LmsParser.parseBrowserItems(await this.request(server, playerId, command));
  }

  async radios(server: ServerConfig): Promise<PluginEntry[]> {
    return LmsParser.parsePlugins(await this.request(server, '', ['radios', '0', '100']));
  }

  async apps(server: ServerConfig): Promise<PluginEntry[]> {
    return LmsParser.parsePlugins(await this.request(server, '', ['apps', '0', '100']));
  }

  async pluginItems(server: ServerConfig, playerId: string, pluginCmd: string,
    parentId: string): Promise<BrowserItem[]> {
    return LmsParser.parseBrowserItems(await this.request(server, playerId, [
      pluginCmd, 'items', '0', '100', `item_id:${parentId}`, 'want_url:1'
    ]));
  }

  async command(server: ServerConfig, playerId: string, command: string[]): Promise<void> {
    await this.request(server, playerId, command);
  }

  async hostAction(server: ServerConfig, action: HostPowerAction): Promise<void> {
    const header: Record<string, string> = {};
    const auth = this.basicAuth(server);
    if (auth.length > 0) {
      header['Authorization'] = auth;
    }
    const task = http.createHttp();
    const path = action === HostPowerAction.RESTART
      ? '/cgi-bin/Settings?ACTION=restart'
      : (action === HostPowerAction.SHUTDOWN
        ? '/cgi-bin/Info?ACTION=shutdown'
        : '/cgi-bin/Info?ACTION=pmsuspend');
    try {
      const response = await this.withTimeout(
        task.request(`${daphileBaseUrl(server)}${path}`, {
          method: http.RequestMethod.GET,
          header: header,
          connectTimeout: 5000,
          readTimeout: 10000,
          expectDataType: http.HttpDataType.STRING
        }), 15000);
      if (response.responseCode < 200 || response.responseCode >= 300) {
        throw new LmsException(LmsError.SERVER, `主机接口返回 HTTP ${response.responseCode}`);
      }
    } catch (error) {
      if (error instanceof LmsException) {
        throw error;
      }
      throw new LmsException(LmsError.NETWORK, `主机电源接口不可达（${daphileBaseUrl(server)}）`);
    } finally {
      task.destroy();
    }
  }
}
