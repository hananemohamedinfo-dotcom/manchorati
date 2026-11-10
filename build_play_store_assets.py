import os
import math
from PIL import Image, ImageDraw, ImageFont, ImageFilter
import arabic_reshaper
from bidi.algorithm import get_display

font_cairo = "app/src/main/res/font/cairo.ttf"
font_tajawal = "app/src/main/res/font/tajawal.ttf"
font_almarai = "app/src/main/res/font/almarai.ttf"

def render_arabic(text):
    reshaped = arabic_reshaper.reshape(text)
    return get_display(reshaped)

def make_gradient(width, height, color_top, color_bottom):
    base = Image.new('RGB', (width, height), color_top)
    top_r, top_g, top_b = color_top
    bot_r, bot_g, bot_b = color_bottom
    draw = ImageDraw.Draw(base)
    for y in range(height):
        factor = y / float(height)
        r = int(top_r + (bot_r - top_r) * factor)
        g = int(top_g + (bot_g - top_g) * factor)
        b = int(top_b + (bot_b - top_b) * factor)
        draw.line([(0, y), (width, y)], fill=(r, g, b))
    return base

out_dir = r"C:\Users\HP\Desktop\Manchorati_Logo\Google_Play_Assets"
os.makedirs(out_dir, exist_ok=True)

# ==========================================
# 1. FEATURE GRAPHIC / BANNER (1024 x 500)
# ==========================================
print("Building 1024x500 Feature Graphic...")
bw, bh = 1024, 500
banner = make_gradient(bw, bh, (7, 12, 19), (14, 25, 40))

# Ambient glow spots
glow_layer = Image.new('RGBA', (bw, bh), (0, 0, 0, 0))
glow_draw = ImageDraw.Draw(glow_layer)
glow_draw.ellipse((80, 70, 480, 470), fill=(2, 132, 199, 45)) # Cyan glow
glow_draw.ellipse((550, 100, 950, 450), fill=(212, 175, 55, 30)) # Gold glow
glow_layer = glow_layer.filter(ImageFilter.GaussianBlur(50))
banner.paste(glow_layer, (0, 0), mask=glow_layer)

# Add 3D App Icon Emblem on the left/center
icon_src = Image.open("manchorati_app_icon_master.png").convert("RGBA")
icon_size = 360
icon_scaled = icon_src.resize((icon_size, icon_size), Image.Resampling.LANCZOS)

# Soft shadow under emblem
shadow_layer = Image.new('RGBA', (bw, bh), (0, 0, 0, 0))
s_draw = ImageDraw.Draw(shadow_layer)
icon_x, icon_y = 70, 70
s_draw.rounded_rectangle((icon_x + 10, icon_y + 15, icon_x + icon_size - 10, icon_y + icon_size + 15), radius=60, fill=(0, 0, 0, 180))
shadow_layer = shadow_layer.filter(ImageFilter.GaussianBlur(18))
banner.paste(shadow_layer, (0, 0), mask=shadow_layer)
banner.paste(icon_scaled, (icon_x, icon_y), mask=icon_scaled)

# Right Side Typography
b_draw = ImageDraw.Draw(banner)
f_title = ImageFont.truetype(font_cairo, 62)
f_sub = ImageFont.truetype(font_cairo, 26)
f_badge = ImageFont.truetype(font_cairo, 18)

# App Title: منشوراتي
title_text = render_arabic("منشوراتي")
b_box = b_draw.textbbox((0, 0), title_text, font=f_title)
tw = b_box[2] - b_box[0]
tx = 940 - tw
ty = 110

# Title soft glow/shadow
b_draw.text((tx + 2, ty + 3), title_text, font=f_title, fill=(0, 0, 0))
b_draw.text((tx, ty), title_text, font=f_title, fill=(254, 240, 138)) # Gold highlight

