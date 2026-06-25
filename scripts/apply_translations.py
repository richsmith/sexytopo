#!/usr/bin/env python3
"""Insert translated strings into locale strings.xml files.

Input is a JSON object mapping each string key to its translations, keyed by
locale (the locale suffix used in res/values-<locale>):

    {
      "menu_xsection": {"de": "Querschnitt", "fr": "Coupe", "es": "Sección"},
      "settings_survey_title": {"de": "Vermessungseinstellungen", ...}
    }

Locales: de, fr, es, it, pl, pt ("fr" maps to the values-b+fr directory). Any
key or locale may be omitted; only the
translations present are applied. Values are plain natural-language text — the
script handles Android escaping (apostrophe -> \\', and XML &, <, >).

Workflow:
    scripts/check_translations.py            # see what's missing
    # write translations.json as above (an AI or human fills it in)
    scripts/apply_translations.py translations.json   # or read stdin
    ./gradlew spotlessApply

Each string is inserted into app/src/main/res/values-<locale>/strings.xml
immediately after whichever already-present key precedes it in the default
values/strings.xml, so locale files stay parallel to the default ordering.
Keys already present in a locale are skipped, so re-running is safe.
"""

import json
import re
import sys
from pathlib import Path

RES = Path("app/src/main/res")
DEFAULT = RES / "values" / "strings.xml"

LOCALES = ["de", "b+fr", "es", "it", "pl", "pt"]

# Friendly aliases for locale keys in the JSON, so callers can write the plain
# language code rather than the res/values directory suffix.
LOCALE_ALIASES = {"fr": "b+fr"}


def escape(value: str) -> str:
    """Escape a plain string for an Android XML string resource."""
    value = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    # Android treats an unescaped apostrophe as a syntax error.
    value = value.replace("'", "\\'")
    return value


def default_order() -> list[str]:
    order = []
    for line in DEFAULT.read_text().splitlines():
        m = re.search(r'<string name="([^"]+)"', line)
        if m:
            order.append(m.group(1))
    return order


def transpose(by_key: dict[str, dict[str, str]]) -> dict[str, dict[str, str]]:
    """Turn {key: {locale: value}} into {locale: {key: value}}."""
    by_locale: dict[str, dict[str, str]] = {}
    for key, locales in by_key.items():
        for locale, value in locales.items():
            locale = LOCALE_ALIASES.get(locale, locale)
            by_locale.setdefault(locale, {})[key] = value
    return by_locale


def apply_locale(locale: str, strings: dict[str, str], order: list[str]) -> int:
    path = RES / f"values-{locale}" / "strings.xml"
    if not path.exists():
        sys.stderr.write(f"skip values-{locale}: {path} not found\n")
        return 0

    lines = path.read_text().splitlines(keepends=True)
    present: dict[str, int] = {}
    for i, line in enumerate(lines):
        m = re.search(r'<string name="([^"]+)"', line)
        if m:
            present[m.group(1)] = i

    # anchor line index -> list of (default_order_index, new_line)
    inserts: dict[int, list[tuple[int, str]]] = {}
    added = 0
    for key, value in strings.items():
        if key in present:
            continue  # already translated; leave it alone
        if key not in order:
            sys.stderr.write(f"skip {key}: not in default strings.xml\n")
            continue
        idx = order.index(key)
        anchor_line = None
        for j in range(idx - 1, -1, -1):
            if order[j] in present:
                anchor_line = present[order[j]]
                break
        new_line = f'    <string name="{key}">{escape(value)}</string>\n'
        inserts.setdefault(anchor_line if anchor_line is not None else -1, []).append(
            (idx, new_line)
        )
        added += 1

    if added == 0:
        print(f"values-{locale}: nothing to add")
        return 0

    out: list[str] = []
    for i, line in enumerate(lines):
        out.append(line)
        if i in inserts:
            for _, nl in sorted(inserts[i]):
                out.append(nl)
    if -1 in inserts:
        res_idx = next(k for k, l in enumerate(out) if "<resources" in l)
        for _, nl in sorted(inserts[-1], reverse=True):
            out.insert(res_idx + 1, nl)

    path.write_text("".join(out))
    print(f"values-{locale}: inserted {added} strings")
    return added


def main() -> None:
    if not DEFAULT.exists():
        sys.stderr.write(f"{DEFAULT} not found (run from repo root)\n")
        sys.exit(2)

    args = sys.argv[1:]
    if args and args[0] not in ("-", "--stdin"):
        text = Path(args[0]).read_text()
    else:
        text = sys.stdin.read()

    try:
        by_key = json.loads(text)
    except json.JSONDecodeError as e:
        sys.stderr.write(f"invalid JSON: {e}\n")
        sys.exit(1)
    if not isinstance(by_key, dict):
        sys.stderr.write("expected a JSON object mapping key -> {locale: value}\n")
        sys.exit(1)

    by_locale = transpose(by_key)
    unknown = set(by_locale) - set(LOCALES)
    if unknown:
        sys.stderr.write(f"warning: unknown locales ignored: {sorted(unknown)}\n")

    order = default_order()
    total = 0
    for locale in LOCALES:
        if locale in by_locale:
            total += apply_locale(locale, by_locale[locale], order)

    print(f"done: {total} strings inserted; run ./gradlew spotlessApply")


if __name__ == "__main__":
    main()
