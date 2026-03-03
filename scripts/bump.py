#!/usr/bin/env python3
"""Tag a release: bumps versionName/versionCode in app/build.gradle, commits, and tags."""

import argparse
import re
import subprocess
import sys
from pathlib import Path

BUILD_GRADLE = Path("app/build.gradle")
RELEASES_MD = Path("docs/releases.md")


def die(msg: str) -> None:
    sys.stderr.write(f"Error: {msg}\n")
    sys.exit(1)


def parse_version(v: str) -> tuple[int, ...]:
    return tuple(int(x) for x in v.split("."))


def valid_version(value: str) -> str:
    if not re.fullmatch(r"\d+\.\d+\.\d+", value):
        raise argparse.ArgumentTypeError(f"version must be in format N.N.N (got '{value}')")
    return value


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Bump version, commit app/build.gradle and docs/releases.md, and tag."
    )
    parser.add_argument("version", type=valid_version, metavar="N.N.N", help="new version number")
    args = parser.parse_args()

    new_version = args.version

    if not BUILD_GRADLE.exists():
        die("run this script from the repo root")

    gradle = BUILD_GRADLE.read_text()

    # Extract current versionName and versionCode
    name_match = re.search(r'versionName "([^"]+)"', gradle)
    code_match = re.search(r"versionCode (\d+)", gradle)
    if not name_match or not code_match:
        die("could not parse versionName/versionCode from app/build.gradle")

    current_name = name_match.group(1)
    current_code = int(code_match.group(1))
    new_code = current_code + 1

    # Warn if new version is not greater than current
    try:
        if parse_version(new_version) <= parse_version(current_name):
            sys.stderr.write(
                f"Warning: new version {new_version} is not greater than current {current_name}\n"
            )
    except ValueError:
        pass  # current version may not be N.N.N format; skip comparison

    # Check releases.md has an entry for this version
    if new_version not in RELEASES_MD.read_text():
        die(
            f"no entry found for {new_version} in {RELEASES_MD}\n"
            "Add release notes before running this script."
        )

    sys.stdout.write(
        f"Releasing {current_name} -> {new_version}  (versionCode {current_code} -> {new_code})\n"
    )

    # Patch build.gradle
    gradle = gradle.replace(
        f'versionName "{current_name}"', f'versionName "{new_version}"'
    )
    gradle = gradle.replace(f"versionCode {current_code}", f"versionCode {new_code}")
    BUILD_GRADLE.write_text(gradle)

    # Commit both files and tag
    subprocess.run(["git", "add", str(BUILD_GRADLE), str(RELEASES_MD)], check=True)
    subprocess.run(["git", "commit", "-m", f"Release {new_version}"], check=True)
    subprocess.run(["git", "tag", new_version], check=True)

    sys.stdout.write(f"\nDone. Commit and tag '{new_version}' created.\n")
    sys.stdout.write("Push with:  git push && git push --tags\n")


if __name__ == "__main__":
    main()
