import {
  ThemeBackdropKind, ThemeCoverFrame, ThemeRuntime, ThemeSceneKind
} from '../theme/ThemeRuntime';
import { AppColors } from '../theme/Theme';
import { CoverArt } from '../components/Common';

function mod(value: number, base: number): number {
  return ((value % base) + base) % base;
}

/**
 * 旋转黑胶：封面圆形裁切，外圈唱片纹路与高光，中心轴孔。
 */
@ComponentV2
export struct VinylCover {
  @Param coverUrl: string = '';
  @Param discSize: number = 196;
  @Param playing: boolean = false;
  @Param accent: string = '#38D6C4';
  @Param coverFrame: ThemeCoverFrame = ThemeCoverFrame.HALO_RING;
  @Param metal: string = '#D4AF37';

  @Local angle: number = 0;
  private timer: number = -1;
  private settings: RenderingContextSettings = new RenderingContextSettings(true);
  private ctx: CanvasRenderingContext2D = new CanvasRenderingContext2D(this.settings);
  private frameCtx: CanvasRenderingContext2D = new CanvasRenderingContext2D(this.settings);

  aboutToAppear(): void {
    if (this.timer < 0) {
      this.timer = setInterval(() => {
        if (this.playing) {
          this.angle = (this.angle + 1.4) % 360;
        }
      }, 100);
    }
  }

  aboutToDisappear(): void {
    if (this.timer >= 0) {
      clearInterval(this.timer);
      this.timer = -1;
    }
  }

  private drawDisc(): void {
    const w = this.ctx.width;
    const h = this.ctx.height;
    const r = Math.min(w, h) / 2;
    const cx = w / 2;
    const cy = h / 2;
    this.ctx.clearRect(0, 0, w, h);

    const gradient = this.ctx.createRadialGradient(cx, cy, r * 0.1, cx, cy, r);
    gradient.addColorStop(0, '#232529');
    gradient.addColorStop(1, '#08090B');
    this.ctx.fillStyle = gradient;
    this.ctx.beginPath();
    this.ctx.arc(cx, cy, r, 0, Math.PI * 2);
    this.ctx.fill();

    this.ctx.globalAlpha = 0.05;
    this.ctx.strokeStyle = '#FFFFFF';
    this.ctx.lineWidth = 1;
    for (let index = 1; index <= 26; index++) {
      const groove = r * (0.35 + 0.62 * index / 26);
      this.ctx.beginPath();
      this.ctx.arc(cx, cy, groove, 0, Math.PI * 2);
      this.ctx.stroke();
    }
    this.ctx.globalAlpha = 0.1;
    this.ctx.lineWidth = r * 0.16;
    this.ctx.beginPath();
    this.ctx.arc(cx, cy, r * 0.78, -0.5, 1.2);
    this.ctx.stroke();
    this.ctx.globalAlpha = 1;
  }

