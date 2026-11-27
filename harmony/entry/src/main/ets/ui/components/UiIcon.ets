/**
 * 通用线性图标：Canvas 绘制，避免字体符号缺失。
 * 坐标系按 24x24 设计，随 iconSize 缩放。
 */
export enum UiIconKind {
  NOW = 'now',
  LIBRARY = 'library',
  RADIO = 'radio',
  PLAYERS = 'players',
  SETTINGS = 'settings',
  SEARCH = 'search',
  REFRESH = 'refresh',
  PLUS = 'plus',
  BACK = 'back',
  CHEVRON_RIGHT = 'chevronRight',
  CHEVRON_DOWN = 'chevronDown',
  QUEUE = 'queue',
  HEART = 'heart',
  SHARE = 'share',
  TIMER = 'timer',
  EQUALIZER = 'equalizer',
  SLIDERS = 'sliders',
  POWER = 'power',
  MORE = 'more',
  VOLUME = 'volume',
  MUTE = 'mute',
  STOP = 'stop',
  TRASH = 'trash',
  MATCH = 'match',
  MUSIC = 'music',
  FOLDER = 'folder',
  PALETTE = 'palette',
  GLOBE = 'globe',
  WIFI = 'wifi',
  EXTERNAL = 'external',
  INFO = 'info',
  DOWNLOAD = 'download',
  COPY = 'copy',
  CHECK = 'check',
  CLOSE = 'close',
  SORT = 'sort',
  GRID = 'grid',
  LIST = 'list',
  ARTIST = 'artist',
  TAG = 'tag',
  CALENDAR = 'calendar',
  SHUFFLE = 'shuffle',
  CLOCK = 'clock',
  BELL = 'bell',
  LOCK = 'lock',
  SPARKLE = 'sparkle'
}

@ComponentV2
export struct UiIcon {
  @Param kind: UiIconKind = UiIconKind.MUSIC;
  @Param iconColor: string = '#FFFFFF';
  @Param iconSize: number = 22;

  private settings: RenderingContextSettings = new RenderingContextSettings(true);
  private ctx: CanvasRenderingContext2D = new CanvasRenderingContext2D(this.settings);
  private ready: boolean = false;
  private timer: number = -1;

  aboutToAppear(): void {
    if (this.timer < 0) {
      this.timer = setInterval(() => {
        this.draw();
      }, 500);
    }
  }

  aboutToDisappear(): void {
    if (this.timer >= 0) {
      clearInterval(this.timer);
      this.timer = -1;
    }
  }

  private u(value: number): number {
    return this.iconSize / 24 * value;
  }

  private begin(): void {
    this.ctx.beginPath();
  }

  private stroke(): void {
    this.ctx.stroke();
  }

  private line(x1: number, y1: number, x2: number, y2: number): void {
    this.ctx.beginPath();
    this.ctx.moveTo(this.u(x1), this.u(y1));
    this.ctx.lineTo(this.u(x2), this.u(y2));
    this.ctx.stroke();
  }

  private circle(cx: number, cy: number, r: number, fill: boolean = false): void {
    this.ctx.beginPath();
    this.ctx.arc(this.u(cx), this.u(cy), this.u(r), 0, Math.PI * 2);
    if (fill) {
      this.ctx.fill();
    }
    this.ctx.stroke();
  }

  private polyline(points: number[]): void {
    if (points.length < 4) {
      return;
    }
    this.ctx.beginPath();
    this.ctx.moveTo(this.u(points[0]), this.u(points[1]));
    for (let i = 2; i < points.length; i += 2) {
      this.ctx.lineTo(this.u(points[i]), this.u(points[i + 1]));
    }
    this.ctx.stroke();
  }

