# aapt2 rejects an unescaped apostrophe with a misleading "invalid unicode escape" error, and only
# the full assemble reaches aapt2, so the cheap PR checks never saw it
import glob
import re
import sys

bad = []
for path in sorted(glob.glob("app/src/main/res/values*/*.xml")):
    with open(path, encoding="utf-8") as f:
        for number, line in enumerate(f, 1):
            match = re.search(r"<(string|item)\b[^>]*>(.*)</\1>", line)
            if not match:
                continue
            value = match.group(2)
            if value.startswith('"') and value.endswith('"'):
                continue
            if re.search(r"(?<!\\)'", value) or re.search(r"\\u(?![0-9a-fA-F]{4})", value):
                bad.append(f"{path}:{number}: {value}")

for line in bad:
    print(line)
if bad:
    print("escape apostrophes as \\' (or wrap the whole string in double quotes)")
sys.exit(1 if bad else 0)
