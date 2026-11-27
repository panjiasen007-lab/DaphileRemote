import { promptAction } from '@kit.ArkUI';
import { AppContext } from '../../app/AppContext';
import { Track, coverUrl, friendlyErrorMessage } from '../../data/model/Models';
import { DaphileRepository } from '../../data/repo/DaphileRepository';
import { AppColors, formatTime } from '../theme/Theme';
import { EmptyState, QualityBadge, TrackQuality, trackQuality } from '../components/Common';
import { TrackDisplay } from '../../data/model/TrackDisplay';
import { UiIcon, UiIconKind } from '../components/UiIcon';
import { TransportIcon, TransportKind } from '../components/TransportIcon';
import { stringsFor, UiStrings } from '../i18n/I18n';

function toast(message: string): void {
  promptAction.showToast({ message: message, duration: 2200 });
}

@ComponentV2
export struct SongsPage {
  @Param pathStack: NavPathStack = new NavPathStack();
  private repo: DaphileRepository = AppContext.get().repository;

  @Local tracks: Track[] = [];
  @Local loading: boolean = false;
  @Local hasMore: boolean = true;
  @Local message: string = '';

  aboutToAppear(): void {
    this.loadMore();
  }

  @Computed
  get s(): UiStrings {
    const settings = this.repo.state.settings;
    return stringsFor(settings.theme.length > 0 ? settings.language : 'en');
  }

  private run(action: () => Promise<void>): void {
    action().catch((error: Error) => toast(friendlyErrorMessage(error)));
  }

  private loadMore(): void {
    if (this.loading || !this.hasMore) {
      return;
    }
    this.loading = true;
    this.repo.songs(this.tracks.length, 200).then((page) => {
      this.tracks = this.tracks.concat(page.items);
      this.hasMore = page.hasMore;
      this.loading = false;
    }).catch((error: Error) => {
      this.loading = false;
      this.hasMore = false;
      this.message = friendlyErrorMessage(error);
    });
  }

  private cover(track: Track): string {
    const server = this.repo.state.server;
    return server === null ? '' : coverUrl(server, track.coverId, this.repo.state.currentPlayerId);
  }

  private isCurrent(track: Track): boolean {
    const state = this.repo.state.nowPlaying;
    return state !== null && state.track !== null && state.track.id === track.id;
  }

  private totalLabel(): string {
    const info = this.repo.state.serverInfo;
    const count = info !== null && info.songCount > 0 ? info.songCount : this.tracks.length;
    return `${count} ${this.s.tracks.toLowerCase()}`;
  }

  private subtitle(track: Track): string {
    return [TrackDisplay.artist(track), TrackDisplay.album(track)]
      .filter((value: string) => value.length > 0).join(' · ');
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

      Text(this.s.songs)
        .fontSize(24)
        .fontWeight(FontWeight.Bold)
        .fontColor(AppColors.TEXT)
        .layoutWeight(1)

      Text(this.totalLabel())
        .fontSize(11)
        .fontColor(AppColors.TEXT_SECONDARY)
        .maxLines(1)
        .margin({ right: 8 })

      Row() {
        UiIcon({ kind: UiIconKind.QUEUE, iconColor: AppColors.TEXT, iconSize: 19 })
      }
      .width(36)
      .height(36)
      .justifyContent(FlexAlign.Center)
      .onClick(() => this.run(() => this.repo.queueAllSongs(false)))

      Row() {
        TransportIcon({ kind: TransportKind.PLAY, iconColor: AppColors.TEXT, iconSize: 18 })
      }
      .width(36)
      .height(36)
      .justifyContent(FlexAlign.Center)
      .onClick(() => this.run(() => this.repo.queueAllSongs(true)))
    }
    .width('100%')
    .padding({ left: 8, right: 8, top: 8, bottom: 4 })
  }

  @Builder
  SongLine(track: Track) {
    Row({ space: 12 }) {
      if (this.cover(track).length > 0) {
        Image(this.cover(track))
          .width(52)
          .height(52)
          .objectFit(ImageFit.Cover)
          .borderRadius(6)
          .interpolation(ImageInterpolation.Medium)
      } else {
        Column() {
          Text('♪')
            .fontSize(20)
            .fontColor(AppColors.TEXT_SECONDARY)
        }
        .width(52)
        .height(52)
        .justifyContent(FlexAlign.Center)
        .backgroundColor(AppColors.SURFACE)
        .borderRadius(6)
      }

      Column({ space: 3 }) {
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
        Text(this.subtitle(track))
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
    .height(64)
    .onClick(() => this.run(() => this.repo.playTrack(track.id)))
  }

  build() {
    Column() {
      this.Header()

      List() {
        if (this.tracks.length === 0 && !this.loading) {
          ListItem() {
            EmptyState({ title: this.s.noSongsTitle, body: this.message })
          }
        }
        ForEach(this.tracks, (track: Track) => {
          ListItem() {
            this.SongLine(track)
          }
        }, (track: Track) => `song-${track.id}-${track.index}-${track.title}`)

        if (this.loading) {
          ListItem() {
            Row() {
              LoadingProgress()
                .width(22)
                .height(22)
                .color(AppColors.PRIMARY)
            }
            .width('100%')
            .justifyContent(FlexAlign.Center)
            .padding(14)
          }
        }
      }
      .width('100%')
      .layoutWeight(1)
      .padding({ left: 16, right: 16, bottom: 10 })
      .scrollBar(BarState.Off)
      .onReachEnd(() => this.loadMore())
    }
    .width('100%')
    .height('100%')
    .backgroundColor(AppColors.BACKGROUND)
  }
}
