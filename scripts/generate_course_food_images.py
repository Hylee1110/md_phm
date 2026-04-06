import os
import re
import hashlib
from dataclasses import dataclass

from PIL import Image, ImageDraw, ImageFont


@dataclass(frozen=True)
class ListsSpec:
    markdown_path: str
    output_dir: str


def _project_root() -> str:
    # scripts/.. -> project root (d:\Code\md_phms)
    return os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))


def _read_lines(path: str) -> list[str]:
    with open(path, "r", encoding="utf-8") as f:
        return f.read().splitlines()


def parse_names_from_markdown(markdown_path: str) -> tuple[list[str], list[str]]:
    """
    Parse:
      - 运动课程名称（24个）
      - 食谱食物名称（24个）

    Expect rows like:
      | 1 | `运动推荐-晨间快走` |
    """
    lines = _read_lines(markdown_path)

    start_course = None
    start_food = None
    for i, line in enumerate(lines):
        if line.strip() == "## 运动课程名称（24 个）":
            start_course = i
        elif line.strip() == "## 食谱食物名称（24 个）":
            start_food = i

    if start_course is None or start_food is None:
        raise ValueError("Markdown 标题未找到：运动课程名称 / 食谱食物名称")

    course_rows = []
    food_rows = []

    row_re = re.compile(r"^\|\s*\d+\s*\|\s*`([^`]+)`\s*\|\s*$")

    for i in range(start_course + 1, start_food):
        m = row_re.match(lines[i].strip())
        if m:
            course_rows.append(m.group(1))

    # food rows until end-of-file (current markdown template ends right after table)
    for i in range(start_food + 1, len(lines)):
        m = row_re.match(lines[i].strip())
        if m:
            food_rows.append(m.group(1))

    return course_rows, food_rows


def sanitize_filename(name: str) -> str:
    # Windows disallows: \ / : * ? " < > |
    sanitized = re.sub(r'[\\/:*?"<>|]', "_", name)
    sanitized = sanitized.strip()
    return sanitized


def pick_font(size: int) -> ImageFont.FreeTypeFont:
    candidates = [
        r"C:\Windows\Fonts\msyh.ttc",      # Microsoft YaHei
        r"C:\Windows\Fonts\msyhbd.ttc",    # Microsoft YaHei Bold
        r"C:\Windows\Fonts\simsun.ttc",    # SimSun
        r"C:\Windows\Fonts\simhei.ttf",    # SimHei (sometimes)
        r"C:\Windows\Fonts\simhei.TTF",
    ]
    for p in candidates:
        if os.path.exists(p):
            return ImageFont.truetype(p, size=size)
    # Fallback (will likely fail for Chinese on some environments)
    return ImageFont.load_default()


def wrap_text(draw: ImageDraw.ImageDraw, text: str, font: ImageFont.ImageFont, max_width: int) -> list[str]:
    # Simple greedy wrap at character level (works for Chinese).
    if draw.textlength(text, font=font) <= max_width:
        return [text]

    lines = []
    current = ""
    for ch in text:
        tentative = current + ch
        if tentative and draw.textlength(tentative, font=font) <= max_width:
            current = tentative
        else:
            if current:
                lines.append(current)
            current = ch
    if current:
        lines.append(current)
    return lines


def gradient_background(w: int, h: int, name: str) -> Image.Image:
    # Deterministic gradient from hash
    digest = hashlib.md5(name.encode("utf-8")).hexdigest()
    v = int(digest[:8], 16)
    hue1 = v % 360
    hue2 = (hue1 + 50 + (v % 80)) % 360

    def hsv_to_rgb(h: float, s: float, vv: float) -> tuple[int, int, int]:
        c = vv * s
        x = c * (1 - abs(((h / 60) % 2) - 1))
        m = vv - c
        if 0 <= h < 60:
            r1, g1, b1 = c, x, 0
        elif 60 <= h < 120:
            r1, g1, b1 = x, c, 0
        elif 120 <= h < 180:
            r1, g1, b1 = 0, c, x
        elif 180 <= h < 240:
            r1, g1, b1 = 0, x, c
        elif 240 <= h < 300:
            r1, g1, b1 = x, 0, c
        else:
            r1, g1, b1 = c, 0, x
        r = int((r1 + m) * 255)
        g = int((g1 + m) * 255)
        b = int((b1 + m) * 255)
        return r, g, b

    c1 = hsv_to_rgb(hue1, 0.55, 0.90)
    c2 = hsv_to_rgb(hue2, 0.55, 0.90)

    img = Image.new("RGB", (w, h), c1)
    px = img.load()
    for y in range(h):
        t = y / (h - 1) if h > 1 else 0
        r = int(c1[0] * (1 - t) + c2[0] * t)
        g = int(c1[1] * (1 - t) + c2[1] * t)
        b = int(c1[2] * (1 - t) + c2[2] * t)
        for x in range(w):
            px[x, y] = (r, g, b)
    return img


