"""Turns the frames IconEffectClips rendered into one animated GIF per icon effect (I4.1).

    bash tools/gradle-locked.sh :app:testDebugUnitTest --tests "*IconEffectClips" -PiconClips=<dir>
    python tools/icon-clips.py <dir> <out-dir>
"""
import os
import re
import sys

from PIL import Image

src, out = sys.argv[1], sys.argv[2]
os.makedirs(out, exist_ok=True)
names = open(os.path.join(src, "names.txt"), encoding="utf-8").read().splitlines()
for i, name in enumerate(names):
    d = os.path.join(src, str(i))
    files = sorted(f for f in os.listdir(d) if f.endswith(".png"))
    frames = [Image.open(os.path.join(d, f)).convert("RGB") for f in files]
    slug = re.sub(r"[^a-z0-9]+", "-", name.lower()).strip("-")
    path = os.path.join(out, f"{i:02d}-{slug}.gif")
    frames[0].save(path, save_all=True, append_images=frames[1:], duration=33, loop=0)
    print(path)
