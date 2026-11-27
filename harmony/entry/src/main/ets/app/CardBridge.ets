import { common } from '@kit.AbilityKit';
import { fileIo } from '@kit.CoreFileKit';
import { formBindingData, formProvider } from '@kit.FormKit';
import { hilog } from '@kit.PerformanceAnalysisKit';

const DOMAIN = 0x0000;

export interface CardSnapshot {
  title: string;
  artist: string;
  album: string;
  playing: string;
  cover: string;
  updated: number;
}

export function emptySnapshot(): CardSnapshot {
  return {
    title: '',
    artist: '',
    album: '',
    playing: 'false',
    cover: '',
    updated: 0
  };
}

/**
 * 主应用与卡片扩展之间通过应用沙箱文件共享「正在播放」快照与卡片指令。
 * 两边各自初始化上下文即可读取同一份文件。
 */
export class CardBridge {
  private static context: common.Context | null = null;

  static init(context: common.Context): void {
    CardBridge.context = context;
  }

  private static path(name: string): string {
    const context = CardBridge.context;
    return context === null ? '' : `${context.filesDir}/${name}`;
  }

  private static writeText(path: string, text: string): void {
    if (path.length === 0) {
      return;
    }
    try {
      const file = fileIo.openSync(path,
        fileIo.OpenMode.READ_WRITE | fileIo.OpenMode.CREATE | fileIo.OpenMode.TRUNC);
      fileIo.writeSync(file.fd, text);
      fileIo.closeSync(file);
    } catch (error) {
      hilog.warn(DOMAIN, 'daphile', 'card write failed: %{public}s', `${error}`);
    }
  }

  private static readText(path: string): string {
    if (path.length === 0) {
      return '';
    }
    try {
      if (!fileIo.accessSync(path)) {
        return '';
      }
      return fileIo.readTextSync(path);
    } catch (error) {
      return '';
    }
  }

  static writeSnapshot(snapshot: CardSnapshot): void {
    try {
      CardBridge.writeText(CardBridge.path('now_playing_card.json'), JSON.stringify(snapshot));
    } catch (error) {
      // 忽略序列化失败
    }
  }

  static readSnapshot(): CardSnapshot {
    const text = CardBridge.readText(CardBridge.path('now_playing_card.json'));
    if (text.length === 0) {
      return emptySnapshot();
    }
    try {
      const parsed = JSON.parse(text) as CardSnapshot;
      const snapshot = emptySnapshot();
      snapshot.title = parsed.title !== undefined ? parsed.title : '';
      snapshot.artist = parsed.artist !== undefined ? parsed.artist : '';
      snapshot.album = parsed.album !== undefined ? parsed.album : '';
      snapshot.playing = parsed.playing !== undefined ? parsed.playing : 'false';
      snapshot.cover = parsed.cover !== undefined ? parsed.cover : '';
      snapshot.updated = parsed.updated !== undefined ? parsed.updated : 0;
      return snapshot;
    } catch (error) {
      return emptySnapshot();
    }
  }

  static formData(): formBindingData.FormBindingData {
    const snapshot = CardBridge.readSnapshot();
    const data: Record<string, string> = {
      'title': snapshot.title.length > 0 ? snapshot.title : 'Daphile Remote',
      'artist': snapshot.artist.length > 0 ? snapshot.artist : snapshot.album,
      'playing': snapshot.playing,
      'cover': snapshot.cover
    };
    return formBindingData.createFormBindingData(data);
  }

  /** 卡片按钮写入待执行指令，主应用轮询时消费。 */
  static writeCommand(command: string): void {
    if (command.length === 0) {
      return;
    }
    CardBridge.writeText(CardBridge.path('card_command.json'), command);
  }

  static takeCommand(): string {
    const path = CardBridge.path('card_command.json');
    const text = CardBridge.readText(path);
    if (text.length > 0) {
      try {
        fileIo.unlinkSync(path);
      } catch (error) {
        // 忽略删除失败
      }
    }
    return text.trim();
  }

  static addFormId(formId: string): void {
    if (formId.length === 0) {
      return;
    }
    const ids = CardBridge.formIds();
    if (ids.indexOf(formId) < 0) {
      ids.push(formId);
    }
    CardBridge.writeText(CardBridge.path('card_form_ids.json'), JSON.stringify(ids));
  }

  static removeFormId(formId: string): void {
    const ids = CardBridge.formIds().filter((value: string) => value !== formId);
    CardBridge.writeText(CardBridge.path('card_form_ids.json'), JSON.stringify(ids));
  }

  static formIds(): string[] {
    const text = CardBridge.readText(CardBridge.path('card_form_ids.json'));
    if (text.length === 0) {
      return [];
    }
    try {
      return JSON.parse(text) as string[];
    } catch (error) {
      return [];
    }
  }

  /** 主应用刷新所有已添加的卡片。 */
  static async notifyForms(): Promise<void> {
    const ids = CardBridge.formIds();
    if (ids.length === 0) {
      return;
    }
    const data = CardBridge.formData();
    for (const formId of ids) {
      try {
        await formProvider.updateForm(formId, data);
      } catch (error) {
        hilog.warn(DOMAIN, 'daphile', 'updateForm failed: %{public}s', `${error}`);
      }
    }
  }
}
