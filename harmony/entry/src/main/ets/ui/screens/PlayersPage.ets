import { promptAction } from '@kit.ArkUI';
import { AppContext } from '../../app/AppContext';
import { Player, friendlyErrorMessage, playerIsPlaying } from '../../data/model/Models';
import { DaphileRepository } from '../../data/repo/DaphileRepository';
import { AppColors, withAlpha } from '../theme/Theme';
import { TransportIcon, TransportKind } from '../components/TransportIcon';
import { UiIcon, UiIconKind } from '../components/UiIcon';
import { stringsFor, UiStrings } from '../i18n/I18n';

function toast(message: string): void {
  promptAction.showToast({ message: message, duration: 2200 });
}

@ComponentV2
export struct PlayersPage {
  private repo: DaphileRepository = AppContext.get().repository;

  @Computed
  get s(): UiStrings {
    const settings = this.repo.state.settings;
    return stringsFor(settings.theme.length > 0 ? settings.language : 'en');
  }

  private run(action: () => Promise<void>): void {
    action().catch((error: Error) => toast(friendlyErrorMessage(error)));
  }

  private isCurrent(player: Player): boolean {
    return this.repo.state.currentPlayerId === player.id;
  }

  private stateLine(player: Player): string {
    const parts: string[] = [];
    parts.push(player.model.length > 0 ? player.model : 'Daphile');
    if (player.currentTitle.length > 0) {
      parts.push(player.currentTitle);
    }
    parts.push(player.connected ? this.s.online : this.s.offline);
    return parts.join(' · ');
  }

