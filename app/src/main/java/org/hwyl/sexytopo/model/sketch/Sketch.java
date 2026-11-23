package org.hwyl.sexytopo.model.sketch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hwyl.sexytopo.control.util.PolygonUtils;
import org.hwyl.sexytopo.control.util.SketchPreferences;
import org.hwyl.sexytopo.control.util.Space2DUtils;
import org.hwyl.sexytopo.model.common.Shape;
import org.hwyl.sexytopo.model.geometry.Coord2D;
import org.hwyl.sexytopo.model.survey.Station;

public class Sketch extends Shape {

    public static final float DEFAULT_XSECTION_SCALE = 1.0f;

    // How near a wall must come to an area to clip it: a fraction of the area's size, with a floor
    private static final float WALL_CLIP_MARGIN_FACTOR = 0.25f;
    private static final float MINIMUM_WALL_CLIP_MARGIN = 0.5f;

    // How far past its ends a wall cuts, in metres, so one stopping just short still cuts across
    private static final float WALL_CLIP_EXTENSION = 0.25f;

    private List<PathDetail> pathDetails = new ArrayList<>();
    private List<AreaDetail> areaDetails = new ArrayList<>();
    private List<SymbolDetail> symbolDetails = new ArrayList<>();
    private List<TextDetail> textDetails = new ArrayList<>();
    private List<CrossSectionDetail> crossSectionDetails = new ArrayList<>();

    private final List<SketchDetail> sketchHistory = new ArrayList<>();
    private final List<SketchDetail> undoneHistory = new ArrayList<>();
    // while deletions are being grouped (e.g. a drag of the eraser), the one they merge into
    private boolean isGroupingDeletions = false;
    private DeletedDetail groupedDeletion;

    private PathDetail activePath;
    // the lines the active path's start and end were snapped to, to be joined into it on finishing
    private PathDetail startJoin;
    private PathDetail endJoin;

    private float crossSectionScale = DEFAULT_XSECTION_SCALE;

    private boolean isSaved = true;
    private boolean isAutosaved = true;

    public Sketch() {}

    public Sketch(Sketch sketch) {
        // shallow copies are OK here because paths are immutable
        this.crossSectionScale = sketch.crossSectionScale;
        setPathDetails(new ArrayList<>(sketch.getPathDetails()));
        setAreaDetails(new ArrayList<>(sketch.getAreaDetails()));
        setSymbolDetails(new ArrayList<>(sketch.getSymbolDetails()));
        setTextDetails(new ArrayList<>(sketch.getTextDetails()));
        setCrossSectionDetails(new ArrayList<>(sketch.getCrossSectionDetails()));
    }

    public boolean isSaved() {
        return isSaved;
    }

    public void setSaved(boolean isSaved) {
        this.isSaved = isSaved;

        if (!isSaved) {
            // saving doesn't mean it's autosaved, but unsaved data means autosave required
            setAutosaved(false);
        }
    }

    public boolean isAutosaved() {
        return isAutosaved;
    }

    public void setAutosaved(boolean isAutosaved) {
        this.isAutosaved = isAutosaved;
    }

    public void setPathDetails(List<PathDetail> pathDetails) {
        this.pathDetails = pathDetails;
        recalculateBoundingBox();
    }

    public void setAreaDetails(List<AreaDetail> areaDetails) {
        this.areaDetails = areaDetails;
        recalculateBoundingBox();
    }

    public List<AreaDetail> getAreaDetails() {
        return areaDetails;
    }

    public void setSymbolDetails(List<SymbolDetail> symbolDetails) {
        this.symbolDetails = symbolDetails;
        recalculateBoundingBox();
    }

    public void setTextDetails(List<TextDetail> textDetails) {
        this.textDetails = textDetails;
        recalculateBoundingBox();
    }

    public List<PathDetail> getPathDetails() {
        return pathDetails;
    }

    public PathDetail getActivePath() {
        return activePath;
    }

    public PathDetail startNewPath(Coord2D start) {
        return startNewPath(start, Colour.BLACK, LineType.SKETCH);
    }

    public PathDetail startNewPath(Coord2D start, Colour colour, LineType lineType) {
        activePath = new PathDetail(start, colour, lineType);
        startJoin = null;
        endJoin = null;
        // Registered in the undo history only on finishPath, so an unfinished path can be
        // abandoned without leaving a trace.
        pathDetails.add(activePath);
        return activePath;
    }

    private void addSketchDetail(SketchDetail sketchDetail) {
        setSaved(false);
        sketchHistory.add(sketchDetail);
        undoneHistory.clear();
        updateBoundingBox(sketchDetail);
    }

    // Drops the in-progress path without committing it to the sketch or undo history.
    public void abandonActivePath() {
        if (activePath == null) {
            return;
        }
        pathDetails.remove(activePath);
        activePath = null;
        recalculateBoundingBox();
    }

