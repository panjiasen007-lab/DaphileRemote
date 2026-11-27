import { promptAction } from '@kit.ArkUI';
import { pasteboard } from '@kit.BasicServicesKit';
import { AppContext } from '../../app/AppContext';
import {
  ConnectionState, NowPlaying, Player, ServerConfig, Track, coverUrl
} from '../../data/model/Models';
import { DaphileRepository } from '../../data/repo/DaphileRepository';
import { AppColors, formatTime, withAlpha } from '../theme/Theme';
import { CoverArt } from '../components/Common';
import { UiIcon, UiIconKind } from '../components/UiIcon';
import { stringsFor, UiStrings } from '../i18n/I18n';

function toast(message: string): void {
  promptAction.showToast({ message: message, duration: 1800 });
}

function decodeLmsPath(url: string): string {
  if (url.length === 0) {
    return '';
  }
  let value = url;
  for (let i = 0; i < 2; i++) {
    try {
      value = decodeURIComponent(value);
    } catch (error) {
      break;
    }
  }
  if (value.startsWith('file://')) {
    value = value.substring(7);
  }
  return value;
}

function formatTypeLabel(type: string): string {
  switch (type.toLowerCase()) {
    case 'flc': return 'FLAC';
    case 'dsf': return 'DSF';
    case 'dff': return 'DFF';
    case 'wav': return 'WAV';
    case 'aif':
    case 'aiff': return 'AIFF';
    case 'mp3': return 'MP3';
    case 'aac': return 'AAC';
    case 'm4a': return 'M4A';
    case 'ogg': return 'OGG';
    case 'opus': return 'OPUS';
    default: return type.toUpperCase();
  }
}

function dsdLabel(type: string, rate: string): string {
  const numeric = Number.parseFloat(rate);
  const isDsd = type.toLowerCase().includes('dsd') || type.toLowerCase() === 'dsf' ||
    type.toLowerCase() === 'dff' || (!Number.isNaN(numeric) && numeric >= 2000000);
  if (!isDsd) {
    return '';
  }
  const multiple = Number.isNaN(numeric) ? 0 : Math.trunc(numeric / 2822400);
  switch (multiple) {
    case 1: return 'DSD64';
    case 2: return 'DSD128';
    case 4: return 'DSD256';
    case 8: return 'DSD512';
    default: return 'DSD';
  }
}

function formatRate(rate: string): string {
  const numeric = Number.parseFloat(rate);
  if (Number.isNaN(numeric)) {
    return rate.length > 0 ? rate : '—';
  }
  if (numeric >= 1000000) {
    return `${(numeric / 1000000).toFixed(1)} MHz`;
  }
  if (numeric >= 1000) {
    const khz = numeric / 1000;
    return khz % 1 === 0 ? `${Math.trunc(khz)} kHz` : `${khz.toFixed(1)} kHz`;
  }
  return `${rate} Hz`;
}

function copyToClipboard(text: string, copiedMessage: string): void {
  try {
    const data = pasteboard.createData(pasteboard.MIMETYPE_TEXT_PLAIN, text);
    pasteboard.getSystemPasteboard().setData(data).then(() => {
      toast(copiedMessage);
    }).catch(() => {
    });
  } catch (error) {
  }
}

@ComponentV2
export struct AudioOutputPage {
  @Param pathStack: NavPathStack = new NavPathStack();
  private repo: DaphileRepository = AppContext.get().repository;

  @Computed
  get s(): UiStrings {
    const settings = this.repo.state.settings;
    return stringsFor(settings.theme.length > 0 ? settings.language : 'en');
  }

  private np(): NowPlaying | null {
    return this.repo.state.nowPlaying;
  }

  private track(): Track | null {
    const np = this.np();
    return np === null ? null : np.track;
  }

  private player(): Player | null {
    const np = this.np();
    const id = np !== null && np.playerId.length > 0 ? np.playerId : this.repo.state.currentPlayerId;
    for (const player of this.repo.state.players) {
      if (player.id === id) {
        return player;
      }
    }
    return null;
  }

