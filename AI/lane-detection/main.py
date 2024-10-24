import cv2
import numpy as np
import math
import time

# ROI 추출
def region_of_interest(img, vertices):
    mask = np.zeros_like(img)
    match_mask_color = 255
    cv2.fillPoly(mask, vertices, match_mask_color)
    masked_image = cv2.bitwise_and(img, mask)
    return masked_image

# 차선 영역 색칠하기
def draw_lane_lines(img, left_line, right_line, color=[0, 255, 0], thickness=10):
    line_img = np.zeros_like(img)
    poly_pts = np.array([[
        (left_line[0], left_line[1]),
        (left_line[2], left_line[3]),
        (right_line[2], right_line[3]),
        (right_line[0], right_line[1])
    ]], dtype=np.int32)
    
    cv2.fillPoly(line_img, poly_pts, color)

    # 원본 이미지에 차선 영역 표시하기
    img = cv2.addWeighted(img, 0.8, line_img, 0.5, 0.0)
    return img

def pipeline(image):
    height = image.shape[0]
    width = image.shape[1]
    region_of_interest_vertices = [
        (0, height),
        (width / 2, height / 2),
        (width, height),
    ]

    # Grayscale로 변환
    gray_image = cv2.cvtColor(image, cv2.COLOR_RGB2GRAY)
    
    # Canny edge detection 적용
    cannyed_image = cv2.Canny(gray_image, 100, 200)

    cropped_image = region_of_interest(
        cannyed_image,
        np.array([region_of_interest_vertices], np.int32)
    )

    # Hough Line Transformation 수행
    lines = cv2.HoughLinesP(
        cropped_image,
        rho=6,
        theta=np.pi / 60,
        threshold=160,
        lines=np.array([]),
        minLineLength=40,
        maxLineGap=25
    )
    
    # 왼쪽 차선, 오른쪽 차선 분리
    left_line_x = []
    left_line_y = []
    right_line_x = []
    right_line_y = []

    if lines is None:
        return image

    for line in lines:
        for x1, y1, x2, y2 in line:
            slope = (y2 - y1) / (x2 - x1) if (x2 - x1) != 0 else 0
            if math.fabs(slope) < 0.5: 
                continue
            if slope <= 0:  # 왼쪽 차선
                left_line_x.extend([x1, x2])
                left_line_y.extend([y1, y2])
            else:  # 오른쪽 차선
                right_line_x.extend([x1, x2])
                right_line_y.extend([y1, y2])

    min_y = int(image.shape[0] * (3 / 5))  
    max_y = image.shape[0]  

    if left_line_x and left_line_y:
        poly_left = np.poly1d(np.polyfit(left_line_y, left_line_x, deg=1))
        left_x_start = int(poly_left(max_y))
        left_x_end = int(poly_left(min_y))
    else:
        left_x_start, left_x_end = 0, 0  

    if right_line_x and right_line_y:
        poly_right = np.poly1d(np.polyfit(right_line_y, right_line_x, deg=1))
        right_x_start = int(poly_right(max_y))
        right_x_end = int(poly_right(min_y))
    else:
        right_x_start, right_x_end = 0, 0  

    lane_image = draw_lane_lines(
        image,
        [left_x_start, max_y, left_x_end, min_y],
        [right_x_start, max_y, right_x_end, min_y]
    )

    return lane_image

def process_video():
    cap = cv2.VideoCapture('video/car.mp4')

    if not cap.isOpened():
        print("Error: Unable to open video file.")
        return

    target_fps = 30
    frame_time = 1.0 / target_fps 

    # 1280x720로 resize
    cap.set(cv2.CAP_PROP_FRAME_WIDTH, 1280)
    cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 720)

    frame_count = 0
    while cap.isOpened():
        ret, frame = cap.read()

        if not ret:
            break

        resized_frame = cv2.resize(frame, (1280, 720))

        lane_frame = pipeline(resized_frame)

        frame_count += 1

        cv2.imshow('Lane and Car Detection', lane_frame)

        time.sleep(frame_time)

        if cv2.waitKey(1) & 0xFF == ord('q'):
            break

    cap.release()
    cv2.destroyAllWindows()

process_video()