package mp3;

import java.util.ArrayList;

public class ReplyTracker {

	Message originalRequest;
	int replyLimit;
	ArrayList<Message> replies;
	
	public ReplyTracker(Message request, int K) {
		originalRequest = request;
		replyLimit = K;
		replies = new ArrayList<Message>();
	}

	public boolean isSatisfied() {
		return replies.size()>=replyLimit;
	}

	public void add(Message msg) {
		replies.add(msg);
	}
}
