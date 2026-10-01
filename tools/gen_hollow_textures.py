#!/usr/bin/env python3
"""Procedural 16x16 placeholder textures for NEXUS Phase 5 (The Hollow).

Vertical-slice placeholders: coherent palettes, deterministic (seeded),
readable at 16x16. Matches the project's early-dev placeholder policy.
Run: python3 tools/gen_hollow_textures.py
"""
import os
import random
import struct
import zlib

SIZE = 16
OUT = "src/main/resources/assets/nexus_echoes/textures"


def write_png(path, pixels):
    def chunk(typ, data):
        c = struct.pack(">I", len(data)) + typ + data
        return c + struct.pack(">I", zlib.crc32(typ + data) & 0xFFFFFFFF)

    raw = b"".join(b"\x00" + b"".join(
        struct.pack("BBB", *px) for px in row) for row in pixels)
    png = (b"\x89PNG\r\n\x1a\n"
           + chunk(b"IHDR", struct.pack(">IIBBBBB", SIZE, SIZE, 8, 2, 0, 0, 0))
           + chunk(b"IDAT", zlib.compress(raw))
           + chunk(b"IEND", b""))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(png)


def base_tex(seed, palette, speckle=None, speckle_chance=0.0, bands=None):
    """palette: list of (rgb, weight). speckle: list of rgb to sprinkle."""
    rng = random.Random(seed)
    total = sum(w for _, w in palette)
    px = []
    for y in range(SIZE):
        row = []
        for x in range(SIZE):
            r = rng.random() * total
            col = palette[0][0]
            for rgb, w in palette:
                r -= w
                if r <= 0:
                    col = rgb
                    break
            if bands:
                col = bands(y, x, col, rng)
            if speckle and rng.random() < speckle_chance:
                col = rng.choice(speckle)
            # slight per-pixel brightness jitter
            j = rng.uniform(0.92, 1.08)
            row.append(tuple(max(0, min(255, int(c * j))) for c in col))
        px.append(row)
    return px


def border(px, color, width=1):
    for y in range(SIZE):
        for x in range(SIZE):
            if x < width or y < width or x >= SIZE - width or y >= SIZE - width:
                px[y][x] = color
    return px


def save(name, px, sub="block"):
    write_png(f"{OUT}/{sub}/{name}.png", px)
    print("tex", sub, name)


GRAY = (110, 100, 130)
DARK = (70, 60, 90)

# --- terrain -----------------------------------------------------------------
save("hollow_stone", base_tex(11, [((96, 88, 118), 5), ((78, 70, 100), 3), ((120, 110, 140), 1)],
                               speckle=[(60, 52, 80)], speckle_chance=0.06))

save("rusted_plating", border(
    base_tex(12, [((150, 90, 50), 4), ((130, 75, 42), 3), ((170, 105, 60), 2)],
             speckle=[(90, 55, 30), (60, 40, 25)], speckle_chance=0.10),
    (70, 45, 28)))

save("ashen_soil", base_tex(13, [((140, 135, 150), 5), ((120, 115, 132), 3)],
                            speckle=[(90, 88, 100)], speckle_chance=0.08))

save("hollow_ore", base_tex(14, [((96, 88, 118), 5), ((78, 70, 100), 3)],
                            speckle=[(120, 220, 200), (90, 200, 180)], speckle_chance=0.12))

# --- spire -------------------------------------------------------------------
def spire_bands(y, x, col, rng):
    if x in (7, 8) and rng.random() < 0.7:
        return (150, 240, 220)  # glowing core vein
    return col

save("dimensional_spire_side",
     base_tex(15, [((60, 40, 110), 4), ((48, 32, 92), 3)], bands=spire_bands))
save("dimensional_spire_top",
     base_tex(16, [((70, 50, 130), 1)], speckle=[(150, 240, 220)], speckle_chance=0.25))

# --- obelisk -----------------------------------------------------------------
save("obelisk_core", border(
    base_tex(17, [((25, 20, 40), 1)],
             speckle=[(160, 120, 255), (120, 80, 220)], speckle_chance=0.18),
    (10, 8, 20), width=2))

# --- anomaly ward (machine faces) ---------------------------------------------
def ward_front(y, x, col, rng):
    if 5 <= x <= 10 and 5 <= y <= 10:
        return (140, 230, 210) if rng.random() < 0.85 else (90, 160, 150)
    if x in (0, 15) or y in (0, 15):
        return (40, 45, 55)
    return col

ward_side = base_tex(18, [((85, 90, 105), 4), ((70, 75, 90), 2)],
                     speckle=[(50, 55, 65)], speckle_chance=0.06)
save("anomaly_ward_side", ward_side)
save("anomaly_ward_top", base_tex(19, [((85, 90, 105), 1)],
                                  speckle=[(140, 230, 210)], speckle_chance=0.10))
save("anomaly_ward_front", base_tex(20, [((85, 90, 105), 4)], bands=ward_front))

# --- unstable fracture ---------------------------------------------------------
save("unstable_fracture", base_tex(24, [((90, 40, 130), 3), ((70, 30, 105), 2)],
                                    speckle=[(220, 150, 255), (160, 90, 220)],
                                    speckle_chance=0.16))

# --- resonant growth (cross) ----------------------------------------------------
growth = [[(0, 0, 0)] * SIZE for _ in range(SIZE)]
rng = random.Random(25)
for y in range(SIZE):
    for x in range(SIZE):
        # two diagonal stalks
        if abs(x - y) <= 1 or abs(x - (SIZE - 1 - y)) <= 1:
            if rng.random() < 0.85:
                growth[y][x] = (60, 220, 170) if rng.random() < 0.7 else (40, 160, 130)
        elif rng.random() < 0.04:
            growth[y][x] = (90, 240, 190)
# note: cross model needs transparency; placeholder uses dark bg, acceptable v-slice
for y in range(SIZE):
    for x in range(SIZE):
        if growth[y][x] == (0, 0, 0):
            growth[y][x] = (12, 18, 22)
save("resonant_growth", growth)

# --- items ----------------------------------------------------------------------
def item_blob(seed, colors, bg=(0, 0, 0)):
    rng = random.Random(seed)
    px = [[bg] * SIZE for _ in range(SIZE)]
    cx, cy = 7.5, 7.5
    for y in range(SIZE):
        for x in range(SIZE):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if d < 5.5 + rng.uniform(-1, 1):
                px[y][x] = rng.choice(colors)
    return px

save("memory_fragment", item_blob(26, [(150, 230, 210), (110, 190, 175), (190, 250, 235)]), "item")
save("resonant_shard", item_blob(27, [(120, 220, 200), (90, 190, 170), (150, 240, 220)]), "item")

# scanner: rounded rect body + screen
rng = random.Random(28)
px = [[(0, 0, 0)] * SIZE for _ in range(SIZE)]
for y in range(3, 13):
    for x in range(2, 14):
        px[y][x] = (70, 75, 90)
for y in range(5, 10):
    for x in range(4, 12):
        px[y][x] = (40, 200, 170) if rng.random() < 0.8 else (30, 150, 130)
px[11][7] = px[11][8] = (180, 120, 60)
save("resonance_scanner", px, "item")

print("all textures done")