  private drawFrame(): void {
    const w = this.frameCtx.width;
    const h = this.frameCtx.height;
    if (w <= 0 || h <= 0) {
      return;
    }
    const r = Math.min(w, h) / 2 - 3;
    const cx = w / 2;
    const cy = h / 2;
    this.frameCtx.clearRect(0, 0, w, h);
    this.frameCtx.strokeStyle = this.metal;
    this.frameCtx.fillStyle = this.metal;
    this.frameCtx.lineWidth = 1.4;
    this.frameCtx.globalAlpha = 0.55;
    switch (this.coverFrame) {
      case ThemeCoverFrame.NONE:
        return;
      case ThemeCoverFrame.LATTICE_RING:
        this.frameCtx.beginPath();
        this.frameCtx.arc(cx, cy, r, 0, Math.PI * 2);
        this.frameCtx.stroke();
        for (let i = 0; i < 12; i++) {
          const angle = i * Math.PI / 6;
          this.frameCtx.strokeRect(
            cx + Math.cos(angle) * r - 3,
            cy + Math.sin(angle) * r - 3,
            6, 6);
        }
        break;
      case ThemeCoverFrame.WINDMILL:
        for (let i = 0; i < 4; i++) {
          const angle = i * Math.PI / 2 + this.angle * Math.PI / 180;
          this.frameCtx.beginPath();
          this.frameCtx.moveTo(cx, cy);
          this.frameCtx.lineTo(cx + Math.cos(angle) * r, cy + Math.sin(angle) * r);
          this.frameCtx.stroke();
          this.frameCtx.beginPath();
          this.frameCtx.ellipse(cx + Math.cos(angle) * r * 0.7,
            cy + Math.sin(angle) * r * 0.7, 9, 4, angle, 0, Math.PI * 2);
          this.frameCtx.fill();
        }
        break;
      case ThemeCoverFrame.BLOSSOM_WINDOW:
      case ThemeCoverFrame.MAPLE_WREATH:
      case ThemeCoverFrame.BAMBOO_LEAF:
        this.frameCtx.beginPath();
        this.frameCtx.arc(cx, cy, r, 0, Math.PI * 2);
        this.frameCtx.stroke();
        for (let i = 0; i < 10; i++) {
          const angle = i * Math.PI / 5;
          this.frameCtx.beginPath();
          this.frameCtx.ellipse(cx + Math.cos(angle) * r,
            cy + Math.sin(angle) * r, 8, 3.5, angle, 0, Math.PI * 2);
          this.frameCtx.fill();
        }
        break;
      case ThemeCoverFrame.FROST:
      case ThemeCoverFrame.ICE_CRYSTAL:
        this.frameCtx.beginPath();
        this.frameCtx.arc(cx, cy, r, 0, Math.PI * 2);
        this.frameCtx.stroke();
        for (let i = 0; i < 12; i++) {
          const angle = i * Math.PI / 6;
          this.frameCtx.beginPath();
          this.frameCtx.moveTo(cx + Math.cos(angle) * (r - 5), cy + Math.sin(angle) * (r - 5));
          this.frameCtx.lineTo(cx + Math.cos(angle) * (r + 5), cy + Math.sin(angle) * (r + 5));
          this.frameCtx.stroke();
        }
        break;
      case ThemeCoverFrame.MOON_PHASE:
        this.frameCtx.beginPath();
        this.frameCtx.arc(cx, cy, r, 0, Math.PI * 2);
        this.frameCtx.stroke();
        this.frameCtx.beginPath();
        this.frameCtx.arc(cx, cy - r * 0.72, r * 0.22, 0, Math.PI * 2);
        this.frameCtx.stroke();
        break;
      case ThemeCoverFrame.HALO_RING:
        this.frameCtx.globalAlpha = 0.25;
        this.frameCtx.lineWidth = 4;
        this.frameCtx.beginPath();
        this.frameCtx.arc(cx, cy, r, 0, Math.PI * 2);
        this.frameCtx.stroke();
        this.frameCtx.globalAlpha = 0.7;
        this.frameCtx.lineWidth = 1.2;
        this.frameCtx.beginPath();
        this.frameCtx.arc(cx, cy, r + 1, 0, Math.PI * 2);
        this.frameCtx.stroke();
        break;
      case ThemeCoverFrame.THIN_FRAME:
        this.frameCtx.globalAlpha = 0.35;
        this.frameCtx.beginPath();
        this.frameCtx.arc(cx, cy, r, 0, Math.PI * 2);
        this.frameCtx.stroke();
        break;
      case ThemeCoverFrame.BAMBOO_RING:
        this.frameCtx.beginPath();
        this.frameCtx.arc(cx, cy, r, 0, Math.PI * 2);
        this.frameCtx.stroke();
        for (let i = 0; i < 8; i++) {
          const angle = i * Math.PI / 4;
          this.frameCtx.beginPath();
          this.frameCtx.moveTo(cx + Math.cos(angle) * (r - 4), cy + Math.sin(angle) * (r - 4));
          this.frameCtx.lineTo(cx + Math.cos(angle) * (r + 4), cy + Math.sin(angle) * (r + 4));
          this.frameCtx.stroke();
        }
        break;
      case ThemeCoverFrame.WAVE_CREST:
        for (let i = 0; i < 3; i++) {
          this.frameCtx.beginPath();
          this.frameCtx.arc(cx, cy + r * 0.55 + i * 4, r * 0.8, 0.2, Math.PI - 0.2);
          this.frameCtx.stroke();
        }
        break;
      case ThemeCoverFrame.BAGUA:
        this.frameCtx.beginPath();
        this.frameCtx.arc(cx, cy, r, 0, Math.PI * 2);
        this.frameCtx.stroke();
        for (let i = 0; i < 8; i++) {
          const angle = i * Math.PI / 4;
          const px = cx + Math.cos(angle) * (r - 9);
          const py = cy + Math.sin(angle) * (r - 9);
          this.frameCtx.beginPath();
          this.frameCtx.moveTo(px - 4, py);
          this.frameCtx.lineTo(px + 4, py);
          this.frameCtx.moveTo(px - 4, py + 3);
          this.frameCtx.lineTo(px + 1, py + 3);
          this.frameCtx.stroke();
        }
        break;
      default:
        break;
    }
    this.frameCtx.globalAlpha = 1;
  }

