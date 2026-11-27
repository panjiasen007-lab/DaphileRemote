import { AppContext } from '../app/AppContext';
import { common } from '@kit.AbilityKit';
import { deviceInfo } from '@kit.BasicServicesKit';
import { window } from '@kit.ArkUI';
import { Album, ConnectionState } from '../data/model/Models';
import { AlbumDetailPage } from '../ui/screens/AlbumDetailPage';
import { SongsPage } from '../ui/screens/SongsPage';
import { FolderPage, FolderParam } from '../ui/screens/FolderPage';
import {
  CustomPlaylistPage, CustomPlaylistParam, DailyRecommendationPage, MyPlaylistsPage
} from '../ui/screens/MyPlaylistsPage';
import { AudioOutputPage, SystemInfoPage } from '../ui/screens/InfoPages';
import {
  AlbumsPage, AlbumsParam, ArtistsPage, GenresPage, YearsPage
} from '../ui/screens/LibraryBrowsePages';
import {
  PluginBrowserPage, PluginBrowserParam, ServerPlaylistDetailPage, ServerPlaylistParam,
  ServerPlaylistsPage
} from '../ui/screens/BrowsePages';
import { DaphileRepository } from '../data/repo/DaphileRepository';
import { NowPlayingPage } from '../ui/screens/NowPlayingPage';
import { LibraryPage } from '../ui/screens/LibraryPage';
import { RadioPage } from '../ui/screens/RadioPage';
import { PlayersPage } from '../ui/screens/PlayersPage';
import { SettingsPage, ServersPage, ServerEditPage, ServerEditParam } from '../ui/screens/SettingsPage';
import { LyricsPage } from '../ui/screens/LyricsPage';
import { AppColors, withAlpha } from '../ui/theme/Theme';
import { UiIcon, UiIconKind } from '../ui/components/UiIcon';
import { stringsFor, UiStrings } from '../ui/i18n/I18n';

@Entry
@ComponentV2
struct Index {
  private repo: DaphileRepository = AppContext.get().repository;
  private pathStack: NavPathStack = new NavPathStack();
  @Local currentTab: number = 0;
  @Local viewportWidth: number = 360;
  private deviceType: string = '';
  private appWindow: window.Window | null = null;

  aboutToAppear(): void {
    try {
      this.deviceType = deviceInfo.deviceType;
    } catch (error) {
      this.deviceType = '';
    }
    try {
      const context = getContext(this) as common.UIAbilityContext;
      window.getLastWindow(context).then((win: window.Window) => {
        this.appWindow = win;
        this.updateViewport(win.getWindowProperties().windowRect.width);
        win.on('windowSizeChange', this.onWindowSizeChange);
      }).catch(() => {
      });
    } catch (error) {
      // 窗口信息不可用时按手机布局兜底
    }
  }

  aboutToDisappear(): void {
    if (this.appWindow !== null) {
      this.appWindow.off('windowSizeChange', this.onWindowSizeChange);
      this.appWindow = null;
    }
  }

  private onWindowSizeChange = (size: window.Size): void => {
    this.updateViewport(size.width);
  };

  private updateViewport(widthPx: number): void {
    const width = px2vp(widthPx);
    if (width > 0) {
      this.viewportWidth = width;
    }
  }

  /**
   * 布局策略：手动选择优先；自动模式按设备类型 + 窗口宽度断点判定。
   * 参考断点：手机 <600vp，折叠屏 600~840vp，平板 840~1200vp，PC ≥1200vp。
   */
  private resolveLayout(): string {
    const manual = this.repo.state.settings.layoutMode;
    if (manual === 'phone' || manual === 'foldable' || manual === 'tablet' || manual === 'pc') {
      return manual;
    }
    const width = this.viewportWidth;
    if (this.deviceType === 'tablet') {
      if (width >= 840) {
        return 'tablet';
      }
      return width >= 600 ? 'foldable' : 'phone';
    }
    if (this.deviceType === '2in1') {
      if (width >= 1000) {
        return 'pc';
      }
      if (width >= 760) {
        return 'tablet';
      }
      return 'phone';
    }
    if (width >= 840) {
      return 'tablet';
    }
    if (width >= 600) {
      return 'foldable';
    }
    return 'phone';
  }

