package cc.cryptovegas.core.game.blackjack.card;

public enum Suit {
    DIAMONDS("♦"), CLUBS("♣"), HEARTS("♥"), SPADES("♠");

    private final String symbol;

    Suit(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }
}
