package mp3;

public class StateManager extends Thread{
	
	public enum  PState {
		INIT, REQUEST, HELD, RELEASE, UNK;
		
		public static PState next(PState s){
			switch(s) {
			case INIT: return REQUEST;
			case REQUEST: return HELD;
			case HELD: return RELEASE;
			case RELEASE: return REQUEST;
			}
			return UNK;
		}
	}
	
	long startTime;
	
	long holdHeldTime;
	long holdReleaseTime;
	protected int option;
	
	Message voted;
	boolean sentInquiry = false;
	
	PState state;
	
	MaekawaProcess parent;
	
	public StateManager(MaekawaProcess parent, long cs_int, long next_req, int option) {
		this.parent = parent;
		
		holdHeldTime = cs_int;
		holdReleaseTime = next_req;
		
		this.option = option;
		
		voted = null;
		
		if (parent.DEBUG)
			log("********Entered Init State.********");
		state = PState.INIT;
	}

	/*
	 * Thread is started when connections with all other processes of my voting set
	 *  are established (we have references to all their blocking queues).
	 * Init stage is finished, so we move on to the "Request" PState where we attempt
	 * entry to the critical section.
	 */
	public void run() {
		nextPState(); //Get out of Init state
		
		//Constantly check conditions for which we can proceed to the next state
		while(true) {
			
			
			switch(state) {
			case REQUEST:
				//Continue Entry Code
				
				System.out.print("");
					
				//If we receive a reply from everyone
				if(parent.replyTracker!=null && parent.replyTracker.isSatisfied()) {
					
					//Throw away the reply track since it is no longer needed
					//parent.replyTracker = null; //TODO: NO, keep it, unless u get rid of revoke condition for HELD state
					nextPState();
				}
				break;
				
			case HELD:
				//Inside Critical Section
				
				//Hold this state for cs_int milliseconds
				if(holdHeldTime > System.currentTimeMillis()-startTime) {
					break;
				}
				
				nextPState();
				
				break;
				
			case RELEASE:
				//Outside of Critical Section
				
				//Banned from Requesting again for a while
				//hold this state for next_req milliseconds
				if(holdReleaseTime > System.currentTimeMillis()-startTime) {
					continue;
				}
				
				nextPState();
				
				break;
			default:
				log("hit an invalid State!");
				throw new RuntimeException();
			}
		}
	}
	
	protected void nextPState() {
		state = PState.next(state);
		
		if(parent.DEBUG)
			log("**********State change to: "+state.name()+"************");
		
		switch(state) {
		case REQUEST:
			//Execute Entry Code
			parent.entry();
			break;
			
		case HELD:
			//Now inside the Critical Section
			parent.onEntry();
			
			parent.yieldSet.clear();
			
			//Time marker needed to hold HELD state for specified cs_int milliseconds
			startTime = System.currentTimeMillis();
			
			break;
			
		case RELEASE:
			//Perform Exit Code
			parent.exitCS();
			
			//Time marker needed to hold RELEASE state for next_req milliseconds before we can request again
			startTime = System.currentTimeMillis();
			break;
			
		default:
			log("hit an invalid state!");
			throw new RuntimeException();
		}
		
	}
	
	public void castVote(Message msg) {
		
		
		Message oldVote = voted;
		voted = msg;
		
		//if(oldVote != voted) {
			sentInquiry = false;
			//TODO:? parent.failsSent.clear();
		//}
		
	}
	
	public void resetVote() {
		voted = null;
		sentInquiry = false;
		//TODO:? parent.failsSent.clear();
	}
	
	protected void log(String str) {
		if(option==-1)
			System.out.println(parent.procID+": "+str);
	}
}