def draw_running_icon(draw: ImageDraw.ImageDraw, cx: int, cy: int, scale: int, fill=(10, 20, 30)) -> None:
    # Minimal stick-figure running icon (no external assets)
    # Head
    r = 10 * scale // 64
    draw.ellipse((cx - r, cy - 70 * scale // 64, cx + r, cy - 70 * scale // 64 + 2 * r), fill=fill)

    # Body
    body_top = cy - 60 * scale // 64 + 2 * r
    body_bottom = cy + 10 * scale // 64
    draw.line((cx, body_top, cx, body_bottom), fill=fill, width=6 * scale // 64)

    # Arms (slanted)
    arm_w = 4 * scale // 64
    draw.line((cx - 30 * scale // 64, body_top + 10, cx - 10 * scale // 64, cy + 0), fill=fill, width=arm_w)
    draw.line((cx + 30 * scale // 64, body_top + 10, cx + 10 * scale // 64, cy + 0), fill=fill, width=arm_w)

    # Legs
    leg_w = 6 * scale // 64
    draw.line((cx, body_bottom, cx - 25 * scale // 64, cy + 55 * scale // 64), fill=fill, width=leg_w)
    draw.line((cx, body_bottom, cx + 25 * scale // 64, cy + 55 * scale // 64), fill=fill, width=leg_w)


def render_card(name: str, out_path: str, size: int = 1024) -> None:
    w = h = size
    img = gradient_background(w, h, name)
    draw = ImageDraw.Draw(img)

    # Layout
    icon_cx = w // 2
    icon_cy = h // 4
    draw_running_icon(draw, icon_cx, icon_cy, scale=size, fill=(10, 20, 30))

    max_text_width = int(w * 0.86)

    # Try different font sizes to fit
    font_size = 96
    font = pick_font(font_size)
    while font_size > 40:
        font = pick_font(font_size)
        lines = wrap_text(draw, name, font, max_text_width)
        # Total height check
        line_height = int(font_size * 1.15)
        total_h = len(lines) * line_height
        if total_h <= int(h * 0.45):
            break
        font_size -= 6
    else:
        lines = wrap_text(draw, name, font, max_text_width)
        line_height = int(font_size * 1.15)
        total_h = len(lines) * line_height

    # Center text block
    text_block_top = int(h * 0.36)
    fill_text = (10, 20, 30)
    shadow = (255, 255, 255, 90)

    # Shadow
    for i, line in enumerate(lines):
        y = text_block_top + i * line_height
        draw.text((w // 2 - draw.textlength(line, font=font) / 2 + 6, y + 6), line, font=font, fill=(0, 0, 0, 60))

    # Main text
    for i, line in enumerate(lines):
        y = text_block_top + i * line_height
        draw.text((w // 2 - draw.textlength(line, font=font) / 2, y), line, font=font, fill=fill_text)

    os.makedirs(os.path.dirname(out_path), exist_ok=True)
    img.save(out_path, format="PNG")


def main() -> None:
    root = _project_root()
    spec = ListsSpec(
        markdown_path=os.path.join(root, "docs", "运动课程与食谱食物名称清单.md"),
        output_dir=os.path.join(root, "static", "photos"),
    )

    course_names, food_names = parse_names_from_markdown(spec.markdown_path)
    names = [("运动课程", n) for n in course_names] + [("食谱食物", n) for n in food_names]

    if not names:
        raise RuntimeError("未解析到任何名称，请检查 Markdown 表格格式。")

    print(f"共要生成图片：{len(names)} 张")
    for kind, name in names:
        filename = sanitize_filename(name) + ".png"
        out_path = os.path.join(spec.output_dir, filename)
        render_card(name=name, out_path=out_path, size=1024)
        print(f"[OK] {kind}: {name} -> {out_path}")


if __name__ == "__main__":
    main()

