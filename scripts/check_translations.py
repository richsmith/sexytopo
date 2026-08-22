#!/usr/bin/env python3
"""Audit translated string resources against the default strings.xml.

Usage:
    scripts/check_translations.py            # report missing keys per locale
    scripts/check_translations.py --stubs    # also print copy-paste stubs

For every locale under app/src/main/res/values-*/ that has a strings.xml,
this reports which translatable keys from the default values/strings.xml are
missing. Keys marked translatable="false" in the default are ignored, as are
keys that only the default is expected to carry.

Exits non-zero if any locale is missing a translatable key, so it can gate CI.
"""

import re
import sys
from pathlib import Path
from xml.etree import ElementTree

RES_DIR = Path("app/src/main/res")
DEFAULT = RES_DIR / "values" / "strings.xml"

# Matches <string name="..."> elements; we use a regex-free XML parse so that
# attribute order and formatting don't matter.


def load_strings(path: Path) -> dict[str, bool]:
    """Return a map of string name -> translatable flag for one strings.xml."""
    tree = ElementTree.parse(path)
    result: dict[str, bool] = {}
    for el in tree.getroot().findall("string"):
        name = el.get("name")
        if name is None:
            continue
        translatable = el.get("translatable", "true") != "false"
        result[name] = translatable
    return result


def locale_dirs() -> list[Path]:
    dirs = []
    for d in sorted(RES_DIR.glob("values-*")):
        # Skip non-locale qualifiers (screen size, night mode, etc.).
        if not (d / "strings.xml").exists():
            continue
        dirs.append(d)
    return dirs


def main() -> None:
    show_stubs = "--stubs" in sys.argv[1:]

    if not DEFAULT.exists():
        sys.stderr.write(f"{DEFAULT} not found (run from repo root)\n")
        sys.exit(2)

    default = load_strings(DEFAULT)
    translatable_keys = {name for name, ok in default.items() if ok}

    # Default values, for emitting stubs.
    default_values: dict[str, str] = {}
    if show_stubs:
        tree = ElementTree.parse(DEFAULT)
        for el in tree.getroot().findall("string"):
            name = el.get("name")
            if name is not None:
                default_values[name] = "".join(el.itertext())

    any_missing = False
    for locale in locale_dirs():
        present = set(load_strings(locale / "strings.xml"))
        missing = sorted(translatable_keys - present)
        if not missing:
            print(f"{locale.name}: OK")
            continue
        any_missing = True
        print(f"{locale.name}: {len(missing)} missing")
        for name in missing:
            print(f"    {name}")
        if show_stubs:
            print(f"  --- stubs for {locale.name} (translate the values) ---")
            for name in missing:
                value = default_values.get(name, "")
                print(f'    <string name="{name}">{value}</string>')

    if any_missing:
        sys.exit(1)


if __name__ == "__main__":
    main()
