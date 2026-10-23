# CLAUDE.md — 플레오스 오토그래피 (Pleos Autography)

현대자동차그룹 차량용 앱 개발 공모전(Pleos App Market) 출품작이다.
운전 중 차 안의 AI 인터뷰어 **‘오토’**와 음성으로 대화하면, 그 이야기가 챕터형 자서전과 오디오 에피소드로 쌓이는 Android Automotive 앱이다.

- 기획 전문: `docs/proposal.md` (특히 5. 개발 계획)
- 화면 명세와 화면 흐름: `docs/screens.md`
- 디자인 값: `design/tokens.md`
- 화면 시안: `design/screens/*.png`, 브라우저로 열 수 있는 HTML 원본 `design/html/*.html`
- 작업을 시작하기 전에 위 문서를 먼저 읽고, 화면은 PNG 시안과 픽셀 단위로 비교하며 만든다.

## 목표와 일정

- **공모전 MVP** (기획서 5. 구현 범위 1단계)를 Pleos Connect 에뮬레이터에서 실제로 동작시키는 것이 목표다.
- 일정: 11.15 기획서·시안·시연 영상 제출 → 11.23~12.02 실동작 MVP 완성 → 12.03~04 PT 심사 시연.
- GitHub 저장소(소스 + APK)와 시연 영상은 선택 제출이며 가산점이 있다.

## 기술 스택

- Android Automotive OS 기반 Pleos Connect, Kotlin 2.2.10(AGP 9.4.1 내장), minSdk 28, Gradle 9.6, JDK 17
- 패키지명: `com.pleosautography.app`
- UI: Jetpack Compose (단일 Activity)
- 저장: Room (기기 내 로컬 DB, 서버 없음)
- 상태: ViewModel + StateFlow, 단방향 데이터 흐름
- Pleos Connect SDK
  - SpeechToText, TextToSpeech, Vehicle(기어·속도), NaviHelper(경로 안내·남은 시간)
  - 정확한 패키지명과 API는 공모전 가이드의 ‘개발 환경’ 문서를 확인한다. 추측해서 쓰지 말고, 모르면 질문한다.
  - 버전을 올리기 전에 Pleos 저장소의 `maven-metadata.xml`에 그 버전이 있는지 확인한다(예: Vehicle은 2.0.3·2.3.2만 있다).
  - Vehicle SDK 2.3.2에는 **속도 API가 없고**, 기어 API는 deprecated다(“Google GEAR_SELECTION 사용”).
    기어·속도는 Android Car API(`CarPropertyManager`의 `GEAR_SELECTION`, `PERF_VEHICLE_SPEED`)로 읽는다.
    `CAR_SPEED`는 런타임 권한이고, 요청 창에는 ‘위치 정보’로 표시된다.
  - 디버그 빌드에는 런처 아이콘 ‘SDK 확인’(`src/debug`의 `VehicleCheckActivity`)이 있어 기어·속도 값을 바로 볼 수 있다.
  - 음성 확인 결과(2026-10-23, 디버그 ‘음성 확인’ 화면 `VoiceCheckActivity`로 다시 확인할 수 있다)
    - Mac 마이크 → 에뮬레이터: 된다(`adb emu avd hostmicon`, 에뮬레이터를 다시 켜면 다시 실행).
    - Android 표준 TTS: 쓸 수 있는 엔진이 없다. Pleos 엔진(`ai.pleos.playground.caas`)은 표준 API로 연결되지 않는다.
    - Android 표준 음성 인식(`ai.umos.ambientai`): 듣기는 시작하지만 결과·오류를 돌려주지 않는다.
    - Pleos TextToSpeech·SpeechToText SDK 3.3.1은 저장소에 있지만 `registerApp(...)`에 앱 등록 정보가 필요하다. 승인 전까지는 Fake로 시연한다.
- **SDK 어댑터 패턴 (필수)**
  - SDK마다 인터페이스를 둔다: `SpeechInput`, `SpeechOutput`, `VehicleState`, `NaviState`.
  - 구현체는 두 가지를 만든다.
    - Pleos 구현체
    - 에뮬레이터·데모용 Fake 구현체: Android `SpeechRecognizer`·`TextToSpeech`와 시나리오 재생기를 쓴다.
  - 시연에서 정체 15분, 도착 10분 전 같은 상황을 버튼 하나로 재현할 수 있어야 한다. 디버그 빌드에만 데모 패널을 둔다.

## 핵심 규칙 (안전)

