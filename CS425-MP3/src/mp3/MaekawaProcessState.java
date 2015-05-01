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
	int voted;
	final int NONE = -1; 
	
	PState state;
	
	MaekawaProcess communication;
	protected int option;
	
	boolean flag = false;
	
	public MaekawaProcessState(MaekawaProcess p, int procID, long cs_int, long next_req, int option) {
		super();
		
		this.option = option;
		
		voted = NONE;
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
		nextPState();
		
		//Constantly check conditions for which we can proceed to the next PState
		while(true) {
			
			//communication.pollReq();
			
			switch(state) {
			case REQUEST:
				//If we receive a reply from everyone
				if(communication.replyTracker!=null && communication.replyTracker.isSatisfied()) {
					if (option!=-1)
						System.out.println(System.currentTimeMillis()+ " "+procID + " "+communication.replyTracker.repliesToStr());
					
					//Throw away the reply track since it is no longer needed
					//communication.replyTracker = null;
					nextPState();
				}
				else if (flag){
					String str = ""+communication.replyTracker.replyLimit;
				}
				break;
			case HELD:
				//Inside Critical Section
				
				//Hold PState for cs_int milliseconds
				if(holdHeldTime > System.currentTimeMillis()-startTime) {
					continue;
				}
				else
					nextPState();
				break;
			case RELEASE:
				//After performing exit code, hold this PState for next_req milliseconds
				if(holdHeldTime > System.currentTimeMillis()-startTime) {
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
			
			//Multicast request to everyone in my voting set
			Message msgReq = new Message(Message.Type.REQUEST, procID);
			communication.replyTracker=new ReplyTracker(msgReq, communication.votingSet.size());
			communication.multicast(msgReq);
			break;
		case HELD:
			//Inside Critical Section
			
			communication.yieldSet.clear();
			//Hold PState for cs_int milliseconds
			startTime = System.currentTimeMillis();
			flag=true;
			break;
		case RELEASE:
			//Perform Exit Code
			
			//Hold PState for next_req milliseconds
			startTime = System.currentTimeMillis();
			
			//Multicast release to all processes in my voting set
			Message msgRel = new Message(Message.Type.RELEASE, procID);
			communication.multicast(msgRel);
			break;
		default:
			log("hit an invalid PState!");
			throw new RuntimeException();
		}
		
	}
	
	public void resetVote() {
		voted = NONE;
	}
}
