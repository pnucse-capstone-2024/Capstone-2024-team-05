import os
import json
import shutil
import random
from PIL import Image

# 경로 설정
label_dir = r'C:\Users\computer1\Desktop\codevision2\data\origin\car_and_person\label'
image_dir = r'C:\Users\computer1\Desktop\codevision2\data\origin\car_and_person\image'
output_dir = r'C:\Users\computer1\Desktop\codevision2\data\processed'

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
    "일반차량": 0,
    "보행자": 1,
    "목적차량(특장차)": 2,
    "이륜차": 3
}

def convert_annotation(label_file, output_label_file):
    with open(label_file, 'r', encoding='utf-8') as f:
        data = json.load(f)
    
    image_file = find_image_file(label_file)
    if not image_file:
        return
    
    with Image.open(image_file) as img:
        width, height = img.size
    
    labels = set()  # 중복 방지를 위해 set 사용
    with open(output_label_file, 'w') as f:
        for annotation in data['annotations']:
            label = annotation['label']
            if label in label_map:
                class_id = label_map[label]
                points = annotation['points']
                
                x_min = min(p[0] for p in points)
                y_min = min(p[1] for p in points)
                x_max = max(p[0] for p in points)
                y_max = max(p[1] for p in points)
                
                # 정규화된 좌표 계산
                x_center = (x_min + x_max) / 2 / width
                y_center = (y_min + y_max) / 2 / height
                box_width = (x_max - x_min) / width
                box_height = (y_max - y_min) / height
                
                label_str = f"{class_id} {x_center} {y_center} {box_width} {box_height}"
                
                if label_str not in labels:  # 중복 체크
                    f.write(label_str + "\n")
                    labels.add(label_str)

def find_image_file(label_file):
    base_name = label_file.replace(label_dir, image_dir).replace('라벨', '원천').replace('.json', '').replace('label', 'image')
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
    # 진행도 출력
    if i % percent_step == 0 or i == len(train_files):
        print(f"학습용 데이터 처리 중: {i}/{len(train_files)} ({(i/len(train_files))*100:.2f}%)")

    image_file = find_image_file(label_file)
    if image_file:
        output_label_file = os.path.join(train_label_dir, os.path.basename(label_file).replace('.json', '.txt'))
        convert_annotation(label_file, output_label_file)
        shutil.copy(image_file, train_image_dir)

# 검증용 데이터 처리
for i, label_file in enumerate(val_files, 1):
    # 진행도 출력
    if i % percent_step == 0 or i == len(val_files):
        print(f"검증용 데이터 처리 중: {i}/{len(val_files)} ({(i/len(val_files))*100:.2f}%)")

    image_file = find_image_file(label_file)
    if image_file:
        output_label_file = os.path.join(val_label_dir, os.path.basename(label_file).replace('.json', '.txt'))
        convert_annotation(label_file, output_label_file)
        shutil.copy(image_file, val_image_dir)

print("변환 완료")