    public void finishPath() {
        finishPath(0);
    }

    /**
     * Commits the active path, unless it spans less than minimumSize (in metres) in both
     * directions, in which case it's taken to be an accidental tap and dropped.
     *
     * <p>If Clip to Walls is on, a line of a type that belongs inside the passage is cut along any
     * walls it strays across and the parts on their outer side are dropped, as for areas. The
     * pieces left are added as one undoable step; any too small to keep are dropped too.
     */
    public void finishPath(float minimumSize) {
        if (activePath == null) {
            return;
        }
        PathDetail startJoin = this.startJoin;
        PathDetail endJoin = this.endJoin;
        this.startJoin = null;
        this.endJoin = null;

        if (activePath.getMaxDimension() < minimumSize) {
            abandonActivePath();
            return;
        }
        float epsilon = Space2DUtils.simplificationEpsilon(activePath);
        activePath.setPath(Space2DUtils.simplify(activePath.getPath(), epsilon));

        List<List<Coord2D>> pieces = clipToWalls(activePath);
        if (pieces.size() == 1
                && pieces.get(0) == activePath.getPath()
                && startJoin == null
                && endJoin == null) {
            addSketchDetail(activePath);
            activePath = null;
            return;
        }

        PathDetail drawn = activePath;
        abandonActivePath();
        List<PathDetail> joined = new ArrayList<>();
        List<SketchDetail> kept =
                pieces.stream()
                        .map(piece -> new PathDetail(piece, drawn.getColour(), drawn.getLineType()))
                        .filter(piece -> piece.getMaxDimension() >= minimumSize)
                        .map(piece -> piece.joinedWith(startJoin, endJoin, joined))
                        .collect(Collectors.toList());
        if (!kept.isEmpty()) {
            deleteDetails(new ArrayList<>(joined), kept);
        }
    }

    /**
     * The line cut along any walls it strays across, keeping the parts on their inner side; may be
     * several pieces. Returns the line's own points, as the only piece, if it isn't to be clipped.
     */
    private List<List<Coord2D>> clipToWalls(PathDetail line) {

        if (!SketchPreferences.Toggle.CLIP_TO_WALLS.isOn()
                || !line.getLineType().isClippedToWalls()) {
            return Collections.singletonList(line.getPath());
        }

        // Nearness by bounding box alone is enough here: each piece is judged against whichever
        // wall is nearest it, so an outlying one has no say
        List<List<Coord2D>> walls =
                findWallsNear(line, getWallClipMargin(line), wall -> true).stream()
                        .map(PathDetail::getPath)
                        .collect(Collectors.toList());
        if (walls.isEmpty()) {
            return Collections.singletonList(line.getPath());
        }

        return PolygonUtils.clipLineToInteriorOf(line.getPath(), walls, WALL_CLIP_EXTENSION);
    }

    /**
     * Start sketching the outline of a new area. The outline is drawn like a normal path (so it
     * renders as the user drags) and is turned into a polygon by finishArea.
     */
    public PathDetail startNewArea(Coord2D start, Colour colour, AreaType areaType) {
        activePath = new PathDetail(start, areaType.getAutoColour().applyTo(colour));
        pathDetails.add(activePath);
        return activePath;
    }

    /**
     * Close the outline being sketched into a polygon and add it as an area. If it overlaps any
     * existing areas of the same type and colour, they are all merged into one (as a single
     * undoable operation).
     *
     * <p>If the stroke carries on past where it started, the overshoot is trimmed off so the ends
     * join cleanly; if it stops short with its ends curling away, the hooks are trimmed off. Any
     * other crossing is resolved to the stroke's outer edge, so it doesn't leave a stray hole
     * behind. Such a stroke can also enclose two separate lobes, in which case each becomes its own
     * area.
     *
     * <p>If Clip Areas to Walls is on, the area is cut along any walls it strays across and the
     * parts on their outer side are dropped (see clipToWalls).
     *
     * <p>Anything enclosing less than minimumArea (in square metres) is discarded, so a stray tap
     * or a tiny lobe doesn't leave a speck of an area behind.
     */
    public void finishArea(AreaType areaType, double minimumArea) {
        if (activePath == null) {
            return;
        }
        PathDetail outlinePath = activePath;
        abandonActivePath();

        float epsilon = Space2DUtils.simplificationEpsilon(outlinePath);
        List<Coord2D> stroke =
                PolygonUtils.trimHooks(PolygonUtils.trimOvershoot(outlinePath.getPath()));
        List<Coord2D> outline = Space2DUtils.simplify(stroke, epsilon);
        if (outline.size() < 3) {
            return; // too small to form a polygon; treat as an accidental tap
        }

        List<PolygonUtils.Region> regions = PolygonUtils.normalise(outline);
        if (regions == null || regions.isEmpty()) {
            // the geometry op failed or gave us nothing back; rather than silently lose the
            // user's stroke, fall back to using it as drawn
            regions = Collections.singletonList(new PolygonUtils.Region(outline));
        }

        addAreaDetails(
                regions.stream()
                        .map(
                                region ->
                                        new AreaDetail(
                                                region.getOutline(),
                                                areaType,
                                                outlinePath.getColour()))
                        .flatMap(area -> clipToWalls(area).stream())
                        .filter(area -> area.toRegion().getArea() >= minimumArea)
                        .collect(Collectors.toList()));
    }

