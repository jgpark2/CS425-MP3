package mp3;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

/*
 * ./Main <cs_int>	<next_req>	<tot_exec_time> <option>
 * 
 * 
 * 
 * The Optimal Voting Set minimizing K (Hard coded)
 * 
 * Since N = 9, K ~ sqrt(9)=3, where K is the size of a voting set.
 * (Each process should have a voting set of the same size).
 * 
 * Also, M = K ~ 3, where M is the number of voting sets a process belongs to.
 * 
 * To calculate the optimal set, we look at a sqrt(N) x sqrt(N) matrix
 * -------------
 * | 0 | 1 | 2 |
 * -------------
 * | 3 | 4 | 5 |
 * -------------
 * | 6 | 7 | 8 |
 * -------------
 * 
 * As an approximation (~ 2*sqrt(N)) voting set V_i will be the union of the row
 *  and column containing p_i (the processor of id i).
 * So, the 0th processor will have a voting set of: 0,1,2,3,6
 * 
 * 
 */
public class Main {
	
	static ArrayList<MaekawaProcess> processes;
	static ArrayList<LinkedBlockingQueue<Message>> procQueues;
	
    public static void main(String[] args) {
		
		if (args.length != 4) {
			System.out.println("Usage: ./Main <cs_int (ms)> <next_req (ms)> <tot_exec_time (s)> <option (0/1)> ");
			System.exit(1);
		}
		
		int N = 9;
		long cs_int = Long.parseLong(args[0]);
		long next_req = Long.parseLong(args[1]);
		int tot_exec_time = Integer.parseInt(args[2]);
		int option = Integer.parseInt(args[3]);
		
		processes = new ArrayList<MaekawaProcess> ();
		procQueues = new ArrayList<LinkedBlockingQueue<Message>> ();
		
		//Create each processes' communication channels
		for(int i=0; i<N; ++i) {
			LinkedBlockingQueue<Message> queue = new LinkedBlockingQueue<Message>(N*10);
			procQueues.add(queue);
		}
		
		//Create the N=9 processes
		for(int pid=0; pid<N; ++pid) {
			MaekawaProcess proc = new MaekawaProcess(N, pid, procQueues.get(pid), cs_int, next_req, option);
			processes.add(proc);
			proc.populateVotingSet(calcVotingSet(procQueues, N ,pid));
		}
		
		//Start the N processes in the "Initial" state as given in the MP
		for(int pid=0; pid<N; ++pid) {
			processes.get(pid).start();
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		long startTime = System.currentTimeMillis();
		while((int)((System.currentTimeMillis()-startTime)/1000) < tot_exec_time) {
			
		}
		
		System.out.println("Total execution time is up!");
		System.exit(0);
		return;
    }

	private static Map<Integer,LinkedBlockingQueue<Message>> calcVotingSet(
			ArrayList<LinkedBlockingQueue<Message>> allQueues, int N, int i) {
		
		Map<Integer, LinkedBlockingQueue<Message>> votingSet = new HashMap<Integer, LinkedBlockingQueue<Message>> ();
		ArrayList<Integer> vSetInd = new ArrayList<Integer>();
		
		int rN = (int) Math.sqrt(N);
		
		//Add rows
		int rowNum = i/rN;
		for(int j = 0; j<rN; ++j) {
			int value = rowNum*rN + j;
			vSetInd.add(value);
		}
		
		//Add columns
		int colNum = i % rN;
		for(int j = 0; j<rN; ++j) {
			int value = colNum+rN*j;
			if (value == i) //Avoid adding my own index a second time
				continue;
			vSetInd.add(value);
		}
		
		System.out.print("Voting set for i="+i+": [");
		for(int j=0; j<vSetInd.size(); ++j) {
			int pid = vSetInd.get(j);
			votingSet.put(pid, allQueues.get(pid));
			System.out.print(vSetInd.get(j)+",");
		}
		System.out.println("]");
		
		return votingSet;
	}

}
