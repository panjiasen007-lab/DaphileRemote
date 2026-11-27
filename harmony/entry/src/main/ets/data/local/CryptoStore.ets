import { huks } from '@kit.UniversalKeystoreKit';
import { cryptoFramework } from '@kit.CryptoArchitectureKit';
import { util } from '@kit.ArkTS';

/**
 * 服务器密码加密存储。
 * 使用 HUKS 生成不可导出的 AES-256-GCM 密钥，密文格式为 nonce(12B) + cipher，整体 Base64。
 */
export class CryptoStore {
  private static readonly ALIAS: string = 'daphile_server_password_v1';
  private static readonly NONCE_SIZE: number = 12;
  private keyChecked: boolean = false;

  private async ensureKey(): Promise<void> {
    if (this.keyChecked) {
      return;
    }
    const properties: huks.HuksParam[] = [
      { tag: huks.HuksTag.HUKS_TAG_ALGORITHM, value: huks.HuksKeyAlg.HUKS_ALG_AES },
      { tag: huks.HuksTag.HUKS_TAG_KEY_SIZE, value: huks.HuksKeySize.HUKS_AES_KEY_SIZE_256 },
      {
        tag: huks.HuksTag.HUKS_TAG_PURPOSE,
        value: huks.HuksKeyPurpose.HUKS_KEY_PURPOSE_ENCRYPT |
          huks.HuksKeyPurpose.HUKS_KEY_PURPOSE_DECRYPT
      },
      { tag: huks.HuksTag.HUKS_TAG_PADDING, value: huks.HuksKeyPadding.HUKS_PADDING_NONE },
      { tag: huks.HuksTag.HUKS_TAG_BLOCK_MODE, value: huks.HuksCipherMode.HUKS_MODE_GCM }
    ];
    try {
      await huks.generateKeyItem(CryptoStore.ALIAS, { properties: properties });
    } catch (error) {
      // 密钥已存在时忽略；后续加解密会使用已有密钥
    }
    this.keyChecked = true;
  }

  private randomNonce(): Uint8Array {
    const random = cryptoFramework.createRandom();
    return random.generateRandomSync(CryptoStore.NONCE_SIZE).data;
  }

  private baseProperties(purpose: number, nonce: Uint8Array): huks.HuksParam[] {
    return [
      { tag: huks.HuksTag.HUKS_TAG_ALGORITHM, value: huks.HuksKeyAlg.HUKS_ALG_AES },
      { tag: huks.HuksTag.HUKS_TAG_PURPOSE, value: purpose },
      { tag: huks.HuksTag.HUKS_TAG_PADDING, value: huks.HuksKeyPadding.HUKS_PADDING_NONE },
      { tag: huks.HuksTag.HUKS_TAG_BLOCK_MODE, value: huks.HuksCipherMode.HUKS_MODE_GCM },
      { tag: huks.HuksTag.HUKS_TAG_NONCE, value: nonce }
    ];
  }

  async encrypt(plain: string): Promise<string> {
    if (plain.length === 0) {
      return '';
    }
    await this.ensureKey();
    const nonce = this.randomNonce();
    const input = new util.TextEncoder().encodeInto(plain);
    const options: huks.HuksOptions = {
      properties: this.baseProperties(huks.HuksKeyPurpose.HUKS_KEY_PURPOSE_ENCRYPT, nonce),
      inData: input
    };
    const session = await huks.initSession(CryptoStore.ALIAS, options);
    const result = await huks.finishSession(session.handle, options);
    const cipher = result.outData;
    if (cipher === undefined) {
      throw new Error('密码加密失败');
    }
    const combined = new Uint8Array(nonce.length + cipher.length);
    combined.set(nonce, 0);
    combined.set(cipher, nonce.length);
    return new util.Base64Helper().encodeToStringSync(combined);
  }

  async decrypt(payload: string): Promise<string> {
    if (payload.length === 0) {
      return '';
    }
    await this.ensureKey();
    let combined: Uint8Array;
    try {
      combined = new util.Base64Helper().decodeSync(payload);
    } catch (error) {
      return '';
    }
    if (combined.length <= CryptoStore.NONCE_SIZE) {
      return '';
    }
    const nonce = combined.slice(0, CryptoStore.NONCE_SIZE);
    const cipher = combined.slice(CryptoStore.NONCE_SIZE);
    const options: huks.HuksOptions = {
      properties: this.baseProperties(huks.HuksKeyPurpose.HUKS_KEY_PURPOSE_DECRYPT, nonce),
      inData: cipher
    };
    try {
      const session = await huks.initSession(CryptoStore.ALIAS, options);
      const result = await huks.finishSession(session.handle, options);
      const plain = result.outData;
      if (plain === undefined) {
        return '';
      }
      return new util.TextDecoder().decodeWithStream(plain);
    } catch (error) {
      return '';
    }
  }
}
