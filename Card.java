/**
 * Class to represent a single player card
 */

public class Card {
    public static final String[] SUITS = {"Hearts", "Clubs", "Spades", "Diamonds"};
    public static final char[] RANKS = {'A', '2', '3', '4', '5', '6', '7', '8', '9', 'T', 'J', 'Q', 'K'};

    private String suit;
    private char rank;

    public Card(String suit, char rank) {
        this.suit = suit;
        this.rank = rank;
    }

    public String toString() {
        return this.rank + ": " + this.suit;
    }
}
