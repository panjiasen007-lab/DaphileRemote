import { ThemeSliderStyle } from '../theme/ThemeRuntime';

/**
 * 主题进度条拇指：按 14 套主题的滑块造型绘制。
 * 仅负责视觉，手势仍由底层 Slider 处理（原生拇指设为透明）。
 */
@ComponentV2
export struct ThemeThumb {
  @Param style: ThemeSliderStyle = ThemeSliderStyle.GLOW_DOT;
  @Param accent: string = '#38D6C4';
  @Param surface: string = '#131A21';
  @Param thumbSize: number = 24;

  private settings: RenderingContextSettings = new RenderingContextSettings(true);
  private ctx: CanvasRenderingContext2D = new CanvasRenderingContext2D(this.settings);
  private ready: boolean = false;

  private drawStar(cx: number, cy: number, outer: number, inner: number, points: number): void {
    const ctx = this.ctx;
    ctx.beginPath();
    for (let i = 0; i < points * 2; i++) {
      const radius = i % 2 === 0 ? outer : inner;
      const angle = (Math.PI / points) * i - Math.PI / 2;
      const x = cx + Math.cos(angle) * radius;
      const y = cy + Math.sin(angle) * radius;
      if (i === 0) {
        ctx.moveTo(x, y);
      } else {
        ctx.lineTo(x, y);
      }
    }
    ctx.closePath();
    ctx.fill();
  }

