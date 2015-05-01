package mp3;

import java.util.ArrayList;
import java.util.Arrays;

public class ReplyTracker {

	Message originalRequest;
	int replyLimit;
	ArrayList<Message> replies;
	ArrayList<Message> fails;
	
	public ReplyTracker(Message request, int K) {
		originalRequest = request;
		replyLimit = K;
		replies = new ArrayList<Message>();
		fails = new ArrayList<Message>();
	}

	public boolean isSatisfied() {
		return replies.size()>=replyLimit;
	}

	public void add(Message msg) {
		if (msg.type==Message.Type.FAIL)
			fails.add(msg);
		else 
			replies.add(msg);
	}
	
	public boolean findSourceID(int sourceID) {
		for (int i=0; i<replies.size(); ++i) {
			if (replies.get(i).sourceID==sourceID)
				return true;
		}
		return false;
	}
	
	public void removeSourceID(int sourceID) {
		for (int i=0; i<replies.size(); ++i) {
			if (replies.get(i).sourceID==sourceID) {
				replies.remove(i);
				i--;
			}
		}
	}

	public void print(int id) {
		String str = id+": [";
		for(Message reply : replies){
			str+=reply.type.name()+"("+reply.sourceID+"), ";
		}
		str+="]";
		System.out.println(str);
	}
	
	public String repliesToStr() {
		String str = "[";
		for(Message reply : replies){
			str+=reply.sourceID+", ";
		}
		str+="]";
		return str;
	}
}
