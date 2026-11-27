import { promptAction } from '@kit.ArkUI';
import { AppContext } from '../../app/AppContext';
import {
  ConnectionState, HostPowerAction, Lyrics, LyricsSource, NowPlaying, Player, SleepAction, Track,
  coverUrl, friendlyErrorMessage, lyricsSourceFromKey, nowPlayingIsPlaying
} from '../../data/model/Models';
import { TrackDisplay } from '../../data/model/TrackDisplay';
import { DaphileRepository } from '../../data/repo/DaphileRepository';
import { AppColors, formatTime, withAlpha } from '../theme/Theme';
import { ThemeRuntime } from '../theme/ThemeRuntime';
import { ThemeBackdropView, ThemeSceneView } from '../art/ThemeViews';
import { EmptyState, QualityBadge, TrackQuality, TrackRow, trackQuality } from '../components/Common';
import { TransportIcon, TransportKind } from '../components/TransportIcon';
import { UiIcon, UiIconKind } from '../components/UiIcon';
import { HaloRing } from '../components/HaloRing';
import { stringsFor, UiStrings } from '../i18n/I18n';

function toast(message: string): void {
  promptAction.showToast({ message: message, duration: 2000 });
}

function formatRateText(rate: string): string {
  const numeric = Number.parseFloat(rate);
  if (Number.isNaN(numeric) || numeric <= 0) {
    return rate;
  }
  if (numeric >= 1000000) {
    return `${(numeric / 1000000).toFixed(1)} MHz`;
  }
  if (numeric >= 1000) {
    const khz = numeric / 1000;
    return khz % 1 === 0 ? `${Math.trunc(khz)} kHz` : `${khz.toFixed(1)} kHz`;
  }
  return `${numeric} Hz`;
}

@ComponentV2
export struct NowPlayingPage {
  @Param pathStack: NavPathStack = new NavPathStack();
  @Param wide: boolean = false;
  private repo: DaphileRepository = AppContext.get().repository;

  @Local lyrics: Lyrics | null = null;
  @Local onlineCover: string = '';
  @Local showMenu: boolean = false;
  @Local showSleep: boolean = false;
  @Local showQueue: boolean = false;
  private queueScroller: Scroller = new Scroller();
  @Local showPlayers: boolean = false;
  @Local showPower: boolean = false;
  @Local coverAngle: number = 0;
  private lyricsTimer: number = -1;
  private coverTimer: number = -1;
  private lastLyricsTrackId: string = '';
  private lastCoverTrackId: string = '';

  aboutToAppear(): void {
    this.refreshLyrics();
    if (this.lyricsTimer < 0) {
      this.lyricsTimer = setInterval(() => {
        const state = this.repo.state.nowPlaying;
        const trackId = state !== null && state.track !== null ? state.track.id : '';
        if (trackId !== this.lastLyricsTrackId) {
          this.refreshLyrics();
        }
      }, 2000);
    }
    if (this.coverTimer < 0) {
      this.coverTimer = setInterval(() => {
        const state = this.repo.state.nowPlaying;
        const trackId = state !== null && state.track !== null ? state.track.id : '';
        if (trackId !== this.lastCoverTrackId) {
          this.lastCoverTrackId = trackId;
          this.coverAngle = 0;
        }
        if (this.livePlaying()) {
          this.coverAngle = (this.coverAngle + 1.1) % 360;
        }
      }, 60);
    }
  }

  aboutToDisappear(): void {
    if (this.lyricsTimer >= 0) {
      clearInterval(this.lyricsTimer);
      this.lyricsTimer = -1;
    }
    if (this.coverTimer >= 0) {
      clearInterval(this.coverTimer);
      this.coverTimer = -1;
    }
  }

  private refreshLyrics(): void {
    const state = this.repo.state.nowPlaying;
    if (state === null || state.track === null) {
      this.lyrics = null;
      this.lastLyricsTrackId = '';
      return;
    }
    const track = state.track;
    if (this.lastLyricsTrackId !== track.id) {
      // 切歌时立即清空，避免短暂显示上一首歌词造成“歌词不匹配”
      this.lyrics = null;
    }
    this.lastLyricsTrackId = track.id;
    this.onlineCover = '';
    if (track.coverId.length === 0) {
      this.repo.resolveOnlineCover(track).then((url) => {
        if (this.lastLyricsTrackId === track.id) {
          this.onlineCover = url;
        }
      }).catch(() => {
      });
    }
    const source: LyricsSource = lyricsSourceFromKey(this.repo.state.settings.lyricsSource);
    this.repo.lyricsForTrack(track, source, false).then((lyrics) => {
      if (this.lastLyricsTrackId === track.id) {
        this.lyrics = lyrics;
      }
    }).catch(() => {
    });
  }

  @Computed
  get s(): UiStrings {
    const settings = this.repo.state.settings;
    return stringsFor(settings.theme.length > 0 ? settings.language : 'en');
  }

  private playProgress(): number {
    const state = this.repo.state.nowPlaying;
    if (state === null || state.duration <= 0) {
      return 0;
    }
    return Math.min(1, Math.max(0, state.time / state.duration));
  }

  // 直接读取仓库状态，确保细粒度刷新
  private liveTrack(): Track | null {
    const state = this.repo.state.nowPlaying;
    return state === null ? null : state.track;
  }

  private liveTime(): number {
    const state = this.repo.state.nowPlaying;
    return state === null ? 0 : state.time;
  }

  private liveDuration(): number {
    const state = this.repo.state.nowPlaying;
    return state === null ? 0 : state.duration;
  }

  private liveCanSeek(): boolean {
    const state = this.repo.state.nowPlaying;
    return state !== null && state.canSeek;
  }