  build() {
    Stack() {
      Stack() {
        Canvas(this.ctx)
          .width(this.discSize)
          .height(this.discSize)
          .onReady(() => this.drawDisc())

        CoverArt({
          url: this.coverUrl,
          coverSize: this.discSize * 0.72,
          radius: this.discSize * 0.36
        })

        Stack() {
          Column()
            .width(this.discSize * 0.075)
            .height(this.discSize * 0.075)
            .borderRadius(this.discSize * 0.04)
            .backgroundColor(this.accent)
          Column()
            .width(this.discSize * 0.03)
            .height(this.discSize * 0.03)
            .borderRadius(this.discSize * 0.015)
            .backgroundColor(AppColors.BACKGROUND)
        }
        .width(this.discSize * 0.075)
        .height(this.discSize * 0.075)
      }
      .width(this.discSize)
      .height(this.discSize)
      .rotate({ angle: this.angle })

      Canvas(this.frameCtx)
        .width(this.discSize)
        .height(this.discSize)
        .onReady(() => this.drawFrame())
    }
    .width(this.discSize)
    .height(this.discSize)
  }
}

/** 14 套主题背景。低透明度绘制，避免干扰前景内容。 */
@ComponentV2
export struct ThemeBackdropView {
  @Param kind: ThemeBackdropKind = ThemeBackdropKind.STARFIELD;
  @Param accent: string = '#38D6C4';
  @Param active: boolean = false;
  @Param progress: number = 0;

  private timer: number = -1;
  private phase: number = 0;
  private settings: RenderingContextSettings = new RenderingContextSettings(true);
  private ctx: CanvasRenderingContext2D = new CanvasRenderingContext2D(this.settings);
  private ready: boolean = false;

  aboutToAppear(): void {
    if (this.timer < 0) {
      this.timer = setInterval(() => {
        if (this.active) {
          this.phase += 0.04;
          this.draw();
        }
      }, 90);
    }
  }

  aboutToDisappear(): void {
    if (this.timer >= 0) {
      clearInterval(this.timer);
      this.timer = -1;
    }
  }

  private draw(): void {
    if (!this.ready) {
      return;
    }
    const w = this.ctx.width;
    const h = this.ctx.height;
    if (w <= 0 || h <= 0) {
      return;
    }
    this.ctx.clearRect(0, 0, w, h);
    this.ctx.strokeStyle = this.accent;
    this.ctx.fillStyle = this.accent;
    switch (this.kind) {
      case ThemeBackdropKind.PULSE:
        this.drawPulse(w, h);
        break;
      case ThemeBackdropKind.LATTICE:
        this.drawLattice(w, h);
        break;
      case ThemeBackdropKind.WIND_FIELD:
        this.drawWind(w, h);
        break;
      case ThemeBackdropKind.BLOSSOM:
        this.drawBlossom(w, h);
        break;
      case ThemeBackdropKind.SNOWFALL:
        this.drawSnowfall(w, h);
        break;
      case ThemeBackdropKind.MOON_SKY:
        this.drawMoon(w, h);
        break;
      case ThemeBackdropKind.STARFIELD:
        this.drawStars(w, h);
        break;
      case ThemeBackdropKind.PAPER_GRID:
        this.drawPaperGrid(w, h);
        break;
      case ThemeBackdropKind.WILLOW_BREEZE:
        this.drawWillow(w, h);
        break;
      case ThemeBackdropKind.OCEAN_WAVES:
        this.drawWaves(w, h);
        break;
      case ThemeBackdropKind.MAPLE_FALL:
        this.drawLeaves(w, h);
        break;
      case ThemeBackdropKind.FROST_BLOOM:
        this.drawFrost(w, h);
        break;
      case ThemeBackdropKind.YIN_YANG:
        this.drawYinYang(w, h);
        break;
      case ThemeBackdropKind.BAMBOO_SHADE:
        this.drawBamboo(w, h);
        break;
      default:
        break;
    }
    this.ctx.globalAlpha = 1;
  }

  private drawPulse(w: number, h: number): void {
    const cx = w / 2;
    const cy = h * 0.42;
    for (let i = 0; i < 3; i++) {
      const radius = Math.max(w, h) * (0.18 + 0.12 * i) + Math.sin(this.phase + i) * 8;
      this.ctx.globalAlpha = 0.05;
      this.ctx.lineWidth = 1.4;
      this.ctx.beginPath();
      this.ctx.arc(cx, cy, radius, 0, Math.PI * 2);
      this.ctx.stroke();
    }
  }

