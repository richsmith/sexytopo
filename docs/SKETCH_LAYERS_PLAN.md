# Plan: Multi-Layer Sketches in SexyTopo

**Status:** Planning  
**Created:** 2026-02-05  
**Updated:** 2026-02-05  
**Branch:** AllTherionExports  
**Feature Flag:** None during development; add at merge if needed for beta testing

## Problem Statement

Overlapping passages in complex caves create visual confusion in single-layer sketches. Users need the ability to organize sketch content into separate layers that can be individually faded or hidden.

## Design Decisions

| Decision | Resolution | Rationale |
|----------|------------|-----------|
| Cross-sections | Layer-specific | Each layer can have its own cross-sections |
| Cross-section display | Zoomable popup window | Placeholder on sketch; tap opens modal popup with zoom controls |
| Undo/Redo | Global stack | Single undo history across all layers for simpler UX |
| Snapping | Cross-layer (all visible layers) | Current snapping is O(n) paths; adding layers doesn't change complexity. No performance issue. |
| Layer naming | Numeric (user preference in settings) | Simple default; names appended to export filenames |
| Layer preferences | Stored in app settings | User can configure default layer names/count |
| Layer visibility | 3 states: hidden, faded, showing | Active layer always "showing"; others faded or hidden |
| Faded color | Grey (distinct from sketch grey) | Fading to single color has same performance as alpha (just changes paint color). Use a muted grey like `#AAAAAA` |
| Layer controls UI | Hidden by default | Button/icon to reveal layer panel; keeps UI clean |

## Current Architecture

| Component | Current State |
|-----------|---------------|
| **Sketch** | Single flat container with `List<PathDetail>`, `List<SymbolDetail>`, etc. |
| **SketchDetail** | Has `Colour` but no layer association |
| **Survey** | Holds one `Sketch` per projection (plan/elevation) |
| **GraphView** | Already supports fading via `FADED_ALPHA` for connected surveys |
| **Therion Export** | One `xvi` per projection |

## Proposed Data Model

```
Survey
  └── Sketch (plan)
        └── SketchLayer[] layers
              ├── layer[0]: "Main Passage" (default)
              ├── layer[1]: "Upper Level"
              └── layer[2]: "Lower Level"
                    └── List<PathDetail>
                    └── List<SymbolDetail>
                    └── List<TextDetail>
                    └── List<CrossSectionDetail>
```

### New class: `SketchLayer`

```java
public class SketchLayer {
    private int id;                    // stable reference
    private String name;               // user-friendly name
    private boolean visible;           // show/hide toggle
    private float alpha;               // 1.0 = full, 0.2 = faded, 0.0 = hidden
    
    private List<PathDetail> pathDetails;
    private List<SymbolDetail> symbolDetails;
    private List<TextDetail> textDetails;
    private List<CrossSectionDetail> crossSectionDetails;
}
```

### Changes to `Sketch`

- Replace flat lists with `List<SketchLayer>`
- Add `activeLayerId` to track which layer receives new drawings
- Add methods: `addLayer()`, `removeLayer()`, `setActiveLayer()`, `getVisibleDetails()`

### Changes to `SketchDetail`

- Add `int layerId` field (or association through parent layer)

## UI Considerations

### Layer Panel (hidden by default)
- Accessed via button/icon in toolbar
- Shows list of layers with:
  - **Active indicator** - which layer receives new drawings
  - **Visibility toggle** - 3-state cycle: showing → faded → hidden
  - **Reorder** - drag to change z-order
  - **Rename/Delete** - context menu or long-press

### Visibility States
| State | Appearance |
|-------|------------|
| **Showing** | Full color (active layer always here) |
| **Faded** | Grey `#AAAAAA` - distinct from sketch grey |
| **Hidden** | Not rendered |

### Active Layer Rule
- Active layer is always "showing"
- Switching active layer automatically sets new layer to "showing"
- Other layers remain in their current visibility state (faded/hidden)

