package src;

/**
 * Author: Samuel Fritz
 * CSCI 4431
 * 
 * Class to represent a single player card
 */
public class Card {
    // All possible suit variants, in strings for easy comparison
    public static final String[] SUITS = {"Hearts", "Clubs", "Spades", "Diamonds"};
    // All possible rank variants, in strings for easy comparison
    public static final char[] RANKS = {'A', '2', '3', '4', '5', '6', '7', '8', '9', 'T', 'J', 'Q', 'K'};

    String suit;
    char rank;

    /**
     * Constructor
     * @param suit A matching suit of one of the variants
     * @param rank A matching rank of one of the variants
     */
    public Card(String suit, char rank) {
        this.suit = suit;
        this.rank = rank;
    }

    /**
     * Returns a composite string for the card using the rank and suit
     */
    public String toString() {
        return this.rank + " of " + this.suit;
    }

    /**
     * Computes the number of points with a given set of cards
     * @param cards The cards to be totaled up
     * @return The number of points in the set
     */
    public static int computePoints(Card[] cards) {
        int points = 0;
        for (int i = 0; i < cards.length; i++) {
            if (cards[i].rank == 'A' || cards[i].rank == 'J' || cards[i].rank == 'Q' || cards[i].rank == 'K' || cards[i].rank == 'T') {
                points += 10;
            } else {
                points += (cards[i].rank - 48);
            }
        }
        return points;
    }
}
