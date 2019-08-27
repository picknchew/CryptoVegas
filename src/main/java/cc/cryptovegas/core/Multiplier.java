package cc.cryptovegas.core;


import cc.cryptovegas.core.cryptocurrency.model.Coin;

import java.math.BigDecimal;

public enum Multiplier {
    DOUBLE("0.25");

    private final BigDecimal decimal;

    Multiplier(String stringRepresentation) {
        this.decimal = new BigDecimal(stringRepresentation);
    }

    public Coin apply(Coin coin) {
        return Coin.valueOf(decimal.multiply(new BigDecimal(coin.getValue())).longValue());
    }
}