# Subtitle
sub_text = render_arabic("عالم الأدب والاقتباسات والكاتب الذكي")
s_box = b_draw.textbbox((0, 0), sub_text, font=f_sub)
sw = s_box[2] - s_box[0]
sx = 940 - sw
sy = 205
b_draw.text((sx + 1, sy + 2), sub_text, font=f_sub, fill=(0, 0, 0))
b_draw.text((sx, sy), sub_text, font=f_sub, fill=(56, 189, 248)) # Cyan

# Badges (Pills)
badges = [
    render_arabic("مجتمع الكُتّاب"),
    render_arabic("استوديو الفيديو والريلز"),
    render_arabic("ذكاء اصطناعي إبداعي"),
]

cur_x = 940
by = 285
for b_txt in badges:
    box = b_draw.textbbox((0, 0), b_txt, font=f_badge)
    pw = (box[2] - box[0]) + 30
    ph = 36
    px = cur_x - pw
    # Pill background
    b_draw.rounded_rectangle((px, by, cur_x, by + ph), radius=18, fill=(22, 36, 56), outline=(56, 189, 248), width=1)
    b_draw.text((px + 15, by + 4), b_txt, font=f_badge, fill=(241, 245, 249))
    cur_x = px - 12

# Subtle bottom border line
b_draw.line([(0, bh - 3), (bw, bh - 3)], fill=(56, 189, 248), width=2)

banner_path = os.path.join(out_dir, "feature_graphic_1024x500.png")
banner.save(banner_path, "PNG")
print("Saved:", banner_path)

# ==========================================
# 2. PHONE SCREENSHOTS (1080 x 1920)
# ==========================================
screens_meta = [
    {
        "id": "1_discover",
        "file": "screen_verified_color.png",
        "title": "آلاف الاقتباسات والأشعار الراقية",
        "sub": "تصاميم أدبية فاخرة تناسب ذوقك الرفيع"
    },
    {
        "id": "2_ai_writer",
        "file": "screen_ai_result.png",
        "title": "الكاتب الذكي بالذكاء الاصطناعي",
        "sub": "صياغة إبداعية للخواطر والأشعار والرسائل"
    },
    {
        "id": "3_community",
        "file": "screen_final_community.png",
        "title": "مجتمع أدبي نابض بالإبداع",
        "sub": "انشر كتاباتك وتفاعل مع نخبة الكُتّاب والقرّاء"
    },
    {
        "id": "4_moods",
        "file": "screen_active_mood2.png",
        "title": "اقتباسات حسب مشاعرك ولحظتك",
        "sub": "تصفح حسب المزاج والتصنيفات المتنوعة"
    },
    {
        "id": "5_inspiration",
        "file": "screen_ai_conversation.png",
        "title": "محادثة تفاعلية وإلهام متجدد",
        "sub": "استلهم أفكارك وشارك أحاسيسك بكل سلاسة"
    }
]

sw, sh = 1080, 1920
font_scr_title = ImageFont.truetype(font_cairo, 48)
font_scr_sub = ImageFont.truetype(font_cairo, 28)

