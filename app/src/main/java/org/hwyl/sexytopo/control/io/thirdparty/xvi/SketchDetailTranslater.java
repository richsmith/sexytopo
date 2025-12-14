package org.hwyl.sexytopo.control.io.thirdparty.xvi;

import org.hwyl.sexytopo.model.sketch.AutoScalableDetail;
import org.hwyl.sexytopo.model.sketch.PathDetail;

import java.util.ArrayList;
import java.util.List;

public abstract class SketchDetailTranslater<T extends AutoScalableDetail> {

    public List<PathDetail> translate(T sketchDetail) {
        List<PathDetail> relativePaths = asPathDetails(sketchDetail);
        List<PathDetail> sketchPaths = new ArrayList<>();
        for (PathDetail pathDetail : relativePaths) {
            PathDetail updated = pathDetail.translate(sketchDetail.getPosition());
            PathDetail scaled = updated.scale(sketchDetail.getSize());
            sketchPaths.add(scaled);
        }
        return sketchPaths;
    }

    public abstract List<PathDetail> asPathDetails(T sketchDetail);

}
