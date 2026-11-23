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
- `LineType activeLineType` — line type applied to new paths (default: SKETCH)
- `List<SketchDetail> sketchHistory` / `undoneHistory` — undo/redo stacks

The undo/redo stacks are **not persisted** — only the current state is saved to disk.

## Drawing a Path

Path drawing is a multi-step operation driven by touch events in `SketchView`:

1. `sketch.startNewPath(Coord2D)` — creates `activePath`, optionally snapping to a nearby existing path endpoint
2. `activePath.lineTo(Coord2D)` — called repeatedly on touch move
3. `sketch.finishPath(minimumSize)` — moves `activePath` into `pathDetails` and applies point simplification (`Space2DUtils.simplify()`). A stroke whose bounding box is smaller than `minimumSize` in both directions is dropped as an accidental tap. `SketchView` passes a fixed on-screen size (10dp, shared with the minimum area size) converted at the current zoom for typed lines, and zero for freehand SKETCH lines, where a tap deliberately leaves a dot

### Line Types

`LineType` types a path as SKETCH (a plain freehand line, the historical behaviour) or as a semantic cave feature, each named after the Therion line it exports as: WALL, PRESUMED_WALL and UNDERLYING_WALL (wall-kind); PIT and FLOOR_STEP (ticked, solid); CHIMNEY and CEILING_STEP (ticked, dashed); OVERHANG (toothed); SLOPE (hachured: alternating long and short ticks); ROCK_BORDER and ROCK_EDGE (plain, thinner than a wall). Therion's default symbol set draws pit and floor-step identically, and likewise chimney and ceiling-step; here the steps get shorter ticks so they can be told apart while sketching. `LineType.isOriented` is true for wall-kind, stamped and outline types — the ones where flipping changes anything — and gates the context menu's Flip and Tools → Flip Last Line. The active line type is picked from a toolbar that opens when the line tool is tapped while already selected (mirroring the symbol toolbar), and is persisted in `SketchPreferences`. Eraser fragments keep the type of the path they came from. Long-pressing a line with the move, select, line, area or symbol tool opens a line context menu (`LineContextMenu`) to flip it, change its type or delete it; with a drawing tool the stroke just begun is abandoned (the in-progress path is ignored when hit-testing, so it can't be the line found). Symbol and text wait for the finger to lift before placing anything, so a long-press can open a menu instead; erase acts on touch-down, so doesn't offer it. Retyping (`Sketch.retypePathDetail`) is a delete-and-replace, so one undo step. Retyping keeps the point order, even into a wall, so a line only ever turns round when drawn or deliberately flipped. A sketch line gaining ornamentation takes its ornament size from the current zoom, since a sketch line's stored size is never shown or saved.

Each type is defined entirely by its constructor arguments — stroke width factor, optional dash pattern, optional tick stamp, wall-ness — so adding a type needs no logic changes and there are no switch statements over the enum. Ornamented types (pit/chimney ticks) use a small stamp path repeated along the line via `PathDashPathEffect` — the TopoDroid approach — so drawing them is a plain `drawPath` call with a per-type `Paint`. Where a type needs both dashes and ticks, the gaps are built into the stamp rather than composing effects. A stamp is a baseline bar plus either one or more ticks spaced evenly along it (one for pit and the rest, a long and a short for slope) or a triangular tooth (overhang); while a stamp effect is in use the paint is FILL_AND_STROKE so teeth come out solid, reverting to STROKE when the effect is dropped (too small on screen), since filling a plain path would fill the area it encloses.

When the "Clip to Walls" quick setting is on (`SketchPreferences.Toggle.CLIP_TO_WALLS`), `Sketch.finishPath` clips lines whose type `isClippedToWalls` — every semantic type except the wall kinds — the same way areas are clipped: `PolygonUtils.clipLineToInteriorOf` cuts the line where it crosses a wall (walls extended a little past their ends), and keeps each piece that lies mostly on the interior side of its nearest wall. If nothing would be cut off, or everything would be, the line is kept as drawn. The surviving pieces are added in a single undo step. Walls aren't clipped because they're what does the clipping, and an underlying wall crosses the walls above by definition. Freehand lines aren't clipped because they can be anything, including notes outside the passage.

SLOPE's hachures hang on the line's left in Therion's frame, as Therion draws them by default, which the export y-flip makes the right on screen. Therion requires a slope to carry an `l-size` (the hachure extent) on at least one point, so the th2 exporter writes one after the first point: the long hachure length (`LineType.getTherionLineSize`) at the export scale. Therion interpolates it along the line.

Stamp and dash measurements are multiples of an **ornament size**. On screen it is a fixed dp size (the "Line ornament size" preference) times `Viewport.getPatternScale`, which zooms the ornamentation with the cave — so it stays put over the rock instead of crawling along the line — but re-picks its density each time the zoom doubles or halves, keeping it within a factor of √2 of its designed size. Zoomed far out it is also capped at `MAX_PATTERN_METRES_PER_DP` of cave per dp, so a tick can't dwarf the line it's on; past the cap the pattern just shrinks with the cave. Because the jump is a factor of two, the ticks already shown stay where they are and new ones appear between them (or every other one drops out). Area fills are sized the same way. Nothing is stored on the line, so every line of a type looks alike at a given zoom. Exports use the fixed `LineType.EXPORT_ORNAMENT_SIZE` (in metres) instead: the SVG exporter's dash array, and the slope `l-size` Therion requires. Therion draws all other line ornamentation itself.

Lines are oriented by point order (Therion's convention: the side matters for walls and ticked types). With the "Auto-Orientation" quick setting on (`SketchPreferences.Toggle.AUTO_ORIENTATION`, default on), wall-kind lines are auto-oriented when the stroke is finished: `LineOrienter` infers the passage-interior side from the survey centreline (per-segment votes for the side the nearest leg is on, weighted by length; legs rather than stations, since the nearest station may be far off down a long leg while a station of a parallel passage is just beyond the wall, and splays are left out as they run to the walls and possibly beyond) and reverses the stored point order if needed, so the data is canonical whichever way the user drew it. Ticked types can't in general be inferred — only the user knows which side the drop is — so they keep the drawn direction, with live tick rendering as feedback. The exception is a line drawn round something (`LineType.isOrientedByLoop`): a hole edge (`LineType.isHoleEdge`: PIT and CHIMNEY) round its hole, or a boulder outline (`LineType.isOutline`: ROCK_BORDER). With Auto-Orientation on, `LoopOrienter.orientInsideOfLoop` reverses it if needed so its inside is on the side given by `LineType.hasLoopInsideOnRight`: for a hole edge, the side its ticks fall on, so they point inwards; for a boulder outline, the right on screen, which after the export y-flip puts the rock on the left as the Therion wiki asks of rock-border. The line encloses something if its ends meet (within the Snap to Lines distance), or if both ends meet the same wall, in which case the stretch of wall between them closes the loop; the loop's winding (`PolygonUtils.signedArea`) gives which side its inside is on. Steps, slopes and overhangs are left alone, since a loop of those could as well be a raised block as a hollow. Before that, an outline whose ends come within the same distance is closed, by adding its first point again at the end, so Therion fills it (it fills closed rock-borders with the background colour). This happens whatever the Auto-Orientation setting, as it changes the shape rather than the direction. A boulder outline has no ornamentation, so nothing on screen shows its direction; it only shows in the export. Tools → Flip Last Line reverses the most recently drawn oriented line (`Sketch.flipPathDetail`, a delete-and-replace so it's one undo step). Every wall-kind line has its rock side (the side away from the passage interior) shaded on screen, so a wrong auto-orientation guess can be spotted anywhere in the sketch. The shading (`WallRockSide`) is a faint gradient, stronger against the wall, with fine stipple fading out into the rock; like ornamentation it's stamped along the line with `PathDashPathEffect`, sized by the ornament size. It's feedback only — never exported and not shown on a stroke in progress. Ticked types get no shading since their ornamentation already shows their orientation.

Two rendering caveats: path effects only work on a hardware-accelerated canvas from API 28, so on older devices `SketchView` draws in software instead (slower, but the ornamentation shows); and zoomed far enough out the ornamentation would render as a smear, so below a minimum on-screen advance the effect is dropped and the line drawn as a plain stroke. `LineRenderer` caches one `Paint` per type (and one per layer of wall rock-side shading) alongside the on-screen ornament size its effect was built for, rebuilding only when that changes (i.e. on zoom) rather than every frame.

On export, semantic lines become first-class Therion `line wall` / `line pit` etc. commands in the th2 — Therion draws the ornamentation itself. This can be turned off with the "Export typed lines" Therion export setting. Unlike areas, semantic lines also stay in the XVI tracing background (all paths do), so the tracing is always a complete record of the sketch whether or not th2 line export is on; the duplication is harmless since the XVI is only a background reference. SVG gets a class attribute naming the type plus the width factor and (for dashed types) a dash array; tick ornamentation is not reproduced in SVG yet.

## Drawing an Area

An area is sketched like a path — `sketch.startNewArea(Coord2D, AreaType)` creates the `activePath`, `lineTo` extends it — but `sketch.finishArea(AreaType)` closes the outline into a polygon and adds an `AreaDetail` instead of a path. Degenerate outlines (fewer than 3 points after simplification) are discarded, as is anything enclosing less than a minimum area: a square 10dp across on screen, converted at the current zoom (`TouchTolerances.getMinimumArea`), so a stray tap or a tiny lobe doesn't leave a speck behind. Erasing applies the same minimum to the pieces that survive, dropping slivers.

To be sure of closing an outline, people tend to carry on past where they started. Closed as drawn, that overshoot (and any lead-in before the crossing) would enclose a little flap outside the shape, left as a jagged spike or notch where the ends meet. So before anything else, `PolygonUtils.trimOvershoot` looks for a crossing between the stroke's opening and closing stretches (the first and last 15% of its length) and cuts the stroke there, joining the ends at the crossing. The window is kept well short of a quarter because a figure of eight started at one end crosses itself a quarter of the way along, and that must not cost it a lobe. A stroke that falls short is closed with a straight line, after `PolygonUtils.trimHooks` has cut off any hooks: ends that came close together and then curled away from each other as the finger lifted, which would leave a kink either side of the closing line. The stroke is cut where its ends came closest. To avoid misreading a narrow channel (whose two sides are about as close as its ends) and lopping off its tip, hooks are only looked for within twice the final gap of each end, and only trimmed if the ends came within half that gap.

A single stroke can also cross itself elsewhere. `PolygonUtils.normalise` resolves such a stroke using the *winding* fill rule and keeps only outermost contours, so the crossing is absorbed into the boundary rather than punching a hole: interior loops from one drawing action are always discarded. This is a deliberate choice — a caver is far more likely to cross a stroke by accident than to draw a hole that way, and stray holes would be an annoying artefact. Deliberate holes come from erasing or merging instead (see below). A self-crossing stroke can also enclose two disjoint lobes, in which case each becomes its own area. If the platform op fails or returns nothing, `finishArea` falls back to using the stroke as drawn rather than silently discarding the user's work.

If the new polygon overlaps existing areas of the same type *and* colour, they are all merged into one via `PolygonUtils.union` as a single undoable operation. So a patch of sand drawn across a pool stays a separate area on top of it. Long-pressing an area — on its edge, or anywhere in its filled part when no other detail's edge is nearer — with the move, select, line, area or symbol tool opens `AreaContextMenu`, to change its type or colour or delete it. Retyping (`Sketch.retypeAreaDetail`) is a delete-and-replace that goes through the same merging, so a retyped area joins any of its new type and colour that it overlaps, in one undo step; it takes the new type's auto colour if that mode is on (Blue Water, Brown Mud), and otherwise keeps its colour. When the "Blue Water" toggle is on, new water and sump areas record `Colour.BLUE` (mirroring the water symbol behaviour).

### Area types and fills

`AreaType` lists the kinds of area (water, sump, sand, clay, pebbles, debris, blocks), each named after the Therion area it exports as. The user picks one from the area type toolbar, opened by tapping the area tool while it is already selected (as with line types and symbols); the choice is remembered in `SketchPreferences`.

Each type carries a `Fill`: a square tile of marks (polylines, closed polygons, filled dots, outlined rings) measured in dp. This one definition drives everything: `AreaRenderer` renders each tile into an `ALPHA_8` bitmap once and fills areas through a repeating `BitmapShader` (an alpha mask, so it takes the area's colour from the paint), rendered at twice its on-screen size and scaled by `getPatternScale` so the pattern is anchored to the cave as lines' ornamentation is, and `SvgExporter` writes the same marks into an SVG `<pattern>`, converting dp to metres at `Fill.METRES_PER_DP`. Marks stay inside their tile, apart from lines meant to join up with the next tile (water's hatching), so the repeats butt together seamlessly. The toolbar icons (`drawable/area_*.xml`) show the same patterns, drawn larger inside a common pool outline.

### Holes

An `AreaDetail` has an outline contour plus zero or more *holes* — unfilled islands within it. Holes arise from two routes: erasing into the middle of a pool punches one, and merging areas that together enclose a gap (four "walls" of water forming a ring, say) turns that gap into one. Both are handled by the same mechanism, so neither needs special-casing. Note that drawing a self-crossing outline in one stroke is deliberately *not* a third route — see above.

Holes are not nested — a hole never contains a further filled region. That can't arise from drawing, merging or erasing without deliberate effort, and if it somehow does, the even-odd fill rule renders the innermost contour filled, which is the sensible picture anyway.

Because the courtyard of a ring isn't part of the area, `PolygonUtils.overlap` tests against the whole contour set rather than just the outline: an area drawn inside a courtyard stays separate instead of merging and filling it in.

`PolygonUtils` (control/util) implements region union/subtraction on top of `android.graphics.Path` boolean ops, recovering contours by reading the result's vertices back with `Path.approximate` (so untouched stretches of boundary survive any number of operations unchanged; only the eraser's curved cut is approximated) and then sorting them into regions (a contour inside another is a hole). Inputs and outputs both use the even-odd fill rule.

Note that under Robolectric, `Path`'s shadow implements neither real boolean geometry nor multi-contour `PathMeasure` iteration, so union/subtract/overlap can only be exercised on a device or emulator. Below Android 8 (no `Path.approximate`) the contours are sampled with `PathMeasure` instead, which cuts corners a little each time. The contour-splitting and grouping steps are pure geometry and are unit tested directly (`PolygonUtilsTest`).

### Export

Areas are exported to SVG as `<path>` elements with `fill-rule="evenodd"` — one closed subpath per contour, so holes come out unfilled — filled with a `<pattern>` for that combination of area type and colour (see SvgExporter). Therion th2 gets one closed `line border:invisible` per contour plus an `area <type>` command (`area water`, `area sand`, ...) referencing them all; Therion treats the borders after the first as holes, so this maps directly (see Th2Exporter). The XVI tracing background deliberately omits areas — the th2 carries them as first-class editable objects, so baking them into the background would just duplicate them. The PocketTopo exporter does not emit areas.

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

Between `startGroupingDeletions` and `endGroupingDeletions`, each deletion is merged into the previous one (`DeletedDetail.followedBy`) rather than pushed as a new entry, so an erasing drag is a single undo step. Anything the group added as a replacement and then deleted again (a fragment erased twice) drops out of the merged entry, as it wasn't there before the group began. `EraseTool.eraseAlong` steps along the gap between touch events so a quick drag doesn't skip over a line; unlike a tap it ignores cross-section bodies, and area interiors unless deleting fragments.

## Undo / Redo

`SketchDetail` entries in `sketchHistory` are either real details (additions) or `DeletedDetail` wrappers (deletions).

- **Undo an addition:** remove the detail from its collection
- **Undo a deletion:** restore the original detail; remove any replacement fragments
- **Redo** reverses the above

## Symbols

`Symbol` is an enum of 26 cave symbols (stalactite, entrance, water flow, etc.). Each has:
- `isDirectional()` — directional symbols (entrance, gradient, etc.) take an angle; non-directional ones are placed immediately
- `getAutoColour()` — `AutoColour.WATER` for water flow, `AutoColour.MUD` for sand and clay, otherwise `NONE`; when that mode ("Auto-Blue Water" or "Auto-Brown Mud" in the drawing pop-up menu) is on, `Sketch.addSymbolDetail` records its colour on the new SymbolDetail instead of the active colour. Area types carry an AutoColour too, applied when an area is drawn or retyped
- `therionName` — used by the Therion exporter
- `svgFilename` — SVG asset used by the SVG exporter
- `createDrawable()` — creates a mutable `Drawable` for rendering

Long-pressing a symbol opens `SymbolContextMenu`, to move or resize it, swap it for another symbol, change its colour or delete it; text's menu offers Move and Resize too. Both switch to a temporary tool extending `DragDetailTool` (`MoveDetailTool`, `ResizeDetailTool`), which highlights the detail, previews it as the next drag would leave it, and makes the change when the drag ends (`Sketch.moveDetail` or `Sketch.resizeDetail`, one undo step) before going back to the tool in use before. Dragging up makes it bigger and down smaller.

## CrossSectionDetail

A `CrossSectionDetail` marks a position on the plan sketch where a cross-section view appears. It holds a reference to a `Station` and an angle. The actual cross-section geometry is **computed dynamically** at render time from the survey legs at that station — it is not stored in the sketch.

## Colour

`Colour` is an enum of 160+ named colours, each with an `intValue` (ARGB, full alpha) and `baseValue` (RGB only). `SketchDetail.getDrawColour(isDarkModeActive)` auto-converts BLACK to WHITE in dark mode at render time — the stored colour is never changed.

The UI exposes only 8 colours via `BrushColour` (BLACK, BROWN, GREY, RED, ORANGE, GREEN, BLUE, PURPLE), each mapping to the corresponding `Colour` entry.

## Coordinate System

Sketch coordinates are in **survey space** (metres). `SketchView` converts to screen pixels through its `Viewport`, which holds the pan offset and the scale in pixels per metre (default 60.0f, range 0.1–500.0f). Hot drawing loops copy the offset and scale into locals and apply the transform inline rather than calling `Viewport.toView` per point. Avoid storing pixel coordinates in sketch data.

## Serialization

`SketchJsonTranslater` serializes to/from JSON. Top-level keys:

| Key | Contents |
|-----|----------|
| `"paths"` | array of `{colour, line-type, points:[{x,y}...]}` (`line-type` omitted for SKETCH, as in pre-line-type files) |
| `"areas"` | array of `{area-type, colour, points:[{x,y}...], holes:[[{x,y}...]...]}` (absent in pre-area files; `holes` omitted when the area has none) |
| `"symbols"` | array of `{location, symbol-id, colour, size, angle}` |
| `"labels"` | array of `{location, text, colour, size}` |
| `"x-sections"` | array of `{station-id, location, angle}` |

Path simplification is re-applied on load. History stacks are not serialized.

## Rendering Overview

`SketchRenderer.draw()` draws a sketch's details in this order, through `AreaRenderer`, `LineRenderer` and `PointRenderer`:
- **Areas:** drawn first (underneath lines): each region is filled with its type's pattern through a repeating bitmap shader and its contours outlined (an even-odd `Path`, so holes are outlined but left unfilled); the shader is anchored to survey space so the pattern doesn't crawl when panning. Only water and sump (`AreaType.hasDefinedEdge`) are outlined at full line weight, since a shoreline is a real edge; other areas get a light outline, at the weight of their fill marks and faded to `AreaType.LIGHT_OUTLINE_OPACITY`, so they don't compete with walls (likewise in SVG export)
- **Paths:** SKETCH paths are batched into `float[]` arrays for `canvas.drawLines()`, the paint colour only being changed when a path's colour differs from the one before; semantic lines are drawn individually via `canvas.drawPath()` with their type's paint so dashes and tick stamps can apply
- **Text:** font size = `textSize * viewport.getScale()`; supports `\n` for multiline
- **Symbols:** rendered as scaled, optionally rotated `Drawable` objects with a colour filter

Off-screen and sub-pixel details are culled via `Viewport.couldBeVisible()` before rendering. Cross-sections are drawn by `CrossSectionLayer` (below), not `SketchRenderer`: `CrossSectionDetail.getProjection()` computes the legs, drawn with a dashed connector line to the actual station.

## The Sketch View

`SketchView` (an Android `View`; `CrossSectionView` extends it for the cross-section editor) is mostly wiring. It owns the parts below, passes the activity's setters on to them, routes touches, and lists the drawing steps in `onDraw`.

Two objects are shared by every part:

- **`Viewport`**: the pan offset and the scale (pixels per metre). It converts between survey and view coordinates, turns an on-screen size in dp into metres at the current zoom (`dpToSurvey`), gives the pattern scale for ornamentation and fills, and holds the visible bounds used for culling. It's plain Java and unit-tested.
- **`SketchState`**: the survey, sketch and projection being shown, the current tool and those it interrupted (a one-use tool interrupts an ordinary one, a pinch or pan either, and each goes back to what it interrupted when done), and the brush colour. The view's setters write here, and the parts read from here rather than asking the view.

**Drawing.** `onDraw` draws, in order:

1. the grid (`OverlayRenderer`)
2. linked surveys, faded; `ConnectedSurveys` keeps them lined up with the survey being shown
3. the survey itself: its sketch (`SketchRenderer`), its cross-sections (`CrossSectionLayer`, main survey only), then its legs and stations (`SurveyDataRenderer`, which calls back for each station so the cross-section layer can draw its facing indicator)
4. the legend and compass (`OverlayRenderer`), the hot corners (`HotCorners`) and, in debug mode, debugging info

Each of these owns its paints and sets them up in an `initialise` method, which `SketchView.initialisePaint` calls. `SketchPaints` holds only what they share (the solid and faded alphas, applying the anti-aliasing preference, looking up theme colours), and `ScreenUnits` converts dp and sp to pixels. The busiest loops (lines, areas, the grid) copy the viewport's offset and scale into locals and do the transform inline, rather than calling `Viewport.toView` for every point.

**Touch.** `onTouchEvent` first deals with what applies whatever the tool:

- a pinch (the scale detector), which also ends an erasing drag
- the end of a modal tool
- a two-finger or hot-corner pan, which switches to `MODAL_MOVE`
- a press on a cross-section's handle or body

It then passes the event to the current tool's `ToolHandler`, looked up in a map from `SketchTool` to handler (`createSketchToolMap`). Each tool class (`MoveTool`, `LineTool`, `AreaTool`, `EraseTool`, `SymbolTool`, `TextTool`, `SelectTool`) keeps its own gesture state and settings: the line type lives on `LineTool`, the area type on `AreaTool`, the symbol on `SymbolTool`. `TouchTolerances` gives touch radii and minimum detail sizes, fixed on screen and converted at the current zoom, and `SketchSelector` finds the station, sketch detail or linked survey under the finger. A tool can also draw feedback on what it's in the middle of doing over the survey (`ToolHandler.drawFeedback`, which does nothing by default), such as the guide line while rotating a cross-section or the highlighted symbol or text being moved or resized; `SketchView.drawToolFeedback` asks the tool in use, or while a pinch or pan interrupts it, the tool it interrupted (`SketchState.getUnderlyingSketchTool`). A tool drawing a detail as feedback can also have it left out of the sketch's normal drawing (`ToolHandler.getHiddenDetail`). Tools reach the view only through `ToolHost` (redraw, context, toast, announcing a survey change, opening a station menu), which a private inner class implements so the view's public API stays small. A long press opens the station menu, or a menu for the detail under the finger: `LineContextMenu`, `AreaContextMenu`, `SymbolContextMenu` (move or resize the symbol, swap it, keeping its place, size and angle, or delete it) or `TextContextMenu` (move, resize, edit or delete). All four also offer a Colour submenu of the brush colours (`ColourSubMenu`, backed by the `Sketch.recolour…` methods); a colour chosen this way holds even where an auto colour (blue water, brown mud) would otherwise apply. Each change is one undo step. Which tools allow these menus is a `SketchTool` trait. Placing text and editing it share `TextEntryDialog`.

**Cross-sections** are one vertical slice, `CrossSectionLayer`, because drawing and touch share state there. Drawing records where each handle is, for touch to hit-test, and the move and rotate gestures keep the in-progress drag or angle, for drawing to preview. The layer owns its paints, its drawing, hit-testing, the position, move and rotate tools, and opening the editor. It draws each section's legs and centre mark through `SurveyDataRenderer`, and its sub-sketch through `SketchRenderer`. Its host adds `getViewContext` to `ToolHost`.

`CrossSectionView` changes the view's behaviour by overriding `getViewContext`, `isLegInPlane`, `isCompassShown`, `checkForChangedSurvey` and `onDraw`.

## Key Files

| File | Purpose |
|------|---------|
| `model/sketch/Sketch.java` | Main container |
| `model/sketch/PathDetail.java` | Path/line element |
| `model/sketch/LineType.java` | Line type enum (wall, pit etc.) with per-type appearance |
| `model/sketch/AreaDetail.java` | Filled region element (outline plus holes) |
| `model/sketch/AreaType.java` | Area type enum, with each type's Therion name and fill pattern |
| `control/util/PolygonUtils.java` | Polygon union/subtraction (Path ops) |
| `model/sketch/SymbolDetail.java` | Symbol element |
| `model/sketch/TextDetail.java` | Text label element |
| `model/sketch/CrossSectionDetail.java` | Cross-section element |
| `model/sketch/DeletedDetail.java` | Undo/redo wrapper |
| `model/sketch/Symbol.java` | Symbol enum |
| `model/sketch/Colour.java` | Colour enum |
| `model/sketch/BrushColour.java` | UI colour picker enum |
| `control/sketch/SketchView.java` | Rendering and touch handling |
| `control/sketch/Viewport.java` | Pan and zoom; survey↔screen coordinates, dp to metres at the current zoom, pattern scale; culling |
| `control/sketch/SketchState.java` | The survey, sketch and projection being shown, and the current tool and those it interrupted; shared by the view and its parts |
| `control/sketch/SketchPaints.java` | What paints share: solid/faded alphas, anti-alias preference, theme colours |
| `control/sketch/ScreenUnits.java` | dp and sp to pixels |
| `control/sketch/SketchRenderer.java` | Draws a sketch: areas, then lines, then text and symbols |
| `control/sketch/LineRenderer.java` | Plain and typed lines, ornament and rock-side shading paints and caches |
| `control/sketch/AreaRenderer.java` | Area fills and outlines, fill pattern shaders |
| `control/sketch/PointRenderer.java` | Text and symbols, the details placed at a point |
| `control/sketch/SurveyDataRenderer.java` | Legs, splays and stations, with their paints, labels, icons and the active station's highlight |
| `control/sketch/OverlayRenderer.java` | Grid, legend and scale bar, compass, debugging info |
| `control/sketch/HotCorners.java` | Hot corners: hit-testing for the modal pan, and drawing |
| `control/sketch/LineContextMenu.java` | The long-press menu on a line: flip it or change its type |
| `control/sketch/CrossSectionLayer.java` | Cross-sections on the sketch: drawing, hit-testing, and the place/move/rotate/open gestures |
| `control/sketch/*Tool.java` | One class per sketch tool (`ToolHandler`), each owning its gesture state; dispatched from `SketchView.onTouchEvent` by tool |
| `control/sketch/TouchTolerances.java` | Touch radii and minimum detail sizes, fixed on screen and converted at the current zoom |
| `control/sketch/SketchSelector.java` | Finds the station or line under the finger |
| `control/io/basic/SketchJsonTranslater.java` | Serialization |
