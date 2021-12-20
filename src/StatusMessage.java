package src;

/**
 * Author: Samuel Fritz
 * CSCI 4431
 * 
 * Class to represent all of the possible status messages that can be sent
 */
public class StatusMessage {
    // All possible message types, in Strings for easier comparison
    public static final String[] MESSAGE_TYPE = {"CONNECT", "TURN", "MOVE", "BEGIN", "END", "OK", "ERR"};

    private String msgType;
    private String sender;
    private String body;
    private String header;

    /**
     * Constructor
     * @param type The type of status message being made
     * @param sender The soon-to-be sender of the message
     */
    public StatusMessage(String type, String sender) {
        this.msgType = type;
        this.sender = sender;
        this.header = null;
        this.body = null;
    }

    /**
     * Returns the entire message composed with a previously created
     * header and body. If either has not been initialized, nothing is created
     * @return The composite message containing the header and body formatted
     */
    public String composeMessage() {
        // Can't get the net message if body or head hasn't been created
        if (this.header == null || this.body == null) return null;

        return this.header + "\n\n" + this.body + "\n\n";
    }

    /**
     * Initializes the header of the message
     */
    public void makeHeader() {
        this.header = "Type: " + this.msgType + "\nSender: " + this.sender;
    }

    /**
     * Initializes the body of the message. If the data argument for a message
     * type is too short, the message fails to be made. Based on the type of
     * message, the body is created with different fields
     * @param data The data to be contained in the message body
     */
    public void makeBody(String[] data) {
        if (this.msgType.compareTo(MESSAGE_TYPE[0]) == 0) {
            // CONNECT
            // Need a src ip and port
            if (data.length != 2) return;

            this.body = "IP Address: " + data[0] + "\nPort Num: " + data[1]; 
        } else if (this.msgType.compareTo(MESSAGE_TYPE[1]) == 0) {
            // TURN
            // Need a move identifier and any information needed for the turn
            if (data.length < 1) return;

            for (int i = 0; i < data.length; i++) {
                this.body += data[i] + "\n";
            }
        } else if (this.msgType.compareTo(MESSAGE_TYPE[2]) == 0) {
            // MOVE
            // Need the player that went and their move and the next player to go
            if (data.length != 3) return;

            this.body = "Player: " + data[0] + "\nMove: " + data[1] + "\nNext Player: " + data[2];
        } else if (this.msgType.compareTo(MESSAGE_TYPE[3]) == 0) {
            // BEGIN
            // Need a first player, round number, scores, and starting hand
            if (data.length != 4) return;

            this.body = "First Player: " + data[0] + "\nRound: " + data[1] + "\nScores: " + data[2] + "\nStarting Hand: " + data[3];
        } else if (this.msgType.compareTo(MESSAGE_TYPE[4]) == 0) {
            // END
            // Need a winning player
            if (data.length != 1) return;

            this.body = "Winner: " + data[0];
        } else if (this.msgType.compareTo(MESSAGE_TYPE[5]) == 0) {
            // OK
            // Need a message type being acknowledged
            if (data.length != 1) return;
            // If the type being acknowledged is TURN, need a card confirmation
            if (data[0].equals("TURN") && data.length != 2) return;

            if (data[0].equals("TURN")) {
                this.body = data[0] + "\n" + data[1];
            } else {
                this.body = data[0];
            }
        } else if (this.msgType.compareTo(MESSAGE_TYPE[6]) == 0) {
            // ERR
            // Need a message type being erred
            if (data.length < 1) return;

            this.body = data[0];
        }
    }
}
