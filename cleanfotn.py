import os
import re

# المسار إلى مجلد الخطوط
FONT_DIR = r"app/src/main/res/font"


def clean_and_rename_fonts(directory):
    if not os.path.exists(directory):
        print(f"المجلد غير موجود: {directory}")
        return

    files = os.listdir(directory)

    # 1. حذف أي ملف ليس خط Regular
    for filename in files:
        filepath = os.path.join(directory, filename)

        # تخطي المجلدات إذا وُجدت
        if os.path.isdir(filepath):
            continue

        # فحص هل الملف ينتهي بـ Regular.ttf (تجاهل حالة الأحرف)
        if not filename.lower().endswith("regular.ttf"):
            try:
                os.remove(filepath)
                print(f"تم الحذف: {filename}")
            except Exception as e:
                print(f"تعذر حذف {filename}: {e}")

    # 2. إعادة تسمية الملفات المتبقية (Regular) لتوافق أندرويد
    remaining_files = os.listdir(directory)
    for filename in remaining_files:
        filepath = os.path.join(directory, filename)

        if os.path.isdir(filepath):
            continue

        name_part, ext = os.path.splitext(filename)

        # إزالة كلمة regular أو -regular أو _regular
        clean_name = re.sub(r"[-_]?regular$", "", name_part, flags=re.IGNORECASE)

        # تحويل لحروف صغيرة
        clean_name = clean_name.lower()

        # استبدال المسافات والشرطات العادية بشرطة سفلية
        clean_name = clean_name.replace(" ", "_").replace("-", "_")

        # إزالة أي تكرار للشرطات السفلية __
        clean_name = re.sub(r"_+", "_", clean_name).strip("_")

        # الاسم النهائي مع امتداد .ttf
        new_filename = f"{clean_name}.ttf"
        new_filepath = os.path.join(directory, new_filename)

        if filepath != new_filepath:
            try:
                os.rename(filepath, new_filepath)
                print(f"تم تعديل الاسم: {filename} -> {new_filename}")
            except Exception as e:
                print(f"تعذر تعديل {filename}: {e}")


if __name__ == "__main__":
    clean_and_rename_fonts(FONT_DIR)
    print("\nاكتمل التنظيف وتعديل الأسماء بنجاح!")