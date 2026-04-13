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

- `./gradlew spotlessApply` — format all files
- `./gradlew spotlessCheck` — verify formatting (used in CI)

A pre-commit hook at `scripts/git-hooks/pre-commit` runs `spotlessApply`
on staged files automatically. Enable it once per clone with:

```
git config core.hooksPath scripts/git-hooks
```

## Documentation maintenance

When implementing new user-facing features or significantly changing existing behaviour, offer to update:

- **User manual** (`app/src/main/assets/guide/index.html`) — a single HTML file shown in-app via Help → Manual. Keep the tone consistent with existing sections. The manual is also deployed to GitHub Pages automatically.
- **Release notes** (`docs/releases.md`) — add a summary line under the `# Unreleased` heading.
