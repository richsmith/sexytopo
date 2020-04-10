package org.hwyl.sexytopo.model.sketch;

import org.hwyl.sexytopo.model.graph.Coord2D;


public class SymbolDetail extends SinglePositionDetail {

    private final Symbol symbol;
    private final float size;

    public SymbolDetail(Coord2D location, Symbol symbol, Colour colour, float size) {
        super(colour, location);
        this.symbol = symbol;
        this.size = size;
    }


    public Symbol getSymbol() {
        return symbol;
    }

    public float getSize() {
        return size;
    }

    @Override
    public SymbolDetail translate(Coord2D point) {
        return new SymbolDetail(getPosition().plus(point), getSymbol(), getColour(), getSize());
    }

}