  private livePlaying(): boolean {
    const state = this.repo.state.nowPlaying;
    return state !== null && nowPlayingIsPlaying(state);
  }

  private liveShuffle(): number {
    const state = this.repo.state.nowPlaying;
    return state === null ? 0 : state.shuffle;
  }

  private liveRepeat(): number {
    const state = this.repo.state.nowPlaying;
    return state === null ? 0 : state.repeat;
  }

  private liveVolume(): number {
    const state = this.repo.state.nowPlaying;
    return state === null ? 0 : state.volume;
  }

  private liveCanSetVolume(): boolean {
    const state = this.repo.state.nowPlaying;
    return state !== null && state.canSetVolume;
  }

  private liveMuted(): boolean {
    const state = this.repo.state.nowPlaying;
    return state !== null && state.muted;
  }

  private liveQueue(): Track[] {
    const state = this.repo.state.nowPlaying;
    return state === null ? [] : state.queue;
  }

  /** 打开队列并滚动定位到当前播放曲目（对齐 Android 1.9.1）。 */
  private openQueue(): void {
    this.showQueue = true;
    const current = this.liveCurrentIndex();
    const queue = this.liveQueue();
    let position = -1;
    for (let i = 0; i < queue.length; i++) {
      if (queue[i].index === current) {
        position = i;
        break;
      }
    }
    if (position < 0) {
      for (let i = 0; i < queue.length; i++) {
        if (this.liveTrack() !== null && queue[i].id === (this.liveTrack() as Track).id) {
          position = i;
          break;
        }
      }
    }
    if (position >= 0) {
      setTimeout(() => {
        try {
          this.queueScroller.scrollToIndex(Math.max(0, position - 1));
        } catch (error) {
        }
      }, 160);
    }
  }

  private liveCurrentIndex(): number {
    const state = this.repo.state.nowPlaying;
    return state === null ? -1 : state.currentIndex;
  }

  private liveTitle(): string {
    const track = this.liveTrack();
    if (track === null) {
      return this.s.unknownTrack;
    }
    const title = TrackDisplay.title(track);
    return title.length > 0 ? title : this.s.unknownTrack;
  }

  private liveSubtitle(): string {
    const track = this.liveTrack();
    if (track === null) {
      return '';
    }
    return [TrackDisplay.artist(track), TrackDisplay.album(track)]
      .filter((value: string) => value.length > 0).join(' · ');
  }

  private liveQuality(): TrackQuality | null {
    return trackQuality(this.liveTrack());
  }

  private liveCover(): string {
    const state = this.repo.state.nowPlaying;
    if (state === null || state.track === null) {
      return '';
    }
    return this.currentCover(state);
  }

  private playerName(): string {
    const id = this.repo.state.currentPlayerId;
    for (const player of this.repo.state.players) {
      if (player.id === id) {
        return player.name;
      }
    }
    return id.length > 0 ? id : 'Daphile';
  }

  private serverAddress(): string {
    const server = this.repo.state.server;
    return server === null ? '' : `${server.host}:${server.port}`;
  }

  private chips(): string[] {
    const track = this.liveTrack();
    if (track === null) {
      return [];
    }
    const result: string[] = [];
    const type = track.type.toUpperCase().trim();
    if (type.length > 0) {
      result.push(type);
    }
    if (track.sampleRate.length > 0) {
      result.push(formatRateText(track.sampleRate));
    }
    if (track.sampleSize.length > 0 && track.sampleSize !== '0') {
      result.push(`${track.sampleSize} bit`);
    }
    if (track.bitRate.length > 0 && track.bitRate !== '0') {
      result.push(track.bitRate);
    }
    return result;
  }

  private lyricIndex(): number {
    const lyrics = this.lyrics;
    const state = this.repo.state.nowPlaying;
    if (lyrics === null || lyrics.lines.length === 0) {
      return -1;
    }
    if (!lyrics.synced) {
      return 0;
    }
    const ms = state === null ? 0 : state.time * 1000;
    let index = 0;
    for (let i = 0; i < lyrics.lines.length; i++) {
      if (lyrics.lines[i].timeMs <= ms) {
        index = i;
      } else {
        break;
      }
    }
    return index;
  }

  private lyricText(index: number): string {
    const lyrics = this.lyrics;
    if (lyrics === null || index < 0 || index >= lyrics.lines.length) {
      return '';
    }
    return lyrics.lines[index].text;
  }

  private connectionLabel(): string {
    switch (this.repo.state.connection) {
      case ConnectionState.CONNECTING: return this.s.connecting;
      case ConnectionState.CONNECTED: return this.s.online;
      case ConnectionState.RECONNECTING: return this.s.reconnecting;
      case ConnectionState.LOST: return this.s.serverUnreachable;
      default: return this.s.notConnected;
    }
  }

  private run(action: () => Promise<void>): void {
    action().catch((error: Error) => toast(friendlyErrorMessage(error)));
  }

  private currentCover(np: NowPlaying): string {
    const server = this.repo.state.server;
    if (server === null || np.track === null) {
      return '';
    }
    const local = coverUrl(server, np.track.coverId, np.playerId);
    if (local.length > 0) {
      return local;
    }
    return this.onlineCover;
  }

  private queueCover(track: Track): string {
    const server = this.repo.state.server;
    return server === null ? '' : coverUrl(server, track.coverId, this.repo.state.currentPlayerId);
  }

  private matchCover(): void {
    const track = this.liveTrack();
    if (track === null) {
      return;
    }
    this.showMenu = false;
    this.repo.resolveOnlineCover(track, true).then((url) => {
      toast(url.length > 0 ? this.s.matchedCoverToast : this.s.noOnlineCoverToast);
    }).catch((error: Error) => toast(friendlyErrorMessage(error)));
  }

