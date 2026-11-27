import { promptAction } from '@kit.ArkUI';
import { pasteboard } from '@kit.BasicServicesKit';
import { AppContext } from '../../app/AppContext';
import {
  LyricLine, Lyrics, LyricsSource, Track, friendlyErrorMessage, lyricsSourceFromKey
} from '../../data/model/Models';
import { DaphileRepository } from '../../data/repo/DaphileRepository';
import { TrackDisplay } from '../../data/model/TrackDisplay';
import { AppColors, withAlpha } from '../theme/Theme';
import { EmptyState } from '../components/Common';
import { UiIcon, UiIconKind } from '../components/UiIcon';
import { stringsFor, UiStrings } from '../i18n/I18n';

function toast(message: string): void {
  promptAction.showToast({ message: message, duration: 2200 });
}

const SOURCE_CYCLE: LyricsSource[] = [
  LyricsSource.AUTO, LyricsSource.NETEASE, LyricsSource.QQ, LyricsSource.LRCLIB
];

@ComponentV2
export struct LyricsPage {
  @Param pathStack: NavPathStack = new NavPathStack();
  private repo: DaphileRepository = AppContext.get().repository;

  @Local lyrics: Lyrics | null = null;
  @Local loading: boolean = false;
  @Local message: string = '';
  @Local trackId: string = '';

  aboutToAppear(): void {
    this.load(false);
  }

  @Computed
  get s(): UiStrings {
    const settings = this.repo.state.settings;
    return stringsFor(settings.theme.length > 0 ? settings.language : 'en');
  }

  private currentTrack(): Track | null {
    const state = this.repo.state.nowPlaying;
    return state === null ? null : state.track;
  }

  private load(force: boolean): void {
    const track = this.currentTrack();
    if (track === null) {
      this.message = this.s.noCurrentTrack;
      return;
    }
    this.trackId = track.id;
    this.loading = true;
    this.message = '';
    const source: LyricsSource = lyricsSourceFromKey(this.repo.state.settings.lyricsSource);
    this.repo.lyricsForTrack(track, source, force).then((lyrics) => {
      this.loading = false;
      this.lyrics = lyrics;
      if (lyrics === null) {
        this.message = this.s.noLyricsHint;
      }
    }).catch((error: Error) => {
      this.loading = false;
      this.message = friendlyErrorMessage(error);
    });
  }

  private currentLineIndex(): number {
    const lyrics = this.lyrics;
    const state = this.repo.state.nowPlaying;
    if (lyrics === null || state === null || !lyrics.synced) {
      return -1;
    }
    const timeMs = state.time * 1000;
    let index = -1;
    for (let i = 0; i < lyrics.lines.length; i++) {
      if (lyrics.lines[i].timeMs <= timeMs) {
        index = i;
      } else {
        break;
      }
    }
    return index;
  }

  private save(): void {
    const track = this.currentTrack();
    const lyrics = this.lyrics;
    if (track === null || lyrics === null) {
      return;
    }
    this.repo.saveLyrics(track, lyrics);
    toast(this.s.lyricsSaved);
  }

  private copy(): void {
    const lyrics = this.lyrics;
    if (lyrics === null || lyrics.lines.length === 0) {
      return;
    }
    const text = lyrics.lines.map((line: LyricLine) => line.text).join('\n');
    try {
      const data = pasteboard.createData(pasteboard.MIMETYPE_TEXT_PLAIN, text);
      pasteboard.getSystemPasteboard().setData(data).then(() => {
        toast(this.s.copyLyrics);
      }).catch(() => {
      });
    } catch (error) {
    }
  }

  private sourceLabel(): string {
    switch (lyricsSourceFromKey(this.repo.state.settings.lyricsSource)) {
      case LyricsSource.NETEASE: return this.s.sourceNetease;
      case LyricsSource.QQ: return this.s.sourceQq;
      case LyricsSource.LRCLIB: return this.s.sourceLrclib;
      default: return this.s.sourceAuto;
    }
  }

  private cycleSource(): void {
    const current = lyricsSourceFromKey(this.repo.state.settings.lyricsSource);
    let index = 0;
    for (let i = 0; i < SOURCE_CYCLE.length; i++) {
      if (SOURCE_CYCLE[i] === current) {
        index = i;
        break;
      }
    }
    const next = SOURCE_CYCLE[(index + 1) % SOURCE_CYCLE.length];
    this.repo.setLyricsSource(next).then(() => this.load(true)).catch((error: Error) => {
      toast(friendlyErrorMessage(error));
    });
  }

