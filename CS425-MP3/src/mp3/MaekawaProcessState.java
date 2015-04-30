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
	
	long holdTime;
	boolean voted = false;
	
	State state;
	
	MaekawaProcess communication;
	
	public MaekawaProcessState(MaekawaProcess p, int procID, long cs_int) {
		super();
		
		communication = p;
		
		this.procID =procID; 
		holdTime = cs_int;
		
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
				//TODO:When some condition is met
				if(false)
					nextState();
				break;
			case HELD:
				//Inside Critical Section
				
				//Hold state for cs_int milliseconds
				long startTime = System.currentTimeMillis();
				while(holdTime > System.currentTimeMillis()-startTime) {
					
				}
				nextState();
				break;
			case RELEASE:
				//Perform Exit Code
				
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
			communication.multicast(new Message(Message.Type.REQUEST, procID));
			break;
		case HELD:
			//Inside Critical Section
			
			//Hold state for cs_int milliseconds
			long startTime = System.currentTimeMillis();
			while(holdTime > System.currentTimeMillis()-startTime) {
				
			}
			nextState();
			break;
		case RELEASE:
			//Perform Exit Code
			nextState();
			break;
		default:
			log("hit an invalid state!");
			throw new RuntimeException();
		}
		
	}
}
