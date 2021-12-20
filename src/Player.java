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
public class Player implements Runnable {

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
    private ArrayList<Card> buffer;
    private char gamePhase;
    private int roundNum;
    private int turn;
    private int numPoints;
    private boolean isHost;

    public Player(String ip, String name, boolean host) {
        // Set up the communicator with the given ip
        this.playerAlias = name;
        this.communicator = new Communicator(ip, name);
        this.isHost = host;
        if (this.communicator == null) {
            // Denied access to game
            System.out.println("Player creation failed");
            return;
        }
        this.otherPlayers = new ArrayList<OtherPlayer>();
        this.gamePhase = 'L';
        this.turn = 0;
        this.roundNum = 0;
        this.numPoints = 0;
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
        this.hand = new ArrayList<Card>();
        this.sets = new ArrayList<CardPile>();
        this.buffer = new ArrayList<Card>();
        Scanner in = new Scanner(System.in);
        while (this.gamePhase == 'L') {
            try {
                if (isHost) {
                    System.out.println("Start game? (y/n)");
                    String input = in.nextLine();
                    if (input.equalsIgnoreCase("y")) {
                        this.gamePhase = 'G';
                        // Write a start cue to the host
                        this.communicator.writer.println("start");
                    }
                }
                Thread.sleep(4000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        in.close();
    }

    private void gameLoop() {
        Scanner input = new Scanner(System.in);
        while (this.gamePhase == 'G') {
            // Player can't perform actions until designated by host
            if (this.turn != 0) {
                // Display all cards and open up actions
                System.out.println("Your hand: " + Arrays.toString(hand.toArray()));

                // Get action input
                System.out.println(playerAlias + ", What would you like to do?");
                System.out.println("(T)ake a card from the deck");
                System.out.println("(P)ick a card from the discard pile");
                String action = input.nextLine();

                while (!action.equals("T") && !action.equals("P")) {
                    System.out.println("Invalid action - try again:");
                    action = input.nextLine();
                }

                // Send a message for either the top card of the deck or for a
                //  set from the discard pile
                if (action.equals("T")) {
                    String[] data = {"T"};
                    this.communicator.sendMsg("TURN", data);
                } else if (action.equals("P")) {

                    // Need a position in the discard pile to take from
                    String discardPos = input.nextLine();
                    while (Integer.valueOf(discardPos) < 0) {
                        System.out.println("Need a positive number");
                        discardPos = input.nextLine();
                    }

                    String[] data = {"P", discardPos};
                    this.communicator.sendMsg("TURN", data);
                }

                // Check the buffer to be filled by the listener - once filled,
                //  can extact the cards and continue the turn
                while (buffer.size() == 0) {
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                // Add the cards from the buffer to the hand
                while (buffer.size() > 0) {
                    this.hand.add(buffer.remove(0));
                }

                // Allow player to continuously make melds until they discard
                Card discarding = null;
                while (!action.equals("D")) {
                    System.out.println("Your hand: " + Arrays.toString(hand.toArray()));
                    System.out.println("(S)et down a new meld");
                    System.out.println("(A)dd a card to an existing meld");
                    System.out.println("(D)iscard and end your turn");

                    action = input.nextLine();

                    while (!action.equals("S") && !action.equals("D") && !action.equals("A")) {
                        System.out.println("Invalid action - try again:");
                        action = input.nextLine();
                    }

                    if (action.equals("A")) {
                        // Adding a card from hand to an exiting meld
                        System.out.println("Adding a card to a meld. Input card in <Rank> of <Suit> format");
                        String meld = input.nextLine();
                        Card meldCard = findInHand(meld);
                        if (meldCard == null) {
                            System.out.println("Card not found in hand");
                        } else {
                            System.out.println("Which set do you want to add to?");
                            int numSets = sets.size();
                            for (int i = 0; i < numSets; i++) {
                                System.out.println(i + ": " + sets.get(i).toString());
                            }

                            // Get the set to be added to
                            String setNum = input.nextLine();
                            while (Integer.valueOf(setNum) < 0 || Integer.valueOf(setNum) > numSets) {
                                System.out.println("Invalid set number - try again");
                                setNum = input.nextLine();
                            }

                            // Add the cards from the set into the buffer
                            int whichSet = Integer.valueOf(setNum);
                            Card[] setCards = sets.get(whichSet).allCards();
                            for (int i = 0; i < setCards.length; i++) {
                                buffer.add(setCards[i]);
                            }
                            buffer.add(meldCard);

                            if (isValidMeld()) {
                                // Meld is valid - add card officially to the set
                                sets.get(whichSet).addCard(meldCard);
                                Card[] temp = {meldCard};
                                this.numPoints += Card.computePoints(temp);
                            } else {
                                System.out.println("Invalid meld");
                            }

                            buffer.clear();
                        }
                    } else if (action.equals("S")) {
                        // Setting down a set of cards - get the cards
                        System.out.println("Making a meld - input cards in `<Rank> of <Suit>` form");
                        System.out.println("Type `End` to continue");

                        String meldCard = input.nextLine();
                        while (!meldCard.equals("End")) {
                            // Use the input to search through hand for a match
                            Card melder = findInHand(meldCard);

                            if (melder == null) {
                                System.out.println("Card not found in hand");
                            } else {
                                System.out.println("Card found");
                                buffer.add(melder);
                            }

                            // Next card
                            meldCard = input.nextLine();
                        }

                        // Check to see if enough cards were obtained
                        if (buffer.size() < 3) {
                            System.out.println("Not enough cards given");
                            buffer.clear();
                            continue;
                        }

                        // Check to see if the meld is valid
                        if (isValidMeld()) {
                            // Meld is valid, make a new set and add the cards to it
                            CardPile meld = new CardPile('S');
                            this.numPoints += Card.computePoints((Card[]) buffer.toArray());
                            while (buffer.size() > 0) {
                                meld.addCard(buffer.remove(0));
                            }
                            sets.add(meld);
                        } else {
                            System.out.println("Meld is invalid");
                            buffer.clear();
                            continue;
                        }

                    } else if (action.equals("D")) {
                        // Discarding a card and ending turn
                        System.out.println("Discarding a card - Input card in <Rank> of <Suit> format");
                        String discard = input.nextLine();
                        discarding = findInHand(discard);
                        if (discarding == null) {
                            System.out.println("Card not found in hand");
                            action = "";
                        } else {
                            // Remove from hand and send back to host
                            hand.remove(discarding);
                        }
                    }
                }
                // Turn over - send remaining hand to the host as well as
                //  number of points player has now
                String handRemaining = "Hand: ";
                for (int i = 0; i < hand.size(); i++) {
                    handRemaining += hand.get(i).toString() + ", ";
                }
                String points = "Points: " + String.valueOf(numPoints);
                String[] data = {"D", handRemaining, points, discarding.toString()};
                this.communicator.sendMsg("TURN", data);

                // Sleep to allow the listener to end the turn
                try {
                    Thread.sleep(5000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        input.close();
    }

    /**
     * Helper method to find a card currently in the hand. Used when looking
     * for cards for melding based on player input
     * @param cardString A version of the toString() output for a card
     * @return A matching card for the string if in the hand; null if there are
     *  no matching cards
     */
    private Card findInHand(String cardString) {
        int handSize = this.hand.size();
        for (int i = 0; i < handSize; i++) {
            Card curCard = hand.get(i);
            if (curCard.toString().equals(cardString)) {
                // Make sure not already added
                if (buffer.contains(curCard)) {
                    System.out.println("Already inputted");
                    break;
                }
                return curCard;
            }
        }
        // Card not found
        return null;
    }

    /**
     * Helper method to detect whether or not a sequence of cards in the buffer
     * is valid as a meld. Either as a run of the same suit or as a matched set
     * @return True if the buffer contains a valid meld; False otherwise
     */
    private boolean isValidMeld() {
        int numCards = buffer.size();
        // Default type being looked for as a run
        char setType = 'R';
        if (buffer.get(0).rank == buffer.get(1).rank) {
            // Actually a matched set
            setType = 'M';
        }

        if (setType == 'R') {
            // Make sure that the cards are in sequential order and that the
            //  suit is always the same
            for (int i = 1; i < numCards; i++) {
                // Rank checking
                if (buffer.get(i - 1).rank != buffer.get(i).rank - 1) {
                    return false;
                }

                // Suit checking
                if (!buffer.get(i - 1).suit.equals(buffer.get(i).suit)) {
                    return false;
                }
            }
        } else {
            // Make sure the ranks are all the same
            for (int i = 1; i < numCards; i++) {
                if (buffer.get(i - 1).rank != buffer.get(i).rank) {
                    return false;
                }
            }
        }

        // No issues found
        return true;
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
        private Thread listen = new Thread(new Listener());
        private StatusMessage curMessage;

        public Communicator(String ip, String alias) {
            try {
                // Make a new connection to the specified server
                this.socket = new Socket(ip, 50100);
                InetAddress hostIp = socket.getInetAddress();
                System.out.println("Connection made to " + hostIp);

                // Start the I/O listeners
                this.writer = new PrintWriter(socket.getOutputStream(), true);
                this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                
                // Send a CONNECT message to get approval to join the lobby
                String[] data = {ip, "50100"};
                sendMsg("CONNECT", data);

                // Make sure lobby sends back an OK
                String ok = reader.readLine();
                while (ok == null || ok.equals("")) {
                    ok = reader.readLine();
                }
                String msgType = ok.split(": ")[1];
                if (!msgType.equals("OK")) {
                    System.out.println("Connection denied by host");
                    ok = reader.readLine();
                    ok = reader.readLine();
                    System.out.println(ok);
                    clearReader();

                    this.reader.close();
                    this.writer.close();
                    this.socket.close();
                    communicator = null;
                    listen = null;
                    return;
                }

                clearReader();

                listen.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * Helper class to simplify the message sending process
         * @param type The message type to be sent
         * @param data The data to be put into the body
         */
        public void sendMsg(String type, String[] data) {
            StatusMessage msg = new StatusMessage(type, playerAlias);
            this.curMessage = msg;
            msg.makeHeader();
            msg.makeBody(data);
            writer.println(msg.composeMessage());
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


        /**
         * Inner class that maintains a thread to listen for messages from the
         * host
         */
        class Listener implements Runnable {

            public void run() {
                while (true) {
                    try {
                        clearReader();

                        // Read message from the host
                        String hostMsg = reader.readLine();
                        while (hostMsg == null || hostMsg.equals("")) {
                            hostMsg = reader.readLine();
                        }

                        // Parse the message based on the message type
                        // Type is always in the first token
                        String msgType = hostMsg.split(": ")[1];

                        // Player doesn't care about sender (always host),
                        //  skip to the body
                        hostMsg = reader.readLine();
                        hostMsg = reader.readLine();

                        if (msgType.equals(StatusMessage.MESSAGE_TYPE[0])) {
                            // CONNECT - new player in lobby
                            hostMsg = reader.readLine();
                            String playerConnected = hostMsg.split(": ")[1];

                            otherPlayers.add(new OtherPlayer(playerConnected));
                            // Send back an OK
                            String[] data = {"CONNECT"};
                            sendMsg("OK", data);
                        } else if (msgType.equals(StatusMessage.MESSAGE_TYPE[2])) {
                            // MOVE - a player has made a move
                            hostMsg = reader.readLine();
                            String playerJustWent = hostMsg.split(": ")[1];
                            System.out.println(playerJustWent + " finished their turn:");

                            hostMsg = reader.readLine();
                            String pointAmount = hostMsg.split(": ")[1];
                            for (int i = 0; i < otherPlayers.size(); i++) {
                                OtherPlayer player = otherPlayers.get(i);
                                if (player.name.equals(playerJustWent)) {
                                    player.numPoints = Integer.valueOf(pointAmount);
                                    System.out.println(playerJustWent + "'s points: " + player.numPoints);
                                }
                            }
                            
                            // Output next player
                            hostMsg = reader.readLine();
                            String nextPlayer = hostMsg.split(": ")[1];
                            System.out.println("Next player: " + nextPlayer);
                            if (playerAlias.equals(nextPlayer)) {
                                turn = 1;
                            }

                            // Display what player discarded
                            hostMsg = reader.readLine();
                            System.out.println(hostMsg.split(": ")[1] + " was discarded");
                            
                            // Net discard pile displayed
                            hostMsg = reader.readLine();
                            System.out.println(hostMsg);

                            System.out.println();

                            // Send back an OK
                            String[] data = {"MOVE"};
                            sendMsg("OK", data);
                        } else if (msgType.equals(StatusMessage.MESSAGE_TYPE[3])) {
                            // BEGIN - starting round
                            
                            // Display first player
                            hostMsg = reader.readLine();
                            String[] firstPlayer = hostMsg.split(": ");
                            if (firstPlayer[1].equals(playerAlias)) {
                                System.out.println("You are going first");
                                turn = 1;
                            } else {
                                System.out.println(firstPlayer[1] + " is going first");
                            }
                            
                            // Save round number
                            hostMsg = reader.readLine();
                            String[] round = hostMsg.split(": ");
                            roundNum = Integer.valueOf(round[1]);
                            System.out.println("Round " + roundNum);

                            // Save scores
                            hostMsg = reader.readLine();
                            String[] scores = hostMsg.split(", ");
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
                            hostMsg = reader.readLine();
                            String[] handMsg = hostMsg.split(": ");
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
                            hostMsg = reader.readLine();
                            String[] winningPlayer = hostMsg.split(": ");
                            System.out.println("Winner: " + winningPlayer[1]);
                            
                            // Send back OK and then close connection
                            String[] data = {"END"};
                            sendMsg("OK", data);
                            
                            gamePhase = 'E';
                            clearReader();
                            reader.close();
                            writer.close();
                            socket.close();
                            return;
                        } else if (msgType.equals(StatusMessage.MESSAGE_TYPE[5])) {
                            // OK - msg received
                            
                            // If its a TURN being ACKed, should either fill 
                            //  the buffer with cards returned by the host or
                            //  end the turn if nothing was sent back
                            hostMsg = reader.readLine();
                            if (hostMsg.equals("TURN")) {
                                hostMsg = reader.readLine();
                                if (hostMsg.equals("")) {
                                    // Turn is over
                                    turn = 0;
                                    clearReader();
                                    continue;
                                }

                                // Need to convert the cards sent back from the
                                //  host into usable objects
                                String[] retCards = hostMsg.split(", ");
                                for (int i = 0; i < retCards.length; i++) {
                                    String[] card = retCards[i].split(" of ");
                                    buffer.add(new Card(card[1], card[0].charAt(0)));
                                }
                            }
                            
                        } else if (msgType.equals(StatusMessage.MESSAGE_TYPE[6])) {
                            // ERR - prev message failed
                            // Resend the last message
                            writer.println(curMessage.composeMessage());
                        }
                        clearReader();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
