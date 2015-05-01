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
	
	private MaekawaProcessState procState;
	
	protected ArrayList<Message> failsSent;
	protected ArrayList<Message> yieldSet;
	
	protected long timestamp;
	//protected int CSSTAT;
	//protected long CSTS;
	
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
	            return ((Long)msg1.timestamp).compareTo((Long)msg2.timestamp);
	        }
		};
		requestsQueue = new PriorityQueue<Message>(N*10, comparator);
		procID = id;
		criticalSectionID = -1;
		timestamp = 0;
		
		yieldSet= new ArrayList<Message>();
		failsSent = new ArrayList<Message>();
		
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
			
			//Check for any in-bound messages
			try {
				msg = msgQueue.take();
			} catch (InterruptedException e) {
				log("Take Process Interrupted.");
				break;
			}
			
			if(option==1){
				System.out.println(System.currentTimeMillis()+" "+procID+ " "+msg.sourceID+ " "+msg.type.name()+" ");
			}
			
			
/*			if(msg.type.name()=="REPLY") {
				log("Received "+msg.type.name()+" message from "+msg.sourceID+ " "+replyTracker.acksToStr());
			}
			else*/
				log("Received "+msg.type.name()+" message from "+msg.sourceID);
			
			//Catch up timestamp if needed
			timestamp = Math.max(timestamp,msg.timestamp);
			
			switch(msg.type){
			
			case REQUEST:
				requestsQueue.add(msg);
				
				if( yetToVote() ) {
log("Received "+msg.type.name()+" message from "+msg.sourceID+ " ...replying!");
					grantTopRequest();
				}
				else {
					//msg = requestsQueue.remove();//REVERT TS
					Message criticalSectionMsg = procState.voted;
					if(criticalSectionMsg.timestamp < msg.timestamp) { //TODO: < or <= ?
log("Received "+msg.type.name()+" message from "+msg.sourceID+ " ...failing!");
						//Reply with FAIL
						failsSent.add(msg);
						sendMessage(Message.Type.FAIL, msg.sourceID);
					}
					else {
log("Received "+msg.type.name()+" message from "+msg.sourceID+ " ...inquiring!");
						if(!procState.sentInquiry) {
							sendMessage(Message.Type.INQUIRE, criticalSectionMsg.sourceID);
							procState.sentInquiry = true;
						}
					}
					//send FAIL to anything larger than msg.timestamp in priority queue
					failAll(msg.timestamp);
				}
				break;
				
			case REPLY:
				log("Received "+msg.type.name()+" message from "+msg.sourceID+ " "+replyTracker.acksToStr());
				replyTracker.add(msg);
				//replyTracker.print(procID);
				break;
				
			case RELEASE:
				log("RELEASEEEE");
				resetLock();
				while(!requestsQueue.isEmpty() && yetToVote()) {
					grantTopRequest();
				}
				break;
			
			case FAIL:
				replyTracker.add(msg);
				
				break;
				
			case INQUIRE:
				if(revokeCondition()) {
/*if (replyTracker.fails.size()>0)
	log("Received "+msg.type.name()+" message from "+msg.sourceID+ " ...yielding :( fails");
if(replyTracker.yielded!=-1) 
	log("Received "+msg.type.name()+" message from "+msg.sourceID+ " ...yielding :( already yielded");
*/
					replyTracker.removeSourceID(msg.sourceID);
					replyTracker.yielded=msg.sourceID;
					sendMessage(Message.Type.YIELD, msg.sourceID);
				}
/*
if(procState.state==PState.HELD)
	log("Received "+msg.type.name()+" message from "+msg.sourceID+ " ...IGNORED! i'm in CS :D");
else
	log("Received "+msg.type.name()+" message from "+msg.sourceID+ " ...IGNORED! no reason!?");
*/
				break;
				
			case YIELD:
				Message oldRequest = procState.voted;
				resetLock();
				requestsQueue.add(oldRequest);
				while(!requestsQueue.isEmpty() && yetToVote()) {
					grantTopRequest();
				}
				break;
				
			default:
				log("Message of unexpectedtype received");
				break;
			}
		}
		
	}

	private void failAll(long minimumTS) {
		for(Message request : requestsQueue) {
			if (request.timestamp > minimumTS)
				if(failsSent.contains(request))
					continue;
				else {
					failsSent.add(request);
					sendMessage(Message.Type.FAIL, request.sourceID);//TODO: do we keep them in the priq? yes i think
				}
				
		}
	}

	private void grantTopRequest() {
		if(!requestsQueue.isEmpty()) {
			Message msgReq = requestsQueue.remove();
			
			timestamp = Math.max(timestamp,msgReq.timestamp); //Probably unecessary
			
			if (msgReq.type==Message.Type.REQUEST){
				if(yetToVote()) {
					procState.castVote(msgReq);
					sendMessage(Message.Type.REPLY, msgReq.sourceID);
				}
				else{
					requestsQueue.add(msgReq);
				}
			}
			else
				log("Nonrequest message in requestsQueue :(");
		}
	}

	/*
	 * The Critical Section entry code
	 */
	protected void entry() {
		//Multicast my ENTER request to everyone in my voting set
		++timestamp;
		Message myRequest = new Message(timestamp, Message.Type.REQUEST, procID);
		
		replyTracker = new ReplyTracker(myRequest, votingSet.size());
		
		multicast(myRequest);		
	}
	
	/*
	 * Critical Section code
	 */
	public void onEntry() {
		//Do whatever is needed here.
		
	}
	
	/*
	 * The Critical Section exit code
	 */
	protected void exitCS() {
		//Multicast release to all processes in my voting set
		multicastMessage(Message.Type.RELEASE);
	}
	
	private void resetLock() {
		procState.resetVote();
	}

	private Message sendMessage(Message.Type type, int destinationID) {
		++timestamp;
		 Message reply = new Message(timestamp, type, procID);
		try {
			votingSet.get(destinationID).put(reply);
		} catch (InterruptedException e) {
			log("Reply Interrupted.");
		}
		
		return reply;
	}

	private void multicast(Message message) {
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
	
	public Message multicastMessage(Message.Type type) {
		++timestamp;
		Message message = new Message(timestamp, type, procID);
		
		multicast(message);
		
		return message;
	}
	
	private boolean revokeCondition() {
		if(procState.state==PState.HELD)
			return false;
		
		if (replyTracker.fails.size()>0)
			return true;
		
		if(replyTracker.yielded!=-1) 
			return true;
		
		return false;
	}
	
	private boolean yetToVote() {
		return (procState.voted==null);
	}
	
	public void populateVotingSet(
			Map<Integer, BlockingQueue<Message>> procQueues) {
		this.votingSet = procQueues;
	}
	
	protected void log(String str) {
		//if(option==-1)
			System.out.println(procID+": "+str);
	}

	
}