  private drawLattice(w: number, h: number): void {
    const size = 54;
    this.ctx.globalAlpha = 0.06;
    this.ctx.lineWidth = 1.2;
    for (let x = 0; x < w + size; x += size) {
      this.ctx.beginPath();
      this.ctx.moveTo(x, 0);
      this.ctx.lineTo(x, h);
      this.ctx.stroke();
    }
    for (let y = 0; y < h + size; y += size) {
      this.ctx.beginPath();
      this.ctx.moveTo(0, y);
      this.ctx.lineTo(w, y);
      this.ctx.stroke();
    }
    this.ctx.globalAlpha = 0.08;
    for (let y = size; y < h; y += size * 2) {
      for (let x = size; x < w; x += size * 2) {
        this.ctx.strokeRect(x - 13, y - 13, 26, 26);
      }
    }
  }

  private drawWind(w: number, h: number): void {
    this.ctx.globalAlpha = 0.075;
    this.ctx.lineWidth = 1.6;
    for (let i = 0; i < 7; i++) {
      const baseY = h * (0.1 + i * 0.13);
      this.ctx.beginPath();
      for (let x = 0; x <= w; x += 12) {
        const y = baseY + Math.sin(x / 90 + this.phase + i) * (10 + i * 2);
        if (x === 0) {
          this.ctx.moveTo(x, y);
        } else {
          this.ctx.lineTo(x, y);
        }
      }
      this.ctx.stroke();
    }
  }

  private drawBlossom(w: number, h: number): void {
    this.ctx.globalAlpha = 0.09;
    for (let i = 0; i < 18; i++) {
      const x = ((i * 137) % w) + Math.sin(this.phase + i) * 12;
      const y = ((i * 211) % h) + Math.cos(this.phase + i) * 10;
      this.ctx.beginPath();
      this.ctx.ellipse(x, y, 6, 3, i, 0, Math.PI * 2);
      this.ctx.fill();
    }
  }

  private drawSnowfall(w: number, h: number): void {
    this.ctx.globalAlpha = 0.14;
    for (let i = 0; i < 36; i++) {
      const x = (i * 97) % w;
      const y = mod(i * 151 + this.phase * 26, h);
      const radius = 1 + (i % 3);
      this.ctx.beginPath();
      this.ctx.arc(x, y, radius, 0, Math.PI * 2);
      this.ctx.fill();
    }
  }

  private drawMoon(w: number, h: number): void {
    this.ctx.globalAlpha = 0.12;
    this.ctx.beginPath();
    this.ctx.arc(w * 0.76, h * 0.16, Math.min(w, h) * 0.11, 0, Math.PI * 2);
    this.ctx.fill();
    this.ctx.globalAlpha = 0.16;
    for (let i = 0; i < 22; i++) {
      const x = (i * 173) % w;
      const y = (i * 89) % (h * 0.6);
      this.ctx.beginPath();
      this.ctx.arc(x, y, 1.2, 0, Math.PI * 2);
      this.ctx.fill();
    }
  }

  private drawStars(w: number, h: number): void {
    for (let i = 0; i < 52; i++) {
      const x = (i * 149) % w;
      const y = (i * 97) % h;
      this.ctx.globalAlpha = 0.05 + (i % 4) * 0.02;
      this.ctx.beginPath();
      this.ctx.arc(x, y, 1 + (i % 2), 0, Math.PI * 2);
      this.ctx.fill();
    }
  }

  private drawPaperGrid(w: number, h: number): void {
    const step = 34;
    this.ctx.globalAlpha = 0.08;
    this.ctx.lineWidth = 0.8;
    for (let x = 0; x < w; x += step) {
      this.ctx.beginPath();
      this.ctx.moveTo(x, 0);
      this.ctx.lineTo(x, h);
      this.ctx.stroke();
    }
    for (let y = 0; y < h; y += step) {
      this.ctx.beginPath();
      this.ctx.moveTo(0, y);
      this.ctx.lineTo(w, y);
      this.ctx.stroke();
    }
  }

  private drawWillow(w: number, h: number): void {
    this.ctx.globalAlpha = 0.08;
    this.ctx.lineWidth = 1.4;
    for (let i = 0; i < 8; i++) {
      const x = w * (0.08 + i * 0.12);
      this.ctx.beginPath();
      this.ctx.moveTo(x, 0);
      this.ctx.quadraticCurveTo(
        x + Math.sin(this.phase + i) * 30,
        h * 0.45,
        x + Math.sin(this.phase + i) * 14,
        h * 0.9
      );
      this.ctx.stroke();
      this.ctx.beginPath();
      this.ctx.ellipse(x + 10, h * 0.35, 4, 11, 0.6, 0, Math.PI * 2);
      this.ctx.fill();
    }
  }

