package org.hwyl.sexytopo.model.sketch;

import android.graphics.drawable.Drawable;

import org.hwyl.sexytopo.model.graph.Coord2D;


public class SymbolDetail extends SinglePositionDetail implements AutoScalableDetail {

    private final Symbol symbol;
    private final float size;
    private final float angle;

    private final Drawable drawable;

    public SymbolDetail(Coord2D location,
                        Symbol symbol,
                        Colour colour,
                        float size,
                        float angle) {

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

    @Override
    public SymbolDetail translate(Coord2D point) {
        return new SymbolDetail(
                getPosition().plus(point), getSymbol(), getColour(), getSize(), getAngle());
    }

}
