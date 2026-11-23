package org.hwyl.sexytopo.control.sketch;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.model.geometry.Coord2D;
import org.hwyl.sexytopo.model.sketch.SinglePositionDetail;
import org.hwyl.sexytopo.model.sketch.Sketch;
import org.hwyl.sexytopo.model.sketch.SketchTool;

/** Moves a symbol or piece of text by as far as it's dragged. */
class MoveDetailTool extends DragDetailTool {

    MoveDetailTool(
            Viewport viewport, SketchState state, SketchRenderer sketchRenderer, ToolHost host) {
        super(
                viewport,
                state,
                sketchRenderer,
                host,
                SketchTool.MOVE_DETAIL,
                R.string.sketch_move_detail_instruction);
    }

    @Override
    SinglePositionDetail drag(SinglePositionDetail detail, Coord2D from, Coord2D to) {
        Coord2D offset = getOffset(from, to);
        return isNoMove(offset) ? detail : (SinglePositionDetail) detail.translate(offset);
    }

    @Override
    void commit(Sketch sketch, SinglePositionDetail detail, Coord2D from, Coord2D to) {
        Coord2D offset = getOffset(from, to);
        if (!isNoMove(offset)) {
            sketch.moveDetail(detail, offset);
        }
    }

    /** How far the drag goes, in survey coordinates. */
    private Coord2D getOffset(Coord2D from, Coord2D to) {
        return viewport.toSurvey(to).minus(viewport.toSurvey(from));
    }

    private static boolean isNoMove(Coord2D offset) {
        return offset.x == 0 && offset.y == 0;
    }
}