  @Builder
  FormatChips() {
    Row({ space: 6 }) {
      ForEach(this.chips(), (chip: string) => {
        Text(chip)
          .fontSize(10)
          .fontColor(AppColors.TEXT_SECONDARY)
          .padding({ left: 8, right: 8, top: 4, bottom: 4 })
          .backgroundColor(withAlpha(AppColors.SURFACE_HIGH, 'CC'))
          .borderRadius(5)
      }, (chip: string, index: number) => `${chip}-${index}`)
    }
    .width('100%')
  }

  @Builder
  GlassIcon(kind: UiIconKind, tint: string, action: () => void, name: string) {
    Row() {
      UiIcon({ kind: kind, iconColor: tint, iconSize: 19 })
    }
    .width(38)
    .height(38)
    .justifyContent(FlexAlign.Center)
    .borderRadius(19)
    .backgroundColor(withAlpha(AppColors.TEXT, '0D'))
    .border({ width: 0.5, color: withAlpha(AppColors.TEXT, '1A') })
    .accessibilityText(name)
    .onClick(() => action())
  }

  @Builder
  TopBar() {
    Column({ space: 4 }) {
      Text(this.serverAddress())
        .fontSize(10)
        .fontColor(AppColors.TEXT_SECONDARY)
        .width('100%')
        .maxLines(1)

      Row({ space: 6 }) {
        UiIcon({ kind: UiIconKind.PLAYERS, iconColor: AppColors.PRIMARY, iconSize: 15 })
        Text(this.playerName())
          .fontSize(13)
          .fontColor(AppColors.PRIMARY)
          .maxLines(1)
          .textOverflow({ overflow: TextOverflow.Ellipsis })
          .constraintSize({ maxWidth: 150 })
          .onClick(() => {
            this.showPlayers = !this.showPlayers;
          })
        UiIcon({ kind: UiIconKind.CHEVRON_DOWN, iconColor: AppColors.PRIMARY, iconSize: 12 })

        Blank()

        Row({ space: 8 }) {
          this.GlassIcon(UiIconKind.QUEUE, AppColors.TEXT, () => this.openQueue(), 'queue')
          this.GlassIcon(UiIconKind.LIST, AppColors.TEXT,
            () => this.pathStack.pushPathByName('lyrics', ''), 'lyrics')
          this.GlassIcon(UiIconKind.POWER,
            this.livePlaying() ? AppColors.PRIMARY : AppColors.TEXT,
            () => {
              this.showPower = true;
            }, 'power')
          this.GlassIcon(UiIconKind.MORE, AppColors.TEXT, () => {
            this.showMenu = !this.showMenu;
          }, 'more')
        }
      }
      .width('100%')
      .alignItems(VerticalAlign.Center)
    }
    .width('100%')
  }

  private coverRingSize(): number {
    return this.wide ? 248 : 196;
  }

  private coverImageSize(): number {
    return this.wide ? 210 : 168;
  }

