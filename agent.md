# Agent Instructions

Agent / developer documentation for this project lives in `docs/dev/`:

- `docs/dev/overview.md` — architecture, conventions, build commands, and key modules
- `docs/dev/quick_reference.md` — cheat sheet: key classes, data flows, supported devices/formats
- `docs/dev/survey_data_model.md` — detailed guide to the survey graph model (stations, legs, splays, deletion semantics)
- `docs/dev/sketch_model.md` — sketch drawing layer (detail types, undo/redo, symbols, serialization, rendering)

Start with `overview.md`.

## Code formatting

This project uses [Spotless](https://github.com/diffplug/spotless) with
google-java-format (AOSP style) for Java and ktlint for Kotlin.

- `./gradlew spotlessApply` (or `make format`) — format all files
- `./gradlew spotlessCheck` (or `make check`) — verify formatting (used in CI)

CI runs `make check` on every PR, so misformatted code can't merge.

## Javadoc style

Keep javadoc plain prose. Avoid javadoc tags (`{@code ...}`, `{@link ...}`, `@param`, `@return`, etc.) unless they're genuinely needed — most of the time they aren't. In particular, **do not wrap identifiers in `{@code ...}`**: write `onReady` rather than `{@code onReady}`. The same goes for inline references to methods, classes, and constants.

Reach for tags only when something would otherwise be ambiguous or unrenderable (e.g. `<` in a generic, an HTML-sensitive character, or a cross-reference that genuinely benefits from being a clickable link).

## Documentation maintenance

When implementing new user-facing features or significantly changing existing behaviour, offer to update:

- **User manual** (`app/src/main/assets/guide/index.html`) — a single HTML file shown in-app via Help → Manual. Keep the tone consistent with existing sections. The manual is also deployed to GitHub Pages automatically.
- **Release notes** (`docs/releases.md`) — add a summary line under the `# Unreleased` heading.