  private drawWaves(w: number, h: number): void {
    this.ctx.globalAlpha = 0.09;
    this.ctx.lineWidth = 2;
    for (let i = 0; i < 4; i++) {
      const baseY = h * (0.62 + i * 0.09);
      this.ctx.beginPath();
      for (let x = 0; x <= w; x += 10) {
        const y = baseY + Math.sin(x / 46 + this.phase + i * 0.8) * 10;
        if (x === 0) {
          this.ctx.moveTo(x, y);
        } else {
          this.ctx.lineTo(x, y);
        }
      }
      this.ctx.stroke();
    }
  }

  private drawLeaves(w: number, h: number): void {
    this.ctx.globalAlpha = 0.12;
    for (let i = 0; i < 20; i++) {
      const x = ((i * 131) % w) + Math.sin(this.phase + i) * 16;
      const y = mod(i * 179 + this.phase * 22, h);
      this.ctx.beginPath();
      this.ctx.ellipse(x, y, 7, 3.2, this.phase + i, 0, Math.PI * 2);
      this.ctx.fill();
    }
  }

  private drawFrost(w: number, h: number): void {
    this.ctx.globalAlpha = 0.08;
    this.ctx.lineWidth = 1;
    for (let i = 0; i < 10; i++) {
      const cx = (i % 2 === 0) ? w * 0.08 : w * 0.92;
      const cy = h * (0.15 + (i % 5) * 0.16);
      for (let arm = 0; arm < 6; arm++) {
        const angle = arm * Math.PI / 3 + this.phase * 0.1;
        this.ctx.beginPath();
        this.ctx.moveTo(cx, cy);
        this.ctx.lineTo(cx + Math.cos(angle) * 26, cy + Math.sin(angle) * 26);
        this.ctx.stroke();
      }
    }
  }

  private drawYinYang(w: number, h: number): void {
    const r = Math.min(w, h) * 0.28;
    const cx = w / 2;
    const cy = h * 0.5;
    this.ctx.globalAlpha = 0.06;
    this.ctx.lineWidth = 1.5;
    this.ctx.beginPath();
    this.ctx.arc(cx, cy, r, 0, Math.PI * 2);
    this.ctx.stroke();
    this.ctx.beginPath();
    this.ctx.arc(cx, cy - r / 2, r / 2, Math.PI, 0);
    this.ctx.stroke();
    this.ctx.beginPath();
    this.ctx.arc(cx, cy + r / 2, r / 2, 0, Math.PI);
    this.ctx.stroke();
    this.ctx.globalAlpha = 0.09;
    this.ctx.beginPath();
    this.ctx.arc(cx, cy, r, -Math.PI / 2, Math.PI / 2);
    this.ctx.fill();
  }

  private drawBamboo(w: number, h: number): void {
    this.ctx.globalAlpha = 0.08;
    this.ctx.lineWidth = 4;
    for (let i = 0; i < 4; i++) {
      const x = w * (0.12 + i * 0.24);
      this.ctx.beginPath();
      this.ctx.moveTo(x, 0);
      this.ctx.lineTo(x + Math.sin(this.phase + i) * 8, h);
      this.ctx.stroke();
      for (let node = 1; node < 6; node++) {
        this.ctx.beginPath();
        this.ctx.ellipse(x + 18, h * node / 6, 12, 3, -0.5, 0, Math.PI * 2);
        this.ctx.fill();
      }
    }
  }

  build() {
    Canvas(this.ctx)
      .width('100%')
      .height('100%')
      .onReady(() => {
        this.ready = true;
        this.draw();
      })
  }
}

/** 14 套播放场景：随播放进度移动的小动画。 */
@ComponentV2
export struct ThemeSceneView {
  @Param kind: ThemeSceneKind = ThemeSceneKind.SOUND_WAVE;
  @Param accent: string = '#38D6C4';
  @Param playing: boolean = false;
  @Param progress: number = 0;

  private timer: number = -1;
  private phase: number = 0;
  private ready: boolean = false;
  private settings: RenderingContextSettings = new RenderingContextSettings(true);
  private ctx: CanvasRenderingContext2D = new CanvasRenderingContext2D(this.settings);

  aboutToAppear(): void {
    if (this.timer < 0) {
      this.timer = setInterval(() => {
        if (this.playing) {
          this.phase += 0.08;
          this.draw();
        }
      }, 60);
    }
  }

  aboutToDisappear(): void {
    if (this.timer >= 0) {
      clearInterval(this.timer);
      this.timer = -1;
    }
  }

