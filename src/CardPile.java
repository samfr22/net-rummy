package src;
import java.util.*;

/**
 * Class to represent a stack of player cards
 */

public class CardPile {
    // Draw, Pile, Set
    public static final char[] PILE_TYPES = {'D', 'P', 'S'};

    private ArrayList<Card> cards;
    private char deckType;

    public CardPile(char deckType) {
        this.deckType = deckType;
        this.cards = new ArrayList<Card>();

        // If making a new deck - init it with a standard 52 card deck
        if (deckType == PILE_TYPES[0]) {
            for (int i = 0; i < Card.RANKS.length; i++) {
                for (int j = 0; j < Card.SUITS.length; j++) {
                    this.cards.add(new Card(Card.SUITS[j], Card.RANKS[i]));
                }
            }
            shuffle();
        }
    }

    public void shuffle() {
        // Ensure you can't shuffle the discard pile
        if (this.deckType == PILE_TYPES[1]) return;

        // Randomize the position of each card in the pile
        Collections.shuffle(this.cards);
    }

    public Card drawCard() {
        // Make sure that the deck type is only for drawing
        if (this.deckType != PILE_TYPES[0]) return null;
        // Make sure not empty
        if (this.cards.size() == 0) return null;

        // Return the top card from the deck - 0 is top
        Card topCard = this.cards.remove(0);

        return topCard;
    }

    public Card[] discardDraw(int cardDown) {
        // Make sure deck type is a discard pile
        if (this.deckType != PILE_TYPES[1]) return null;

        // Make sure not empty
        if (this.cards.size() == 0) return null;

        // Check final capacity to prevent exceptions
        if (this.cards.size() - cardDown < 0) return null;

        // Take the number of cards specified
        Card[] pileCards = new Card[cardDown];
        for (int i = 0; i < cardDown; i++) {
            pileCards[i] = this.cards.remove(0);
        }

        return pileCards;
    }

    public void addCard(Card card) {
        // Should only be able to add to a discard pile
        if (this.deckType != PILE_TYPES[1]) return;

        // Use stack-like ops -> Insert to top
        cards.add(0, card);
    }

}