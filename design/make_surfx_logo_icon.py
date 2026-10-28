"""Render the SurfX wave-and-sun app icon at exactly 512 × 512."""

from pathlib import Path
import math
from PIL import Image, ImageDraw, ImageFilter


SIZE = 512
SCALE = 4
W = H = SIZE * SCALE
ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "surfx_logo_icon_512.png"


def cubic(p0, p1, p2, p3, steps=180):
    points = []
    for i in range(steps + 1):
        t = i / steps
        u = 1 - t
        x = u**3 * p0[0] + 3 * u**2 * t * p1[0] + 3 * u * t**2 * p2[0] + t**3 * p3[0]
        y = u**3 * p0[1] + 3 * u**2 * t * p1[1] + 3 * u * t**2 * p2[1] + t**3 * p3[1]
        points.append((round(x * SCALE), round(y * SCALE)))
    return points


def blend(a, b, t):
    return tuple(round(a[i] * (1 - t) + b[i] * t) for i in range(3))


# Deep ocean background with a restrained mint glow behind the mark.
pixels = Image.new("RGB", (W, H))
px = pixels.load()
for y in range(H):
    for x in range(W):
        nx, ny = x / SCALE, y / SCALE
        d = math.hypot((nx - 333) / 310, (ny - 245) / 310)
        glow = max(0.0, 1.0 - d) ** 2 * 0.17
        px[x, y] = blend((6, 22, 27), (23, 75, 68), glow)

mask = Image.new("L", (W, H), 0)
draw = ImageDraw.Draw(mask)
for band in range(6):
    offset = band * 19
    points = cubic(
        (91, 275 + offset),
        (180, 321 + offset),
        (263, 171 + offset),
        (405, 224 + offset),
    )
    draw.line(points, fill=255, width=12 * SCALE, joint="curve")
    radius = 6 * SCALE
    for x, y in (points[0], points[-1]):
        draw.ellipse((x - radius, y - radius, x + radius, y + radius), fill=255)

gradient = Image.new("RGB", (W, H))
g = gradient.load()
for x in range(W):
    t = x / max(1, W - 1)
    color = blend((21, 175, 174), (106, 255, 210), min(1.0, t * 1.2))
    for y in range(H):
        g[x, y] = color

glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
glow.paste((57, 236, 193, 0), (0, 0, W, H))
glow.putalpha(mask.filter(ImageFilter.GaussianBlur(18 * SCALE)).point(lambda a: int(a * 0.18)))
pixels = Image.alpha_composite(pixels.convert("RGBA"), glow)
pixels.paste(gradient, (0, 0), mask)

sun = Image.new("RGBA", (W, H), (0, 0, 0, 0))
sd = ImageDraw.Draw(sun)
cx, cy, r = 360 * SCALE, 158 * SCALE, 31 * SCALE
sd.ellipse((cx - r, cy - r, cx + r, cy + r), fill=(99, 248, 205, 255))
sun_glow = sun.filter(ImageFilter.GaussianBlur(15 * SCALE))
sun_glow.putalpha(sun_glow.getchannel("A").point(lambda a: int(a * 0.22)))
pixels = Image.alpha_composite(pixels, sun_glow)
pixels = Image.alpha_composite(pixels, sun)

pixels.convert("RGB").resize((SIZE, SIZE), Image.Resampling.LANCZOS).save(OUTPUT, optimize=True)
print(OUTPUT)
