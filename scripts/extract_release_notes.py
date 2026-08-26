#!/usr/bin/env python3
"""Extract release notes for a given version from docs/releases.md.

Usage:  scripts/extract_release_notes.py <version>

Prints the bullets under the matching '# <date> <version>' heading to stdout.
Exits non-zero if the version is not found.
"""

import re
import sys
from pathlib import Path

RELEASES_MD = Path("docs/releases.md")


def main() -> None:
    if len(sys.argv) != 2:
        sys.stderr.write("usage: extract_release_notes.py <version>\n")
        sys.exit(2)

    version = sys.argv[1]

    if not RELEASES_MD.exists():
        sys.stderr.write(f"{RELEASES_MD} not found (run from repo root)\n")
        sys.exit(2)

    text = RELEASES_MD.read_text()
    pattern = re.compile(
        rf"^#\s+\S+\s+{re.escape(version)}\s*\n(.*?)(?=^#\s|\Z)",
        re.MULTILINE | re.DOTALL,
    )
    match = pattern.search(text)
    if not match:
        sys.stderr.write(f"no entry found for version {version} in {RELEASES_MD}\n")
        sys.exit(1)

    sys.stdout.write(match.group(1).strip() + "\n")


if __name__ == "__main__":
    main()
