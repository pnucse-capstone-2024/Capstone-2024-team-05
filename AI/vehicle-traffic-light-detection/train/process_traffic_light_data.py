import os
import json
import shutil
import random
from PIL import Image

# 경로 설정
label_dir = r'C:\Users\computer1\Desktop\codevision2\data\origin\traffic_light\label'
image_dir = r'C:\Users\computer1\Desktop\codevision2\data\origin\traffic_light\image'
output_dir = r'C:\Users\computer1\Desktop\codevision2\data\processed\traffic_light'

# 출력 폴더 구조 생성
train_image_dir = os.path.join(output_dir, 'train', 'images')
train_label_dir = os.path.join(output_dir, 'train', 'labels')
val_image_dir = os.path.join(output_dir, 'val', 'images')
val_label_dir = os.path.join(output_dir, 'val', 'labels')

os.makedirs(train_image_dir, exist_ok=True)
os.makedirs(train_label_dir, exist_ok=True)
os.makedirs(val_image_dir, exist_ok=True)
os.makedirs(val_label_dir, exist_ok=True)

# 레이블 맵핑
label_map = {
    "red": 4,
    "green": 5,
    "x_light": 6,
    "others_arrow": 7,
    "yellow": 8,
    "left_arrow": 9
}

def convert_annotation(label_file, output_label_file):
    try:
        with open(label_file, 'r', encoding='utf-8') as f:
            data = json.load(f)
    except Exception as e:
        print(f"라벨 파일 로드에 실패: {label_file}\n에러: {str(e)}")
        return
    
    image_info = data['image']

    image_file = find_image_file(label_file)
    if not image_file:
        print(f"이미지 파일을 찾을 수 없음: {label_file}")
        return

    width, height = image_info["imsize"]

    try:
        with open(output_label_file, 'w') as f:
            for annotation in data['annotation']:
                cls = annotation.get('class')
                typ = annotation.get('type')
                
                if cls != 'traffic_light' or typ != 'car':
                    continue

                box = annotation['box']
                attributes = annotation['attribute'][0]
                
                for label, value in attributes.items():
                    if value == "on" and label in label_map:
                        class_id = label_map[label]

                        x_min = box[0]
                        y_min = box[1]
                        x_max = box[2]
                        y_max = box[3]

                        x_center = (x_min + x_max) / 2 / width
                        y_center = (y_min + y_max) / 2 / height
                        box_width = (x_max - x_min) / width
                        box_height = (y_max - y_min) / height

                        label_str = f"{class_id} {x_center} {y_center} {box_width} {box_height}\n"
                        f.write(label_str)
                        break
    except Exception as e:
        print(f"라벨 파일 저장 중 오류 발생: {output_label_file}\n에러: {str(e)}")

def find_image_file(label_file):
    # 라벨 경로에서 파일 이름 추출
    label_file_name = os.path.basename(label_file)
    
    # 라벨 경로에서 원천 폴더 이름 추출
    label_folder = os.path.dirname(label_file).split(os.sep)[-1]
    
    # 이미지 경로 생성
    base_name = os.path.join(
        image_dir,
        f"[원천]{label_folder}",
        label_file_name
    ).replace('.json', '')
    
    for ext in ['.png', '.jpg']:
        image_file = base_name + ext
        if os.path.exists(image_file):
            return image_file
    return None

# 레이블 파일 리스트 생성
label_files = [os.path.join(root, file) for root, _, files in os.walk(label_dir) for file in files if file.endswith('.json')]

random.shuffle(label_files)

# 전체 데이터 수
total_files = len(label_files)
print(f"전체 데이터 수: {total_files}")

# 데이터 분할 (80:20)
split_index = int(len(label_files) * 0.8)
train_files = label_files[:split_index]
val_files = label_files[split_index:]

# 1%에 해당하는 파일 수
percent_step = max(1, total_files // 100)

# 학습용 데이터 처리
for i, label_file in enumerate(train_files, 1):
    if i % percent_step == 0 or i == len(train_files):
        print(f"학습용 데이터 처리 중: {i}/{len(train_files)} ({(i/len(train_files))*100:.2f}%)")

    image_file = find_image_file(label_file)
    if image_file:
        output_label_file = os.path.join(train_label_dir, os.path.basename(label_file).replace('.json', '.txt'))
        convert_annotation(label_file, output_label_file)  # 라벨 파일 변환 후 에러 체크

        try:
            shutil.copy(image_file, train_image_dir)
        except Exception as e:
            print(f"이미지 복사 중 에러 발생: {image_file}\n에러: {str(e)}")

# 검증용 데이터 처리
for i, label_file in enumerate(val_files, 1):
    if i % percent_step == 0 or i == len(val_files):
        print(f"검증용 데이터 처리 중: {i}/{len(val_files)} ({(i/len(val_files))*100:.2f}%)")

    image_file = find_image_file(label_file)
    if image_file:
        output_label_file = os.path.join(val_label_dir, os.path.basename(label_file).replace('.json', '.txt'))
        convert_annotation(label_file, output_label_file)  # 라벨 파일 변환 후 에러 체크

        try:
            shutil.copy(image_file, val_image_dir)
        except Exception as e:
            print(f"이미지 복사 중 에러 발생: {image_file}\n에러: {str(e)}")

print("변환 완료")
