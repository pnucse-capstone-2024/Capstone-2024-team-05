### 1. 프로젝트 소개

#### 1.1. 배경 및 필요성

운전은 현대 생활에서 필수적인 기술로, 경찰청에서 제공한 2022년 교통통계분석에 따르면 전체 인구의 약 66%가 운전 면허를 소지하고 있습니다. 이는 운전이 일상생활에서 중요한 역할을 하고 있음을 나타냅니다. 하지만 많은 초보 운전자들은 도로 위에서 불안과 어려움을 겪고 있으며, 교통 상황에 대한 빠른 판단과 차량 조작에 어려움을 느껴 사고 위험성이 높아지고 있습니다.

고령 운전자들도 유사한 문제를 겪고 있습니다. 도로교통공단에서 제공한 2022년 교통사고 통계에 따르면, 전체 교통사고는 감소했으나 고령 운전자가 일으킨 사고는 오히려 증가했습니다. 고령 운전자들은 반응 속도 저하와 인지 기능 약화로 인해 사고 위험이 높습니다. 교통 사고는 개인과 사회에 큰 피해를 초래할 수 있으므로, 이를 줄이기 위한 노력이 필요합니다.
![연도별 교통 사고 건수](https://github.com/user-attachments/assets/7da6d111-625a-4481-b094-51ca5fc632ce)

기존의 네비게이션 시스템과 운전 보조 장치는 일반 운전자들을 대상으로 설계되어 있어, 특별한 지원이 필요한 운전 미숙자들에게 충분한 도움을 제공하지 못하고 있습니다. 또한, 고령 운전자들을 위한 운전 면허 반납제나 조건부 운전 면허와 같은 정책은 실질적인 해결책이 되지 못하고 있습니다. 초보 운전자와 고령 운전자에게는 보다 세심하고 맞춤형 운전 보조가 필요합니다.

#### 1.2. 목표 및 주요 내용

본 프로젝트의 목표는 운전 미숙자(초보 운전자 및 고령 운전자)를 위한 맞춤형 운전 보조 애플리케이션을 개발하여 이들의 운전 경험을 개선하고 교통 안전을 증진하는 것입니다. 기존의 플랫폼과 차별화하여 운전 미숙자들에게 보다 세심하고 맞춤형 서비스를 제공합니다. 이를 통해 고령화 사회에서 증가하는 고령 운전자 사고 문제 해결에 기여하고자 합니다.

구체적인 목표 및 주요 내용은 다음과 같습니다:

-  **실시간 교통 상황 파악**: 스마트폰 후면 카메라와 GPS 기능을 활용하여 운전자가 실시간으로 교통 상황을 파악하고 안전하게 운전할 수 있도록 지원합니다.
-  **안전 운전 경고 제공**: 전방 차량과의 안전 거리 유지, 차선 이탈 방지, 제한 속도 준수를 위한 실시간 경고를 제공합니다.
-  **운전 교육 및 피드백 제공**: 운전 기록을 대시보드 형태로 제공하여 운전 습관을 개선하고, 게임화된 미션을 통해 재미있게 연습할 수 있는 환경을 조성합니다.
-  **안전한 운전 경로 안내**: 초보 운전자가 연습하기 좋은, 교통량이 적고 사고 발생률이 낮은 안전한 도로 구역을 안내합니다.
-  **사고 대응 지원**: 사고 발생 시 상황별 대응법 안내와 자동차 보험 회사와의 신속한 연결을 지원합니다.

이 애플리케이션을 통해 운전 미숙자들이 도로 위에서 느끼는 불안감을 해소하고, 보다 안전하고 자신감 있게 운전할 수 있도록 돕는 것을 목표로 합니다. 또한, 고령 운전자 사고 증가 문제를 해결하는 실질적인 방안 중 하나로 작용할 것입니다.

### 2. 상세설계

#### 2.1. 시스템 구성도

![시스템구성도_코드비전](https://github.com/user-attachments/assets/08a0b965-7882-4c98-952d-893c52f09297)

#### 2.2. 사용 기술

-  **Android SDK 및 Java 버전**
   -  Compile SDK: 34
   -  Min SDK: 26
   -  Target SDK: 34
   -  Java Version: 17
-  **YOLO 및 OpenCV 버전**
   -  YOLO: 8.3.23 (YOLOv8n, YOLOv8n-seg)
   -  OpenCV: 4.10.0
-  **TensorFlow Lite**
   -  TensorFlow Lite: `org.tensorflow:tensorflow-lite:2.14.0`
   -  TensorFlow Lite Support Library: `org.tensorflow:tensorflow-lite-support:0.4.4`
   -  TensorFlow Lite GPU: `org.tensorflow:tensorflow-lite-gpu:2.10.0`

### 3. 설치 및 사용 방법

#### 3.1. 제품 설치를 위한 준비

- **필수 소프트웨어**: Android Studio 및 Java SDK 1.8 이상이 필요합니다.
- **API 키 발급**: T map API를 사용하기 위해 T map 개발자 사이트에서 API 키를 발급받아야 합니다.

#### 3.2. 설치 방법

1. **API 키 설정**: 발급받은 T map API 키를 `local.properties` 파일에 추가합니다.
   ```properties
   API_KEY=발급받은_API_KEY
   ```

### 4. 소개 및 시연 영상

[![운전 미숙자를 위한 운전 보조 애플리케이션](https://img.youtube.com/vi/B24bSNfnOq8/0.jpg)](https://youtu.be/B24bSNfnOq8)

### 5. 팀 소개

| **김대길** | **김주송** | **이지수** |
|:-:|:-:|:-:|
| <img src="https://github.com/mong3125.png" width="100" height="100" style="border-radius: 50%;"> | <img src="https://github.com/jooiss.png" width="100" height="100" style="border-radius: 50%;"> | <img src="https://github.com/dlwltn0430.png" width="100" height="100" style="border-radius: 50%;"> |
| **AI** | **Android** | **AI** |
| - 주변 차량, 보행자, 신호 인식을 위한 논문 탐색 및 관련 코드 분석<br> - 주변 차량, 보행자, 신호 인식을 위한 데이터셋 전처리 및 가공<br> - YOLOv8 모델의 성능과 구조에 대한 분석 및 연구<br> - YOLOv8 기반의 주변 차량, 보행자, 신호 인식 모델 설계 및 구현<br> - 탐지 결과를 화면에 표시하는 후처리 코드 구현 | - 애플리케이션 개발<br> - 피그마를 활용한 애플리케이션 UI 설계 및 구현<br> - 초보 운전자 연수 기능 설계<br> - 피드백 대시보드 기능 설계<br> - 긴급상황 대응 기능 설계 | - 차선 인식을 위한 논문 탐색 및 관련 코드 분석<br> - 차선 인식을 위한 데이터셋 전처리 및 가공<br> - OpenCV 기반의 차선 인식 모델 설계 및 구현<br> - 탐지 결과를 화면에 표시하는 후처리 코드 구현<br> - 피그마를 활용한 애플리케이션 UI 설계 및 구현 |
| [Github](https://github.com/mong3125) | [Github](https://github.com/jooiss) | [Github](https://github.com/dlwltn0430) |
| mong3125@pusan.ac.kr | jusong513@pusan.ac.kr | dlwltn0430@pusan.ac.kr |

