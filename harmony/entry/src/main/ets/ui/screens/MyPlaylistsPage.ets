import { promptAction } from '@kit.ArkUI';
import { AppContext } from '../../app/AppContext';
import {
  CustomPlaylist, DailyRecommendation, Track, coverUrl, friendlyErrorMessage
} from '../../data/model/Models';
import { DaphileRepository } from '../../data/repo/DaphileRepository';
import { AppColors, withAlpha } from '../theme/Theme';
import { EmptyState, TrackRow } from '../components/Common';
import { UiIcon, UiIconKind } from '../components/UiIcon';
import { TransportIcon, TransportKind } from '../components/TransportIcon';
import { stringsFor, UiStrings } from '../i18n/I18n';

function toast(message: string): void {
  promptAction.showToast({ message: message, duration: 2200 });
}

export class CustomPlaylistParam {
  id: string;

  constructor(id: string) {
    this.id = id;
  }
}

@ComponentV2
export struct MyPlaylistsPage {
  @Param pathStack: NavPathStack = new NavPathStack();
  private repo: DaphileRepository = AppContext.get().repository;

  @Local showCreate: boolean = false;
  @Local newName: string = '';
  @Local creating: boolean = false;

  @Computed
  get s(): UiStrings {
    const settings = this.repo.state.settings;
    return stringsFor(settings.theme.length > 0 ? settings.language : 'en');
  }

  private run(action: () => Promise<void>): void {
    action().catch((error: Error) => toast(friendlyErrorMessage(error)));
  }

  private createPlaylist(): void {
    const name = this.newName.trim();
    if (name.length === 0 || this.creating) {
      return;
    }
    this.creating = true;
    this.repo.createCustomPlaylist(name).then(() => {
      this.creating = false;
      this.showCreate = false;
      this.newName = '';
    }).catch((error: Error) => {
      this.creating = false;
      toast(friendlyErrorMessage(error));
    });
  }

  private confirmDelete(playlist: CustomPlaylist): void {
    promptAction.showDialog({
      title: this.s.deletePlaylistTitle,
      message: this.s.deletePlaylistBody.replace('%s', playlist.name),
      buttons: [
        { text: this.s.cancel, color: AppColors.TEXT_SECONDARY },
        { text: this.s.delete, color: AppColors.ERROR }
      ]
    }).then((result) => {
      if (result.index === 1) {
        this.run(() => this.repo.deleteCustomPlaylist(playlist.id));
      }
    }).catch(() => {
    });
  }

