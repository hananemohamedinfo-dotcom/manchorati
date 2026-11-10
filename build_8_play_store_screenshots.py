import os
from PIL import Image, ImageDraw, ImageFont, ImageFilter
import arabic_reshaper
from bidi.algorithm import get_display

font_cairo = "app/src/main/res/font/cairo.ttf"

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
raw_dir = r"C:\Users\HP\Desktop\Manchorati_Logo\Device_Screenshots"
os.makedirs(out_dir, exist_ok=True)

screens_config = [
    {
        "index": 1,
        "raw": "raw_1.jpg",
        "name": "play_screen_1_cards",
        "title": "تصاميم أدبية فاخرة ومتنوعة",
        "sub": "تصفح آلاف الاقتباسات ببطاقات كلاسيكية وعصرية متميزة"
    },
    {
        "index": 2,
        "raw": "raw_2.jpg",
        "name": "play_screen_2_reading",
        "title": "أشعار وخواطر تلامس الوجدان",
        "sub": "تجربة قراءة ليلية مريحة تعتني بأدق تفاصيل الكلمة"
    },
    {
        "index": 3,
        "raw": "raw_3.jpg",
        "name": "play_screen_3_categories",
        "title": "اقتباسات مصنفة حسب حالتك",
        "sub": "حكمة، حب، أمل، وكبرياء تناسب كل لحظة وشعور"
    },
    {
        "index": 4,
        "raw": "raw_4.jpg",
        "name": "play_screen_4_moods",
        "title": "فلترة ذكية للمشاعر بلمسة واحدة",
        "sub": "اختر مزاجك واكتشف نصوصاً تعبر عنك بأصدق أسلوب"
    },
    {
        "index": 5,
        "raw": "raw_5.jpg",
        "name": "play_screen_5_video_studio",
        "title": "استوديو الفيديو الحركي المتطور",
        "sub": "حوّل نصوصك إلى مقاطع سينمائية مع تحكم كامل بالخط والموسيقى"
    },
    {
        "index": 6,
        "raw": "raw_6.jpg",
        "name": "play_screen_6_reels",
        "title": "ريلز أدبية بجودة استثنائية",
        "sub": "استمتع بمقاطع فيديو قصيرة وملهمة تجمع الصوت والصورة"
    },
    {
        "index": 7,
        "raw": "raw_7.jpg",
        "name": "play_screen_7_ai_writer",
        "title": "كاتبك الإبداعي بالذكاء الاصطناعي",
        "sub": "صياغة فورية للقصائد والخواطر وأجمل المنشورات بلمسة ذكية"
    },
    {
        "index": 8,
        "raw": "raw_8.jpg",
        "name": "play_screen_8_community",
        "title": "مجتمع حي لعشاق الكلمة والقلم",
        "sub": "انشر إبداعاتك، تابع كُتابك المفضلين، وشارك أثرك الملهم"
    }
]

sw, sh = 1080, 1920
font_title = ImageFont.truetype(font_cairo, 48)
font_sub = ImageFont.truetype(font_cairo, 26)

