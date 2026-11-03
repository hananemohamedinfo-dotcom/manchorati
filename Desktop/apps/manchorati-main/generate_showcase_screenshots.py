import os
import math
from PIL import Image, ImageDraw, ImageFont, ImageFilter
import arabic_reshaper
from bidi.algorithm import get_display

RAW_DIR = r"c:\Users\X4P\Desktop\apps\application test\manchorati-main\raw_captures"
OUTPUT_DIR = r"C:\Users\X4P\Desktop\Manchorati_GooglePlay_Release\screenshots"
os.makedirs(OUTPUT_DIR, exist_ok=True)

CANVAS_W = 1080
CANVAS_H = 2400

# Fonts
FONT_TITLE = ImageFont.truetype(r"C:\Windows\Fonts\segoeuib.ttf", 52)
FONT_SUBTITLE = ImageFont.truetype(r"C:\Windows\Fonts\segoeui.ttf", 32)
FONT_PILL = ImageFont.truetype(r"C:\Windows\Fonts\segoeuib.ttf", 26)
FONT_EMOJI_PILL = ImageFont.truetype(r"C:\Windows\Fonts\seguiemj.ttf", 26)

def ar(text):
    return get_display(arabic_reshaper.reshape(text))

def create_gradient(w, h, top_rgb, bottom_rgb, glow_rgb=None):
    base = Image.new("RGBA", (w, h))
    draw = ImageDraw.Draw(base)
    for y in range(h):
        ratio = y / float(h)
        t = ratio * ratio * (3 - 2 * ratio)
        r = int(top_rgb[0] * (1 - t) + bottom_rgb[0] * t)
        g = int(top_rgb[1] * (1 - t) + bottom_rgb[1] * t)
        b = int(top_rgb[2] * (1 - t) + bottom_rgb[2] * t)
        draw.line([(0, y), (w, y)], fill=(r, g, b, 255))
    
    # Ambient radial glow behind phone
    if glow_rgb:
        glow = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        gdraw = ImageDraw.Draw(glow)
        center_x = w // 2
        center_y = int(h * 0.50)
        radius = 580
        for rad in range(radius, 0, -15):
            alpha = int(50 * (1 - rad / radius))
            gdraw.ellipse(
                [center_x - rad, center_y - rad, center_x + rad, center_y + rad],
                fill=(glow_rgb[0], glow_rgb[1], glow_rgb[2], alpha)
            )
        glow = glow.filter(ImageFilter.GaussianBlur(50))
        base = Image.alpha_composite(base, glow)

    return base

def draw_pill(canvas, cx, y, text_ar, emoji_str, fill_rgba, border_rgba, text_color):
    draw = ImageDraw.Draw(canvas)
    reshaped_text = ar(text_ar)
    
    t_bbox = draw.textbbox((0, 0), reshaped_text, font=FONT_PILL)
    tw = t_bbox[2] - t_bbox[0]
    
    e_bbox = draw.textbbox((0, 0), emoji_str, font=FONT_EMOJI_PILL)
    ew = e_bbox[2] - e_bbox[0]
    
    spacing = 14
    total_w = tw + ew + spacing + 48
    pill_h = 56
    
    px1 = cx - total_w // 2
    py1 = y
    px2 = px1 + total_w
    py2 = py1 + pill_h
    
    pill_overlay = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    pdraw = ImageDraw.Draw(pill_overlay)
    pdraw.rounded_rectangle([px1, py1, px2, py2], radius=28, fill=fill_rgba, outline=border_rgba, width=2)
    canvas.alpha_composite(pill_overlay)
    
    draw = ImageDraw.Draw(canvas)
    draw.text((px1 + 22, py1 + 13), emoji_str, font=FONT_EMOJI_PILL, embedded_color=True)
    draw.text((px1 + 22 + ew + spacing, py1 + 11), reshaped_text, font=FONT_PILL, fill=text_color)