## Therion Export Changes

**Current:** One `survey.plan.xvi` and `survey.ee.xvi`

**Proposed:** One xvi per layer (numeric naming):
```
survey.plan.0.xvi
survey.plan.1.xvi
survey.plan.2.xvi
survey.ee.0.xvi
survey.ee.1.xvi
```

**th2 file changes:**
```therion
input "survey.plan.0.xvi"
input "survey.plan.1.xvi"
input "survey.plan.2.xvi"
```

Each xvi contains only that layer's sketch data, allowing Therion users to manage layers separately.

Layer numbering comes from user preferences in settings.

## Migration & Compatibility

1. **File format version** - bump sketch JSON version
2. **Auto-migration** - existing sketches load into a single "Default" layer
3. **Backward compatibility** - old SexyTopo versions would fail to load new format (unavoidable with structural changes)

## Cross-Sections (Separate from Layers)

**Cross-sections are NOT layers** - they are station-attached sub-sketches with their own coordinate space.

### Key Differences

| Aspect | Layers | Cross-Sections |
|--------|--------|----------------|
| **Purpose** | Organize overlapping passages in same view | Vertical slice at a station |
| **Coordinate space** | Same as main sketch | Own local space (station-centered) |
| **Relationship** | Part of plan/elevation sketch | Attached to specific station |
| **Count** | User-defined (1+) | One per station (optional) |

### Display Behavior
- **Main sketch**: Only a placeholder icon at cross-section location
- **Tap placeholder**: Opens zoomable popup window with full cross-section
- **Performance**: Excellent - main sketch only draws small icons, not full splays

```
Main Sketch:                    Popup Window:
                               ┌─────────────────────┐
    ● placeholder  ──tap──►    │   Cross-section     │
                               │     ╲   ╱           │
                               │      ╲ ╱            │
                               │   ────●────         │
                               │      ╱ ╲            │
                               │     ╱   ╲           │
                               │   [zoom controls]   │
                               └─────────────────────┘
```

### Cross-Section Export (Therion)

Each cross-section exports as a **separate XVI scrap** named by station:
```
scrap1.xvi          (cross-section at station 1)
scrap2.xvi          (cross-section at station 2)
scrapS3.xvi         (cross-section at station S3)
```

**th2 file includes all scraps:**
```therion
input "survey.plan.0.xvi"
input "survey.plan.1.xvi"
input "scrap1.xvi"
input "scrap2.xvi"
```

### Current Implementation

Cross-sections already exist as `CrossSectionDetail` containing a `CrossSection`:
- `CrossSection` holds station reference and angle
- `CrossSectionDetail` holds position on main sketch and projects splays
- Currently rendered inline on main sketch (performance concern for complex surveys)

### Proposed Changes for Cross-Sections

1. **Rendering**: Replace inline splay rendering with placeholder icon
2. **Interaction**: Tap placeholder → open popup window
3. **Popup**: Zoomable view showing full cross-section with splays
4. **Export**: Each cross-section becomes separate XVI file (scrap{station}.xvi)
5. **No layer membership**: Cross-sections exist independently of sketch layers

## Additional Considerations

| Topic | Notes |
|-------|-------|
| **Performance** | Cross-section boxes hidden by default helps; layers with many paths may need visibility culling |
| **Layer limits** | Consider max layer count (e.g., 10) to prevent complexity |
| **Color per layer** | Optional: default color for new items in each layer |
| **Import** | XVI import would need layer selection target |

## Implementation Order

### Phase 1: Core Sketching (Current Focus)
- [x] 1. **Model changes** - `SketchLayer`, `LayerSwitchDetail`, update `Sketch`
- [x] 2. **JSON serialization** - update `SketchJsonTranslater` (with legacy format support)
- [x] 3. **Rendering** - update `GraphView` for layer visibility/fading
- [x] 4. **Layer UI** - layer panel with visibility toggles
- [x] 5. **Cross-section placeholder** - X icon with connection line
- [x] 6. **Cross-section popup** - `CrossSectionActivity` with full sketching support (draw/erase/undo/redo, auto-opens on new cross-section)