    /**
     * The area cut along any walls it strays across, keeping the parts on their inner side; may be
     * several pieces. Relies on walls having been oriented to the passage interior when drawn.
     * Falls back to the area as drawn if clipping fails.
     */
    private List<AreaDetail> clipToWalls(AreaDetail area) {

        if (!SketchPreferences.Toggle.CLIP_TO_WALLS.isOn()) {
            return Collections.singletonList(area);
        }

        List<List<Coord2D>> walls =
                findClippingWalls(area).stream()
                        .map(PathDetail::getPath)
                        .collect(Collectors.toList());
        if (walls.isEmpty()) {
            return Collections.singletonList(area);
        }

        List<PolygonUtils.Region> clipped =
                PolygonUtils.clipToInteriorOf(area.toRegion(), walls, WALL_CLIP_EXTENSION);
        if (clipped == null || clipped.isEmpty()) {
            return Collections.singletonList(area);
        }

        return clipped.stream().map(area::withRegion).collect(Collectors.toList());
    }

    /**
     * Wall-kind lines near enough the area to cut it or to say which side of them it's on. Pit
     * edges and chimneys don't count: they aren't passage boundaries and have no inferred interior
     * side.
     */
    private List<PathDetail> findClippingWalls(AreaDetail area) {
        float margin = getWallClipMargin(area);
        return findWallsNear(area, margin, wall -> comesWithin(wall, area, margin));
    }

    /** How near a wall must come to a detail to clip it. */
    private static float getWallClipMargin(Shape shape) {
        return Math.max(
                MINIMUM_WALL_CLIP_MARGIN,
                WALL_CLIP_MARGIN_FACTOR * Math.max(shape.getWidth(), shape.getHeight()));
    }

    /**
     * Wall-kind lines within the margin of the shape's bounding box that pass the closer check
     * given, which says whether a wall really comes near enough the shape itself.
     */
    private List<PathDetail> findWallsNear(
            Shape shape, float margin, Predicate<PathDetail> comesNear) {
        Coord2D grow = new Coord2D(margin, margin);
        Coord2D topLeft = shape.getTopLeft().minus(grow);
        Coord2D bottomRight = shape.getBottomRight().plus(grow);

        return pathDetails.stream()
                .filter(path -> path.getLineType().isWall())
                .filter(path -> path.getPath().size() >= 2)
                .filter(path -> path.intersectsRectangle(topLeft, bottomRight))
                .filter(comesNear)
                .collect(Collectors.toList());
    }

    /**
     * Whether the line comes within the given distance of the area. Checks both ways round, so
     * neither a long straight stretch of wall nor a long straight edge of area slips through.
     */
    private static boolean comesWithin(PathDetail path, AreaDetail area, float margin) {
        return path.getPath().stream()
                        .anyMatch(
                                point ->
                                        area.getDistanceFrom(point) <= margin
                                                || area.contains(point))
                || area.getOutline().stream()
                        .anyMatch(point -> path.getDistanceFrom(point) <= margin);
    }

    public void addAreaDetail(AreaDetail newArea) {
        addAreaDetails(Collections.singletonList(newArea));
    }

    /**
     * Add areas from a single action as one undoable step, merging any that overlap existing areas
     * of the same type and colour, or each other.
     */
    public void addAreaDetails(List<AreaDetail> newAreas) {
        replaceAreaDetails(Collections.emptyList(), newAreas);
    }

    /** Changes an area's type as a single undoable operation. */
    public void retypeAreaDetail(AreaDetail areaDetail, AreaType areaType) {
        Colour colour = areaType.getAutoColour().applyTo(areaDetail.getColour());
        AreaDetail retyped =
                new AreaDetail(areaDetail.getOutline(), areaDetail.getHoles(), areaType, colour);
        replaceAreaDetails(
                Collections.singletonList(areaDetail), Collections.singletonList(retyped));
    }

    /** Changes an area's colour as a single undoable operation. */
    public void recolourAreaDetail(AreaDetail areaDetail, Colour colour) {
        replaceAreaDetails(
                Collections.singletonList(areaDetail),
                Collections.singletonList(areaDetail.withColour(colour)));
    }

