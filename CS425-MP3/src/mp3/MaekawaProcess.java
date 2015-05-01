package mp3;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import mp3.MaekawaProcessState.PState;

/*
 * A class representing a single process for the Maekawa Algorithm (specifically, this is a
 *  thread in this implementation. Other Maekawa Algorithms may refer to these instances as
 *  nodes or other processes).
 */
public class MaekawaProcess extends Thread{
	
	final boolean DEBUG = true;
	
	//This is where all output from "show" commands goes
	protected BufferedWriter out;
	
	protected int N;
	protected BlockingQueue<Message> msgQueue;
	protected PriorityQueue<Message> requestsQueue;
	protected int procID;
	protected int criticalSectionID;
	protected ReplyTracker replyTracker;
	//protected long holdTime;
	private MaekawaProcessState procState;
	
	protected ArrayList<Message> yieldSet;
	
	protected long timeStamp;
	protected int CSSTAT;
	protected long CSTS;
	
	//State state;
	
	protected Map<Integer, BlockingQueue<Message>> votingSet;

	private int option;
	
	public MaekawaProcess() {
		
	}
	
	/*
	 * MaekawaProcess constructor
	 * List of other MaekawaProcesses' queues are initialized from Main after
	 *  this constructor, but before launching the thread.
	 *  
	 * Start the process on "Initial" state.
	 */
	public MaekawaProcess(int N, int id, BlockingQueue<Message> queue, long cs_int, long next_req, int option) {
		this.N = N;
		this.option = option;
		msgQueue = queue;
		Comparator<Message> comparator = new Comparator<Message>() {
	        public int compare(Message msg1, Message msg2) {
	        	//if (msg1.timestamp==msg2.timestamp)
	        	//	msg1.timestamp+=1;
	            return ((Long)msg1.timestamp).compareTo((Long)msg2.timestamp);
	        }
		};
		requestsQueue = new PriorityQueue<Message>(N*10, comparator);
		procID = id;
		criticalSectionID = -1;
		timeStamp = 0;
		
		yieldSet= new ArrayList<Message>();
		
		replyTracker = null;
		
		procState = new MaekawaProcessState(this, procID, cs_int, next_req, option);
		resetLock();
	}
	
	
	/*
	 * Thread is started when connections with all other processes of my voting set
	 *  are established (we have references to all their blocking queues).
	 * Start our state manager.
	 * This thread simply fetches new in-bound messages from its queue now.
	 */
	public void run() {
		procState.start();
		
		while(true){
			Message msg;
			
			//Take a Message from my queue
			/*if(msgQueue.isEmpty()) {
				if(!requestsQueue.isEmpty()) {
					Message msgReq = requestsQueue.remove();
					msg = msgReq;
				}
				else continue;
			}
			else {*/
				try {
					msg = msgQueue.take();
				} catch (InterruptedException e) {
					log("Take Process Interrupted.");
					break;
				}
				
				if(option==1){
					System.out.println(System.currentTimeMillis()+" "+procID+ " "+msg.sourceID+ " "+msg.type.name()+" ");
				}
				
				
				if(msg.type.name()=="REPLY") {
					log("Received "+msg.type.name()+" message from "+msg.sourceID+ " "+replyTracker.repliesToStr());
				}
				else
					log("Received "+msg.type.name()+" message from "+msg.sourceID);
			//}
			
			switch(msg.type){
			case REQUEST:
				if(procState.voted || procState.state==PState.HELD) {
					if (CSTS < msg.timestamp) {
						//if(CSSTAT==procID)
						//	break;
						Message reply = new Message(Message.Type.INQUIRE, procID, msg);
						sendMessage(reply, CSSTAT);
						//queue request comes later after yielding
					} else {
						requestsQueue.add(msg);
						//if(CSSTAT==procID)
						//	break;
						
						Message reply = new Message(Message.Type.FAIL, procID);
						sendMessage(reply, msg.sourceID);
						
					}					
				}
				else {
					//send reply
					procState.voted=true;
					
					Message reply = new Message(Message.Type.REPLY, procID);
					CSSTAT=reply.sourceID;
					CSTS=reply.timestamp;
					
					sendMessage(reply, msg.sourceID);
				}
				break;
			case REPLY:
				replyTracker.add(msg);
				//replyTracker.print(procID);
				break;
			case RELEASE:
				log("RELEASEEEE");
				resetLock();
				pollReq();
				break;
				
			case FAIL:
				replyTracker.add(msg);
				break;
			case INQUIRE:
				if(replyTracker==null){
					//most likely in HELD state already
					break;
				}
				if(replyTracker.fails.size()>0) {
					//revoke the old grant i got from him and send yield
					replyTracker.removeSourceID(msg.sourceID);
					Message reply = new Message(Message.Type.YIELD, procID, msg.submsg);
					sendMessage(reply, msg.sourceID);
					yieldSet.add(msg);
				}
				else if (yieldSet.size()>0){
					for(int i=0; i<yieldSet.size(); i++) {
						if (replyTracker.findSourceID(yieldSet.get(i).sourceID)) {
							break;
						}
					}
					//revoke the old grant i got from him and send yield
					replyTracker.removeSourceID(msg.sourceID);
					Message reply = new Message(Message.Type.YIELD, procID, msg.submsg);
					sendMessage(reply, msg.sourceID);
					yieldSet.add(msg);
				}
				break;
			case YIELD:
				
				resetLock();
				
				msg.submsg.timestamp=System.currentTimeMillis();//TODO:?
				requestsQueue.add(msg.submsg); 
				
				pollReq();
				
				break;
				
			default:
				log("Message of unexpectedtype received");
				break;
			}
		}
		
	}

	private void resetLock() {
		procState.resetVote();
		CSSTAT=-1;
		CSTS=Long.MAX_VALUE;
	}

	private void sendMessage(Message reply, int destinationID) {
		try {
			votingSet.get(destinationID).put(reply);
		} catch (InterruptedException e) {
			log("Reply Interrupted.");
		}
	}

	public void multicast(Message message) {
		//Multicast (broadcast) to everyone in my voting set
		for(Map.Entry<Integer, BlockingQueue<Message>> process : votingSet.entrySet()) {
			
			//if (process.getValue()==msgQueue) //No need to multicast to myself
			//	continue;
			
			try {
				process.getValue().put(message);
			} catch (InterruptedException e) {
				log("Multicast Put Interrupted.");
				break;
			}
		}
			
	}
	
	public void populateVotingSet(
			Map<Integer, BlockingQueue<Message>> procQueues) {
		this.votingSet = procQueues;
	}
	
	protected void log(String str) {
		//if(option==-1)
			System.out.println(procID+": "+str);
	}

	protected void pollReq() {
		if(!requestsQueue.isEmpty()) {
			//msgQueue.add(requestsQueue.remove());

			Message msgReq = requestsQueue.remove();
			if (msgReq.type==Message.Type.REQUEST){
				procState.voted=true;
				CSSTAT=msgReq.sourceID;
				CSTS=msgReq.timestamp;
				
				Message reply = new Message(Message.Type.REPLY, procID);
				sendMessage(reply, msgReq.sourceID);
			}
			else
				log("Nonrequest message in requestsQueue :(");
		}
	}
}
