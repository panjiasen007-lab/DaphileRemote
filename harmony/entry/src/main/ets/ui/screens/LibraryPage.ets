import { promptAction } from '@kit.ArkUI';
import { AppContext } from '../../app/AppContext';
import { Album, Artist, Track, coverUrl, friendlyErrorMessage } from '../../data/model/Models';
import { DaphileRepository } from '../../data/repo/DaphileRepository';
import { AppColors, withAlpha } from '../theme/Theme';
import { EmptyState, TrackRow } from '../components/Common';
import { UiIcon, UiIconKind } from '../components/UiIcon';
import { AlbumsParam } from './LibraryBrowsePages';
import { FolderParam } from './FolderPage';
import { stringsFor, UiStrings } from '../i18n/I18n';

function toast(message: string): void {
  promptAction.showToast({ message: message, duration: 2200 });
}

@ComponentV2
export struct LibraryPage {
  @Param pathStack: NavPathStack = new NavPathStack();
  @Param columns: number = 2;
  private repo: DaphileRepository = AppContext.get().repository;

  @Local query: string = '';
  @Local searchFocused: boolean = false;
  @Local searchSongs: Track[] = [];
  @Local searchAlbums: Album[] = [];
  @Local searchArtists: Artist[] = [];
  @Local searching: boolean = false;
  @Local message: string = '';
  @Local showRandomDialog: boolean = false;
  @Local randomCountText: string = '20';
  private searchTimer: number = -1;

  aboutToDisappear(): void {
    if (this.searchTimer >= 0) {
      clearTimeout(this.searchTimer);
      this.searchTimer = -1;
    }
  }

  @Computed
  get s(): UiStrings {
    const settings = this.repo.state.settings;
    return stringsFor(settings.theme.length > 0 ? settings.language : 'en');
  }

  private run(action: () => Promise<void>): void {
    action().catch((error: Error) => toast(friendlyErrorMessage(error)));
  }

  private albumCountText(): string {
    const info = this.repo.state.serverInfo;
    return `${info !== null ? info.albumCount : 0} ${this.s.albums}`;
  }

  private songCountText(): string {
    const info = this.repo.state.serverInfo;
    return `${info !== null ? info.songCount : 0} ${this.s.tracks}`;
  }

  private artistCountText(): string {
    const info = this.repo.state.serverInfo;
    return `${info !== null ? info.artistCount : 0} ${this.s.artists}`;
  }

  private genreCountText(): string {
    const info = this.repo.state.serverInfo;
    return `${info !== null ? info.genreCount : 0} ${this.s.genres}`;
  }

  private statsText(): string {
    const info = this.repo.state.serverInfo;
    if (info === null) {
      return '';
    }
    return `${info.albumCount} ${this.s.albums.toLowerCase()} · ${info.songCount} ${this.s.tracks.toLowerCase()}`;
  }

  private doSearch(): void {
    const query = this.query.trim();
    if (query.length === 0) {
      this.searchSongs = [];
      this.searchAlbums = [];
      this.searchArtists = [];
      return;
    }
    this.searching = true;
    this.repo.search(query).then((results) => {
      this.searchSongs = results.songs;
      this.searchAlbums = results.albums;
      this.searchArtists = results.artists;
      this.searching = false;
    }).catch((error: Error) => {
      this.searching = false;
      this.message = friendlyErrorMessage(error);
    });
  }

  private scheduleSearch(): void {
    if (this.searchTimer >= 0) {
      clearTimeout(this.searchTimer);
    }
    this.searchTimer = setTimeout(() => {
      this.searchTimer = -1;
      this.doSearch();
    }, 350);
  }

  private scanLabel(): string {
    const status = this.repo.state.scanStatus;
    if (status === null) {
      return '';
    }
    const message = status.message.length > 0 ? status.message : this.s.scanningLibrary;
    return status.percent >= 0 ? `${message} ${status.percent}%` : message;
  }

  private queueRandom(): void {
    const count = Number.parseInt(this.randomCountText);
    if (Number.isNaN(count) || count < 1 || count > 200) {
      return;
    }
    this.showRandomDialog = false;
    this.repo.queueRandomTracks(count).then((added) => {
      toast(this.s.randomQueued.replace('%d', `${added}`));
    }).catch((error: Error) => toast(friendlyErrorMessage(error)));
  }

