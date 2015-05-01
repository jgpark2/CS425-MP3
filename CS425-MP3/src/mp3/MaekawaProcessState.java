package mp3;

public class MaekawaProcessState extends MaekawaProcess{
	
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
	
	long holdHeldTime;
	long holdReleaseTime;
	long startTime;
	
	Message voted;
	boolean sentInquiry = false;
	
	PState state;
	
	MaekawaProcess communication;
	protected int option;
	
	boolean flag = false;
	
	
	public MaekawaProcessState(MaekawaProcess p, int procID, long cs_int, long next_req, int option) {
		super();
		
		this.option = option;
		
		voted = null;
		communication = p;
		
		this.procID =procID; 
		holdHeldTime = cs_int;
		holdReleaseTime = next_req;
		
		if (DEBUG)
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
			
			//communication.pollReq();
			
			switch(state) {
			case REQUEST:
				//Continue Entry Code
				
				//If we receive a reply from everyone
				if(communication.replyTracker!=null && communication.replyTracker.isSatisfied()) {
					if (option!=-1)
						System.out.println(System.currentTimeMillis()+ " "+procID + " "+communication.replyTracker.acksToStr());
					
					//Throw away the reply track since it is no longer needed
//TODO:					communication.replyTracker = null;
					nextPState();
				}
				else if (flag){
					String str = ""+communication.replyTracker.replyLimit;
				}
				break;
				
			case HELD:
				//Inside Critical Section
				
				//Hold this state for cs_int milliseconds
				if(holdHeldTime > System.currentTimeMillis()-startTime) {
					continue;
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
		
		if(DEBUG)
			log("**********State change to: "+state.name()+"************");
		
		switch(state) {
		case REQUEST:
			//Execute Entry Code
			communication.entry();
			break;
			
		case HELD:
			//Now inside the Critical Section
			communication.onEntry();
			
			communication.yieldSet.clear();
			
			//Time marker needed to hold HELD state for specified cs_int milliseconds
			startTime = System.currentTimeMillis();
			
			flag=true;
			break;
			
		case RELEASE:
			//Perform Exit Code
			communication.exitCS();
			
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
		
		if(oldVote != voted) {//TODO:?
			sentInquiry = false;
			communication.failsSent.clear();
		}
		
	}
	
	public void resetVote() {
		voted = null;
		sentInquiry = false;
		communication.failsSent.clear();
	}
}
