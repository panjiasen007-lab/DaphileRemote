Add-Type -AssemblyName System.Drawing

# 生成鸿蒙分层图标：深色背景 + 青绿色圆形音符，与安卓自适应图标保持品牌一致。
$ErrorActionPreference = 'Stop'

function New-Bitmap([int]$size) {
  $bmp = New-Object System.Drawing.Bitmap($size, $size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
  return $bmp
}

function Fill-Background([System.Drawing.Bitmap]$bmp) {
  $g = [System.Drawing.Graphics]::FromImage($bmp)
  $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
  $rect = New-Object System.Drawing.Rectangle(0, 0, $bmp.Width, $bmp.Height)
  $brush = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
    $rect,
    [System.Drawing.Color]::FromArgb(255, 11, 15, 20),
    [System.Drawing.Color]::FromArgb(255, 17, 26, 36),
    [System.Drawing.Drawing2D.LinearGradientMode]::Vertical)
  $g.FillRectangle($brush, $rect)
  $brush.Dispose()

  # 中心青绿色柔光，增加图标层次
  $glowPath = New-Object System.Drawing.Drawing2D.GraphicsPath
  $glowPath.AddEllipse(24, 24, $bmp.Width - 48, $bmp.Height - 48)
  $glow = New-Object System.Drawing.Drawing2D.PathGradientBrush($glowPath)
  $glow.CenterPoint = New-Object System.Drawing.PointF(($bmp.Width / 2), ($bmp.Height / 2 + 40))
  $glow.CenterColor = [System.Drawing.Color]::FromArgb(52, 56, 214, 196)
  $glow.SurroundColors = @([System.Drawing.Color]::FromArgb(0, 56, 214, 196))
  $g.FillEllipse($glow, 24, 24, $bmp.Width - 48, $bmp.Height - 48)
  $glow.Dispose()
  $glowPath.Dispose()
  $g.Dispose()
}

function Draw-AppMark([System.Drawing.Graphics]$g, [float]$centerX, [float]$centerY, [float]$radius) {
  $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias

  $teal = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 56, 214, 196))
  $g.FillEllipse($teal, $centerX - $radius, $centerY - $radius, $radius * 2, $radius * 2)
  $teal.Dispose()

  # 与安卓 vector 音符路径一致；将三次贝塞尔采样为多边形，保证 GDI+ 正确填充
  $s = $radius / 32.0
  $raw = New-Object 'System.Collections.Generic.List[System.Drawing.PointF]'

  function Add-Point([double]$x, [double]$y) {
    $raw.Add((New-Object System.Drawing.PointF(
      [float]($centerX + ($x - 54) * $s),
      [float]($centerY + ($y - 54) * $s))))
  }

  function Add-Cubic([double]$x0, [double]$y0, [double]$x1, [double]$y1, [double]$x2, [double]$y2, [double]$x3, [double]$y3) {
    for ($i = 1; $i -le 24; $i++) {
      $t = $i / 24.0
      $u = 1.0 - $t
      $x = $u * $u * $u * $x0 + 3 * $u * $u * $t * $x1 + 3 * $u * $t * $t * $x2 + $t * $t * $t * $x3
      $y = $u * $u * $u * $y0 + 3 * $u * $u * $t * $y1 + 3 * $u * $t * $t * $y2 + $t * $t * $t * $y3
      Add-Point $x $y
    }
  }

  Add-Point 60 32
  Add-Point 70 30
  Add-Point 70 62
  Add-Cubic 70 62 70 69 64 74 57 74
  Add-Cubic 57 74 51 74 46 70 46 65
  Add-Cubic 46 65 46 60 50 56 56 56
  Add-Cubic 56 56 58 56 60 57 61 58
  Add-Point 61 40
  Add-Point 50 42
  Add-Point 50 34

  $dark = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 11, 15, 20))
  $g.FillPolygon($dark, $raw.ToArray(), [System.Drawing.Drawing2D.FillMode]::Winding)
  $dark.Dispose()
}

function Save-Foreground([string]$path) {
  $bmp = New-Bitmap 1024
  $g = [System.Drawing.Graphics]::FromImage($bmp)
  $g.Clear([System.Drawing.Color]::Transparent)
  Draw-AppMark $g 512 512 300
  $g.Dispose()
  $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
  $bmp.Dispose()
}

function Save-Background([string]$path) {
  $bmp = New-Bitmap 1024
  Fill-Background $bmp
  $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
  $bmp.Dispose()
}

function Save-StartIcon([string]$path) {
  $bmp = New-Bitmap 1024
  Fill-Background $bmp
  $g = [System.Drawing.Graphics]::FromImage($bmp)
  Draw-AppMark $g 512 512 300
  $g.Dispose()

  $small = New-Bitmap 144
  $sg = [System.Drawing.Graphics]::FromImage($small)
  $sg.Clear([System.Drawing.Color]::Transparent)
  $sg.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
  $sg.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
  $sg.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality

  # 深色圆角方块底座
  $round = New-Object System.Drawing.Drawing2D.GraphicsPath
  $r = 32
  $w = 144
  $round.AddArc(0, 0, $r * 2, $r * 2, 180, 90)
  $round.AddArc($w - $r * 2, 0, $r * 2, $r * 2, 270, 90)
  $round.AddArc($w - $r * 2, $w - $r * 2, $r * 2, $r * 2, 0, 90)
  $round.AddArc(0, $w - $r * 2, $r * 2, $r * 2, 90, 90)
  $round.CloseFigure()
  $sg.SetClip($round)
  $sg.DrawImage($bmp, (New-Object System.Drawing.Rectangle(0, 0, 144, 144)))
  $sg.ResetClip()
  $round.Dispose()
  $sg.Dispose()
  $small.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
  $small.Dispose()
  $bmp.Dispose()
}

$targets = @(
  "D:\daphile\harmony\AppScope\resources\base\media",
  "D:\daphile\harmony\entry\src\main\resources\base\media"
)

foreach ($dir in $targets) {
  Save-Background (Join-Path $dir 'background.png')
  Save-Foreground (Join-Path $dir 'foreground.png')
  Save-StartIcon (Join-Path $dir 'startIcon.png')
  Write-Output "generated: $dir"
}
