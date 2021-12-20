package src;

import java.util.Collections;
import java.util.ArrayList;

/**
 * Author: Samuel Fritz
 * CSCI 4431
 * 
 * Class to represent a stack of player cards, for use as a deck, discard pile,
 * or meld/put down set
 */
public class CardPile {
    // Draw, Pile, Set
    public static final char[] PILE_TYPES = {'D', 'P', 'S'};

    // The representation of the deck. Index 0 represents the top of the pile
    private ArrayList<Card> cards;
    private char deckType;

    /**
     * Constructor
     * @param deckType A matching type of pile
     */
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

    /**
     * Shuffles the deck by randomizing the placement of cards in the pile.
     * Only can be used for piles that are being used as decks
     */
    public void shuffle() {
        // Ensure you can't shuffle non-decks
        if (this.deckType != PILE_TYPES[0]) return;

        // Randomize the position of each card in the pile
        Collections.shuffle(this.cards);
    }

    /**
     * Draws a card from the top of the deck. Only can be used by piles that
     * are decks
     * @return The top card of the deck; null if the deck is empty or the
     *  pile type does not match
     */
    public Card drawCard() {
        // Make sure that the deck type is only for drawing
        if (this.deckType != PILE_TYPES[0]) return null;
        // Make sure not empty
        if (this.cards.size() == 0) return null;

        // Return the top card from the deck - 0 is top
        Card topCard = this.cards.remove(0);

        return topCard;
    }

    public void changeDeckType(char newType) {
        this.deckType = newType;
    }

    /**
     * Draws a set of cards from the discard pile, based on the position of the
     * desired card in the deck. The card and all cards above it are returned
     * @param cardDown The position of the card in the discard pile
     * @return An set of cards from the discard pile. If the pile type does not
     *  match or there are not enough cards, return nothing
     */
    public Card[] discardDraw(int cardDown) {
        // Make sure deck type is a discard pile
        if (this.deckType != PILE_TYPES[1]) return null;

        // Make sure not empty
        if (this.cards.size() == 0) return null;

        // Take the number specified or as many as possible
        int numTaken = 0;
        ArrayList<Card> buffer = new ArrayList<Card>();
        while (this.cards.size() != 0 && numTaken <= cardDown) {
            buffer.add(this.cards.remove(0));
            numTaken++;
        }

        return (Card[]) buffer.toArray();
    }

    /**
     * Adds a card to the top of the pile. Used for for discard piles and sets
     * @param card The card to be added to the top of the pile
     */
    public void addCard(Card card) {
        // Shouldn't be able to put back on the deck
        if (this.deckType == PILE_TYPES[0]) return;

        // Use stack-like ops -> Insert to top
        cards.add(0, card);
    }

    /**
     * Pile outputted into string
     */
    public String toString() {
        // If a deck, should only return the top card
        if (this.deckType == PILE_TYPES[0]) {
            return "Top card: " + cards.get(0).toString();
        }
        
        // Otherwise print out all of the cards in the pile
        String str = "";
        for (int i = 0; i < cards.size(); i++) {
            str += cards.get(i).toString() + ", ";
        }
        return str;
    }

    /**
     * Returns all of the cards in the pile. Only usable for non-decks
     * @return An array of inside the pile
     */
    public Card[] allCards() {
        if (this.deckType == PILE_TYPES[0]) return null;

        return (Card[]) this.cards.toArray();
    }

    /**
     * Returns the number of cards to a an outside caller
     * @return The number of cards tracked by this pile
     */
    public int numCards() {
        return this.cards.size();
    }
}