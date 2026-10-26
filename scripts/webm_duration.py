"""webm(Matroska) 파일의 길이(초)를 머리글 Duration 값으로 읽는다(ffprobe 없이). 사용: python3 webm_duration.py 파일.webm"""
import struct
import sys

data = open(sys.argv[1], "rb").read(1 << 20)
scale = 1_000_000  # TimecodeScale 기본값(ns)
i = data.find(b"\x2a\xd7\xb1")
if i >= 0 and data[i + 3] & 0x80:
    n = data[i + 3] & 0x7F
    scale = int.from_bytes(data[i + 4:i + 4 + n], "big")
j = data.find(b"\x44\x89")
if j < 0:
    sys.exit("Duration 없음")
size = data[j + 2] & 0x7F
raw = data[j + 3:j + 3 + size]
value = struct.unpack(">d", raw)[0] if size == 8 else struct.unpack(">f", raw)[0]
print(f"{value * scale / 1e9:.2f}")
