# 플레오스 오토그래피 (Pleos Autography)

**플레오스 오토그래피 – 현대차와 함께 쓰는 내 삶의 조각모음**

운전하는 동안 차 안의 AI 인터뷰어 **‘오토’**와 음성으로 대화하면, 그 이야기가 챕터형 자서전과 오디오 에피소드로 쌓이는 Android Automotive(Pleos Connect) 앱입니다.
현대자동차그룹 Pleos App Market 공모전 출품작 · 팀 **Studio Kimpan**

- 시연 영상: https://www.instagram.com/reel/DfAGEjfzTbJ/
- 설치 파일: [`apk/StudioKimpan_PleosAutography_release.apk`](apk/StudioKimpan_PleosAutography_release.apk)

## 주요 기능

- **주행 중**: 말로만 쓰는 화면. 큰 마이크, “오토야, 뭐해?”로 인터뷰 시작, “여기까지”·“오토야, 잠깐만”으로 저장 후 멈춤. 텍스트 입력·목록 스크롤·상세 화면은 막습니다.
- **오토가 먼저 말 걸기**: 정체 15분·긴 직진 15분이면 알림 멜로디 뒤 제안(5초 뒤 자동 닫힘). 복잡한 구간에서는 대화를 미루고, 목적지 10분 전에는 새 질문 없이 마무리·자동 저장합니다.
- **인터뷰**: 질문 → 받아 적어 즉시 저장 → 방금 이야기를 이어받는 후속 질문. 챕터를 다 채우면 완성, ‘여기까지’ 뒤에는 이번 인터뷰 요약(진행률 변화·새로 적힌 문장·대화 시간).
- **정차 중**: 챕터 목록, 원고 보기(이어쓰기·챕터 듣기·말로 고치기·지우기), 오토그래피 라디오, 여러 권 자서전과 환경설정.
- **창 배치**: Pleos 앱 영역의 세로 한쪽 창(내비와 나란히)과 가로 전체 창(2단) 모두 지원합니다.
- 서버 없이 기기 안(Room DB)에만 저장합니다.

## 테스트 환경

| 항목 | 값 |
|---|---|
| 에뮬레이터 | Pleos Connect v2.1.0 (Android Studio AVD, API 34, arm64-v8a) |
| 빌드 | Kotlin 2.2 · AGP 9 · Gradle 9.6 · JDK 17 이상(Android Studio 내장 JBR) |
| minSdk / targetSdk | 28 / 37 |
| UI | Jetpack Compose(단일 Activity) |

## 실행 방법

### APK 설치

```bash
adb install -r apk/StudioKimpan_PleosAutography_release.apk
```

Pleos Connect는 사용자 10에서 앱이 실행됩니다. 처음 실행하면 마이크·차량 속도(‘위치 정보’로 표시) 권한을 묻습니다. 미리 줄 수도 있습니다.

```bash
adb shell pm grant --user 10 com.pleosautography.app android.permission.RECORD_AUDIO
adb shell pm grant --user 10 com.pleosautography.app android.car.permission.CAR_SPEED
```

### 소스 빌드

1. Android Studio로 저장소를 엽니다.
2. `local.properties`에 아래 키를 둡니다(값은 각자 환경. 이 파일은 저장소에 넣지 않습니다).

   | 키 | 용도 |
   |---|---|
   | `sdk.dir` | Android SDK 경로(Android Studio가 자동으로 만듭니다) |
   | `release.storeFile` | release 서명 키스토어 경로(선택) |
   | `release.storePassword` | 키스토어 비밀번호(선택) |
   | `release.keyAlias` | 키 별칭(선택) |
   | `release.keyPassword` | 키 비밀번호(선택) |

   서명 키가 없으면 release는 서명 없이 만들어집니다. 디버그 빌드는 키가 필요 없습니다.
3. 빌드·테스트

```bash
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew test
```

Pleos SDK는 `settings.gradle.kts`의 Pleos maven 저장소(`nexus-playground.pleos.ai`, 계정 없이 접근)에서 받습니다.

## 데모 방법

심사·시연을 위해 **디버그 빌드**(`./gradlew assembleDebug`)에 데모 도구가 들어 있습니다. release APK에는 들어 있지 않습니다.

