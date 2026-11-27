import { promptAction } from '@kit.ArkUI';
import { AppContext } from '../../app/AppContext';
import { Album, Track, coverUrl, friendlyErrorMessage } from '../../data/model/Models';
import { DaphileRepository } from '../../data/repo/DaphileRepository';
import { AppColors, formatTime, withAlpha } from '../theme/Theme';
import { EmptyState, QualityBadge, TrackQuality, trackQuality } from '../components/Common';
import { TrackDisplay } from '../../data/model/TrackDisplay';
import { UiIcon, UiIconKind } from '../components/UiIcon';
import { TransportIcon, TransportKind } from '../components/TransportIcon';
import { stringsFor, UiStrings } from '../i18n/I18n';

function toast(message: string): void {
  promptAction.showToast({ message: message, duration: 2200 });
}

@ComponentV2
export struct AlbumDetailPage {
  @Param pathStack: NavPathStack = new NavPathStack();
  @Require @Param album: Album;
  private repo: DaphileRepository = AppContext.get().repository;

  @Local tracks: Track[] = [];
  @Local loading: boolean = false;
  @Local message: string = '';

  aboutToAppear(): void {
    this.load();
  }

  @Computed
  get s(): UiStrings {
    const settings = this.repo.state.settings;
    return stringsFor(settings.theme.length > 0 ? settings.language : 'en');
  }

  private run(action: () => Promise<void>): void {
    action().catch((error: Error) => toast(friendlyErrorMessage(error)));
  }

  private load(): void {
    this.loading = true;
    this.repo.albumTracks(this.album.id).then((tracks) => {
      this.tracks = tracks;
      this.loading = false;
      if (tracks.length === 0) {
        this.message = this.s.noSongsBody;
      }
    }).catch((error: Error) => {
      this.loading = false;
      this.message = friendlyErrorMessage(error);
    });
  }

  private albumCover(): string {
    const server = this.repo.state.server;
    return server === null ? '' : coverUrl(server, this.album.artworkId, '');
  }

  private quality(): TrackQuality | null {
    return this.tracks.length > 0 ? trackQuality(this.tracks[0]) : null;
  }

  private isCurrent(track: Track): boolean {
    const state = this.repo.state.nowPlaying;
    return state !== null && state.track !== null && state.track.id === track.id;
  }

  private metaLine(): string {
    const count = this.tracks.length > 0 ? this.tracks.length : this.album.trackCount;
    const parts: string[] = [];
    if (this.album.year.length > 0) {
      parts.push(this.album.year);
    }
    parts.push(`${count} ${this.s.tracks.toLowerCase()}`);
    return parts.join(' · ');
  }

  private matchCover(): void {
    if (this.tracks.length === 0) {
      return;
    }
    this.run(async () => {
      const url = await this.repo.resolveOnlineCover(this.tracks[0], true);
      toast(url.length > 0 ? this.s.matchedCoverToast : this.s.noOnlineCoverToast);
    });
  }

  @Builder
  Header() {
    Row({ space: 6 }) {
      Row() {
        UiIcon({ kind: UiIconKind.BACK, iconColor: AppColors.TEXT, iconSize: 22 })
      }
      .width(40)
      .height(40)
      .justifyContent(FlexAlign.Center)
      .onClick(() => this.pathStack.pop())

      Text(this.album.title)
        .fontSize(22)
        .fontWeight(FontWeight.Bold)
        .fontColor(AppColors.TEXT)
        .maxLines(1)
        .textOverflow({ overflow: TextOverflow.Ellipsis })
        .layoutWeight(1)
        .margin({ right: 8 })
    }
    .width('100%')
    .padding({ left: 8, right: 16, top: 8, bottom: 4 })
  }