    /**
     * Removes some details and adds new areas in their place as one undoable step, merging the new
     * areas with any existing ones of the same type and colour they overlap, and with each other.
     */
    private void replaceAreaDetails(List<SketchDetail> replaced, List<AreaDetail> newAreas) {

        if (newAreas.isEmpty() && replaced.isEmpty()) {
            return;
        }

        // anything being replaced is gone, so it's left out of the merging
        List<SketchDetail> mergedAway = new ArrayList<>(replaced);
        List<AreaDetail> added = new ArrayList<>();

        for (AreaDetail newArea : newAreas) {
            // check against what's been added so far too, so two new areas overlapping the same
            // existing one all end up merged together
            List<AreaDetail> mergeWith =
                    Stream.concat(
                                    areaDetails.stream()
                                            .filter(existing -> !mergedAway.contains(existing)),
                                    added.stream())
                            .filter(other -> overlaps(other, newArea))
                            .collect(Collectors.toList());

            List<PolygonUtils.Region> union = null;
            if (!mergeWith.isEmpty()) {
                List<PolygonUtils.Region> regions = new ArrayList<>();
                regions.add(newArea.toRegion());
                mergeWith.stream().map(AreaDetail::toRegion).forEach(regions::add);
                union = PolygonUtils.union(regions);
            }

            if (union == null || union.isEmpty()) {
                // nothing to merge with (or the merge failed): just add the new area as-is
                added.add(newArea);
                continue;
            }

            for (AreaDetail other : mergeWith) {
                if (!added.remove(other)) {
                    mergedAway.add(other);
                }
            }
            union.stream().map(newArea::withRegion).forEach(added::add);
        }

        if (mergedAway.isEmpty() && added.size() == 1) {
            AreaDetail only = added.get(0);
            areaDetails.add(only);
            addSketchDetail(only);
        } else {
            deleteDetails(mergedAway, new ArrayList<>(added));
        }
    }

    private static boolean overlaps(AreaDetail existing, AreaDetail newArea) {
        return existing.getAreaType() == newArea.getAreaType()
                && existing.getColour() == newArea.getColour()
                && existing.intersectsRectangle(newArea.getTopLeft(), newArea.getBottomRight())
                && PolygonUtils.overlap(existing.toRegion(), newArea.toRegion());
    }

    public void addTextDetail(Coord2D location, String text, Colour colour, float size) {
        TextDetail textDetail = new TextDetail(location, text, colour, size);
        textDetails.add(textDetail);
        addSketchDetail(textDetail);
    }

    public List<SymbolDetail> getSymbolDetails() {
        return symbolDetails;
    }

    public void addSymbolDetail(
            Coord2D location, Symbol symbol, Colour colour, float size, float angle) {
        SymbolDetail symbolDetail =
                new SymbolDetail(
                        location, symbol, symbol.getAutoColour().applyTo(colour), size, angle);
        symbolDetails.add(symbolDetail);
        addSketchDetail(symbolDetail);
    }

    public List<TextDetail> getTextDetails() {
        return textDetails;
    }

    /**
     * The most recently added path whose direction matters, or null if there isn't one. Used by the
     * "Flip Last Line" tool, since flipping any other line would do nothing visible.
     */
    public PathDetail getMostRecentOrientedPath() {
        for (int i = pathDetails.size() - 1; i >= 0; i--) {
            PathDetail pathDetail = pathDetails.get(i);
            if (pathDetail.getLineType().isOriented()) {
                return pathDetail;
            }
        }
        return null;
    }

    /**
     * Reverses the point order of a path (flipping which side its ticks fall on) as a single
     * undoable operation. Since details are immutable this is a delete-and-replace.
     */
    public PathDetail flipPathDetail(PathDetail pathDetail) {
        List<Coord2D> reversedPoints = new ArrayList<>(pathDetail.getPath());
        Collections.reverse(reversedPoints);
        PathDetail flipped =
                new PathDetail(reversedPoints, pathDetail.getColour(), pathDetail.getLineType());
        deleteDetail(pathDetail, Collections.singletonList((SketchDetail) flipped));
        return flipped;
    }

    /**
     * Changes a path's line type as a single undoable operation. The point order is kept, so the
     * line faces the same way as before: once drawn, a line only turns round when deliberately
     * flipped.
     */
    public PathDetail retypePathDetail(PathDetail pathDetail, LineType lineType) {
        List<Coord2D> points = new ArrayList<>(pathDetail.getPath());
        PathDetail retyped = new PathDetail(points, pathDetail.getColour(), lineType);
        deleteDetail(pathDetail, Collections.singletonList((SketchDetail) retyped));
        return retyped;
    }