  @Builder
  LyricPanel() {
    Column({ space: this.wide ? 8 : 5 }) {
      if (this.lyrics !== null && this.lyrics.lines.length > 0) {
        if (this.lyrics.synced) {
          if (this.wide && this.lyricText(this.lyricIndex() - 2).length > 0) {
            Text(this.lyricText(this.lyricIndex() - 2))
              .fontSize(13)
              .fontColor(withAlpha(AppColors.TEXT_SECONDARY, '66'))
              .maxLines(1)
              .textOverflow({ overflow: TextOverflow.Ellipsis })
              .width('100%')
              .textAlign(TextAlign.Center)
          }
          if (this.lyricText(this.lyricIndex() - 1).length > 0) {
            Text(this.lyricText(this.lyricIndex() - 1))
              .fontSize(this.wide ? 15 : 11)
              .fontColor(withAlpha(AppColors.TEXT_SECONDARY, 'AA'))
              .maxLines(1)
              .textOverflow({ overflow: TextOverflow.Ellipsis })
              .width('100%')
              .textAlign(this.wide ? TextAlign.Center : TextAlign.Start)
          }
          Text(this.lyricText(this.lyricIndex()))
            .fontSize(this.wide ? 24 : 16)
            .fontWeight(FontWeight.Bold)
            .fontColor(AppColors.TEXT)
            .maxLines(2)
            .textOverflow({ overflow: TextOverflow.Ellipsis })
            .width('100%')
            .textAlign(this.wide ? TextAlign.Center : TextAlign.Start)
          if (this.lyricText(this.lyricIndex() + 1).length > 0) {
            Text(this.lyricText(this.lyricIndex() + 1))
              .fontSize(this.wide ? 15 : 11)
              .fontColor(AppColors.TEXT_SECONDARY)
              .maxLines(2)
              .textOverflow({ overflow: TextOverflow.Ellipsis })
              .width('100%')
              .textAlign(this.wide ? TextAlign.Center : TextAlign.Start)
          }
          if (this.lyricText(this.lyricIndex() + 2).length > 0) {
            Text(this.lyricText(this.lyricIndex() + 2))
              .fontSize(this.wide ? 13 : 11)
              .fontColor(withAlpha(AppColors.TEXT_SECONDARY, '77'))
              .maxLines(2)
              .textOverflow({ overflow: TextOverflow.Ellipsis })
              .width('100%')
              .textAlign(this.wide ? TextAlign.Center : TextAlign.Start)
          }
        } else {
          // 未同步歌词没有时间轴，只做普通摘录展示，避免把第一行误当成当前行
          Text(this.lyricText(0))
            .fontSize(this.wide ? 15 : 11)
            .fontColor(withAlpha(AppColors.TEXT_SECONDARY, 'CC'))
            .maxLines(1)
            .textOverflow({ overflow: TextOverflow.Ellipsis })
            .width('100%')
            .textAlign(this.wide ? TextAlign.Center : TextAlign.Start)
          if (this.lyricText(1).length > 0) {
            Text(this.lyricText(1))
              .fontSize(this.wide ? 15 : 11)
              .fontColor(withAlpha(AppColors.TEXT_SECONDARY, 'CC'))
              .maxLines(1)
              .textOverflow({ overflow: TextOverflow.Ellipsis })
              .width('100%')
              .textAlign(this.wide ? TextAlign.Center : TextAlign.Start)
          }
          if (this.lyricText(2).length > 0) {
            Text(this.lyricText(2))
              .fontSize(this.wide ? 15 : 11)
              .fontColor(withAlpha(AppColors.TEXT_SECONDARY, '99'))
              .maxLines(1)
              .textOverflow({ overflow: TextOverflow.Ellipsis })
              .width('100%')
              .textAlign(this.wide ? TextAlign.Center : TextAlign.Start)
          }
          if (this.lyricText(3).length > 0) {
            Text(this.lyricText(3))
              .fontSize(this.wide ? 15 : 11)
              .fontColor(withAlpha(AppColors.TEXT_SECONDARY, '66'))
              .maxLines(1)
              .textOverflow({ overflow: TextOverflow.Ellipsis })
              .width('100%')
              .textAlign(this.wide ? TextAlign.Center : TextAlign.Start)
          }
        }
      } else {
        Text(this.s.matchingLyrics)
          .fontSize(this.wide ? 14 : 12)
          .fontColor(AppColors.TEXT_SECONDARY)
          .maxLines(2)
          .width('100%')
          .textAlign(this.wide ? TextAlign.Center : TextAlign.Start)
        Text(this.s.noLyricsHint)
          .fontSize(this.wide ? 11 : 10)
          .fontColor(withAlpha(AppColors.TEXT_SECONDARY, '77'))
          .maxLines(2)
          .width('100%')
          .textAlign(this.wide ? TextAlign.Center : TextAlign.Start)
      }

      Row({ space: 4 }) {
        Text('♪')
          .fontSize(this.wide ? 11 : 10)
          .fontColor(AppColors.PRIMARY)
        Text(`${this.s.lyrics} · ${this.lyricsSourceLabel()}`)
          .fontSize(this.wide ? 10 : 9)
          .fontColor(withAlpha(AppColors.PRIMARY, 'AA'))
          .maxLines(1)
      }
      .width('100%')
      .justifyContent(this.wide ? FlexAlign.Center : FlexAlign.Start)
      .margin({ top: 2 })
    }
    .layoutWeight(1)
    .height(this.wide ? 248 : 190)
    .justifyContent(FlexAlign.Center)
    .alignItems(this.wide ? HorizontalAlign.Center : HorizontalAlign.Start)
    .padding({ left: 16, right: 16, top: 12, bottom: 12 })
    .backgroundColor(withAlpha(AppColors.SURFACE, this.wide ? '4D' : '66'))
    .borderRadius(this.wide ? 20 : 18)
    .border({ width: 0.5, color: AppColors.OUTLINE })
    .onClick(() => this.pathStack.pushPathByName('lyrics', ''))
  }

  private lyricsSourceLabel(): string {
    if (this.lyrics === null) {
      return '';
    }
    return this.lyrics.source;
  }

  @Builder
  CoverPanel() {
    Stack({ alignContent: Alignment.Center }) {
      HaloRing({ ringSize: this.coverRingSize(), accent: withAlpha(AppColors.PRIMARY, '88') })
      if (this.liveCover().length > 0) {
        Image(this.liveCover())
          .width(this.coverImageSize())
          .height(this.coverImageSize())
          .objectFit(ImageFit.Cover)
          .borderRadius(this.coverImageSize() / 2)
          .interpolation(ImageInterpolation.Medium)
          .rotate({ angle: this.coverAngle })
      } else {
        Column() {
          Text('♪')
            .fontSize(this.wide ? 54 : 48)
            .fontColor(AppColors.TEXT_SECONDARY)
        }
        .width(this.coverImageSize())
        .height(this.coverImageSize())
        .justifyContent(FlexAlign.Center)
        .backgroundColor(AppColors.SURFACE_HIGH)
        .borderRadius(this.coverImageSize() / 2)
        .rotate({ angle: this.coverAngle })
      }
    }
    .width(this.wide ? 252 : 200)
    .height(this.wide ? 252 : 200)
  }

  @Builder
  ProgressBar() {
    Column({ space: 2 }) {
      Slider({
        value: this.liveTime(),
        min: 0,
        max: Math.max(1, this.liveDuration()),
        step: 1,
        style: SliderStyle.OutSet
      })
        .selectedColor(AppColors.PRIMARY)
        .trackColor(withAlpha(AppColors.TEXT_SECONDARY, '33'))
        .blockColor(AppColors.PRIMARY)
        .enabled(this.liveCanSeek())
        .width('100%')
        .height(32)
        .onChange((value: number, mode: SliderChangeMode) => {
          if (mode === SliderChangeMode.End || mode === SliderChangeMode.Click) {
            this.run(() => this.repo.seekTo(value));
          }
        })

      Row() {
        Text(formatTime(this.liveTime()))
          .fontSize(11)
          .fontColor(AppColors.TEXT_SECONDARY)
        Text(formatTime(this.liveDuration()))
          .fontSize(11)
          .fontColor(AppColors.TEXT_SECONDARY)
      }
      .width('100%')
      .justifyContent(FlexAlign.SpaceBetween)
      .padding({ left: 4, right: 4 })
    }
    .width('100%')
  }

