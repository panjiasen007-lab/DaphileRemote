import { promptAction } from '@kit.ArkUI';
import { AppContext } from '../../app/AppContext';
import {
  Album, Artist, Genre, YearItem, coverUrl, friendlyErrorMessage
} from '../../data/model/Models';
import { DaphileRepository } from '../../data/repo/DaphileRepository';
import { AppColors, withAlpha } from '../theme/Theme';
import { CoverArt, EmptyState } from '../components/Common';
import { UiIcon, UiIconKind } from '../components/UiIcon';
import { TransportIcon, TransportKind } from '../components/TransportIcon';
import { stringsFor, UiStrings } from '../i18n/I18n';

function toast(message: string): void {
  promptAction.showToast({ message: message, duration: 2200 });
}

export class AlbumsParam {
  artistId: string;
  genreId: string;
  year: string;
  title: string;
  sort: string;

  constructor(artistId: string, genreId: string, year: string, title: string, sort: string = 'album') {
    this.artistId = artistId;
    this.genreId = genreId;
    this.year = year;
    this.title = title;
    this.sort = sort;
  }
}

@ComponentV2
export struct AlbumsPage {
  @Param pathStack: NavPathStack = new NavPathStack();
  @Param param: AlbumsParam = new AlbumsParam('', '', '', '');
  @Param gridColumns: number = 3;
  private repo: DaphileRepository = AppContext.get().repository;

  @Local albums: Album[] = [];
  @Local loading: boolean = false;
  @Local hasMore: boolean = true;
  @Local grid: boolean = true;
  @Local sort: string = 'album';
  @Local message: string = '';

  aboutToAppear(): void {
    if (this.param.sort.length > 0) {
      this.sort = this.param.sort;
    }
    this.loadMore();
  }

  @Computed
  get s(): UiStrings {
    const settings = this.repo.state.settings;
    return stringsFor(settings.theme.length > 0 ? settings.language : 'en');
  }

  private loadMore(): void {
    if (this.loading || !this.hasMore) {
      return;
    }
    this.loading = true;
    this.repo.albums(this.albums.length, 60, this.sort,
      this.param.artistId, this.param.genreId, this.param.year).then((page) => {
      this.albums = this.albums.concat(page.items);
      this.hasMore = page.hasMore;
      this.loading = false;
    }).catch((error: Error) => {
      this.loading = false;
      this.hasMore = false;
      this.message = friendlyErrorMessage(error);
    });
  }

  private cover(album: Album): string {
    const server = this.repo.state.server;
    return server === null ? '' : coverUrl(server, album.artworkId, '');
  }

  @Builder
  Header() {
    Row({ space: 6 }) {
      Row() {
        UiIcon({ kind: UiIconKind.BACK, iconColor: AppColors.TEXT, iconSize: 21 })
      }
      .width(38)
      .height(38)
      .justifyContent(FlexAlign.Center)
      .onClick(() => this.pathStack.pop())

      Text(this.param.title.length > 0 ? this.param.title : this.s.albums)
        .fontSize(24)
        .fontWeight(FontWeight.Bold)
        .fontColor(AppColors.TEXT)
        .layoutWeight(1)
        .maxLines(1)
        .textOverflow({ overflow: TextOverflow.Ellipsis })

      Row() {
        UiIcon({ kind: UiIconKind.SHUFFLE, iconColor: AppColors.TEXT, iconSize: 19 })
      }
      .width(36)
      .height(36)
      .justifyContent(FlexAlign.Center)
      .onClick(() => {
        this.repo.randomAlbum().catch((error: Error) => toast(friendlyErrorMessage(error)));
      })
      Row() {
        UiIcon({ kind: UiIconKind.SORT, iconColor: AppColors.TEXT, iconSize: 19 })
      }
      .width(36)
      .height(36)
      .justifyContent(FlexAlign.Center)
      .onClick(() => {
        this.sort = this.sort === 'album' ? 'new' : 'album';
        this.albums = [];
        this.hasMore = true;
        this.loadMore();
      })
      Row() {
        UiIcon({
          kind: this.grid ? UiIconKind.LIST : UiIconKind.GRID,
          iconColor: AppColors.TEXT,
          iconSize: 19
        })
      }
      .width(36)
      .height(36)
      .justifyContent(FlexAlign.Center)
      .onClick(() => {
        this.grid = !this.grid;
      })
    }
    .width('100%')
    .padding({ left: 10, right: 10, top: 8, bottom: 6 })
  }