### Phase 2: Export/Import (Deferred)
- [ ] 7. **Export** - multiple XVI files, updated th2 input statements
- [ ] 8. **Import** - load multi-layer sketches

## Implementation Decisions

### 1. Cross-section layer membership
**Recommendation:** Each `SketchLayer` owns its own `List<CrossSectionDetail>`

This is cleanest because:
- Layer visibility naturally applies to its cross-sections
- No need for `layerId` lookups
- Matches how paths/symbols/text are handled

```java
public class SketchLayer {
    private List<PathDetail> pathDetails;
    private List<SymbolDetail> symbolDetails;
    private List<TextDetail> textDetails;
    private List<CrossSectionDetail> crossSectionDetails;  // owned by layer
}
```

### 2. Cross-section popup editor
**Recommendation:** New `CrossSectionActivity` with embedded `GraphView`

- Full-screen Activity for maximum editing space
- Reuse `GraphView` rendering logic configured for cross-section mode
- Pass station ID via Intent, return edited data via result
- Simpler than Dialog/Fragment for zoom/pan gestures

### 3. Backward compatibility (old SexyTopo opening new format)
**Recommendation:** Old versions will fail to parse new JSON structure

For **forward compatibility** (new SexyTopo opening old format):
- Detect missing `layers` array in JSON
- Load all content into a single "Default" layer (layer 0)
- Cross-sections included in that layer
- Seamless migration, no data loss

### 4. Undo stack with layer switching
**Recommendation:** Layer switches are recorded in undo stack

No performance issue - just stores an int (layer ID). Example flow:
```
Action              Undo Stack
------              ----------
Draw on layer 0     [PathDetail]
Switch to layer 1   [PathDetail, LayerSwitch(0→1)]
Draw on layer 1     [PathDetail, LayerSwitch(0→1), PathDetail]
Undo                → removes PathDetail from layer 1
Undo                → switches back to layer 0
Undo                → removes PathDetail from layer 0
```

New class needed:
```java
public class LayerSwitchDetail extends SketchDetail {
    private final int fromLayerId;
    private final int toLayerId;
}
```

## Open Questions

- [x] Maximum number of layers? **No limit** - trust the user
- [x] Default number of layers for new survey? **1** - add more as needed
- [x] How to handle layer selection when importing XVI files? **First layer in file order**
- [x] Should cross-section popup allow drawing/editing, or view-only initially? **Allow editing**
- [x] Cross-section placeholder icon design? **Simple X with connection line to station** (same line as current)
- [x] Cross-section placeholder visibility? **Hidden with parent layer** (only visible when layer is SHOWING). *Future consideration: may want to keep cross-section placeholders visible even when layer is faded/hidden, to allow navigation to cross-section sketches from any layer state.*
- [x] Cross-section layer membership? **Each layer owns its cross-sections**
- [x] Cross-section popup UI? **New Activity with GraphView**
- [x] Backward compatibility? **Old format loads into single layer**
- [x] Undo across layers? **Layer switches recorded in undo stack**

## Performance Optimisations

Sketching becomes slow with multiple layers, particularly when faded layers are visible. The following optimisations are listed in priority order.

### Benchmarking (Future)

To measure the impact of optimisations, consider adding:
- **Debug timing in GraphView.onDraw()** - Log frame times when debug mode is enabled, average over N frames
- **Automated benchmark test** - JUnit test that creates a Sketch with known content (e.g., 100 paths with 50 points each) and measures draw time in a loop
- Compare results before/after changes using git stash/checkout