  private cover(): string {
    const server = this.repo.state.server;
    const track = this.track();
    if (server === null || track === null) {
      return '';
    }
    const np = this.np();
    return coverUrl(server, track.coverId, np === null ? '' : np.playerId);
  }

  private playbackLabel(): string {
    const np = this.np();
    if (np === null) {
      return this.s.stoppedState;
    }
    if (!np.power) {
      return this.s.playerOff;
    }
    if (np.mode === 'play') {
      return this.s.playingState;
    }
    if (np.mode === 'pause') {
      return this.s.pausedToast;
    }
    return this.s.stoppedState;
  }

  private formatSummary(track: Track): string {
    const type = formatTypeLabel(track.type);
    const dsd = dsdLabel(track.type, track.sampleRate);
    if (type.length === 0) {
      return dsd;
    }
    return dsd.length > 0 ? `${type} / ${dsd}` : type;
  }

  private specSummary(track: Track): string {
    const parts: string[] = [];
    if (track.sampleRate.length > 0) {
      parts.push(formatRate(track.sampleRate));
    }
    if (track.sampleSize.length > 0 && track.sampleSize !== '0') {
      parts.push(`${track.sampleSize} bit`);
    }
    if (track.bitRate.length > 0 && track.bitRate !== '0') {
      parts.push(track.bitRate);
    }
    if (track.channels > 0) {
      parts.push(`${track.channels} ch`);
    }
    return parts.join(' · ');
  }

  @Builder
  Header(title: string) {
    Row({ space: 6 }) {
      Row() {
        UiIcon({ kind: UiIconKind.BACK, iconColor: AppColors.TEXT, iconSize: 22 })
      }
      .width(40)
      .height(40)
      .justifyContent(FlexAlign.Center)
      .onClick(() => this.pathStack.pop())
      Text(title)
        .fontSize(24)
        .fontWeight(FontWeight.Bold)
        .fontColor(AppColors.TEXT)
        .layoutWeight(1)
        .maxLines(1)
        .textOverflow({ overflow: TextOverflow.Ellipsis })
    }
    .width('100%')
    .padding({ left: 8, right: 16, top: 8, bottom: 4 })
  }

  @Builder
  SectionTitle(title: string) {
    Text(title)
      .fontSize(14)
      .fontWeight(FontWeight.Medium)
      .fontColor(AppColors.PRIMARY)
      .width('100%')
      .padding({ left: 2, top: 6, bottom: 2 })
  }

  @Builder
  InfoRow(kind: UiIconKind, label: string, value: string) {
    Row({ space: 8 }) {
      Row() {
        UiIcon({ kind, iconColor: AppColors.TEXT_SECONDARY, iconSize: 15 })
      }
      .width(20)
      .height(22)
      .justifyContent(FlexAlign.Center)
      Text(label)
        .fontSize(13)
        .fontColor(AppColors.TEXT_SECONDARY)
        .width(88)
      Text(value.length > 0 ? value : '—')
        .fontSize(13)
        .fontWeight(FontWeight.Medium)
        .fontColor(AppColors.TEXT)
        .layoutWeight(1)
        .maxLines(3)
        .textOverflow({ overflow: TextOverflow.Ellipsis })
    }
    .width('100%')
    .padding({ top: 7, bottom: 7 })
    .alignItems(VerticalAlign.Top)
  }

