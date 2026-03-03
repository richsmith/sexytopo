# Sketch Model

A `Survey` contains two `Sketch` objects: one for the plan view and one for the extended elevation. Each sketch is a container of drawn elements called **sketch details**.

## Class Hierarchy

```
SketchDetail (abstract)
├── PathDetail          — a drawn line (list of Coord2D points)
├── SinglePositionDetail (abstract)
│   ├── SymbolDetail    — a cave symbol at a fixed location
│   ├── TextDetail      — a text label
│   └── CrossSectionDetail — reference to a cross-section view
└── DeletedDetail       — undo/redo wrapper (not a real element)
```

All `SketchDetail` subclasses are **immutable** — colour and geometry cannot be changed in place. To "edit" an element you delete it and add a new one.

## The Sketch Container

`Sketch` holds:
- `List<PathDetail> pathDetails`
- `List<SymbolDetail> symbolDetails`
- `List<TextDetail> textDetails`
- `List<CrossSectionDetail> crossSectionDetails`
- `PathDetail activePath` — the path currently being drawn (null when not drawing)
- `Colour activeColour` — colour applied to new elements (default: BLACK)
- `List<SketchDetail> sketchHistory` / `undoneHistory` — undo/redo stacks

The undo/redo stacks are **not persisted** — only the current state is saved to disk.

## Drawing a Path

Path drawing is a multi-step operation driven by touch events in `GraphView`:

1. `sketch.startNewPath(Coord2D)` — creates `activePath`, optionally snapping to a nearby existing path endpoint
2. `activePath.lineTo(Coord2D)` — called repeatedly on touch move
3. `sketch.finishPath()` — moves `activePath` into `pathDetails` and applies point simplification (`Space2DUtils.simplify()`)

## Adding Other Elements

```java
sketch.addTextDetail(Coord2D location, String text, float size)
sketch.addSymbolDetail(Coord2D location, Symbol symbol, float size, float angle)
sketch.addCrossSection(CrossSection crossSection, Coord2D touchPointOnSurvey)
```

Every add clears the redo stack and sets `isSaved = false`.

## Deletion

```java
sketch.deleteDetail(SketchDetail toDelete, List<SketchDetail> replacements)
```

When erasing a path fragment (rather than the whole path), `replacements` contains the surviving path segments. The deleted detail is wrapped in a `DeletedDetail` and pushed onto `sketchHistory`.

## Undo / Redo

`SketchDetail` entries in `sketchHistory` are either real details (additions) or `DeletedDetail` wrappers (deletions).

- **Undo an addition:** remove the detail from its collection
- **Undo a deletion:** restore the original detail; remove any replacement fragments
- **Redo** reverses the above

## Symbols

`Symbol` is an enum of 26 cave symbols (stalactite, entrance, water flow, etc.). Each has:
- `isDirectional()` — directional symbols (entrance, gradient, etc.) take an angle; non-directional ones are placed immediately
- `therionName` — used by the Therion exporter
- `svgFilename` — SVG asset used by the SVG exporter
- `createDrawable()` — creates a mutable `Drawable` for rendering

## CrossSectionDetail

A `CrossSectionDetail` marks a position on the plan sketch where a cross-section view appears. It holds a reference to a `Station` and an angle. The actual cross-section geometry is **computed dynamically** at render time from the survey legs at that station — it is not stored in the sketch.

## Colour

`Colour` is an enum of 160+ named colours, each with an `intValue` (ARGB, full alpha) and `baseValue` (RGB only). `SketchDetail.getDrawColour(isDarkModeActive)` auto-converts BLACK to WHITE in dark mode at render time — the stored colour is never changed.

The UI exposes only 8 colours via `BrushColour` (BLACK, BROWN, GREY, RED, ORANGE, GREEN, BLUE, PURPLE), each mapping to the corresponding `Colour` entry.

## Coordinate System

Sketch coordinates are in **survey space** (metres). `GraphView` converts to screen pixels using `surveyToViewScale` (default 60.0f, range 0.1–500.0f). Avoid storing pixel coordinates in sketch data.

## Serialization

`SketchJsonTranslater` serializes to/from JSON. Top-level keys:

| Key | Contents |
|-----|----------|
| `"paths"` | array of `{colour, points:[{x,y}...]}` |
| `"symbols"` | array of `{location, symbol-id, colour, size, angle}` |
| `"labels"` | array of `{location, text, colour, size}` |
| `"x-sections"` | array of `{station-id, location, angle}` |

Path simplification is re-applied on load. History stacks are not serialized.

## Rendering Overview

`GraphView.drawSketch()` iterates each detail collection:
- **Paths:** sorted by colour (to minimize paint changes), then batched into `float[]` arrays for `canvas.drawLines()`
- **Symbols:** rendered as scaled, optionally rotated `Drawable` objects with a colour filter
- **Text:** font size = `textSize * surveyToViewScale`; supports `\n` for multiline
- **Cross-sections:** `CrossSectionDetail.getProjection()` computes the legs; drawn with dashed connector line to the actual station

Off-screen details are culled via `couldBeOnScreen()` before rendering.

## Key Files

| File | Purpose |
|------|---------|
| `model/sketch/Sketch.java` | Main container |
| `model/sketch/PathDetail.java` | Path/line element |
| `model/sketch/SymbolDetail.java` | Symbol element |
| `model/sketch/TextDetail.java` | Text label element |
| `model/sketch/CrossSectionDetail.java` | Cross-section element |
| `model/sketch/DeletedDetail.java` | Undo/redo wrapper |
| `model/sketch/Symbol.java` | Symbol enum |
| `model/sketch/Colour.java` | Colour enum |
| `model/sketch/BrushColour.java` | UI colour picker enum |
| `control/graph/GraphView.java` | Rendering and touch handling |
| `control/io/basic/SketchJsonTranslater.java` | Serialization |
