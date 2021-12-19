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

            gameLoop();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private void lobbyPhase() throws IOException {
        // Spin until either the max number of players has been reached or the
        //  host tells the game to start
        while (this.players.size() < 5 && !hostingPlayer.reader.readLine().equals("start")) {
            Socket socket = publicListener.accept();
            PlayerHandler handler = new PlayerHandler(socket);
            players.add(handler);
        }

        // Start the game
        gameLoop();
    }

    private void gameLoop() throws IOException {
        // Init the structures
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
                StatusMessage begin = new StatusMessage("BEGIN", "host");
                begin.makeHeader();
                String startingCards = Arrays.toString(players.get(i).heldCards.toArray());
                String[] data = {firstPlayer, roundNumber, scores, startingCards};
                begin.makeBody(data);
                players.get(i).writer.println(begin.composeMessage());
            }

            // Enter round loop
            PlayerHandler curPlayer = players.get(whoseTurn);
            while (true) {
                // Check to see if any player is out of cards
                for (int i = 0; i < numPlayers; i++) {
                    if (players.get(i).heldCards.size() == 0) {
                        break;
                    }
                }

                // Wait for input from the current player
                

                // Look for the new move
                
                // Update for next turn
                this.whoseTurn = (whoseTurn + 1) % (numPlayers);
                curPlayer = players.get(whoseTurn);
            }

            // Round over
            this.roundNum++;
        }
    }

    private void cleanUp(String winnerName) throws IOException {
        // Send out END messages with the winner
        StatusMessage end = new StatusMessage("END", "host");
        end.makeHeader();
        String[] data = {winnerName};
        end.makeBody(data);
        for (int i = 0; i < numPlayers; i++) {
            players.get(i).sendMsg(end);
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

        public PlayerHandler(Socket socket) {
            try {
                this.socket = socket;
                writer = new PrintWriter(socket.getOutputStream());
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (Exception e) {
                System.out.println(e);
            }
        }

        public void sendMsg(StatusMessage message) {
            writer.println(message.composeMessage());
        }
    }    
}
