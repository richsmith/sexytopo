#!/usr/bin/env python3
"""Commit and tag a release using the version already set in app/build.gradle.

Run scripts/bump.py first to set the version. This reads the current versionName,
checks docs/releases.md has an entry for it, commits build.gradle + releases.md, and
creates the tag. It does not push; pushing the tag is what triggers the release
workflow, so that step is left to you.
"""

import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from bump import BUILD_GRADLE, die, read_versions

RELEASES_MD = Path("docs/releases.md")


def main() -> None:
    import subprocess

    if not BUILD_GRADLE.exists():
        die("run this script from the repo root")

    version, _code = read_versions(BUILD_GRADLE.read_text())

    if version not in RELEASES_MD.read_text():
        die(
            f"no entry found for {version} in {RELEASES_MD}\n"
            "Add release notes before publishing."
        )

    # Missing translations fall back to English rather than crashing, so warn (and let
    # the user decide) rather than hard-blocking the release.
    check = subprocess.run([sys.executable, "scripts/check_translations.py"])
    if check.returncode != 0:
        sys.stderr.write("\nSome translations are missing (see above).\n")
        if input("Publish anyway? [y/N] ").strip().lower() not in ("y", "yes"):
            die("aborted; fill in translations or re-run to continue")

    tag_exists = subprocess.run(
        ["git", "rev-parse", "-q", "--verify", f"refs/tags/{version}"],
        stdout=subprocess.DEVNULL,
    )
    if tag_exists.returncode == 0:
        die(f"tag '{version}' already exists")

    subprocess.run(["git", "add", str(BUILD_GRADLE), str(RELEASES_MD)], check=True)
    subprocess.run(["git", "commit", "-m", f"Release {version}"], check=True)
    subprocess.run(["git", "tag", version], check=True)

    sys.stdout.write(f"\nDone. Commit and tag '{version}' created.\n")
    sys.stdout.write("Push to trigger the release:  git push && git push --tags\n")


if __name__ == "__main__":
    main()
