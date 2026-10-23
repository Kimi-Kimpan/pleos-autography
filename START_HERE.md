# 내일 시작하기 — Claude Code로 오토그래피 개발

이 폴더는 Claude Code가 처음부터 프로젝트 맥락을 알고 시작할 수 있도록 만든 시작 패키지예요. (이 파일은 저장소에 올리지 않아도 돼요.)

## 0. 미리 설치

- Android Studio (최신, JDK 17 포함) → SDK Manager에서 Android Automotive 시스템 이미지 설치
- 공모전 가이드 ‘개발 환경’ 페이지의 Pleos Connect SDK와 에뮬레이터
- Git, GitHub CLI(`gh`), Claude Code

## 1. 저장소 만들기 (실명 노출 주의)

```bash
cd pleos-autography
git init
git config user.name "Studio Kimpan"        # 실명 대신 팀명/가명
git config user.email "<가명 또는 noreply 이메일>"
git add . && git commit -m "프로젝트 시작: 기획·디자인 자료"
```

- GitHub 계정 이름에 실명이 들어 있다면, 팀명으로 된 별도 계정이나 조직(Organization)을 만드는 것을 권해요.
- `design/reference/pleos-connect-base.jpg`(Pleos 화면 캡처)는 공개 저장소에 올려도 되는지 확인하기 전까지 `.gitignore`에 추가해 두는 게 안전해요.

## 2. Claude Code 실행 후 첫 요청 예시

폴더에서 `claude`를 실행하면 CLAUDE.md를 자동으로 읽어요. 아래 순서대로 한 단계씩 요청하세요.

1. `CLAUDE.md와 docs/, design/ 문서를 읽고, 마일스톤 1(프로젝트 뼈대)을 위한 계획을 먼저 보여줘.`
2. `계획대로 Android Automotive용 Kotlin + Compose 프로젝트를 만들고 디자인 토큰과 폰트, 책등 레이아웃 컴포넌트를 구현해줘. 빌드까지 확인해줘.`
3. `design/screens 시안대로 Main, Driving, Interview 화면을 데모 데이터로 만들어줘. 만든 뒤 에뮬레이터 스크린샷을 시안과 비교해줘.`
4. `나머지 화면도 같은 방식으로 만들고 docs/screens.md 흐름대로 화면 이동을 연결해줘.`
5. `Fake VehicleState와 디버그용 데모 패널을 만들어서 주행/정차 전환, 정체 15분, 도착 10분 전을 버튼으로 재현할 수 있게 해줘.`
6. `공모전 개발 환경 문서의 Pleos SDK 사용법은 이거야: (붙여넣기). SpeechToText/TextToSpeech 어댑터를 구현해줘.`

💡 Claude Code에 한 번에 다 만들어 달라고 하기보다, 마일스톤마다 빌드와 실행을 확인하고 커밋하는 게 훨씬 안정적이에요.

## 3. 올리기와 제출

```bash
gh repo create pleos-autography --public --source . --push
./gradlew assembleRelease   # 또는 assembleDebug
gh release create v0.1 app/build/outputs/apk/debug/app-debug.apk -t "공모전 MVP"
```

- 저장소 링크와 Releases 링크를 기획서 7번 ‘추가 예정 자료’ 줄에 적어 다시 제출하면 돼요. 마감 전이면 같은 발신 이메일로 최종본을 다시 보낼 수 있어요.
- 시연 영상은 에뮬레이터 화면을 녹화하면 돼요. 데모 패널로 정체 제안 → 인터뷰 → 도착 전 마무리 흐름을 2~3분 안에 보여 주는 구성을 추천해요.
