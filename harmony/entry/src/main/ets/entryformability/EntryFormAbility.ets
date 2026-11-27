import { Want } from '@kit.AbilityKit';
import { FormExtensionAbility, formBindingData, formProvider } from '@kit.FormKit';
import { CardBridge } from '../app/CardBridge';

const FORM_IDENTITY_KEY = 'ohos.extra.param.key.form_identity';

export default class EntryFormAbility extends FormExtensionAbility {
  onAddForm(want: Want): formBindingData.FormBindingData {
    CardBridge.init(this.context);
    const params = want.parameters;
    if (params !== undefined) {
      const formId = params[FORM_IDENTITY_KEY] as string;
      if (formId !== undefined && formId.length > 0) {
        CardBridge.addFormId(formId);
      }
    }
    return CardBridge.formData();
  }

  onUpdateForm(formId: string, wantParams?: Record<string, Object>): void {
    CardBridge.init(this.context);
    CardBridge.addFormId(formId);
    formProvider.updateForm(formId, CardBridge.formData(), () => {
    });
  }

  onFormEvent(formId: string, message: string): void {
    CardBridge.init(this.context);
    try {
      const params = JSON.parse(message) as Record<string, string>;
      const command = params['command'];
      if (command !== undefined) {
        CardBridge.writeCommand(command);
      }
    } catch (error) {
      // 忽略无法解析的消息
    }
  }

  onRemoveForm(formId: string): void {
    CardBridge.init(this.context);
    CardBridge.removeFormId(formId);
  }
}
