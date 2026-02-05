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
- [x] Cross-section placeholder visibility? **Inherits from parent layer** (hidden/faded/showing)
- [x] Cross-section layer membership? **Each layer owns its cross-sections**
- [x] Cross-section popup UI? **New Activity with GraphView**
- [x] Backward compatibility? **Old format loads into single layer**
- [x] Undo across layers? **Layer switches recorded in undo stack**

## Related Files

- `app/src/main/java/org/hwyl/sexytopo/model/sketch/Sketch.java`
- `app/src/main/java/org/hwyl/sexytopo/model/sketch/SketchDetail.java`
- `app/src/main/java/org/hwyl/sexytopo/control/graph/GraphView.java`
- `app/src/main/java/org/hwyl/sexytopo/control/io/basic/SketchJsonTranslater.java`
- `app/src/main/java/org/hwyl/sexytopo/control/io/thirdparty/therion/TherionExporter.java`
- `app/src/main/java/org/hwyl/sexytopo/control/io/thirdparty/xvi/XviExporter.java`
