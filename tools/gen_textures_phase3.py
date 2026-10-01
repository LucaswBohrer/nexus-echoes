#!/usr/bin/env python3
"""Generate placeholder 16x16 textures for NEXUS Phase 3 (pure stdlib, no PIL)."""
import os, struct, zlib, random

BASE = os.path.expanduser("~/workspace/nexus-echoes/src/main/resources/assets/nexus_echoes/textures")
BLOCK = os.path.join(BASE, "block")
ITEM = os.path.join(BASE, "item")
os.makedirs(BLOCK, exist_ok=True)
os.makedirs(ITEM, exist_ok=True)

def write_png(path, pixels):
    """pixels: list of 16 rows, each a list of 16 (r,g,b,a) tuples."""
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

def noise_tex(base, speck, speck_chance, seed):
    rnd = random.Random(seed)
    rows = []
    for y in range(16):
        row = []
        for x in range(16):
            v = rnd.randint(-12, 12)
            c = tuple(max(0, min(255, b + v)) for b in base)
            if rnd.random() < speck_chance:
                c = speck
            row.append(c + (255,))
        rows.append(row)
    return rows

def machine_tex(base, line, seed):
    rnd = random.Random(seed)
    rows = []
    for y in range(16):
        row = []
        for x in range(16):
            v = rnd.randint(-8, 8)
            c = tuple(max(0, min(255, b + v)) for b in base)
            if x in (0, 15) or y in (0, 15) or (x == 7 and 3 <= y <= 12):
                c = line
            row.append(c + (255,))
        rows.append(row)
    return rows

def plate_tex(base, edge, seed):
    """Flat plate: border + rivets."""
    rnd = random.Random(seed)
    rows = []
    for y in range(16):
        row = []
        for x in range(16):
            v = rnd.randint(-6, 6)
            c = tuple(max(0, min(255, b + v)) for b in base)
            if x in (0, 15) or y in (0, 15):
                c = edge
            if (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)):
                c = (20, 22, 30)
            row.append(c + (255,))
        rows.append(row)
    return rows

def component_tex(base, accent, seed):
    """Mechanical component: gear-ish cross on dark base."""
    rnd = random.Random(seed)
    rows = []
    for y in range(16):
        row = []
        for x in range(16):
            v = rnd.randint(-6, 6)
            c = tuple(max(0, min(255, b + v)) for b in base)
            dx, dy = abs(x - 7.5), abs(y - 7.5)
            if dx < 2 or dy < 2:
                c = accent
            if dx + dy < 2.5:
                c = (200, 220, 235)
            row.append(c + (255,))
        rows.append(row)
    return rows

write_png(f"{BLOCK}/crusher.png", machine_tex((52, 50, 58), (200, 120, 60), 101))
write_png(f"{BLOCK}/processor.png", machine_tex((38, 46, 60), (53, 224, 230), 102))
write_png(f"{ITEM}/nexus_dust.png", noise_tex((90, 95, 110), (53, 224, 230), 0.06, 103))
write_png(f"{ITEM}/refined_nexus.png", noise_tex((60, 80, 110), (120, 240, 245), 0.12, 104))
write_png(f"{ITEM}/nexus_plate.png", plate_tex((70, 90, 120), (30, 40, 55), 105))
write_png(f"{ITEM}/nexus_component.png", component_tex((45, 50, 62), (150, 160, 175), 106))