  @Builder
  Card() {
    Column() {
      if (this.track() === null) {
        Row({ space: 12 }) {
          CoverArt({ url: '', coverSize: 56, radius: 12 })
          Text(this.s.notPlayingTitle)
            .fontSize(15)
            .fontColor(AppColors.TEXT)
            .layoutWeight(1)
        }
        .width('100%')
        .padding(14)
      } else {
        Row({ space: 12 }) {
          CoverArt({ url: this.cover(), coverSize: 56, radius: 12 })
          Column({ space: 3 }) {
            Text((this.track() as Track).title)
              .fontSize(15)
              .fontWeight(FontWeight.Medium)
              .fontColor(AppColors.TEXT)
              .maxLines(1)
              .textOverflow({ overflow: TextOverflow.Ellipsis })
            Text([(this.track() as Track).artist, (this.track() as Track).album]
              .filter((value: string) => value.length > 0).join(' · '))
              .fontSize(11)
              .fontColor(AppColors.TEXT_SECONDARY)
              .maxLines(1)
              .textOverflow({ overflow: TextOverflow.Ellipsis })
            Row({ space: 5 }) {
              Column()
                .width(6)
                .height(6)
                .borderRadius(3)
                .backgroundColor(AppColors.PRIMARY)
              Text(this.playbackLabel())
                .fontSize(11)
                .fontColor(AppColors.PRIMARY)
            }
            .alignItems(VerticalAlign.Center)
          }
          .alignItems(HorizontalAlign.Start)
          .layoutWeight(1)
        }
        .width('100%')
        .padding(14)
      }
    }
    .width('100%')
    .backgroundColor(withAlpha(AppColors.SURFACE, 'CC'))
    .borderRadius(16)
    .border({ width: 0.5, color: AppColors.OUTLINE })
  }

