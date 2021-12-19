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
        }
    }
    
    private Communicator communicator;
    private ArrayList<Card> hand;
    private ArrayList<CardPile> sets;
    private String playerAlias;
    private ArrayList<OtherPlayer> otherPlayers;
    private ArrayList<CardPile> tempMoveState;
    private char gamePhase;

    public Player(String ip, String name) {
        // Set up the communicator with the given ip
        this.playerAlias = name;
        this.otherPlayers = new ArrayList<OtherPlayer>();
        this.communicator = new Communicator(ip, name);
        this.gamePhase = 'L';
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

                        // Player doesn't care about sender; skip to fourth tok

                        if (msgType.equals(StatusMessage.MESSAGE_TYPE[0])) {
                            // CONNECT - new player in lobby
                            String[] playerConnected = msgPieces[4].split(": ");

                            otherPlayers.add(new OtherPlayer(playerConnected[1]));
                            // Send back an OK
                            String[] data = {"CONNECT"};
                            sendMsg("OK", data);
                        } else if (msgType.equals(StatusMessage.MESSAGE_TYPE[2])) {
                            // MOVE - player has made a move
                        } else if (msgType.equals(StatusMessage.MESSAGE_TYPE[3])) {
                            // BEGIN - starting round
                        } else if (msgType.equals(StatusMessage.MESSAGE_TYPE[4])) {
                            // END - game over
                            // Display winner and end game
                            String[] winningPlayer = msgPieces[4].split(": ");
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
                            String ackType = msgPieces[4];

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
