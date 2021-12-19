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

    public void run() {
        try {
            // Open the server socket at the port number
            publicListener = new ServerSocket(50100);

            // Should get a connection from the hosting player
            Socket hosting = publicListener.accept();

            // Check to make sure from localhost
            if (hosting.getInetAddress() != InetAddress.getLocalHost()) {
                System.out.println("First client wasn't local host");
                return;
            }

            // First host is valid - save information and let the host run
            this.hostingPlayer = new PlayerHandler(hosting);
            players.add(hostingPlayer);

            System.out.println("Hosting Player Connected. Opening lobby...");
            lobbyPhase();

            // Game continues and ends from the lobby phase method
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private void lobbyPhase() {
        try {
            // Spin until either the max number of players has been reached or the
            //  host tells the game to start
            while (this.players.size() < 5 && !hostingPlayer.reader.readLine().equals("start")) {
                Socket socket = publicListener.accept();
                PlayerHandler handler = new PlayerHandler(socket);
                
                // Get the CONNECT message to verify player
                String connect = handler.reader.readLine();
                while (connect == null || connect.equals("")) {
                    connect = handler.reader.readLine();
                }

                // Verify type
                String[] pieces = connect.split("\n");
                String msgType = pieces[0].split(": ")[1];
                if (!msgType.equals("CONNECT")) {
                    // Send back ERR and close connection
                    String[] data = {"CONNECT"};
                    handler.sendMsg("ERR", data);
                    handler.writer.close();
                    handler.reader.close();
                    handler.socket.close();
                    continue;
                }
                
                // Save the player name from the sender field
                handler.playerAlias = pieces[1].split(": ")[1];

                // Check that no other player is using the name
                for (int i = 0; i < players.size(); i++) {
                    if (players.get(i).playerAlias.equals(handler.playerAlias)) {
                        // Name in use
                        String[] data = {"CONNECT", "Name in use"};
                        handler.sendMsg("ERR", data);
                        handler.writer.close();
                        handler.reader.close();
                        handler.socket.close();
                    }
                }

                // Send back ok
                String[] data = {"CONNECT"};
                handler.sendMsg("OK", data);

                // Send out CONNECT messages for all current players to see the
                //  new player
                String[] data2 = {handler.playerAlias, String.valueOf(50100)};
                for (int i = 0; i < players.size(); i++) {
                    players.get(i).sendMsg("CONNECT", data2);
                }

                // Add player to list and look for a new player
                players.add(handler);
            }
        } catch (Exception e) {
            System.out.println("Lobby error: " + e);
        }

        // Start the game
        gameLoop();
    }

    private void gameLoop() {
        // Init the structures
        try {
            this.deck = new CardPile('D');
            this.discardPile = new CardPile('P');
            this.whoseTurn = (int) Math.random() * players.size();
            this.roundNum = 1;
            this.numPlayers = players.size();

            for (int i = 0; i < numPlayers; i++) {
                players.get(i).heldCards = new ArrayList<Card>();
            }

            // Game loop
            while (true) {
                // Check scores of all players and compose into msg while doing so
                String scores = new String();
                ArrayList<PlayerHandler> winningPlayers = new ArrayList<PlayerHandler>();
                for (int i = 0; i < numPlayers; i++) {
                    PlayerHandler player = players.get(i);
                    if (player.numPoints >= 500) {
                        winningPlayers.add(player);
                    }
                    scores += player.playerAlias + ": " + player.numPoints + ", ";
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
                for (int i = 0; i < numPlayers; i++) {
                    String startingCards = Arrays.toString(players.get(i).heldCards.toArray());
                    String[] data = {firstPlayer, roundNumber, scores, startingCards};
                    players.get(i).sendMsg("BEGIN", data);
                }

                // Enter round loop
                PlayerHandler curPlayer = players.get(whoseTurn);
                while (!outOfCards()) {
                    // Wait for input from the current player
                    

                    // Look for the new move

                    // Take action based on the new move
                    
                    // Update for next turn
                    this.whoseTurn = (whoseTurn + 1) % (numPlayers);
                    curPlayer = players.get(whoseTurn);
                }

                // Round over
                this.roundNum++;
            }
        } catch (Exception e) {
            System.out.println("Game error: " + e);
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

    private void cleanUp(String winnerName) throws IOException {
        // Send out END messages with the winner
        String[] data = {winnerName};
        for (int i = 0; i < numPlayers; i++) {
            players.get(i).sendMsg("END", data);
        }

        // Need OK messages from all players to know that the player is closing
        while (players.size() > 0) {
            for (int i = 0; i < players.size(); i++) {
                PlayerHandler player = players.get(i);
                String ok = player.reader.readLine();
                if (ok != null && !ok.equals("")) {
                    // Message received - check that the type is OK
                    String[] msgPieces = ok.split("\n");

                    // Parse the message based on the message type
                    // Type is always in the first token
                    String[] typeHeader = msgPieces[0].split(": ");
                    String msgType = typeHeader[1];
                    if (msgType.equals("OK")) {
                        // Can close up the connection with this player
                        player.socket.close();
                        player.reader.close();
                        player.writer.close();
                        players.remove(i);
                        i--;
                    }
                }
            }
        }

        // All players have closed connections
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
                writer = new PrintWriter(socket.getOutputStream());
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (Exception e) {
                System.out.println(e);
            }
        }

        /**
         * Send a message into the socket
         * @param msgType The type of StatusMessage to make
         * @param data The data for the body
         */
        public void sendMsg(String msgType, String[] data) {
            StatusMessage message = new StatusMessage(msgType, "host");
            message.makeHeader();
            message.makeBody(data);
            writer.println(message.composeMessage());
        }
    }    
}
