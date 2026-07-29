# Sketch Model

A `Survey` contains two `Sketch` objects: one for the plan view and one for the extended elevation. Each sketch is a container of drawn elements called **sketch details**.

## Class Hierarchy

```
SketchDetail (abstract)
├── PathDetail          — a drawn line (list of Coord2D points)
├── AreaDetail          — a filled polygon region (e.g. water), typed by AreaType
├── SinglePositionDetail (abstract)
│   ├── SymbolDetail    — a cave symbol at a fixed location
│   ├── TextDetail      — a text label
│   └── CrossSectionDetail — reference to a cross-section view
└── DeletedDetail       — undo/redo wrapper (not a real element)
```

All `SketchDetail` subclasses are **immutable** — colour and geometry cannot be changed in place. To "edit" an element you delete it and add a new one.

The one exception is a `CrossSectionDetail`'s sub-sketch: committing an edit from the cross-section editor replaces it in place (`CrossSectionDetail.setSketch`). This is deliberate — it keeps the detail's identity stable so references held by the plan's undo/redo stacks don't go stale. Geometry changes to a cross-section (move, rotate) still follow the immutable delete-and-add pattern and remain undoable.

## The Sketch Container

`Sketch` holds:
- `List<PathDetail> pathDetails`
- `List<AreaDetail> areaDetails`
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

## Drawing an Area

An area (currently only water) is sketched like a path — `sketch.startNewArea(Coord2D, AreaType)` creates the `activePath`, `lineTo` extends it — but `sketch.finishArea(AreaType)` closes the outline into a polygon and adds an `AreaDetail` instead of a path. Degenerate outlines (fewer than 3 points after simplification) are discarded.

If the new polygon overlaps existing areas of the same type *and* colour, they are all merged into one via `PolygonUtils.union` as a single undoable operation. When the "Blue Water" toggle is on, new water areas record `Colour.BLUE` (mirroring the water symbol behaviour).

`PolygonUtils` (control/util) implements polygon union/subtraction on top of `android.graphics.Path` boolean ops, recovering polygons by sampling the result contours with `PathMeasure`. The polygon model can't represent holes, so hole contours are dropped (filled in). Note this means `PolygonUtils` is a no-op under plain JUnit (stubbed framework); merge behaviour degrades to a plain add there.

Areas are exported to SVG as `<polygon>` elements filled with a per-colour horizontal-line `<pattern>` (see SvgExporter), and to Therion th2 as a closed `line border:invisible` plus an `area water` command referencing it (see Th2Exporter). The XVI tracing background deliberately omits areas — the th2 carries them as first-class editable objects, so baking them into the background would just duplicate them. The PocketTopo exporter does not emit areas.

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

When erasing a path fragment (rather than the whole path), `replacements` contains the surviving path segments. Erasing part of an area similarly subtracts a disc from the polygon; the survivors (possibly two or more polygons if the notch split it) become the replacements. The deleted detail(s) are wrapped in a `DeletedDetail` and pushed onto `sketchHistory`. `deleteDetails` deletes several details in one undoable step (used when merging areas).

## Undo / Redo

`SketchDetail` entries in `sketchHistory` are either real details (additions) or `DeletedDetail` wrappers (deletions).

- **Undo an addition:** remove the detail from its collection
- **Undo a deletion:** restore the original detail; remove any replacement fragments
- **Redo** reverses the above

## Symbols

`Symbol` is an enum of 26 cave symbols (stalactite, entrance, water flow, etc.). Each has:
- `isDirectional()` — directional symbols (entrance, gradient, etc.) take an angle; non-directional ones are placed immediately
- `isWater()` — true for water flow; when the "Blue Water" toggle in the drawing pop-up menu is on, `Sketch.addSymbolDetail` records `Colour.BLUE` on the new SymbolDetail instead of the active colour
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
| `"areas"` | array of `{area-type, colour, points:[{x,y}...]}` (absent in pre-area files) |
| `"symbols"` | array of `{location, symbol-id, colour, size, angle}` |
| `"labels"` | array of `{location, text, colour, size}` |
| `"x-sections"` | array of `{station-id, location, angle}` |

Path simplification is re-applied on load. History stacks are not serialized.

## Rendering Overview

`GraphView.drawSketch()` iterates each detail collection:
- **Areas:** drawn first (underneath lines): polygon outline plus horizontal parallel-line hatching, clipped to the polygon and anchored to survey space so it doesn't crawl when panning
- **Paths:** sorted by colour (to minimize paint changes), then batched into `float[]` arrays for `canvas.drawLines()`
- **Symbols:** rendered as scaled, optionally rotated `Drawable` objects with a colour filter
- **Text:** font size = `textSize * surveyToViewScale`; supports `\n` for multiline
- **Cross-sections:** `CrossSectionDetail.getProjection()` computes the legs; drawn with dashed connector line to the actual station

Off-screen and sub-pixel details are culled via `couldBeVisible()` before rendering.

## Key Files

| File | Purpose |
|------|---------|
| `model/sketch/Sketch.java` | Main container |
| `model/sketch/PathDetail.java` | Path/line element |
| `model/sketch/AreaDetail.java` | Filled polygon region element |
| `model/sketch/AreaType.java` | Area type enum (currently just WATER) |
| `control/util/PolygonUtils.java` | Polygon union/subtraction (Path ops) |
| `model/sketch/SymbolDetail.java` | Symbol element |
| `model/sketch/TextDetail.java` | Text label element |
| `model/sketch/CrossSectionDetail.java` | Cross-section element |
| `model/sketch/DeletedDetail.java` | Undo/redo wrapper |
| `model/sketch/Symbol.java` | Symbol enum |
| `model/sketch/Colour.java` | Colour enum |
| `model/sketch/BrushColour.java` | UI colour picker enum |
| `control/graph/GraphView.java` | Rendering and touch handling |
| `control/io/basic/SketchJsonTranslater.java` | Serialization |