  build() {
    Column() {
      this.Header(this.s.audioOutputScreenTitle)
      Scroll() {
        Column({ space: 12 }) {
          this.Card()

          this.SectionTitle(this.s.audioSource)
          Column() {
            if (this.track() !== null) {
              Row({ space: 8 }) {
                Row() {
                  UiIcon({ kind: UiIconKind.FOLDER, iconColor: AppColors.TEXT_SECONDARY, iconSize: 15 })
                }
                .width(20)
                .height(22)
                .justifyContent(FlexAlign.Center)
                Text(this.s.path)
                  .fontSize(13)
                  .fontColor(AppColors.TEXT_SECONDARY)
                  .width(88)
                Text(decodeLmsPath((this.track() as Track).url).length > 0
                  ? decodeLmsPath((this.track() as Track).url) : '—')
                  .fontSize(13)
                  .fontColor(AppColors.TEXT)
                  .layoutWeight(1)
                  .maxLines(3)
                  .textOverflow({ overflow: TextOverflow.Ellipsis })
                if (decodeLmsPath((this.track() as Track).url).length > 0) {
                  Row() {
                    UiIcon({ kind: UiIconKind.COPY, iconColor: AppColors.TEXT_SECONDARY, iconSize: 15 })
                  }
                  .width(26)
                  .height(26)
                  .justifyContent(FlexAlign.Center)
                  .onClick(() => copyToClipboard(decodeLmsPath((this.track() as Track).url),
                    this.s.pathCopied))
                }
              }
              .width('100%')
              .padding({ top: 7, bottom: 7 })
              .alignItems(VerticalAlign.Top)

              this.InfoRow(UiIconKind.EQUALIZER, this.s.formatLabel, this.formatSummary(this.track() as Track))
              this.InfoRow(UiIconKind.EQUALIZER, this.s.sourceSampleRate,
                formatRate((this.track() as Track).sampleRate))
              this.InfoRow(UiIconKind.EQUALIZER, this.s.bitDepth,
                (this.track() as Track).sampleSize.length > 0 && (this.track() as Track).sampleSize !== '0'
                  ? ((this.track() as Track).sampleSize === '1' &&
                    dsdLabel((this.track() as Track).type, (this.track() as Track).sampleRate).length > 0
                    ? this.s.bitDsd : `${(this.track() as Track).sampleSize} bit`)
                  : '—')
              this.InfoRow(UiIconKind.EQUALIZER, this.s.bitRate,
                (this.track() as Track).bitRate.length > 0 && (this.track() as Track).bitRate !== '0'
                  ? (this.track() as Track).bitRate : '—')
              if ((this.track() as Track).channels > 0) {
                this.InfoRow(UiIconKind.SLIDERS, this.s.channels, `${(this.track() as Track).channels}`)
              }
              this.InfoRow(UiIconKind.INFO, this.s.lossless,
                (this.track() as Track).lossless ? this.s.yesValue : this.s.noOrUnknown)
            } else {
              this.InfoRow(UiIconKind.FOLDER, this.s.path, '—')
            }
          }
          .width('100%')
          .padding({ left: 14, right: 14, top: 4, bottom: 6 })
          .backgroundColor(AppColors.SURFACE)
          .borderRadius(16)

          this.SectionTitle(this.s.playersAndOutput)
          Column() {
            this.InfoRow(UiIconKind.VOLUME, this.s.outputDevice,
              this.player() !== null ? (this.player() as Player).name : '—')
            this.InfoRow(UiIconKind.EXTERNAL, this.s.deviceModel,
              this.player() !== null && (this.player() as Player).model.length > 0
                ? (this.player() as Player).model : 'Squeezelite / Daphile')
            this.InfoRow(UiIconKind.SLIDERS, this.s.outputEngine, this.s.outputEngineValue)
            this.InfoRow(UiIconKind.EQUALIZER, this.s.outputSpec,
              this.track() !== null
                ? (dsdLabel((this.track() as Track).type, (this.track() as Track).sampleRate).length > 0
                  ? this.s.outputSpecDsd : this.s.outputSpecAuto)
                : '—')
            this.InfoRow(UiIconKind.VOLUME, this.s.volume, this.volumeText())
            this.InfoRow(UiIconKind.INFO, this.s.lmsVersion,
              this.repo.state.serverInfo !== null ? this.repo.state.serverInfo.version : '—')
          }
          .width('100%')
          .padding({ left: 14, right: 14, top: 4, bottom: 6 })
          .backgroundColor(AppColors.SURFACE)
          .borderRadius(16)

          this.SectionTitle(this.s.realtimeStatus)
          Column() {
            this.InfoRow(UiIconKind.CLOCK, this.s.elapsed, this.np() !== null
              ? formatTime((this.np() as NowPlaying).time) : '—')
            this.InfoRow(UiIconKind.TIMER, this.s.totalDuration, this.np() !== null
              ? formatTime((this.np() as NowPlaying).duration) : '—')
            this.InfoRow(UiIconKind.QUEUE, this.s.queuePosition, this.queuePositionText())
            this.InfoRow(UiIconKind.EXTERNAL, this.s.dataSource, this.dataSourceText())
            if (this.track() !== null && this.specSummary(this.track() as Track).length > 0) {
              this.InfoRow(UiIconKind.EQUALIZER, this.s.specSummary, this.specSummary(this.track() as Track))
            }
          }
          .width('100%')
          .padding({ left: 14, right: 14, top: 4, bottom: 6 })
          .backgroundColor(AppColors.SURFACE)
          .borderRadius(16)

          Row({ space: 8 }) {
            Text('♪')
              .fontSize(14)
              .fontColor(AppColors.PRIMARY)
            Text(this.s.audioOutputNote)
              .fontSize(11)
              .fontColor(AppColors.TEXT_SECONDARY)
              .layoutWeight(1)
          }
          .width('100%')
          .padding(12)
          .backgroundColor(withAlpha(AppColors.PRIMARY, '14'))
          .borderRadius(12)
        }
        .width('100%')
        .padding({ left: 16, right: 16, bottom: 24 })
      }
      .width('100%')
      .layoutWeight(1)
      .scrollBar(BarState.Off)
      .align(Alignment.TopStart)
    }
    .width('100%')
    .height('100%')
    .backgroundColor(AppColors.BACKGROUND)
  }

  private volumeText(): string {
    const np = this.np();
    if (np === null) {
      return '—';
    }
    if (!np.canSetVolume) {
      return this.s.fixedLevel.replace('%d', `${np.volume}`);
    }
    return `${np.volume} / 100`;
  }

  private queuePositionText(): string {
    const np = this.np();
    if (np === null || np.trackCount <= 0) {
      return '—';
    }
    return `${np.currentIndex + 1} / ${np.trackCount}`;
  }

  private dataSourceText(): string {
    const track = this.track();
    if (track === null) {
      return '—';
    }
    return track.url.startsWith('http') ? this.s.networkStream : this.s.localFile;
  }
}