| Priority | Fix | Effort | Impact |
|----------|-----|--------|--------|
| 1 | Bitmap cache for faded layers | Medium | High |
| 2 | Reusable float array for path drawing | Low | Medium |
| 3 | Canvas rotation instead of RotateDrawable | Low | Medium |
| 4 | Spatial index for snap points | High | Medium |
| 5 | Primitive arrays instead of List\<Coord2D\> | Very High | Low (Not Recommended) |

### 1. Bitmap Cache for Faded Layers

**Location:** `GraphView.java#L1263-L1386`

**Problem:** Faded layers are drawn exactly like visible layers - every path, every point, every symbol. The only difference is the color.

**Fix options:**
- **A) Bitmap caching for faded layers** - Render faded layers to an off-screen bitmap once, then just `drawBitmap()` on each frame. Invalidate cache when layer content changes or view transforms.
- **B) Simplified rendering for faded layers** - Skip symbols, reduce path point density, or only draw every Nth path.
- **C) Draw faded layers less frequently** - Only redraw faded layers when panning stops, not during active drawing.

**Implications:** Option A provides best quality but uses memory. Option B may look visibly worse. Option C causes visual lag but is simple to implement.

### 2. Reusable Float Array for Path Drawing

**Location:** `GraphView.java#L1291-L1323`

**Problem:** Every path drawn allocates a new `float[]` array:
```java
float[] lines = new float[path.size() * 4];
```
This happens for every path, on every frame, triggering garbage collection.

**Fix options:**
- **A) Reusable float array pool** - Pre-allocate and reuse arrays.
- **B) Pre-compute view coordinates** - Cache transformed coordinates when path changes, not on every draw.

**Implications:** Option A is simpler but still iterates all points. Option B requires more refactoring but eliminates per-frame coordinate transforms.

### 3. Canvas Rotation Instead of RotateDrawable

**Location:** `GraphView.java#L1373-L1384`

**Problem:** For each directional symbol, a new `RotateDrawable` is created every frame.

**Fix options:**
- **A) Cache RotateDrawables per symbol** - Create once, reuse.
- **B) Use Canvas rotation** - `canvas.save()`, `canvas.rotate()`, draw, `canvas.restore()`.

**Implications:** Option B is cleaner and faster, just needs careful pivot point handling.

### 4. Spatial Index for Snap Points

**Location:** `Sketch.java#L299-L324` and `GraphView.java#L492-L496`

**Problem:** `findEligibleSnapPointWithin()` iterates all paths in all visible layers on ACTION_DOWN and ACTION_UP. With many paths, this is slow.

**Fix options:**
- **A) Spatial index (quadtree)** - Index path endpoints for O(log n) lookup.
- **B) Only snap to active layer** - Reduces iteration scope.
- **C) Cache snap points** - Build list of endpoints once, update incrementally.

**Implications:** Option A is most scalable but adds complexity. Option B changes behavior. Option C is a good middle ground.

### 5. Primitive Arrays Instead of List\<Coord2D\> (Not Recommended)

**Locations:** Multiple, including `GraphView.java#L1297-L1300`

**Problem:** While coordinate transforms already avoid intermediate objects (per comments at lines 424-428), `Coord2D` objects are still created in loops.

**Fix:** Replace `List<Coord2D>` data model with primitive `float[]` arrays throughout the codebase.

**Implications:** Significant refactoring for marginal gains. Would affect serialization, undo/redo, and all path manipulation code. **Not recommended.**

## Related Files

- `app/src/main/java/org/hwyl/sexytopo/model/sketch/Sketch.java`
- `app/src/main/java/org/hwyl/sexytopo/model/sketch/SketchDetail.java`
- `app/src/main/java/org/hwyl/sexytopo/control/graph/GraphView.java`
- `app/src/main/java/org/hwyl/sexytopo/control/io/basic/SketchJsonTranslater.java`
- `app/src/main/java/org/hwyl/sexytopo/control/io/thirdparty/therion/TherionExporter.java`
- `app/src/main/java/org/hwyl/sexytopo/control/io/thirdparty/xvi/XviExporter.java`
