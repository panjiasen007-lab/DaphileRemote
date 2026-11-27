import { promptAction } from '@kit.ArkUI';
import { AppContext } from '../../app/AppContext';
import {
  BrowserItem, PlaylistSummary, ServerConfig, Track, coverUrl, friendlyErrorMessage, serverBaseUrl
} from '../../data/model/Models';
import { DaphileRepository } from '../../data/repo/DaphileRepository';
import { AppColors, withAlpha } from '../theme/Theme';
import { CoverArt, EmptyState, TrackRow } from '../components/Common';
import { UiIcon, UiIconKind } from '../components/UiIcon';
import { TransportIcon, TransportKind } from '../components/TransportIcon';
import { stringsFor, UiStrings } from '../i18n/I18n';

function toast(message: string): void {
  promptAction.showToast({ message: message, duration: 2000 });
}

export class ServerPlaylistParam {
  id: string;
  name: string;

  constructor(id: string, name: string) {
    this.id = id;
    this.name = name;
  }
}

export class PluginBrowserParam {
  cmd: string;
  name: string;
  kind: string;

  constructor(cmd: string, name: string, kind: string) {
    this.cmd = cmd;
    this.name = name;
    this.kind = kind;
  }
}

class BrowserLevel {
  id: string;
  name: string;

  constructor(id: string, name: string) {
    this.id = id;
    this.name = name;
  }
}

@ComponentV2
export struct ServerPlaylistsPage {
  @Param pathStack: NavPathStack = new NavPathStack();
  private repo: DaphileRepository = AppContext.get().repository;

  @Local playlists: PlaylistSummary[] = [];
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

  private load(): void {
    if (this.repo.state.server === null) {
      this.message = this.s.notConnected;
      return;
    }
    this.loading = true;
    this.message = '';
    this.repo.playlists(0, 300).then((page) => {
      this.playlists = page.items;
      this.loading = false;
    }).catch((error: Error) => {
      this.loading = false;
      this.message = friendlyErrorMessage(error);
    });
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
        Text(this.s.playlists)
          .fontSize(23)
          .fontWeight(FontWeight.Bold)
          .fontColor(AppColors.TEXT)
          .layoutWeight(1)
      }
      .width('100%')
      .padding({ left: 8, right: 12, top: 8, bottom: 4 })

      if (this.message.length > 0) {
        Text(this.message)
          .fontSize(11)
          .fontColor(AppColors.WARN)
          .width('100%')
          .padding({ left: 16, right: 16, bottom: 6 })
      }

      if (this.loading && this.playlists.length === 0) {
        Row() {
          LoadingProgress()
            .width(22)
            .height(22)
            .color(AppColors.PRIMARY)
          Text(this.s.loading)
            .fontSize(12)
            .fontColor(AppColors.TEXT_SECONDARY)
            .margin({ left: 8 })
        }
        .width('100%')
        .justifyContent(FlexAlign.Center)
        .padding(24)
      } else if (this.playlists.length === 0) {
        EmptyState({ title: this.s.playlists, body: this.message })
      } else {
        List({ space: 2 }) {
          ForEach(this.playlists, (playlist: PlaylistSummary) => {
            ListItem() {
              Row({ space: 12 }) {
                Column() {
                  UiIcon({ kind: UiIconKind.QUEUE, iconColor: AppColors.PRIMARY, iconSize: 22 })
                }
                .width(46)
                .height(46)
                .justifyContent(FlexAlign.Center)
                .backgroundColor(withAlpha(AppColors.PRIMARY, '1E'))
                .borderRadius(12)

                Column() {
                  Text(playlist.name)
                    .fontSize(16)
                    .fontWeight(FontWeight.Medium)
                    .fontColor(AppColors.TEXT)
                    .maxLines(1)
                    .textOverflow({ overflow: TextOverflow.Ellipsis })
                  Text(this.s.openPlaylist)
                    .fontSize(12)
                    .fontColor(AppColors.TEXT_SECONDARY)
                    .margin({ top: 2 })
                }
                .alignItems(HorizontalAlign.Start)
                .layoutWeight(1)
                Row() {
                  TransportIcon({ kind: TransportKind.PLAY, iconColor: AppColors.TEXT, iconSize: 15 })
                }
                .width(34)
                .height(34)
                .justifyContent(FlexAlign.Center)
                .borderRadius(17)
                .backgroundColor(withAlpha(AppColors.TEXT, '0D'))
                .onClick(() => {
                  this.repo.playPlaylist(playlist.id).catch((error: Error) => {
                    toast(friendlyErrorMessage(error));
                  });
                })
                Text('›')
                  .fontSize(18)
                  .fontColor(AppColors.TEXT_SECONDARY)
              }
              .width('100%')
              .padding({ top: 9, bottom: 9, left: 4, right: 4 })
              .onClick(() => {
                this.pathStack.pushPathByName('serverPlaylistDetail',
                  new ServerPlaylistParam(playlist.id, playlist.name));
              })
            }
          }, (playlist: PlaylistSummary) => playlist.id)
        }
        .width('100%')
        .layoutWeight(1)
        .padding({ left: 16, right: 16, bottom: 10 })
        .scrollBar(BarState.Off)
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor(AppColors.BACKGROUND)
  }
}