@ComponentV2
export struct SystemInfoPage {
  @Param pathStack: NavPathStack = new NavPathStack();
  private repo: DaphileRepository = AppContext.get().repository;

  @Computed
  get s(): UiStrings {
    const settings = this.repo.state.settings;
    return stringsFor(settings.theme.length > 0 ? settings.language : 'en');
  }

  private connectionLabel(): string {
    switch (this.repo.state.connection) {
      case ConnectionState.CONNECTING: return this.s.connecting;
      case ConnectionState.CONNECTED: return this.s.online;
      case ConnectionState.RECONNECTING: return this.s.reconnecting;
      case ConnectionState.LOST: return this.s.unreachable;
      default: return this.s.notConnected;
    }
  }

  private copyPayload(): string {
    const server = this.repo.state.server;
    const info = this.repo.state.serverInfo;
    const lines: string[] = [];
    lines.push(this.s.systemInfoCopyTitle);
    lines.push(this.s.systemInfoCopyServer
      .replace('%s', server !== null ? server.name : '')
      .replace('%s', server !== null ? server.host : '')
      .replace('%d', server !== null ? `${server.port}` : '0'));
    lines.push(this.s.systemInfoCopyVersion.replace('%s', info !== null ? info.version : ''));
    lines.push(this.s.systemInfoCopyState.replace('%s', this.connectionLabel()));
    lines.push(this.s.systemInfoCopyPlayers.replace('%d', `${this.repo.state.players.length}`));
    lines.push(this.s.systemInfoCopyLibrary
      .replace('%d', info !== null ? `${info.albumCount}` : '0')
      .replace('%d', info !== null ? `${info.songCount}` : '0')
      .replace('%d', info !== null ? `${info.artistCount}` : '0'));
    lines.push(this.s.systemInfoCopyNetwork
      .replace('%s', info !== null ? info.ip : '')
      .replace('%d', info !== null ? `${info.httpPort}` : '0'));
    return lines.join('\n');
  }

  @Builder
  InfoRow(label: string, value: string) {
    Row() {
      Text(label)
        .fontSize(12)
        .fontColor(AppColors.TEXT_SECONDARY)
        .layoutWeight(1)
      Text(value.length > 0 ? value : '—')
        .fontSize(12)
        .fontColor(AppColors.TEXT)
    }
    .width('100%')
    .padding({ top: 5, bottom: 5 })
  }

  @Builder
  CardTitle(title: string) {
    Text(title)
      .fontSize(14)
      .fontWeight(FontWeight.Medium)
      .fontColor(AppColors.TEXT)
      .width('100%')
      .margin({ bottom: 4 })
  }

