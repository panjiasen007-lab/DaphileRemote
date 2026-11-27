import { avSession } from '@kit.AVSessionKit';
import { common } from '@kit.AbilityKit';
import { hilog } from '@kit.PerformanceAnalysisKit';
import { NowPlaying, coverUrl, nowPlayingIsPlaying } from '../data/model/Models';
import { DaphileRepository } from '../data/repo/DaphileRepository';
import { CardBridge } from '../app/CardBridge';

const DOMAIN = 0x0000;

/**
 * 把当前播放状态发布到系统媒体中心 / 锁屏。
 * 纯遥控端没有本地音频流，AVSession 仅用于展示与控制转发；
 * 若系统拒绝创建会话，降级为无锁屏控制，不影响主功能。
 */
export class AvSessionController {
  private context: common.UIAbilityContext;
  private repo: DaphileRepository;
  private session: avSession.AVSession | null = null;
  private timer: number = -1;
  private lastAssetId: string = '';
  private lastCardKey: string = '';
  private enabled: boolean = true;

  constructor(context: common.UIAbilityContext, repo: DaphileRepository) {
    this.context = context;
    this.repo = repo;
  }

  async start(): Promise<void> {
    if (!this.enabled) {
      return;
    }
    if (this.timer < 0) {
      this.timer = setInterval(() => {
        this.sync().catch(() => {
        });
      }, 1000);
    }
    await this.sync();
  }

  /** 依据「锁屏控制」开关创建或释放 AVSession。 */
  private async ensureSession(): Promise<void> {
    if (!this.enabled) {
      return;
    }
    if (!this.repo.state.settings.lockScreenControls) {
      if (this.session !== null) {
        try {
          await this.session.destroy();
        } catch (error) {
          // 忽略释放失败
        }
        this.session = null;
        this.lastAssetId = '';
      }
      return;
    }
    if (this.session !== null) {
      return;
    }
    try {
      this.session = await avSession.createAVSession(this.context, 'DaphileRemote', 'audio');
      this.lastAssetId = '';
      this.bindControls();
    } catch (error) {
      this.enabled = false;
      hilog.warn(DOMAIN, 'daphile', 'AVSession unavailable: %{public}s', `${error}`);
    }
  }

  private bindControls(): void {
    const session = this.session;
    if (session === null) {
      return;
    }
    session.on('play', () => {
      this.repo.play().catch(() => {
      });
    });
    session.on('pause', () => {
      this.repo.pause().catch(() => {
      });
    });
    session.on('stop', () => {
      this.repo.stop().catch(() => {
      });
    });
    session.on('playNext', () => {
      this.repo.next().catch(() => {
      });
    });
    session.on('playPrevious', () => {
      this.repo.previous().catch(() => {
      });
    });
    session.on('seek', (time: number) => {
      this.repo.seekTo(time / 1000).catch(() => {
      });
    });
  }

  private async sync(): Promise<void> {
    await this.ensureSession();
    const session = this.session;
    const state = this.repo.state;
    const playing: NowPlaying | null = state.nowPlaying;
    if (session === null || playing === null || playing.track === null ||
      playing.track.title.length === 0) {
      return;
    }
    const track = playing.track;
    const assetId = track.id.length > 0 ? track.id : (track.url.length > 0 ? track.url : track.title);
    if (assetId !== this.lastAssetId) {
      this.lastAssetId = assetId;
      const server = state.server;
      const artwork = server === null ? '' : coverUrl(server, track.coverId, playing.playerId);
      try {
        await session.setAVMetadata({
          assetId: assetId,
          title: track.title,
          artist: track.artist,
          album: track.album,
          duration: Math.trunc((playing.duration > 0 ? playing.duration : track.duration) * 1000),
          mediaImage: artwork
        });
      } catch (error) {
        hilog.warn(DOMAIN, 'daphile', 'setAVMetadata failed: %{public}s', `${error}`);
      }
    }
    this.publishCard(playing);

    if (playing.mode !== 'play' && playing.mode !== 'pause') {
      // 纯遥控端没有本地媒体流，系统不接受 STOP 状态；停止时保持上一次状态即可
      return;
    }
    const playbackState: avSession.PlaybackState = playing.mode === 'play'
      ? avSession.PlaybackState.PLAYBACK_STATE_PLAY
      : avSession.PlaybackState.PLAYBACK_STATE_PAUSE;
    try {
      await session.setAVPlaybackState({
        state: playbackState,
        speed: nowPlayingIsPlaying(playing) ? 1 : 0
      });
    } catch (error) {
      // 播放状态更新失败不阻断轮询
    }
  }

  /** 把当前曲目信息写入卡片快照，并刷新已添加的桌面卡片。 */
  private publishCard(playing: NowPlaying): void {
    const track = playing.track;
    if (track === null || track.title.length === 0) {
      return;
    }
    const server = this.repo.state.server;
    const cover = server === null ? '' : coverUrl(server, track.coverId, playing.playerId);
    const key = `${track.title}|${track.artist}|${playing.mode}|${cover}`;
    if (key === this.lastCardKey) {
      return;
    }
    this.lastCardKey = key;
    CardBridge.writeSnapshot({
      title: track.title,
      artist: track.artist,
      album: track.album,
      playing: nowPlayingIsPlaying(playing) ? 'true' : 'false',
      cover: cover,
      updated: Date.now()
    });
    CardBridge.notifyForms().catch(() => {
    });
  }

  async release(): Promise<void> {
    if (this.timer >= 0) {
      clearInterval(this.timer);
      this.timer = -1;
    }
    if (this.session !== null) {
      try {
        await this.session.destroy();
      } catch (error) {
        // 忽略释放失败
      }
      this.session = null;
    }
  }
}