  @Builder
  CoverBlock() {
    Column({ space: 0 }) {
      if (this.albumCover().length > 0) {
        Image(this.albumCover())
          .width(248)
          .height(248)
          .objectFit(ImageFit.Cover)
          .borderRadius(8)
          .shadow({ radius: 18, color: '#55000000', offsetY: 6 })
      } else {
        Column() {
          Text('♪')
            .fontSize(56)
            .fontColor(AppColors.TEXT_SECONDARY)
        }
        .width(248)
        .height(248)
        .justifyContent(FlexAlign.Center)
        .backgroundColor(AppColors.SURFACE)
        .borderRadius(8)
      }

      Text(this.album.title)
        .fontSize(22)
        .fontWeight(FontWeight.Bold)
        .fontColor(AppColors.TEXT)
        .textAlign(TextAlign.Center)
        .maxLines(2)
        .textOverflow({ overflow: TextOverflow.Ellipsis })
        .margin({ top: 18 })
        .padding({ left: 8, right: 8 })

      Text(this.album.artist)
        .fontSize(13)
        .fontColor(AppColors.TEXT_SECONDARY)
        .maxLines(1)
        .textOverflow({ overflow: TextOverflow.Ellipsis })
        .margin({ top: 6 })

      Row({ space: 6 }) {
        Text(this.metaLine())
          .fontSize(11)
          .fontColor(AppColors.TEXT_SECONDARY)
        if (this.quality() !== null) {
          QualityBadge({ quality: this.quality() as TrackQuality })
        }
      }
      .margin({ top: 6 })

      Row({ space: 14 }) {
        Row({ space: 8 }) {
          TransportIcon({ kind: TransportKind.PLAY, iconColor: AppColors.ON_PRIMARY, iconSize: 14 })
          Text(this.s.playAllItems)
            .fontSize(13)
            .fontColor(AppColors.ON_PRIMARY)
        }
        .height(46)
        .padding({ left: 22, right: 24 })
        .backgroundColor(AppColors.PRIMARY)
        .borderRadius(23)
        .onClick(() => this.run(() => this.repo.playAlbum(this.album.id)))

        Row({ space: 8 }) {
          UiIcon({ kind: UiIconKind.QUEUE, iconColor: AppColors.PRIMARY, iconSize: 18 })
          Text(this.s.addToQueue)
            .fontSize(13)
            .fontColor(AppColors.PRIMARY)
        }
        .height(46)
        .padding({ left: 18, right: 22 })
        .backgroundColor(withAlpha(AppColors.PRIMARY, '00'))
        .borderRadius(23)
        .border({ width: 1, color: withAlpha(AppColors.PRIMARY, '88') })
        .enabled(this.tracks.length > 0)
        .onClick(() => this.run(() => this.repo.playAlbum(this.album.id, true)))
      }
      .margin({ top: 18 })

      Row({ space: 6 }) {
        UiIcon({ kind: UiIconKind.MATCH, iconColor: AppColors.PRIMARY, iconSize: 16 })
        Text(this.s.matchCover)
          .fontSize(13)
          .fontColor(AppColors.PRIMARY)
      }
      .margin({ top: 14 })
      .onClick(() => this.matchCover())
    }
    .width('100%')
    .alignItems(HorizontalAlign.Center)
    .padding({ top: 8 })
  }

  @Builder
  TrackLine(track: Track) {
    Row({ space: 10 }) {
      Text(`${this.tracks.indexOf(track) + 1}`.padStart(2, '0'))
        .fontSize(11)
        .fontColor(AppColors.TEXT_SECONDARY)
        .width(24)

      Column({ space: 2 }) {
        Row({ space: 6 }) {
          Text(TrackDisplay.title(track).length > 0 ? TrackDisplay.title(track) : this.s.unknownTrack)
            .fontSize(14)
            .fontWeight(this.isCurrent(track) ? FontWeight.Medium : FontWeight.Normal)
            .fontColor(this.isCurrent(track) ? AppColors.PRIMARY : AppColors.TEXT)
            .maxLines(1)
            .textOverflow({ overflow: TextOverflow.Ellipsis })
          if (trackQuality(track) !== null) {
            QualityBadge({ quality: trackQuality(track) as TrackQuality })
          }
        }
        Text([TrackDisplay.artist(track), TrackDisplay.album(track)]
          .filter((value: string) => value.length > 0).join(' · '))
          .fontSize(10)
          .fontColor(AppColors.TEXT_SECONDARY)
          .maxLines(1)
          .textOverflow({ overflow: TextOverflow.Ellipsis })
      }
      .alignItems(HorizontalAlign.Start)
      .layoutWeight(1)

      Text(formatTime(track.duration))
        .fontSize(11)
        .fontColor(AppColors.TEXT_SECONDARY)

      Row() {
        UiIcon({ kind: UiIconKind.QUEUE, iconColor: AppColors.TEXT_SECONDARY, iconSize: 18 })
      }
      .width(30)
      .height(30)
      .justifyContent(FlexAlign.Center)
      .onClick(() => {
        this.run(async () => {
          await this.repo.queueTrack(track.id);
          toast(this.s.addToQueue);
        });
      })
    }
    .width('100%')
    .height(56)
    .onClick(() => this.run(() => this.repo.playTrack(track.id)))
  }

  build() {
    Column() {
      this.Header()

      Scroll() {
        Row() {
          Column() {
            this.CoverBlock()

            if (this.loading) {
              LoadingProgress()
                .width(24)
                .height(24)
                .color(AppColors.PRIMARY)
                .margin({ top: 30, bottom: 30 })
            } else if (this.tracks.length === 0) {
              EmptyState({ title: this.s.noSongsTitle, body: this.message })
            } else {
              Column() {
                ForEach(this.tracks, (track: Track) => {
                  this.TrackLine(track)
                }, (track: Track) => `${track.id}-${track.index}-${track.title}`)
              }
              .width('100%')
              .margin({ top: 8 })
            }
          }
          .width('100%')
          .constraintSize({ maxWidth: 900 })
          .padding({ left: 16, right: 16, bottom: 24 })
        }
        .width('100%')
        .justifyContent(FlexAlign.Center)
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
