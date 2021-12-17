import java.util.*;
import java.net.*;
import java.io.*;

public class Player {

    class OtherPlayer {
        int numPoints;
        String name;

        public OtherPlayer(String name) {
            this.name = name;
            this.numPoints = 0;
        }
    }
    
    protected Communicator communicator;
    protected ArrayList<Card> hand;
    protected ArrayList<CardPile> sets;
    protected String playerAlias;
    protected ArrayList<OtherPlayer> otherPlayers;
    protected ArrayList<CardPile> tempMoveState;

    public Player(String ip, String name) {
        // Set up the communicator with the given ip
        this.playerAlias = name;
        this.otherPlayers = new ArrayList<OtherPlayer>();
        this.communicator = new Communicator(this, ip, name);
    }

    public void run() {
        // Idle in lobby phase
        lobbyPhase();

        // Game loop
        gameLoop();

        // Cleaning up
        cleanUp();
    }

    void lobbyPhase() {

    }

    void gameLoop() {

    }

    void cleanUp() {

    }

    /**
     * Inner class handles communication with the host
     */
    class Communicator {
        private Socket socket;
        private PrintWriter writer;
        private BufferedReader reader;
        private Thread listen;

        public Communicator(Player player, String ip, String alias) {
            try {
                // Make a new connection to the specified server
                this.socket = new Socket(ip, 50100);
                InetAddress hostIp = socket.getInetAddress();
                System.out.println("Connection made to " + hostIp);

                // Start the I/O listeners
                this.writer = new PrintWriter(socket.getOutputStream());
                this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                listen = new Thread(new Listener(player));
                this.listen.start();

            } catch (Exception e) {
                System.out.println(e);
            }
        }

        public void sendMsg(String type, String[] data) {
            StatusMessage msg = new StatusMessage(type, playerAlias);
            msg.makeHeader();
            msg.makeBody(data);
            writer.println(msg.composeMessage());
        }


        /**
         * Inner class that maintains a thread to listen for messages from the
         * host
         */
        class Listener implements Runnable {

            private Player player;

            public Listener(Player player) {
                this.player = player;
            }

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
                        }
                    } catch (Exception e) {
                        System.out.println(e);
                    }
                }
            }
        }
    }

    /**
     * Runner of the entire program
     */
    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);

        System.out.println("Welcome to Networked Rummy\n");
        System.out.println("The rules are simple:");
        System.out.println("\tBe the first to run out of cards in your hand by putting down sets");
        System.out.println("\tThese sets can be made by making runs/straights of the same suit or by making sets of the same rank");
        System.out.println("\tAll sets must be at least three cards but can be added to later");
        System.out.println("\tPoints are given based on the value of the cards put down in sets");
        System.out.println("\tPoints are lost based on the value of cards still in your hand when the round ends");
        System.out.println("\tThe first player to reach 500 points wins\n\n");

        while (true) {
            System.out.println("What would you like to do?");
            System.out.println("1) Join a lobby");
            System.out.println("2) Host a game");
            System.out.println("3) Exit");
            
            String gameChoice = input.nextLine();
            while (!gameChoice.equals("1") && !gameChoice.equals("2") && !gameChoice.equals("3")) {
                System.out.print("Invalid choice - try again: ");
                gameChoice = input.nextLine();
            }

            if (gameChoice.equals("1")) {
                // Joining another player lobby - get ip and connect
                System.out.print("Enter host IP to connect to: ");
                String host = input.nextLine();

                while (host == null || host.equals("")) {
                    System.out.print("Invalid input - try again: ");
                    host = input.nextLine();
                }

                System.out.print("Enter name: ");
                String alias = input.nextLine();

                while (alias == null || alias.equals("")) {
                    System.out.print("Invalid input - try again: ");
                    alias = input.nextLine();
                }

                System.out.println("Connecting to host...");
                Player player = new Player(host, alias);

                // Let the game run until the host decides to end
                player.run();

            } else if (gameChoice.equals("2")) {
                // Start the host in the background
                System.out.println("Starting host...");
                Thread host = new Thread(new Host());
                host.run();

                // Then connect from the client-facing player program
                System.out.print("Enter name: ");
                String alias = input.nextLine();

                while (alias == null || alias.equals("")) {
                    System.out.print("Invalid input - try again: ");
                    alias = input.nextLine();
                }

                System.out.println("Connecting to host...");
                Player player = new Player("127.0.0.1", alias);

                player.run();
            } else {
                break;
            }
        }

        input.close();
    }
}