  private draw(): void {
    if (!this.ready) {
      return;
    }
    const ctx = this.ctx;
    const size = ctx.width;
    if (size <= 0) {
      return;
    }
    const c = size / 2;
    ctx.clearRect(0, 0, size, size);
    ctx.fillStyle = this.accent;
    ctx.strokeStyle = this.accent;
    ctx.lineCap = 'round';
    ctx.globalAlpha = 1;

    switch (this.style) {
      case ThemeSliderStyle.HAIR_LINE:
        ctx.lineWidth = 3;
        ctx.beginPath();
        ctx.moveTo(c, c - 9);
        ctx.lineTo(c, c + 9);
        ctx.stroke();
        break;
      case ThemeSliderStyle.SILVER_LINE:
        ctx.lineWidth = 2;
        ctx.beginPath();
        ctx.moveTo(c, c - 8);
        ctx.lineTo(c, c + 8);
        ctx.stroke();
        ctx.beginPath();
        ctx.arc(c, c, 5.5, 0, Math.PI * 2);
        ctx.stroke();
        break;
      case ThemeSliderStyle.RUYI_BAR:
        ctx.beginPath();
        ctx.arc(c - 7, c, 4.4, 0, Math.PI * 2);
        ctx.fill();
        ctx.beginPath();
        ctx.arc(c + 7, c, 4.4, 0, Math.PI * 2);
        ctx.fill();
        ctx.fillRect(c - 7, c - 4.4, 14, 8.8);
        ctx.fillStyle = this.surface;
        ctx.globalAlpha = 0.55;
        ctx.fillRect(c - 5, c - 1, 10, 2);
        ctx.globalAlpha = 1;
        break;
      case ThemeSliderStyle.LEAF_THUMB:
        ctx.save();
        ctx.translate(c, c);
        ctx.rotate(-0.65);
        ctx.beginPath();
        ctx.ellipse(0, 0, 5.5, 9.5, 0, 0, Math.PI * 2);
        ctx.fill();
        ctx.strokeStyle = this.surface;
        ctx.globalAlpha = 0.8;
        ctx.lineWidth = 1.2;
        ctx.beginPath();
        ctx.moveTo(0, -7);
        ctx.lineTo(0, 7);
        ctx.stroke();
        ctx.globalAlpha = 1;
        ctx.restore();
        break;
      case ThemeSliderStyle.BUD_THUMB:
        ctx.beginPath();
        ctx.arc(c, c + 2, 5.2, 0, Math.PI * 2);
        ctx.fill();
        ctx.beginPath();
        ctx.arc(c - 5, c - 4, 3.8, 0, Math.PI * 2);
        ctx.fill();
        ctx.beginPath();
        ctx.arc(c + 5, c - 4, 3.8, 0, Math.PI * 2);
        ctx.fill();
        break;
      case ThemeSliderStyle.SEED_THUMB:
        ctx.beginPath();
        ctx.ellipse(c, c, 6, 8.5, 0.3, 0, Math.PI * 2);
        ctx.fill();
        ctx.globalAlpha = 0.5;
        ctx.fillStyle = this.surface;
        ctx.beginPath();
        ctx.arc(c + 1, c, 2.4, 0, Math.PI * 2);
        ctx.fill();
        ctx.globalAlpha = 1;
        break;
      case ThemeSliderStyle.SHELL_THUMB:
        ctx.beginPath();
        ctx.moveTo(c, c + 8);
        ctx.arc(c, c + 8, 9.5, Math.PI, Math.PI * 2);
        ctx.closePath();
        ctx.fill();
        ctx.strokeStyle = this.surface;
        ctx.globalAlpha = 0.7;
        ctx.lineWidth = 1;
        for (let i = -2; i <= 2; i++) {
          ctx.beginPath();
          ctx.moveTo(c, c + 7);
          ctx.lineTo(c + i * 4.2, c - 0.5);
          ctx.stroke();
        }
        ctx.globalAlpha = 1;
        break;
      case ThemeSliderStyle.MAPLE_THUMB:
        this.drawStar(c, c, 10, 4.4, 5);
        break;
      case ThemeSliderStyle.FROST_NEEDLE:
        ctx.beginPath();
        ctx.moveTo(c, c + 11);
        ctx.lineTo(c - 4.5, c - 7);
        ctx.lineTo(c + 4.5, c - 7);
        ctx.closePath();
        ctx.fill();
        ctx.beginPath();
        ctx.arc(c, c - 7, 2.6, 0, Math.PI * 2);
        ctx.fill();
        break;
      case ThemeSliderStyle.ICICLE:
        ctx.beginPath();
        ctx.arc(c, c - 4, 5, 0, Math.PI * 2);
        ctx.fill();
        ctx.beginPath();
        ctx.moveTo(c - 4, c - 1);
        ctx.lineTo(c, c + 11);
        ctx.lineTo(c + 4, c - 1);
        ctx.closePath();
        ctx.fill();
        break;
      case ThemeSliderStyle.YIN_YANG_THUMB:
        ctx.beginPath();
        ctx.arc(c, c, 10, 0, Math.PI * 2);
        ctx.fill();
        ctx.fillStyle = this.surface;
        ctx.beginPath();
        ctx.arc(c, c, 10, Math.PI / 2, Math.PI * 1.5);
        ctx.fill();
        ctx.beginPath();
        ctx.arc(c, c - 5, 5, 0, Math.PI * 2);
        ctx.fill();
        ctx.fillStyle = this.accent;
        ctx.beginPath();
        ctx.arc(c, c + 5, 5, 0, Math.PI * 2);
        ctx.fill();
        ctx.fillStyle = this.surface;
        ctx.beginPath();
        ctx.arc(c, c - 5, 2, 0, Math.PI * 2);
        ctx.fill();
        ctx.fillStyle = this.accent;
        ctx.beginPath();
        ctx.arc(c, c + 5, 2, 0, Math.PI * 2);
        ctx.fill();
        break;
      case ThemeSliderStyle.PAW_THUMB:
        ctx.beginPath();
        ctx.ellipse(c, c + 4, 6.2, 5.2, 0, 0, Math.PI * 2);
        ctx.fill();
        ctx.beginPath();
        ctx.arc(c - 6, c - 4, 2.7, 0, Math.PI * 2);
        ctx.fill();
        ctx.beginPath();
        ctx.arc(c - 1.6, c - 7, 2.7, 0, Math.PI * 2);
        ctx.fill();
        ctx.beginPath();
        ctx.arc(c + 3.6, c - 6, 2.7, 0, Math.PI * 2);
        ctx.fill();
        break;
      case ThemeSliderStyle.GLOW_DOT:
        ctx.globalAlpha = 0.18;
        ctx.beginPath();
        ctx.arc(c, c, 11, 0, Math.PI * 2);
        ctx.fill();
        ctx.globalAlpha = 1;
        ctx.beginPath();
        ctx.arc(c, c, 7, 0, Math.PI * 2);
        ctx.fill();
        ctx.strokeStyle = '#FFFFFF';
        ctx.globalAlpha = 0.75;
        ctx.lineWidth = 1.4;
        ctx.beginPath();
        ctx.arc(c, c, 7, 0, Math.PI * 2);
        ctx.stroke();
        ctx.globalAlpha = 1;
        break;
      case ThemeSliderStyle.DEFAULT:
      default:
        ctx.beginPath();
        ctx.arc(c, c, 8, 0, Math.PI * 2);
        ctx.fill();
        break;
    }
  }

  build() {
    Canvas(this.ctx)
      .width(this.thumbSize)
      .height(this.thumbSize)
      .onReady(() => {
        this.ready = true;
        this.draw();
      })
  }
}
