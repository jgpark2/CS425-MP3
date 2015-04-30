package mp3;

public class MaekawaProcessState extends MaekawaProcess{
	
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
	
	long holdHeldTime;
	long holdReleaseTime;
	long startTime;
	boolean voted;
	
	State state;
	
	MaekawaProcess communication;
	
	public MaekawaProcessState(MaekawaProcess p, int procID, long cs_int, long next_req) {
		super();
		
		voted = false;
		communication = p;
		
		this.procID =procID; 
		holdHeldTime = cs_int;
		holdReleaseTime = next_req;
		
		if (DEBUG)
			log("Entered Init State.");
		state = State.INIT;
	}

	/*
	 * Thread is started when connections with all other processes of my voting set
	 *  are established (we have references to all their blocking queues).
	 * Init stage is finished, so we move on to the "Request" state where we attempt
	 * entry to the critical section.
	 */
	public void run() {
		nextState();
		
		//Constantly check conditions for which we can proceed to the next state
		while(true) {
			switch(state) {
			case REQUEST:
				//If we receive a reply from everyone
				if(communication.CSSTAT==procID) {
					//Throw away the reply track since it is no longer needed
					communication.replyTracker = null;
					nextState();
				}
				else if(communication.replyTracker!=null && communication.replyTracker.isSatisfied()) {
					//Throw away the reply track since it is no longer needed
					communication.replyTracker = null;
					nextState();
				}
				break;
			case HELD:
				//Inside Critical Section
				
				//Hold state for cs_int milliseconds
				if(holdHeldTime > System.currentTimeMillis()-startTime) {
					continue;
				}
				else
					nextState();
				break;
			case RELEASE:
				//After performing exit code, hold this state for next_req milliseconds
				if(holdHeldTime > System.currentTimeMillis()-startTime) {
					continue;
				}
				nextState();
				break;
			default:
				log("hit an invalid state!");
				throw new RuntimeException();
			}
		}
	}
	
	protected void nextState() {
		state = State.next(state);
		
		if(DEBUG)
			log("State change to: "+state.name());
		
		switch(state) {
		case REQUEST:
			//Execute Entry Code
			
			//Multicast request to everyone in my voting set
			Message msgReq = new Message(System.currentTimeMillis(), Message.Type.REQUEST, procID);
			communication.replyTracker=new ReplyTracker(msgReq, communication.votingSet.size());
			communication.multicast(msgReq);
			break;
		case HELD:
			//Inside Critical Section
			
			//Hold state for cs_int milliseconds
			startTime = System.currentTimeMillis();
			break;
		case RELEASE:
			//Perform Exit Code
			
			//Hold state for next_req milliseconds
			startTime = System.currentTimeMillis();
			
			//Multicast release to all processes in my voting set
			Message msgRel = new Message(System.currentTimeMillis(), Message.Type.RELEASE, procID);
			communication.multicast(msgRel);
			break;
		default:
			log("hit an invalid state!");
			throw new RuntimeException();
		}
		
	}
}
