import { promptAction } from '@kit.ArkUI';
import { AppContext } from '../../app/AppContext';
import { BrowserItem, PluginEntry, friendlyErrorMessage } from '../../data/model/Models';
import { DaphileRepository } from '../../data/repo/DaphileRepository';
import { AppColors, withAlpha } from '../theme/Theme';
import { UiIcon, UiIconKind } from '../components/UiIcon';
import { TransportIcon, TransportKind } from '../components/TransportIcon';
import { PluginBrowserParam } from './BrowsePages';
import { stringsFor, UiStrings } from '../i18n/I18n';

function toast(message: string): void {
  promptAction.showToast({ message: message, duration: 2200 });
}

@ComponentV2
export struct RadioPage {
  @Param pathStack: NavPathStack = new NavPathStack();
  private repo: DaphileRepository = AppContext.get().repository;

  @Local tab: number = 0;
  @Local favorites: BrowserItem[] = [];
  @Local radios: PluginEntry[] = [];
  @Local apps: PluginEntry[] = [];
  @Local loading: boolean = false;
  @Local message: string = '';

  aboutToAppear(): void {
    this.reload();
  }

  @Computed
  get s(): UiStrings {
    const settings = this.repo.state.settings;
    return stringsFor(settings.theme.length > 0 ? settings.language : 'en');
  }

  private run(action: () => Promise<void>): void {
    action().catch((error: Error) => toast(friendlyErrorMessage(error)));
  }

  private reload(): void {
    if (this.repo.state.server === null) {
      this.message = this.s.notConnected;
      return;
    }
    this.loading = true;
    this.message = '';
    this.repo.favorites().then((items) => {
      this.favorites = items;
      this.loading = false;
    }).catch((error: Error) => {
      this.loading = false;
      this.message = friendlyErrorMessage(error);
    });
    this.repo.radios().then((items) => {
      this.radios = items;
    }).catch(() => {
    });
    this.repo.apps().then((items) => {
      this.apps = items;
    }).catch(() => {
    });
  }

  @Builder
  TabItem(title: string, index: number) {
    Column({ space: 0 }) {
      Text(title)
        .fontSize(13)
        .fontColor(this.tab === index ? AppColors.PRIMARY : AppColors.TEXT_SECONDARY)
        .maxLines(2)
        .textAlign(TextAlign.Center)
        .width('100%')
        .padding({ top: 10, bottom: 10 })
      Row()
        .width('100%')
        .height(3)
        .borderRadius(2)
        .backgroundColor(this.tab === index ? AppColors.PRIMARY : Color.Transparent)
    }
    .layoutWeight(1)
    .onClick(() => {
      this.tab = index;
    })
  }

  @Builder
  Tabs() {
    Column({ space: 0 }) {
      Row({ space: 0 }) {
        this.TabItem(this.s.favorites, 0)
        this.TabItem(this.s.radios, 1)
        this.TabItem(this.s.streamingServices, 2)
      }
      .width('100%')
      Row()
        .width('100%')
        .height(0.5)
        .backgroundColor(withAlpha(AppColors.TEXT_SECONDARY, '33'))
    }
    .width('100%')
    .padding({ left: 12, right: 12, top: 2 })
  }

  @Builder
  ActionPills() {
    Row({ space: 10 }) {
      Row({ space: 6 }) {
        TransportIcon({ kind: TransportKind.PLAY, iconColor: AppColors.PRIMARY, iconSize: 13 })
        Text(this.s.playAll)
          .fontSize(13)
          .fontColor(AppColors.PRIMARY)
      }
      .height(42)
      .layoutWeight(1)
      .justifyContent(FlexAlign.Center)
      .backgroundColor(withAlpha(AppColors.PRIMARY, '20'))
      .borderRadius(21)
      .border({ width: 1, color: withAlpha(AppColors.PRIMARY, '66') })
      .onClick(() => this.run(() => this.repo.favoritesToQueue(true)))

      Row({ space: 6 }) {
        UiIcon({ kind: UiIconKind.QUEUE, iconColor: AppColors.PRIMARY, iconSize: 17 })
        Text(this.s.addAllToQueue)
          .fontSize(13)
          .fontColor(AppColors.PRIMARY)
          .maxLines(1)
      }
      .height(42)
      .layoutWeight(1)
      .justifyContent(FlexAlign.Center)
      .backgroundColor(withAlpha(AppColors.PRIMARY, '14'))
      .borderRadius(21)
      .border({ width: 1, color: withAlpha(AppColors.PRIMARY, '66') })
      .onClick(() => this.run(() => this.repo.favoritesToQueue(false)))
    }
    .width('100%')
    .padding({ left: 16, right: 16, top: 12, bottom: 8 })
  }