@ComponentV2
export struct ServerPlaylistDetailPage {
  @Param pathStack: NavPathStack = new NavPathStack();
  @Param param: ServerPlaylistParam = new ServerPlaylistParam('', '');
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

  private load(): void {
    this.loading = true;
    this.message = '';
    this.repo.playlistTracks(this.param.id).then((tracks) => {
      this.tracks = tracks;
      this.loading = false;
    }).catch((error: Error) => {
      this.loading = false;
      this.message = friendlyErrorMessage(error);
    });
  }

  private cover(track: Track): string {
    const server = this.repo.state.server;
    return server === null ? '' : coverUrl(server, track.coverId, this.repo.state.currentPlayerId);
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
        Text(this.param.name.length > 0 ? this.param.name : this.s.playlists)
          .fontSize(23)
          .fontWeight(FontWeight.Bold)
          .fontColor(AppColors.TEXT)
          .layoutWeight(1)
          .maxLines(1)
          .textOverflow({ overflow: TextOverflow.Ellipsis })
        Row({ space: 5 }) {
          TransportIcon({ kind: TransportKind.PLAY, iconColor: AppColors.ON_PRIMARY, iconSize: 11 })
          Text(this.s.playAllItems)
            .fontSize(11)
            .fontColor(AppColors.ON_PRIMARY)
        }
        .height(30)
        .padding({ left: 12, right: 12 })
        .backgroundColor(AppColors.PRIMARY)
        .borderRadius(15)
        .enabled(this.tracks.length > 0)
        .onClick(() => {
          this.repo.playPlaylist(this.param.id).catch((error: Error) => {
            toast(friendlyErrorMessage(error));
          });
        })
      }
      .width('100%')
      .padding({ left: 8, right: 12, top: 8, bottom: 4 })

      Row({ space: 8 }) {
        Text(this.s.tracksUnit.replace('%d', `${this.tracks.length}`))
          .fontSize(11)
          .fontColor(AppColors.TEXT_SECONDARY)
          .layoutWeight(1)
        if (this.message.length > 0) {
          Text(this.message)
            .fontSize(11)
            .fontColor(AppColors.WARN)
            .maxLines(1)
            .textOverflow({ overflow: TextOverflow.Ellipsis })
        }
      }
      .width('100%')
      .padding({ left: 16, right: 16, bottom: 4 })

      if (this.loading && this.tracks.length === 0) {
        Row() {
          LoadingProgress()
            .width(22)
            .height(22)
            .color(AppColors.PRIMARY)
          Text(this.s.loading)
            .fontSize(12)
            .fontColor(AppColors.TEXT_SECONDARY)
            .margin({ left: 8 })
        }
        .width('100%')
        .justifyContent(FlexAlign.Center)
        .padding(24)
      } else if (this.tracks.length === 0) {
        EmptyState({ title: this.s.playlistEmptyTitle, body: this.s.playlistEmptyBody })
      } else {
        List({ space: 6 }) {
          ForEach(this.tracks, (track: Track) => {
            ListItem() {
              TrackRow({
                track: track,
                coverUrl: this.cover(track),
                isCurrent: this.repo.state.nowPlaying !== null &&
                  this.repo.state.nowPlaying.track !== null &&
                  this.repo.state.nowPlaying.track.id === track.id,
                onTap: () => {
                  this.repo.playTrack(track.id).catch((error: Error) => {
                    toast(friendlyErrorMessage(error));
                  });
                },
                onLongPress: () => {
                  this.repo.queueTrack(track.id).then(() => {
                    toast(this.s.queueAddedCount);
                  }).catch((error: Error) => {
                    toast(friendlyErrorMessage(error));
                  });
                }
              })
            }
          }, (track: Track) => `${track.id}-${track.index}-${track.title}`)
        }
        .width('100%')
        .layoutWeight(1)
        .padding({ left: 16, right: 16, bottom: 10 })
        .scrollBar(BarState.Off)
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor(AppColors.BACKGROUND)
  }
}

@ComponentV2
export struct PluginBrowserPage {
  @Param pathStack: NavPathStack = new NavPathStack();
  @Param param: PluginBrowserParam = new PluginBrowserParam('', '', 'plugin');
  private repo: DaphileRepository = AppContext.get().repository;

  @Local levels: BrowserLevel[] = [new BrowserLevel('0', '')];
  @Local items: BrowserItem[] = [];
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

  private parentId(): string {
    return this.levels.length === 0 ? '0' : this.levels[this.levels.length - 1].id;
  }

  private title(): string {
    if (this.levels.length === 0) {
      return this.param.name;
    }
    const name = this.levels[this.levels.length - 1].name;
    return name.length > 0 ? name : this.param.name;
  }

