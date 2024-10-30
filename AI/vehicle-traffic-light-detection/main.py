from ultralytics import YOLO
import cv2

# 학습된 YOLOv8 모델 로드
model = YOLO("./car.pt")

# 동영상 파일 경로 설정
video_path = "./F3000주간주행영상.mp4"
output_path = "./car.mp4"

# 동영상 파일 읽기
cap = cv2.VideoCapture(video_path)

# 동영상 저장을 위한 설정 (프레임 너비, 높이, FPS 설정)
fourcc = cv2.VideoWriter_fourcc(*'XVID')
out = cv2.VideoWriter(output_path, fourcc, cap.get(cv2.CAP_PROP_FPS), 
                      (int(cap.get(cv2.CAP_PROP_FRAME_WIDTH)), int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))))

while cap.isOpened():
    ret, frame = cap.read()
    if not ret:
        break

    # YOLO 모델로 예측 수행
    result = model.predict(frame)

    # 각 프레임에 대한 예측 결과를 플롯
    for r in result:
        frame_with_detections = r.plot()  # 플롯된 결과를 프레임에 추가

    # 결과 프레임을 출력 창에 표시
    cv2.imshow("YOLOv8 Inference", frame_with_detections)

    # 결과 프레임을 저장
    out.write(frame_with_detections)

    # 'q' 키를 누르면 중지
    if cv2.waitKey(1) & 0xFF == ord('q'):
        break

# 모든 작업 종료 후 자원 해제
cap.release()
out.release()
cv2.destroyAllWindows()