  @Builder
  GlassControl(kind: UiIconKind, tint: string, action: () => void) {
    Row() {
      UiIcon({ kind: kind, iconColor: tint, iconSize: 20 })
    }
    .width(42)
    .height(42)
    .justifyContent(FlexAlign.Center)
    .borderRadius(21)
    .backgroundColor(withAlpha(AppColors.TEXT, '0A'))
    .border({ width: 0.5, color: withAlpha(AppColors.TEXT, '16') })
    .onClick(() => action())
  }

  @Builder
  SecondaryControls() {
    Row({ space: 0 }) {
      this.GlassControl(UiIconKind.SHUFFLE,
        this.liveShuffle() === 1 ? AppColors.PRIMARY : AppColors.TEXT_SECONDARY,
        () => this.run(() => this.repo.setShuffle(this.liveShuffle() !== 1)))
      this.GlassControl(UiIconKind.HEART, AppColors.TEXT_SECONDARY,
        () => this.run(() => this.repo.addCurrentFavorite()))
      this.GlassControl(UiIconKind.SHARE, AppColors.TEXT_SECONDARY, () => this.shareTrack())
      this.GlassControl(UiIconKind.TIMER, AppColors.TEXT_SECONDARY, () => {
        this.showSleep = !this.showSleep;
      })
      this.GlassControl(UiIconKind.EQUALIZER, AppColors.TEXT_SECONDARY,
        () => this.pathStack.pushPathByName('audioOutput', ''))
    }
    .width('100%')
    .justifyContent(FlexAlign.SpaceBetween)
    .padding({ left: 12, right: 12 })
  }

  @Builder
  MainControls() {
    Row({ space: 0 }) {
      Row() {
        TransportIcon({ kind: TransportKind.PREV, iconColor: AppColors.TEXT, iconSize: 26 })
      }
      .width(48)
      .height(48)
      .justifyContent(FlexAlign.Center)
      .onClick(() => this.run(() => this.repo.previous()))

      Button({ type: ButtonType.Circle }) {
        TransportIcon({
          kind: this.livePlaying() ? TransportKind.PAUSE : TransportKind.PLAY,
          iconColor: AppColors.ON_PRIMARY,
          iconSize: 28
        })
      }
      .width(68)
      .height(68)
      .backgroundColor(AppColors.PRIMARY)
      .shadow({ radius: 20, color: withAlpha(AppColors.PRIMARY, '55'), offsetY: 4 })
      .onClick(() => this.run(() => this.repo.playPause()))

      Row() {
        TransportIcon({ kind: TransportKind.NEXT, iconColor: AppColors.TEXT, iconSize: 26 })
      }
      .width(48)
      .height(48)
      .justifyContent(FlexAlign.Center)
      .onClick(() => this.run(() => this.repo.next()))

      Row() {
        TransportIcon({
          kind: this.liveRepeat() === 2 ? TransportKind.REPEAT_ONE : TransportKind.REPEAT,
          iconColor: this.liveRepeat() > 0 ? AppColors.PRIMARY : AppColors.TEXT_SECONDARY,
          iconSize: 21
        })
      }
      .width(44)
      .height(44)
      .justifyContent(FlexAlign.Center)
      .onClick(() => this.run(() => this.repo.setRepeat((this.liveRepeat() + 1) % 3)))

      Row() {
        TransportIcon({
          kind: this.liveMuted() ? TransportKind.MUTE : TransportKind.VOLUME,
          iconColor: AppColors.TEXT_SECONDARY,
          iconSize: 21
        })
      }
      .width(44)
      .height(44)
      .justifyContent(FlexAlign.Center)
      .onClick(() => this.run(() => this.repo.toggleMute()))
    }
    .width('100%')
    .justifyContent(FlexAlign.SpaceBetween)
    .alignItems(VerticalAlign.Center)
    .padding({ left: 6, right: 6 })
  }

  @Builder
  VolumeArea() {
    if (this.liveCanSetVolume()) {
      Row({ space: 10 }) {
        TransportIcon({
          kind: this.liveMuted() ? TransportKind.MUTE : TransportKind.VOLUME,
          iconColor: AppColors.TEXT_SECONDARY,
          iconSize: 16
        })
        Slider({ value: this.liveVolume(), min: 0, max: 100, step: 1, style: SliderStyle.OutSet })
          .selectedColor(AppColors.PRIMARY)
          .trackColor(withAlpha(AppColors.TEXT_SECONDARY, '44'))
          .blockColor(AppColors.PRIMARY)
          .layoutWeight(1)
          .onChange((value: number, mode: SliderChangeMode) => {
            if (mode === SliderChangeMode.End || mode === SliderChangeMode.Click) {
              this.run(() => this.repo.setVolume(value));
            }
          })
        Text(`${this.liveVolume()}`)
          .fontSize(10)
          .fontColor(AppColors.TEXT_SECONDARY)
          .width(24)
          .textAlign(TextAlign.End)
      }
      .width('100%')
      .padding({ left: 12, right: 12 })
    } else {
      Row({ space: 8 }) {
        TransportIcon({ kind: TransportKind.MUTE, iconColor: AppColors.TEXT_SECONDARY, iconSize: 15 })
        Column({ space: 1 }) {
          Text(this.s.fixedVolume)
            .fontSize(11)
            .fontWeight(FontWeight.Medium)
            .fontColor(AppColors.TEXT_SECONDARY)
          Text(this.s.volumeDisabledHint)
            .fontSize(9)
            .fontColor(withAlpha(AppColors.TEXT_SECONDARY, 'AA'))
            .maxLines(2)
          Text(this.s.independentPlayer)
            .fontSize(9)
            .fontColor(withAlpha(AppColors.TEXT_SECONDARY, '88'))
        }
        .alignItems(HorizontalAlign.Start)
        Blank()
        Text(this.connectionLabel())
          .fontSize(9)
          .fontColor(this.repo.state.connection === ConnectionState.CONNECTED
            ? AppColors.PRIMARY : AppColors.WARN)
      }
      .width('100%')
      .padding({ left: 12, right: 12 })
    }
  }

