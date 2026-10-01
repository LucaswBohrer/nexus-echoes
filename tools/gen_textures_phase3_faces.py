#!/usr/bin/env python3
"""Generate per-face 16x16 placeholder textures for NEXUS Phase 3 machines.

Pure stdlib (no PIL), same PNG writer style as gen_textures_phase3.py.

Faces follow the vanilla ``minecraft:block/orientable`` convention:
  front -> the machine's working face (north at facing=north)
  side  -> left/right/back
  top   -> up AND down (orientable maps #top to the down face)
"""
import os, struct, zlib, random

BASE = os.path.expanduser("~/workspace/nexus-echoes/src/main/resources/assets/nexus_echoes/textures")
BLOCK = os.path.join(BASE, "block")
os.makedirs(BLOCK, exist_ok=True)


def write_png(path, pixels):
    raw = b"".join(b"\x00" + b"".join(struct.pack("4B", *p) for p in row) for row in pixels)

    def chunk(typ, data):
        c = struct.pack(">I", len(data)) + typ + data
        return c + struct.pack(">I", zlib.crc32(typ + data) & 0xFFFFFFFF)

    png = (b"\x89PNG\r\n\x1a\n"
           + chunk(b"IHDR", struct.pack(">IIBBBBB", 16, 16, 8, 6, 0, 0, 0))
           + chunk(b"IDAT", zlib.compress(raw, 9))
           + chunk(b"IEND", b""))
    with open(path, "wb") as f:
        f.write(png)
    print("wrote", path)


def base_grid(base, seed, spread=7):
    """16x16 noise grid of (r,g,b) tuples."""
    rnd = random.Random(seed)
    return [[tuple(max(0, min(255, b + rnd.randint(-spread, spread))) for b in base)
             for _ in range(16)] for _ in range(16)]


def frame(grid, color, width=1):
    for i in range(16):
        for w in range(width):
            grid[w][i] = color
            grid[15 - w][i] = color
            grid[i][w] = color
            grid[i][15 - w] = color


def rivets(grid, dark=(16, 15, 20)):
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
        grid[y][x] = dark


def rect(grid, x0, y0, x1, y1, color):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            grid[y][x] = color


def to_rgba(grid):
    return [[c + (255,) for c in row] for row in grid]


# ---------------------------------------------------------------- Crusher
STEEL = (56, 54, 62)
DARK = (24, 22, 28)
ORANGE = (200, 120, 60)
TOOTH = (150, 148, 158)
CHAMBER = (10, 9, 12)

# -- crusher_side: riveted panels, vent slots, thin warning stripe
g = base_grid(STEEL, 201)
frame(g, (28, 26, 32))
rivets(g)
rect(g, 2, 2, 13, 2, ORANGE)                       # warning stripe under top frame
rect(g, 2, 8, 13, 8, (34, 32, 38))                 # horizontal panel seam
for vx in (3, 9):                                   # vent slots
    rect(g, vx, 10, vx + 3, 12, (14, 13, 16))
    rect(g, vx, 10, vx + 3, 10, (70, 68, 76))       # top lip highlight
write_png(f"{BLOCK}/crusher_side.png", to_rgba(g))

# -- crusher_front: crushing chamber with teeth + hazard stripes
g = base_grid(STEEL, 202)
frame(g, DARK, width=2)
rivets(g)
rect(g, 4, 4, 11, 11, CHAMBER)                      # chamber opening
for x in range(4, 12):                              # upper teeth (point down)
    if x % 2 == 0:
        g[4][x] = TOOTH
    else:
        g[5][x] = TOOTH
for x in range(4, 12):                              # lower teeth (point up)
    if x % 2 == 0:
        g[11][x] = TOOTH
    else:
        g[10][x] = TOOTH
for x in (2, 3, 12, 13):                            # hazard stripes flanking chamber
    for y in range(4, 12):
        g[y][x] = ORANGE if (x + y) % 2 == 0 else DARK
write_png(f"{BLOCK}/crusher_front.png", to_rgba(g))

# -- crusher_top: input hopper funnel
g = base_grid(STEEL, 203)
frame(g, (28, 26, 32))
rivets(g)
rect(g, 4, 4, 11, 11, (35, 33, 40))                 # hopper outer
rect(g, 4, 4, 11, 4, (95, 93, 103))                 # bevel highlight (top/left)
rect(g, 4, 4, 4, 11, (95, 93, 103))
rect(g, 6, 6, 9, 9, (8, 7, 10))                     # dark throat
write_png(f"{BLOCK}/crusher_top.png", to_rgba(g))

# -------------------------------------------------------------- Processor
BLUE = (42, 50, 64)
BFRAME = (24, 28, 38)
CYAN = (53, 224, 230)
CYAN_DIM = (40, 150, 160)
SCREEN = (8, 12, 18)

# -- processor_side: circuit traces + vent slots
g = base_grid(BLUE, 204)
frame(g, BFRAME)
rivets(g)
for x in range(2, 9):                               # trace 1: L shape
    g[4][x] = CYAN_DIM
for y in range(4, 11):
    g[y][8] = CYAN_DIM
rect(g, 7, 10, 8, 11, (200, 240, 245))              # pad
for x in range(9, 14):                              # trace 2: mirrored L
    g[6][x] = CYAN_DIM
for y in range(6, 13):
    g[y][9] = CYAN_DIM
rect(g, 8, 12, 9, 13, (200, 240, 245))              # pad
rect(g, 3, 9, 4, 12, (14, 16, 22))                  # vent slots
rect(g, 3, 9, 4, 9, (80, 88, 102))
write_png(f"{BLOCK}/processor_side.png", to_rgba(g))

# -- processor_front: screen, status LEDs, output slit
g = base_grid(BLUE, 205)
frame(g, BFRAME)
rivets(g)
rect(g, 3, 2, 12, 6, SCREEN)                        # display
for x in range(4, 9):
    g[3][x] = CYAN                                  # bright line
for x in range(4, 7):
    g[5][x] = CYAN_DIM                              # dim line
leds = {4: (60, 220, 120), 6: (230, 170, 60), 8: (220, 70, 70), 10: (20, 24, 32), 11: (20, 24, 32)}
for x, c in leds.items():
    g[8][x] = c                                     # status LEDs (last two off)
rect(g, 3, 10, 12, 10, (70, 78, 92))                # label plate line
rect(g, 4, 12, 11, 13, (10, 10, 14))                # output slit
write_png(f"{BLOCK}/processor_front.png", to_rgba(g))

# -- processor_top: lid with exhaust grille + cyan stripe
g = base_grid(BLUE, 206)
frame(g, BFRAME)
rivets(g)
rect(g, 3, 2, 12, 2, CYAN_DIM)                      # accent stripe
for y in range(5, 11):                              # exhaust grille
    for x in range(5, 11):
        if (x + y) % 2 == 0:
            g[y][x] = (15, 18, 24)
rect(g, 5, 5, 10, 5, (80, 88, 102))                 # grille top highlight
write_png(f"{BLOCK}/processor_top.png", to_rgba(g))

# Remove the old single-texture placeholders (replaced by per-face sprites).
for old in ("crusher.png", "processor.png"):
    p = os.path.join(BLOCK, old)
    if os.path.exists(p):
        os.remove(p)
        print("removed", p)
