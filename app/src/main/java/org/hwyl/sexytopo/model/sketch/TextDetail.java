package org.hwyl.sexytopo.model.sketch;

import org.hwyl.sexytopo.model.graph.Coord2D;


public class TextDetail extends SinglePositionDetail implements AutoScalableDetail {

    private final String text;
    private final float size;

    public TextDetail(Coord2D location, String text, Colour colour, float size) {
        super(colour, location);
        this.text = text;
        this.size = size;
    }

    public String getText() {
        return text;
    }

    public float getSize() {
        return size;
    }

    @Override
    public TextDetail translate(Coord2D point) {
        return new TextDetail(getPosition().plus(point), getText(), getColour(), getSize());
    }

    @Override
    public TextDetail scale(float scale) {
        return new TextDetail(getPosition(), getText(), getColour(), getSize() * scale);
    }

    @Override
    public float getDistanceFrom(Coord2D point) {
        // Text size represents approximate height; estimate width as proportional to length
        float estimatedWidth = text.length() * size * 0.6f;
        float estimatedHeight = size;

        Coord2D pos = getPosition();
        float dx = Math.abs(point.x - pos.x);
        float dy = Math.abs(point.y - pos.y);

        // If point is within the text bounds, return scaled distance to center
        // This gives priority without completely blocking line deletion
        if (dx <= estimatedWidth / 2 && dy <= estimatedHeight / 2) {
            return super.getDistanceFrom(point) * 0.5f;
        }

        // Otherwise, return distance to nearest edge of the text box
        float distX = Math.max(0, dx - estimatedWidth / 2);
        float distY = Math.max(0, dy - estimatedHeight / 2);
        return (float) Math.sqrt(distX * distX + distY * distY);
    }


}