1. **주행 중(기어 D/R, 속도 > 0)**
   - 음성 조작과 아래 컨트롤만 활성화한다.
     - 큰 마이크
     - ‘여기까지’(저장 후 중지)
     - 미니 플레이어의 일시정지·다음
     - 오토 툴바
   - 텍스트 입력, 목록 스크롤, 상세 화면 진입은 막는다. 비활성 요소는 opacity 0.32로 보여 준다.
   - Android `CarUxRestrictions`를 따른다.
2. **정차 중(P단)**: 홈의 챕터 목록, 원고 보기·고치기, 라디오, 환경설정, 첫 실행 화면을 연다.
3. **오토가 먼저 말을 걸 때**
   - 항상 짧은 알림 멜로디를 먼저 재생한 뒤 TTS를 시작한다.
   - 트리거는 세 가지다.
     - 긴 직진 15분
     - 정체(저속) 15분
     - 피로 감지(차량 지원 시)
4. **대화를 미루거나 끝내는 경우**
   - 회전·합류가 이어지는 복잡한 구간: 대화를 미루고, 구간을 지나면 다시 제안한다.
   - 목적지까지 10분 이하: 새 질문을 하지 않고 마무리 화면을 띄운다. 3초 뒤 자동 저장하고 주행 홈으로 돌아간다.
   - 연속 주행이 길면 휴식을 권한다. 대화가 휴식을 대신하지 않는다.
5. **중지와 저장**
   - “오토야, 잠깐만” 또는 ‘여기까지’를 누르면 즉시 중지한다.
   - 지금까지의 답변은 항상 저장되어야 한다. 데이터 유실은 없어야 한다.
6. 제안 팝업은 5초 동안 응답이 없으면 스스로 닫힌다. 링 타이머로 보여 준다.

## 음성 명령 (MVP)

| 발화 | 동작 |
|---|---|
| “오토야, 뭐해?” | 현재 챕터 상태를 알리고 인터뷰 시작 제안 |
| “응” / “이어서 하자” | 인터뷰 시작(제안 팝업에서) |
| “오토야, 잠깐만” | 인터뷰 중지 + 저장 |
| “들려줘” / “오토야, 이 이야기 들려줘” | 내 원고 낭독 시작 |
| “방금 거기 고치자” + 정정 내용 | 낭독 중인 문장 수정(예: “1988년이 아니라 1987년이었어”) |

## 인터뷰 엔진 (MVP는 온디바이스)

- 챕터별 **질문 은행**(JSON, `app/src/main/assets/questions/*.json`)을 쓴다.
- 기본 12챕터
  - 01 나의 뿌리 · 02 첫 차 · 03 일과 생계 · 04 가족 · 05 길 위에서 만난 사람들 · 06 가장 길었던 밤
  - 07~12는 질문 은행 파일에서 정의한다.
- 루프: 질문 TTS → 답변 STT → `Answer` 저장 → 진행률 갱신 → 다음 질문
- 챕터 진행률은 답변 수와 질문 수로 계산한다. 100%가 되면 ‘챕터 완성’ 화면을 띄운다.
- 후속 질문 AI 생성, 클라우드 동기화, 원고 자동 정리는 2단계 범위다. MVP에서는 인터페이스 자리만 만든다.

## 데이터 모델 (Room 초안)

- `Book(id, title, ownerCallName, createdAt)` — 자서전 여러 권 지원
- `Chapter(id, bookId, number, title, status[NOT_STARTED|WRITING|DONE], progress)`
- `Question(id, chapterNumber, order, text, followUpHint)` — assets에서 시드
- `Answer(id, chapterId, questionId, transcript, createdAt, durationSec, editedAt?)`
- `Settings(callName, proactiveTalk, chimeOn, radioPublic)`

## 디자인 규칙 요약 (자세한 값은 design/tokens.md)

- 배경 #EFEFEF, 잉크 블루 #1A0BD0. 파란 꽉 찬 버튼은 쓰기·말하기 행동에만 쓴다: 이어쓰기, 이어서 하기.
- UI 글꼴은 Asta Sans다. 오토의 말과 원고는 Gowun Batang(명조)으로 쓰고, 그 아래에 노트 줄을 깐다.
- 세로 앱 화면(3:4) 왼쪽에는 72px **책등 띠**를 둔다.
  - 위에는 자서전 제목을 세로로 쓴다.
  - 아래에는 ‘Autography’ 로고와 설정 버튼을 둔다. 설정 버튼은 주행 중 비활성이다.
