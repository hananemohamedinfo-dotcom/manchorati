import cv2
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

# 1. Load source image
src_path = r'C:/Users/HP/.gemini/antigravity/brain/9251aa75-9c2e-4b3a-9e31-c2966dc3dc14/.user_uploaded/media_1793874254308_f4df1d1e.jpg'
img_bgr = cv2.imread(src_path)
H, W, _ = img_bgr.shape

# 2. Build clean background model for the upper area
# We fit a 2D polynomial surface to background points in the upper half
y_grid, x_grid = np.mgrid[0:H, 0:W]

# Let's collect clean background masks (pixels outside all logo elements)
# Logo elements have high V in HSV or high cyan/gold saturation
hsv = cv2.cvtColor(img_bgr, cv2.COLOR_BGR2HSV)
logo_mask = (hsv[:, :, 1] > 60) | (hsv[:, :, 2] > 110)
# dilate logo mask so we only sample true background
kernel = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (45, 45))
expanded_logo = cv2.dilate(logo_mask.astype(np.uint8), kernel)

# Sample background in the upper squircle region (y < 450, 180 < x < 840, 150 < y < 450)
bg_region = (expanded_logo == 0) & (y_grid < 450) & (y_grid > 140) & (x_grid > 170) & (x_grid < 850)

bx = x_grid[bg_region]
by = y_grid[bg_region]

# Polynomial features: 1, x, y, x^2, y^2, x*y
A = np.column_stack([np.ones_like(bx), bx, by, bx**2, by**2, bx*by])

# Fit each channel (B, G, R)
bg_reconstructed = np.zeros_like(img_bgr, dtype=np.float32)
all_A = np.column_stack([np.ones(W*H), x_grid.ravel(), y_grid.ravel(), x_grid.ravel()**2, y_grid.ravel()**2, (x_grid*y_grid).ravel()])

for c in range(3):
    coeffs, _, _, _ = np.linalg.lstsq(A, img_bgr[:, :, c][bg_region], rcond=None)
    bg_reconstructed[:, :, c] = (all_A @ coeffs).reshape(H, W)

bg_reconstructed = np.clip(bg_reconstructed, 0, 255).astype(np.uint8)

# 3. Mask of the old upper nib that we want to replace with clean background
# The old upper nib is above the gold band
# Center of gold band: (505, 470)
# Axis: theta = -52.7 deg
theta = np.radians(-52.7)
ux, uy = np.cos(theta), np.sin(theta)
vx, vy = -uy, ux

# Coordinate along pen axis relative to gold band:
d_axis = (x_grid - 505) * ux + (y_grid - 470) * uy
d_perp = (x_grid - 505) * vx + (y_grid - 470) * vy

# Old upper nib is roughly at d_axis between 15 and 260, and |d_perp| < 80
old_nib_mask = (d_axis > 18) & (d_axis < 260) & (np.abs(d_perp) < 85)

# Smooth transition of the background replacement
old_nib_mask_smooth = cv2.GaussianBlur(old_nib_mask.astype(np.float32), (31, 31), 0)

# Base image with old nib erased cleanly back to pristine background
base_erased = (img_bgr * (1.0 - old_nib_mask_smooth[:, :, None]) + bg_reconstructed * old_nib_mask_smooth[:, :, None]).astype(np.uint8)

cv2.imwrite('base_erased.png', base_erased)
print('Erased old top nib, clean base saved!')