    /**
     * Swaps a symbol for another in the same place, as a single undoable operation. It keeps its
     * size and angle, and its colour unless the new symbol has an auto colour that's on (blue
     * water, brown mud), as it would if drawn as that symbol.
     */
    public SymbolDetail changeSymbol(SymbolDetail symbolDetail, Symbol symbol) {
        SymbolDetail changed =
                new SymbolDetail(
                        symbolDetail.getPosition(),
                        symbol,
                        symbol.getAutoColour().applyTo(symbolDetail.getColour()),
                        symbolDetail.getSize(),
                        symbolDetail.getAngle());
        deleteDetail(symbolDetail, Collections.singletonList((SketchDetail) changed));
        return changed;
    }

    /** Changes what a piece of text says, as a single undoable operation. */
    public TextDetail changeText(TextDetail textDetail, String text) {
        TextDetail changed =
                new TextDetail(
                        textDetail.getPosition(),
                        text,
                        textDetail.getColour(),
                        textDetail.getSize());
        deleteDetail(textDetail, Collections.singletonList((SketchDetail) changed));
        return changed;
    }

    /** Changes a line's colour, as a single undoable operation. */
    public PathDetail recolourPathDetail(PathDetail pathDetail, Colour colour) {
        PathDetail recoloured = pathDetail.withColour(colour);
        deleteDetail(pathDetail, Collections.singletonList((SketchDetail) recoloured));
        return recoloured;
    }

    /**
     * Changes a symbol's colour, as a single undoable operation. The chosen colour holds even for a
     * symbol with an auto colour.
     */
    public SymbolDetail recolourSymbolDetail(SymbolDetail symbolDetail, Colour colour) {
        SymbolDetail recoloured = symbolDetail.withColour(colour);
        deleteDetail(symbolDetail, Collections.singletonList((SketchDetail) recoloured));
        return recoloured;
    }

    /** Moves a symbol or piece of text by the given offset, as a single undoable operation. */
    public SketchDetail moveDetail(SinglePositionDetail detail, Coord2D offset) {
        SketchDetail moved = detail.translate(offset);
        deleteDetail(detail, Collections.singletonList(moved));
        return moved;
    }

    /**
     * Scales a symbol or piece of text by the given factor, keeping its place, as a single undoable
     * operation.
     */
    public SketchDetail resizeDetail(SinglePositionDetail detail, float scale) {
        SketchDetail resized = detail.scale(scale);
        deleteDetail(detail, Collections.singletonList(resized));
        return resized;
    }

    /** Changes the colour of a piece of text, as a single undoable operation. */
    public TextDetail recolourTextDetail(TextDetail textDetail, Colour colour) {
        TextDetail recoloured = textDetail.withColour(colour);
        deleteDetail(textDetail, Collections.singletonList((SketchDetail) recoloured));
        return recoloured;
    }

    public float getCrossSectionScale() {
        return crossSectionScale;
    }

    public void setCrossSectionScale(float crossSectionScale) {
        this.crossSectionScale = crossSectionScale;
        recalculateBoundingBox();
    }

    public boolean canUndo() {
        return !sketchHistory.isEmpty();
    }

    public boolean canRedo() {
        return !undoneHistory.isEmpty();
    }

    public void undo() {
        if (!sketchHistory.isEmpty()) {
            SketchDetail toUndo = sketchHistory.remove(sketchHistory.size() - 1);

            if (toUndo instanceof DeletedDetail) {
                DeletedDetail deletedDetail = (DeletedDetail) toUndo;
                for (SketchDetail sketchDetail : deletedDetail.getDeletedDetails()) {
                    restoreDetailToSketch(sketchDetail);
                }
                for (SketchDetail sketchDetail : deletedDetail.getReplacementDetails()) {
                    removeDetailFromSketch(sketchDetail);
                }
            } else {
                removeDetailFromSketch(toUndo);
            }

            undoneHistory.add(toUndo);
        }
    }

    public void redo() {
        if (!undoneHistory.isEmpty()) {
            SketchDetail toRedo = undoneHistory.remove(undoneHistory.size() - 1);

            if (toRedo instanceof DeletedDetail) {
                DeletedDetail deletedDetail = (DeletedDetail) toRedo;
                for (SketchDetail sketchDetail : deletedDetail.getDeletedDetails()) {
                    removeDetailFromSketch(sketchDetail);
                }
                for (SketchDetail sketchDetail : deletedDetail.getReplacementDetails()) {
                    restoreDetailToSketch(sketchDetail);
                }
            } else {
                restoreDetailToSketch(toRedo);
            }

            sketchHistory.add(toRedo);
        }
    }

    public void deleteDetail(SketchDetail sketchDetail) {
        deleteDetail(sketchDetail, new ArrayList<>());
    }

