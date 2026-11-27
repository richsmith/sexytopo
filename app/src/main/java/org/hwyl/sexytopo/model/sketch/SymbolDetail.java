package org.hwyl.sexytopo.model.sketch;

import android.graphics.drawable.Drawable;
import org.hwyl.sexytopo.model.geometry.Coord2D;

public class SymbolDetail extends PointDetail implements AutoScalableDetail {

    private final Symbol symbol;
    private final float size;
    private final float angle;

    private final Drawable drawable;

    public SymbolDetail(Coord2D location, Symbol symbol, Colour colour, float size, float angle) {

        super(colour, location);
        this.symbol = symbol;
        this.size = size;
        this.angle = angle;

        this.drawable = symbol.createDrawable();
    }

    public Symbol getSymbol() {
        return symbol;
    }

    public float getSize() {
        return size;
    }

    public Drawable getDrawable() {
        return drawable;
    }

    public float getAngle() {
        return angle;
    }

    /** A symbol the same as this one but in a different colour. */
    public SymbolDetail withColour(Colour colour) {
        return new SymbolDetail(getPosition(), getSymbol(), colour, getSize(), getAngle());
    }

    @Override
    public SymbolDetail translate(Coord2D translation) {
        return new SymbolDetail(
                getPosition().plus(translation), getSymbol(), getColour(), getSize(), getAngle());
    }

    /** A symbol that points somewhere is turned as well as moved. */
    @Override
    public SymbolDetail rotate(float degrees) {
        float newAngle = symbol.isDirectional() ? (getAngle() + degrees) % 360 : getAngle();
        return new SymbolDetail(
                getPosition().rotate(degrees), getSymbol(), getColour(), getSize(), newAngle);
    }

    @Override
    public SymbolDetail scale(float scale) {
        return new SymbolDetail(
                getPosition(), getSymbol(), getColour(), getSize() * scale, getAngle());
    }

    @Override
    public boolean couldBeVisibleAtScale(float scale) {
        return size * scale >= 1;
    }

    @Override
    public float getDistanceFrom(Coord2D point) {
        // Check if point is within the symbol's radius
        float radius = size / 2;
        float distance = super.getDistanceFrom(point);

        // If within the symbol bounds, return scaled distance to center
        // This gives priority without completely blocking line deletion
        if (distance <= radius) {
            return distance * 0.5f;
        }

        // Otherwise, return distance from edge
        return distance - radius;
    }
}