  private confirmScan(): void {
    promptAction.showDialog({
      title: this.s.scanLibrary,
      message: `${this.s.scanDialogBody}\n${this.s.rebuildHint}`,
      buttons: [
        { text: this.s.cancel, color: AppColors.TEXT_SECONDARY },
        { text: this.s.rebuildLibrary, color: AppColors.ERROR },
        { text: this.s.scanUpdate, color: AppColors.PRIMARY }
      ]
    }).then((result) => {
      if (result.index === 1) {
        this.repo.startScan(true).then(() => toast(this.s.rebuildStarted))
          .catch((error: Error) => toast(friendlyErrorMessage(error)));
      } else if (result.index === 2) {
        this.repo.startScan(false).then(() => toast(this.s.scanStartedUpdate))
          .catch((error: Error) => toast(friendlyErrorMessage(error)));
      }
    }).catch(() => {
    });
  }

  private coverFor(track: Track): string {
    const server = this.repo.state.server;
    return server === null ? '' : coverUrl(server, track.coverId, this.repo.state.currentPlayerId);
  }

  private searchActive(): boolean {
    return this.query.trim().length > 0;
  }

  @Builder
  Header() {
    Row() {
      Text(this.s.libraryTitle)
        .fontSize(28)
        .fontWeight(FontWeight.Bold)
        .fontColor(AppColors.TEXT)
        .layoutWeight(1)
      Row() {
        UiIcon({ kind: UiIconKind.REFRESH, iconColor: AppColors.TEXT, iconSize: 20 })
      }
      .width(38)
      .height(38)
      .justifyContent(FlexAlign.Center)
      .onClick(() => this.confirmScan())
    }
    .width('100%')
    .padding({ left: 16, right: 12, top: 8, bottom: 4 })
  }

  @Builder
  SearchBar() {
    Row({ space: 10 }) {
      UiIcon({ kind: UiIconKind.SEARCH, iconColor: AppColors.TEXT_SECONDARY, iconSize: 20 })
      TextInput({ placeholder: this.s.searchHint, text: this.query })
        .fontSize(14)
        .fontColor(AppColors.TEXT)
        .placeholderColor(AppColors.TEXT_SECONDARY)
        .backgroundColor(Color.Transparent)
        .padding(0)
        .layoutWeight(1)
        .height(44)
        .onChange((value: string) => {
          this.query = value;
          this.scheduleSearch();
        })
        .onFocus(() => {
          this.searchFocused = true;
        })
        .onBlur(() => {
          this.searchFocused = false;
        })
        .onSubmit(() => {
          this.doSearch();
        })
      if (this.query.length > 0) {
        UiIcon({ kind: UiIconKind.CLOSE, iconColor: AppColors.TEXT_SECONDARY, iconSize: 18 })
          .onClick(() => {
            this.query = '';
            this.searchSongs = [];
            this.searchAlbums = [];
          })
      }
    }
    .width('100%')
    .height(48)
    .padding({ left: 14, right: 14 })
    .backgroundColor(withAlpha(AppColors.SURFACE, '99'))
    .borderRadius(16)
    .border({
      width: 1,
      color: this.searchFocused ? AppColors.PRIMARY : withAlpha(AppColors.OUTLINE, 'CC')
    })
  }

  @Builder
  StatLine() {
    if (this.statsText().length > 0) {
      Text(this.statsText())
        .fontSize(11)
        .fontColor(AppColors.TEXT_SECONDARY)
        .width('100%')
        .margin({ top: 2 })
    }
  }

  private cardWidth(): string {
    if (this.columns >= 5) {
      return '19.2%';
    }
    if (this.columns === 4) {
      return '23.8%';
    }
    if (this.columns === 3) {
      return '32%';
    }
    return '48.4%';
  }