  private draw(): void {
    if (!this.ready) {
      return;
    }
    const w = this.ctx.width;
    const h = this.ctx.height;
    if (w <= 0 || h <= 0) {
      return;
    }
    this.ctx.clearRect(0, 0, w, h);
    this.ctx.strokeStyle = this.accent;
    this.ctx.fillStyle = this.accent;
    this.ctx.lineWidth = 2;
    this.ctx.globalAlpha = 0.75;
    const x = w * (0.08 + 0.84 * this.progress);
    const mid = h * 0.55;
    switch (this.kind) {
      case ThemeSceneKind.STICK_FIGHT:
        this.drawStickFight(w, h, x);
        break;
      case ThemeSceneKind.SHADOW_PUPPET:
        this.drawShadow(w, h, x);
        break;
      case ThemeSceneKind.KITES:
        this.drawKite(w, h, x);
        break;
      case ThemeSceneKind.BUTTERFLIES:
        this.drawButterfly(w, h, x);
        break;
      case ThemeSceneKind.SNOW_CHASE:
        this.drawSkater(w, h, x);
        break;
      case ThemeSceneKind.MOON_SWORD:
        this.drawSword(w, h, x);
        break;
      case ThemeSceneKind.SOUND_WAVE:
        this.drawWave(w, h);
        break;
      case ThemeSceneKind.PAPER_PLANE:
        this.drawPlane(w, h, x);
        break;
      case ThemeSceneKind.SWALLOWS:
        this.drawSwallows(w, h, x);
        break;
      case ThemeSceneKind.SAILBOAT:
        this.drawSailboat(w, h, x);
        break;
      case ThemeSceneKind.GEESE:
        this.drawGeese(w, h, x);
        break;
      case ThemeSceneKind.SKATER:
        this.drawSkater(w, h, x);
        break;
      case ThemeSceneKind.PUSH_HANDS:
        this.drawPushHands(w, h, x);
        break;
      case ThemeSceneKind.PANDA_BAMBOO:
        this.drawPanda(w, h, x);
        break;
      default:
        this.drawWave(w, h);
        break;
    }
    this.ctx.globalAlpha = 1;
  }

  private drawStickFight(w: number, h: number, x: number): void {
    const y = h * 0.68;
    const swing = Math.sin(this.phase * 2) * 8;
    this.ctx.beginPath();
    this.ctx.arc(x - 16, y - 26, 6, 0, Math.PI * 2);
    this.ctx.moveTo(x - 16, y - 20);
    this.ctx.lineTo(x - 16, y - 2);
    this.ctx.moveTo(x - 24, y - 14);
    this.ctx.lineTo(x - 6 + swing, y - 18);
    this.ctx.moveTo(x - 16, y - 2);
    this.ctx.lineTo(x - 24, y + 16);
    this.ctx.moveTo(x - 16, y - 2);
    this.ctx.lineTo(x - 8, y + 16);
    this.ctx.stroke();
    this.ctx.beginPath();
    this.ctx.arc(x + 16, y - 24, 6, 0, Math.PI * 2);
    this.ctx.moveTo(x + 16, y - 18);
    this.ctx.lineTo(x + 16, y);
    this.ctx.moveTo(x + 24, y - 12);
    this.ctx.lineTo(x + 8 - swing, y - 16);
    this.ctx.moveTo(x + 16, y);
    this.ctx.lineTo(x + 8, y + 16);
    this.ctx.moveTo(x + 16, y);
    this.ctx.lineTo(x + 26, y + 16);
    this.ctx.stroke();
  }

  private drawShadow(w: number, h: number, x: number): void {
    const y = h * 0.7;
    this.ctx.globalAlpha = 0.4;
    this.ctx.beginPath();
    this.ctx.ellipse(x, y + 18, 42, 7, 0, 0, Math.PI * 2);
    this.ctx.fill();
    this.ctx.globalAlpha = 0.8;
    this.ctx.beginPath();
    this.ctx.arc(x, y - 22, 7, 0, Math.PI * 2);
    this.ctx.moveTo(x, y - 15);
    this.ctx.lineTo(x, y + 8);
    this.ctx.moveTo(x - 20, y - 8 + Math.sin(this.phase) * 6);
    this.ctx.lineTo(x, y - 12);
    this.ctx.lineTo(x + 20, y - 8 - Math.sin(this.phase) * 6);
    this.ctx.moveTo(x, y + 8);
    this.ctx.lineTo(x - 14, y + 22);
    this.ctx.moveTo(x, y + 8);
    this.ctx.lineTo(x + 14, y + 22);
    this.ctx.stroke();
  }

