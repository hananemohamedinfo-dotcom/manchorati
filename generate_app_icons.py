import os
from PIL import Image, ImageDraw

source_path = "manchorati_app_icon_master.png"
if not os.path.exists(source_path):
    raise FileNotFoundError(f"{source_path} not found")

src = Image.open(source_path).convert("RGBA")
bg_color = (10, 17, 28, 255) # #0A111C dark luxury slate

densities = {
    "mipmap-mdpi": {"fg": 108, "launcher": 48},
    "mipmap-hdpi": {"fg": 162, "launcher": 72},
    "mipmap-xhdpi": {"fg": 216, "launcher": 96},
    "mipmap-xxhdpi": {"fg": 324, "launcher": 144},
    "mipmap-xxxhdpi": {"fg": 432, "launcher": 192},
}

res_base = os.path.join("app", "src", "main", "res")

def make_round_icon(image, size):
    mask = Image.new("L", (size, size), 0)
    draw = ImageDraw.Draw(mask)
    draw.ellipse((0, 0, size - 1, size - 1), fill=255)
    output = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    output.paste(image, (0, 0), mask=mask)
    return output

def make_squircle_icon(image, size, radius_pct=0.22):
    mask = Image.new("L", (size, size), 0)
    draw = ImageDraw.Draw(mask)
    radius = int(size * radius_pct)
    draw.rounded_rectangle((0, 0, size - 1, size - 1), radius=radius, fill=255)
    output = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    output.paste(image, (0, 0), mask=mask)
    return output

for folder, sizes in densities.items():
    target_dir = os.path.join(res_base, folder)
    os.makedirs(target_dir, exist_ok=True)
    
    # 1. Adaptive foreground (108dp base)
    # Safe zone diameter is 66-70% of fg_size. Sizing the squircle tile to 70% ensures
    # the entire double-beveled gold/dark emblem is fully visible on any OEM shape mask.
    fg_size = sizes["fg"]
    fg = Image.new("RGBA", (fg_size, fg_size), bg_color)
    target_emblem = int(fg_size * 0.70)
    scaled_emblem = src.resize((target_emblem, target_emblem), Image.Resampling.LANCZOS)
    
    off_x = (fg_size - target_emblem) // 2
    off_y = (fg_size - target_emblem) // 2
    fg.paste(scaled_emblem, (off_x, off_y))
    fg.save(os.path.join(target_dir, "ic_launcher_foreground.png"), "PNG")
    
    # 2. Legacy Launcher Icon (Squircle / Rounded Rect)
    l_size = sizes["launcher"]
    base_l = Image.new("RGBA", (l_size, l_size), bg_color)
    t_size = int(l_size * 0.94)
    scaled_l = src.resize((t_size, t_size), Image.Resampling.LANCZOS)
    off_l = (l_size - t_size) // 2
    base_l.paste(scaled_l, (off_l, off_l))
    
    squircle_icon = make_squircle_icon(base_l, l_size)
    squircle_icon.save(os.path.join(target_dir, "ic_launcher.png"), "PNG")
    
    # 3. Round Launcher Icon
    round_icon = make_round_icon(base_l, l_size)
    round_icon.save(os.path.join(target_dir, "ic_launcher_round.png"), "PNG")

    print(f"Generated icons for {folder}: fg={fg_size}x{fg_size}, launcher={l_size}x{l_size}")

# 4. Google Play Store 512x512 Icon
play_size = 512
play_icon = Image.new("RGBA", (play_size, play_size), bg_color)
p_size = int(play_size * 0.94)
scaled_p = src.resize((p_size, p_size), Image.Resampling.LANCZOS)
off_p = (play_size - p_size) // 2
play_icon.paste(scaled_p, (off_p, off_p))
play_icon.convert("RGB").save("play_store_512.png", "PNG")
play_icon.convert("RGB").save("C:/Users/HP/Desktop/Manchorati_Logo/play_store_512.png", "PNG")
print("Generated play_store_512.png (512x512 RGB) locally and on Desktop")