  @Builder
  CategoryCard(icon: UiIconKind, title: string, subtitle: string, action: () => void) {
    Column({ space: 0 }) {
      Column() {
        UiIcon({ kind: icon, iconColor: AppColors.PRIMARY, iconSize: 22 })
      }
      .width(44)
      .height(44)
      .justifyContent(FlexAlign.Center)
      .backgroundColor(withAlpha(AppColors.PRIMARY, '1C'))
      .borderRadius(11)

      Text(title)
        .fontSize(17)
        .fontWeight(FontWeight.Medium)
        .fontColor(AppColors.TEXT)
        .maxLines(1)
        .textOverflow({ overflow: TextOverflow.Ellipsis })
        .width('100%')
        .margin({ top: 11 })

      Text(subtitle)
        .fontSize(12)
        .fontColor(AppColors.TEXT_SECONDARY)
        .maxLines(1)
        .textOverflow({ overflow: TextOverflow.Ellipsis })
        .width('100%')
        .margin({ top: 3 })
    }
    .width(this.cardWidth())
    .height(this.columns >= 5 ? 132 : 120)
    .padding({ left: 14, right: 12, top: 14 })
    .alignItems(HorizontalAlign.Start)
    .backgroundColor(withAlpha(AppColors.SURFACE, 'DD'))
    .borderRadius(16)
    .onClick(() => action())
  }

  @Builder
  GridContent() {
    Flex({ wrap: FlexWrap.Wrap, justifyContent: FlexAlign.SpaceBetween }) {
      this.CategoryCard(UiIconKind.MUSIC, this.s.albums, this.albumCountText(), () => {
        this.pathStack.pushPathByName('albums', new AlbumsParam('', '', '', this.s.albums));
      })
      this.CategoryCard(UiIconKind.LIST, this.s.songs, this.songCountText(), () => {
        this.pathStack.pushPathByName('songs', '');
      })
      this.CategoryCard(UiIconKind.QUEUE, this.s.myPlaylists, this.s.myPlaylistsSub, () => {
        this.pathStack.pushPathByName('myPlaylists', '');
      })
      this.CategoryCard(UiIconKind.ARTIST, this.s.artists, this.artistCountText(), () => {
        this.pathStack.pushPathByName('artists', '');
      })
      this.CategoryCard(UiIconKind.TAG, this.s.genres, this.genreCountText(), () => {
        this.pathStack.pushPathByName('genres', '');
      })
      this.CategoryCard(UiIconKind.CALENDAR, this.s.years, this.s.browseByYear, () => {
        this.pathStack.pushPathByName('years', '');
      })
      this.CategoryCard(UiIconKind.FOLDER, this.s.folders, this.s.browseByFolder, () => {
        this.pathStack.pushPathByName('folder', new FolderParam('', this.s.folders));
      })
      this.CategoryCard(UiIconKind.QUEUE, this.s.playlists, this.s.playlists, () => {
        this.pathStack.pushPathByName('serverPlaylists', '');
      })
    }
    .width('100%')
    .margin({ top: 4 })
  }

  @Builder
  ShuffleBanner() {
    Row({ space: 12 }) {
      Column() {
        UiIcon({ kind: UiIconKind.SHUFFLE, iconColor: AppColors.PRIMARY, iconSize: 22 })
      }
      .width(40)
      .height(40)
      .justifyContent(FlexAlign.Center)
      .backgroundColor(withAlpha(AppColors.PRIMARY, '1C'))
      .borderRadius(10)

      Column({ space: 2 }) {
        Text(this.s.randomAlbum)
          .fontSize(15)
          .fontWeight(FontWeight.Medium)
          .fontColor(AppColors.TEXT)
        Text(this.s.randomAlbumSub)
          .fontSize(11)
          .fontColor(AppColors.TEXT_SECONDARY)
          .maxLines(1)
          .textOverflow({ overflow: TextOverflow.Ellipsis })
      }
      .alignItems(HorizontalAlign.Start)
      .layoutWeight(1)
    }
    .width('100%')
    .padding(12)
    .backgroundColor(withAlpha(AppColors.PRIMARY, '14'))
    .borderRadius(16)
    .margin({ top: 4, bottom: 8 })
    .onClick(() => this.run(() => this.repo.randomAlbum()))
  }

