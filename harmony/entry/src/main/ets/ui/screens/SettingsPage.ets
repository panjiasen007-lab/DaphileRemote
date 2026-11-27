import { promptAction } from '@kit.ArkUI';
import { util } from '@kit.ArkTS';
import { bundleManager, common } from '@kit.AbilityKit';
import { AppContext } from '../../app/AppContext';
import {
  AppLanguage, ConnectionState, DiscoveredServer, HostPowerAction, ServerConfig, WakeState,
  friendlyErrorMessage, languageFromKey, languageLabel
} from '../../data/model/Models';
import { DaphileRepository } from '../../data/repo/DaphileRepository';
import { AppColors, THEME_OPTIONS, ThemeOption, withAlpha } from '../theme/Theme';
import { paletteFor, ThemePalette } from '../theme/ThemeRuntime';
import { UiIcon, UiIconKind } from '../components/UiIcon';
import { stringsFor, UiStrings } from '../i18n/I18n';

function toast(message: string): void {
  promptAction.showToast({ message: message, duration: 2200 });
}

function connectionText(state: ConnectionState): string {
  switch (state) {
    case ConnectionState.CONNECTING: return '正在连接';
    case ConnectionState.CONNECTED: return '已连接';
    case ConnectionState.RECONNECTING: return '正在重连';
    case ConnectionState.LOST: return '连接已断开';
    default: return '未连接';
  }
}

export class ServerEditParam {
  server: ServerConfig | null;

  constructor(server: ServerConfig | null) {
    this.server = server;
  }
}

const LANGUAGE_VALUES: AppLanguage[] = [
  AppLanguage.AUTO, AppLanguage.ZH, AppLanguage.ZH_TW, AppLanguage.EN,
  AppLanguage.JA, AppLanguage.KO, AppLanguage.KO_KP, AppLanguage.RU,
  AppLanguage.DE, AppLanguage.FR, AppLanguage.ES, AppLanguage.IT,
  AppLanguage.PT, AppLanguage.NL, AppLanguage.EL, AppLanguage.VI,
  AppLanguage.TH, AppLanguage.AR, AppLanguage.AZ, AppLanguage.FA
];

const LAYOUT_VALUES: string[] = ['auto', 'phone', 'foldable', 'tablet', 'pc'];

@ComponentV2
export struct SettingsPage {
  @Param pathStack: NavPathStack = new NavPathStack();
  @Param effectiveLayout: string = 'phone';
  private repo: DaphileRepository = AppContext.get().repository;

  @Local showThemePicker: boolean = false;
  @Local showLanguagePicker: boolean = false;
  @Local showLayoutPicker: boolean = false;
  @Local lyricsCount: number = 0;
  @Local appVersion: string = '1.9.2';

  aboutToAppear(): void {
    this.lyricsCount = this.repo.lyricsLibraryCount();
    try {
      this.appVersion = bundleManager.getBundleInfoForSelfSync(
        bundleManager.BundleFlag.GET_BUNDLE_INFO_DEFAULT).versionName;
    } catch (error) {
      this.appVersion = '1.9.2';
    }
  }

  private powerLabel(action: HostPowerAction): string {
    switch (action) {
      case HostPowerAction.RESTART: return this.s.hostRestart;
      case HostPowerAction.SHUTDOWN: return this.s.hostShutdown;
      case HostPowerAction.SUSPEND: return this.s.hostSuspend;
      default: return this.s.hostPower;
    }
  }

  private confirmHostPower(action: HostPowerAction): void {
    promptAction.showDialog({
      title: this.powerLabel(action),
      message: this.s.hostPowerHint,
      buttons: [
        { text: this.s.cancel, color: AppColors.TEXT_SECONDARY },
        { text: this.s.confirm, color: action === HostPowerAction.SHUTDOWN
          ? AppColors.ERROR : AppColors.PRIMARY }
      ]
    }).then((result) => {
      if (result.index === 1) {
        this.repo.hostPower(action).then(() => {
          toast(this.powerLabel(action));
        }).catch((error: Error) => toast(friendlyErrorMessage(error)));
      }
    }).catch(() => {
    });
  }

