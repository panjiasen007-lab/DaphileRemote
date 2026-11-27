import { Track } from '../../data/model/Models';
import { AppColors, formatTime, withAlpha } from '../theme/Theme';
import { TrackDisplay } from '../../data/model/TrackDisplay';
import { UiIcon, UiIconKind } from './UiIcon';

export interface TrackQuality {
  label: string;
  color: string;
}

const DSD_TYPES: string[] = ['dsd', 'dsf', 'dff'];
const LOSSLESS_TYPES: string[] = ['flac', 'flc', 'wav', 'wave', 'aif', 'aiff', 'ape', 'alac', 'wv', 'tta'];
const LOSSY_TYPES: string[] = ['mp3', 'aac', 'm4a', 'm4b', 'ogg', 'opus', 'wma', 'mpc'];

/** 由格式 / 采样率 / 位深 / 码率推导音质标签。 */
export function trackQuality(track: Track | null): TrackQuality | null {
  if (track === null) {
    return null;
  }
  let type = track.type.trim().toLowerCase();
  const atIndex = type.indexOf('@');
  if (atIndex >= 0) {
    type = type.substring(0, atIndex).trim();
  }
  const dotIndex = type.lastIndexOf('.');
  if (dotIndex >= 0) {
    type = type.substring(dotIndex + 1).trim();
  }
  const rateText = track.sampleRate.trim();
  const bitsText = track.sampleSize.trim();
  const rate = rateText.length > 0 ? Number.parseFloat(rateText) : 0;
  const bits = bitsText.length > 0 ? Number.parseFloat(bitsText) : 0;
  let bitrate = 0;
  const match = track.bitRate.match(/(\d+)\s*kbps/i);
  if (match !== null) {
    bitrate = Number.parseInt(match[1]);
  }

  if (DSD_TYPES.includes(type) || track.sampleRate.toLowerCase().includes('dsd') ||
    track.type.toLowerCase().includes('dsd')) {
    let label = 'DSD';
    if (rate >= 22579200) {
      label = 'DSD512';
    } else if (rate >= 11289600) {
      label = 'DSD256';
    } else if (rate >= 5644800) {
      label = 'DSD128';
    } else if (rate >= 2822400) {
      label = 'DSD64';
    }
    return { label: label, color: '#D4A017' };
  }
  if (rate >= 88200 || bits >= 24) {
    return { label: 'Hi-Res', color: '#D4A017' };
  }
  if (track.lossless || LOSSLESS_TYPES.includes(type)) {
    return { label: 'SQ', color: '#31A79B' };
  }
  if (bitrate >= 320) {
    return { label: 'HQ', color: '#4C8DF6' };
  }
  if (LOSSY_TYPES.includes(type) || bitrate > 0) {
    return { label: '标准', color: '#8C939C' };
  }
  return null;
}

function noop(): void {
}

@ComponentV2
export struct CoverArt {
  @Param url: string = '';
  @Param coverSize: number = 44;
  @Param radius: number = 8;

  build() {
    Stack() {
      if (this.url.length > 0) {
        Image(this.url)
          .width(this.coverSize)
          .height(this.coverSize)
          .objectFit(ImageFit.Cover)
          .borderRadius(this.radius)
          .interpolation(ImageInterpolation.Medium)
      } else {
        Column() {
          Text('♪')
            .fontSize(this.coverSize * 0.36)
            .fontColor(AppColors.TEXT_SECONDARY)
        }
        .width(this.coverSize)
        .height(this.coverSize)
        .justifyContent(FlexAlign.Center)
        .backgroundColor(AppColors.SURFACE_HIGH)
        .borderRadius(this.radius)
      }
    }
    .width(this.coverSize)
    .height(this.coverSize)
  }
}

@ComponentV2
export struct QualityBadge {
  @Require @Param quality: TrackQuality;

  build() {
    Text(this.quality.label)
      .fontSize(9)
      .fontColor(this.quality.color)
      .padding({ left: 5, right: 5, top: 1, bottom: 1 })
      .backgroundColor(withAlpha(this.quality.color, '22'))
      .borderRadius(4)
      .border({ width: 0.5, color: withAlpha(this.quality.color, '66') })
  }
}

