#!/usr/bin/env bash
# 시연 영상용 데모 진행 스크립트(약 90초, 7장면). 디버그 빌드가 에뮬레이터에 설치되어 있어야 한다.
#
#   tools/demo/demo.sh            장면마다 Enter를 눌러 진행(발표자가 속도를 정한다)
#   tools/demo/demo.sh --reset    시작 전에 앱 데이터를 지우고 시연용 서재(‘첫 차’ 5/7편, 71%)부터 시작한다
#   tools/demo/demo.sh --record   에뮬레이터 화면을 녹화해 captures/demo/에 받는다(최대 3분)
#
# 장면 구성과 오토 대사는 docs/demo-video.md. 오토 목소리와 알림 멜로디는 영상 편집에서 입힌다.
set -euo pipefail

ADB="${ADB:-$HOME/Library/Android/sdk/platform-tools/adb}"
PKG="com.pleosautography.app"
RECORD=false
RESET=false
for arg in "$@"; do
  case "$arg" in
    --record) RECORD=true ;;
    --reset) RESET=true ;;
    *) echo "알 수 없는 옵션: $arg" >&2; exit 1 ;;
  esac
done

ev() { "$ADB" shell am broadcast -a "$PKG.DEMO_EVENT" -p "$PKG" --es event "$1" >/dev/null; }
open_app() { "$ADB" shell am start -n "$PKG/.MainActivity" "$@" >/dev/null; }
# 앱 영역 오른쪽 끝 ⋮ 핸들을 끌어 내비와 나란히 놓는다(2560×1440 에뮬레이터 기준 좌표).
split_with_navi() {
  "$ADB" shell am start -n ai.umos.maps.android.navigation.app/.MainActivity >/dev/null
  sleep 2
  open_app
  sleep 1
  "$ADB" shell input swipe 2547 717 1700 717 600
}
# 앱 영역을 한 창(가로 전체)으로: 핸들을 오른쪽 끝으로 되돌린다.
full_window() { "$ADB" shell input swipe 1706 719 2550 719 600; }
step() {
  echo
  echo "▶ $1"
  read -r -p "  Enter를 누르면 진행합니다 " _
}
# 답변 장면: 말을 멈추면 앱이 1초 뒤 스스로 받아 적는다. 침묵 감지가 안 되면 a를 입력해 말을 끝낸다.
answer_step() {
  echo
  echo "▶ $1"
  local key
  read -r -p "  받아 적었으면 Enter, 강제로 말을 끝내려면 a + Enter " key
  if [[ "$key" == "a" ]]; then ev answer; read -r -p "  받아 적은 것을 확인하고 Enter " _; fi
}

# ── 준비 ───────────────────────────────────────────────
"$ADB" get-state >/dev/null
"$ADB" emu avd hostmicon >/dev/null || true      # Mac 마이크 → 에뮬레이터(에뮬레이터를 켤 때마다)
if $RESET; then "$ADB" shell pm clear --user 10 "$PKG" >/dev/null; fi
for perm in android.car.permission.CAR_SPEED android.permission.RECORD_AUDIO; do
  "$ADB" shell pm grant --user 10 "$PKG" "$perm" || true
done
# 시연용 서재(책이 없을 때만 만든다)와 정차 상태로 연다. 가로 전체 창에서 시작한다.
open_app --es setup demo --es drive park
sleep 2
full_window
sleep 1

if $RECORD; then
  "$ADB" shell rm -f /sdcard/autography-demo.mp4
  "$ADB" shell screenrecord --bit-rate 12000000 --time-limit 180 /sdcard/autography-demo.mp4 &
  REC_PID=$!
  echo "● 녹화 중(최대 3분)"
fi

# ── 장면 ───────────────────────────────────────────────
step "① 정차 홈(가로 전체 창): 챕터 목록과 인사말 — ‘첫 차’ 71%"
ev park

step "② 출발: 기어 D → 주행 홈 + 라디오"
ev drive

step "② 이어서: 내비와 나란히(세로 한쪽 창)"
split_with_navi

step "③ 정체 15분 → 알림 멜로디 → 대화 제안(5초 안에 다음 Enter)"
ev congestion15

step "③ “응” → 인터뷰 시작(질문 6)"
ev yes

answer_step "④ 질문 6에 답하기: 마이크에 대고 말한다. 말을 멈추면 받아 적고, 오토가 후속 질문을 한다"

answer_step "⑤ 질문 7에 답하기 → ‘첫 차’를 다 채웠어요!(작은 파형으로 계속 듣는다)"

step "⑤ “여기까지” → 챕터 완성, 주행 홈(‘일과 생계’)"
ev finish

step "⑥ “오토야, 뭐해?” → ‘일과 생계’ 인터뷰"
ev whatsup

step "⑥ 도착 9분 전(오토가 말하는 중에) → 마무리 화면, 3초 뒤 저장"
ev arrival10

step "⑥ 도착·정차(P) → 정차 홈"
ev park

step "⑦ 가로 전체 창 → ‘첫 차’ 줄을 눌러 원고 보기"
full_window
sleep 2
"$ADB" shell input tap 1100 286       # 02 첫 차(완료) 줄

step "⑦ ‘1988년 겨울’ 문단을 길게 눌러 말로 고치기·다시 듣기·지우기"
"$ADB" shell input swipe 2140 410 2140 410 900

if $RECORD; then
  step "녹화를 마칩니다"
  kill -INT "$REC_PID" 2>/dev/null || true
  sleep 2
  mkdir -p captures/demo
  "$ADB" pull /sdcard/autography-demo.mp4 "captures/demo/autography-demo-$(date +%Y%m%d-%H%M).mp4"
fi
ev real
echo "끝"