  private confirmRescan(): void {
    promptAction.showDialog({
      title: this.s.rescanTitle,
      message: `${this.s.rescanBody}\n${this.s.rebuildHint}`,
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

  private confirmClearLyrics(): void {
    promptAction.showDialog({
      title: this.s.clearLyricsTitle,
      message: this.s.clearLyricsBody.replace('%d', `${this.lyricsCount}`),
      buttons: [
        { text: this.s.cancel, color: AppColors.TEXT_SECONDARY },
        { text: this.s.clear, color: AppColors.ERROR }
      ]
    }).then((result) => {
      if (result.index === 1) {
        this.lyricsCount = this.repo.clearLyricsLibrary();
        toast(this.s.clearLyricsTitle);
      }
    }).catch(() => {
    });
  }

  private showAbout(): void {
    promptAction.showDialog({
      title: this.s.appShortName,
      message: `${this.s.version} ${this.appVersion}\n\n${this.s.aboutLicense}`,
      buttons: [{ text: this.s.confirm, color: AppColors.PRIMARY }]
    }).catch(() => {
    });
  }

  private wakeLabel(): string {
    return this.repo.state.wakeState === WakeState.WAKING ? this.s.wolWaking : this.s.wol;
  }

  private wakeHint(): string {
    const server = this.repo.state.server;
    if (server === null || server.macAddress.length === 0) {
      return this.s.wolMacMissing;
    }
    if (this.repo.state.wakeState === WakeState.WAKING) {
      return this.s.wolWaking;
    }
    if (this.repo.state.wakeState === WakeState.ONLINE) {
      return this.s.wolOnline;
    }
    if (this.repo.state.wakeState === WakeState.FAILED) {
      return this.s.wolTimeout;
    }
    return this.s.wolHint;
  }

  @Computed
  get s(): UiStrings {
    const settings = this.repo.state.settings;
    // 同时读取 theme，确保主题切换时页面重新构建
    return stringsFor(settings.theme.length > 0 ? settings.language : 'en');
  }

  private themeLabel(key: string): string {
    switch (key) {
      case 'system': return this.s.themeSystem;
      case 'light': return this.s.themeLight;
      case 'spring': return this.s.themeSpring;
      case 'summer': return this.s.themeSummer;
      case 'autumn': return this.s.themeAutumn;
      case 'winter': return this.s.themeWinter;
      case 'taiji': return this.s.themeTaiji;
      case 'panda': return this.s.themePanda;
      case 'china_red': return this.s.themeChinaRed;
      case 'wind': return this.s.themeWind;
      case 'flower': return this.s.themeFlower;
      case 'snow': return this.s.themeSnow;
      case 'moon': return this.s.themeMoon;
      case 'dark':
      default:
        return this.s.themeDark;
    }
  }

  private layoutLabel(key: string): string {
    switch (key) {
      case 'phone': return this.s.layoutPhone;
      case 'foldable': return this.s.layoutFoldable;
      case 'tablet': return this.s.layoutTablet;
      case 'pc': return this.s.layoutPc;
      default: return this.s.layoutAuto;
    }
  }

  @Computed
  get layoutSummary(): string {
    const manual = this.repo.state.settings.layoutMode;
    if (manual === 'auto') {
      return `${this.s.layoutAuto} · ${this.layoutLabel(this.effectiveLayout)}`;
    }
    return this.layoutLabel(manual);
  }

  private run(action: () => Promise<void>): void {
    action().catch((error: Error) => toast(friendlyErrorMessage(error)));
  }

  private languageOptions(): SelectOption[] {
    const options: SelectOption[] = [];
    for (const language of LANGUAGE_VALUES) {
      options.push({ value: language === AppLanguage.AUTO ? '跟随系统' : languageLabel(language) });
    }
    return options;
  }

  private languageIndex(): number {
    const current = languageFromKey(this.repo.state.settings.language);
    for (let index = 0; index < LANGUAGE_VALUES.length; index++) {
      if (LANGUAGE_VALUES[index] === current) {
        return index;
      }
    }
    return 0;
  }

  @Builder
  SectionTitle(title: string) {
    Text(title)
      .fontSize(13)
      .fontWeight(FontWeight.Medium)
      .fontColor(AppColors.PRIMARY)
      .width('100%')
      .margin({ top: 16, bottom: 2 })
  }

  @Builder
  SwitchRow(title: string, subtitle: string, value: boolean, onChange: (value: boolean) => void) {
    Row({ space: 12 }) {
      Column({ space: 3 }) {
        Text(title)
          .fontSize(15)
          .fontWeight(FontWeight.Medium)
          .fontColor(AppColors.TEXT)
        if (subtitle.length > 0) {
          Text(subtitle)
            .fontSize(11)
            .fontColor(AppColors.TEXT_SECONDARY)
            .maxLines(2)
        }
      }
      .alignItems(HorizontalAlign.Start)
      .layoutWeight(1)

      Toggle({ type: ToggleType.Switch, isOn: value })
        .selectedColor(AppColors.PRIMARY)
        .onChange((isOn: boolean) => onChange(isOn))
    }
    .width('100%')
    .padding({ top: 10, bottom: 10 })
  }

  @Builder
  ActionRow(icon: UiIconKind, title: string, subtitle: string, action: () => void) {
    Row({ space: 12 }) {
      UiIcon({ kind: icon, iconColor: AppColors.TEXT_SECONDARY, iconSize: 19 })
      Column({ space: 2 }) {
        Text(title)
          .fontSize(15)
          .fontWeight(FontWeight.Medium)
          .fontColor(AppColors.TEXT)
        if (subtitle.length > 0) {
          Text(subtitle)
            .fontSize(11)
            .fontColor(AppColors.TEXT_SECONDARY)
            .maxLines(2)
            .textOverflow({ overflow: TextOverflow.Ellipsis })
        }
      }
      .alignItems(HorizontalAlign.Start)
      .layoutWeight(1)
      UiIcon({ kind: UiIconKind.CHEVRON_RIGHT, iconColor: AppColors.TEXT_SECONDARY, iconSize: 16 })
    }
    .width('100%')
    .padding({ top: 10, bottom: 10 })
    .onClick(() => action())
  }

  @Builder
  PowerButton(label: string, danger: boolean, action: () => void) {
    Row() {
      Text(label)
        .fontSize(13)
        .fontColor(danger ? AppColors.ERROR : AppColors.TEXT)
        .maxLines(1)
    }
    .height(38)
    .layoutWeight(1)
    .justifyContent(FlexAlign.Center)
    .backgroundColor(AppColors.SURFACE_HIGH)
    .borderRadius(19)
    .border({ width: 0.5, color: danger ? withAlpha(AppColors.ERROR, '88') : AppColors.OUTLINE })
    .onClick(() => action())
  }

  @Builder
  HostPowerBlock() {
    Column({ space: 8 }) {
      Row({ space: 12 }) {
        UiIcon({ kind: UiIconKind.POWER, iconColor: AppColors.TEXT_SECONDARY, iconSize: 19 })
        Text(this.s.hostPower)
          .fontSize(15)
          .fontWeight(FontWeight.Medium)
          .fontColor(AppColors.TEXT)
      }
      .width('100%')

      Text(this.s.hostPowerHint)
        .fontSize(11)
        .fontColor(AppColors.TEXT_SECONDARY)
        .width('100%')

      Row({ space: 8 }) {
        this.PowerButton(this.s.reboot, false, () => this.confirmHostPower(HostPowerAction.RESTART))
        this.PowerButton(this.s.shutdown, true, () => this.confirmHostPower(HostPowerAction.SHUTDOWN))
        this.PowerButton(this.s.suspend, false, () => this.confirmHostPower(HostPowerAction.SUSPEND))
      }
      .width('100%')
    }
    .width('100%')
    .padding({ top: 8, bottom: 6 })
  }

  @Builder
  WolBlock() {
    Row({ space: 12 }) {
      UiIcon({ kind: UiIconKind.WIFI, iconColor: AppColors.TEXT_SECONDARY, iconSize: 19 })
      Column({ space: 2 }) {
        Text(this.s.wol)
          .fontSize(15)
          .fontWeight(FontWeight.Medium)
          .fontColor(AppColors.TEXT)
        Text(this.wakeHint())
          .fontSize(11)
          .fontColor(AppColors.TEXT_SECONDARY)
          .maxLines(2)
      }
      .alignItems(HorizontalAlign.Start)
      .layoutWeight(1)

      Row({ space: 4 }) {
        if (this.repo.state.wakeState === WakeState.WAKING) {
          LoadingProgress()
            .width(16)
            .height(16)
            .color(AppColors.PRIMARY)
        }
        Text(this.wakeLabel())
          .fontSize(12)
          .fontColor(AppColors.PRIMARY)
          .maxLines(1)
      }
      .height(34)
      .padding({ left: 14, right: 14 })
      .borderRadius(17)
      .border({ width: 0.5, color: withAlpha(AppColors.PRIMARY, '99') })
      .backgroundColor(withAlpha(AppColors.PRIMARY, '14'))
      .enabled(this.repo.state.server !== null &&
        (this.repo.state.server as ServerConfig).macAddress.length > 0 &&
        this.repo.state.wakeState !== WakeState.WAKING)
      .onClick(() => {
        this.repo.wakeHost().then(() => {
          toast(this.s.wolOnline);
        }).catch((error: Error) => {
          toast(this.repo.state.wakeState === WakeState.FAILED
            ? this.s.wolTimeout : friendlyErrorMessage(error));
        });
      })
    }
    .width('100%')
    .padding({ top: 8, bottom: 6 })
  }

  private openUrl(url: string): void {
    try {
      const context = getContext(this) as common.UIAbilityContext;
      context.openLink(url).catch(() => {
        toast(url);
      });
    } catch (error) {
      toast(url);
    }
  }

  private serverSummary(): string {
    const server = this.repo.state.server;
    const info = this.repo.state.serverInfo;
    if (server === null) {
      return this.s.addServerHint;
    }
    const version = info !== null && info.version.length > 0 ? `LMS ${info.version}` : 'LMS';
    const players = `${this.repo.state.players.length} ${this.s.playerLabel}`;
    return `${version} · ${players} · Admin ${server.daphilePort}`;
  }

  @Builder
  ServerAction(icon: UiIconKind, label: string, action: () => void) {
    Row({ space: 8 }) {
      UiIcon({ kind: icon, iconColor: AppColors.PRIMARY, iconSize: 17 })
      Text(label)
        .fontSize(12)
        .fontColor(AppColors.TEXT)
        .maxLines(1)
        .textOverflow({ overflow: TextOverflow.Ellipsis })
    }
    .width('50%')
    .height(40)
    .onClick(() => action())
  }

  @Builder
  ServerCard() {
    Column({ space: 8 }) {
      Row({ space: 10 }) {
        Column() {
          UiIcon({ kind: UiIconKind.PLAYERS, iconColor: AppColors.PRIMARY, iconSize: 22 })
        }
        .width(42)
        .height(42)
        .justifyContent(FlexAlign.Center)
        .backgroundColor(withAlpha(AppColors.PRIMARY, '18'))
        .borderRadius(10)

        Column({ space: 2 }) {
          Text(this.repo.state.server !== null
            ? `${this.repo.state.server.host}` : this.s.noServerAdded)
            .fontSize(19)
            .fontWeight(FontWeight.Bold)
            .fontColor(AppColors.TEXT)
            .maxLines(1)
            .textOverflow({ overflow: TextOverflow.Ellipsis })
          Text(this.repo.state.server !== null
            ? `${this.repo.state.server.host}:${this.repo.state.server.port}`
            : this.s.addServerHint)
            .fontSize(11)
            .fontColor(AppColors.TEXT_SECONDARY)
            .maxLines(1)
          Text(this.serverSummary())
            .fontSize(11)
            .fontColor(AppColors.TEXT_SECONDARY)
            .maxLines(1)
            .textOverflow({ overflow: TextOverflow.Ellipsis })
        }
        .alignItems(HorizontalAlign.Start)
        .layoutWeight(1)

        Text(connectionText(this.repo.state.connection))
          .fontSize(11)
          .fontColor(this.repo.state.connection === ConnectionState.CONNECTED
            ? AppColors.PRIMARY : AppColors.WARN)
      }
      .width('100%')

      Flex({ wrap: FlexWrap.Wrap }) {
        this.ServerAction(UiIconKind.WIFI, this.s.serverManage, () => {
          this.pathStack.pushPathByName('servers', '');
        })
        this.ServerAction(UiIconKind.EXTERNAL, 'Daphile Admin', () => {
          const server = this.repo.state.server;
          if (server !== null) {
            this.openUrl(`http://${server.host}:${server.daphilePort}`);
          }
        })
        this.ServerAction(UiIconKind.MUSIC, this.s.lmsWeb, () => {
          const server = this.repo.state.server;
          if (server !== null) {
            this.openUrl(`http://${server.host}:${server.port}`);
          }
        })
        this.ServerAction(UiIconKind.INFO, this.s.systemInfo, () => {
          this.pathStack.pushPathByName('systemInfo', '');
        })
      }
      .width('100%')
      .margin({ top: 2 })
    }
    .width('100%')
    .padding(16)
    .backgroundColor(withAlpha(AppColors.SURFACE, 'DD'))
    .borderRadius(24)
  }

  @Builder
  RadioMark(selected: boolean) {
    Stack() {
      Column()
        .width(20)
        .height(20)
        .borderRadius(10)
        .border({ width: 2, color: selected ? AppColors.PRIMARY : withAlpha(AppColors.TEXT_SECONDARY, '99') })
      if (selected) {
        Column()
          .width(10)
          .height(10)
          .borderRadius(5)
          .backgroundColor(AppColors.PRIMARY)
      }
    }
    .width(20)
    .height(20)
  }

  @Builder
  ThemePicker() {
    Column() {
      Text(this.s.theme)
        .fontSize(20)
        .fontWeight(FontWeight.Medium)
        .fontColor(AppColors.TEXT)
        .width('100%')
        .padding({ left: 22, right: 22, top: 18, bottom: 6 })

      List({ space: 2 }) {
        ForEach(THEME_OPTIONS, (option: ThemeOption) => {
          ListItem() {
            Row({ space: 12 }) {
              this.RadioMark(this.repo.state.settings.theme === option.key)
              Row({ space: 3 }) {
                Column()
                  .width(13)
                  .height(13)
                  .borderRadius(2)
                  .backgroundColor(paletteFor(option.key).primary)
                Column()
                  .width(13)
                  .height(13)
                  .borderRadius(2)
                  .backgroundColor(paletteFor(option.key).primaryContainer)
                Column()
                  .width(13)
                  .height(13)
                  .borderRadius(2)
                  .backgroundColor(paletteFor(option.key).surface)
                Column()
                  .width(13)
                  .height(13)
                  .borderRadius(2)
                  .backgroundColor(paletteFor(option.key).background)
              }
              Text(this.themeLabel(option.key))
                .fontSize(14)
                .fontColor(AppColors.TEXT)
                .layoutWeight(1)
                .maxLines(2)
            }
            .width('100%')
            .height(46)
            .padding({ left: 22, right: 16 })
            .focusable(false)
            .onClick(() => {
              this.showThemePicker = false;
              this.run(() => this.repo.setTheme(option.key));
            })
          }
        }, (option: ThemeOption) => option.key)
      }
      .width('100%')
      .height(520)
      .scrollBar(BarState.Off)
      .focusable(false)

      Row() {
        Text(this.s.close)
          .fontSize(14)
          .fontColor(AppColors.PRIMARY)
          .padding({ left: 14, right: 14, top: 8, bottom: 8 })
          .onClick(() => {
            this.showThemePicker = false;
          })
      }
      .width('100%')
      .height(52)
      .justifyContent(FlexAlign.End)
      .padding({ right: 10 })
    }
    .width('86%')
    .constraintSize({ maxWidth: 440 })
    .backgroundColor(withAlpha(AppColors.SURFACE_HIGH, 'FA'))
    .borderRadius(26)
    .border({ width: 0.5, color: AppColors.OUTLINE })
  }

  @Builder
  ThemePickerOverlay() {
    Column() {
      this.ThemePicker()
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
    .backgroundColor('#99000000')
  }

  @Builder
  LanguagePicker() {
    Column() {
      Text(this.s.language)
        .fontSize(20)
        .fontWeight(FontWeight.Medium)
        .fontColor(AppColors.TEXT)
        .width('100%')
        .padding({ left: 22, right: 22, top: 18, bottom: 6 })

      List({ space: 2 }) {
        ForEach(LANGUAGE_VALUES, (language: AppLanguage) => {
          ListItem() {
            Row({ space: 12 }) {
              this.RadioMark(languageFromKey(this.repo.state.settings.language) === language)
              Text(language === AppLanguage.AUTO ? this.s.languageAuto : languageLabel(language))
                .fontSize(14)
                .fontColor(AppColors.TEXT)
                .layoutWeight(1)
            }
            .width('100%')
            .height(46)
            .padding({ left: 22, right: 16 })
            .focusable(false)
            .onClick(() => {
              this.showLanguagePicker = false;
              this.run(() => this.repo.setLanguage(language));
            })
          }
        }, (language: AppLanguage) => language as string)
      }
      .width('100%')
      .height(520)
      .scrollBar(BarState.Off)
      .focusable(false)

      Row() {
        Text(this.s.close)
          .fontSize(14)
          .fontColor(AppColors.PRIMARY)
          .padding({ left: 14, right: 14, top: 8, bottom: 8 })
          .onClick(() => {
            this.showLanguagePicker = false;
          })
      }
      .width('100%')
      .height(52)
      .justifyContent(FlexAlign.End)
      .padding({ right: 10 })
    }
    .width('86%')
    .constraintSize({ maxWidth: 440 })
    .backgroundColor(withAlpha(AppColors.SURFACE_HIGH, 'FA'))
    .borderRadius(26)
    .border({ width: 0.5, color: AppColors.OUTLINE })
  }

  @Builder
  LanguagePickerOverlay() {
    Column() {
      this.LanguagePicker()
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
    .backgroundColor('#99000000')
  }

  @Builder
  LayoutPicker() {
    Column() {
      Text(this.s.layoutMode)
        .fontSize(20)
        .fontWeight(FontWeight.Medium)
        .fontColor(AppColors.TEXT)
        .width('100%')
        .padding({ left: 22, right: 22, top: 18, bottom: 6 })

      List({ space: 2 }) {
        ForEach(LAYOUT_VALUES, (mode: string) => {
          ListItem() {
            Row({ space: 12 }) {
              this.RadioMark(this.repo.state.settings.layoutMode === mode)
              Text(this.layoutLabel(mode))
                .fontSize(14)
                .fontColor(AppColors.TEXT)
                .layoutWeight(1)
              if (mode === 'auto') {
                Text(this.layoutLabel(this.effectiveLayout))
                  .fontSize(11)
                  .fontColor(AppColors.TEXT_SECONDARY)
              }
            }
            .width('100%')
            .height(48)
            .padding({ left: 22, right: 16 })
            .focusable(false)
            .onClick(() => {
              this.showLayoutPicker = false;
              this.run(() => this.repo.setLayoutMode(mode));
            })
          }
        }, (mode: string) => mode)
      }
      .width('100%')
      .height(250)
      .scrollBar(BarState.Off)
      .focusable(false)

      Row() {
        Text(this.s.close)
          .fontSize(14)
          .fontColor(AppColors.PRIMARY)
          .padding({ left: 14, right: 14, top: 8, bottom: 8 })
          .onClick(() => {
            this.showLayoutPicker = false;
          })
      }
      .width('100%')
      .height(52)
      .justifyContent(FlexAlign.End)
      .padding({ right: 10 })
    }
    .width('86%')
    .constraintSize({ maxWidth: 440 })
    .backgroundColor(withAlpha(AppColors.SURFACE_HIGH, 'FA'))
    .borderRadius(26)
    .border({ width: 0.5, color: AppColors.OUTLINE })
  }

  @Builder
  LayoutPickerOverlay() {
    Column() {
      this.LayoutPicker()
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
    .backgroundColor('#99000000')
  }

  @Builder
  NavRow(icon: UiIconKind, title: string, value: string, action: () => void) {
    Row({ space: 12 }) {
      UiIcon({ kind: icon, iconColor: AppColors.TEXT_SECONDARY, iconSize: 19 })
      Text(title)
        .fontSize(15)
        .fontWeight(FontWeight.Medium)
        .fontColor(AppColors.TEXT)
        .layoutWeight(1)
      Text(value)
        .fontSize(12)
        .fontColor(AppColors.TEXT_SECONDARY)
        .maxLines(1)
      UiIcon({ kind: UiIconKind.CHEVRON_RIGHT, iconColor: AppColors.TEXT_SECONDARY, iconSize: 16 })
    }
    .width('100%')
    .height(48)
    .onClick(() => action())
  }

  build() {
    Stack() {
      Scroll() {
      Column({ space: 2 }) {
        Text(this.s.settingsTitle)
          .fontSize(28)
          .fontWeight(FontWeight.Bold)
          .fontColor(AppColors.TEXT)
          .width('100%')
          .padding({ top: 6, bottom: 8 })

        this.ServerCard()

        this.SectionTitle(this.s.playbackSection)
        Row({ space: 12 }) {
          UiIcon({ kind: UiIconKind.CLOCK, iconColor: AppColors.TEXT_SECONDARY, iconSize: 19 })
          Column({ space: 2 }) {
            Text(this.s.pollInterval)
              .fontSize(15)
              .fontWeight(FontWeight.Medium)
              .fontColor(AppColors.TEXT)
            Text(this.s.pollIntervalSub)
              .fontSize(11)
              .fontColor(AppColors.TEXT_SECONDARY)
              .maxLines(2)
          }
          .alignItems(HorizontalAlign.Start)
          .layoutWeight(1)
          Text(`${this.repo.state.settings.pollSeconds} s`)
            .fontSize(12)
            .fontColor(AppColors.TEXT_SECONDARY)
          UiIcon({ kind: UiIconKind.CHEVRON_RIGHT, iconColor: AppColors.TEXT_SECONDARY, iconSize: 16 })
        }
        .width('100%')
        .padding({ top: 10, bottom: 10 })
        .onClick(() => {
          const current = this.repo.state.settings.pollSeconds;
          const next = current < 3 ? 3 : (current < 5 ? 5 : (current < 10 ? 10 : 2));
          this.run(() => this.repo.setPollSeconds(next));
        })

        this.SwitchRow(this.s.mediaNotification, this.s.mediaNotificationSub,
          this.repo.state.settings.notificationsEnabled,
          (value: boolean) => this.run(() => this.repo.setNotificationsEnabled(value)))
        this.SwitchRow(this.s.lockScreen, this.s.lockScreenSub,
          this.repo.state.settings.lockScreenControls,
          (value: boolean) => this.run(() => this.repo.setLockScreenControls(value)))
        this.SwitchRow(this.s.onlineMeta, this.s.onlineMetaSub,
          this.repo.state.settings.onlineMetadataEnabled,
          (value: boolean) => this.run(() => this.repo.setOnlineMetadataEnabled(value)))

        this.SectionTitle(this.s.appearanceSection)
        this.NavRow(UiIconKind.PALETTE, this.s.theme,
          this.themeLabel(this.repo.state.settings.theme), () => {
            this.showThemePicker = true;
          })
        this.NavRow(UiIconKind.GLOBE, this.s.language,
          this.repo.state.settings.language === 'auto'
            ? this.s.languageAuto
            : languageLabel(languageFromKey(this.repo.state.settings.language)), () => {
            this.showLanguagePicker = true;
          })
        this.NavRow(UiIconKind.GRID, this.s.layoutMode, this.layoutSummary, () => {
            this.showLayoutPicker = true;
          })

        this.SwitchRow(this.s.titleFromFileName, '',
          this.repo.state.settings.useFileNameAsTitle,
          (value: boolean) => this.run(() => this.repo.setUseFileNameAsTitle(value)))
        this.SwitchRow(this.s.fixGarbledText, '',
          this.repo.state.settings.fixGarbledText,
          (value: boolean) => this.run(() => this.repo.setFixGarbledText(value)))

        this.SectionTitle(this.s.storageSection)
        this.ActionRow(UiIconKind.TAG, this.s.lyricsLibrary,
          `${this.repo.lyricsLibraryPath()} · ${this.s.lyricsFileCount.replace('%d', `${this.lyricsCount}`)}`,
          () => this.confirmClearLyrics())
        this.ActionRow(UiIconKind.MUSIC, this.s.clearCoverCache, this.s.clearCoverCacheSub, () => {
          this.repo.clearCoverCache();
          toast(this.s.cacheCleared);
        })

        this.SectionTitle(this.s.systemSection)
        this.ActionRow(UiIconKind.INFO, this.s.systemInfo, this.s.systemInfoSub,
          () => this.pathStack.pushPathByName('systemInfo', ''))
        this.ActionRow(UiIconKind.REFRESH, this.s.rescan, this.s.rescanSub,
          () => this.confirmRescan())
        this.HostPowerBlock()
        this.WolBlock()
        this.SwitchRow(this.s.wolAuto, this.s.wolAutoHint,
          this.repo.state.settings.autoWake,
          (value: boolean) => this.run(() => this.repo.setAutoWake(value)))

        this.SectionTitle(this.s.aboutSection)
        this.NavRow(UiIconKind.INFO, this.s.version, this.appVersion, () => this.showAbout())

        Row()
          .width('100%')
          .height(20)
      }
      .width('100%')
      .padding({ left: 16, right: 16, top: 4 })
      }
      .width('100%')
      .height('100%')
      .scrollBar(BarState.Off)
      .align(Alignment.TopStart)

      if (this.showThemePicker) {
        this.ThemePickerOverlay()
      }
      if (this.showLanguagePicker) {
        this.LanguagePickerOverlay()
      }

      if (this.showLayoutPicker) {
        this.LayoutPickerOverlay()
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor(AppColors.BACKGROUND)
  }
}

@ComponentV2
export struct ServersPage {
  @Param pathStack: NavPathStack = new NavPathStack();
  private repo: DaphileRepository = AppContext.get().repository;

  @Local discovering: boolean = false;
  @Local discovered: DiscoveredServer[] = [];
  @Local showDiscovered: boolean = false;

  @Computed
  get s(): UiStrings {
    const settings = this.repo.state.settings;
    // 同时读取 theme，确保主题切换时页面重新构建
    return stringsFor(settings.theme.length > 0 ? settings.language : 'en');
  }

  private startDiscovery(): void {
    if (this.discovering) {
      return;
    }
    this.discovering = true;
    this.repo.discoverServers().then((servers) => {
      this.discovering = false;
      this.discovered = servers;
      if (servers.length === 0) {
        toast(this.s.noServersFound);
      } else {
        this.showDiscovered = true;
      }
    }).catch((error: Error) => {
      this.discovering = false;
      toast(friendlyErrorMessage(error));
    });
  }

  private addDiscovered(item: DiscoveredServer): void {
    let existingId = '';
    for (const server of this.repo.state.servers) {
      if (server.host === item.host && server.port === item.port) {
        existingId = server.id;
        break;
      }
    }
    const save = existingId.length > 0
      ? this.repo.switchServer(existingId)
      : this.repo.upsertServer({
        id: util.generateRandomUUID(true),
        name: item.name,
        host: item.host,
        port: item.port,
        daphilePort: 80,
        username: '',
        password: '',
        useHttps: false,
        macAddress: ''
      }, true);
    save.then(() => {
      this.showDiscovered = false;
      this.discovered = [];
      toast(this.s.saved);
    }).catch((error: Error) => toast(friendlyErrorMessage(error)));
  }

  @Builder
  DiscoveredOverlay() {
    Column() {
      Column({ space: 10 }) {
        Text(this.s.discoverServers)
          .fontSize(16)
          .fontWeight(FontWeight.Medium)
          .fontColor(AppColors.TEXT)
          .width('100%')

        List({ space: 6 }) {
          ForEach(this.discovered, (item: DiscoveredServer) => {
            ListItem() {
              Row({ space: 10 }) {
                Column({ space: 2 }) {
                  Text(item.name)
                    .fontSize(14)
                    .fontColor(AppColors.TEXT)
                    .maxLines(1)
                    .textOverflow({ overflow: TextOverflow.Ellipsis })
                  Text(`${item.host}:${item.port} · LMS ${item.version}`)
                    .fontSize(11)
                    .fontColor(AppColors.TEXT_SECONDARY)
                    .maxLines(1)
                    .textOverflow({ overflow: TextOverflow.Ellipsis })
                }
                .alignItems(HorizontalAlign.Start)
                .layoutWeight(1)
                Row() {
                  Text(this.s.save)
                    .fontSize(12)
                    .fontColor(AppColors.PRIMARY)
                }
                .height(30)
                .padding({ left: 12, right: 12 })
                .borderRadius(15)
                .backgroundColor(withAlpha(AppColors.PRIMARY, '18'))
                .onClick(() => this.addDiscovered(item))
              }
              .width('100%')
              .padding(10)
              .backgroundColor(AppColors.SURFACE)
              .borderRadius(10)
            }
          }, (item: DiscoveredServer) => `${item.host}-${item.port}`)
        }
        .width('100%')
        .height(Math.min(300, Math.max(60, this.discovered.length * 62)))
        .scrollBar(BarState.Off)

        Row() {
          Text(this.s.close)
            .fontSize(14)
            .fontColor(AppColors.PRIMARY)
        }
        .width('100%')
        .height(40)
        .justifyContent(FlexAlign.End)
        .onClick(() => {
          this.showDiscovered = false;
        })
      }
      .width('86%')
    .constraintSize({ maxWidth: 440 })
      .padding(16)
      .backgroundColor(AppColors.SURFACE)
      .borderRadius(16)
      .border({ width: 0.5, color: AppColors.OUTLINE })
      .shadow({ radius: 24, color: '#66000000', offsetY: 6 })
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
    .backgroundColor('#88000000')
  }

  private confirmDelete(server: ServerConfig): void {
    promptAction.showDialog({
      title: this.s.deleteServerTitle,
      message: this.s.deleteServerBody.replace('%s', server.name),
      buttons: [
        { text: this.s.cancel, color: AppColors.TEXT_SECONDARY },
        { text: this.s.delete, color: AppColors.ERROR }
      ]
    }).then((result) => {
      if (result.index === 1) {
        this.repo.deleteServer(server.id).catch((error: Error) => toast(friendlyErrorMessage(error)));
      }
    }).catch(() => {
    });
  }

  @Builder
  ServerRow(server: ServerConfig) {
    Column({ space: 8 }) {
      Row() {
        Column() {
          Row({ space: 6 }) {
            Text(server.name)
              .fontSize(16)
              .fontColor(this.repo.state.server !== null && this.repo.state.server.id === server.id
                ? AppColors.PRIMARY : AppColors.TEXT)
              .fontWeight(FontWeight.Medium)
            if (this.repo.state.server !== null && this.repo.state.server.id === server.id) {
              Text(this.s.currentBadge)
                .fontSize(9)
                .fontColor(AppColors.ON_PRIMARY)
                .padding({ left: 5, right: 5, top: 1, bottom: 1 })
                .backgroundColor(AppColors.PRIMARY)
                .borderRadius(4)
            }
          }
          Text(`${server.host}:${server.port} · Daphile ${server.daphilePort}`)
            .fontSize(11)
            .fontColor(AppColors.TEXT_SECONDARY)
            .margin({ top: 3 })
        }
        .alignItems(HorizontalAlign.Start)
        .layoutWeight(1)
        if (server.macAddress.length > 0) {
          Text('WOL')
            .fontSize(9)
            .fontColor(AppColors.WARN)
            .padding({ left: 5, right: 5, top: 1, bottom: 1 })
            .backgroundColor(withAlpha(AppColors.WARN, '22'))
            .borderRadius(4)
        }
      }
      .width('100%')

      Row({ space: 8 }) {
        Button('切换')
          .fontSize(11)
          .fontColor(AppColors.TEXT)
          .backgroundColor(AppColors.SURFACE_HIGH)
          .enabled(this.repo.state.server === null || this.repo.state.server.id !== server.id)
          .onClick(() => this.repo.switchServer(server.id).catch((error: Error) => toast(friendlyErrorMessage(error))))
        Button(this.s.edit)
          .fontSize(11)
          .fontColor(AppColors.TEXT)
          .backgroundColor(AppColors.SURFACE_HIGH)
          .onClick(() => this.pathStack.pushPathByName('serverEdit', new ServerEditParam(server)))
        Button(this.s.delete)
          .fontSize(11)
          .fontColor(AppColors.ERROR)
          .backgroundColor(AppColors.SURFACE_HIGH)
          .onClick(() => this.confirmDelete(server))
      }
      .width('100%')
    }
    .width('100%')
    .padding(12)
    .backgroundColor(this.repo.state.server !== null && this.repo.state.server.id === server.id
      ? withAlpha(AppColors.PRIMARY, '10') : AppColors.SURFACE)
    .borderRadius(12)
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
          Text(this.s.serverManage)
            .fontSize(24)
            .fontWeight(FontWeight.Bold)
            .fontColor(AppColors.TEXT)
            .layoutWeight(1)
          Button(this.s.addServer)
            .fontSize(11)
            .fontColor(AppColors.ON_PRIMARY)
            .backgroundColor(AppColors.PRIMARY)
            .height(32)
            .onClick(() => this.pathStack.pushPathByName('serverEdit', new ServerEditParam(null)))
        }
        .width('100%')
        .padding({ left: 8, right: 12, top: 8, bottom: 4 })

        if (this.repo.state.servers.length === 0) {
          Text(this.s.noServerAdded)
            .fontSize(13)
            .fontColor(AppColors.TEXT_SECONDARY)
            .width('100%')
            .textAlign(TextAlign.Center)
            .padding({ top: 40 })
        } else {
          List({ space: 10 }) {
            ForEach(this.repo.state.servers, (server: ServerConfig) => {
              ListItem() {
                this.ServerRow(server)
              }
            }, (server: ServerConfig) => server.id)
          }
          .width('100%')
          .layoutWeight(1)
          .padding({ left: 16, right: 16 })
          .scrollBar(BarState.Off)
        }

        Row({ space: 8 }) {
          if (this.discovering) {
            LoadingProgress()
              .width(16)
              .height(16)
              .color(AppColors.PRIMARY)
          }
          Text(this.discovering ? this.s.loading : this.s.discoverServers)
            .fontSize(12)
            .fontColor(this.discovering ? AppColors.TEXT_SECONDARY : AppColors.TEXT)
        }
        .width('100%')
        .height(42)
        .justifyContent(FlexAlign.Center)
        .backgroundColor(AppColors.SURFACE)
        .borderRadius(10)
        .margin({ left: 16, right: 16, top: 8, bottom: 16 })
        .enabled(!this.discovering)
        .onClick(() => this.startDiscovery())
      }
      .width('100%')
      .height('100%')

      if (this.showDiscovered) {
        this.DiscoveredOverlay()
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor(AppColors.BACKGROUND)
  }
}

@ComponentV2
export struct ServerEditPage {
  @Param pathStack: NavPathStack = new NavPathStack();
  @Param initial: ServerEditParam = new ServerEditParam(null);
  private repo: DaphileRepository = AppContext.get().repository;

  @Computed
  get s(): UiStrings {
    const settings = this.repo.state.settings;
    // 同时读取 theme，确保主题切换时页面重新构建
    return stringsFor(settings.theme.length > 0 ? settings.language : 'en');
  }

  @Local name: string = '';
  @Local host: string = '';
  @Local port: string = '9000';
  @Local daphilePort: string = '80';
  @Local username: string = '';
  @Local password: string = '';
  @Local mac: string = '';
  @Local useHttps: boolean = false;
  @Local busy: boolean = false;
  @Local errorText: string = '';

  aboutToAppear(): void {
    const server = this.initial.server;
    if (server !== null) {
      this.name = server.name;
      this.host = server.host;
      this.port = server.port.toString();
      this.daphilePort = server.daphilePort.toString();
      this.username = server.username;
      this.password = server.password;
      this.mac = server.macAddress;
      this.useHttps = server.useHttps;
    }
  }

  private buildConfig(): ServerConfig {
    return {
      id: this.initial.server !== null ? this.initial.server.id : util.generateRandomUUID(true),
      name: this.name.trim().length > 0 ? this.name.trim() : this.host.trim(),
      host: this.host.trim(),
      port: Number.parseInt(this.port.length > 0 ? this.port : '9000'),
      daphilePort: Number.parseInt(this.daphilePort.length > 0 ? this.daphilePort : '80'),
      username: this.username.trim(),
      password: this.password,
      useHttps: this.useHttps,
      macAddress: this.mac.trim()
    };
  }

  private validate(config: ServerConfig): string {
    if (config.host.length === 0) {
      return this.s.hostRequired;
    }
    if (Number.isNaN(config.port) || config.port < 1 || config.port > 65535) {
      return this.s.lmsPortRange;
    }
    if (Number.isNaN(config.daphilePort) || config.daphilePort < 1 || config.daphilePort > 65535) {
      return this.s.daphilePortRange;
    }
    return '';
  }

  private save(test: boolean): void {
    const config = this.buildConfig();
    const validation = this.validate(config);
    if (validation.length > 0) {
      this.errorText = validation;
      return;
    }
    this.busy = true;
    this.errorText = '';
    const work = async () => {
      if (test) {
        await this.repo.testServer(config);
      }
      await this.repo.upsertServer(config, true);
      this.busy = false;
      toast(this.s.saved);
      this.pathStack.pop();
    };
    work().catch((error: Error) => {
      this.busy = false;
      this.errorText = friendlyErrorMessage(error);
    });
  }

  @Builder
  Field(label: string, placeholder: string, value: string, onChange: (value: string) => void,
    type: InputType = InputType.Normal) {
    Column({ space: 4 }) {
      Text(label)
        .fontSize(11)
        .fontColor(AppColors.TEXT_SECONDARY)
        .width('100%')
      TextInput({ placeholder: placeholder, text: value })
        .type(type)
        .fontSize(14)
        .fontColor(AppColors.TEXT)
        .placeholderColor(AppColors.TEXT_SECONDARY)
        .backgroundColor(AppColors.SURFACE)
        .borderRadius(8)
        .onChange((text: string) => onChange(text))
    }
    .width('100%')
  }

  build() {
    Scroll() {
      Column({ space: 10 }) {
        Row({ space: 6 }) {
          Row() {
            UiIcon({ kind: UiIconKind.BACK, iconColor: AppColors.TEXT, iconSize: 22 })
          }
          .width(40)
          .height(40)
          .justifyContent(FlexAlign.Center)
          .onClick(() => this.pathStack.pop())
          Text(this.initial.server === null ? this.s.addServer : this.s.editServer)
            .fontSize(24)
            .fontWeight(FontWeight.Bold)
            .fontColor(AppColors.TEXT)
            .layoutWeight(1)
        }
        .width('100%')
        .margin({ left: -8 })

        this.Field(this.s.nameOptional, 'Daphile', this.name, (value: string) => {
          this.name = value;
        })
        this.Field(this.s.hostAddressLabel, '192.168.124.88', this.host, (value: string) => {
          this.host = value;
        })
        Row({ space: 8 }) {
          Column() {
            this.Field(this.s.lmsHttpPort, '9000', this.port, (value: string) => {
              this.port = value;
            }, InputType.Number)
          }
          .layoutWeight(1)
          Column() {
            this.Field(this.s.daphilePortLabel, '80', this.daphilePort, (value: string) => {
              this.daphilePort = value;
            }, InputType.Number)
          }
          .layoutWeight(1)
        }
        .width('100%')
        this.Field(this.s.usernameOptional, '', this.username, (value: string) => {
          this.username = value;
        })
        this.Field(this.s.passwordOptional, '', this.password, (value: string) => {
          this.password = value;
        }, InputType.Password)
        this.Field(this.s.wolMacLabel, 'AA:BB:CC:DD:EE:FF', this.mac, (value: string) => {
          this.mac = value;
        })

        Row() {
          Column() {
            Text('使用 HTTPS')
              .fontSize(14)
              .fontColor(AppColors.TEXT)
            Text('LMS 开启 HTTPS 时再打开')
              .fontSize(10)
              .fontColor(AppColors.TEXT_SECONDARY)
          }
          .alignItems(HorizontalAlign.Start)
          .layoutWeight(1)
          Toggle({ type: ToggleType.Switch, isOn: this.useHttps })
            .selectedColor(AppColors.PRIMARY)
            .onChange((isOn: boolean) => {
              this.useHttps = isOn;
            })
        }
        .width('100%')
        .padding(12)
        .backgroundColor(AppColors.SURFACE)
        .borderRadius(10)

        if (this.errorText.length > 0) {
          Text(this.errorText)
            .fontSize(12)
            .fontColor(AppColors.ERROR)
            .width('100%')
        }

        Row({ space: 8 }) {
          Button(this.busy ? this.s.loading : this.s.testAndSave)
            .fontSize(13)
            .fontColor(AppColors.ON_PRIMARY)
            .backgroundColor(AppColors.PRIMARY)
            .layoutWeight(1)
            .enabled(!this.busy)
            .onClick(() => this.save(true))
          Button(this.s.saveWithoutTest)
            .fontSize(13)
            .fontColor(AppColors.TEXT)
            .backgroundColor(AppColors.SURFACE_HIGH)
            .layoutWeight(1)
            .enabled(!this.busy)
            .onClick(() => this.save(false))
        }
        .width('100%')

        Button(this.s.cancel)
          .fontSize(12)
          .fontColor(AppColors.TEXT_SECONDARY)
          .backgroundColor(Color.Transparent)
          .width('100%')
          .onClick(() => this.pathStack.pop())
      }
      .width('100%')
      .padding(16)
    }
    .width('100%')
    .height('100%')
    .backgroundColor(AppColors.BACKGROUND)
    .scrollBar(BarState.Off)
  }
}
