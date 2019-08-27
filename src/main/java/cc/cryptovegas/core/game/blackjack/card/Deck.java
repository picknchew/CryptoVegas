package cc.cryptovegas.core.game.blackjack.card;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Deck {
    private static final ThreadLocal<SecureRandom> random = ThreadLocal.withInitial(SecureRandom::new);
    private static final List<Card> DEFAULT_DECK = new ArrayList<>();

    static {
        for (Rank rank : Rank.values()) {
            for (Suit suit : Suit.values()) {
                DEFAULT_DECK.add(new Card(rank, suit));
            }
        }
    }

    private final List<Card> cards;

    public Deck() {
        this.cards = new ArrayList<>(DEFAULT_DECK);
        Collections.shuffle(cards, random.get());
    }

    public Card getCard() {
        return cards.remove(0);
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();

        cards.stream().map(Card::toString).collect(Collectors.joining("-"));

        return builder.toString();
    }
}
