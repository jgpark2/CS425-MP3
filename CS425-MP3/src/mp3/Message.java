package mp3;

public class Message {
	
	public enum Type {
		REQUEST, REPLY, RELEASE, FAIL, INQUIRE, YIELD;		
	}
	
	String msg;
	long timestamp;
	Type type;
	int sourceID;
	
	public Message(String msg) {
		this.msg = msg;
	}

	public Message(Type type, int procID) {
		this.timestamp = System.currentTimeMillis();
		this.type = type;
		sourceID = procID;
	}
	
	public Message(long timestamp, Type type, int procID) {
		this.timestamp = timestamp;
		this.type = type;
		sourceID = procID;
	}
}
