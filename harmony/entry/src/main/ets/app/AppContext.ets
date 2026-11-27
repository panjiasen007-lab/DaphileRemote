import { common } from '@kit.AbilityKit';
import { LmsClient } from '../data/remote/LmsClient';
import { CryptoStore } from '../data/local/CryptoStore';
import { SettingsStore } from '../data/local/SettingsStore';
import { OnlineMetadataService } from '../data/online/OnlineMetadataService';
import { DaphileRepository } from '../data/repo/DaphileRepository';
import { AvSessionController } from '../media/AvSessionController';
import { CardBridge } from './CardBridge';

/** 手工依赖注入，等价于 Android 版的 AppGraph。 */
export class AppContext {
  static instance: AppContext | null = null;

  readonly crypto: CryptoStore;
  readonly settings: SettingsStore;
  readonly client: LmsClient;
  readonly onlineMetadata: OnlineMetadataService;
  readonly repository: DaphileRepository;
  readonly avSession: AvSessionController;

  private constructor(context: common.UIAbilityContext) {
    CardBridge.init(context);
    this.crypto = new CryptoStore();
    this.settings = new SettingsStore(context, this.crypto);
    this.client = new LmsClient();
    this.onlineMetadata = new OnlineMetadataService(context.filesDir);
    this.repository = new DaphileRepository(this.settings, this.client, this.onlineMetadata);
    this.avSession = new AvSessionController(context, this.repository);
    this.avSession.start().catch(() => {
    });
  }

  static init(context: common.UIAbilityContext): AppContext {
    if (AppContext.instance === null) {
      AppContext.instance = new AppContext(context);
    }
    return AppContext.instance;
  }

  static get(): AppContext {
    if (AppContext.instance === null) {
      throw new Error('AppContext 尚未初始化');
    }
    return AppContext.instance;
  }
}