- ‘여기까지’ 버튼: 클립 아이콘이 들어간 아웃라인 알약 버튼(저장의 의미)이다.
- 주행 중 마이크: 큰 파란 원 + 은은한 후광이다(움직이는 효과 없음). 인터뷰 화면은 이미 듣는 중이라 마이크 대신 음성 파형을 보여 준다.
- 정보는 1~2초 안에 읽혀야 한다(Pleos 디자인 가이드). 명도 대비는 4.5:1 이상이다. 주행 중에는 자동 애니메이션과 영상을 최소화한다.

## 화면 배치 (Pleos 앱 영역)

- Pleos 앱 영역(1682×1210dp, 160dpi)은 1~2개 창으로 나뉜다. `screenOrientation`은 지정하지 않는다.
- **세로 한쪽 창(841×1210dp)이 기본이다.** 시안 1px = 창 높이 / 1280 dp로 환산한다(`ui/theme/DesignScale.kt`의 `px`, `pxSp`).
  창 폭은 시안(960px)보다 조금 좁으므로(약 890px) 가로는 반응형으로 맞춘다. 고정 폭 대신 weight와 말줄임을 쓴다.
- **가로 전체 창(1682×1210dp)**에서는 늘리지 않고 2단으로 놓는다: 왼쪽 홈(정차 Main / 주행 Driving), 오른쪽 홈에서 연 화면.
- 06·07(오토 툴바·정체 제안)은 내비 위 오버레이가 아니라 **오토그래피 창 안의 카드 + 음성 안내 + (필요 시) 알림**으로 구현한다.
  `SYSTEM_ALERT_WINDOW` 같은 오버레이 권한은 쓰지 않는다.
- 에뮬레이터 팁
  - 창 나누기: 앱 영역 오른쪽 끝의 ⋮ 핸들을 왼쪽으로 끈다. 내비와 오토그래피를 나란히 띄워 시안과 비교한다.
  - 클러스터의 P R N D는 표시만 된다. `cmd car_service`의 속성 명령도 user 빌드라 막혀 있으니, 주행 상태는 에뮬레이터 확장 컨트롤이나 데모 패널(마일스톤 3)로 바꾼다.

## 공모전 규정 (저장소에 반드시 반영)

- **개발자 실명, 개인정보, 계정 정보를 저장소에 노출하지 않는다.**
  - git `user.name`과 커밋 작성자는 팀명이나 가명으로 설정한다.
  - README와 코드 주석에 실명, 연락처, 이메일을 쓰지 않는다.
- API 키와 비밀값은 `local.properties` 또는 환경 변수에만 둔다. 절대 커밋하지 않는다.
- APK는 GitHub Releases에 올린다. 저장소에는 빌드 산출물을 커밋하지 않는다.
- `design/reference/`의 Pleos Connect 화면 캡처는 참고용이다. 공개 저장소에 올릴지는 사용자에게 먼저 확인한다.

## 작업 방식

- 마일스톤 단위로 작게 커밋한다. 커밋 메시지는 한국어로 간단히 쓴다.
- 새 화면을 만들면 에뮬레이터 스크린샷을 찍어 `design/screens/` 시안과 비교한다.
- SDK 동작이 불확실하면 Fake 구현으로 먼저 흐름을 완성한 뒤 Pleos 구현체를 붙인다.
- 빌드 확인: `./gradlew assembleDebug`, 단위 테스트: `./gradlew test`
- 디버그 빌드는 adb로 화면을 바로 연다(시안 비교 스크린샷용):
  `adb shell am start -n com.pleosautography.app/.MainActivity --es screen Nudge --ez timers false`
  - `screen`: `Screen` 이름, `timers false`: 링 타이머 멈춤
- 시안 비교 이미지는 `captures/<마일스톤>/`에 둔다(저장소 제외).

## 마일스톤 (권장 순서)

1. 프로젝트 뼈대: Compose, 디자인 토큰, 폰트, 책등 레이아웃 컴포넌트, 내비게이션
2. 정적 화면 구현: 시안 14장을 데모 데이터로 그리고, 화면 간 이동은 `docs/screens.md` 흐름을 따른다
3. 주행/정차 상태 머신: Fake `VehicleState` + 데모 패널 + `CarUxRestrictions`
4. Room 저장소 + 질문 은행 + 챕터 진행률
5. 음성: STT/TTS 어댑터, 인터뷰 루프, 음성 명령, 알림 멜로디
6. 상황 트리거: 정체 15분 제안, 복잡 구간 미루기, 도착 10분 전 마무리(NaviState Fake로 재현)
7. 내 원고 듣기·고치기
8. Pleos SDK 구현체 연결 → 에뮬레이터 검증 → APK 릴리스 → README·시연 영상
