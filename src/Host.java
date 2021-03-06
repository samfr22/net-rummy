package src;

import java.util.ArrayList;
import java.util.Arrays;
import java.net.*;
import java.io.*;

/**
 * Author: Samuel Fritz
 * CSCI 4431
 * 
 * The acting server - Host - program to coordinate the entire game and open a
 * public lobby that other players can connect to
 */
public class Host implements Runnable {
    
    // Management structures 
    private ArrayList<PlayerHandler> players;
    private PlayerHandler hostingPlayer;
    private ServerSocket publicListener;
    private int numPlayers;
    
    // Game related structures
    private CardPile deck;
    private CardPile discardPile;
    private int whoseTurn;
    private int roundNum;
    private final int POINTS_TO_WIN = 100;

    public void run() {
        try {
            // Open the server socket at the port number
            publicListener = new ServerSocket(50100);

            // Should get a connection from the hosting player
            Socket hosting = publicListener.accept();

            // Check to make sure from localhost
            if (!hosting.getInetAddress().toString().equals("/" + InetAddress.getLocalHost().getHostAddress())) {
                System.out.println("First client wasn't local host");
                publicListener.close();
                return;
            }

            // First host is valid - save information and let the host run
            this.hostingPlayer = new PlayerHandler(hosting);
            this.players = new ArrayList<PlayerHandler>();
            players.add(hostingPlayer);

            // Read a CONNECT from the hosting - skip straight to alias
            String initConnect = hostingPlayer.reader.readLine();
            initConnect = hostingPlayer.reader.readLine();
            hostingPlayer.playerAlias = initConnect.split(": ")[1];

            System.out.println("Opening lobby at " + InetAddress.getLocalHost().toString() + "...\n");
            hostingPlayer.clearReader();

            // Send back an OK for the hosting player
            String[] data = {"CONNECT"};
            hostingPlayer.sendMsg("OK", data);

            lobbyPhase();

            // Game continues and ends from the lobby phase method
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Inner class to allow the lobby phase to be using multiple threads, as
     * the accept() method for a new connection blocks. Handles new player
     * connections and adding them to the player list
     */
    class Lobby implements Runnable {
        public void run() {
            try {
                while (true) {
                    Socket socket = publicListener.accept();
                    PlayerHandler handler = new PlayerHandler(socket);

                    // Get the CONNECT message to verify player
                    String connect = handler.reader.readLine();
                    while (connect == null || connect.equals("")) {
                        connect = handler.reader.readLine();
                    }

                    // Verify type
                    String msgType = connect.split(": ")[1];
                    if (!msgType.equals("CONNECT")) {
                        // Send back ERR and close connection
                        String[] data = {"CONNECT"};
                        handler.sendMsg("ERR", data);
                        handler.writer.close();
                        handler.reader.close();
                        handler.socket.close();
                    }
                    
                    connect = handler.reader.readLine();
                    // Save the player name from the sender field
                    handler.playerAlias = connect.split(": ")[1];

                    handler.clearReader();


                    // Check that no other player is using the name
                    for (int i = 0; i < players.size(); i++) {
                        if (players.get(i).playerAlias.equals(handler.playerAlias)) {
                            // Name in use
                            String[] data = {"CONNECT", "Name in use"};
                            handler.sendMsg("ERR", data);
                            handler.writer.close();
                            handler.reader.close();
                            handler.socket.close();
                            continue;
                        }
                    }

                    // Send back ok
                    String[] data = {"CONNECT"};
                    handler.sendMsg("OK", data);

                    // Send out CONNECT messages for all current players to see the
                    //  new player
                    String[] data2 = {handler.playerAlias, String.valueOf(50100)};
                    for (int i = 0; i < players.size(); i++) {
                        // Prevents issues with the host accessing the same input stream
                        if (players.get(i).playerAlias.equals(hostingPlayer.playerAlias)) continue;

                        players.get(i).sendMsg("CONNECT", data2);
                    }
                    recvReplies("CONNECT");

                    // For hosting player
                    System.out.println("New player connected: " + handler.playerAlias);

                    // Add player to list and look for a new player
                    players.add(handler);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void lobbyPhase() {
        try {
            // Start a new thread to listen for incoming requests
            Thread listener = new Thread(new Lobby());
            listener.start();

            // Spin until either the max number of players has been reached or the
            //  host tells the game to start
            while (this.players.size() < 5) {
                // Check if the host player has told it to start
                String hostingPlayerMsg = hostingPlayer.reader.readLine();
                if (hostingPlayerMsg == null || hostingPlayerMsg.equals("")) continue;

                if (hostingPlayerMsg.equals("start")) {
                    break;
                }
            }

            listener.interrupt();
            listener = null;
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Start the game
        gameLoop();
    }

    private void gameLoop() {
        // Init the structures
        try {
            this.roundNum = 1;
            this.numPlayers = players.size();
            
            for (int i = 0; i < numPlayers; i++) {
                players.get(i).heldCards = new ArrayList<Card>();
            }
            
            // Game loop
            while (true) {
                this.deck = new CardPile('D');
                this.discardPile = new CardPile('P');
                this.whoseTurn = (int) (Math.random() * numPlayers);

                // Check scores of all players and compose into msg while doing so
                String scores = new String();
                ArrayList<PlayerHandler> winningPlayers = new ArrayList<PlayerHandler>();
                for (int i = 0; i < numPlayers; i++) {
                    PlayerHandler player = players.get(i);
                    if (player.numPoints >= POINTS_TO_WIN) {
                        winningPlayers.add(player);
                    }
                    scores += player.playerAlias + " - " + player.numPoints + ", ";
                }

                // Check if any winners
                if (winningPlayers.size() != 0) {
                    // Find the one with the highest score
                    PlayerHandler highestScore = winningPlayers.get(0);
                    int numWinners = winningPlayers.size();
                    for (int i = 0; i < numWinners; i++) {
                        if (winningPlayers.get(i).numPoints > highestScore.numPoints) {
                            highestScore = winningPlayers.get(i);
                        }
                    }
                    // End the game
                    cleanUp(highestScore.playerAlias);
                    return;
                }

                // Starting hands - 13 if 2 players, 7 if more
                int startingAmount = 7;
                if (numPlayers == 2) startingAmount = 13;
                for (int i = 0; i < startingAmount; i++) {
                    // Make sure that cards are being dealt spread out
                    for (int j = 0; j < numPlayers; j++) {
                        players.get(j).heldCards.add(deck.drawCard());
                    }
                }

                // Send out BEGIN messages
                String firstPlayer = players.get(whoseTurn).playerAlias;
                String roundNumber = String.valueOf(this.roundNum);
                // First card in discard pile
                Card firstDiscard = deck.drawCard();
                discardPile.addCard(firstDiscard);
                String firstDis = firstDiscard.toString();
                for (int i = 0; i < numPlayers; i++) {
                    String startingCards = "";
                    for (int j = 0; j < players.get(i).heldCards.size(); j++) {
                        startingCards += players.get(i).heldCards.get(j).toString() + ", ";
                    }
                    String[] data = {firstPlayer, roundNumber, scores, startingCards, firstDis};
                    players.get(i).sendMsg("BEGIN", data);
                }
                // Make sure players send back an OK
                recvReplies("BEGIN");

                // Enter round loop
                PlayerHandler curPlayer = players.get(whoseTurn);
                boolean roundRun = true;
                while (roundRun) {
                    curPlayer.clearReader();
                    // Look for the deck/discard pile taking
                    String action = curPlayer.reader.readLine();
                    while (!action.equals("T") && !action.equals("P")) {
                        while (action == null || action.equals("")) {
                            action = curPlayer.reader.readLine();
                        }
                        // Sender
                        action = curPlayer.reader.readLine();
                        // Intermediary
                        action = curPlayer.reader.readLine();
                        // Action
                        action = curPlayer.reader.readLine();
                    }

                    String cardData = "";
                    if (action.equals("T")) {
                        // Deal the top card of the deck out
                        Card topCard = deck.drawCard();
                        if (topCard == null) {
                            // If deck is empty, need to reshuffle the discard
                            //  into the deck
                            deck = discardPile;
                            deck.changeDeckType('D');
                            deck.shuffle();
                            discardPile = new CardPile('P');

                            // Try again
                            topCard = deck.drawCard();
                        }

                        // Set data as the card
                        cardData = topCard.toString();
                    } else if (action.equals("P")) {
                        // Get the position in the discard pile
                        action = curPlayer.reader.readLine();
                        Card[] discardCards = discardPile.discardDraw(Integer.valueOf(action));

                        // Set the data piece with the card information
                        for (int i = 0; i < discardCards.length; i++) {
                            cardData += discardCards[i].toString() + ", ";
                        }
                    }

                    curPlayer.clearReader();
                    // Send back the retrieved card(s)
                    String[] data = {"TURN", cardData};
                    curPlayer.sendMsg("OK", data);

                    // Let the player program handle melds - waiting until the
                    //  next TURN message is sent to contain info about the 
                    //  outcome of the turn
                    String turnOver = curPlayer.reader.readLine();
                    while (turnOver == null || turnOver.equals("")) {
                        turnOver = curPlayer.reader.readLine();
                    }

                    // Skip to the body
                    // Sender
                    action = curPlayer.reader.readLine();
                    // Intermediary
                    action = curPlayer.reader.readLine();
                    // Discard identifier
                    action = curPlayer.reader.readLine();
                    // Hand
                    action = curPlayer.reader.readLine();

                    // Get all of the cards in the player's hand

                    if (outOfCards()) {
                        roundRun = false;
                        curPlayer.clearReader();
                        continue;
                    }

                    curPlayer.heldCards.clear();
                    String[] handMsg = action.split(": ");
                    if (handMsg.length == 1) {
                        roundRun = false;
                        curPlayer.clearReader();
                        continue;
                    }
                    String[] hand = handMsg[1].split(", ");

                    for (int i = 0; i < hand.length; i++) {
                        String[] card = hand[i].split(" of ");
                        curPlayer.heldCards.add(new Card(card[1], card[0].charAt(0)));
                    }
                    // Save the points for player
                    action = curPlayer.reader.readLine();
                    curPlayer.numPoints = Integer.valueOf(action.split(": ")[1]);

                    // Get the discarded card
                    action = curPlayer.reader.readLine();
                    String[] discarded = action.split(" of ");
                    Card discardedCard = new Card(discarded[1], discarded[0].charAt(0));
                    discardPile.addCard(discardedCard);

                    curPlayer.clearReader();
                    
                    // Update for next turn
                    this.whoseTurn = (whoseTurn + 1) % (numPlayers);
                    PlayerHandler nextPlayer = players.get(whoseTurn);

                    // Send out a MOVE message to all players
                    String[] data3 = {curPlayer.playerAlias, String.valueOf(curPlayer.numPoints), nextPlayer.playerAlias, discardedCard.toString(), discardPile.toString()};
                    for (int i = 0; i < players.size(); i++) {
                        players.get(i).sendMsg("MOVE", data3);
                    }

                    recvReplies("MOVE");
                    
                    curPlayer = nextPlayer;
                }

                // Round over
                this.roundNum++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Helper method to make sure that all players send back an OK message for
     * a previously sent message. Requires the reader to be cleared
     * @param messageType The message type to be acknowledged
     */
    void recvReplies(String messageType) {
        for (int i = 0; i < numPlayers; i++) {
            try {
                // Chance for client to read off the channel
                Thread.sleep(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
            players.get(i).clearReader();
        }
    }

    /**
     * Helper method to detect if any player is out of cards. Prevents
     * unreachable code errors in the round loop
     * @return True if some player has no more cards remaining in hand; False
     *  if no players are out of cards
     */
    private boolean outOfCards() {
        for (int i = 0; i < numPlayers; i++) {
            if (players.get(i).heldCards.size() == 0) {
                return true;
            }
        }
        return false;
    }

    private void cleanUp(String winnerName) {
        try {
            // Send out END messages with the winner
            String[] data = {winnerName};
            for (int i = 0; i < numPlayers; i++) {
                players.get(i).sendMsg("END", data);
            }

            // Need OK messages from all players to know that the player is closing
            recvReplies("END");

            // Clean up connection for each player
            for (int i = 0; i < players.size(); i++) {
                PlayerHandler player = players.get(i);
                player.reader.close();
                player.writer.close();
                player.socket.close();
                players.remove(i);
                i--;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Inner class to handle player communication
     */
    class PlayerHandler {
        // Player specific game information
        private ArrayList<Card> heldCards;
        private int numPoints;

        // Connection and communication
        private Socket socket;
        private BufferedReader reader;
        private PrintWriter writer;
        private String playerAlias;

        /**
         * Constructor
         * @param socket The connection socket this handler is for
         */
        public PlayerHandler(Socket socket) {
            try {
                this.socket = socket;
                writer = new PrintWriter(socket.getOutputStream(), true);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * Send a message into the socket
         * @param msgType The type of StatusMessage to make
         * @param data The data for the body
         */
        public void sendMsg(String msgType, String[] data) {
            // System.out.println("Sending a " + msgType + " to " + this.playerAlias);
            StatusMessage message = new StatusMessage(msgType, "host");
            message.makeHeader();
            message.makeBody(data);
            String msg = message.composeMessage();
            // System.out.println("Sending\n------------\n" + msg + "--------------\nto " + playerAlias + "\n");
            writer.println(msg);
        }

        /**
         * Clear out the output buffer after useful information has been taken
         */
        public void clearReader() {
            try {
                while (!reader.readLine().equals(""));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }    
}