  private drawKite(w: number, h: number, x: number): void {
    const y = h * 0.36 + Math.sin(this.phase) * 8;
    this.ctx.beginPath();
    this.ctx.moveTo(x, y - 16);
    this.ctx.lineTo(x + 14, y);
    this.ctx.lineTo(x, y + 18);
    this.ctx.lineTo(x - 14, y);
    this.ctx.closePath();
    this.ctx.stroke();
    this.ctx.beginPath();
    this.ctx.moveTo(x, y + 18);
    this.ctx.quadraticCurveTo(x + 20 + Math.sin(this.phase) * 10, y + 34, x + 34, y + 46);
    this.ctx.stroke();
  }

  private drawButterfly(w: number, h: number, x: number): void {
    const y = h * 0.46;
    const flap = 0.5 + Math.abs(Math.sin(this.phase)) * 0.8;
    this.ctx.beginPath();
    this.ctx.ellipse(x - 8, y, 10, 5 * flap, -0.5, 0, Math.PI * 2);
    this.ctx.ellipse(x + 8, y, 10, 5 * flap, 0.5, 0, Math.PI * 2);
    this.ctx.fill();
    this.ctx.beginPath();
    this.ctx.moveTo(x, y - 8);
    this.ctx.lineTo(x, y + 8);
    this.ctx.stroke();
  }

  private drawSkater(w: number, h: number, x: number): void {
    const y = h * 0.68;
    this.ctx.beginPath();
    this.ctx.arc(x, y - 24, 6, 0, Math.PI * 2);
    this.ctx.moveTo(x, y - 18);
    this.ctx.lineTo(x, y);
    this.ctx.moveTo(x - 16, y - 10);
    this.ctx.lineTo(x + 14, y - 16);
    this.ctx.moveTo(x, y);
    this.ctx.lineTo(x - 16, y + 16);
    this.ctx.moveTo(x, y);
    this.ctx.lineTo(x + 16, y + 16);
    this.ctx.stroke();
    this.ctx.globalAlpha = 0.35;
    for (let i = 1; i <= 3; i++) {
      this.ctx.beginPath();
      this.ctx.moveTo(x - 30 - i * 12, y + 16);
      this.ctx.lineTo(x - 38 - i * 12, y + 16);
      this.ctx.stroke();
    }
  }

  private drawSword(w: number, h: number, x: number): void {
    const y = h * 0.66;
    this.ctx.beginPath();
    this.ctx.arc(x, y - 24, 6, 0, Math.PI * 2);
    this.ctx.moveTo(x, y - 18);
    this.ctx.lineTo(x, y + 2);
    this.ctx.moveTo(x, y + 2);
    this.ctx.lineTo(x - 14, y + 18);
    this.ctx.moveTo(x, y + 2);
    this.ctx.lineTo(x + 16, y + 18);
    this.ctx.stroke();
    this.ctx.beginPath();
    this.ctx.moveTo(x + 8, y - 12);
    this.ctx.lineTo(x + 34, y - 30);
    this.ctx.stroke();
    this.ctx.globalAlpha = 0.3;
    this.ctx.beginPath();
    this.ctx.arc(x + 18, y - 18, 22, -1.2, 0.6);
    this.ctx.stroke();
  }

  private drawWave(w: number, h: number): void {
    const bars = 28;
    for (let i = 0; i < bars; i++) {
      const bh = 6 + Math.abs(Math.sin(this.phase + i * 0.55)) * (h * 0.55);
      const x = w * (0.04 + i * 0.033);
      this.ctx.globalAlpha = 0.35 + (i % 4) * 0.1;
      this.ctx.fillRect(x, h * 0.75 - bh, 3, bh);
    }
  }

  private drawPlane(w: number, h: number, x: number): void {
    const y = h * 0.42 - Math.sin(this.phase) * 6;
    this.ctx.beginPath();
    this.ctx.moveTo(x, y);
    this.ctx.lineTo(x - 18, y + 10);
    this.ctx.lineTo(x - 6, y + 12);
    this.ctx.lineTo(x - 10, y + 22);
    this.ctx.lineTo(x - 2, y + 14);
    this.ctx.lineTo(x + 10, y + 16);
    this.ctx.closePath();
    this.ctx.stroke();
    this.ctx.globalAlpha = 0.3;
    this.ctx.beginPath();
    this.ctx.moveTo(x - 18, y + 10);
    this.ctx.lineTo(x - 46, y + 22);
    this.ctx.stroke();
  }