  @Builder
  PlayerCard(player: Player) {
    Column({ space: 6 }) {
      Row({ space: 8 }) {
        Text(player.name)
          .fontSize(18)
          .fontWeight(FontWeight.Bold)
          .fontColor(AppColors.TEXT)
          .maxLines(1)
          .textOverflow({ overflow: TextOverflow.Ellipsis })
        if (this.isCurrent(player)) {
          Text(this.s.currentBadge)
            .fontSize(10)
            .fontColor(AppColors.PRIMARY)
        }
        Blank()
        Row() {
          TransportIcon({
            kind: playerIsPlaying(player) ? TransportKind.PAUSE : TransportKind.PLAY,
            iconColor: AppColors.PRIMARY,
            iconSize: 18
          })
        }
        .width(32)
        .height(32)
        .justifyContent(FlexAlign.Center)
        .onClick(() => this.run(() => this.repo.togglePlayer(player.id)))

        Row() {
          UiIcon({ kind: UiIconKind.EXTERNAL, iconColor: AppColors.TEXT_SECONDARY, iconSize: 17 })
        }
        .width(32)
        .height(32)
        .justifyContent(FlexAlign.Center)
        .onClick(() => {
          this.repo.selectPlayer(player.id);
          toast(this.s.switchedTo.replace('%s', player.name));
        })

        Row() {
          UiIcon({
            kind: UiIconKind.POWER,
            iconColor: player.power ? AppColors.PRIMARY : AppColors.TEXT_SECONDARY,
            iconSize: 18
          })
        }
        .width(32)
        .height(32)
        .justifyContent(FlexAlign.Center)
        .onClick(() => this.run(() => this.repo.setPlayerPower(player.id, !player.power)))
      }
      .width('100%')

      Text(this.stateLine(player))
        .fontSize(11)
        .fontColor(AppColors.TEXT_SECONDARY)
        .maxLines(1)
        .textOverflow({ overflow: TextOverflow.Ellipsis })
        .width('100%')

      if (player.currentTitle.length > 0) {
        Column({ space: 1 }) {
          Text(player.currentTitle)
            .fontSize(14)
            .fontColor(AppColors.TEXT)
            .maxLines(1)
            .textOverflow({ overflow: TextOverflow.Ellipsis })
          if (player.currentArtist.length > 0) {
            Text(player.currentArtist)
              .fontSize(11)
              .fontColor(AppColors.TEXT_SECONDARY)
              .maxLines(1)
              .textOverflow({ overflow: TextOverflow.Ellipsis })
          }
        }
        .alignItems(HorizontalAlign.Start)
        .width('100%')
        .margin({ top: 4 })
      }

      if (player.canSetVolume) {
        Row({ space: 8 }) {
          TransportIcon({
            kind: player.muted ? TransportKind.MUTE : TransportKind.VOLUME,
            iconColor: AppColors.TEXT_SECONDARY,
            iconSize: 15
          })
          Slider({ value: player.volume, min: 0, max: 100, step: 1, style: SliderStyle.OutSet })
            .selectedColor(AppColors.PRIMARY)
            .trackColor(withAlpha(AppColors.TEXT_SECONDARY, '44'))
            .blockColor(AppColors.PRIMARY)
            .layoutWeight(1)
            .onChange((value: number, mode: SliderChangeMode) => {
              if (mode === SliderChangeMode.End || mode === SliderChangeMode.Click) {
                this.run(() => this.repo.setPlayerVolume(player.id, value));
              }
            })
          Text(`${player.volume}`)
            .fontSize(10)
            .fontColor(AppColors.TEXT_SECONDARY)
            .width(24)
            .textAlign(TextAlign.End)
        }
        .width('100%')
        .margin({ top: 4 })
      } else {
        Row({ space: 8 }) {
          TransportIcon({ kind: TransportKind.MUTE, iconColor: AppColors.TEXT_SECONDARY, iconSize: 15 })
          Column({ space: 1 }) {
            Text(this.s.fixedVolume)
              .fontSize(12)
              .fontWeight(FontWeight.Medium)
              .fontColor(AppColors.TEXT_SECONDARY)
            Text(this.s.volumeDisabledHint)
              .fontSize(10)
              .fontColor(withAlpha(AppColors.TEXT_SECONDARY, 'AA'))
              .maxLines(2)
          }
          .alignItems(HorizontalAlign.Start)
          .layoutWeight(1)
        }
        .width('100%')
        .padding({ top: 6 })
      }

      if (player.syncMaster.length > 0) {
        Text(this.s.independentPlayer)
          .fontSize(10)
          .fontColor(AppColors.TEXT_SECONDARY)
          .width('100%')
      }
    }
    .width('100%')
    .padding(16)
    .backgroundColor(this.isCurrent(player)
      ? withAlpha(AppColors.PRIMARY, '12') : withAlpha(AppColors.SURFACE, 'DD'))
    .borderRadius(22)
    .border({
      width: this.isCurrent(player) ? 1 : 0,
      color: withAlpha(AppColors.PRIMARY, '33')
    })
    .onClick(() => {
      if (!this.isCurrent(player)) {
        this.repo.selectPlayer(player.id);
        toast(this.s.switchedTo.replace('%s', player.name));
      }
    })
  }

  build() {
    Column() {
      Row() {
        Text(this.s.playersTitle)
          .fontSize(28)
          .fontWeight(FontWeight.Bold)
          .fontColor(AppColors.TEXT)
          .layoutWeight(1)
        Row() {
          UiIcon({ kind: UiIconKind.EXTERNAL, iconColor: AppColors.TEXT, iconSize: 20 })
        }
        .width(38)
        .height(38)
        .justifyContent(FlexAlign.Center)
      }
      .width('100%')
      .padding({ left: 16, right: 12, top: 8, bottom: 6 })

      if (this.repo.state.players.length === 0) {
        Text(this.s.noPlayersTitle)
          .fontSize(13)
          .fontColor(AppColors.TEXT_SECONDARY)
          .width('100%')
          .textAlign(TextAlign.Center)
          .padding({ top: 40 })
      } else {
        List({ space: 10 }) {
          ForEach(this.repo.state.players, (player: Player) => {
            ListItem() {
              this.PlayerCard(player)
            }
          }, (player: Player) => player.id)
        }
        .width('100%')
        .layoutWeight(1)
        .padding({ left: 16, right: 16, bottom: 12 })
        .scrollBar(BarState.Off)
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor(AppColors.BACKGROUND)
  }
}