    public void deleteDetail(SketchDetail sketchDetail, List<SketchDetail> replacementDetails) {
        deleteDetails(Collections.singletonList(sketchDetail), replacementDetails);
    }

    /**
     * Delete several details in one undoable step, optionally replacing them with others (e.g.
     * areas merged into one).
     */
    public void deleteDetails(
            List<SketchDetail> sketchDetails, List<SketchDetail> replacementDetails) {
        DeletedDetail deletedDetail = new DeletedDetail(sketchDetails, replacementDetails);
        if (groupedDeletion != null && isLastInHistory(groupedDeletion)) {
            deletedDetail = groupedDeletion.followedBy(deletedDetail);
            sketchHistory.set(sketchHistory.size() - 1, deletedDetail);
            setSaved(false);
        } else {
            addSketchDetail(deletedDetail);
        }
        if (isGroupingDeletions) {
            groupedDeletion = deletedDetail;
        }
        for (SketchDetail sketchDetail : sketchDetails) {
            removeDetailFromSketch(sketchDetail);
        }
        for (SketchDetail replacementDetail : replacementDetails) {
            restoreDetailToSketch(replacementDetail);
        }
    }

    /**
     * Merge the deletions made from now until endGroupingDeletions into a single undo step, so
     * erasing by dragging across several details is undone in one go.
     */
    public void startGroupingDeletions() {
        isGroupingDeletions = true;
        groupedDeletion = null;
    }

    public void endGroupingDeletions() {
        isGroupingDeletions = false;
        groupedDeletion = null;
    }

    private boolean isLastInHistory(SketchDetail sketchDetail) {
        return !sketchHistory.isEmpty()
                && sketchHistory.get(sketchHistory.size() - 1) == sketchDetail;
    }

    private void removeDetailFromSketch(SketchDetail sketchDetail) {
        // this is a separate function to deleteDetail because former is user-called and handles
        // undo history etc. whereas this actually removes the data
        if (sketchDetail instanceof PathDetail) {
            pathDetails.remove(sketchDetail);
        } else if (sketchDetail instanceof AreaDetail) {
            areaDetails.remove(sketchDetail);
        } else if (sketchDetail instanceof SymbolDetail) {
            symbolDetails.remove(sketchDetail);
        } else if (sketchDetail instanceof TextDetail) {
            textDetails.remove(sketchDetail);
        } else if (sketchDetail instanceof CrossSectionDetail) {
            crossSectionDetails.remove(sketchDetail);
        }

        recalculateBoundingBox();
    }

    public void restoreDetailToSketch(SketchDetail sketchDetail) {
        if (sketchDetail instanceof PathDetail) {
            pathDetails.add((PathDetail) sketchDetail);
        } else if (sketchDetail instanceof AreaDetail) {
            areaDetails.add((AreaDetail) sketchDetail);
        } else if (sketchDetail instanceof SymbolDetail) {
            symbolDetails.add((SymbolDetail) sketchDetail);
        } else if (sketchDetail instanceof TextDetail) {
            textDetails.add((TextDetail) sketchDetail);
        } else if (sketchDetail instanceof CrossSectionDetail) {
            crossSectionDetails.add((CrossSectionDetail) sketchDetail);
        }

        updateBoundingBox(sketchDetail);
    }

    /**
     * Snaps the ends of the active path to the ends of nearby lines of the same type and colour, to
     * be joined into one line with them when it's finished, or closes it into a loop if its end
     * comes back to its own start. Lines only join head to tail: where it matters which way a line
     * runs (see LineType.isOriented), the path's start only snaps to another line's end and its end
     * to another line's start, so the joined line doesn't change direction partway. A path drawn
     * against the way of the lines it would join is reversed to run with them, so it takes their
     * orientation over whatever it was drawn or oriented with.
     *
     * <p>The points drawn near a join are dropped, so a stroke that starts or finishes a little
     * beyond its target, or wobbles about it, doesn't double back, and the end of the stroke is
     * bent smoothly round to meet it. A stroke that never strays beyond delta of its start is too
     * small to close on itself.
     */
    public void snapActivePath(float delta) {
        startJoin = null;
        endJoin = null;
        if (activePath == null || activePath.getPath().size() < 2 || activePath.isClosed()) {
            return;
        }
        Coord2D start = activePath.getStart();
        Coord2D end = activePath.getEnd();

        if (activePath.getLineType().isOriented()) {
            int joinsAsDrawn = countLinesToSnapTo(start, end, delta);
            int joinsReversed = countLinesToSnapTo(end, start, delta);
            if (joinsReversed > joinsAsDrawn) {
                Collections.reverse(activePath.getPath());
                start = activePath.getStart();
                end = activePath.getEnd();
            }
        }

        float distanceToStart = Space2DUtils.getDistance(end, start);
        PathDetail endTarget = findLineToSnapTo(end, delta, false, null);
        if (activePath.reachesBeyond(delta)
                && distanceToStart < delta
                && (endTarget == null
                        || distanceToStart
                                < Space2DUtils.getDistance(
                                        end, endTarget.getEndToJoinBefore(end)))) {
            activePath.snapEndTo(start, delta);
            return;
        }

        PathDetail startTarget = findLineToSnapTo(start, delta, true, null);
        if (startTarget != null) {
            activePath.snapStartTo(startTarget.getEndToJoinAfter(start), delta);
            startJoin = startTarget;
        }

        // the end mustn't snap to the very point the start just did
        endTarget = findLineToSnapTo(end, delta, false, activePath.getStart());
        if (endTarget != null) {
            activePath.snapEndTo(endTarget.getEndToJoinBefore(end), delta);
            endJoin = endTarget;
        }
    }

