package org.hwyl.sexytopo.control.sketch;

import java.util.ArrayDeque;
import java.util.Deque;
import org.hwyl.sexytopo.model.geometry.Coord2D;
import org.hwyl.sexytopo.model.geometry.Projection2D;
import org.hwyl.sexytopo.model.geometry.Space;
import org.hwyl.sexytopo.model.sketch.Colour;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.sketch.SketchTool;
import org.hwyl.sexytopo.model.survey.Survey;

/**
 * What a sketch view is showing, which tool is in use and in what colour. The view and the parts it
 * hands work to share one of these, so those parts read the current survey, sketch and tool here
 * rather than asking the view for them.
 */
class SketchState {

    private Survey survey;
    private Sketch sketch;
    private Space<Coord2D> projection;
    private Projection2D projectionType = Projection2D.PLAN;

    private SketchTool currentTool = SketchTool.MOVE;
    // the tools the current one interrupted, to go back to when it's done, most recent first
    private final Deque<SketchTool> interruptedTools = new ArrayDeque<>();

    // what lines, areas, symbols and text are drawn in
    private Colour colour = Colour.BLACK;

    Survey getSurvey() {
        return survey;
    }

    void setSurvey(Survey survey) {
        this.survey = survey;
    }

    Sketch getSketch() {
        return sketch;
    }

    void setSketch(Sketch sketch) {
        this.sketch = sketch;
    }

    /** Where each station and leg of the survey is drawn, in survey coordinates. */
    Space<Coord2D> getProjection() {
        return projection;
    }

    void setProjection(Space<Coord2D> projection) {
        this.projection = projection;
    }

    Projection2D getProjectionType() {
        return projectionType;
    }

    void setProjectionType(Projection2D projectionType) {
        this.projectionType = projectionType;
    }

    SketchTool getSketchTool() {
        return currentTool;
    }

    /**
     * The tool the user is working with: the current one, unless that's a pinch or pan, in which
     * case the one it interrupted, which it goes back to after.
     */
    SketchTool getUnderlyingSketchTool() {
        if (currentTool.isModal() && !interruptedTools.isEmpty()) {
            return interruptedTools.peek();
        }
        return currentTool;
    }

    /** Switch to an ordinary tool, as chosen by the user, forgetting anything interrupted. */
    void setSketchTool(SketchTool sketchTool) {
        interruptedTools.clear();
        currentTool = sketchTool;
    }

    /**
     * Start a one-use or modal tool, to last until endTemporaryTool. A one-use tool interrupts an
     * ordinary one, and a modal one (a pinch or a pan) interrupts either, so the tool interrupted
     * is remembered to go back to. Otherwise the new tool replaces the current one.
     */
    void startTemporaryTool(SketchTool sketchTool) {
        if (getRank(currentTool) < getRank(sketchTool)) {
            interruptedTools.push(currentTool);
        }
        currentTool = sketchTool;
    }

    /** Go back to the tool the temporary one interrupted, or to Move if there isn't one. */
    void endTemporaryTool() {
        currentTool = interruptedTools.isEmpty() ? SketchTool.MOVE : interruptedTools.pop();
    }

    private static int getRank(SketchTool sketchTool) {
        if (sketchTool.isModal()) {
            return 2;
        }
        return sketchTool.isOneUse() ? 1 : 0;
    }

    Colour getColour() {
        return colour;
    }

    void setColour(Colour colour) {
        this.colour = colour;
    }
}
