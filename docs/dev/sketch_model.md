# Sketch Model

A `Survey` contains two `Sketch` objects: one for the plan view and one for the extended elevation. Each sketch is a container of drawn elements called **sketch details**.

## Class Hierarchy

```
SketchDetail (abstract)
├── PathDetail          — a drawn line (list of Coord2D points), typed by LineType
├── AreaDetail          — a filled region (e.g. water) with optional holes, typed by AreaType
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
- `LineType activeLineType` — line type applied to new paths (default: GENERAL)
- `List<SketchDetail> sketchHistory` / `undoneHistory` — undo/redo stacks

The undo/redo stacks are **not persisted** — only the current state is saved to disk.

## Drawing a Path

Path drawing is a multi-step operation driven by touch events in `GraphView`:

1. `sketch.startNewPath(Coord2D)` — creates `activePath`, optionally snapping to a nearby existing path endpoint
2. `activePath.lineTo(Coord2D)` — called repeatedly on touch move
3. `sketch.finishPath()` — moves `activePath` into `pathDetails` and applies point simplification (`Space2DUtils.simplify()`)

### Line Types

`LineType` types a path as GENERAL (a plain freehand line, the historical behaviour) or as a semantic cave feature: WALL, PRESUMED_WALL, PIT, CHIMNEY or SLOPE. The active line type is picked from a toolbar that opens when the draw tool is tapped while already selected (mirroring the symbol toolbar), and is persisted in `SketchPreferences`. Eraser fragments keep the type of the path they came from. There is no retype operation yet — changing a line's type means redrawing it.

Each type describes its own appearance: a stroke width factor plus an optional path effect. Ornamented types (pit/slope/chimney ticks) use a small stamp path repeated along the line via `PathDashPathEffect` — the TopoDroid approach — so drawing them is a plain `drawPath` call with a per-type `Paint`. Where a type needs both dashes and ticks, the gaps are built into the stamp rather than composing effects.

Lines are oriented by point order (Therion's convention: the side matters for walls and ticked types). Wall-kind lines are auto-oriented when the stroke is finished: `LineOrienter` infers the passage-interior side from the survey centreline (per-segment nearest-station votes, weighted by length) and reverses the stored point order if needed, so the data is canonical whichever way the user drew it. Ticked types can't be inferred — only the user knows which side the drop is — so they keep the drawn direction, with live tick rendering as feedback. Tools → Flip Last Line reverses the most recently drawn semantic line (`Sketch.flipPathDetail`, a delete-and-replace so it's one undo step). The most recently drawn wall-kind line gets a slim red arrowhead at its midpoint pointing to the side taken to be the passage interior, so a wrong auto-orientation guess is visible immediately; it is feedback only — never exported, not shown on a stroke in progress, and toggled by the "Wall Inside Marker" quick setting. Ticked types get no marker since their ornamentation already shows their orientation.

Two rendering caveats: path effects only work on a hardware-accelerated canvas from API 28, so older devices fall back to a plain stroke (the width factor still applies); and effects are applied to the view-space path, so ornamentation keeps a constant on-screen size across zoom levels.

On export, semantic lines become first-class Therion `line wall` / `line pit` etc. commands in the th2 — Therion draws the ornamentation itself. This can be turned off with the "Export typed lines" Therion export setting. Unlike areas, semantic lines also stay in the XVI tracing background (all paths do), so the tracing is always a complete record of the sketch whether or not th2 line export is on; the duplication is harmless since the XVI is only a background reference. SVG gets a class attribute naming the type plus the width factor and (for dashed types) a dash array; tick ornamentation is not reproduced in SVG yet.

## Drawing an Area

An area (currently only water) is sketched like a path — `sketch.startNewArea(Coord2D, AreaType)` creates the `activePath`, `lineTo` extends it — but `sketch.finishArea(AreaType)` closes the outline into a polygon and adds an `AreaDetail` instead of a path. Degenerate outlines (fewer than 3 points after simplification) are discarded.

A single stroke can cross itself, most often when the user overshoots the point where the outline closes. `PolygonUtils.normalise` resolves such a stroke using the *winding* fill rule and keeps only outermost contours, so the crossing is absorbed into the boundary rather than punching a hole: interior loops from one drawing action are always discarded. This is a deliberate choice — a caver is far more likely to cross a stroke by accident than to draw a hole that way, and stray holes would be an annoying artefact. Deliberate holes come from erasing or merging instead (see below). A self-crossing stroke can also enclose two disjoint lobes, in which case each becomes its own area. If the platform op fails or returns nothing, `finishArea` falls back to using the stroke as drawn rather than silently discarding the user's work.

If the new polygon overlaps existing areas of the same type *and* colour, they are all merged into one via `PolygonUtils.union` as a single undoable operation. When the "Blue Water" toggle is on, new water areas record `Colour.BLUE` (mirroring the water symbol behaviour).

### Holes

An `AreaDetail` has an outline contour plus zero or more *holes* — unfilled islands within it. Holes arise from two routes: erasing into the middle of a pool punches one, and merging areas that together enclose a gap (four "walls" of water forming a ring, say) turns that gap into one. Both are handled by the same mechanism, so neither needs special-casing. Note that drawing a self-crossing outline in one stroke is deliberately *not* a third route — see above.

Holes are not nested — a hole never contains a further filled region. That can't arise from drawing, merging or erasing without deliberate effort, and if it somehow does, the even-odd fill rule renders the innermost contour filled, which is the sensible picture anyway.

Because the courtyard of a ring isn't part of the area, `PolygonUtils.overlap` tests against the whole contour set rather than just the outline: an area drawn inside a courtyard stays separate instead of merging and filling it in.

`PolygonUtils` (control/util) implements region union/subtraction on top of `android.graphics.Path` boolean ops, recovering contours by sampling the result with `PathMeasure` and then sorting them into regions (a contour inside another is a hole). Inputs and outputs both use the even-odd fill rule.

Note that under Robolectric, `Path`'s shadow implements neither real boolean geometry nor multi-contour `PathMeasure` iteration, so union/subtract/overlap can only be exercised on a device or emulator. The contour-grouping step is pure geometry and is unit tested directly (`PolygonUtilsTest`).

### Export

Areas are exported to SVG as `<path>` elements with `fill-rule="evenodd"` — one closed subpath per contour, so holes come out unfilled — filled with a per-colour horizontal-line `<pattern>` (see SvgExporter). Therion th2 gets one closed `line border:invisible` per contour plus an `area water` command referencing them all; Therion treats the borders after the first as holes, so this maps directly (see Th2Exporter). The XVI tracing background deliberately omits areas — the th2 carries them as first-class editable objects, so baking them into the background would just duplicate them. The PocketTopo exporter does not emit areas.

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

When erasing a path fragment (rather than the whole path), `replacements` contains the surviving path segments. Erasing part of an area similarly subtracts a disc from the region; the survivors become the replacements — possibly one shrunken region, possibly one with a new hole if the user erased into the middle, possibly two or more if the notch split it, possibly none if the disc swallowed it. The deleted detail(s) are wrapped in a `DeletedDetail` and pushed onto `sketchHistory`. `deleteDetails` deletes several details in one undoable step (used when merging areas).

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
| `"paths"` | array of `{colour, line-type, points:[{x,y}...]}` (`line-type` omitted for GENERAL, as in pre-line-type files) |
| `"areas"` | array of `{area-type, colour, points:[{x,y}...], holes:[[{x,y}...]...]}` (absent in pre-area files; `holes` omitted when the area has none) |
| `"symbols"` | array of `{location, symbol-id, colour, size, angle}` |
| `"labels"` | array of `{location, text, colour, size}` |
| `"x-sections"` | array of `{station-id, location, angle}` |

Path simplification is re-applied on load. History stacks are not serialized.

## Rendering Overview

`GraphView.drawSketch()` iterates each detail collection:
- **Areas:** drawn first (underneath lines): each contour is outlined, and horizontal parallel-line hatching is clipped to the region (an even-odd `Path`, so holes are outlined but left unhatched) and anchored to survey space so it doesn't crawl when panning
- **Paths:** GENERAL paths are sorted by colour (to minimize paint changes), then batched into `float[]` arrays for `canvas.drawLines()`; semantic lines are drawn individually via `canvas.drawPath()` with their type's paint so dashes and tick stamps can apply
- **Symbols:** rendered as scaled, optionally rotated `Drawable` objects with a colour filter
- **Text:** font size = `textSize * surveyToViewScale`; supports `\n` for multiline
- **Cross-sections:** `CrossSectionDetail.getProjection()` computes the legs; drawn with dashed connector line to the actual station

Off-screen and sub-pixel details are culled via `couldBeVisible()` before rendering.

## Key Files

| File | Purpose |
|------|---------|
| `model/sketch/Sketch.java` | Main container |
| `model/sketch/PathDetail.java` | Path/line element |
| `model/sketch/LineType.java` | Line type enum (wall, pit etc.) with per-type appearance |
| `model/sketch/AreaDetail.java` | Filled region element (outline plus holes) |
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