def crop_and_frame_screenshot(raw_img_path, crop_box, target_screen_w=854):
    raw_im = Image.open(raw_img_path)
    cropped = raw_im.crop(crop_box)
    
    cw, ch = cropped.size
    aspect = ch / float(cw)
    target_screen_h = int(target_screen_w * aspect)
    
    resized_screen = cropped.resize((target_screen_w, target_screen_h), Image.Resampling.LANCZOS)
    
    bezel_h = 16
    bezel_top = 22
    bezel_bottom = 22
    phone_w = target_screen_w + (bezel_h * 2)
    phone_h = target_screen_h + bezel_top + bezel_bottom
    
    phone_img = Image.new("RGBA", (phone_w, phone_h), (0, 0, 0, 0))
    pdraw = ImageDraw.Draw(phone_img)
    
    # Outer titanium bezel
    pdraw.rounded_rectangle(
        [0, 0, phone_w, phone_h],
        radius=50,
        fill=(20, 20, 25, 255),
        outline=(75, 85, 100, 255),
        width=3
    )
    
    # Inner border line for 3D bezel depth
    pdraw.rounded_rectangle(
        [3, 3, phone_w - 3, phone_h - 3],
        radius=47,
        outline=(30, 42, 60, 200),
        width=1
    )
    
    # Top speaker slit
    speaker_w = 70
    speaker_x = (phone_w - speaker_w) // 2
    speaker_y = 9
    pdraw.rounded_rectangle([speaker_x, speaker_y, speaker_x + speaker_w, speaker_y + 4], radius=2, fill=(60, 70, 85, 240))
    
    # Rounded screen corner mask
    screen_mask = Image.new("L", (target_screen_w, target_screen_h), 0)
    sdraw = ImageDraw.Draw(screen_mask)
    sdraw.rounded_rectangle([0, 0, target_screen_w, target_screen_h], radius=32, fill=255)
    
    # Composite screen
    phone_img.paste(resized_screen, (bezel_h, bezel_top), screen_mask)
    return phone_img

def render_showcase(spec):
    print(f"Rendering: {spec['output_name']}...")
    canvas = create_gradient(
        CANVAS_W, CANVAS_H,
        spec["bg_top"], spec["bg_bottom"],
        spec.get("glow_rgb")
    )
    
    # 1. Header Pill
    draw_pill(
        canvas,
        cx=CANVAS_W // 2,
        y=90,
        text_ar=spec["pill_text"],
        emoji_str=spec["pill_emoji"],
        fill_rgba=spec["pill_fill"],
        border_rgba=spec["pill_border"],
        text_color=spec["pill_text_color"]
    )
    
    # 2. Header Title
    draw = ImageDraw.Draw(canvas)
    title_ar = ar(spec["title"])
    t_bbox = draw.textbbox((0, 0), title_ar, font=FONT_TITLE)
    tw = t_bbox[2] - t_bbox[0]
    tx = (CANVAS_W - tw) // 2
    ty = 180
    draw.text((tx, ty + 3), title_ar, font=FONT_TITLE, fill=(0, 0, 0, 160))
    draw.text((tx, ty), title_ar, font=FONT_TITLE, fill=(255, 255, 255, 255))
    
    # 3. Header Subtitle
    sub_ar = ar(spec["subtitle"])
    s_bbox = draw.textbbox((0, 0), sub_ar, font=FONT_SUBTITLE)
    sw = s_bbox[2] - s_bbox[0]
    sx = (CANVAS_W - sw) // 2
    sy = 265
    draw.text((sx, sy + 2), sub_ar, font=FONT_SUBTITLE, fill=(0, 0, 0, 140))
    draw.text((sx, sy), sub_ar, font=FONT_SUBTITLE, fill=spec.get("subtitle_color", (210, 220, 235, 240)))
    
    # 4. Mockup Phone
    phone_img = crop_and_frame_screenshot(spec["raw_image"], spec["crop_box"], target_screen_w=854)
    pw, ph = phone_img.size
    phone_x = (CANVAS_W - pw) // 2
    phone_y = 430
    
    # 5. Photorealistic Ambient Shadows
    shadow = Image.new("RGBA", (CANVAS_W, CANVAS_H), (0, 0, 0, 0))
    sdraw = ImageDraw.Draw(shadow)
    sdraw.rounded_rectangle(
        [phone_x - 14, phone_y + 25, phone_x + pw + 14, phone_y + ph + 25],
        radius=58,
        fill=(0, 0, 0, 170)
    )
    shadow = shadow.filter(ImageFilter.GaussianBlur(42))
    
    contact_shadow = Image.new("RGBA", (CANVAS_W, CANVAS_H), (0, 0, 0, 0))
    csdraw = ImageDraw.Draw(contact_shadow)
    csdraw.rounded_rectangle(
        [phone_x, phone_y + 12, phone_x + pw, phone_y + ph + 12],
        radius=52,
        fill=(0, 0, 0, 200)
    )
    contact_shadow = contact_shadow.filter(ImageFilter.GaussianBlur(16))
    
    canvas = Image.alpha_composite(canvas, shadow)
    canvas = Image.alpha_composite(canvas, contact_shadow)
    canvas.paste(phone_img, (phone_x, phone_y), phone_img)
    
    # Ensure RGB conversion for Google Play Console compatibility
    final_rgb = canvas.convert("RGB")
    out_path = os.path.join(OUTPUT_DIR, spec["output_name"])
    final_rgb.save(out_path, quality=95)
    print(f"Saved: {out_path} ({final_rgb.size})")

