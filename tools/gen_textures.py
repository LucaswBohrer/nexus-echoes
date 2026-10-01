#!/usr/bin/env python3
"""Generate placeholder 16x16 textures for NEXUS Phase 1 (pure stdlib, no PIL)."""
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

def crystal_tex(core, edge, seed):
    rnd = random.Random(seed)
    rows = []
    for y in range(16):
        row = []
        for x in range(16):
            dx, dy = abs(x - 7.5), abs(y - 7.5)
            d = dx + dy
            if d < 3:
                c = core
            elif d < 6:
                c = tuple((a + b) // 2 for a, b in zip(core, edge))
            else:
                c = (10, 12, 18)
            if rnd.random() < 0.08:
                c = (255, 255, 255)
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

write_png(f"{BLOCK}/nexus_ore.png", noise_tex((110, 110, 115), (53, 224, 230), 0.10, 42))
write_png(f"{BLOCK}/resonator.png", machine_tex((38, 44, 58), (53, 224, 230), 7))
write_png(f"{BLOCK}/creative_energy_cell.png", machine_tex((20, 26, 40), (120, 240, 245), 13))
write_png(f"{ITEM}/nexus_shard.png", crystal_tex((53, 224, 230), (20, 90, 110), 21))
write_png(f"{ITEM}/resonant_crystal.png", crystal_tex((150, 245, 250), (40, 140, 160), 22))
