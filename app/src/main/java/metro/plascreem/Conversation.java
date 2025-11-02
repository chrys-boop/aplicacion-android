package metro.plascreem;

public class Conversation {
    private String otherUserId;
    private String otherUserName;
    private String lastMessage;
    private long timestamp;

    // Constructor vacío requerido para Firebase/Jackson
    public Conversation() {}

    public Conversation(String otherUserId, String otherUserName, String lastMessage, long timestamp) {
        this.otherUserId = otherUserId;
        this.otherUserName = otherUserName;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
    }

    // Getters
    public String getOtherUserId() {
        return otherUserId;
    }

    public String getOtherUserName() {
        return otherUserName;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public long getTimestamp() {
        return timestamp;
    }

    // Setters
    public void setOtherUserId(String otherUserId) {
        this.otherUserId = otherUserId;
    }

    public void setOtherUserName(String otherUserName) {
        this.otherUserName = otherUserName;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
