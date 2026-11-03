import subprocess
import time
import os
from PIL import Image

adb = r'C:\Users\X4P\AppData\Local\Android\Sdk\platform-tools\adb.exe'
RAW_DIR = r"c:\Users\X4P\Desktop\apps\application test\manchorati-main\raw_captures"
os.makedirs(RAW_DIR, exist_ok=True)

def adb_shell(cmd_args):
    return subprocess.run([adb, '-s', '79SSZL9XWWDMO7A6', 'shell'] + cmd_args, capture_output=True, text=True, encoding='utf-8', errors='ignore')

def adb_tap(x, y):
    adb_shell(['input', 'tap', str(x), str(y)])

def adb_key(key_code):
    adb_shell(['input', 'keyevent', str(key_code)])

def capture(name):
    out_path = os.path.join(RAW_DIR, name)
    with open(out_path, 'wb') as f:
        subprocess.run([adb, '-s', '79SSZL9XWWDMO7A6', 'exec-out', 'screencap', '-p'], stdout=f)
    im = Image.open(out_path)
    print(f"Captured {name}: {im.size}, {im.mode}")
    return out_path

def setup_demo_mode():
    adb_shell(['settings', 'put', 'system', 'accelerometer_rotation', '0'])
    adb_shell(['settings', 'put', 'system', 'user_rotation', '0'])
    adb_shell(['settings', 'put', 'global', 'sysui_demo_allowed', '1'])
    adb_shell(['am', 'broadcast', '-a', 'com.android.systemui.demo', '-e', 'command', 'enter'])
    adb_shell(['am', 'broadcast', '-a', 'com.android.systemui.demo', '-e', 'command', 'clock', '-e', 'hhmm', '1200'])
    adb_shell(['am', 'broadcast', '-a', 'com.android.systemui.demo', '-e', 'command', 'battery', '-e', 'level', '100', '-e', 'plugged', 'false'])
    adb_shell(['am', 'broadcast', '-a', 'com.android.systemui.demo', '-e', 'command', 'network', '-e', 'wifi', 'show', '-e', 'level', '4'])
    adb_shell(['am', 'broadcast', '-a', 'com.android.systemui.demo', '-e', 'command', 'notifications', '-e', 'visible', 'false'])

def main():
    setup_demo_mode()
    
    # 1. Screen 1: Community Feed
    print("Capturing 1. Community Feed...")
    adb_shell(['am', 'start', '-n', 'com.manchorati.www/.SplashActivity'])
    time.sleep(3)
    # Ensure Community tab is active (tap community tab: 630, 1470)
    adb_tap(630, 1470)
    time.sleep(1)
    capture("1_community_feed.png")
    
    # 2. Screen 2: Writers Leaderboard
    print("Capturing 2. Writers Leaderboard...")
    adb_shell(['am', 'start', '-n', 'com.manchorati.www/.WritersActivity'])
    time.sleep(2)
    capture("2_writers_leaderboard.png")
    adb_key(4) # back
    time.sleep(1)
    
    # 3. Screen 3: AI Writer Bottom Sheet with generated quote
    print("Capturing 3. AI Writer...")
    # Tap AI writer button in header (x=310, y=124)
    adb_tap(310, 124)
    time.sleep(1.5)
    # Tap generate button (x=360, y=1434)
    adb_tap(360, 1434)
    time.sleep(2)
    capture("3_ai_writer.png")
    # Close AI dialog
    adb_tap(64, 654)
    time.sleep(1)
    
    # 4. Screen 4: Video Studio
    print("Capturing 4. Video Studio...")
    adb_shell(['am', 'start', '-n', 'com.manchorati.www/.VideoStudioActivity',
               '--es', 'EXTRA_CONTENT', 'كن في الحياة كعابر سبيل، واترك وراءك كل أثر جميل.',
               '--es', 'EXTRA_TAG', 'حكم وأمثال'])
    time.sleep(2.5)
    capture("4_video_studio.png")
    adb_key(4) # back
    time.sleep(1)
    
    # 5. Screen 5: Daily Streak & Writer Challenges Dialog
    print("Capturing 5. Daily Streak & Challenges Dialog...")
    # Back to community feed, tap streak fire badge (x=520, y=120)
    adb_shell(['am', 'start', '-n', 'com.manchorati.www/.SplashActivity'])
    time.sleep(2)
    adb_tap(520, 120)
    time.sleep(1.5)
    capture("5_challenges_and_streaks.png")
    adb_key(4) # dismiss dialog
    time.sleep(1)
    
    # 6. Screen 6: Categories / Quotes Library
    print("Capturing 6. Categories & Wisdom Library...")
    # Tap Categories tab in bottom nav (x=450, y=1470)
    adb_tap(450, 1470)
    time.sleep(1.5)
    capture("6_categories_library.png")
    
    print("\nAll 6 screens captured successfully!")

if __name__ == '__main__':
    main()