    /** How many of a path's ends, were it to run from start to end, would snap to another line. */
    private int countLinesToSnapTo(Coord2D start, Coord2D end, float delta) {
        int count = 0;
        if (findLineToSnapTo(start, delta, true, null) != null) {
            count++;
        }
        if (findLineToSnapTo(end, delta, false, null) != null) {
            count++;
        }
        return count;
    }

    /**
     * The finished line the active path could join whose joinable end is nearest the point and
     * within delta of it, or null if there isn't one. atStart says which end of the active path is
     * joining: its start joins on after another line, its end before one.
     */
    private PathDetail findLineToSnapTo(
            Coord2D point, float delta, boolean atStart, Coord2D excluded) {
        Function<PathDetail, Coord2D> joinableEnd =
                line -> atStart ? line.getEndToJoinAfter(point) : line.getEndToJoinBefore(point);
        Function<PathDetail, Float> distance =
                line -> Space2DUtils.getDistance(point, joinableEnd.apply(line));
        return pathDetails.stream()
                .filter(line -> line != activePath && activePath.canJoin(line))
                .filter(line -> !joinableEnd.apply(line).equals(excluded))
                .filter(line -> distance.apply(line) < delta)
                .min(Comparator.comparing(distance))
                .orElse(null);
    }

    private List<SketchDetail> allSketchDetails() {
        List<SketchDetail> all = new ArrayList<>();
        all.addAll(pathDetails);
        all.addAll(areaDetails);
        all.addAll(symbolDetails);
        all.addAll(textDetails);
        all.addAll(crossSectionDetails);
        return all;
    }

    public SketchDetail findNearestDetailWithin(Coord2D point, float delta) {
        return findNearestVisibleDetailWithin(point, delta, Float.MAX_VALUE);
    }

    public SketchDetail findNearestVisibleDetailWithin(
            Coord2D point, float delta, float viewScale) {
        return findNearestVisibleDetailWithin(point, delta, viewScale, detail -> true);
    }

    /** The nearest visible detail within delta of the point that passes the filter, or null. */
    public SketchDetail findNearestVisibleDetailWithin(
            Coord2D point, float delta, float viewScale, Predicate<SketchDetail> filter) {

        SketchDetail closest = null;
        float minDistance = Float.MAX_VALUE;

        for (SketchDetail detail : allSketchDetails()) {
            // a stroke still being drawn isn't part of the sketch yet
            if (detail == activePath
                    || !detail.couldBeVisibleAtScale(viewScale)
                    || !filter.test(detail)) {
                continue;
            }
            float distance = detail.getDistanceFrom(point);
            if (distance < delta && distance < minDistance) {
                closest = detail;
                minDistance = distance;
            }
        }

        return closest;
    }

    /**
     * The area whose filled part covers this point, or null if there isn't one. Used by the eraser,
     * which needs to find an area the user has tapped the middle of — too far from the boundary for
     * findNearestVisibleDetailWithin to pick it up. The last matching area wins, matching the draw
     * order so the topmost one is returned.
     */
    public AreaDetail findAreaContaining(Coord2D point) {
        AreaDetail found = null;
        for (AreaDetail areaDetail : areaDetails) {
            if (areaDetail.contains(point)) {
                found = areaDetail;
            }
        }
        return found;
    }

    public void addCrossSection(CrossSection crossSection, Coord2D touchPointOnSurvey) {
        CrossSectionDetail sectionDetail = new CrossSectionDetail(crossSection, touchPointOnSurvey);
        addCrossSection(sectionDetail);
    }

    public void addCrossSection(CrossSectionDetail sectionDetail) {
        crossSectionDetails.add(sectionDetail);
        addSketchDetail(sectionDetail);
    }

