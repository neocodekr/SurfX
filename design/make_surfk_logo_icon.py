"""Render the SurfK website's single wave mark as a 512 × 512 PNG."""

from pathlib import Path
from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "surfk_logo_icon_512.png"
SIZE = 512
SCALE = 4
BACKGROUND = (7, 23, 27)
MINT = (85, 235, 199)


def bezier(a, b, c, d, count=120):
    result = []
    for i in range(count + 1):
        t = i / count
        u = 1 - t
        result.append((
            round((u**3 * a[0] + 3 * u**2 * t * b[0] + 3 * u * t**2 * c[0] + t**3 * d[0]) * SCALE),
            round((u**3 * a[1] + 3 * u**2 * t * b[1] + 3 * u * t**2 * c[1] + t**3 * d[1]) * SCALE),
        ))
    return result


canvas = Image.new("RGB", (SIZE * SCALE, SIZE * SCALE), BACKGROUND)
draw = ImageDraw.Draw(canvas)
curve = bezier((124, 326), (129, 250), (153, 213), (195, 216))
curve += bezier((195, 216), (239, 219), (252, 317), (302, 325))[1:]
curve += bezier((302, 325), (349, 338), (376, 307), (387, 227))[1:]
draw.line(curve, fill=MINT, width=42 * SCALE, joint="curve")
radius = 21 * SCALE
for x, y in (curve[0], curve[-1]):
    draw.ellipse((x - radius, y - radius, x + radius, y + radius), fill=MINT)

canvas.resize((SIZE, SIZE), Image.Resampling.LANCZOS).save(OUTPUT, optimize=True)
print(OUTPUT)
