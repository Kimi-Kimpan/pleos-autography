#!/usr/bin/env bash
# 시연 영상용 데모 진행 스크립트(초안). 디버그 빌드가 에뮬레이터에 설치되어 있어야 한다.
#
#   tools/demo/demo.sh            장면마다 Enter를 눌러 진행(발표자가 속도를 정한다)
#   tools/demo/demo.sh --record   위와 같고, 에뮬레이터 화면을 녹화해 captures/demo/에 받는다(최대 3분)
#   tools/demo/demo.sh --reset    시작 전에 앱 데이터를 지우고 예시 답변 3편부터 다시 시작한다
#
# 장면 구성은 docs/demo-video.md 참고. 오토 목소리와 알림 멜로디는 영상 편집에서 입힌다.
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
  "$ADB" shell input swipe 2547 719 1700 719 600
}
# 앱 영역을 한 창(가로 전체)으로: 핸들을 오른쪽 끝으로 되돌린다.
full_window() { "$ADB" shell input swipe 1706 719 2550 719 600; }
step() {
  echo
  echo "▶ $1"
  read -r -p "  Enter를 누르면 진행합니다 " _
}
# 답변 장면: 말을 멈추면 앱이 1.5초 뒤 스스로 받아 적는다. 침묵 감지가 안 되면 a를 입력해 말을 끝낸다.
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
ev real
open_app
sleep 2

if $RECORD; then
  "$ADB" shell rm -f /sdcard/autography-demo.mp4
  "$ADB" shell screenrecord --bit-rate 12000000 --time-limit 180 /sdcard/autography-demo.mp4 &
  REC_PID=$!
  echo "● 녹화 중(최대 3분)"
fi

# ── 장면 ───────────────────────────────────────────────
step "1. 정차 중 홈(가로 전체 창): 챕터 목록과 인사말"
ev park

step "2. 출발: 기어 D → 주행 홈 + 라디오로 자동 전환"
ev drive

step "3. 내비와 나란히(세로 한쪽 창)"
split_with_navi

step "4. 정체 15분 → 알림 멜로디 → 대화 제안"
ev congestion15

step "5. “응” → 인터뷰 시작(오토가 질문을 읽는 시간만큼 기다린 뒤 듣기 시작)"
ev yes

answer_step "6. 질문 4에 답하기: 마이크에 대고 실제로 말한다(파형이 따라 움직인다). 말을 멈추면 받아 적고 진행률이 오른 뒤, 오토가 후속 질문을 한다"

step "7. 복잡한 구간 진입 → 대화를 미룬다(답변은 이미 저장)"
ev complex_start

step "8. 구간 통과 → 알림 멜로디 → 아까 이야기 다시 제안"
ev complex_end

step "8-1. “응” → 후속 질문부터 이어 하기"
ev yes

answer_step "9-1. 질문 5에 답하기 → 후속 질문"
answer_step "9-2. 질문 6에 답하기 → 후속 질문"
answer_step "9-3. 질문 7에 답하기 → ‘첫 차’를 다 채웠어요! (작은 파형으로 계속 듣는다)"

step "10. “여기까지” → 챕터를 완성으로 저장하고 주행 홈으로"
ev finish

step "11. “들려줘” → 내 원고 듣기·고치기(1988년 → 1987년)"
ev listen

step "12. “여기까지” → 주행 홈, 이어서 “오토야, 뭐해?” → 다음 챕터 인터뷰"
ev stop
sleep 1
ev whatsup

step "13. 도착 10분 전 → 새 질문 없이 마무리(3초 뒤 자동 저장, 주행 홈으로)"
ev arrival10

step "14. 도착·정차(P) → 정차 홈: ‘첫 차’ 완료, ‘일과 생계’ 쓰는 중"
full_window
ev park

if $RECORD; then
  step "녹화를 마칩니다"
  kill -INT "$REC_PID" 2>/dev/null || true
  sleep 2
  mkdir -p captures/demo
  "$ADB" pull /sdcard/autography-demo.mp4 "captures/demo/autography-demo-$(date +%Y%m%d-%H%M).mp4"
fi
ev real
echo "끝"
