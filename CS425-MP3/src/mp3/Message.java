package mp3;

public class Message {
	
	public enum Type {
		REQUEST;		
	}
	
	String msg;
	Type type;
	int sourceID;
	
	public Message(String msg) {
		this.msg = msg;
	}

	public Message(Type type, int procID) {
		this.type = type;
		sourceID = procID;
	}
}