  private load(): void {
    if (this.repo.state.server === null) {
      this.message = this.s.notConnected;
      return;
    }
    this.loading = true;
    this.message = '';
    const parent = this.parentId();
    const task = this.param.kind === 'favorites'
      ? this.repo.favorites(parent === '0' ? '' : parent)
      : this.repo.pluginItems(this.param.cmd, parent);
    task.then((items) => {
      this.items = items;
      this.loading = false;
    }).catch((error: Error) => {
      this.items = [];
      this.loading = false;
      this.message = friendlyErrorMessage(error);
    });
  }

  private goBack(): void {
    if (this.levels.length > 1) {
      this.levels = this.levels.slice(0, this.levels.length - 1);
      this.load();
    } else {
      this.pathStack.pop();
    }
  }

  private open(item: BrowserItem): void {
    if (item.hasItems && (item.type === 'link' || !item.isAudio)) {
      this.levels = this.levels.concat([new BrowserLevel(item.id, item.name)]);
      this.load();
      return;
    }
    const cmd = this.param.kind === 'favorites' ? '' : this.param.cmd;
    this.repo.playBrowserItem(cmd, item).catch((error: Error) => {
      toast(friendlyErrorMessage(error));
    });
  }

  private iconUrl(item: BrowserItem): string {
    const server = this.repo.state.server;
    if (server === null || item.icon.length === 0) {
      return '';
    }
    if (item.icon.startsWith('http://') || item.icon.startsWith('https://')) {
      return item.icon;
    }
    const path = item.icon.startsWith('/') ? item.icon.substring(1) : item.icon;
    return `${serverBaseUrl(server)}/${path}`;
  }

  @Builder
  ItemRow(item: BrowserItem) {
    Row({ space: 10 }) {
      CoverArt({ url: this.iconUrl(item), coverSize: 42, radius: 8 })
      Column() {
        Text(item.name)
          .fontSize(14)
          .fontColor(AppColors.TEXT)
          .maxLines(1)
          .textOverflow({ overflow: TextOverflow.Ellipsis })
        if (item.description.length > 0) {
          Text(item.description)
            .fontSize(11)
            .fontColor(AppColors.TEXT_SECONDARY)
            .maxLines(1)
            .textOverflow({ overflow: TextOverflow.Ellipsis })
            .margin({ top: 2 })
        }
      }
      .alignItems(HorizontalAlign.Start)
      .layoutWeight(1)
      if (item.hasItems && (item.type === 'link' || !item.isAudio)) {
        Text('›')
          .fontSize(16)
          .fontColor(AppColors.TEXT_SECONDARY)
      } else {
        Button(this.s.addToQueue)
          .fontSize(10)
          .height(28)
          .fontColor(AppColors.TEXT)
          .backgroundColor(AppColors.SURFACE_HIGH)
          .onClick(() => {
            this.repo.queueBrowserItem(this.param.kind === 'favorites' ? '' : this.param.cmd, item)
              .then(() => toast(this.s.queueAddedCount))
              .catch((error: Error) => toast(friendlyErrorMessage(error)));
          })
      }
    }
    .width('100%')
    .padding(10)
    .backgroundColor(AppColors.SURFACE)
    .borderRadius(10)
    .onClick(() => this.open(item))
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
        .onClick(() => this.goBack())
        Text(this.title())
          .fontSize(23)
          .fontWeight(FontWeight.Bold)
          .fontColor(AppColors.TEXT)
          .layoutWeight(1)
          .maxLines(1)
          .textOverflow({ overflow: TextOverflow.Ellipsis })
        Row() {
          UiIcon({ kind: UiIconKind.REFRESH, iconColor: AppColors.TEXT, iconSize: 19 })
        }
        .width(38)
        .height(38)
        .justifyContent(FlexAlign.Center)
        .onClick(() => this.load())
      }
      .width('100%')
      .padding({ left: 8, right: 8, top: 8, bottom: 4 })

      if (this.loading && this.items.length === 0) {
        Row() {
          LoadingProgress()
            .width(22)
            .height(22)
            .color(AppColors.PRIMARY)
          Text(this.s.loading)
            .fontSize(12)
            .fontColor(AppColors.TEXT_SECONDARY)
            .margin({ left: 8 })
        }
        .width('100%')
        .justifyContent(FlexAlign.Center)
        .padding(24)
      } else if (this.items.length === 0) {
        EmptyState({
          title: this.message.length > 0 ? this.message : this.s.noContentTitle,
          body: this.message.length > 0 ? '' : this.s.noContentBody
        })
      } else {
        List({ space: 6 }) {
          ForEach(this.items, (item: BrowserItem, index: number) => {
            ListItem() {
              this.ItemRow(item)
            }
          }, (item: BrowserItem, index: number) => `${index}-${item.id}`)
        }
        .width('100%')
        .layoutWeight(1)
        .padding({ left: 16, right: 16, bottom: 10 })
        .scrollBar(BarState.Off)
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor(AppColors.BACKGROUND)
  }
}