  private gridWidth(): string {
    if (this.gridColumns >= 6) {
      return '15.7%';
    }
    if (this.gridColumns === 5) {
      return '19%';
    }
    if (this.gridColumns === 4) {
      return '23.8%';
    }
    return '31.6%';
  }

  @Builder
  AlbumGridItem(album: Album) {
    Column({ space: 0 }) {
      Stack() {
        if (this.cover(album).length > 0) {
          Image(this.cover(album))
            .width('100%')
            .height('100%')
            .objectFit(ImageFit.Cover)
            .interpolation(ImageInterpolation.Medium)
        } else {
          Column() {
            Text('♪')
              .fontSize(24)
              .fontColor(AppColors.TEXT_SECONDARY)
          }
          .width('100%')
          .height('100%')
          .justifyContent(FlexAlign.Center)
          .backgroundColor(AppColors.SURFACE_HIGH)
        }
      }
      .width('100%')
      .aspectRatio(1)
      .borderRadius(8)
      .clip(true)

      Text(album.title)
        .fontSize(14)
        .fontColor(AppColors.TEXT)
        .maxLines(2)
        .lineHeight(18)
        .textOverflow({ overflow: TextOverflow.Ellipsis })
        .width('100%')
        .height(36)
        .margin({ top: 6 })
      Text(album.artist)
        .fontSize(12)
        .fontColor(AppColors.TEXT_SECONDARY)
        .maxLines(1)
        .textOverflow({ overflow: TextOverflow.Ellipsis })
        .width('100%')
        .margin({ top: 1 })
    }
    .width(this.gridWidth())
    .margin({ bottom: 12 })
    .onClick(() => this.pathStack.pushPathByName('albumDetail', album))
    .gesture(
      LongPressGesture({ repeat: false })
        .onAction(() => {
          this.repo.playAlbum(album.id).catch((error: Error) => toast(friendlyErrorMessage(error)));
        })
    )
  }