  @Builder
  PlaylistRow(playlist: CustomPlaylist) {
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
        Text(this.s.tracksUnit.replace('%d', `${playlist.tracks.length}`))
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
      .onClick(() => this.run(() => this.repo.playCustomPlaylist(playlist)))
      Text('›')
        .fontSize(18)
        .fontColor(AppColors.TEXT_SECONDARY)
    }
    .width('100%')
    .padding({ top: 9, bottom: 9, left: 4, right: 4 })
    .onClick(() => this.pathStack.pushPathByName('customPlaylist', new CustomPlaylistParam(playlist.id)))
    .gesture(
      LongPressGesture({ repeat: false })
        .onAction(() => this.confirmDelete(playlist))
    )
  }

  @Builder
  CreateOverlay() {
    Column() {
      Column({ space: 12 }) {
        Text(this.s.createPlaylist)
          .fontSize(17)
          .fontWeight(FontWeight.Medium)
          .fontColor(AppColors.TEXT)
          .width('100%')
        TextInput({ placeholder: this.s.newPlaylistName, text: this.newName })
          .fontSize(14)
          .fontColor(AppColors.TEXT)
          .placeholderColor(AppColors.TEXT_SECONDARY)
          .backgroundColor(AppColors.SURFACE_HIGH)
          .borderRadius(8)
          .onChange((value: string) => {
            this.newName = value;
          })
          .onSubmit(() => this.createPlaylist())
        Row({ space: 8 }) {
          Button(this.s.cancel)
            .fontSize(12)
            .fontColor(AppColors.TEXT)
            .backgroundColor(AppColors.SURFACE_HIGH)
            .layoutWeight(1)
            .onClick(() => {
              this.showCreate = false;
            })
          Button(this.creating ? this.s.loading : this.s.createPlaylist)
            .fontSize(12)
            .fontColor(AppColors.ON_PRIMARY)
            .backgroundColor(AppColors.PRIMARY)
            .layoutWeight(1)
            .enabled(!this.creating)
            .onClick(() => this.createPlaylist())
        }
        .width('100%')
      }
      .width('86%')
      .padding(16)
      .backgroundColor(AppColors.SURFACE)
      .borderRadius(16)
      .border({ width: 0.5, color: AppColors.OUTLINE })
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
    .backgroundColor('#000000AA')
  }

  build() {
    Stack() {
      Column() {
        Row({ space: 6 }) {
          Row() {
            UiIcon({ kind: UiIconKind.BACK, iconColor: AppColors.TEXT, iconSize: 22 })
          }
          .width(40)
          .height(40)
          .justifyContent(FlexAlign.Center)
          .onClick(() => this.pathStack.pop())
          Text(this.s.myPlaylists)
            .fontSize(23)
            .fontWeight(FontWeight.Bold)
            .fontColor(AppColors.TEXT)
            .layoutWeight(1)
          Row() {
            UiIcon({ kind: UiIconKind.PLUS, iconColor: AppColors.TEXT, iconSize: 22 })
          }
          .width(40)
          .height(40)
          .justifyContent(FlexAlign.Center)
          .onClick(() => {
            this.showCreate = true;
          })
        }
        .width('100%')
        .padding({ left: 8, right: 12, top: 8, bottom: 4 })

        List({ space: 2 }) {
          if (this.repo.state.customPlaylists.length === 0) {
            ListItem() {
              EmptyState({ title: this.s.noPlaylistsTitle, body: this.s.noPlaylistsHint })
            }
          }
          ForEach(this.repo.state.customPlaylists, (playlist: CustomPlaylist) => {
            ListItem() {
              this.PlaylistRow(playlist)
            }
          }, (playlist: CustomPlaylist) => playlist.id)
        }
        .width('100%')
        .layoutWeight(1)
        .padding({ left: 16, right: 16, bottom: 10 })
        .scrollBar(BarState.Off)
      }
      .width('100%')
      .height('100%')

      if (this.showCreate) {
        this.CreateOverlay()
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor(AppColors.BACKGROUND)
  }
}

@ComponentV2
export struct CustomPlaylistPage {
  @Param pathStack: NavPathStack = new NavPathStack();
  @Param param: CustomPlaylistParam = new CustomPlaylistParam('');
  private repo: DaphileRepository = AppContext.get().repository;

  @Computed
  get s(): UiStrings {
    const settings = this.repo.state.settings;
    return stringsFor(settings.theme.length > 0 ? settings.language : 'en');
  }

  private current(): CustomPlaylist | null {
    for (const playlist of this.repo.state.customPlaylists) {
      if (playlist.id === this.param.id) {
        return playlist;
      }
    }
    return null;
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
        Text(this.current() !== null ? (this.current() as CustomPlaylist).name : this.s.myPlaylists)
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
        .enabled(this.current() !== null)
        .onClick(() => {
          const playlist = this.current();
          if (playlist !== null) {
            this.repo.playCustomPlaylist(playlist).catch((error: Error) => {
              toast(friendlyErrorMessage(error));
            });
          }
        })
      }
      .width('100%')
      .padding({ left: 8, right: 12, top: 8, bottom: 4 })

      if (this.current() === null) {
        EmptyState({ title: this.s.noPlaylistsTitle, body: this.s.noPlaylistsBody })
      } else if ((this.current() as CustomPlaylist).tracks.length === 0) {
        EmptyState({ title: this.s.playlistEmptyTitle, body: this.s.playlistEmptyBody })
      } else {
        List({ space: 6 }) {
          ForEach((this.current() as CustomPlaylist).tracks, (track: Track) => {
            ListItem() {
              TrackRow({
                track: track,
                coverUrl: this.cover(track),
                onTap: () => {
                  this.repo.playTrack(track.id).catch((error: Error) => {
                    toast(friendlyErrorMessage(error));
                  });
                },
                onLongPress: () => {
                  this.repo.removeTrackFromCustomPlaylist(this.param.id, track)
                    .catch((error: Error) => toast(friendlyErrorMessage(error)));
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
export struct DailyRecommendationPage {
  @Param pathStack: NavPathStack = new NavPathStack();
  private repo: DaphileRepository = AppContext.get().repository;

  @Local tracks: Track[] = [];
  @Local loading: boolean = false;
  @Local message: string = '';

  aboutToAppear(): void {
    this.load(false);
  }

  @Computed
  get s(): UiStrings {
    const settings = this.repo.state.settings;
    return stringsFor(settings.theme.length > 0 ? settings.language : 'en');
  }

  private load(force: boolean): void {
    this.loading = true;
    this.repo.refreshDailyRecommendation(force).then((recommendation) => {
      this.tracks = recommendation.tracks;
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

  private temporaryPlaylist(): CustomPlaylist {
    return {
      id: 'daily',
      name: this.s.dailyRecommendation,
      tracks: this.tracks,
      createdAt: 0,
      updatedAt: 0
    };
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
        Text(this.s.dailyRecommendation)
          .fontSize(23)
          .fontWeight(FontWeight.Bold)
          .fontColor(AppColors.TEXT)
          .layoutWeight(1)
        Row() {
          UiIcon({ kind: UiIconKind.REFRESH, iconColor: AppColors.TEXT, iconSize: 19 })
        }
        .width(38)
        .height(38)
        .justifyContent(FlexAlign.Center)
        .onClick(() => this.load(true))
      }
      .width('100%')
      .padding({ left: 8, right: 8, top: 8, bottom: 4 })

      Row({ space: 8 }) {
        Button(this.s.playAllItems)
          .fontSize(11)
          .fontColor(AppColors.ON_PRIMARY)
          .backgroundColor(AppColors.PRIMARY)
          .layoutWeight(1)
          .enabled(this.tracks.length > 0)
          .onClick(() => {
            this.repo.playCustomPlaylist(this.temporaryPlaylist()).catch((error: Error) => {
              toast(friendlyErrorMessage(error));
            });
          })
        Button(this.s.addToQueue)
          .fontSize(11)
          .fontColor(AppColors.TEXT)
          .backgroundColor(AppColors.SURFACE_HIGH)
          .layoutWeight(1)
          .enabled(this.tracks.length > 0)
          .onClick(() => {
            this.repo.playCustomPlaylist(this.temporaryPlaylist(), true).catch((error: Error) => {
              toast(friendlyErrorMessage(error));
            });
          })
      }
      .width('100%')
      .padding({ left: 16, right: 16, bottom: 6 })

      if (this.loading) {
        LoadingProgress()
          .width(26)
          .height(26)
          .color(AppColors.PRIMARY)
          .margin({ top: 40 })
      } else if (this.tracks.length === 0) {
        EmptyState({ title: this.s.dailyRecommendation, body: this.message })
      } else {
        List({ space: 6 }) {
          ForEach(this.tracks, (track: Track) => {
            ListItem() {
              TrackRow({
                track: track,
                coverUrl: this.cover(track),
                onTap: () => {
                  this.repo.playTrack(track.id).catch((error: Error) => {
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
