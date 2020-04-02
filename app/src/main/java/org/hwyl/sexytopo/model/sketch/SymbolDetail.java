package org.hwyl.sexytopo.model.sketch;

import org.hwyl.sexytopo.model.graph.Coord2D;


public class SymbolDetail extends SinglePositionDetail {

    private Symbol symbol;

    public SymbolDetail(Coord2D location, Symbol symbol, Colour colour) {
        super(colour, location);
        this.symbol = symbol;
    }


    public Symbol getSymbol() {
        return symbol;
    }


    @Override
    public SymbolDetail translate(Coord2D point) {
        return new SymbolDetail(getPosition().plus(point), getSymbol(), getColour());
    }

}