SPECS = [
    {
        "raw_image": os.path.join(RAW_DIR, "1_community_feed.png"),
        "crop_box": (0, 68, 720, 1545),
        "output_name": "screenshot_1_community_feed.png",
        "pill_text": "مجتمع كُتّاب منشوراتي",
        "pill_emoji": "✨",
        "pill_fill": (225, 29, 72, 50),
        "pill_border": (244, 63, 94, 180),
        "pill_text_color": (254, 205, 211),
        "title": "مجتمع ملهم وخاطرة تناسب مزاجك",
        "subtitle": "شارك كتاباتك مع آلاف القراء مع ميزة كيف تشعر اليوم",
        "subtitle_color": (253, 230, 138),
        "bg_top": (95, 12, 38),
        "bg_bottom": (8, 12, 20),
        "glow_rgb": (225, 29, 72)
    },
    {
        "raw_image": os.path.join(RAW_DIR, "2_writers_leaderboard.png"),
        "crop_box": (0, 68, 720, 1545),
        "output_name": "screenshot_2_writers_leaderboard.png",
        "pill_text": "كاتب الأسبوع والمتصدرون",
        "pill_emoji": "👑",
        "pill_fill": (217, 119, 6, 50),
        "pill_border": (251, 191, 36, 180),
        "pill_text_color": (254, 243, 199),
        "title": "تنافس على الصدارة وكن كاتب الأسبوع",
        "subtitle": "لوحة متصدرين حية ورتب شرفية وأوسمة فخرية لقلمك",
        "subtitle_color": (253, 224, 71),
        "bg_top": (78, 30, 4),
        "bg_bottom": (10, 10, 14),
        "glow_rgb": (245, 158, 11)
    },
    {
        "raw_image": os.path.join(RAW_DIR, "3_ai_writer.png"),
        "crop_box": (0, 68, 720, 1545),
        "output_name": "screenshot_3_ai_writer.png",
        "pill_text": "استوديو الذكاء الاصطناعي",
        "pill_emoji": "🤖",
        "pill_fill": (37, 99, 235, 50),
        "pill_border": (96, 165, 250, 180),
        "pill_text_color": (191, 219, 254),
        "title": "كاتب الخواطر بالذكاء الاصطناعي",
        "subtitle": "ولّد خواطر وحكماً أدبية بلمسة زر وبأنماط متنوعة",
        "subtitle_color": (224, 231, 255),
        "bg_top": (28, 28, 85),
        "bg_bottom": (6, 9, 18),
        "glow_rgb": (59, 130, 246)
    },
    {
        "raw_image": os.path.join(RAW_DIR, "4_video_studio.png"),
        "crop_box": (0, 68, 720, 1545),
        "output_name": "screenshot_4_video_studio.png",
        "pill_text": "استوديو ريلز وفيديوهات",
        "pill_emoji": "🎬",
        "pill_fill": (147, 51, 234, 50),
        "pill_border": (192, 132, 252, 180),
        "pill_text_color": (243, 232, 255),
        "title": "صمم فيديوهات وريلز الخواطر المتحركة",
        "subtitle": "حركات تايبوغرافي فاخرة، مقاطع صوتية وتصدير فوري",
        "subtitle_color": (233, 213, 255),
        "bg_top": (88, 25, 130),
        "bg_bottom": (8, 9, 12),
        "glow_rgb": (168, 85, 247)
    },
    {
        "raw_image": os.path.join(RAW_DIR, "5_challenges_and_streaks.png"),
        "crop_box": (0, 68, 720, 1545),
        "output_name": "screenshot_5_challenges_badges.png",
        "pill_text": "شعلة القراءة والتحديات",
        "pill_emoji": "🔥",
        "pill_fill": (234, 88, 12, 50),
        "pill_border": (251, 146, 60, 180),
        "pill_text_color": (255, 237, 213),
        "title": "حافظ على شعلتك وافتح أوسمة الإلهام",
        "subtitle": "تتبع أيام قراءتك المتتالية واجمع الأوسمة التقديرية",
        "subtitle_color": (254, 215, 170),
        "bg_top": (85, 25, 5),
        "bg_bottom": (12, 9, 10),
        "glow_rgb": (234, 88, 12)
    },
    {
        "raw_image": os.path.join(RAW_DIR, "6_categories_library.png"),
        "crop_box": (0, 68, 720, 1545),
        "output_name": "screenshot_6_categories_library.png",
        "pill_text": "مكتبة الخواطر والحكم",
        "pill_emoji": "📚",
        "pill_fill": (5, 150, 105, 50),
        "pill_border": (52, 211, 153, 180),
        "pill_text_color": (209, 250, 229),
        "title": "آلاف الخواطر والحكم بدون إنترنت",
        "subtitle": "تصنيفات ثرية: أمل، حب، أدعية، وأقوال مأثورة مع وضع ليلي",
        "subtitle_color": (167, 243, 208),
        "bg_top": (6, 75, 55),
        "bg_bottom": (4, 15, 10),
        "glow_rgb": (16, 185, 129)
    }
]

if __name__ == '__main__':
    for s in SPECS:
        render_showcase(s)
    print("\nAll 6 Google Play Store screenshots generated successfully!")
