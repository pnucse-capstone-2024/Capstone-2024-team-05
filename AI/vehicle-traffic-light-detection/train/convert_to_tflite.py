import ultralytics

if __name__ == '__main__':
    # 모델 로드
    model = ultralytics.YOLO("./runs/detect/train8/weights/best.pt")
    
    # 모델을 ONNX 형식으로 내보내기
    model.export(format='tflite')
