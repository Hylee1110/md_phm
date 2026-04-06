"""Copy the 6 generated course cover PNGs from Cursor assets into static/photos.

Run once from project root:
  python scripts/copy_course_images.py
"""
from __future__ import annotations

import shutil
from pathlib import Path

SRC = Path(r"C:\Users\31649\.cursor\projects\d-Code-md-phms\assets")
DST = Path(__file__).resolve().parent.parent / "static" / "photos"

NAMES = [
    "运动推荐-晨间快走.png",
    "运动推荐-舒缓拉伸.png",
    "运动推荐-基础核心训练.png",
    "运动推荐-室内单车入门.png",
    "运动推荐-慢跑耐力课.png",
    "运动推荐-徒手HIIT轻量.png",
]


def main() -> None:
    DST.mkdir(parents=True, exist_ok=True)
    ok = 0
    for name in NAMES:
        src = SRC / name
        if not src.is_file():
            print(f"MISSING: {src}")
            continue
        shutil.copy2(src, DST / name)
        print(f"OK: {name}")
        ok += 1
    print(f"Done: {ok}/{len(NAMES)} -> {DST}")


if __name__ == "__main__":
    main()
