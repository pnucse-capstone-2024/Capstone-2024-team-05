from ultralytics import YOLO

# 학습 시작
if __name__ == '__main__':
    # Load the YOLOv8 model
    model = YOLO("yolov8n.pt")

    results = model.train(data='./data.yaml', epochs=100)
