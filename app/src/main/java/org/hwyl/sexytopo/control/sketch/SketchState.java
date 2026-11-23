package org.hwyl.sexytopo.control.sketch;

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
    // used to jump back to the previous tool when using one-use tools
    private SketchTool previousTool = SketchTool.MOVE;

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

    SketchTool getPreviousSketchTool() {
        return previousTool;
    }

    /**
     * The tool the user is working with: the current one, unless that's a pinch or pan, in which
     * case the one it interrupted, which it goes back to after.
     */
    SketchTool getUnderlyingSketchTool() {
        return currentTool.isModal() ? previousTool : currentTool;
    }

    /**
     * Switch tool. The tool being left is remembered, so a one-use tool can go back to it, unless
     * it's a modal one (a pinch or a pan) that is itself only temporary.
     */
    void setSketchTool(SketchTool sketchTool) {
        if (previousTool != currentTool && !currentTool.isModal()) {
            previousTool = currentTool;
        }
        currentTool = sketchTool;
    }

    /** Go back to the tool in use before the current, one-use, one. */
    void revertToPreviousTool() {
        setSketchTool(previousTool);
    }

    Colour getColour() {
        return colour;
    }

    void setColour(Colour colour) {
        this.colour = colour;
    }
}