  @Computed
  get layout(): string {
    return this.resolveLayout();
  }

  @Computed
  get sidebarWidth(): number {
    if (this.layout === 'pc') {
      return 248;
    }
    if (this.layout === 'tablet') {
      return 208;
    }
    return 88;
  }

  @Computed
  get wideFrame(): boolean {
    return this.viewportWidth >= 840;
  }

  @Computed
  get libraryColumns(): number {
    if (this.layout === 'pc' || this.layout === 'tablet') {
      return 5;
    }
    if (this.layout === 'foldable') {
      return 3;
    }
    return 2;
  }

  @Computed
  get albumColumns(): number {
    if (this.layout === 'pc') {
      return 6;
    }
    if (this.layout === 'tablet') {
      return 5;
    }
    if (this.layout === 'foldable') {
      return 4;
    }
    return 3;
  }

  @Computed
  get s(): UiStrings {
    const settings = this.repo.state.settings;
    // 同时读取 theme，确保主题切换时页面重新构建
    return stringsFor(settings.theme.length > 0 ? settings.language : 'en');
  }

  private connectionLabel(): string {
    switch (this.repo.state.connection) {
      case ConnectionState.CONNECTING: return this.s.connectingDaphile;
      case ConnectionState.RECONNECTING: return this.s.reconnectingBanner;
      case ConnectionState.LOST: return this.s.serverUnreachable;
      default: return this.s.notConnected;
    }
  }