  build() {
    Column() {
      Row({ space: 6 }) {
        Row() {
          UiIcon({ kind: UiIconKind.BACK, iconColor: AppColors.TEXT, iconSize: 22 })
        }
        .width(40)
        .height(40)
        .justifyContent(FlexAlign.Center)
        .onClick(() => this.pathStack.pop())
        Text(this.s.systemInfo)
          .fontSize(24)
          .fontWeight(FontWeight.Bold)
          .fontColor(AppColors.TEXT)
          .layoutWeight(1)
        Row() {
          UiIcon({ kind: UiIconKind.REFRESH, iconColor: AppColors.TEXT, iconSize: 19 })
        }
        .width(38)
        .height(38)
        .justifyContent(FlexAlign.Center)
        .onClick(() => {
          this.repo.refreshServerInfo().catch((error: Error) => {
            toast(error.message);
          });
        })
      }
      .width('100%')
      .padding({ left: 8, right: 8, top: 8, bottom: 4 })

      Scroll() {
        Column({ space: 12 }) {
          Column() {
            Text(this.repo.state.server !== null ? (this.repo.state.server as ServerConfig).name
              : this.s.notConnected)
              .fontSize(16)
              .fontWeight(FontWeight.Medium)
              .fontColor(AppColors.TEXT)
              .width('100%')
            Text(this.repo.state.server !== null
              ? `${(this.repo.state.server as ServerConfig).host}:${(this.repo.state.server as ServerConfig).port}`
              : '—')
              .fontSize(11)
              .fontColor(AppColors.TEXT_SECONDARY)
              .width('100%')
              .margin({ top: 3, bottom: 8 })
            this.InfoRow(this.s.lmsVersion,
              this.repo.state.serverInfo !== null ? this.repo.state.serverInfo.version : '—')
            this.InfoRow(this.s.serverStatus, this.connectionLabel())
            this.InfoRow(this.s.networkAddress, this.repo.state.serverInfo !== null
              ? `${this.repo.state.serverInfo.ip}:${this.repo.state.serverInfo.httpPort}` : '—')
            this.InfoRow(this.s.playerCount, `${this.repo.state.players.length}`)
          }
          .width('100%')
          .padding(16)
          .backgroundColor(AppColors.SURFACE)
          .borderRadius(16)

          Column() {
            this.CardTitle(this.s.libraryTitle)
            this.InfoRow(this.s.albums,
              this.repo.state.serverInfo !== null ? `${this.repo.state.serverInfo.albumCount}` : '0')
            this.InfoRow(this.s.tracks,
              this.repo.state.serverInfo !== null ? `${this.repo.state.serverInfo.songCount}` : '0')
            this.InfoRow(this.s.artists,
              this.repo.state.serverInfo !== null ? `${this.repo.state.serverInfo.artistCount}` : '0')
            this.InfoRow(this.s.genres,
              this.repo.state.serverInfo !== null ? `${this.repo.state.serverInfo.genreCount}` : '0')
            if (this.repo.state.serverInfo !== null && this.repo.state.serverInfo.scanning) {
              Text(this.s.scanningLibrary)
                .fontSize(11)
                .fontColor(AppColors.PRIMARY)
                .width('100%')
                .margin({ top: 6 })
            }
          }
          .width('100%')
          .padding(16)
          .backgroundColor(AppColors.SURFACE)
          .borderRadius(16)

          Column() {
            this.CardTitle(this.s.playersTitle)
            if (this.repo.state.players.length === 0) {
              Text(this.s.noPlayers)
                .fontSize(11)
                .fontColor(AppColors.TEXT_SECONDARY)
                .width('100%')
            } else {
              ForEach(this.repo.state.players, (player: Player) => {
                Row({ space: 8 }) {
                  Column()
                    .width(8)
                    .height(8)
                    .borderRadius(4)
                    .backgroundColor(player.connected ? AppColors.PRIMARY : AppColors.OUTLINE)
                  Column() {
                    Text(player.name)
                      .fontSize(13)
                      .fontColor(AppColors.TEXT)
                      .maxLines(1)
                      .textOverflow({ overflow: TextOverflow.Ellipsis })
                    Text(player.model.length > 0 ? player.model : 'Squeezelite')
                      .fontSize(10)
                      .fontColor(AppColors.TEXT_SECONDARY)
                  }
                  .alignItems(HorizontalAlign.Start)
                  .layoutWeight(1)
                  Text(player.connected ? this.s.online : this.s.offline)
                    .fontSize(10)
                    .fontColor(player.connected ? AppColors.PRIMARY : AppColors.TEXT_SECONDARY)
                }
                .width('100%')
                .padding({ top: 5, bottom: 5 })
              }, (player: Player) => player.id)
            }
          }
          .width('100%')
          .padding(16)
          .backgroundColor(AppColors.SURFACE)
          .borderRadius(16)

          Button(this.s.copyInfo)
            .fontSize(12)
            .fontColor(AppColors.ON_PRIMARY)
            .backgroundColor(AppColors.PRIMARY)
            .width('100%')
            .height(42)
            .onClick(() => copyToClipboard(this.copyPayload(), this.s.copiedToClipboard))
        }
        .width('100%')
        .padding({ left: 16, right: 16, bottom: 24 })
      }
      .width('100%')
      .layoutWeight(1)
      .scrollBar(BarState.Off)
      .align(Alignment.TopStart)
    }
    .width('100%')
    .height('100%')
    .backgroundColor(AppColors.BACKGROUND)
  }
}