  private polygon(points: number[], fill: boolean): void {
    if (points.length < 6) {
      return;
    }
    this.ctx.beginPath();
    this.ctx.moveTo(this.u(points[0]), this.u(points[1]));
    for (let i = 2; i < points.length; i += 2) {
      this.ctx.lineTo(this.u(points[i]), this.u(points[i + 1]));
    }
    this.ctx.closePath();
    if (fill) {
      this.ctx.fill();
    }
    this.ctx.stroke();
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
    ctx.clearRect(0, 0, size, size);
    ctx.strokeStyle = this.iconColor;
    ctx.fillStyle = this.iconColor;
    ctx.lineWidth = this.u(1.7);
    ctx.lineCap = 'round';
    ctx.globalAlpha = 1;

    switch (this.kind) {
      case UiIconKind.NOW:
        this.circle(12, 12, 9);
        this.polygon([10, 7.5, 17, 12, 10, 16.5], true);
        break;
      case UiIconKind.LIBRARY:
        ctx.beginPath();
        ctx.moveTo(this.u(6), this.u(4));
        ctx.lineTo(this.u(18), this.u(4));
        ctx.lineTo(this.u(18), this.u(20));
        ctx.lineTo(this.u(6), this.u(20));
        ctx.stroke();
        this.line(9.5, 4, 9.5, 20);
        this.circle(14.5, 16, 1.8, true);
        this.line(16.3, 16, 16.3, 9.5);
        break;
      case UiIconKind.RADIO:
        ctx.beginPath();
        ctx.moveTo(this.u(5), this.u(9));
        ctx.lineTo(this.u(19), this.u(9));
        ctx.lineTo(this.u(19), this.u(19));
        ctx.lineTo(this.u(5), this.u(19));
        ctx.closePath();
        ctx.stroke();
        this.line(8, 9, 12, 4);
        this.line(12, 4, 15, 4);
        this.circle(9, 14.5, 1.3, true);
        this.line(13, 13, 18, 13);
        this.line(13, 16, 18, 16);
        break;
      case UiIconKind.PLAYERS:
        ctx.beginPath();
        ctx.moveTo(this.u(4), this.u(9));
        ctx.lineTo(this.u(8), this.u(9));
        ctx.lineTo(this.u(13), this.u(5));
        ctx.lineTo(this.u(13), this.u(19));
        ctx.lineTo(this.u(8), this.u(15));
        ctx.lineTo(this.u(4), this.u(15));
        ctx.closePath();
        ctx.stroke();
        ctx.beginPath();
        ctx.arc(this.u(13), this.u(12), this.u(5), -Math.PI / 3, Math.PI / 3);
        ctx.stroke();
        ctx.beginPath();
        ctx.arc(this.u(13), this.u(12), this.u(8.5), -Math.PI / 3, Math.PI / 3);
        ctx.stroke();
        break;
      case UiIconKind.SETTINGS:
        this.circle(12, 12, 3.3);
        ctx.lineWidth = this.u(2.4);
        for (let i = 0; i < 8; i++) {
          const angle = Math.PI / 4 * i;
          const x1 = 12 + Math.cos(angle) * 5.4;
          const y1 = 12 + Math.sin(angle) * 5.4;
          const x2 = 12 + Math.cos(angle) * 8.6;
          const y2 = 12 + Math.sin(angle) * 8.6;
          this.ctx.beginPath();
          this.ctx.moveTo(this.u(x1), this.u(y1));
          this.ctx.lineTo(this.u(x2), this.u(y2));
          this.ctx.stroke();
        }
        this.circle(12, 12, 7.4);
        ctx.lineWidth = this.u(1.7);
        break;
      case UiIconKind.SEARCH:
        this.circle(10.5, 10.5, 6.2);
        this.line(15, 15, 20, 20);
        break;
      case UiIconKind.REFRESH:
        ctx.beginPath();
        ctx.arc(this.u(12), this.u(12), this.u(7.5), -Math.PI * 0.35, Math.PI * 1.2);
        ctx.stroke();
        this.polygon([17.5, 3.5, 20, 7.5, 15.5, 7.8], true);
        break;
      case UiIconKind.PLUS:
        this.line(12, 5, 12, 19);
        this.line(5, 12, 19, 12);
        break;
      case UiIconKind.BACK:
        this.line(20, 12, 4.5, 12);
        this.polyline([10.5, 5.5, 4, 12, 10.5, 18.5]);
        break;
      case UiIconKind.CHEVRON_RIGHT:
        this.polyline([9, 5, 16, 12, 9, 19]);
        break;
      case UiIconKind.CHEVRON_DOWN:
        this.polyline([5, 9, 12, 16, 19, 9]);
        break;
      case UiIconKind.QUEUE:
        this.line(4, 7, 16, 7);
        this.line(4, 12, 16, 12);
        this.line(4, 17, 12, 17);
        this.line(18, 14, 18, 20);
        this.line(15, 17, 21, 17);
        break;
      case UiIconKind.HEART:
        ctx.beginPath();
        ctx.moveTo(this.u(12), this.u(20));
        ctx.bezierCurveTo(this.u(5), this.u(15), this.u(3), this.u(11), this.u(6), this.u(7));
        ctx.bezierCurveTo(this.u(8.5), this.u(4), this.u(11.5), this.u(5.5), this.u(12), this.u(8));
        ctx.bezierCurveTo(this.u(12.5), this.u(5.5), this.u(15.5), this.u(4), this.u(18), this.u(7));
        ctx.bezierCurveTo(this.u(21), this.u(11), this.u(19), this.u(15), this.u(12), this.u(20));
        ctx.closePath();
        ctx.stroke();
        break;
      case UiIconKind.SHARE:
        this.circle(6.5, 12, 2.6);
        this.circle(17.5, 6, 2.6);
        this.circle(17.5, 18, 2.6);
        this.line(8.8, 10.8, 15.2, 7.3);
        this.line(8.8, 13.2, 15.2, 16.7);
        break;
      case UiIconKind.TIMER:
        this.circle(12, 13.5, 7.2);
        this.line(12, 13.5, 12, 9.5);
        this.line(9.5, 2.5, 14.5, 2.5);
        this.line(12, 2.5, 12, 6);
        break;
      case UiIconKind.EQUALIZER:
        this.line(5, 10, 5, 18);
        this.line(9.5, 5, 9.5, 18);
        this.line(14, 8, 14, 18);
        this.line(18.5, 12, 18.5, 18);
        break;
      case UiIconKind.SLIDERS:
        this.line(3, 7, 21, 7);
        this.line(3, 12, 21, 12);
        this.line(3, 17, 21, 17);
        this.circle(8, 7, 2, true);
        this.circle(16, 12, 2, true);
        this.circle(10, 17, 2, true);
        break;
      case UiIconKind.POWER:
        ctx.beginPath();
        ctx.arc(this.u(12), this.u(12), this.u(8), -Math.PI / 2 + 0.6, Math.PI / 2 + Math.PI - 0.6);
        ctx.stroke();
        this.line(12, 3.5, 12, 11);
        break;
      case UiIconKind.MORE:
        this.circle(12, 5.5, 1.6, true);
        this.circle(12, 12, 1.6, true);
        this.circle(12, 18.5, 1.6, true);
        break;
      case UiIconKind.VOLUME:
        ctx.beginPath();
        ctx.moveTo(this.u(4), this.u(9));
        ctx.lineTo(this.u(8), this.u(9));
        ctx.lineTo(this.u(12), this.u(5));
        ctx.lineTo(this.u(12), this.u(19));
        ctx.lineTo(this.u(8), this.u(15));
        ctx.lineTo(this.u(4), this.u(15));
        ctx.closePath();
        ctx.stroke();
        ctx.beginPath();
        ctx.arc(this.u(12), this.u(12), this.u(5.5), -Math.PI / 3, Math.PI / 3);
        ctx.stroke();
        break;
      case UiIconKind.MUTE:
        ctx.beginPath();
        ctx.moveTo(this.u(4), this.u(9));
        ctx.lineTo(this.u(8), this.u(9));
        ctx.lineTo(this.u(12), this.u(5));
        ctx.lineTo(this.u(12), this.u(19));
        ctx.lineTo(this.u(8), this.u(15));
        ctx.lineTo(this.u(4), this.u(15));
        ctx.closePath();
        ctx.stroke();
        this.line(15, 9, 21, 15);
        this.line(21, 9, 15, 15);
        break;
      case UiIconKind.STOP:
        this.polygon([6.5, 6.5, 17.5, 6.5, 17.5, 17.5, 6.5, 17.5], true);
        break;
      case UiIconKind.TRASH:
        this.line(4, 7, 20, 7);
        this.line(9, 4, 15, 4);
        ctx.beginPath();
        ctx.moveTo(this.u(6), this.u(7));
        ctx.lineTo(this.u(7.5), this.u(20));
        ctx.lineTo(this.u(16.5), this.u(20));
        ctx.lineTo(this.u(18), this.u(7));
        ctx.stroke();
        this.line(10, 11, 10, 16.5);
        this.line(14, 11, 14, 16.5);
        break;
      case UiIconKind.MATCH:
        this.polygon([12, 3, 13.6, 9.4, 20, 11, 13.6, 12.6, 12, 19, 10.4, 12.6, 4, 11, 10.4, 9.4], false);
        this.line(18, 16, 18, 21);
        this.line(15.5, 18.5, 20.5, 18.5);
        break;
      case UiIconKind.MUSIC:
        this.circle(8, 17, 2.6, true);
        this.line(10.6, 17, 10.6, 5);
        this.polyline([10.6, 5, 18.5, 7, 18.5, 14]);
        this.circle(16.4, 14.2, 2.2, true);
        break;
      case UiIconKind.FOLDER:
        ctx.beginPath();
        ctx.moveTo(this.u(3.5), this.u(7));
        ctx.lineTo(this.u(9.5), this.u(7));
        ctx.lineTo(this.u(11.5), this.u(9.5));
        ctx.lineTo(this.u(20.5), this.u(9.5));
        ctx.lineTo(this.u(20.5), this.u(18.5));
        ctx.lineTo(this.u(3.5), this.u(18.5));
        ctx.closePath();
        ctx.stroke();
        break;
      case UiIconKind.PALETTE:
        this.circle(12, 12, 8.5);
        this.circle(8, 9.5, 1.3, true);
        this.circle(12.5, 7.5, 1.3, true);
        this.circle(16.5, 10, 1.3, true);
        this.circle(15.5, 15, 1.3, true);
        break;
      case UiIconKind.GLOBE:
        this.circle(12, 12, 8.5);
        this.line(3.5, 12, 20.5, 12);
        ctx.beginPath();
        ctx.ellipse(this.u(12), this.u(12), this.u(4), this.u(8.5), 0, 0, Math.PI * 2);
        ctx.stroke();
        break;
      case UiIconKind.WIFI:
        ctx.beginPath();
        ctx.arc(this.u(12), this.u(18), this.u(9), Math.PI * 1.25, Math.PI * 1.75);
        ctx.stroke();
        ctx.beginPath();
        ctx.arc(this.u(12), this.u(18), this.u(5.5), Math.PI * 1.25, Math.PI * 1.75);
        ctx.stroke();
        this.circle(12, 18, 1.5, true);
        break;
      case UiIconKind.EXTERNAL:
        this.polyline([13, 4, 20, 4, 20, 11]);
        this.line(20, 4, 11, 13);
        ctx.beginPath();
        ctx.moveTo(this.u(18), this.u(14));
        ctx.lineTo(this.u(18), this.u(20));
        ctx.lineTo(this.u(4), this.u(20));
        ctx.lineTo(this.u(4), this.u(6));
        ctx.lineTo(this.u(10), this.u(6));
        ctx.stroke();
        break;
      case UiIconKind.INFO:
        this.circle(12, 12, 8.5);
        this.circle(12, 8, 1.2, true);
        this.line(12, 11, 12, 16.5);
        break;
      case UiIconKind.DOWNLOAD:
        this.line(12, 4, 12, 15);
        this.polyline([7.5, 10.5, 12, 15, 16.5, 10.5]);
        this.polyline([4.5, 16, 4.5, 20, 19.5, 20, 19.5, 16]);
        break;
      case UiIconKind.COPY:
        ctx.beginPath();
        ctx.rect(this.u(8), this.u(8), this.u(12), this.u(12));
        ctx.stroke();
        ctx.beginPath();
        ctx.moveTo(this.u(16), this.u(4));
        ctx.lineTo(this.u(4), this.u(4));
        ctx.lineTo(this.u(4), this.u(16));
        ctx.stroke();
        break;
      case UiIconKind.CHECK:
        this.polyline([4.5, 12.5, 9.5, 17.5, 19.5, 6.5]);
        break;
      case UiIconKind.CLOSE:
        this.line(6, 6, 18, 18);
        this.line(18, 6, 6, 18);
        break;
      case UiIconKind.SORT:
        this.line(4, 6.5, 20, 6.5);
        this.line(4, 12, 14, 12);
        this.line(4, 17.5, 9, 17.5);
        break;
      case UiIconKind.GRID:
        ctx.beginPath();
        ctx.rect(this.u(4), this.u(4), this.u(7), this.u(7));
        ctx.stroke();
        ctx.beginPath();
        ctx.rect(this.u(13), this.u(4), this.u(7), this.u(7));
        ctx.stroke();
        ctx.beginPath();
        ctx.rect(this.u(4), this.u(13), this.u(7), this.u(7));
        ctx.stroke();
        ctx.beginPath();
        ctx.rect(this.u(13), this.u(13), this.u(7), this.u(7));
        ctx.stroke();
        break;
      case UiIconKind.LIST:
        this.circle(5, 7, 1.2, true);
        this.line(9, 7, 20, 7);
        this.circle(5, 12, 1.2, true);
        this.line(9, 12, 20, 12);
        this.circle(5, 17, 1.2, true);
        this.line(9, 17, 20, 17);
        break;
      case UiIconKind.ARTIST:
        this.circle(12, 8, 4);
        ctx.beginPath();
        ctx.arc(this.u(12), this.u(19), this.u(7), Math.PI * 1.15, Math.PI * 1.85);
        ctx.stroke();
        break;
      case UiIconKind.TAG:
        this.polygon([4, 4, 13, 4, 20, 11, 12, 19, 4, 11], false);
        this.circle(8.5, 8.5, 1.4, true);
        break;
      case UiIconKind.CALENDAR:
        ctx.beginPath();
        ctx.rect(this.u(4), this.u(6), this.u(16), this.u(14));
        ctx.stroke();
        this.line(4, 10.5, 20, 10.5);
        this.line(8.5, 3.5, 8.5, 7);
        this.line(15.5, 3.5, 15.5, 7);
        this.circle(8.5, 15, 1.2, true);
        this.circle(12, 15, 1.2, true);
        this.circle(15.5, 15, 1.2, true);
        break;
      case UiIconKind.SHUFFLE:
        this.polyline([4, 7, 8, 7, 17, 17, 20, 17]);
        this.polyline([4, 17, 8, 17, 17, 7, 20, 7]);
        this.polyline([17, 4, 20, 7, 17, 10]);
        this.polyline([17, 14, 20, 17, 17, 20]);
        break;
      case UiIconKind.CLOCK:
        this.circle(12, 12, 8.5);
        this.polyline([12, 7, 12, 12, 15.5, 14]);
        break;
      case UiIconKind.BELL:
        ctx.beginPath();
        ctx.moveTo(this.u(6), this.u(16.5));
        ctx.lineTo(this.u(6), this.u(11));
        ctx.bezierCurveTo(this.u(6), this.u(5.5), this.u(18), this.u(5.5), this.u(18), this.u(11));
        ctx.lineTo(this.u(18), this.u(16.5));
        ctx.closePath();
        ctx.stroke();
        this.line(4.5, 16.5, 19.5, 16.5);
        ctx.beginPath();
        ctx.arc(this.u(12), this.u(17.5), this.u(2.4), 0, Math.PI);
        ctx.stroke();
        break;
      case UiIconKind.LOCK:
        ctx.beginPath();
        ctx.rect(this.u(5.5), this.u(10.5), this.u(13), this.u(9.5));
        ctx.stroke();
        ctx.beginPath();
        ctx.arc(this.u(12), this.u(10.5), this.u(4), Math.PI, 0);
        ctx.stroke();
        this.circle(12, 15, 1.2, true);
        break;
      case UiIconKind.SPARKLE:
        this.polygon([12, 3, 13.8, 10.2, 21, 12, 13.8, 13.8, 12, 21, 10.2, 13.8, 3, 12, 10.2, 10.2], true);
        break;
      default:
        this.circle(12, 12, 8);
        break;
    }
  }

  build() {
    Canvas(this.ctx)
      .width(this.iconSize)
      .height(this.iconSize)
      .onReady(() => {
        this.ready = true;
        this.draw();
      })
  }
}