  @Builder
  PageMap(name: string, param: Object) {
    if (name === 'servers') {
      NavDestination() {
        Row() {
          Column() {
            ServersPage({ pathStack: this.pathStack })
          }
          .width('100%')
          .height('100%')
          .constraintSize({ maxWidth: 1000 })
        }
        .width('100%')
        .height('100%')
        .justifyContent(FlexAlign.Center)
      }
      .title('服务器管理')
      .hideTitleBar(true)
      .backgroundColor(AppColors.BACKGROUND)
    } else if (name === 'serverEdit') {
      NavDestination() {
        Row() {
          Column() {
            ServerEditPage({ pathStack: this.pathStack, initial: param as ServerEditParam })
          }
          .width('100%')
          .height('100%')
          .constraintSize({ maxWidth: 1000 })
        }
        .width('100%')
        .height('100%')
        .justifyContent(FlexAlign.Center)
      }
      .title('服务器')
      .hideTitleBar(true)
      .backgroundColor(AppColors.BACKGROUND)
    } else if (name === 'lyrics') {
      NavDestination() {
        LyricsPage({ pathStack: this.pathStack })
      }
      .title(this.s.lyrics)
      .hideTitleBar(true)
      .backgroundColor(AppColors.BACKGROUND)
    } else if (name === 'albumDetail') {
      NavDestination() {
        AlbumDetailPage({ pathStack: this.pathStack, album: param as Album })
      }
      .title(this.s.albums)
      .hideTitleBar(true)
      .backgroundColor(AppColors.BACKGROUND)
    } else if (name === 'albums') {
      NavDestination() {
        AlbumsPage({ pathStack: this.pathStack, param: param as AlbumsParam,
          gridColumns: this.albumColumns })
      }
      .title(this.s.albums)
      .hideTitleBar(true)
      .backgroundColor(AppColors.BACKGROUND)
    } else if (name === 'artists') {
      NavDestination() {
        ArtistsPage({ pathStack: this.pathStack })
      }
      .title(this.s.artists)
      .hideTitleBar(true)
      .backgroundColor(AppColors.BACKGROUND)
    } else if (name === 'genres') {
      NavDestination() {
        GenresPage({ pathStack: this.pathStack })
      }
      .title(this.s.genres)
      .hideTitleBar(true)
      .backgroundColor(AppColors.BACKGROUND)
    } else if (name === 'years') {
      NavDestination() {
        YearsPage({ pathStack: this.pathStack })
      }
      .title(this.s.years)
      .hideTitleBar(true)
      .backgroundColor(AppColors.BACKGROUND)
    } else if (name === 'songs') {
      NavDestination() {
        SongsPage({ pathStack: this.pathStack })
      }
      .title(this.s.songs)
      .hideTitleBar(true)
      .backgroundColor(AppColors.BACKGROUND)
    } else if (name === 'folder') {
      NavDestination() {
        FolderPage({ pathStack: this.pathStack, folder: param as FolderParam })
      }
      .title(this.s.folders)
      .hideTitleBar(true)
      .backgroundColor(AppColors.BACKGROUND)
    } else if (name === 'myPlaylists') {
      NavDestination() {
        MyPlaylistsPage({ pathStack: this.pathStack })
      }
      .title(this.s.myPlaylists)
      .hideTitleBar(true)
      .backgroundColor(AppColors.BACKGROUND)
    } else if (name === 'customPlaylist') {
      NavDestination() {
        CustomPlaylistPage({ pathStack: this.pathStack, param: param as CustomPlaylistParam })
      }
      .title(this.s.myPlaylists)
      .hideTitleBar(true)
      .backgroundColor(AppColors.BACKGROUND)
    } else if (name === 'daily') {
      NavDestination() {
        DailyRecommendationPage({ pathStack: this.pathStack })
      }
      .title(this.s.dailyRecommendation)
      .hideTitleBar(true)
      .backgroundColor(AppColors.BACKGROUND)
    } else if (name === 'audioOutput') {
      NavDestination() {
        Row() {
          Column() {
            AudioOutputPage({ pathStack: this.pathStack })
          }
          .width('100%')
          .height('100%')
          .constraintSize({ maxWidth: 1000 })
        }
        .width('100%')
        .height('100%')
        .justifyContent(FlexAlign.Center)
      }
      .title(this.s.audioOutputScreenTitle)
      .hideTitleBar(true)
      .backgroundColor(AppColors.BACKGROUND)
    } else if (name === 'systemInfo') {
      NavDestination() {
        Row() {
          Column() {
            SystemInfoPage({ pathStack: this.pathStack })
          }
          .width('100%')
          .height('100%')
          .constraintSize({ maxWidth: 1000 })
        }
        .width('100%')
        .height('100%')
        .justifyContent(FlexAlign.Center)
      }
      .title(this.s.systemInfo)
      .hideTitleBar(true)
      .backgroundColor(AppColors.BACKGROUND)
    } else if (name === 'serverPlaylists') {
      NavDestination() {
        ServerPlaylistsPage({ pathStack: this.pathStack })
      }
      .title(this.s.playlists)
      .hideTitleBar(true)
      .backgroundColor(AppColors.BACKGROUND)
    } else if (name === 'serverPlaylistDetail') {
      NavDestination() {
        ServerPlaylistDetailPage({
          pathStack: this.pathStack,
          param: param as ServerPlaylistParam
        })
      }
      .title(this.s.playlists)
      .hideTitleBar(true)
      .backgroundColor(AppColors.BACKGROUND)
    } else if (name === 'pluginBrowser') {
      NavDestination() {
        PluginBrowserPage({ pathStack: this.pathStack, param: param as PluginBrowserParam })
      }
      .title(this.s.radios)
      .hideTitleBar(true)
      .backgroundColor(AppColors.BACKGROUND)
    }
  }

  @Builder
  ConnectionBanner() {
    if (this.repo.state.servers.length > 0 &&
      this.repo.state.connection !== ConnectionState.CONNECTED) {
      Row({ space: 8 }) {
        Text(this.connectionLabel())
          .fontSize(11)
          .fontColor(AppColors.WARN)
          .maxLines(1)
          .layoutWeight(1)
        if (this.repo.state.lastError.length > 0) {
          Text(this.repo.state.lastError)
            .fontSize(10)
            .fontColor(AppColors.TEXT_SECONDARY)
            .maxLines(1)
            .textOverflow({ overflow: TextOverflow.Ellipsis })
            .layoutWeight(1)
        }
        Button(this.s.retry)
          .fontSize(10)
          .height(26)
          .fontColor(AppColors.TEXT)
          .backgroundColor(AppColors.SURFACE_HIGH)
          .onClick(() => this.repo.retryConnection())
      }
      .width('100%')
      .padding({ left: 16, right: 16, top: 6, bottom: 6 })
      .backgroundColor(AppColors.SURFACE)
    }
  }