for item in screens_config:
    idx = item["index"]
    print(f"Rendering Screen {idx}/8: {item['name']}...")
    
    # Background gradient
    canvas = make_gradient(sw, sh, (8, 14, 22), (16, 27, 43))
    
    # Ambient glowing spots
    bg_glow = Image.new('RGBA', (sw, sh), (0, 0, 0, 0))
    bg_g_draw = ImageDraw.Draw(bg_glow)
    if idx % 2 == 1:
        bg_g_draw.ellipse((sw - 380, 40, sw + 200, 620), fill=(2, 132, 199, 45)) # Cyan
        bg_g_draw.ellipse((-150, sh - 600, 450, sh), fill=(212, 175, 55, 30)) # Gold
    else:
        bg_g_draw.ellipse((-180, 40, 400, 620), fill=(212, 175, 55, 40)) # Gold
        bg_g_draw.ellipse((sw - 380, sh - 600, sw + 200, sh), fill=(2, 132, 199, 40)) # Cyan
    bg_glow = bg_glow.filter(ImageFilter.GaussianBlur(65))
    canvas.paste(bg_glow, (0, 0), mask=bg_glow)
    
    c_draw = ImageDraw.Draw(canvas)
    
    # 1. Header Area (Titles)
    t_text = render_arabic(item["title"])
    s_text = render_arabic(item["sub"])
    
    t_box = c_draw.textbbox((0, 0), t_text, font=font_title)
    tw = t_box[2] - t_box[0]
    tx = (sw - tw) // 2
    ty = 90
    
    # Drop shadow & title text
    c_draw.text((tx + 2, ty + 3), t_text, font=font_title, fill=(0, 0, 0))
    c_draw.text((tx, ty), t_text, font=font_title, fill=(254, 240, 138)) # Champagne gold
    
    # Subtitle text
    s_box = c_draw.textbbox((0, 0), s_text, font=font_sub)
    sw_sub = s_box[2] - s_box[0]
    sx = (sw - sw_sub) // 2
    sy = 175
    c_draw.text((sx + 1, sy + 2), s_text, font=font_sub, fill=(0, 0, 0))
    c_draw.text((sx, sy), s_text, font=font_sub, fill=(147, 197, 253)) # Light cyan
    
    # Decorative accent bar
    c_draw.line([(sw//2 - 60, 238), (sw//2 + 60, 238)], fill=(56, 189, 248), width=3)
    
    # 2. Smartphone Mockup
    raw_path = os.path.join(raw_dir, item["raw"])
    raw_img = Image.open(raw_path).convert("RGBA")
    
    # Dimensions for phone screen
    pw = 760
    ph = int(pw * (1640 / 720)) # ~1730px
    screen_resized = raw_img.resize((pw, ph), Image.Resampling.LANCZOS)
    
    bezel = 14
    fw = pw + bezel * 2
    fh = ph + bezel * 2
    fx = (sw - fw) // 2
    fy = 275
    
    # Ambient Drop Shadow under device
    d_shadow = Image.new('RGBA', (sw, sh), (0, 0, 0, 0))
    ds_draw = ImageDraw.Draw(d_shadow)
    ds_draw.rounded_rectangle((fx - 18, fy + 18, fx + fw + 18, fy + fh + 35), radius=58, fill=(0, 0, 0, 195))
    d_shadow = d_shadow.filter(ImageFilter.GaussianBlur(30))
    canvas.paste(d_shadow, (0, 0), mask=d_shadow)
    
    # Device Body Layer
    phone_layer = Image.new('RGBA', (fw, fh), (0, 0, 0, 0))
    p_draw = ImageDraw.Draw(phone_layer)
    # Dark titanium body
    p_draw.rounded_rectangle((0, 0, fw - 1, fh - 1), radius=48, fill=(20, 28, 42), outline=(71, 85, 105), width=2)
    # Inner gold hairline bevel
    p_draw.rounded_rectangle((2, 2, fw - 3, fh - 3), radius=46, outline=(212, 175, 55), width=1)
    
    # Screen Mask
    screen_mask = Image.new('L', (pw, ph), 0)
    sm_draw = ImageDraw.Draw(screen_mask)
    sm_draw.rounded_rectangle((0, 0, pw - 1, ph - 1), radius=38, fill=255)
    
    # Paste resized screenshot into phone frame
    phone_layer.paste(screen_resized, (bezel, bezel), mask=screen_mask)
    
    # Camera punch hole
    p_draw.ellipse((fw//2 - 7, bezel + 10, fw//2 + 7, bezel + 24), fill=(10, 10, 12))
    
    # Paste complete phone onto canvas
    canvas.paste(phone_layer, (fx, fy), mask=phone_layer)
    
    # Save to Google Play Assets folder
    out_file = os.path.join(out_dir, f"{item['name']}.png")
    canvas.save(out_file, "PNG")
    print(f"Saved: {out_file}")

print("All 8 Google Play Screenshots successfully generated!")