  @Builder
  FavoriteRow(item: BrowserItem) {
    Row({ space: 12 }) {
      Column() {
        UiIcon({ kind: UiIconKind.HEART, iconColor: AppColors.PRIMARY, iconSize: 19 })
      }
      .width(44)
      .height(44)
      .justifyContent(FlexAlign.Center)
      .backgroundColor(withAlpha(AppColors.PRIMARY, '1C'))
      .borderRadius(12)

      Column({ space: 2 }) {
        Text(item.name)
          .fontSize(15)
          .fontColor(AppColors.TEXT)
          .maxLines(1)
          .textOverflow({ overflow: TextOverflow.Ellipsis })
        if (item.description.length > 0) {
          Text(item.description)
            .fontSize(11)
            .fontColor(AppColors.TEXT_SECONDARY)
            .maxLines(1)
            .textOverflow({ overflow: TextOverflow.Ellipsis })
        }
      }
      .alignItems(HorizontalAlign.Start)
      .layoutWeight(1)

      Row({ space: 14 }) {
        if (item.hasItems) {
          UiIcon({ kind: UiIconKind.CHEVRON_RIGHT, iconColor: AppColors.TEXT_SECONDARY, iconSize: 18 })
        } else {
          UiIcon({ kind: UiIconKind.QUEUE, iconColor: AppColors.TEXT_SECONDARY, iconSize: 18 })
            .onClick(() => this.run(() => this.repo.queueBrowserItem('', item)))
          TransportIcon({ kind: TransportKind.PLAY, iconColor: AppColors.TEXT, iconSize: 18 })
            .onClick(() => this.run(() => this.repo.playFavorite(item.id)))
        }
      }
    }
    .width('100%')
    .height(58)
    .onClick(() => {
      if (item.hasItems) {
        this.pathStack.pushPathByName('pluginBrowser',
          new PluginBrowserParam('favorites', item.name, 'favorites'));
      } else {
        this.run(() => this.repo.playFavorite(item.id));
      }
    })
  }

  @Builder
  EmptyPanel(icon: UiIconKind, title: string, body: string) {
    Column({ space: 0 }) {
      Column() {
        UiIcon({ kind: icon, iconColor: AppColors.PRIMARY, iconSize: 32 })
      }
      .width(76)
      .height(76)
      .justifyContent(FlexAlign.Center)
      .backgroundColor(withAlpha(AppColors.SURFACE, 'CC'))
      .borderRadius(20)

      Text(title)
        .fontSize(15)
        .fontWeight(FontWeight.Medium)
        .fontColor(AppColors.TEXT)
        .margin({ top: 18 })
      Text(body)
        .fontSize(12)
        .fontColor(AppColors.TEXT_SECONDARY)
        .textAlign(TextAlign.Center)
        .maxLines(3)
        .padding({ left: 40, right: 40 })
        .margin({ top: 6 })
    }
    .width('100%')
    .padding({ top: 56 })
    .alignItems(HorizontalAlign.Center)
  }

  @Builder
  PluginRows(items: PluginEntry[], kind: UiIconKind) {
    List({ space: 2 }) {
      ForEach(items, (item: PluginEntry) => {
        ListItem() {
          Row({ space: 12 }) {
            Column() {
              UiIcon({ kind: kind, iconColor: AppColors.PRIMARY, iconSize: 20 })
            }
            .width(44)
            .height(44)
            .justifyContent(FlexAlign.Center)
            .backgroundColor(withAlpha(AppColors.PRIMARY, '1C'))
            .borderRadius(12)

            Text(item.name)
              .fontSize(15)
              .fontColor(AppColors.TEXT)
              .maxLines(1)
              .textOverflow({ overflow: TextOverflow.Ellipsis })
              .layoutWeight(1)

            UiIcon({ kind: UiIconKind.CHEVRON_RIGHT, iconColor: AppColors.TEXT_SECONDARY, iconSize: 18 })
          }
          .width('100%')
          .height(60)
          .onClick(() => {
            this.pathStack.pushPathByName('pluginBrowser',
              new PluginBrowserParam(item.cmd, item.name, 'plugin'));
          })
        }
      }, (item: PluginEntry) => `${kind}-${item.cmd}`)
    }
    .width('100%')
    .layoutWeight(1)
    .padding({ left: 16, right: 16, top: 4 })
    .scrollBar(BarState.Off)
  }

  build() {
    Column() {
      Row() {
        Text(this.s.radios)
          .fontSize(28)
          .fontWeight(FontWeight.Bold)
          .fontColor(AppColors.TEXT)
          .layoutWeight(1)
      }
      .width('100%')
      .padding({ left: 16, right: 16, top: 8, bottom: 6 })

      this.Tabs()

      if (this.message.length > 0) {
        Text(this.message)
          .fontSize(11)
          .fontColor(AppColors.WARN)
          .width('100%')
          .padding({ left: 16, right: 16, top: 6 })
      }

      if (this.tab === 0) {
        if (this.favorites.length > 0) {
          this.ActionPills()
          List({ space: 2 }) {
            ForEach(this.favorites, (item: BrowserItem) => {
              ListItem() {
                this.FavoriteRow(item)
              }
            }, (item: BrowserItem) => `favorite-${item.id}`)
          }
          .width('100%')
          .layoutWeight(1)
          .padding({ left: 16, right: 16 })
          .scrollBar(BarState.Off)
        } else if (this.loading) {
          Row() {
            LoadingProgress()
              .width(22)
              .height(22)
              .color(AppColors.PRIMARY)
          }
          .width('100%')
          .padding(32)
        } else {
          this.EmptyPanel(UiIconKind.HEART, this.s.favoritesEmptyTitle, this.s.favoritesEmptyBody)
        }
      } else if (this.tab === 1) {
        if (this.radios.length > 0) {
          this.PluginRows(this.radios, UiIconKind.RADIO)
        } else {
          this.EmptyPanel(UiIconKind.RADIO, this.s.noRadiosTitle, this.s.noRadiosBody)
        }
      } else {
        if (this.apps.length > 0) {
          this.PluginRows(this.apps, UiIconKind.SPARKLE)
        } else {
          this.EmptyPanel(UiIconKind.SPARKLE, this.s.noStreamingTitle, this.s.noStreamingBody)
        }
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor(AppColors.BACKGROUND)
  }
}
