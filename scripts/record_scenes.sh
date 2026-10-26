#!/usr/bin/env bash
# 시연 영상 장면 녹화(에뮬레이터 쪽 녹화, 원본 해상도) → captures/video/<장면>.webm
#
#   scripts/record_scenes.sh            모든 장면(S3 S5 S6 S7 S8 S8b)
#   scripts/record_scenes.sh S6         장면 하나만
#   scripts/record_scenes.sh S7 S8      여러 장면
#   REC=device scripts/record_scenes.sh S6   기기 쪽 녹화(adb shell screenrecord, mp4)로. 에뮬레이터 쪽 녹화가
#                                            호스트 GPU(Metal) 오류로 에뮬레이터를 멈추게 할 때 쓴다.
#
# 모든 장면은 tools/demo/demo.sh --reset --layout nav-left --prepare-only로 시작한다
# (앱 데이터 초기화 → 시연용 서재 ‘첫 차’ 71% → 내비 대체 화면 왼쪽·오토그래피 오른쪽).
# 녹화 앞 1초·뒤 1.5초는 움직임 없는 화면이다. 화면 요소는 uiautomator로 찾아 누른다(좌표를 적지 않는다).
# 결과: captures/video/<장면>.webm, <장면>_last.png(마지막 화면), S8_last_app.png(S8 끝의 오토그래피 창만), layout.txt
# 디버그 빌드가 설치되어 있어야 한다. 에뮬레이터는 Android Studio에서 켠다.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
source "$ROOT/tools/demo/lib.sh"
OUT="$ROOT/captures/video"
# 에뮬레이터 콘솔은 공백이 든 경로를 받지 못해 임시 폴더에 녹화한 뒤 옮긴다.
TMP="${TMPDIR:-/tmp}/autography-rec"
mkdir -p "$OUT" "$TMP"

