import java.util.ArrayList;
import java.util.Arrays;
import java.net.*;
import java.io.*;

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
            Thread host = new Thread(hostingPlayer);
            players.add(hostingPlayer);

            System.out.println("Hosting Player Connected. Opening lobby...");

            host.start();

            // Model the life cycle of the host program
            lobbyPhase();

            gameLoop();

            cleanUp();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public void lobbyPhase() throws IOException {
        // Spin until either the max number of players has been reached or the
        //  host tells the game to start
        while (this.players.size() < 5 && !hostingPlayer.reader.readLine().equals("start")) {
            Socket socket = publicListener.accept();
            PlayerHandler handler = new PlayerHandler(socket);
            Thread playerHandle = new Thread(handler);
            players.add(handler);
            playerHandle.start();
        }
    }

    public void gameLoop() throws IOException {
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
            for (int i = 0; i < numPlayers; i++) {
                PlayerHandler player = players.get(i);
                if (player.numPoints >= 500) break;
                scores += player.playerAlias + ": " + player.numPoints + ", ";
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
            String firstPlayer = String.valueOf(this.whoseTurn);
            String roundNumber = String.valueOf(this.roundNum);
            for (int i = 0; i < numPlayers; i++) {
                StatusMessage begin = new StatusMessage("BEGIN", players.get(i).playerAlias);
                begin.makeHeader();
                String startingCards = Arrays.toString(players.get(i).heldCards.toArray());
                String[] data = {firstPlayer, roundNumber, scores, startingCards};
                begin.makeBody(data);
                players.get(i).writer.println(begin.composeMessage());
            }

            // Enter round loop
            while (true) {
                // Check to see if any player is out of cards
                for (int i = 0; i < numPlayers; i++) {
                    if (players.get(i).heldCards.size() == 0) {
                        break;
                    }
                }

                // Wait for input from the current player
                String turn;
                while ((turn = players.get(whoseTurn).reader.readLine()).equals(""));

                // Look for the new move
                
                
            }

            // Round over
        }
    }

    public void cleanUp() {

    }
    
    /**
     * Inner class to handle player communication and maintain external threads
     */
    class PlayerHandler implements Runnable {
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

        public void readMsg() {

        }

        public void sendMsg(StatusMessage message) {
            writer.println(message.composeMessage());
        }

        
        public void run() {
            try {
                while(true) {
                    readMsg();
                }
            } catch (Exception e) {
                System.out.println("Error: " + e);
            }
        }
    }    
}
