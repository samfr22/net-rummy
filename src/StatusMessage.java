package src;
/**
 * Class to represent all of the possible status messages that can be sent
 */

public class StatusMessage {
    public static final String[] MESSAGE_TYPE = {"CONNECT", "TURN", "MOVE", "BEGIN", "END", "OK", "ERR"};

    private String msgType;
    private String sender;
    private String body;
    private String header;

    public StatusMessage(String type, String sender) {
        this.msgType = type;
        this.sender = sender;
    }

    public String composeMessage() {
        // Can't get the net message if body or head hasn't been created
        if (this.header == null || this.body == null) return null;

        return this.header + "\n\n" + this.body + "\n\n";
    }

    public void makeHeader() {
        this.header = "Type: " + this.msgType + "\nSender: " + this.sender;
    }

    public void makeBody(String[] data) {
        if (this.msgType.compareTo(MESSAGE_TYPE[0]) == 0) {
            // CONNECT
            // Need a src ip and port
            if (data.length != 2) return;

            this.body = "IP Address: " + data[0] + "\nPort Num: " + data[1]; 
        } else if (this.msgType.compareTo(MESSAGE_TYPE[1]) == 0) {
            // TURN
            // Need a prev state, new state, and the number of cards in hand
            if (data.length != 3) return;

            this.body = "Previous: " + data[0] + "\nNew: " + data[1] + "\nCards Remaining: " + data[2];
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
            if (data.length != 1) return;

            this.body = data[0];
        }
    }
}
