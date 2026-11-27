@Entry
@Component
struct NowPlayingCard {
  @LocalStorageProp('title') title: string = 'Daphile Remote';
  @LocalStorageProp('artist') artist: string = '';
  @LocalStorageProp('playing') playing: string = 'false';
  @LocalStorageProp('cover') cover: string = '';

  private openApp(): void {
    postCardAction(this, {
      action: 'router',
      abilityName: 'EntryAbility',
      params: {
        'from': 'card'
      }
    });
  }

  private command(value: string): void {
    postCardAction(this, {
      action: 'message',
      params: {
        'command': value
      }
    });
  }

  build() {
    Column({ space: 8 }) {
      Row({ space: 9 }) {
        Column() {
          Text('♪')
            .fontSize(22)
            .fontColor('#2FA894')
        }
        .width(52)
        .height(52)
        .justifyContent(FlexAlign.Center)
        .backgroundColor('#DCEBE6')
        .borderRadius(9)
        Column({ space: 2 }) {
          Text(this.title.length > 0 ? this.title : 'Daphile Remote')
            .fontSize(13)
            .fontWeight(FontWeight.Medium)
            .fontColor('#10201C')
            .maxLines(1)
            .textOverflow({ overflow: TextOverflow.Ellipsis })
            .width('100%')
          Text(this.artist.length > 0 ? this.artist : 'Daphile')
            .fontSize(10)
            .fontColor('#5D6F6A')
            .maxLines(1)
            .textOverflow({ overflow: TextOverflow.Ellipsis })
            .width('100%')
        }
        .alignItems(HorizontalAlign.Start)
        .layoutWeight(1)
      }
      .width('100%')
      .onClick(() => this.openApp())

      Row({ space: 8 }) {
        Row() {
          Text('PREV')
            .fontSize(9)
            .fontColor('#1F3A34')
        }
        .height(28)
        .layoutWeight(1)
        .justifyContent(FlexAlign.Center)
        .backgroundColor('#DCEBE6')
        .borderRadius(14)
        .onClick(() => this.command('previous'))

        Row() {
          Text(this.playing === 'true' ? 'II' : '>')
            .fontSize(14)
            .fontColor('#FFFFFF')
        }
        .height(28)
        .layoutWeight(1)
        .justifyContent(FlexAlign.Center)
        .backgroundColor('#2FA894')
        .borderRadius(14)
        .onClick(() => this.command('playPause'))

        Row() {
          Text('NEXT')
            .fontSize(9)
            .fontColor('#1F3A34')
        }
        .height(28)
        .layoutWeight(1)
        .justifyContent(FlexAlign.Center)
        .backgroundColor('#DCEBE6')
        .borderRadius(14)
        .onClick(() => this.command('next'))
      }
      .width('100%')
    }
    .width('100%')
    .height('100%')
    .padding(12)
    .backgroundColor('#F1F7F4')
    .borderRadius(16)
  }
}