  private pathText(): string {
    const track = this.currentTrack();
    if (track === null) {
      return '';
    }
    return this.repo.savedLyricsPath(track);
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

      Text(this.s.lyrics)
        .fontSize(24)
        .fontWeight(FontWeight.Bold)
        .fontColor(AppColors.TEXT)
        .layoutWeight(1)

      Row() {
        UiIcon({ kind: UiIconKind.COPY, iconColor: AppColors.TEXT, iconSize: 19 })
      }
      .width(38)
      .height(38)
      .justifyContent(FlexAlign.Center)
      .onClick(() => this.copy())

      Row() {
        UiIcon({ kind: UiIconKind.DOWNLOAD, iconColor: AppColors.TEXT, iconSize: 19 })
      }
      .width(38)
      .height(38)
      .justifyContent(FlexAlign.Center)
      .onClick(() => this.save())

      Row() {
        UiIcon({ kind: UiIconKind.REFRESH, iconColor: AppColors.TEXT, iconSize: 19 })
      }
      .width(38)
      .height(38)
      .justifyContent(FlexAlign.Center)
      .onClick(() => this.load(true))
    }
    .width('100%')
    .padding({ left: 8, right: 8, top: 8, bottom: 2 })
  }

  @Builder
  TrackInfo() {
    Column({ space: 4 }) {
      Row({ space: 8 }) {
        Text(this.currentTrack() !== null
          ? TrackDisplay.title(this.currentTrack() as Track) : '')
          .fontSize(13)
          .fontWeight(FontWeight.Medium)
          .fontColor(AppColors.TEXT)
          .maxLines(1)
          .textOverflow({ overflow: TextOverflow.Ellipsis })
        Text(this.currentTrack() !== null
          ? TrackDisplay.artist(this.currentTrack() as Track) : '')
          .fontSize(11)
          .fontColor(AppColors.TEXT_SECONDARY)
          .maxLines(1)
          .textOverflow({ overflow: TextOverflow.Ellipsis })
          .layoutWeight(1)
      }
      .width('100%')

      Row({ space: 8 }) {
        Text(this.pathText())
          .fontSize(9)
          .fontColor(withAlpha(AppColors.TEXT_SECONDARY, 'CC'))
          .maxLines(1)
          .textOverflow({ overflow: TextOverflow.Ellipsis })
          .layoutWeight(1)
        if (this.lyrics !== null) {
          Text(this.lyrics.source.length > 0 ? this.lyrics.source : this.s.saved)
            .fontSize(9)
            .fontColor(AppColors.PRIMARY)
            .maxLines(1)
        }
      }
      .width('100%')

      Row({ space: 6 }) {
        UiIcon({ kind: UiIconKind.GLOBE, iconColor: AppColors.PRIMARY, iconSize: 15 })
        Text(`${this.s.lyricsSourceLabel}: ${this.sourceLabel()}`)
          .fontSize(11)
          .fontColor(AppColors.PRIMARY)
          .maxLines(1)
        UiIcon({ kind: UiIconKind.CHEVRON_DOWN, iconColor: AppColors.PRIMARY, iconSize: 12 })
      }
      .padding({ left: 12, right: 10, top: 6, bottom: 6 })
      .backgroundColor(withAlpha(AppColors.PRIMARY, '18'))
      .borderRadius(14)
      .margin({ top: 6 })
      .onClick(() => this.cycleSource())
    }
    .width('100%')
    .alignItems(HorizontalAlign.Start)
    .padding({ left: 16, right: 16, top: 4, bottom: 6 })
  }

  @Builder
  LineRow(line: LyricLine, index: number, activeIndex: number) {
    Text(line.text)
      .fontSize(activeIndex === index ? 22 : 15)
      .fontWeight(activeIndex === index ? FontWeight.Medium : FontWeight.Normal)
      .fontColor(activeIndex === index
        ? AppColors.PRIMARY
        : withAlpha(AppColors.TEXT_SECONDARY, activeIndex >= 0 && index < activeIndex ? '88' : 'FF'))
      .width('100%')
      .textAlign(TextAlign.Center)
      .padding({ top: 7, bottom: 7 })
      .onClick(() => {
        if (line.timeMs > 0) {
          this.repo.seekTo(line.timeMs / 1000).catch((error: Error) => {
            toast(friendlyErrorMessage(error));
          });
        }
      })
  }

  build() {
    Row() {
      Column() {
        this.Header()

        if (this.currentTrack() !== null) {
          this.TrackInfo()
        }

        if (this.loading) {
          Column() {
            LoadingProgress()
              .width(26)
              .height(26)
              .color(AppColors.PRIMARY)
            Text(this.s.loading)
              .fontSize(12)
              .fontColor(AppColors.TEXT_SECONDARY)
              .margin({ top: 8 })
          }
          .width('100%')
          .layoutWeight(1)
          .justifyContent(FlexAlign.Center)
        } else if (this.lyrics !== null) {
          List({ space: 0 }) {
            ForEach(this.lyrics.lines, (line: LyricLine, index: number) => {
              ListItem() {
                this.LineRow(line, index, this.currentLineIndex())
              }
            }, (line: LyricLine, index: number) => `${index}-${line.timeMs}-${line.text}`)
          }
          .width('100%')
          .layoutWeight(1)
          .padding({ left: 24, right: 24, bottom: 8 })
          .scrollBar(BarState.Off)

          Text(`Source: ${this.lyrics.source} · ${this.lyrics.synced ? this.s.syncedLabel : ''}`)
            .fontSize(9)
            .fontColor(AppColors.TEXT_SECONDARY)
            .width('100%')
            .textAlign(TextAlign.Center)
            .padding({ bottom: 12 })
        } else {
          EmptyState({
            title: this.message.length > 0 ? this.message : this.s.noLyricsHint,
            body: this.s.matchingLyrics
          })
        }
      }
      .width('100%')
      .height('100%')
      .constraintSize({ maxWidth: 820 })
      .backgroundColor(AppColors.BACKGROUND)
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
    .backgroundColor(AppColors.BACKGROUND)
  }
}
