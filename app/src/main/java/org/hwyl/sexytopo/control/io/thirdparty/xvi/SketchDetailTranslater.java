package org.hwyl.sexytopo.control.io.thirdparty.xvi;

import java.util.ArrayList;
import java.util.List;
import org.hwyl.sexytopo.model.sketch.AutoScalableDetail;
import org.hwyl.sexytopo.model.sketch.LineDetail;

public abstract class SketchDetailTranslater<T extends AutoScalableDetail> {

    public List<LineDetail> translate(T sketchDetail) {
        List<LineDetail> relativePaths = asLineDetails(sketchDetail);
        List<LineDetail> sketchPaths = new ArrayList<>();
        for (LineDetail lineDetail : relativePaths) {
            LineDetail updated = lineDetail.translate(sketchDetail.getPosition());
            LineDetail scaled = updated.scale(sketchDetail.getSize());
            sketchPaths.add(scaled);
        }
        return sketchPaths;
    }

    public abstract List<LineDetail> asLineDetails(T sketchDetail);
}
