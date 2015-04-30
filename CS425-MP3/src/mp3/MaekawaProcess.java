package mp3;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;

/*
 * A class representing a single process for the Maekawa Algorithm (specifically, this is a
 *  thread in this implementation. Other Maekawa Algorithms may refer to these instances as
 *  nodes or other processes).
 */
public class MaekawaProcess extends Thread{
	
	final boolean DEBUG = true;
	
	public enum State {
		INIT, REQUEST, HELD, RELEASE, UNK;
		
		public static State next(State s){
			switch(s) {
			case INIT: return REQUEST;
			case REQUEST: return HELD;
			case HELD: return RELEASE;
			case RELEASE: return REQUEST;
			}
			return UNK;
		}
	}
	
	//This is where all output from "show" commands goes
	protected BufferedWriter out;
	
	protected int N;
	protected BlockingQueue<Message> msgQueue;
	protected int procID;
	//protected long holdTime;
	private MaekawaProcessState procState;
	
	//State state;
	
	protected ArrayList<BlockingQueue<Message>> procQueues;
	
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
		procID = id;
		/*holdTime = cs_int;
		
		if (DEBUG)
			log("Entered Init State.");
		state = State.INIT;*/
		
		procState = new MaekawaProcessState(procID, cs_int);
	}
	
	
	/*
	 * Thread is started when connections with all other processes are established
	 *  (we have references to all their blocking queues).
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
				log("Process Interrupted.");
				break;
			}
			
		}
		
	}

	public void populateQueueReferences(
			ArrayList<BlockingQueue<Message>> procQueues) {
		this.procQueues = procQueues;		
	}
	
	protected void log(String str) {
		System.out.println(procID+": "+str);
	}

}
