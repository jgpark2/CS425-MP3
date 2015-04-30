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

//TODO: http://static.aminer.org/pdf/PDF/000/297/662/a_dynamic_information_structure_mutual_exclusion_algorithm_for_distributed_systems.pdf
//pg 292,293

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
	protected int CSSTAT;
	protected long CSTS;
	protected ReplyTracker replyTracker;
	//protected long holdTime;
	private MaekawaProcessState procState;
	
	protected ArrayList<Message> requestSet;
	
	//State state;
	
	protected Map<Integer, BlockingQueue<Message>> votingSet;
	
	public MaekawaProcess() {
		
	}
	
	/*
	 * MaekawaProcess constructor
	 * List of other MaekawaProcesses' queues are initialized from Main after
	 *  this constructor, but before launching the thread.
	 *  
	 * Start the process on "Initial" state.
	 */
	public MaekawaProcess(int N, int id, BlockingQueue<Message> queue, long cs_int, long next_req) {
		this.N = N;
		msgQueue = queue;
		Comparator<Message> comparator = new Comparator<Message>() {
	        public int compare(Message msg1, Message msg2) {
	        	if (msg1.timestamp==msg2.timestamp)
	        		msg1.timestamp+=1;
	            return ((Long)msg1.timestamp).compareTo((Long)msg2.timestamp);
	        }
		};
		requestsQueue = new PriorityQueue<Message>(N*10, comparator);
		procID = id;
		CSSTAT = -1;
		CSTS=0;
		
		requestSet = new ArrayList<Message>();
		/*holdTime = cs_int;
		
		if (DEBUG)
			log("Entered Init State.");
		state = State.INIT;*/
		
		replyTracker = null;
		
		procState = new MaekawaProcessState(this, procID, cs_int, next_req);
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
			try {
				msg = msgQueue.take();
			} catch (InterruptedException e) {
				log("Take Process Interrupted.");
				break;
			}
			
			log("Received "+msg.type.name()+" message from "+msg.sourceID);
			switch(msg.type){
			case REQUEST:
				requestsQueue.add(msg);
				
				//if(procState.state==procState.state.HELD || procState.voted) {
				if(CSSTAT!=-1) {
					//Message oldReq = findSourceID(CSSTAT);
					if (CSTS < msg.timestamp) {
						Message reply = new Message(Message.Type.FAIL, procID);
						sendMessage(reply, msg.sourceID);
					}
					else {
						Message reply = new Message(Message.Type.INQUIRE, procID);
						sendMessage(reply, CSSTAT);//TODO: unless one has been already sent
						sendFailMessages(msg.timestamp); //TODO: right location under if? right timestamp?
					}
				}
				else {
					//Crit Section is free
					
					//send reply
					procState.voted=true;
					
					Message msgReq = requestsQueue.remove();
					
					Message reply = new Message(Message.Type.REPLY, procID);
					sendMessage(reply, msgReq.sourceID);
					
					CSSTAT=msgReq.sourceID;//TODO: only if the id is in S_i...?
					CSTS=msgReq.timestamp;
				}
				break;
			case REPLY:
				replyTracker.add(msg);
				break;
			case RELEASE:
				CSSTAT=-1;
				CSTS=0;
				
				if(!requestsQueue.isEmpty()) {
					Message msgReq = requestsQueue.remove();
					Message reply = new Message(Message.Type.REPLY, procID);
					sendMessage(reply, msgReq.sourceID);
					procState.voted=true;
					CSSTAT=msgReq.sourceID;//TODO: only if the id is in S_i...?
					CSTS=msgReq.timestamp;
				}
				else
					procState.voted=false;
				break;
			case FAIL:
				requestSet.add(msg);
				break;
			case INQUIRE:
				for(int i=0; i<requestSet.size(); ++i) {
					if(msg.type==Message.Type.FAIL){
						//revoke grant
						replyTracker.removeSourceID(msg.sourceID);
						
						Message reply = new Message(Message.Type.YIELD, procID);
						sendMessage(reply, msg.sourceID);
					}
				}
				break;
			case YIELD:
				CSSTAT=-1;
				CSTS=0;
				//requestsQueue.add(); //TODO: the YIELDing process is returned to the 
				//priority queue in the appropriate location. 

				if(!requestsQueue.isEmpty()) {
					Message msgReq = requestsQueue.remove();
					Message reply = new Message(Message.Type.REPLY, procID);
					sendMessage(reply, msgReq.sourceID);
					procState.voted=true;
					CSSTAT=msgReq.sourceID;//TODO: only if the id is in S_i...?
					CSTS=msgReq.timestamp;
				}
				
				break;
			default:
				log("Message of unexpectedtype received");
				break;
			}
		}
		
	}

	private void sendFailMessages(long timestamp) {
		if(!requestsQueue.isEmpty()) {
			for(Message msg : requestsQueue)
				if (msg.timestamp>timestamp) {
					Message reply = new Message(Message.Type.FAIL, procID);
					sendMessage(reply, msg.sourceID);
				}
		}
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
		System.out.println(procID+": "+str);
	}
	
	protected Message findSourceID(int ID) {
		if(!requestsQueue.isEmpty()) {
			for(Message msg : requestsQueue)
				if (msg.sourceID==ID)
					return msg;
		}
		
		return null;
	}

}
