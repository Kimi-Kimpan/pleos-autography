#!/usr/bin/env bash
# 시연 영상용 데모 진행 스크립트(약 90초, 7장면). 디버그 빌드가 에뮬레이터에 설치되어 있어야 한다.
#
#   tools/demo/demo.sh            장면마다 Enter를 눌러 진행(발표자가 속도를 정한다)
#   tools/demo/demo.sh --reset    시작 전에 앱 데이터를 지우고 시연용 서재(‘첫 차’ 5/7편, 71%)부터 시작한다
#   tools/demo/demo.sh --record   에뮬레이터 화면을 녹화해 captures/demo/에 받는다(최대 3분)
#   tools/demo/demo.sh --layout nav-left   내비와 나란히 놓을 때 내비 대체 화면(디버그 NavStandIn)을 왼쪽,
#                                          오토그래피를 오른쪽에 둔다(기본은 오토그래피 왼쪽·실제 내비 오른쪽)
#   tools/demo/demo.sh --prepare-only      준비(초기화·권한·시연용 서재·창 배치)만 하고 끝낸다(scripts/record_scenes.sh가 쓴다)
#
# 장면 구성과 오토 대사는 docs/demo-video.md. 오토 목소리와 알림 멜로디는 영상 편집에서 입힌다.
set -euo pipefail

ADB="${ADB:-$HOME/Library/Android/sdk/platform-tools/adb}"
PKG="com.pleosautography.app"
RECORD=false
RESET=false
LAYOUT="app-left"
PREPARE_ONLY=false
while [[ $# -gt 0 ]]; do
  case "$1" in
    --record) RECORD=true ;;
    --reset) RESET=true ;;
    --prepare-only) PREPARE_ONLY=true ;;
    --layout)
      shift
      case "${1:-}" in
        nav-left|app-left) LAYOUT="$1" ;;
        *) echo "--layout 값은 nav-left 또는 app-left: ${1:-}" >&2; exit 1 ;;
      esac
      ;;
    *) echo "알 수 없는 옵션: $1" >&2; exit 1 ;;
  esac
  shift
done

source "$(dirname "$0")/lib.sh"
# 앱 영역 오른쪽 끝 ⋮ 핸들을 끌어 내비와 나란히 놓는다(2560×1440 에뮬레이터 기준 좌표).
# 핸들을 끌면 지금 앞에 있는 앱이 왼쪽, 바로 전에 쓴 앱이 오른쪽에 온다.
#   app-left(기본): 실제 내비 → 오토그래피 순으로 열고 끈다 → 오토그래피 왼쪽, 내비 오른쪽
#   nav-left:       오토그래피 → 내비 대체 화면 순(lib.sh layout_nav_left) → 내비 왼쪽, 오토그래피 오른쪽
#                   에뮬레이터의 Pleos 내비는 지도 타일을 받지 못해 하얗게 보여서 직접 그린 지도로 대신한다.
NAVI="ai.umos.maps.android.navigation.app/.MainActivity"
split_with_navi() {
  if [[ "$LAYOUT" == "nav-left" ]]; then
    layout_nav_left
    return
  fi
  "$ADB" shell am start -n "$NAVI" >/dev/null
  sleep 2
  open_app
  sleep 1
  "$ADB" shell input swipe 2547 717 1700 717 600
}
# 앱 영역을 한 창(가로 전체)으로.
full_window() { layout_full; }
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
# 시연용 서재(책이 없을 때만 만든다)와 정차 상태로 연다. 디버그 ‘음성 확인’은 닫는다.
open_app --es setup demo --es drive park
sleep 3
close_voice_check
sleep 1
if $PREPARE_ONLY; then
  # 녹화 스크립트용: 처음부터 내비와 나란히 둔다.
  if [[ "$LAYOUT" == "nav-left" ]]; then layout_nav_left; else split_with_navi; fi
  exit 0
fi
# 가로 전체 창에서 시작한다.
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