for idx, meta in enumerate(screens_meta, 1):
    print(f"Building Screenshot {idx}/5: {meta['id']}...")
    canvas = make_gradient(sw, sh, (8, 14, 22), (15, 26, 42))
    
    # Ambient corner light
    bg_glow = Image.new('RGBA', (sw, sh), (0, 0, 0, 0))
    bg_g_draw = ImageDraw.Draw(bg_glow)
    if idx % 2 == 1:
        bg_g_draw.ellipse((sw - 400, 50, sw + 200, 650), fill=(2, 132, 199, 40))
        bg_g_draw.ellipse((-150, sh - 600, 450, sh), fill=(212, 175, 55, 30))
    else:
        bg_g_draw.ellipse((-200, 50, 400, 650), fill=(212, 175, 55, 35))
        bg_g_draw.ellipse((sw - 400, sh - 600, sw + 200, sh), fill=(2, 132, 199, 35))
    bg_glow = bg_glow.filter(ImageFilter.GaussianBlur(60))
    canvas.paste(bg_glow, (0, 0), mask=bg_glow)
    
    c_draw = ImageDraw.Draw(canvas)
    
    # Header: Title & Subtitle centered
    t_rendered = render_arabic(meta["title"])
    s_rendered = render_arabic(meta["sub"])
    
    t_box = c_draw.textbbox((0, 0), t_rendered, font=font_scr_title)
    tw = t_box[2] - t_box[0]
    tx = (sw - tw) // 2
    ty = 90
    
    # Shadow and Title
    c_draw.text((tx + 2, ty + 3), t_rendered, font=font_scr_title, fill=(0, 0, 0))
    c_draw.text((tx, ty), t_rendered, font=font_scr_title, fill=(254, 240, 138)) # Gold
    
    # Subtitle
    s_box = c_draw.textbbox((0, 0), s_rendered, font=font_scr_sub)
    sw_sub = s_box[2] - s_box[0]
    sx = (sw - sw_sub) // 2
    sy = 175
    c_draw.text((sx + 1, sy + 2), s_rendered, font=font_scr_sub, fill=(0, 0, 0))
    c_draw.text((sx, sy), s_rendered, font=font_scr_sub, fill=(147, 197, 253)) # Light blue
    
    # Decorative header divider
    c_draw.line([(sw//2 - 60, 240), (sw//2 + 60, 240)], fill=(56, 189, 248), width=3)
    
    # Phone Mockup
    # Load actual screenshot
    src_img_path = os.path.join(r"C:\Users\HP\.gemini\antigravity\brain\9a8e3b55-f4f8-4534-a232-e548bf885a00", meta["file"])
    screen_raw = Image.open(src_img_path).convert("RGBA")
    
    # Dimensions for phone screen on canvas
    # Target width = 760, aspect ratio ~ 720/1640 -> target height = 1730 (partially cut off at bottom, which is standard for modern App Store shots)
    pw = 760
    ph = int(pw * (1640 / 720)) # ~1731
    screen_resized = screen_raw.resize((pw, ph), Image.Resampling.LANCZOS)
    
    # Frame outer dimensions (bezel = 12px)
    bezel = 14
    fw = pw + bezel * 2
    fh = ph + bezel * 2
    fx = (sw - fw) // 2
    fy = 280
    
    # Device Shadow
    d_shadow = Image.new('RGBA', (sw, sh), (0, 0, 0, 0))
    ds_draw = ImageDraw.Draw(d_shadow)
    ds_draw.rounded_rectangle((fx - 15, fy + 15, fx + fw + 15, fy + fh + 30), radius=56, fill=(0, 0, 0, 190))
    d_shadow = d_shadow.filter(ImageFilter.GaussianBlur(28))
    canvas.paste(d_shadow, (0, 0), mask=d_shadow)
    
    # Phone Body (Titanium dark border with beveled rim)
    phone_layer = Image.new('RGBA', (fw, fh), (0, 0, 0, 0))
    p_draw = ImageDraw.Draw(phone_layer)
    # Outer frame
    p_draw.rounded_rectangle((0, 0, fw - 1, fh - 1), radius=48, fill=(22, 30, 44), outline=(71, 85, 105), width=2)
    # Inner gold/cyan hairline
    p_draw.rounded_rectangle((2, 2, fw - 3, fh - 3), radius=46, outline=(212, 175, 55), width=1)
    
    # Screen Mask
    screen_mask = Image.new('L', (pw, ph), 0)
    sm_draw = ImageDraw.Draw(screen_mask)
    sm_draw.rounded_rectangle((0, 0, pw - 1, ph - 1), radius=38, fill=255)
    
    # Paste screen inside phone layer
    phone_layer.paste(screen_resized, (bezel, bezel), mask=screen_mask)
    
    # Top speaker and camera punch hole
    p_draw.ellipse((fw//2 - 7, bezel + 10, fw//2 + 7, bezel + 24), fill=(10, 10, 12))
    
    # Paste phone onto canvas
    canvas.paste(phone_layer, (fx, fy), mask=phone_layer)
    
    out_file = os.path.join(out_dir, f"screenshot_{meta['id']}.png")
    canvas.save(out_file, "PNG")
    print("Saved:", out_file)

print("All Google Play Store assets successfully created!")
