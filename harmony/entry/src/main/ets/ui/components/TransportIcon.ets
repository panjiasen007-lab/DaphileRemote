/** 与主题配色一致的矢量播放控制图标（Canvas 绘制，避免 emoji 与主题冲突）。 */
export enum TransportKind {
  PLAY = 'play',
  PAUSE = 'pause',
  PREV = 'prev',
  NEXT = 'next',
  SHUFFLE = 'shuffle',
  REPEAT = 'repeat',
  REPEAT_ONE = 'repeatOne',
  VOLUME = 'volume',
  MUTE = 'mute'
}

@ComponentV2
export struct TransportIcon {
  @Param kind: TransportKind = TransportKind.PLAY;
  @Param iconColor: string = '#FFFFFF';
  @Param iconSize: number = 22;

  private timer: number = -1;
  private settings: RenderingContextSettings = new RenderingContextSettings(true);
  private ctx: CanvasRenderingContext2D = new CanvasRenderingContext2D(this.settings);

  aboutToAppear(): void {
    if (this.timer < 0) {
      this.timer = setInterval(() => this.draw(), 250);
    }
  }

  aboutToDisappear(): void {
    if (this.timer >= 0) {
      clearInterval(this.timer);
      this.timer = -1;
    }
  }

  private draw(): void {
    const ctx = this.ctx;
    ctx.clearRect(0, 0, 24, 24);
    ctx.fillStyle = this.iconColor;
    ctx.strokeStyle = this.iconColor;
    ctx.lineWidth = 2;
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';

    if (this.kind === TransportKind.PLAY) {
      ctx.beginPath();
      ctx.moveTo(8, 5);
      ctx.lineTo(19, 12);
      ctx.lineTo(8, 19);
      ctx.closePath();
      ctx.fill();
      ctx.lineWidth = 4.5;
      ctx.stroke();
    } else if (this.kind === TransportKind.PAUSE) {
      ctx.fillRect(7, 5, 3, 14);
      ctx.fillRect(14, 5, 3, 14);
    } else if (this.kind === TransportKind.PREV) {
      ctx.fillRect(6, 5, 3, 14);
      ctx.beginPath();
      ctx.moveTo(19, 5);
      ctx.lineTo(10, 12);
      ctx.lineTo(19, 19);
      ctx.closePath();
      ctx.fill();
      ctx.lineWidth = 3;
      ctx.stroke();
    } else if (this.kind === TransportKind.NEXT) {
      ctx.fillRect(15, 5, 3, 14);
      ctx.beginPath();
      ctx.moveTo(5, 5);
      ctx.lineTo(14, 12);
      ctx.lineTo(5, 19);
      ctx.closePath();
      ctx.fill();
      ctx.lineWidth = 3;
      ctx.stroke();
    } else if (this.kind === TransportKind.SHUFFLE) {
      ctx.beginPath();
      ctx.moveTo(4, 7);
      ctx.lineTo(8, 7);
      ctx.lineTo(16, 17);
      ctx.lineTo(20, 17);
      ctx.stroke();
      ctx.beginPath();
      ctx.moveTo(4, 17);
      ctx.lineTo(8, 17);
      ctx.lineTo(16, 7);
      ctx.lineTo(20, 7);
      ctx.stroke();
      ctx.beginPath();
      ctx.moveTo(17, 4);
      ctx.lineTo(20, 7);
      ctx.lineTo(17, 10);
      ctx.moveTo(17, 14);
      ctx.lineTo(20, 17);
      ctx.lineTo(17, 20);
      ctx.stroke();
    } else if (this.kind === TransportKind.REPEAT || this.kind === TransportKind.REPEAT_ONE) {
      ctx.beginPath();
      ctx.moveTo(6, 7);
      ctx.lineTo(18, 7);
      ctx.arc(18, 10, 3, -Math.PI / 2, 0);
      ctx.lineTo(21, 11);
      ctx.stroke();
      ctx.beginPath();
      ctx.moveTo(18, 17);
      ctx.lineTo(6, 17);
      ctx.arc(6, 14, 3, Math.PI / 2, Math.PI);
      ctx.lineTo(3, 13);
      ctx.stroke();
      ctx.beginPath();
      ctx.moveTo(18, 4);
      ctx.lineTo(21, 7);
      ctx.lineTo(18, 10);
      ctx.moveTo(6, 14);
      ctx.lineTo(3, 17);
      ctx.lineTo(6, 20);
      ctx.stroke();
      if (this.kind === TransportKind.REPEAT_ONE) {
        ctx.font = 'bold 8px sans-serif';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText('1', 12, 12);
      }
    } else if (this.kind === TransportKind.VOLUME) {
      ctx.beginPath();
      ctx.moveTo(4, 9);
      ctx.lineTo(8, 9);
      ctx.lineTo(12, 5);
      ctx.lineTo(12, 19);
      ctx.lineTo(8, 15);
      ctx.lineTo(4, 15);
      ctx.closePath();
      ctx.fill();
      ctx.lineWidth = 2;
      ctx.stroke();
      ctx.beginPath();
      ctx.arc(13, 12, 5, -Math.PI / 3, Math.PI / 3);
      ctx.stroke();
      ctx.beginPath();
      ctx.arc(13, 12, 8, -Math.PI / 3, Math.PI / 3);
      ctx.stroke();
    } else {
      ctx.beginPath();
      ctx.moveTo(4, 9);
      ctx.lineTo(8, 9);
      ctx.lineTo(12, 5);
      ctx.lineTo(12, 19);
      ctx.lineTo(8, 15);
      ctx.lineTo(4, 15);
      ctx.closePath();
      ctx.fill();
      ctx.lineWidth = 2;
      ctx.stroke();
      ctx.beginPath();
      ctx.moveTo(15, 8);
      ctx.lineTo(21, 16);
      ctx.moveTo(21, 8);
      ctx.lineTo(15, 16);
      ctx.stroke();
    }
  }

  build() {
    Canvas(this.ctx)
      .width(24)
      .height(24)
      .scale({ x: this.iconSize / 24, y: this.iconSize / 24 })
      .onReady(() => this.draw())
  }
}
