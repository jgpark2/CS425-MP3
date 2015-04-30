package mp3;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

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
	protected BlockingQueue<Message> requestsQueue;
	protected int procID;
	//protected long holdTime;
	private MaekawaProcessState procState;
	
	//State state;
	
	protected ArrayList<BlockingQueue<Message>> votingSet;
	
	public MaekawaProcess() {
		
	}
	
	/*
	 * MaekawaProcess constructor
	 * List of other MaekawaProcesses' queues are initialized from Main after
	 *  this constructor, but before launching the thread.
	 *  
	 * Start the process on "Initial" state.
	 */
	public MaekawaProcess(int N, int id, BlockingQueue<Message> queue, long cs_int) {
		this.N = N;
		msgQueue = queue;
		requestsQueue = new ArrayBlockingQueue<Message>(N*10);
		procID = id;
		/*holdTime = cs_int;
		
		if (DEBUG)
			log("Entered Init State.");
		state = State.INIT;*/
		
		procState = new MaekawaProcessState(this, procID, cs_int);
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
			
			switch(msg.type){
			case REQUEST:
				log("Received REQUEST message from "+msg.sourceID);
				break;
			default:
				log("Message of unexpectedtype received");
				break;
			}
		}
		
	}

	public void multicast(Message message) {
		//Multicast (broadcast) to everyone in my voting set
		for(BlockingQueue<Message> process : votingSet) {
			
			if (process==msgQueue) //No need to multicast to myself
				continue;
			
			try {
				process.put(message);
			} catch (InterruptedException e) {
				log("Put Process Interrupted.");
				break;
			}
		}
			
	}
	
	public void populateVotingSet(
			ArrayList<BlockingQueue<Message>> procQueues) {
		this.votingSet = procQueues;
	}
	
	protected void log(String str) {
		System.out.println(procID+": "+str);
	}

}
