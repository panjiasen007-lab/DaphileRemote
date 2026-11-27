import { promptAction } from '@kit.ArkUI';
import { AppContext } from '../../app/AppContext';
import { BrowserItem, friendlyErrorMessage } from '../../data/model/Models';
import { DaphileRepository } from '../../data/repo/DaphileRepository';
import { AppColors, withAlpha } from '../theme/Theme';
import { CoverArt, EmptyState } from '../components/Common';
import { UiIcon, UiIconKind } from '../components/UiIcon';
import { TransportIcon, TransportKind } from '../components/TransportIcon';
import { stringsFor, UiStrings } from '../i18n/I18n';

function toast(message: string): void {
  promptAction.showToast({ message: message, duration: 2200 });
}

export class FolderParam {
  id: string;
  name: string;

  constructor(id: string, name: string) {
    this.id = id;
    this.name = name;
  }
}

@ComponentV2
export struct FolderPage {
  @Param pathStack: NavPathStack = new NavPathStack();
  @Param folder: FolderParam = new FolderParam('', '');
  private repo: DaphileRepository = AppContext.get().repository;

  @Local items: BrowserItem[] = [];
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
    this.repo.musicFolders(this.folder.id, this.items.length, 200).then((page) => {
      this.items = this.items.concat(page.items);
      this.hasMore = page.hasMore;
      this.loading = false;
      if (this.items.length === 0) {
        this.message = this.s.noSongsBody;
      }
    }).catch((error: Error) => {
      this.loading = false;
      this.hasMore = false;
      this.message = friendlyErrorMessage(error);
    });
  }

  private reload(): void {
    this.items = [];
    this.hasMore = true;
    this.message = '';
    this.loadMore();
  }

  private titleText(): string {
    return this.folder.name.length > 0 ? this.folder.name : this.s.musicFolders;
  }

  private openItem(item: BrowserItem): void {
    if (item.hasItems || item.type === 'folder' || item.type === 'dir') {
      this.pathStack.pushPathByName('folder', new FolderParam(item.id, item.name));
    } else {
      this.run(() => this.repo.playFolderItem(item));
    }
  }

  private cover(item: BrowserItem): string {
    const server = this.repo.state.server;
    if (server === null || item.coverId.length === 0) {
      return '';
    }
    return `${server.useHttps ? 'https' : 'http'}://${server.host}:${server.port}` +
      `/music/${encodeURIComponent(item.coverId)}/cover.jpg`;
  }

  @Builder
  FolderRow(item: BrowserItem) {
    Row({ space: 12 }) {
      if (item.hasItems || item.type === 'folder' || item.type === 'dir') {
        Column() {
          UiIcon({ kind: UiIconKind.FOLDER, iconColor: AppColors.PRIMARY, iconSize: 24 })
        }
        .width(46)
        .height(46)
        .borderRadius(12)
        .backgroundColor(withAlpha(AppColors.PRIMARY, '1E'))
        .justifyContent(FlexAlign.Center)
      } else {
        CoverArt({ url: this.cover(item), coverSize: 46, radius: 8 })
      }
      Column() {
        Text(item.name)
          .fontSize(16)
          .fontWeight(FontWeight.Medium)
          .fontColor(AppColors.TEXT)
          .maxLines(1)
          .textOverflow({ overflow: TextOverflow.Ellipsis })
        Text(item.hasItems ? this.s.folders :
          (item.description.length > 0 ? item.description :
            (item.durationText.length > 0 ? item.durationText : item.filename)))
          .fontSize(12)
          .fontColor(AppColors.TEXT_SECONDARY)
          .maxLines(1)
          .textOverflow({ overflow: TextOverflow.Ellipsis })
          .margin({ top: 2 })
      }
      .alignItems(HorizontalAlign.Start)
      .layoutWeight(1)
      if (item.hasItems || item.type === 'folder' || item.type === 'dir') {
        Row() {
          TransportIcon({ kind: TransportKind.PLAY, iconColor: AppColors.TEXT, iconSize: 15 })
        }
        .width(34)
        .height(34)
        .justifyContent(FlexAlign.Center)
        .borderRadius(17)
        .backgroundColor(withAlpha(AppColors.TEXT, '0D'))
        .onClick(() => this.run(() => this.repo.playFolder(item.id)))
      }
      Text('›')
        .fontSize(18)
        .fontColor(AppColors.TEXT_SECONDARY)
    }
    .width('100%')
    .padding({ top: 9, bottom: 9, left: 4, right: 4 })
    .onClick(() => this.openItem(item))
    .gesture(
      LongPressGesture({ repeat: false })
        .onAction(() => {
          if (!item.hasItems) {
            this.run(() => this.repo.queueFolderItem(item));
          }
        })
    )
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
        Text(this.titleText())
          .fontSize(23)
          .fontWeight(FontWeight.Bold)
          .fontColor(AppColors.TEXT)
          .layoutWeight(1)
          .maxLines(1)
          .textOverflow({ overflow: TextOverflow.Ellipsis })
        if (this.folder.id.length > 0) {
          Row() {
            TransportIcon({ kind: TransportKind.PLAY, iconColor: AppColors.TEXT, iconSize: 17 })
          }
          .width(38)
          .height(38)
          .justifyContent(FlexAlign.Center)
          .onClick(() => this.run(() => this.repo.playFolder(this.folder.id)))
        }
        Row() {
          UiIcon({ kind: UiIconKind.REFRESH, iconColor: AppColors.TEXT, iconSize: 19 })
        }
        .width(38)
        .height(38)
        .justifyContent(FlexAlign.Center)
        .onClick(() => this.reload())
      }
      .width('100%')
      .padding({ left: 8, right: 8, top: 8, bottom: 4 })

      List({ space: 2 }) {
        if (this.items.length === 0 && !this.loading) {
          ListItem() {
            EmptyState({ title: this.s.noSongsTitle, body: this.message })
          }
        }
        ForEach(this.items, (item: BrowserItem) => {
          ListItem() {
            this.FolderRow(item)
          }
        }, (item: BrowserItem) => `${item.id}-${item.name}`)

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
