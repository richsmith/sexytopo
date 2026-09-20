package org.hwyl.sexytopo.control.graph;

/**
 * The marks on the handle bar of a cross-section's box on the sketch, which show that it can be
 * dragged. There are three: ticks for a vertical cross-section and dashes for a horizontal one,
 * echoing the direction of the line that marks its station. That means the two kinds can be told
 * apart without relying on the colour of the box.
 *
 * <p>This only works out where the marks go, in view coordinates, so that it can be tested without
 * a canvas. Each mark is a line from a start point to an end point.
 */
public final class CrossSectionGrip {

    private static final int MARK_COUNT = 3;

    // The shortest a dash is made, as a fraction of its full length, to fit a narrow bar
    private static final float MIN_DASH_FRACTION = 0.125f;

    // Once the dashes are that short, everything is shrunk together, but never below this
    private static final float MIN_SHRINK = 0.1f;

    // startX, startY, endX, endY for each mark in turn
    private final float[] marks;

    private CrossSectionGrip(float[] marks) {
        this.marks = marks;
    }

    /**
     * Three vertical ticks side by side, centred on (centreX, centreY).
     *
     * @param length how tall each tick is
     * @param spacing how far apart the ticks are, from one to the next
     */
    public static CrossSectionGrip vertical(
            float centreX, float centreY, float length, float spacing) {
        float halfLength = length / 2f;
        float[] xs = {centreX - spacing, centreX, centreX + spacing};
        float[] marks = new float[MARK_COUNT * 4];
        for (int i = 0; i < MARK_COUNT; i++) {
            marks[i * 4] = xs[i];
            marks[i * 4 + 1] = centreY - halfLength;
            marks[i * 4 + 2] = xs[i];
            marks[i * 4 + 3] = centreY + halfLength;
        }
        return new CrossSectionGrip(marks);
    }

    /**
     * Three horizontal dashes in a row, centred on (centreX, centreY). If the row would be wider
     * than maxWidth the dashes are made shorter to fit, keeping the gaps between them so that they
     * still look like separate dashes. Only if they are as short as they can go is everything
     * shrunk together.
     *
     * @param length how long each dash is
     * @param gap the space between one dash and the next
     * @param maxWidth the widest the row of dashes may be
     */
    public static CrossSectionGrip horizontal(
            float centreX, float centreY, float length, float gap, float maxWidth) {
        float fullWidth = MARK_COUNT * length + (MARK_COUNT - 1) * gap;

        float dashLength = length;
        float dashGap = gap;
        if (maxWidth < fullWidth) {
            float shortestDash = length * MIN_DASH_FRACTION;
            float shortestWidth = MARK_COUNT * shortestDash + (MARK_COUNT - 1) * gap;
            if (maxWidth >= shortestWidth) {
                dashLength = (maxWidth - (MARK_COUNT - 1) * gap) / MARK_COUNT;
            } else {
                float shrink = Math.max(MIN_SHRINK, maxWidth / shortestWidth);
                dashLength = shortestDash * shrink;
                dashGap = gap * shrink;
            }
        }
        float pitch = dashLength + dashGap;

        float[] marks = new float[MARK_COUNT * 4];
        for (int i = 0; i < MARK_COUNT; i++) {
            float dashCentreX = centreX + (i - 1) * pitch;
            marks[i * 4] = dashCentreX - dashLength / 2f;
            marks[i * 4 + 1] = centreY;
            marks[i * 4 + 2] = dashCentreX + dashLength / 2f;
            marks[i * 4 + 3] = centreY;
        }
        return new CrossSectionGrip(marks);
    }

    /**
     * How wide a row of marks can be on a bar of the given width: the bar less its rounded corners
     * and the overhang of the marks' end caps, though never less than half of the bar so that there
     * is always something to draw.
     */
    public static float getUsableWidth(float barWidth, float cornerRadius, float strokeWidth) {
        return Math.max(barWidth - 2 * cornerRadius - strokeWidth, barWidth / 2f);
    }

    public int getMarkCount() {
        return MARK_COUNT;
    }

    public float getStartX(int mark) {
        return marks[mark * 4];
    }

    public float getStartY(int mark) {
        return marks[mark * 4 + 1];
    }

    public float getEndX(int mark) {
        return marks[mark * 4 + 2];
    }

    public float getEndY(int mark) {
        return marks[mark * 4 + 3];
    }
}
