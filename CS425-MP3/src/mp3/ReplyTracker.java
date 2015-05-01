package mp3;

import java.util.ArrayList;
import java.util.Arrays;

public class ReplyTracker {

	private static final boolean DEBUG = true;
	
	Message originalRequest;
	int replyLimit;
	ArrayList<Message> acks;
	ArrayList<Message> fails;

	public int yielded;
	
	public ReplyTracker(Message request, int K) {
		originalRequest = request;
		replyLimit = K;
		acks = new ArrayList<Message>();
		fails = new ArrayList<Message>();
		
		yielded = -1;
	}

	public boolean isSatisfied() {
		return acks.size()>=replyLimit;
	}

	public void add(Message msg) {
		if (msg.type==Message.Type.FAIL)
			fails.add(msg);
		else if(msg.type==Message.Type.REPLY) {
			if(yielded==msg.sourceID)
				yielded=-1;
			acks.add(msg);
		}
		else
			System.out.println("Tried to put non-reply or non-fail message to reply tracker! Ignored.");
	}
	
	public boolean findSourceID(int sourceID) {
		for (int i=0; i<acks.size(); ++i) {
			if (acks.get(i).sourceID==sourceID)
				return true;
		}
		return false;
	}
	
	public void removeSourceID(int sourceID) {
		for (int i=0; i<acks.size(); ++i) {
			if (acks.get(i).sourceID==sourceID) {
				acks.remove(i);
				if(DEBUG)
					System.out.println("revoked");
				i--;
			}
		}
	}

	public void print(int id) {
		String str = id+": [";
		for(Message reply : acks){
			str+=reply.type.name()+"("+reply.sourceID+"), ";
		}
		str+="]";
		System.out.println(str);
	}
	
	public String acksToStr() {
		String str = "[";
		for(Message reply : acks){
			str+=reply.sourceID+", ";
		}
		str+="]";
		return str;
	}
}
