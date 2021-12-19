package src;

import java.util.*;
import java.net.*;
import java.io.*;

/**
 * Author: Samuel Fritz
 * CSCI 4431
 * 
 * The program to handle player actions and connecting to an existing host
 * lobby
 */
public class Player {

    class OtherPlayer {
        int numPoints;
        String name;

        public OtherPlayer(String name) {
            this.name = name;
            this.numPoints = 0;
            System.out.println("New player connected: " + this.name);
        }
    }
    
    private Communicator communicator;
    private ArrayList<Card> hand;
    private ArrayList<CardPile> sets;
    private String playerAlias;
    private ArrayList<OtherPlayer> otherPlayers;
    private ArrayList<CardPile> tempMoveState;
    private char gamePhase;
    private int roundNum;
    private int turn;

    public Player(String ip, String name) {
        // Set up the communicator with the given ip
        this.playerAlias = name;
        this.otherPlayers = new ArrayList<OtherPlayer>();
        this.communicator = new Communicator(ip, name);
        this.gamePhase = 'L';
        this.turn = 0;
        this.roundNum = 0;
    }

    public void run() {
        // Idle in lobby phase
        lobbyPhase();

        // Game loop
        gameLoop();

        // Cleaning up handled when END received
    }

    private void lobbyPhase() {
        // If this is the hosting player - allow them to start the game
        while (this.gamePhase == 'L') {
            try {
                Scanner in = new Scanner(System.in);
                if (this.communicator.socket.getInetAddress().equals(InetAddress.getLocalHost())) {
                    System.out.println("Start game? (y/n)");
                    String input = in.nextLine();
                    if (input.equalsIgnoreCase("y")) {
                        this.gamePhase = 'G';
                    }
                }
                in.close();
                Thread.sleep(4000);
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

    private void gameLoop() {
        while (this.gamePhase == 'G') {

        }
    }

    /**
     * Helper method to update the score of a player
     * @param name The name to find the player by
     * @param newScore The new score for the player
     */
    private void updatePlayer(String name, int newScore) {
        int numPlayers = otherPlayers.size();
        for (int i = 0; i < numPlayers; i++) {
            OtherPlayer other = otherPlayers.get(i);
            if (other.name.equals(name)) {
                other.numPoints = newScore;
                System.out.println(other.name + ": " + other.numPoints);
            }
        }
    }

    /**
     * Inner class handles communication with the host
     */
    class Communicator {
        private Socket socket;
        private PrintWriter writer;
        private BufferedReader reader;
        private Thread listen;
        private StatusMessage curMessage;

        public Communicator(String ip, String alias) {
            try {
                // Make a new connection to the specified server
                this.socket = new Socket(ip, 50100);
                InetAddress hostIp = socket.getInetAddress();
                System.out.println("Connection made to " + hostIp);

                // Start the I/O listeners
                this.writer = new PrintWriter(socket.getOutputStream());
                this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                listen = new Thread(new Listener());
                this.listen.start();

            } catch (Exception e) {
                System.out.println(e);
            }
        }

        public void sendMsg(String type, String[] data) {
            StatusMessage msg = new StatusMessage(type, playerAlias);
            this.curMessage = msg;
            msg.makeHeader();
            msg.makeBody(data);
            writer.println(msg.composeMessage());
        }


        /**
         * Inner class that maintains a thread to listen for messages from the
         * host
         */
        class Listener implements Runnable {

            public void run() {
                while (true) {
                    try {
                        // Read message from the host
                        String hostMsg = reader.readLine();

                        String[] msgPieces = hostMsg.split("\n");

                        // Parse the message based on the message type
                        // Type is always in the first token
                        String[] typeHeader = msgPieces[0].split(": ");
                        String msgType = typeHeader[1];

                        // Player doesn't care about sender (always host),
                        //  skip to third index

                        if (msgType.equals(StatusMessage.MESSAGE_TYPE[0])) {
                            // CONNECT - new player in lobby
                            String[] playerConnected = msgPieces[3].split(": ");

                            otherPlayers.add(new OtherPlayer(playerConnected[1]));
                            // Send back an OK
                            String[] data = {"CONNECT"};
                            sendMsg("OK", data);
                        } else if (msgType.equals(StatusMessage.MESSAGE_TYPE[2])) {
                            // MOVE - a player has made a move
                        } else if (msgType.equals(StatusMessage.MESSAGE_TYPE[3])) {
                            // BEGIN - starting round
                            
                            // Display first player
                            String[] firstPlayer = msgPieces[3].split(": ");
                            if (firstPlayer[1].equals(playerAlias)) {
                                System.out.println("You are going first");
                                turn = 1;
                            } else {
                                System.out.println(firstPlayer[1] + " is going first");
                            }
                            
                            // Save round number
                            String[] round = msgPieces[4].split(": ");
                            roundNum = Integer.valueOf(round[1]);

                            // Save scores
                            String[] scores = msgPieces[5].split(", ");
                            // First score needs additional logic, since it has
                            //  the data header on it
                            System.out.println("Current Points:\n--------------------------------");
                            String[] firstScore = scores[0].split(": ");
                            updatePlayer(firstScore[1], Integer.valueOf(firstScore[2]));
                            for (int i = 1; i < scores.length; i++) {
                                String[] scoreInfo = scores[i].split("");
                                if (scoreInfo[1].charAt(scoreInfo[1].length() - 1) == ',') {
                                    scoreInfo[1] = scoreInfo[1].substring(0, scoreInfo[1].length() - 1);
                                }
                                updatePlayer(scoreInfo[0], Integer.valueOf(scoreInfo[1]));
                            }

                            // Save the starting hand for the player
                            hand = new ArrayList<Card>();
                            String[] handMsg = msgPieces[6].split(": ");
                            String[] givenHand = handMsg[1].split(", ");
                            for (int i = 0; i < givenHand.length; i++) {
                                String[] card = givenHand[i].split(" of ");
                                hand.add(new Card(card[1], card[0].charAt(0)));
                            }
                            System.out.println("Starting hand:");
                            System.out.println(Arrays.toString(hand.toArray()));

                            // Send back an OK
                            String[] data = {"BEGIN"};
                            sendMsg("OK", data);
                        } else if (msgType.equals(StatusMessage.MESSAGE_TYPE[4])) {
                            // END - game over
                            // Display winner and end game
                            String[] winningPlayer = msgPieces[3].split(": ");
                            System.out.println("Winner: " + winningPlayer[1]);
                            
                            // Send back OK and then close connection
                            String[] data = {"END"};
                            sendMsg("OK", data);
                            
                            gamePhase = 'E';
                            reader.close();
                            writer.close();
                            socket.close();
                            return;
                        } else if (msgType.equals(StatusMessage.MESSAGE_TYPE[5])) {
                            // OK - msg received
                            // Check what the ACK type was for
                            String ackType = msgPieces[3];

                            // TURN - move was valid, save the state
                            if (ackType.equals("TURN")) {
                                int moveStateSize = tempMoveState.size();
                                for (int i = 0; i < moveStateSize; i++) {
                                    
                                }
                            }

                            // All others types aren't needed by player side

                        } else if (msgType.equals(StatusMessage.MESSAGE_TYPE[6])) {
                            // ERR - prev message failed
                            // Resend the last message
                            writer.println(curMessage.composeMessage());
                        }
                    } catch (Exception e) {
                        System.out.println(e);
                    }
                }
            }
        }
    }
}