@ComponentV2
export struct TrackRow {
  @Require @Param track: Track;
  @Param coverUrl: string = '';
  @Param isCurrent: boolean = false;
  @Event onTap: () => void = noop;
  @Event onLongPress: () => void = noop;

  private subtitle(): string {
    const parts: string[] = [];
    const artist = TrackDisplay.artist(this.track);
    const album = TrackDisplay.album(this.track);
    if (artist.length > 0) {
      parts.push(artist);
    }
    if (album.length > 0) {
      parts.push(album);
    }
    return parts.join(' · ');
  }

  private title(): string {
    const title = TrackDisplay.title(this.track);
    return title.length > 0 ? title : '未知曲目';
  }

  build() {
    Row({ space: 10 }) {
      CoverArt({ url: this.coverUrl, coverSize: 42, radius: 7 })
      Column() {
        Row({ space: 6 }) {
          Text(this.title())
            .fontSize(15)
            .fontColor(this.isCurrent ? AppColors.PRIMARY : AppColors.TEXT)
            .fontWeight(this.isCurrent ? FontWeight.Medium : FontWeight.Normal)
            .maxLines(1)
            .textOverflow({ overflow: TextOverflow.Ellipsis })
            .layoutWeight(1)
          if (trackQuality(this.track) !== null) {
            QualityBadge({ quality: trackQuality(this.track) as TrackQuality })
          }
        }
        .width('100%')

        if (this.subtitle().length > 0) {
          Text(this.subtitle())
            .fontSize(11)
            .fontColor(AppColors.TEXT_SECONDARY)
            .maxLines(1)
            .textOverflow({ overflow: TextOverflow.Ellipsis })
            .margin({ top: 2 })
        }
      }
      .alignItems(HorizontalAlign.Start)
      .layoutWeight(1)

      Text(formatTime(this.track.duration))
        .fontSize(11)
        .fontColor(AppColors.TEXT_SECONDARY)
    }
    .width('100%')
    .padding({ left: 10, right: 10, top: 8, bottom: 8 })
    .backgroundColor(this.isCurrent ? withAlpha(AppColors.PRIMARY, '14') : AppColors.SURFACE)
    .borderRadius(10)
    .onClick(() => this.onTap())
    .gesture(
      LongPressGesture({ repeat: false })
        .onAction(() => this.onLongPress())
    )
  }
}

@ComponentV2
export struct EmptyState {
  @Param title: string = '暂无内容';
  @Param body: string = '';

  build() {
    Column() {
      Column() {
        UiIcon({ kind: UiIconKind.QUEUE, iconColor: AppColors.PRIMARY, iconSize: 38 })
      }
      .width(88)
      .height(88)
      .borderRadius(24)
      .backgroundColor(withAlpha(AppColors.TEXT, '0F'))
      .justifyContent(FlexAlign.Center)

      Text(this.title)
        .fontSize(16)
        .fontWeight(FontWeight.Medium)
        .fontColor(AppColors.TEXT)
        .margin({ top: 16 })
      if (this.body.length > 0) {
        Text(this.body)
          .fontSize(13)
          .fontColor(AppColors.TEXT_SECONDARY)
          .textAlign(TextAlign.Center)
          .margin({ top: 6 })
          .padding({ left: 28, right: 28 })
      }
    }
    .width('100%')
    .padding({ top: 44, bottom: 48 })
    .justifyContent(FlexAlign.Center)
  }
}

@ComponentV2
export struct SectionHeader {
  @Require @Param title: string;
  @Param actionText: string = '';

  build() {
    Row() {
      Text(this.title)
        .fontSize(17)
        .fontWeight(FontWeight.Medium)
        .fontColor(AppColors.TEXT)
        .layoutWeight(1)
      if (this.actionText.length > 0) {
        Text(this.actionText)
          .fontSize(12)
          .fontColor(AppColors.PRIMARY)
      }
    }
    .width('100%')
    .padding({ top: 14, bottom: 8 })
  }
}