  private shareTrack(): void {
    const track = this.liveTrack();
    if (track === null) {
      return;
    }
    const text = `${track.title} - ${track.artist} (${track.album})`;
    toast(text);
  }

  @Builder
  MoreMenu() {
    Column() {
      Column() {
        this.MenuItem(UiIconKind.EQUALIZER, this.s.audioInfo, () => {
          this.showMenu = false;
          this.pathStack.pushPathByName('audioOutput', '');
        })
        this.MenuItem(UiIconKind.MATCH, this.s.matchCover, () => this.matchCover())
        this.MenuItem(UiIconKind.TIMER, this.s.sleepTimer, () => {
          this.showMenu = false;
          this.showSleep = true;
        })
        this.MenuItem(UiIconKind.HEART, this.s.favoriteTrack, () => {
          this.showMenu = false;
          this.run(() => this.repo.addCurrentFavorite());
        })
        this.MenuItem(UiIconKind.QUEUE, this.s.saveQueue, () => {
          this.showMenu = false;
          this.run(async () => {
            await this.repo.saveQueue(`Queue ${new Date().getHours()}:${new Date().getMinutes()}`);
            toast(this.s.saved);
          });
        })
        this.MenuItem(UiIconKind.STOP, this.s.stop, () => {
          this.showMenu = false;
          this.run(() => this.repo.stop());
        })
        this.MenuItem(UiIconKind.TRASH, this.s.clearQueue, () => {
          this.showMenu = false;
          this.run(() => this.repo.clearQueue());
        }, AppColors.ERROR)
      }
      .width(250)
      .padding({ top: 6, bottom: 6 })
      .backgroundColor(withAlpha(AppColors.SURFACE_HIGH, 'FA'))
      .borderRadius(4)
      .border({ width: 0.5, color: AppColors.OUTLINE })
      .shadow({ radius: 24, color: '#66000000', offsetY: 6 })
    }
    .width('100%')
    .alignItems(HorizontalAlign.End)
    .padding({ right: 12, top: 84 })
  }

  @Builder
  MenuItem(icon: UiIconKind, label: string, action: () => void, color: string = AppColors.TEXT) {
    Row({ space: 16 }) {
      UiIcon({ kind: icon, iconColor: color, iconSize: 19 })
      Text(label)
        .fontSize(14)
        .fontColor(color)
        .layoutWeight(1)
        .maxLines(1)
    }
    .width('100%')
    .height(46)
    .padding({ left: 18, right: 18 })
    .onClick(() => action())
  }