### 주행/정차 전환

에뮬레이터 클러스터의 P R N D는 표시만 되므로 데모 이벤트로 바꿉니다.

```bash
adb shell am broadcast -a com.pleosautography.app.DEMO_EVENT -p com.pleosautography.app --es event drive
adb shell am broadcast -a com.pleosautography.app.DEMO_EVENT -p com.pleosautography.app --es event park
```

### 데모 패널

책등 아래의 **‘Autography’ 로고를 길게 누르면** 데모 패널이 열려 아래 이벤트를 버튼으로 보낼 수 있습니다.

### 데모 이벤트 목록

| 이벤트 | 동작 |
|---|---|
| `drive` / `park` / `real` | 주행(D·40km/h) / 정차(P) / 실제 차량 값 |
| `congestion15` | 정체 15분 → 대화 제안 |
| `straight15` | 긴 직진 15분 → 대화 제안 |
| `complex_start` / `complex_end` | 복잡한 구간 시작(대화 미룸) / 끝(다시 제안) |
| `arrival10` | 도착 9분 전 → 마무리 화면, 3초 뒤 저장 |
| `yes` | “응”(제안 수락) |
| `whatsup` | “오토야, 뭐해?”(인터뷰 시작) |
| `stop` | “오토야, 잠깐만”(멈추고 저장 → 요약) |
| `listen` | “오토야, 이 이야기 들려줘”(원고 듣기) |
| `fix` | “방금 거기 고치자. 1988년이 아니라 1987년이었어.” |
| `answer` | 지금 말이 끝난 것으로(받아 적기) |
| `more` | 챕터를 다 채운 뒤 더 이야기하기 |
| `finish` | “여기까지” 챕터 마무리 |
| `setup` | 시연용 자서전 준비(책이 없을 때만) |

### 시연 스크립트

```bash
tools/demo/demo.sh --reset                    # 앱 데이터를 지우고 시연용 자서전(‘첫 차’ 71%)부터, 장면마다 Enter
tools/demo/demo.sh --reset --layout nav-left  # 내비(대체 화면) 왼쪽 · 오토그래피 오른쪽
scripts/record_scenes.sh S6                   # 장면 하나 녹화(S3 S5 S6 S7 S8 S8b)
```

화면을 바로 열 수도 있습니다(디버그 빌드).

```bash
adb shell am start -n com.pleosautography.app/.MainActivity --es screen Interview
```

## 알려진 제약

- **음성 대화는 시연용 시나리오입니다.** Pleos TextToSpeech·SpeechToText SDK 사용 승인 전이라, 실제 마이크 음량으로 파형만 움직이고 받아 적는 문장은 준비된 문장(`app/src/main/assets/demo/answers.json`)을 씁니다. 오토의 목소리는 시연 영상에서 편집으로 입혔습니다. 음성 입출력은 어댑터(`SpeechInput`·`SpeechOutput`)로 나뉘어 있어 SDK 구현체만 바꿔 끼우면 됩니다.
- **에뮬레이터에서 Pleos 내비 지도가 표시되지 않습니다**(지도 타일을 받지 못해 흰 화면). 시연 영상의 내비 화면은 디버그 빌드 전용 대체 화면입니다.
- 경로 안내(남은 시간·다음 회전)는 시연용 값이며, NaviHelper 연결은 다음 단계입니다.
- 후속 질문 AI 생성, 클라우드 동기화, 원고 자동 정리, 출판 신청 처리는 2단계 범위입니다(MVP는 접수 안내만).
- 07~12장 질문 은행은 아직 없습니다(01~06장).

## 폴더 구성

| 경로 | 내용 |
|---|---|
| `app/src/main` | 앱 소스(Compose UI, Room, 인터뷰·상황 트리거) |
| `app/src/debug` | 디버그 전용 도구(데모 이벤트 수신, SDK·음성 확인, 내비 대체 화면) |
| `apk/` | 제출용 release APK |
| `docs/` | 기획서·화면 명세·시연 영상 구성 |
| `design/` | 디자인 토큰·화면 시안 |
| `tools/demo`, `scripts/` | 시연·녹화 스크립트 |

© Studio Kimpan