REC="${REC:-emu}"
EXT=$([[ "$REC" == "device" ]] && echo mp4 || echo webm)
ALL=(S3 S5 S6 S7 S8 S8b)
SCENES=("$@")
[[ ${#SCENES[@]} -eq 0 ]] && SCENES=("${ALL[@]}")

HEAD_HOLD=1
TAIL_HOLD=1.5
REPORT=()

prepare() {
  "$ROOT/tools/demo/demo.sh" --reset --layout nav-left --prepare-only
  # 장면은 adb 이벤트로만 답한다. Mac 마이크를 켜 두면 주변 소리가 답으로 받아 적혀 장면이 어긋난다.
  "$ADB" emu avd hostmicoff >/dev/null || true
  sleep 1
  write_layout
}

write_layout() {
  {
    echo "# 화면 좌표(px, 2560×1440): left top right bottom"
    echo "autography $(task_bounds "$PKG")"
    echo "nav        $(task_bounds "$STANDIN_AFFINITY")"
  } > "$OUT/layout.txt"
}

rec_start() {
  if [[ "$REC" == "device" ]]; then
    "$ADB" shell rm -f "/sdcard/$1.mp4"
    "$ADB" shell screenrecord --bit-rate 12000000 --time-limit 170 "/sdcard/$1.mp4" &
    REC_PID=$!
    sleep 0.8 # 녹화가 시작될 때까지
  else
    rm -f "$TMP/$1.webm"
    "$ADB" emu screenrecord start --bit-rate 12M --fps 30 "$TMP/$1.webm" >/dev/null
  fi
  REC_T0=$SECONDS
  sleep "$HEAD_HOLD"
}

rec_stop() {
  sleep "$TAIL_HOLD"
  "$ADB" exec-out screencap -p > "$OUT/$1_last.png"
  local dur
  if [[ "$REC" == "device" ]]; then
    "$ADB" shell pkill -INT screenrecord || true
    wait "$REC_PID" 2>/dev/null || true
    sleep 1
    "$ADB" pull "/sdcard/$1.mp4" "$OUT/$1.mp4" >/dev/null
    dur=$((SECONDS - REC_T0))
  else
    "$ADB" emu screenrecord stop >/dev/null
    sleep 2 # 파일을 다 쓸 때까지
    mv -f "$TMP/$1.webm" "$OUT/$1.webm"
    dur=$(python3 -I "$ROOT/scripts/webm_duration.py" "$OUT/$1.webm" 2>/dev/null || echo "?")
  fi
  REPORT+=("$1  ${dur}s  captures/video/$1.$EXT  ($1_last.png)")
}

# 인터뷰에서 답이 저장될 때까지(받아 적기) 기다린다. 이전 저장 수보다 늘면 끝.
saves() { "$ADB" logcat -d -s InterviewSession:I | grep -c "저장" || true; }
answer_and_wait() {
  local before; before=$(saves)
  "$ADB" shell am broadcast -a "$PKG.DEMO_EVENT" -p "$PKG" --es event "$1" >/dev/null
  local start=$SECONDS
  until [[ $(saves) -gt $before ]]; do
    (( SECONDS - start > 10 )) && { echo "받아 적지 않음($1)" >&2; return 1; }
    sleep 0.3
  done
}

# ── 장면 ───────────────────────────────────────────────

# S3 주행 중 오토그래피 주행 홈, 내비 지도가 흐르는 채로 12초.
scene_S3() {
  ev drive
  sleep 2
  rec_start S3
  sleep 12
  rec_stop S3
}

# S5 주행 중 “오토야, 뭐해?” → 인터뷰 화면.
scene_S5() {
  ev drive
  sleep 2
  rec_start S5
  ev whatsup
  ui_wait "오토가 물었어요" 10
  sleep 2
  rec_stop S5
}

# S6 인터뷰: answer → 파형(후속 질문을 듣는 중) → more → 한 번 더.
# ‘일과 생계’(3장, 0/5)로 시작해 네 번 답해도 챕터가 다 차지 않게 한다.
scene_S6() {
  ui_tap "3장 시작하기"
  ui_wait "오토가 물었어요" 10
  ev drive
  sleep 6 # 첫 질문을 읽고 듣기 시작할 때까지
  "$ADB" logcat -c
  rec_start S6
  local round
  for round in 1 2; do
    sleep 1.5
    answer_and_wait answer
    sleep 7.5 # 후속 질문을 읽고 다시 파형
    answer_and_wait more
    sleep 7.5
  done
  rec_stop S6
}

# S7 정차 → 홈 → 원고 보기 → 문단을 길게 눌러 말로 고치기(인라인 입력창) → 확인 → 원고에 반영.
scene_S7() {
  ev park
  sleep 1.5
  rec_start S7
  ui_tap "원고 보기"
  # 세로 창에서는 원고 끝의 안내가 화면 밖이라 보이지 않으므로 맨 위 안내를 기다린다.
  ui_wait "고칠 부분을 길게 눌러 고르면, 말로 고칠 수 있어요" 8
  sleep 1.5
  ui_long "1988년"
  ui_wait "말로 고치기" 5
  sleep 1
  ui_tap "말로 고치기"
  ui_wait "고칠 내용을 말씀해 주세요" 5
  sleep 1.5
  ev answer # 시연: 준비된 정정 문장을 받아 적는다
  sleep 4   # 입력창에 타이핑 → 취소선 미리보기
  ui_tap "확인"
  sleep 2.5
  rec_stop S7
}

# S8 정차 중 미니 플레이어 ⌃ → 플레이어가 아래에서 올라옴 → 3초.
scene_S8() {
  ev park
  sleep 1.5
  rec_start S8
  ui_tap "플레이어 펼치기"
  sleep 3.3
  rec_stop S8
  # 오토그래피 창만 잘라 표지 덮기 장면에 잇는다.
  local b; b=$(task_bounds "$PKG")
  read -r l t r bt <<< "$b"
  sips -c $((bt - t)) $((r - l)) --cropOffset "$t" "$l" "$OUT/S8_last.png" --out "$OUT/S8_last_app.png" >/dev/null
}

# S8b 여기까지 → 요약 → 나와 비슷한 이야기 듣기 → 홈 + 플레이어.
scene_S8b() {
  ev park
  sleep 1.5
  ui_tap "이어쓰기"
  ui_wait "오토가 물었어요" 10
  sleep 4
  rec_start S8b
  ui_tap "여기까지"
  ui_wait "나와 비슷한 이야기 듣기" 8
  sleep 2.5
  ui_tap "나와 비슷한 이야기 듣기"
  sleep 3
  rec_stop S8b
}

# ── 실행 ───────────────────────────────────────────────
# 중간에 실패해도 녹화를 멈추고 마이크를 되돌린다.
cleanup() {
  if [[ "$REC" == "device" ]]; then "$ADB" shell pkill -INT screenrecord >/dev/null 2>&1 || true
  else "$ADB" emu screenrecord stop >/dev/null 2>&1 || true; fi
  "$ADB" emu avd hostmicon >/dev/null 2>&1 || true
}
trap 'cleanup' ERR INT
"$ADB" get-state >/dev/null
for s in "${SCENES[@]}"; do
  if ! declare -F "scene_$s" >/dev/null; then echo "모르는 장면: $s (가능: ${ALL[*]})" >&2; exit 1; fi
done
for s in "${SCENES[@]}"; do
  echo "▶ $s 준비"
  prepare
  echo "● $s 녹화"
  "scene_$s"
done
"$ADB" emu avd hostmicon >/dev/null || true # 다음 시연을 위해 마이크를 되돌린다
echo
echo "장면  길이  파일"
printf '%s\n' "${REPORT[@]}"
echo "창 좌표: captures/video/layout.txt"