  @Builder
  SleepMenu() {
    Column() {
      Column() {
        Text(this.s.sleepTimer)
          .fontSize(16)
          .fontWeight(FontWeight.Medium)
          .fontColor(AppColors.TEXT)
          .width('100%')
          .padding({ left: 18, right: 18, top: 14, bottom: 6 })
        this.SleepOption(15)
        this.SleepOption(30)
        this.SleepOption(60)
        this.SleepOption(90)
        Row() {
          Text(this.s.cancel)
            .fontSize(14)
            .fontColor(AppColors.PRIMARY)
        }
        .width('100%')
        .height(44)
        .justifyContent(FlexAlign.End)
        .padding({ right: 18 })
        .onClick(() => {
          this.showSleep = false;
          this.run(() => this.repo.cancelSleepTimer());
        })
      }
      .width(240)
      .backgroundColor(withAlpha(AppColors.SURFACE_HIGH, 'FA'))
      .borderRadius(14)
      .border({ width: 0.5, color: AppColors.OUTLINE })
      .shadow({ radius: 24, color: '#66000000', offsetY: 6 })
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
    .alignItems(HorizontalAlign.Center)
    .backgroundColor('#66000000')
    .onClick(() => {
      this.showSleep = false;
    })
  }

  @Builder
  SleepOption(minutes: number) {
    Row() {
      Text(minutes === 30 ? `${minutes} min` : `${minutes} min`)
        .fontSize(14)
        .fontColor(AppColors.TEXT)
    }
    .width('100%')
    .height(44)
    .padding({ left: 18, right: 18 })
    .onClick(() => {
      this.showSleep = false;
      this.run(() => this.repo.startSleepTimer(minutes, SleepAction.PAUSE));
    })
  }

  private livePower(): boolean {
    const state = this.repo.state.nowPlaying;
    return state === null ? true : state.power;
  }

  private currentPlayerName(): string {
    for (const player of this.repo.state.players) {
      if (player.id === this.repo.state.currentPlayerId) {
        return player.name;
      }
    }
    return this.s.playerLabel;
  }

  private powerLabel(action: HostPowerAction): string {
    switch (action) {
      case HostPowerAction.RESTART: return this.s.hostRestart;
      case HostPowerAction.SHUTDOWN: return this.s.hostShutdown;
      case HostPowerAction.SUSPEND: return this.s.hostSuspend;
      default: return this.s.hostPower;
    }
  }

  private confirmHostPower(action: HostPowerAction): void {
    this.showPower = false;
    promptAction.showDialog({
      title: this.powerLabel(action),
      message: this.s.hostPowerHint,
      buttons: [
        { text: this.s.cancel, color: AppColors.TEXT_SECONDARY },
        { text: this.s.confirm, color: action === HostPowerAction.SHUTDOWN
          ? AppColors.ERROR : AppColors.PRIMARY }
      ]
    }).then((result) => {
      if (result.index === 1) {
        this.run(async () => {
          await this.repo.hostPower(action);
          toast(this.s.hostCommandSent);
        });
      }
    }).catch(() => {
    });
  }

  @Builder
  PowerAction(label: string, danger: boolean, action: HostPowerAction) {
    Row() {
      Text(label)
        .fontSize(12)
        .fontColor(danger ? AppColors.ERROR : AppColors.TEXT)
        .maxLines(1)
    }
    .height(36)
    .layoutWeight(1)
    .justifyContent(FlexAlign.Center)
    .backgroundColor(AppColors.SURFACE)
    .borderRadius(18)
    .border({ width: 0.5, color: danger ? withAlpha(AppColors.ERROR, '88') : AppColors.OUTLINE })
    .onClick(() => this.confirmHostPower(action))
  }

  @Builder
  PowerOverlay() {
    Column() {
      Column({ space: 10 }) {
        Text(this.s.power)
          .fontSize(16)
          .fontWeight(FontWeight.Medium)
          .fontColor(AppColors.TEXT)
          .width('100%')
          .padding({ left: 18, right: 18, top: 14 })

        Row({ space: 10 }) {
          Column({ space: 2 }) {
            Text(this.s.playerLabel)
              .fontSize(14)
              .fontColor(AppColors.TEXT)
            Text(this.currentPlayerName())
              .fontSize(11)
              .fontColor(AppColors.TEXT_SECONDARY)
              .maxLines(1)
              .textOverflow({ overflow: TextOverflow.Ellipsis })
          }
          .alignItems(HorizontalAlign.Start)
          .layoutWeight(1)

          Toggle({ type: ToggleType.Switch, isOn: this.livePower() })
            .selectedColor(AppColors.PRIMARY)
            .onChange((on: boolean) => {
              this.repo.setPlayerPower(this.repo.state.currentPlayerId, on)
                .catch((error: Error) => toast(friendlyErrorMessage(error)));
            })
        }
        .width('100%')
        .padding({ left: 18, right: 18 })

        Divider()
          .color(AppColors.OUTLINE)
          .margin({ left: 18, right: 18 })

        Text(this.s.hostPower)
          .fontSize(13)
          .fontWeight(FontWeight.Medium)
          .fontColor(AppColors.PRIMARY)
          .width('100%')
          .padding({ left: 18, right: 18 })

        Text(this.s.hostPowerHint)
          .fontSize(11)
          .fontColor(AppColors.TEXT_SECONDARY)
          .width('100%')
          .padding({ left: 18, right: 18 })

        Row({ space: 8 }) {
          this.PowerAction(this.s.reboot, false, HostPowerAction.RESTART)
          this.PowerAction(this.s.shutdown, true, HostPowerAction.SHUTDOWN)
          this.PowerAction(this.s.suspend, false, HostPowerAction.SUSPEND)
        }
        .width('100%')
        .padding({ left: 18, right: 18 })

        Row() {
          Text(this.s.cancel)
            .fontSize(14)
            .fontColor(AppColors.PRIMARY)
        }
        .width('100%')
        .height(42)
        .justifyContent(FlexAlign.End)
        .padding({ right: 18 })
        .onClick(() => {
          this.showPower = false;
        })
      }
      .width(300)
      .backgroundColor(withAlpha(AppColors.SURFACE_HIGH, 'FA'))
      .borderRadius(16)
      .border({ width: 0.5, color: AppColors.OUTLINE })
      .shadow({ radius: 24, color: '#66000000', offsetY: 6 })
      .padding({ bottom: 4 })
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
    .alignItems(HorizontalAlign.Center)
    .backgroundColor('#66000000')
    .onClick(() => {
      this.showPower = false;
    })
  }

  @Builder
  QueueSheet() {
    Column() {
      Column() {
        Row() {
          Text(`${this.s.queue} · ${this.liveQueue().length}`)
            .fontSize(16)
            .fontWeight(FontWeight.Medium)
            .fontColor(AppColors.TEXT)
            .layoutWeight(1)
          Text(this.s.clear)
            .fontSize(12)
            .fontColor(AppColors.ERROR)
            .onClick(() => this.run(() => this.repo.clearQueue()))
          Text(this.s.close)
            .fontSize(12)
            .fontColor(AppColors.TEXT_SECONDARY)
            .margin({ left: 16 })
            .onClick(() => {
              this.showQueue = false;
            })
        }
        .width('100%')
        .padding({ left: 16, right: 16, top: 16, bottom: 8 })

        if (this.liveQueue().length === 0) {
          Text(this.s.queueEmptyBody)
            .fontSize(12)
            .fontColor(AppColors.TEXT_SECONDARY)
            .padding(24)
        } else {
          List({ space: 4, scroller: this.queueScroller }) {
            ForEach(this.liveQueue(), (track: Track) => {
              ListItem() {
                TrackRow({
                  track: track,
                  coverUrl: this.queueCover(track),
                  isCurrent: this.liveCurrentIndex() === track.index,
                  onTap: () => this.run(() => this.repo.playQueueIndex(track.index)),
                  onLongPress: () => this.run(() => this.repo.removeQueueItem(track.index))
                })
              }
            }, (track: Track) => `${track.index}-${track.id}-${track.title}`)
          }
          .width('100%')
          .layoutWeight(1)
          .padding({ left: 12, right: 12, bottom: 12 })
          .scrollBar(BarState.Off)
        }
      }
      .width('100%')
      .constraintSize({ maxWidth: this.wide ? 640 : 10000 })
      .height('68%')
      .backgroundColor(AppColors.SURFACE)
      .borderRadius({ topLeft: 20, topRight: 20 })
      .padding({ bottom: 10 })
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.End)
    .backgroundColor('#66000000')
    .onClick(() => {
      this.showQueue = false;
    })
  }

  @Builder
  PlayerPicker() {
    Column() {
      Column() {
        ForEach(this.repo.state.players, (player: Player) => {
          Row({ space: 10 }) {
            UiIcon({
              kind: UiIconKind.PLAYERS,
              iconColor: player.id === this.repo.state.currentPlayerId ? AppColors.PRIMARY : AppColors.TEXT_SECONDARY,
              iconSize: 17
            })
            Text(player.name)
              .fontSize(14)
              .fontColor(player.id === this.repo.state.currentPlayerId ? AppColors.PRIMARY : AppColors.TEXT)
              .layoutWeight(1)
              .maxLines(1)
              .textOverflow({ overflow: TextOverflow.Ellipsis })
            if (player.id === this.repo.state.currentPlayerId) {
              UiIcon({ kind: UiIconKind.CHECK, iconColor: AppColors.PRIMARY, iconSize: 16 })
            }
          }
          .width('100%')
          .height(44)
          .padding({ left: 14, right: 14 })
          .onClick(() => {
            this.showPlayers = false;
            this.repo.selectPlayer(player.id);
          })
        }, (player: Player) => player.id)
      }
      .width(250)
      .padding({ top: 6, bottom: 6 })
      .backgroundColor(withAlpha(AppColors.SURFACE_HIGH, 'FA'))
      .borderRadius(10)
      .border({ width: 0.5, color: AppColors.OUTLINE })
      .shadow({ radius: 24, color: '#66000000', offsetY: 6 })
    }
    .width('100%')
    .alignItems(HorizontalAlign.Start)
    .padding({ left: 16, top: 102 })
  }

  @Builder
  PlayerContent() {
    Column({ space: 0 }) {
      this.TopBar()

      Column({ space: 4 }) {
        Row({ space: 8 }) {
          Text(this.liveTitle())
            .fontSize(this.wide ? 30 : 26)
            .fontWeight(FontWeight.Bold)
            .fontColor(AppColors.TEXT)
            .maxLines(1)
            .textOverflow({ overflow: TextOverflow.Ellipsis })
            .layoutWeight(1)
          if (this.liveQuality() !== null) {
            QualityBadge({ quality: this.liveQuality() as TrackQuality })
          }
        }
        .width('100%')

        Text(this.liveSubtitle())
          .fontSize(this.wide ? 13 : 12)
          .fontColor(AppColors.TEXT_SECONDARY)
          .maxLines(1)
          .textOverflow({ overflow: TextOverflow.Ellipsis })
          .width('100%')
      }
      .width('100%')
      .margin({ top: 6 })

      this.FormatChips()

      Row({ space: this.wide ? 20 : 12 }) {
        this.CoverPanel()
        this.LyricPanel()
      }
      .width('100%')
      .alignItems(VerticalAlign.Center)
      .margin({ top: 12 })

      ThemeSceneView({
        kind: ThemeRuntime.instance.art.scene,
        accent: AppColors.PRIMARY,
        playing: this.livePlaying(),
        progress: this.playProgress()
      })
        .width('100%')
        .height(52)
        .margin({ top: 2 })

      this.ProgressBar()

      this.SecondaryControls()

      this.MainControls()

      this.VolumeArea()
    }
    .width('100%')
    .padding({ left: 16, right: 16, top: 8, bottom: 14 })
  }

  build() {
    Stack({ alignContent: Alignment.TopStart }) {
      ThemeBackdropView({
        kind: ThemeRuntime.instance.art.backdrop,
        accent: AppColors.PRIMARY,
        active: this.repo.state.nowPlaying !== null &&
          nowPlayingIsPlaying(this.repo.state.nowPlaying as NowPlaying),
        progress: this.playProgress()
      })
        .width('100%')
        .height('100%')

      if (this.repo.state.nowPlaying === null) {
        EmptyState({
          title: this.repo.state.server === null ? this.s.noServerTitle : this.s.noContent,
          body: this.repo.state.server === null ? this.s.noServerBody : this.s.selectMusic
        })
      } else {
        Scroll() {
          Row() {
            Column() {
              this.PlayerContent()
            }
            .width('100%')
            .constraintSize({ maxWidth: 900 })
          }
          .width('100%')
          .justifyContent(FlexAlign.Center)
        }
        .width('100%')
        .height('100%')
        .scrollBar(BarState.Off)
      }

      if (this.showMenu) {
        Column()
          .width('100%')
          .height('100%')
          .backgroundColor(Color.Transparent)
          .onClick(() => {
            this.showMenu = false;
          })
        this.MoreMenu()
      }

      if (this.showPlayers) {
        Column()
          .width('100%')
          .height('100%')
          .backgroundColor(Color.Transparent)
          .onClick(() => {
            this.showPlayers = false;
          })
        this.PlayerPicker()
      }

      if (this.showSleep) {
        this.SleepMenu()
      }

      if (this.showPower) {
        this.PowerOverlay()
      }

      if (this.showQueue) {
        this.QueueSheet()
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor(AppColors.BACKGROUND)
  }
}
