package mp3;


public class MaekawaProcessState extends MaekawaProcess{
	
	protected long holdTime;
	
	State state;
	
	public MaekawaProcessState(int procID, long cs_int) {
		super();
		
		this.procID =procID; 
		holdTime = cs_int;
		
		if (DEBUG)
			log("Entered Init State.");
		state = State.INIT;
	}

	/*
	 * Thread is started when connections with all other processes are established
	 *  (we have references to all their blocking queues).
	 * Init stage is finished, so we move on to the "Request" state where we attempt
	 * entry to the critical section.
	 */
	public void run() {
		nextState();
		
		while(true) {
			switch(state) {
			case REQUEST:
				//Executing Entry Code
				
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
	}
}
