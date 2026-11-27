/** 封面外圈光环：Canvas 仅描边，避免遮挡下层封面。 */
@ComponentV2
export struct HaloRing {
  @Param ringSize: number = 196;
  @Param accent: string = '#38D6C4';

  private settings: RenderingContextSettings = new RenderingContextSettings(true);
  private ctx: CanvasRenderingContext2D = new CanvasRenderingContext2D(this.settings);
  private ready: boolean = false;

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
    const center = size / 2;
    ctx.strokeStyle = this.accent;
    ctx.lineWidth = Math.max(0.8, size * 0.0042);
    ctx.beginPath();
    ctx.arc(center, center, center * 0.94, 0, Math.PI * 2);
    ctx.stroke();
    ctx.globalAlpha = 0.35;
    ctx.lineWidth = Math.max(0.6, size * 0.003);
    ctx.beginPath();
    ctx.arc(center, center, center * 0.88, 0, Math.PI * 2);
    ctx.stroke();
    ctx.globalAlpha = 1;
  }

  build() {
    Canvas(this.ctx)
      .width(this.ringSize)
      .height(this.ringSize)
      .onReady(() => {
        this.ready = true;
        this.draw();
      })
  }
}
