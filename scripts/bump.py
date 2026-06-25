#!/usr/bin/env python3
"""Bump versionName/versionCode in app/build.gradle.

This only edits the file; it does not commit, tag, or push. Run scripts/publish.py
afterwards to commit and tag the release.
"""

import argparse
import re
import sys
from pathlib import Path

BUILD_GRADLE = Path("app/build.gradle")


def die(msg: str) -> None:
    sys.stderr.write(f"Error: {msg}\n")
    sys.exit(1)


def parse_version(v: str) -> tuple[int, ...]:
    return tuple(int(x) for x in v.split("."))


def valid_version(value: str) -> str:
    if not re.fullmatch(r"\d+\.\d+\.\d+", value):
        raise argparse.ArgumentTypeError(f"version must be in format N.N.N (got '{value}')")
    return value


def bump_patch(version: str) -> str:
    major, minor, patch = parse_version(version)
    return f"{major}.{minor}.{patch + 1}"


def bump_minor(version: str) -> str:
    major, minor, _patch = parse_version(version)
    return f"{major}.{minor + 1}.0"


def read_versions(gradle: str) -> tuple[str, int]:
    name_match = re.search(r'versionName "([^"]+)"', gradle)
    code_match = re.search(r"versionCode (\d+)", gradle)
    if not name_match or not code_match:
        die("could not parse versionName/versionCode from app/build.gradle")
    return name_match.group(1), int(code_match.group(1))


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Bump versionName/versionCode in app/build.gradle (no git operations)."
    )
    parser.add_argument(
        "version", nargs="?", type=valid_version, metavar="N.N.N",
        help="new version number (default: bump patch version)"
    )
    parser.add_argument(
        "--minor", action="store_true",
        help="bump the minor version (resets patch to 0) instead of the patch version"
    )
    args = parser.parse_args()

    if args.minor and args.version:
        die("pass either an explicit version or --minor, not both")

    if not BUILD_GRADLE.exists():
        die("run this script from the repo root")

    gradle = BUILD_GRADLE.read_text()
    current_name, current_code = read_versions(gradle)
    new_code = current_code + 1

    if args.version:
        new_version = args.version
    elif args.minor:
        new_version = bump_minor(current_name)
    else:
        new_version = bump_patch(current_name)

    # Warn if new version is not greater than current
    try:
        if parse_version(new_version) <= parse_version(current_name):
            sys.stderr.write(
                f"Warning: new version {new_version} is not greater than current {current_name}\n"
            )
    except ValueError:
        pass  # current version may not be N.N.N format; skip comparison

    gradle = gradle.replace(
        f'versionName "{current_name}"', f'versionName "{new_version}"'
    )
    gradle = gradle.replace(f"versionCode {current_code}", f"versionCode {new_code}")
    BUILD_GRADLE.write_text(gradle)

    sys.stdout.write(
        f"Bumped {current_name} -> {new_version}  (versionCode {current_code} -> {new_code})\n"
    )
    sys.stdout.write(
        "Add release notes to docs/releases.md, then run 'make publish' to commit and tag.\n"
    )


if __name__ == "__main__":
    main()