  @Builder
  FeatureBanner(icon: UiIconKind, title: string, subtitle: string, action: () => void) {
    Row({ space: 12 }) {
      Column() {
        UiIcon({ kind: icon, iconColor: AppColors.PRIMARY, iconSize: 22 })
      }
      .width(40)
      .height(40)
      .justifyContent(FlexAlign.Center)
      .backgroundColor(withAlpha(AppColors.PRIMARY, '1C'))
      .borderRadius(10)

      Column({ space: 2 }) {
        Text(title)
          .fontSize(15)
          .fontWeight(FontWeight.Medium)
          .fontColor(AppColors.TEXT)
        Text(subtitle)
          .fontSize(11)
          .fontColor(AppColors.TEXT_SECONDARY)
          .maxLines(1)
          .textOverflow({ overflow: TextOverflow.Ellipsis })
      }
      .alignItems(HorizontalAlign.Start)
      .layoutWeight(1)
    }
    .width('100%')
    .padding(12)
    .backgroundColor(withAlpha(AppColors.SURFACE, 'DD'))
    .borderRadius(16)
    .margin({ top: 4, bottom: 8 })
    .onClick(() => action())
  }

  @Builder
  RandomCountOverlay() {
    Column() {
      Column({ space: 12 }) {
        Text(this.s.randomCountTitle)
          .fontSize(17)
          .fontWeight(FontWeight.Medium)
          .fontColor(AppColors.TEXT)
          .width('100%')
        Text(this.s.randomCountHint)
          .fontSize(12)
          .fontColor(AppColors.TEXT_SECONDARY)
          .width('100%')
        TextInput({ text: this.randomCountText, placeholder: '20' })
          .type(InputType.Number)
          .fontSize(15)
          .fontColor(AppColors.TEXT)
          .backgroundColor(AppColors.SURFACE_HIGH)
          .borderRadius(8)
          .height(42)
          .onChange((value: string) => {
            let digits = '';
            for (let i = 0; i < value.length; i++) {
              const ch = value.charAt(i);
              if (ch >= '0' && ch <= '9') {
                digits += ch;
              }
            }
            this.randomCountText = digits.substring(0, 3);
          })
        Row({ space: 8 }) {
          Button(this.s.cancel)
            .fontSize(13)
            .fontColor(AppColors.TEXT)
            .backgroundColor(AppColors.SURFACE_HIGH)
            .layoutWeight(1)
            .onClick(() => {
              this.showRandomDialog = false;
            })
          Button(this.s.confirm)
            .fontSize(13)
            .fontColor(AppColors.ON_PRIMARY)
            .backgroundColor(AppColors.PRIMARY)
            .layoutWeight(1)
            .enabled(this.randomCountText.length > 0)
            .onClick(() => this.queueRandom())
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

  @Builder
  SearchResults() {
    Column() {
      if (this.searching) {
        Row() {
          LoadingProgress()
            .width(20)
            .height(20)
            .color(AppColors.PRIMARY)
          Text(this.s.loading)
            .fontSize(12)
            .fontColor(AppColors.TEXT_SECONDARY)
            .margin({ left: 8 })
        }
        .width('100%')
        .padding(16)
      } else if (this.searchSongs.length === 0 && this.searchAlbums.length === 0 &&
        this.searchArtists.length === 0) {
        EmptyState({
          title: this.s.noSearchResultsTitle,
          body: this.s.noSearchResultsBody.replace('%s', this.query)
        })
      } else {
        ForEach(this.searchArtists, (artist: Artist) => {
          Row({ space: 10 }) {
            Column() {
              UiIcon({ kind: UiIconKind.ARTIST, iconColor: AppColors.PRIMARY, iconSize: 18 })
            }
            .width(38)
            .height(38)
            .justifyContent(FlexAlign.Center)
            .backgroundColor(withAlpha(AppColors.PRIMARY, '18'))
            .borderRadius(9)
            Column({ space: 2 }) {
              Text(artist.name)
                .fontSize(14)
                .fontColor(AppColors.TEXT)
                .maxLines(1)
                .textOverflow({ overflow: TextOverflow.Ellipsis })
              Text(this.s.artists)
                .fontSize(11)
                .fontColor(AppColors.TEXT_SECONDARY)
            }
            .alignItems(HorizontalAlign.Start)
            .layoutWeight(1)
            UiIcon({ kind: UiIconKind.CHEVRON_RIGHT, iconColor: AppColors.TEXT_SECONDARY, iconSize: 16 })
          }
          .width('100%')
          .padding(10)
          .onClick(() => this.pathStack.pushPathByName('albums',
            new AlbumsParam(artist.id, '', '', artist.name)))
        }, (artist: Artist) => `search-artist-${artist.id}`)
        ForEach(this.searchAlbums, (album: Album) => {
          Row({ space: 10 }) {
            Column() {
              UiIcon({ kind: UiIconKind.MUSIC, iconColor: AppColors.PRIMARY, iconSize: 18 })
            }
            .width(38)
            .height(38)
            .justifyContent(FlexAlign.Center)
            .backgroundColor(withAlpha(AppColors.PRIMARY, '18'))
            .borderRadius(9)
            Column({ space: 2 }) {
              Text(album.title)
                .fontSize(14)
                .fontColor(AppColors.TEXT)
                .maxLines(1)
                .textOverflow({ overflow: TextOverflow.Ellipsis })
              Text([album.artist, album.year].filter((value: string) => value.length > 0).join(' · '))
                .fontSize(11)
                .fontColor(AppColors.TEXT_SECONDARY)
                .maxLines(1)
            }
            .alignItems(HorizontalAlign.Start)
            .layoutWeight(1)
            UiIcon({ kind: UiIconKind.CHEVRON_RIGHT, iconColor: AppColors.TEXT_SECONDARY, iconSize: 16 })
          }
          .width('100%')
          .padding(10)
          .onClick(() => this.pathStack.pushPathByName('albumDetail', album))
        }, (album: Album) => `search-album-${album.id}`)
        ForEach(this.searchSongs, (track: Track) => {
          TrackRow({
            track: track,
            coverUrl: this.coverFor(track),
            isCurrent: this.repo.state.nowPlaying !== null &&
              this.repo.state.nowPlaying.track !== null &&
              this.repo.state.nowPlaying.track.id === track.id,
            onTap: () => {
              if (track.id.length > 0) {
                this.run(() => this.repo.playTrack(track.id));
              }
            }
          })
        }, (track: Track) => `search-track-${track.id}-${track.title}`)
      }
    }
    .width('100%')
  }

  build() {
    Stack() {
      Column() {
        this.Header()

        Column({ space: 8 }) {
          this.SearchBar()
          this.StatLine()
          if (this.scanLabel().length > 0) {
            Row({ space: 6 }) {
              LoadingProgress()
                .width(13)
                .height(13)
                .color(AppColors.PRIMARY)
              Text(this.scanLabel())
                .fontSize(10)
                .fontColor(AppColors.PRIMARY)
                .maxLines(1)
                .textOverflow({ overflow: TextOverflow.Ellipsis })
                .layoutWeight(1)
            }
            .width('100%')
          }
        }
        .width('100%')
        .padding({ left: 16, right: 16, top: 2, bottom: 8 })

        Scroll() {
          Column() {
            if (this.searchActive()) {
              this.SearchResults()
            } else {
              this.GridContent()
              this.ShuffleBanner()
              this.FeatureBanner(UiIconKind.SPARKLE, this.s.dailyRecommendation,
                this.s.dailyRecommendationSub, () => this.pathStack.pushPathByName('daily', ''))
              this.FeatureBanner(UiIconKind.QUEUE, this.s.randomToQueue,
                this.s.randomToQueueSub, () => {
                  this.showRandomDialog = true;
                })
              this.FeatureBanner(UiIconKind.CALENDAR, this.s.sortNew,
                this.s.recentlyAddedSub, () => this.pathStack.pushPathByName('albums',
                  new AlbumsParam('', '', '', this.s.sortNew, 'new')))
            }

            if (this.message.length > 0) {
              Text(this.message)
                .fontSize(11)
                .fontColor(AppColors.WARN)
                .width('100%')
                .padding({ top: 8 })
            }
          }
          .width('100%')
          .padding({ left: 16, right: 16, bottom: 12 })
        }
        .width('100%')
        .layoutWeight(1)
        .scrollBar(BarState.Off)
        .align(Alignment.TopStart)
      }
      .width('100%')
      .height('100%')

      if (this.showRandomDialog) {
        this.RandomCountOverlay()
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor(AppColors.BACKGROUND)
  }
}