  private drawSwallows(w: number, h: number, x: number): void {
    const y = h * 0.4;
    for (let i = 0; i < 3; i++) {
      const bx = x - i * 30;
      const by = y + Math.sin(this.phase + i) * 5 + i * 8;
      this.ctx.beginPath();
      this.ctx.arc(bx - 6, by, 7, -0.9, 0.1);
      this.ctx.arc(bx + 6, by, 7, Math.PI - 0.1, Math.PI + 0.9);
      this.ctx.stroke();
    }
  }

  private drawSailboat(w: number, h: number, x: number): void {
    const y = h * 0.68;
    this.ctx.beginPath();
    this.ctx.moveTo(x - 20, y);
    this.ctx.lineTo(x + 20, y);
    this.ctx.lineTo(x + 12, y + 10);
    this.ctx.lineTo(x - 12, y + 10);
    this.ctx.closePath();
    this.ctx.stroke();
    this.ctx.beginPath();
    this.ctx.moveTo(x, y - 30);
    this.ctx.lineTo(x, y);
    this.ctx.lineTo(x + 16, y);
    this.ctx.closePath();
    this.ctx.stroke();
    this.ctx.globalAlpha = 0.3;
    this.ctx.beginPath();
    for (let dx = -30; dx <= 30; dx += 6) {
      this.ctx.moveTo(x + dx, y + 16 + Math.sin(this.phase + dx / 12) * 3);
      this.ctx.lineTo(x + dx + 4, y + 16 + Math.sin(this.phase + dx / 12) * 3);
    }
    this.ctx.stroke();
  }

  private drawGeese(w: number, h: number, x: number): void {
    const y = h * 0.4;
    for (let i = 0; i < 4; i++) {
      const gx = x - i * 18;
      const gy = y + Math.abs(i - 1.5) * 14;
      this.ctx.beginPath();
      this.ctx.arc(gx - 5, gy, 5, -1, 0.1);
      this.ctx.arc(gx + 5, gy, 5, Math.PI - 0.1, Math.PI + 1);
      this.ctx.stroke();
    }
  }

  private drawPushHands(w: number, h: number, x: number): void {
    const y = h * 0.66;
    const offset = Math.sin(this.phase) * 6;
    this.ctx.beginPath();
    this.ctx.arc(x - 18, y - 24, 5, 0, Math.PI * 2);
    this.ctx.moveTo(x - 18, y - 18);
    this.ctx.lineTo(x - 18, y + 2);
    this.ctx.moveTo(x - 18, y - 8);
    this.ctx.lineTo(x - 2 - offset, y - 4);
    this.ctx.moveTo(x - 18, y + 2);
    this.ctx.lineTo(x - 26, y + 18);
    this.ctx.moveTo(x - 18, y + 2);
    this.ctx.lineTo(x - 10, y + 18);
    this.ctx.stroke();
    this.ctx.beginPath();
    this.ctx.arc(x + 18, y - 24, 5, 0, Math.PI * 2);
    this.ctx.moveTo(x + 18, y - 18);
    this.ctx.lineTo(x + 18, y + 2);
    this.ctx.moveTo(x + 18, y - 8);
    this.ctx.lineTo(x + 2 + offset, y - 4);
    this.ctx.moveTo(x + 18, y + 2);
    this.ctx.lineTo(x + 26, y + 18);
    this.ctx.moveTo(x + 18, y + 2);
    this.ctx.lineTo(x + 10, y + 18);
    this.ctx.stroke();
  }

  private drawPanda(w: number, h: number, x: number): void {
    const y = h * 0.66;
    this.ctx.beginPath();
    this.ctx.arc(x, y - 10, 18, 0, Math.PI * 2);
    this.ctx.fill();
    this.ctx.fillStyle = AppColors.SURFACE;
    this.ctx.beginPath();
    this.ctx.arc(x, y - 10, 13, 0, Math.PI * 2);
    this.ctx.fill();
    this.ctx.fillStyle = this.accent;
    this.ctx.beginPath();
    this.ctx.arc(x - 5, y - 13, 4, 0, Math.PI * 2);
    this.ctx.arc(x + 5, y - 13, 4, 0, Math.PI * 2);
    this.ctx.fill();
    this.ctx.beginPath();
    this.ctx.arc(x - 14, y - 24, 6, 0, Math.PI * 2);
    this.ctx.arc(x + 14, y - 24, 6, 0, Math.PI * 2);
    this.ctx.fill();
  }

  build() {
    Canvas(this.ctx)
      .width('100%')
      .height('100%')
      .onReady(() => {
        this.ready = true;
        this.draw();
      })
  }
}