  @Builder
  SetupView() {
    Column({ space: 14 }) {
      Text('♪')
        .fontSize(64)
        .fontColor(AppColors.PRIMARY)
      Text(this.s.appShortName)
        .fontSize(28)
        .fontWeight(FontWeight.Bold)
        .fontColor(AppColors.TEXT)
      Text('HarmonyOS')
        .fontSize(12)
        .fontColor(AppColors.TEXT_SECONDARY)
      Text(this.s.serverIntro)
        .fontSize(13)
        .fontColor(AppColors.TEXT_SECONDARY)
        .textAlign(TextAlign.Center)
        .padding({ left: 32, right: 32 })
        .margin({ top: 6 })
      Button(this.s.addServer)
        .fontSize(15)
        .fontColor(AppColors.ON_PRIMARY)
        .backgroundColor(AppColors.PRIMARY)
        .width(200)
        .height(46)
        .margin({ top: 10 })
        .onClick(() => {
          this.pathStack.pushPathByName('serverEdit', new ServerEditParam(null));
        })
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
    .backgroundColor(AppColors.BACKGROUND)
  }

  @Builder
  NavTab(label: string, index: number, kind: UiIconKind) {
    Column({ space: 2 }) {
      Row() {
        UiIcon({
          kind: kind,
          iconColor: this.currentTab === index ? AppColors.TEXT : AppColors.TEXT_SECONDARY,
          iconSize: 21
        })
      }
      .width(56)
      .height(30)
      .justifyContent(FlexAlign.Center)
      .borderRadius(15)
      .backgroundColor(this.currentTab === index ? withAlpha(AppColors.TEXT, '1E') : Color.Transparent)

      Text(label)
        .fontSize(10)
        .fontColor(this.currentTab === index ? AppColors.TEXT : AppColors.TEXT_SECONDARY)
        .maxLines(1)
        .width('100%')
        .textAlign(TextAlign.Center)
    }
    .width('100%')
    .height(62)
    .justifyContent(FlexAlign.Center)
  }

  @Builder
  PhoneFrame() {
    Row() {
      Column() {
        this.MainTabs()
      }
      .width('100%')
      .height('100%')
      .constraintSize({ maxWidth: 480 })
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
    .backgroundColor(AppColors.BACKGROUND)
  }

  @Builder
  MainTabs() {
    Column() {
      this.ConnectionBanner()
      Tabs({ barPosition: BarPosition.End, index: this.currentTab }) {
        TabContent() {
          NowPlayingPage({ pathStack: this.pathStack, wide: false })
        }
        .tabBar(this.NavTab(this.s.navNow, 0, UiIconKind.NOW))

        TabContent() {
          LibraryPage({ pathStack: this.pathStack, columns: this.libraryColumns })
        }
        .tabBar(this.NavTab(this.s.navLibrary, 1, UiIconKind.LIBRARY))

        TabContent() {
          RadioPage({ pathStack: this.pathStack })
        }
        .tabBar(this.NavTab(this.s.radios, 2, UiIconKind.RADIO))

        TabContent() {
          PlayersPage()
        }
        .tabBar(this.NavTab(this.s.playerLabel, 3, UiIconKind.PLAYERS))

        TabContent() {
          SettingsPage({ pathStack: this.pathStack, effectiveLayout: this.layout })
        }
        .tabBar(this.NavTab(this.s.settingsTitle, 4, UiIconKind.SETTINGS))
      }
      .barMode(BarMode.Fixed)
      .barBackgroundColor(AppColors.BACKGROUND)
      .barHeight(66)
      .onChange((index: number) => {
        this.currentTab = index;
      })
      .layoutWeight(1)
    }
    .width('100%')
    .height('100%')
    .backgroundColor(AppColors.BACKGROUND)
  }

  @Builder
  SideBrand() {
    if (this.layout === 'foldable') {
      Row() {
        Row() {
          Text('♪')
            .fontSize(20)
            .fontColor(AppColors.ON_PRIMARY)
        }
        .width(40)
        .height(40)
        .borderRadius(20)
        .backgroundColor(AppColors.PRIMARY)
        .justifyContent(FlexAlign.Center)
      }
      .width('100%')
      .height(72)
      .justifyContent(FlexAlign.Center)
    } else {
      Row({ space: 10 }) {
        Row() {
          Text('♪')
            .fontSize(19)
            .fontColor(AppColors.ON_PRIMARY)
        }
        .width(38)
        .height(38)
        .borderRadius(19)
        .backgroundColor(AppColors.PRIMARY)
        .justifyContent(FlexAlign.Center)

        Column({ space: 1 }) {
          Text('Daphile')
            .fontSize(16)
            .fontWeight(FontWeight.Bold)
            .fontColor(AppColors.TEXT)
          Text('HarmonyOS')
            .fontSize(9)
            .fontColor(AppColors.TEXT_SECONDARY)
        }
        .alignItems(HorizontalAlign.Start)
        .layoutWeight(1)
      }
      .width('100%')
      .height(72)
      .padding({ left: 14, right: 12 })
    }
  }

  @Builder
  SideItem(label: string, index: number, kind: UiIconKind) {
    if (this.layout === 'foldable') {
      Column({ space: 3 }) {
        Row() {
          UiIcon({
            kind: kind,
            iconColor: this.currentTab === index ? AppColors.TEXT : AppColors.TEXT_SECONDARY,
            iconSize: 21
          })
        }
        .width(48)
        .height(30)
        .justifyContent(FlexAlign.Center)
        .borderRadius(15)
        .backgroundColor(this.currentTab === index ? withAlpha(AppColors.TEXT, '1E') : Color.Transparent)

        Text(label)
          .fontSize(9)
          .fontColor(this.currentTab === index ? AppColors.TEXT : AppColors.TEXT_SECONDARY)
          .maxLines(1)
          .width('100%')
          .textAlign(TextAlign.Center)
      }
      .width('100%')
      .height(58)
      .justifyContent(FlexAlign.Center)
      .onClick(() => {
        this.currentTab = index;
      })
    } else {
      Row({ space: 12 }) {
        Row() {
          UiIcon({
            kind: kind,
            iconColor: this.currentTab === index ? AppColors.PRIMARY : AppColors.TEXT_SECONDARY,
            iconSize: 20
          })
        }
        .width(34)
        .height(34)
        .justifyContent(FlexAlign.Center)
        .borderRadius(10)
        .backgroundColor(this.currentTab === index ? withAlpha(AppColors.PRIMARY, '26') : Color.Transparent)

        Text(label)
          .fontSize(14)
          .fontWeight(this.currentTab === index ? FontWeight.Medium : FontWeight.Normal)
          .fontColor(this.currentTab === index ? AppColors.TEXT : AppColors.TEXT_SECONDARY)
          .layoutWeight(1)
          .maxLines(1)
      }
      .width('100%')
      .height(46)
      .padding({ left: 8, right: 8 })
      .borderRadius(12)
      .backgroundColor(this.currentTab === index ? withAlpha(AppColors.SURFACE_HIGH, 'B0') : Color.Transparent)
      .onClick(() => {
        this.currentTab = index;
      })
    }
  }

  @Builder
  Sidebar() {
    Column() {
      this.SideBrand()

      Column({ space: 4 }) {
        this.SideItem(this.s.navNow, 0, UiIconKind.NOW)
        this.SideItem(this.s.navLibrary, 1, UiIconKind.LIBRARY)
        this.SideItem(this.s.radios, 2, UiIconKind.RADIO)
        this.SideItem(this.s.playerLabel, 3, UiIconKind.PLAYERS)
        this.SideItem(this.s.settingsTitle, 4, UiIconKind.SETTINGS)
      }
      .width('100%')
      .padding({ left: this.layout === 'foldable' ? 8 : 10, right: this.layout === 'foldable' ? 8 : 10 })
      .margin({ top: 6 })

      Blank()

      if (this.layout !== 'foldable') {
        Column({ space: 4 }) {
          Text(this.repo.state.server !== null ? this.repo.state.server.name : this.s.notConnected)
            .fontSize(12)
            .fontColor(AppColors.TEXT)
            .maxLines(1)
            .textOverflow({ overflow: TextOverflow.Ellipsis })
            .width('100%')
          Row({ space: 6 }) {
            Column()
              .width(6)
              .height(6)
              .borderRadius(3)
              .backgroundColor(this.repo.state.connection === ConnectionState.CONNECTED
                ? AppColors.PRIMARY : AppColors.WARN)
            Text(this.repo.state.connection === ConnectionState.CONNECTED
              ? this.s.online : this.connectionLabel())
              .fontSize(10)
              .fontColor(AppColors.TEXT_SECONDARY)
              .maxLines(1)
          }
          .width('100%')
        }
        .alignItems(HorizontalAlign.Start)
        .width('100%')
        .padding(12)
        .backgroundColor(withAlpha(AppColors.SURFACE, '99'))
        .borderRadius(12)
        .margin({ left: 10, right: 10, bottom: 14 })
      }
    }
    .width(this.sidebarWidth)
    .height('100%')
    .backgroundColor(withAlpha(AppColors.SURFACE, 'B3'))
    .border({ width: { right: 0.5 }, color: { right: AppColors.OUTLINE } })
  }

  @Builder
  WideNowPlaying() {
    NowPlayingPage({ pathStack: this.pathStack, wide: true })
  }

  @Builder
  WideLibrary() {
    Row() {
      Column() {
        LibraryPage({ pathStack: this.pathStack, columns: this.libraryColumns })
      }
      .width('100%')
      .height('100%')
      .constraintSize({ maxWidth: this.wideFrame ? 1280 : 10000 })
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
  }

  @Builder
  WideRadio() {
    Row() {
      Column() {
        RadioPage({ pathStack: this.pathStack })
      }
      .width('100%')
      .height('100%')
      .constraintSize({ maxWidth: this.wideFrame ? 900 : 10000 })
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
  }

  @Builder
  WidePlayers() {
    Row() {
      Column() {
        PlayersPage()
      }
      .width('100%')
      .height('100%')
      .constraintSize({ maxWidth: this.wideFrame ? 900 : 10000 })
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
  }

  @Builder
  WideSettings() {
    Row() {
      Column() {
        SettingsPage({ pathStack: this.pathStack, effectiveLayout: this.layout })
      }
      .width('100%')
      .height('100%')
      .constraintSize({ maxWidth: this.wideFrame ? 1000 : 10000 })
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
  }

  @Builder
  WideShell() {
    Row() {
      this.Sidebar()

      Column() {
        this.ConnectionBanner()
        Tabs({ barPosition: BarPosition.Start, index: this.currentTab }) {
          TabContent() {
            this.WideNowPlaying()
          }

          TabContent() {
            this.WideLibrary()
          }

          TabContent() {
            this.WideRadio()
          }

          TabContent() {
            this.WidePlayers()
          }

          TabContent() {
            this.WideSettings()
          }
        }
        .barHeight(0)
        .scrollable(false)
        .animationDuration(0)
        .onChange((index: number) => {
          this.currentTab = index;
        })
        .layoutWeight(1)
      }
      .layoutWeight(1)
      .height('100%')
      .backgroundColor(AppColors.BACKGROUND)
    }
    .width('100%')
    .height('100%')
    .backgroundColor(AppColors.BACKGROUND)
  }

  build() {
    Navigation(this.pathStack) {
      ForEach([`${this.repo.state.settings.theme}|${this.repo.state.settings.language}|${this.repo.state.settings.layoutMode}`],
        (uiKey: string) => {
          if (this.repo.state.servers.length === 0) {
            this.SetupView()
          } else if (this.layout === 'phone') {
            this.PhoneFrame()
          } else {
            this.WideShell()
          }
        }, (uiKey: string) => uiKey)
    }
    .hideTitleBar(true)
    .mode(NavigationMode.Stack)
    .navDestination(this.PageMap)
    .width('100%')
    .height('100%')
    .backgroundColor(AppColors.BACKGROUND)
  }
}
