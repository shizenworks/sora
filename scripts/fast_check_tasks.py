#!/usr/bin/env python3
"""Print the Gradle test tasks the Fast checks workflow runs.

Every module included in settings.gradle.kts that has tests takes part, except :app
(whose Kotlin is compiled by the other job). A Kotlin Multiplatform module with a
jvm() target runs `jvmTest` (`desktopTest` when the target is named desktop); an Android library module runs `testDebugUnitTest`.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
INCLUDE = re.compile(r'^\s*include\(\s*"(:[^"]+)"\s*\)', re.MULTILINE)
TEST_DIRS = ("src/test", "src/commonTest", "src/jvmTest")


def tasks() -> list[str]:
    settings = (ROOT / "settings.gradle.kts").read_text(encoding="utf-8")
    out: list[str] = []
    for path in INCLUDE.findall(settings):
        if path == ":app":
            continue
        module = ROOT / path.lstrip(":").replace(":", "/")
        build = module / "build.gradle.kts"
        if not build.is_file() or not any((module / d).is_dir() for d in TEST_DIRS):
            continue
        script = build.read_text(encoding="utf-8")
        if "multiplatform" in script and 'jvm("desktop")' in script:
            out.append(f"{path}:desktopTest")
        elif "multiplatform" in script and "jvm(" in script:
            out.append(f"{path}:jvmTest")
        elif "com.android.library" in script:
            out.append(f"{path}:testDebugUnitTest")
        elif "jvm" in script:
            out.append(f"{path}:test")
    return out


if __name__ == "__main__":
    result = tasks()
    # Gradle needs at least one task; `help` is a no-op that keeps an empty set green.
    sys.stdout.write(" ".join(result) if result else "help")
    sys.stdout.write("\n")
