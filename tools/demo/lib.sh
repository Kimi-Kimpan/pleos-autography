#!/usr/bin/env bash
# demo.sh·scripts/record_scenes.sh가 함께 쓰는 함수(source로 읽는다). 디버그 빌드가 설치되어 있어야 한다.
#   ev <id>              데모 이벤트(drive, park, whatsup, answer …)
#   close_voice_check    디버그 ‘음성 확인’ 화면을 닫아 독에서 빠지게 한다(액티비티는 남긴다)
#   layout_nav_left      내비 대체 화면(NavStandIn) 왼쪽, 오토그래피 오른쪽으로 창을 나눈다(⋮ 손잡이 드래그 포함)
#   layout_full          오토그래피 한 창(가로 전체)으로
#   ui_find <글자>        지금 화면(uiautomator)에서 글자·설명이 맞는 요소의 가운데 "x y"
#   ui_tap <글자> / ui_long <글자> / ui_wait <글자> [초]
#   task_bounds <affinity>   "left top right bottom"

ADB="${ADB:-$HOME/Library/Android/sdk/platform-tools/adb}"
PKG="com.pleosautography.app"
STANDIN="$PKG/.debug.NavStandInActivity"
STANDIN_AFFINITY="$PKG.navstandin"
LIB_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

ev() { "$ADB" shell am broadcast -a "$PKG.DEMO_EVENT" -p "$PKG" --es event "$1" >/dev/null; }
open_app() { "$ADB" shell am start -n "$PKG/.MainActivity" "$@" >/dev/null 2>&1; }

# ‘음성 확인’은 앱과 같은 태스크 위에 쌓이므로, MainActivity를 clear-top으로 열어 걷어 낸다.
close_voice_check() { open_app --activity-clear-top; }

# 태스크(affinity가 정확히 같은 것)의 창 좌표.
task_bounds() {
  "$ADB" shell dumpsys activity activities | python3 -I -c '
import re, sys
want = sys.argv[1]
cur = None
for line in sys.stdin:
    m = re.search(r"\* Task\{.* A=\d+:(\S+) ", line)
    if m:
        cur = m.group(1)
        continue
    b = re.search(r"mBounds=Rect\((\d+), (\d+) - (\d+), (\d+)\)", line)
    if b and cur == want:
        print(*b.groups())
        break
' "$1"
}

# 앱 영역 오른쪽 끝의 ⋮ 손잡이를 왼쪽으로 끈다. 지금 앞의 앱이 왼쪽, 바로 전 앱이 오른쪽에 온다.
_drag_handle_to_split() {
  local b; b=$(task_bounds "$1")
  local right=$(echo "$b" | awk '{print $3}')
  local midy=$(echo "$b" | awk '{print int(($2+$4)/2)}')
  "$ADB" shell input swipe $((right + 12)) "$midy" 1700 "$midy" 600
}

layout_full() {
  local b; b=$(task_bounds "$PKG")
  local l=$(echo "$b" | awk '{print $1}') r=$(echo "$b" | awk '{print $3}')
  if [[ -n "$b" && $((r - l)) -lt 1200 ]]; then
    # 나눈 창이면 가운데 손잡이를 오른쪽 끝으로 민다.
    local midy=$(echo "$b" | awk '{print int(($2+$4)/2)}')
    if [[ "$l" -gt 1200 ]]; then "$ADB" shell input swipe $((l - 12)) "$midy" 2550 "$midy" 600
    else "$ADB" shell input swipe $((r + 12)) "$midy" 2550 "$midy" 600; fi
    sleep 1.5
  fi
}

layout_nav_left() {
  local tries
  for tries in 1 2; do
    local app; app=$(task_bounds "$PKG")
    local nav; nav=$(task_bounds "$STANDIN_AFFINITY")
    if [[ -n "$app" && -n "$nav" && $(echo "$app" | awk '{print $1}') -gt 1200 && $(echo "$nav" | awk '{print $3}') -lt 1800 ]]; then
      open_app; "$ADB" shell am start -n "$STANDIN" >/dev/null 2>&1   # 둘 다 보이게 앞으로
      return 0
    fi
    layout_full
    open_app
    sleep 1.5
    "$ADB" shell am start -n "$STANDIN" >/dev/null
    sleep 2
    _drag_handle_to_split "$STANDIN_AFFINITY"
    sleep 2.5
  done
  echo "창 나누기를 확인하지 못했습니다: app=[$(task_bounds "$PKG")] nav=[$(task_bounds "$STANDIN_AFFINITY")]" >&2
  return 1
}

# uiautomator로 지금 화면의 요소를 찾는다. 글자(text)나 설명(content-desc)이 같으면 우선, 없으면 포함.
# 화면이 계속 움직이면(깜빡이는 커서 등) uiautomator가 덤프하지 못한다. 예전 덤프를 읽지 않게 먼저 지운다.
ui_find() {
  "$ADB" shell rm -f /sdcard/autography-ui.xml
  "$ADB" shell uiautomator dump /sdcard/autography-ui.xml >/dev/null 2>&1
  "$ADB" shell test -s /sdcard/autography-ui.xml || return 1
  "$ADB" exec-out cat /sdcard/autography-ui.xml | python3 -I -c '
import re, sys
want = sys.argv[1]
nodes = []
for n in re.findall(r"<node [^>]*>", sys.stdin.read()):
    a = lambda k: (re.search(k + r"=\"([^\"]*)\"", n) or [None, ""])[1]
    b = re.search(r"bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"", n)
    if b: nodes.append((a("text"), a("content-desc"), [int(v) for v in b.groups()]))
for exact in (True, False):
    for t, d, (l, t2, r, b2) in nodes:
        hit = want in (t, d) if exact else (want in t or want in d)
        if hit and r > l and b2 > t2:
            print((l + r) // 2, (t2 + b2) // 2); sys.exit(0)
sys.exit(1)
' "$1"
}

ui_tap() {
  local xy; xy=$(ui_find "$1") || { echo "화면에서 찾지 못함: $1" >&2; return 1; }
  "$ADB" shell input tap $xy
}

ui_long() {
  local xy; xy=$(ui_find "$1") || { echo "화면에서 찾지 못함: $1" >&2; return 1; }
  "$ADB" shell input swipe $xy $xy 900
}

# 요소가 나타날 때까지 기다린다(기본 15초).
ui_wait() {
  local limit=${2:-15} start=$SECONDS
  until ui_find "$1" >/dev/null; do
    (( SECONDS - start >= limit )) && { echo "기다렸지만 나타나지 않음: $1" >&2; return 1; }
    sleep 0.5
  done
}