    /**
     * Replace a cross-section detail as an undoable operation on this sketch's undo stack. Use this
     * for plan-level edits like move and rotate.
     */
    public void replaceCrossSectionDetail(
            CrossSectionDetail oldDetail, CrossSectionDetail newDetail) {
        List<SketchDetail> replacements = new ArrayList<>();
        replacements.add(newDetail);
        deleteDetail(oldDetail, replacements);
    }

    public List<CrossSectionDetail> getCrossSectionDetails() {
        return crossSectionDetails;
    }

    public void setCrossSectionDetails(List<CrossSectionDetail> crossSectionDetails) {
        this.crossSectionDetails = crossSectionDetails;
        recalculateBoundingBox();
    }

    public CrossSectionDetail getCrossSectionDetail(Station station) {
        // this is a bit inefficient... not sure if it's worth caching this in a map though since
        // there'll probably be max a couple of dozen x-sections per survey chunk
        for (CrossSectionDetail detail : crossSectionDetails) {
            CrossSection crossSection = detail.getCrossSection();
            if (crossSection.getStation() == station) {
                return detail;
            }
        }
        return null;
    }

    @Override
    public Sketch translate(Coord2D translation) {
        Sketch sketch = new Sketch();

        List<PathDetail> newPathDetails = new ArrayList<>();
        for (PathDetail pathDetail : pathDetails) {
            newPathDetails.add(pathDetail.translate(translation));
        }
        sketch.setPathDetails(newPathDetails);

        List<AreaDetail> newAreaDetails = new ArrayList<>();
        for (AreaDetail areaDetail : areaDetails) {
            newAreaDetails.add(areaDetail.translate(translation));
        }
        sketch.setAreaDetails(newAreaDetails);

        List<SymbolDetail> newSymbolDetails = new ArrayList<>();
        for (SymbolDetail symbolDetail : symbolDetails) {
            newSymbolDetails.add(symbolDetail.translate(translation));
        }
        sketch.setSymbolDetails(newSymbolDetails);

        List<TextDetail> newTextDetails = new ArrayList<>();
        for (TextDetail textDetail : textDetails) {
            newTextDetails.add(textDetail.translate(translation));
        }
        sketch.setTextDetails(newTextDetails);

        List<CrossSectionDetail> newCrossSectionDetails = new ArrayList<>();
        for (CrossSectionDetail crossSectionDetail : crossSectionDetails) {
            newCrossSectionDetails.add(crossSectionDetail.translate(translation));
        }
        sketch.setCrossSectionDetails(newCrossSectionDetails);

        return sketch;
    }

    @Override
    public Sketch scale(float scale) {
        Sketch sketch = new Sketch();

        List<PathDetail> newPathDetails = new ArrayList<>();
        for (PathDetail pathDetail : pathDetails) {
            newPathDetails.add(pathDetail.scale(scale));
        }
        sketch.setPathDetails(newPathDetails);

        List<AreaDetail> newAreaDetails = new ArrayList<>();
        for (AreaDetail areaDetail : areaDetails) {
            newAreaDetails.add(areaDetail.scale(scale));
        }
        sketch.setAreaDetails(newAreaDetails);

        List<SymbolDetail> newSymbolDetails = new ArrayList<>();
        for (SymbolDetail symbolDetail : symbolDetails) {
            newSymbolDetails.add(symbolDetail.scale(scale));
        }
        sketch.setSymbolDetails(newSymbolDetails);

        List<TextDetail> newTextDetails = new ArrayList<>();
        for (TextDetail textDetail : textDetails) {
            newTextDetails.add(textDetail.scale(scale));
        }
        sketch.setTextDetails(newTextDetails);

        List<CrossSectionDetail> newCrossSectionDetails = new ArrayList<>();
        for (CrossSectionDetail crossSectionDetail : crossSectionDetails) {
            SketchDetail scaled = crossSectionDetail.scale(scale);
            newCrossSectionDetails.add((CrossSectionDetail) scaled);
        }
        sketch.setCrossSectionDetails(newCrossSectionDetails);

        return sketch;
    }

    /**
     * Cross-sections are drawn enlarged (or shrunk) by the cross-section scale about their
     * positions, so the sketch's extent has to take that into account rather than using their
     * natural size.
     */
    @Override
    public void updateBoundingBox(Shape shape) {
        if (shape instanceof CrossSectionDetail) {
            CrossSectionDetail crossSectionDetail = (CrossSectionDetail) shape;
            updateBoundingBox(crossSectionDetail.getTopLeftAtScale(crossSectionScale));
            updateBoundingBox(crossSectionDetail.getBottomRightAtScale(crossSectionScale));
        } else {
            super.updateBoundingBox(shape);
        }
    }

    public void recalculateBoundingBox() {
        resetBoundingBox();
        for (SketchDetail sketchDetail : allSketchDetails()) {
            updateBoundingBox(sketchDetail);
        }
    }
}
