import os
import math
from PIL import Image, ImageDraw, ImageFont, ImageFilter
import arabic_reshaper
from bidi.algorithm import get_display

BANNER_W = 1024
BANNER_H = 500
OUTPUT_DIR = r"C:\Users\X4P\Desktop\Manchorati_GooglePlay_Release\banner"
os.makedirs(OUTPUT_DIR, exist_ok=True)

ICON_PATH = r"C:\Users\X4P\Desktop\Manchorati_GooglePlay_Release\Store_Assets\app_icon_512x512.png"

FONT_BRAND = ImageFont.truetype(r"C:\Windows\Fonts\segoeuib.ttf", 62)
FONT_TAGLINE = ImageFont.truetype(r"C:\Windows\Fonts\segoeui.ttf", 23)
FONT_BADGE = ImageFont.truetype(r"C:\Windows\Fonts\segoeuib.ttf", 17)

def ar(text):
    return get_display(arabic_reshaper.reshape(text))

def build_banner():
    # 1. Base Gradient Canvas
    base = Image.new("RGBA", (BANNER_W, BANNER_H))
    draw = ImageDraw.Draw(base)
    
    # Horizontal rich crimson-burgundy to deep onyx gradient
    left_color = (139, 21, 56)    # Warm brand crimson
    center_color = (65, 10, 28)   # Deep wine
    right_color = (13, 15, 24)    # Midnight onyx
    
    for x in range(BANNER_W):
        t = x / float(BANNER_W)
        if t < 0.4:
            local_t = t / 0.4
            r = int(left_color[0] * (1 - local_t) + center_color[0] * local_t)
            g = int(left_color[1] * (1 - local_t) + center_color[1] * local_t)
            b = int(left_color[2] * (1 - local_t) + center_color[2] * local_t)
        else:
            local_t = (t - 0.4) / 0.6
            r = int(center_color[0] * (1 - local_t) + right_color[0] * local_t)
            g = int(center_color[1] * (1 - local_t) + right_color[1] * local_t)
            b = int(center_color[2] * (1 - local_t) + right_color[2] * local_t)
        draw.line([(x, 0), (x, BANNER_H)], fill=(r, g, b, 255))
    
    # 2. Atmospheric Ambient Glow behind icon and texts
    glow = Image.new("RGBA", (BANNER_W, BANNER_H), (0, 0, 0, 0))
    gdraw = ImageDraw.Draw(glow)
    
    # Icon back-glow (Golden crimson nebula)
    icon_glow_center = (200, 250)
    for rad in range(230, 0, -8):
        alpha = int(70 * (1 - rad / 230.0))
        gdraw.ellipse([icon_glow_center[0] - rad, icon_glow_center[1] - rad,
                       icon_glow_center[0] + rad, icon_glow_center[1] + rad],
                      fill=(244, 63, 94, alpha))
        
    # Golden crest glow
    gold_glow_center = (670, 160)
    for rad in range(200, 0, -10):
        alpha = int(35 * (1 - rad / 200.0))
        gdraw.ellipse([gold_glow_center[0] - rad, gold_glow_center[1] - rad,
                       gold_glow_center[0] + rad, gold_glow_center[1] + rad],
                      fill=(245, 158, 11, alpha))
        
    glow = glow.filter(ImageFilter.GaussianBlur(35))
    base = Image.alpha_composite(base, glow)
    
    # 3. Add 3D App Icon on the left
    if os.path.exists(ICON_PATH):
        raw_icon = Image.open(ICON_PATH).convert("RGBA")
        icon_size = 260
        icon_resized = raw_icon.resize((icon_size, icon_size), Image.Resampling.LANCZOS)
        
        # Rounded mask for icon
        icon_mask = Image.new("L", (icon_size, icon_size), 0)
        mdraw = ImageDraw.Draw(icon_mask)
        mdraw.rounded_rectangle([0, 0, icon_size, icon_size], radius=58, fill=255)
        
        # Icon drop shadow
        icon_x = 60
        icon_y = (BANNER_H - icon_size) // 2
        
        icon_shadow = Image.new("RGBA", (BANNER_W, BANNER_H), (0, 0, 0, 0))
        isdraw = ImageDraw.Draw(icon_shadow)
        isdraw.rounded_rectangle([icon_x - 6, icon_y + 12, icon_x + icon_size + 6, icon_y + icon_size + 12],
                                 radius=64, fill=(0, 0, 0, 190))
        icon_shadow = icon_shadow.filter(ImageFilter.GaussianBlur(22))
        base = Image.alpha_composite(base, icon_shadow)
        
        # Border stroke for premium feel
        icon_border = Image.new("RGBA", (icon_size, icon_size), (0, 0, 0, 0))
        bdraw = ImageDraw.Draw(icon_border)
        bdraw.rounded_rectangle([0, 0, icon_size, icon_size], radius=58, outline=(255, 255, 255, 75), width=3)
        icon_composite = Image.alpha_composite(icon_resized, icon_border)
        
        base.paste(icon_composite, (icon_x, icon_y), icon_mask)
    
    # 4. Typography (Right Side)
    draw = ImageDraw.Draw(base)
    right_margin = 55
    
    # Top pill: "مجتمع الكُتّاب والاقتباسات الأدبية"
    top_badge_text = ar("مجتمع الكُتّاب والاقتباسات الأدبية")
    tb_bbox = draw.textbbox((0, 0), top_badge_text, font=FONT_BADGE)
    tb_w = tb_bbox[2] - tb_bbox[0]
    tb_h = 32
    tb_x = BANNER_W - tb_w - right_margin
    tb_y = 65
    
    tb_bg = Image.new("RGBA", (BANNER_W, BANNER_H), (0, 0, 0, 0))
    tbdraw = ImageDraw.Draw(tb_bg)
    tbdraw.rounded_rectangle([tb_x - 16, tb_y - 4, tb_x + tb_w + 16, tb_y + tb_h - 4],
                            radius=14, fill=(244, 63, 94, 50), outline=(244, 63, 94, 160), width=1)
    base = Image.alpha_composite(base, tb_bg)
    draw = ImageDraw.Draw(base)
    draw.text((tb_x, tb_y + 1), top_badge_text, font=FONT_BADGE, fill=(254, 205, 211))
    
    # Main App Title: "منشوراتي"
    app_title = ar("منشوراتي")
    title_bbox = draw.textbbox((0, 0), app_title, font=FONT_BRAND)
    title_w = title_bbox[2] - title_bbox[0]
    title_x = BANNER_W - title_w - right_margin
    title_y = 114
    # Shadow
    draw.text((title_x, title_y + 4), app_title, font=FONT_BRAND, fill=(0, 0, 0, 180))
    draw.text((title_x, title_y), app_title, font=FONT_BRAND, fill=(255, 255, 255))
    
    # Subtitle / Tagline
    tagline = ar("عالمك اليومي للخواطر الملهمة، الحكم العميقة، وريلز الاقتباسات")
    tag_bbox = draw.textbbox((0, 0), tagline, font=FONT_TAGLINE)
    tag_w = tag_bbox[2] - tag_bbox[0]
    tag_x = BANNER_W - tag_w - right_margin
    tag_y = 212
    draw.text((tag_x, tag_y + 2), tagline, font=FONT_TAGLINE, fill=(0, 0, 0, 150))
    draw.text((tag_x, tag_y), tagline, font=FONT_TAGLINE, fill=(253, 230, 138)) # Gold cream
    
    # 5. Feature Highlights Row (4 Pills)
    features = [
        ("سلسلة القراءة", (234, 88, 12)),
        ("استوديو الريلز", (147, 51, 234)),
        ("ذكاء اصطناعي", (37, 99, 235)),
        ("كاتب الأسبوع", (217, 119, 6)),
    ]
    
    curr_x = BANNER_W - right_margin
    pill_y = 295
    pill_h = 38
    
    pills_overlay = Image.new("RGBA", (BANNER_W, BANNER_H), (0, 0, 0, 0))
    pdraw = ImageDraw.Draw(pills_overlay)
    
    for f_text, color in features:
        reshaped = ar(f_text)
        b_box = draw.textbbox((0, 0), reshaped, font=FONT_BADGE)
        fw = b_box[2] - b_box[0]
        
        pw = fw + 24
        px1 = curr_x - pw
        px2 = curr_x
        
        pdraw.rounded_rectangle([px1, pill_y, px2, pill_y + pill_h], radius=19,
                                fill=(color[0], color[1], color[2], 55),
                                outline=(color[0], color[1], color[2], 190),
                                width=2)
        
        curr_x = px1 - 10
        
    base = Image.alpha_composite(base, pills_overlay)
    draw = ImageDraw.Draw(base)
    
    # Draw pill texts
    curr_x = BANNER_W - right_margin
    for f_text, color in features:
        reshaped = ar(f_text)
        b_box = draw.textbbox((0, 0), reshaped, font=FONT_BADGE)
        fw = b_box[2] - b_box[0]
        pw = fw + 24
        px1 = curr_x - pw
        draw.text((px1 + 12, pill_y + 9), reshaped, font=FONT_BADGE, fill=(255, 255, 255))
        curr_x = px1 - 10
        
    # Small bottom hint: "بدون إنترنت • مجاني بالكامل • ودجت للشاشة الرئيسية • وضع ليلي فاخر"
    bottom_features = ar("بدون إنترنت  •  مجاني بالكامل  •  ودجت للشاشة الرئيسية  •  وضع ليلي فاخر")
    bf_bbox = draw.textbbox((0, 0), bottom_features, font=FONT_BADGE)
    bf_w = bf_bbox[2] - bf_bbox[0]
    bf_x = BANNER_W - bf_w - right_margin
    bf_y = 380
    draw.text((bf_x, bf_y), bottom_features, font=FONT_BADGE, fill=(156, 163, 175, 240))
    
    # 6. Ensure strict RGB output (no alpha) for Google Play Console compliance
    final_rgb = base.convert("RGB")
    out_path = os.path.join(OUTPUT_DIR, "feature_graphic_1024x500.png")
    final_rgb.save(out_path, quality=95)
    print(f"Generated Feature Graphic Banner: {out_path} ({final_rgb.size})")

if __name__ == '__main__':
    build_banner()