  @Builder
  AlbumListItem(album: Album) {
    Row({ space: 10 }) {
      CoverArt({ url: this.cover(album), coverSize: 52, radius: 8 })
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
          .textOverflow({ overflow: TextOverflow.Ellipsis })
      }
      .alignItems(HorizontalAlign.Start)
      .layoutWeight(1)
    }
    .width('100%')
    .padding({ top: 6, bottom: 6 })
    .onClick(() => this.pathStack.pushPathByName('albumDetail', album))
  }

  build() {
    Column() {
      this.Header()
      if (this.loading && this.albums.length === 0) {
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
        .padding(24)
      } else if (this.albums.length === 0) {
        EmptyState({
          title: this.message.length > 0 ? this.message : this.s.noAlbumsTitle,
          body: this.message.length > 0 ? '' : this.s.noAlbumsBody
        })
      } else {
        Scroll() {
          if (this.grid) {
            Flex({ wrap: FlexWrap.Wrap, justifyContent: FlexAlign.SpaceBetween }) {
              ForEach(this.albums, (album: Album) => {
                this.AlbumGridItem(album)
              }, (album: Album) => `grid-${album.id}-${album.title}`)
            }
            .width('100%')
          } else {
            Column() {
              ForEach(this.albums, (album: Album) => {
                this.AlbumListItem(album)
              }, (album: Album) => `list-${album.id}-${album.title}`)
            }
            .width('100%')
          }
        }
        .width('100%')
        .layoutWeight(1)
        .padding({ left: 16, right: 16 })
        .scrollBar(BarState.Off)
        .align(Alignment.TopStart)
        .onReachEnd(() => this.loadMore())
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor(AppColors.BACKGROUND)
  }
}

@ComponentV2
struct BrowseRow {
  @Param icon: UiIconKind = UiIconKind.MUSIC;
  @Require @Param title: string;
  @Param subtitle: string = '';
  @Param showPlay: boolean = false;
  @Event onTap: () => void = () => {
  };
  @Event onPlay: () => void = () => {
  };

  build() {
    Row({ space: 12 }) {
      Column() {
        UiIcon({ kind: this.icon, iconColor: AppColors.PRIMARY, iconSize: 20 })
      }
      .width(40)
      .height(40)
      .justifyContent(FlexAlign.Center)
      .backgroundColor(withAlpha(AppColors.PRIMARY, '1C'))
      .borderRadius(10)

      Column({ space: 2 }) {
        Text(this.title)
          .fontSize(15)
          .fontColor(AppColors.TEXT)
          .maxLines(1)
          .textOverflow({ overflow: TextOverflow.Ellipsis })
        if (this.subtitle.length > 0) {
          Text(this.subtitle)
            .fontSize(11)
            .fontColor(AppColors.TEXT_SECONDARY)
            .maxLines(1)
            .textOverflow({ overflow: TextOverflow.Ellipsis })
        }
      }
      .alignItems(HorizontalAlign.Start)
      .layoutWeight(1)

      if (this.showPlay) {
        Row() {
          TransportIcon({ kind: TransportKind.PLAY, iconColor: AppColors.TEXT, iconSize: 15 })
        }
        .width(34)
        .height(34)
        .justifyContent(FlexAlign.Center)
        .borderRadius(17)
        .backgroundColor(withAlpha(AppColors.TEXT, '0D'))
        .onClick(() => this.onPlay())
      }
      UiIcon({ kind: UiIconKind.CHEVRON_RIGHT, iconColor: AppColors.TEXT_SECONDARY, iconSize: 17 })
    }
    .width('100%')
    .padding({ top: 8, bottom: 8 })
    .onClick(() => this.onTap())
  }
}

@ComponentV2
export struct ArtistsPage {
  @Param pathStack: NavPathStack = new NavPathStack();
  private repo: DaphileRepository = AppContext.get().repository;

  @Local items: Artist[] = [];
  @Local loading: boolean = false;
  @Local hasMore: boolean = true;

  aboutToAppear(): void {
    this.loadMore();
  }

  @Computed
  get s(): UiStrings {
    const settings = this.repo.state.settings;
    return stringsFor(settings.theme.length > 0 ? settings.language : 'en');
  }

  private loadMore(): void {
    if (this.loading || !this.hasMore) {
      return;
    }
    this.loading = true;
    this.repo.artists(this.items.length, 100).then((page) => {
      this.items = this.items.concat(page.items);
      this.hasMore = page.hasMore;
      this.loading = false;
    }).catch(() => {
      this.loading = false;
      this.hasMore = false;
    });
  }

  build() {
    Column() {
      Row({ space: 6 }) {
        Row() {
          UiIcon({ kind: UiIconKind.BACK, iconColor: AppColors.TEXT, iconSize: 21 })
        }
        .width(38)
        .height(38)
        .justifyContent(FlexAlign.Center)
        .onClick(() => this.pathStack.pop())
        Text(this.s.artists)
          .fontSize(24)
          .fontWeight(FontWeight.Bold)
          .fontColor(AppColors.TEXT)
          .layoutWeight(1)
      }
      .width('100%')
      .padding({ left: 10, right: 16, top: 8, bottom: 6 })

      if (this.items.length === 0) {
        EmptyState({ title: this.s.artists, body: '' })
      } else {
        List() {
          ForEach(this.items, (artist: Artist) => {
            ListItem() {
              BrowseRow({
                icon: UiIconKind.ARTIST,
                title: artist.name,
                subtitle: this.s.artists,
                showPlay: true,
                onTap: () => {
                  this.pathStack.pushPathByName('albums',
                    new AlbumsParam(artist.id, '', '', artist.name));
                },
                onPlay: () => {
                  this.repo.playArtist(artist.id).catch((error: Error) => {
                    toast(friendlyErrorMessage(error));
                  });
                }
              })
            }
          }, (artist: Artist) => `artist-${artist.id}-${artist.name}`)
        }
        .width('100%')
        .layoutWeight(1)
        .padding({ left: 16, right: 16 })
        .scrollBar(BarState.Off)
        .align(Alignment.TopStart)
        .onReachEnd(() => this.loadMore())
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor(AppColors.BACKGROUND)
  }
}

@ComponentV2
export struct GenresPage {
  @Param pathStack: NavPathStack = new NavPathStack();
  private repo: DaphileRepository = AppContext.get().repository;

  @Local items: Genre[] = [];
  @Local loading: boolean = false;
  @Local hasMore: boolean = true;

  aboutToAppear(): void {
    this.loadMore();
  }

  @Computed
  get s(): UiStrings {
    const settings = this.repo.state.settings;
    return stringsFor(settings.theme.length > 0 ? settings.language : 'en');
  }

  private loadMore(): void {
    if (this.loading || !this.hasMore) {
      return;
    }
    this.loading = true;
    this.repo.genres(this.items.length, 100).then((page) => {
      this.items = this.items.concat(page.items);
      this.hasMore = page.hasMore;
      this.loading = false;
    }).catch(() => {
      this.loading = false;
      this.hasMore = false;
    });
  }

  build() {
    Column() {
      Row({ space: 6 }) {
        Row() {
          UiIcon({ kind: UiIconKind.BACK, iconColor: AppColors.TEXT, iconSize: 21 })
        }
        .width(38)
        .height(38)
        .justifyContent(FlexAlign.Center)
        .onClick(() => this.pathStack.pop())
        Text(this.s.genres)
          .fontSize(24)
          .fontWeight(FontWeight.Bold)
          .fontColor(AppColors.TEXT)
          .layoutWeight(1)
      }
      .width('100%')
      .padding({ left: 10, right: 16, top: 8, bottom: 6 })

      if (this.items.length === 0) {
        EmptyState({ title: this.s.genres, body: '' })
      } else {
        List() {
          ForEach(this.items, (genre: Genre) => {
            ListItem() {
              BrowseRow({
                icon: UiIconKind.TAG,
                title: genre.name,
                subtitle: this.s.genres,
                onTap: () => {
                  this.pathStack.pushPathByName('albums',
                    new AlbumsParam('', genre.id, '', genre.name));
                }
              })
            }
          }, (genre: Genre) => `genre-${genre.id}-${genre.name}`)
        }
        .width('100%')
        .layoutWeight(1)
        .padding({ left: 16, right: 16 })
        .scrollBar(BarState.Off)
        .align(Alignment.TopStart)
        .onReachEnd(() => this.loadMore())
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor(AppColors.BACKGROUND)
  }
}

@ComponentV2
export struct YearsPage {
  @Param pathStack: NavPathStack = new NavPathStack();
  private repo: DaphileRepository = AppContext.get().repository;

  @Local items: YearItem[] = [];
  @Local loading: boolean = false;
  @Local hasMore: boolean = true;

  aboutToAppear(): void {
    this.loadMore();
  }

  @Computed
  get s(): UiStrings {
    const settings = this.repo.state.settings;
    return stringsFor(settings.theme.length > 0 ? settings.language : 'en');
  }

  private loadMore(): void {
    if (this.loading || !this.hasMore) {
      return;
    }
    this.loading = true;
    this.repo.years(this.items.length, 100).then((page) => {
      this.items = this.items.concat(page.items);
      this.hasMore = page.hasMore;
      this.loading = false;
    }).catch(() => {
      this.loading = false;
      this.hasMore = false;
    });
  }

  build() {
    Column() {
      Row({ space: 6 }) {
        Row() {
          UiIcon({ kind: UiIconKind.BACK, iconColor: AppColors.TEXT, iconSize: 21 })
        }
        .width(38)
        .height(38)
        .justifyContent(FlexAlign.Center)
        .onClick(() => this.pathStack.pop())
        Text(this.s.years)
          .fontSize(24)
          .fontWeight(FontWeight.Bold)
          .fontColor(AppColors.TEXT)
          .layoutWeight(1)
      }
      .width('100%')
      .padding({ left: 10, right: 16, top: 8, bottom: 6 })

      if (this.items.length === 0) {
        EmptyState({ title: this.s.years, body: '' })
      } else {
        List() {
          ForEach(this.items, (item: YearItem) => {
            ListItem() {
              BrowseRow({
                icon: UiIconKind.CALENDAR,
                title: item.year,
                subtitle: this.s.browseByYear,
                onTap: () => {
                  this.pathStack.pushPathByName('albums', new AlbumsParam('', '', item.year, item.year));
                }
              })
            }
          }, (item: YearItem) => `year-${item.year}`)
        }
        .width('100%')
        .layoutWeight(1)
        .padding({ left: 16, right: 16 })
        .scrollBar(BarState.Off)
        .align(Alignment.TopStart)
        .onReachEnd(() => this.loadMore())
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor(AppColors.BACKGROUND)
  }
}
