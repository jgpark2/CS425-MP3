package mp3;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/*
 * ./Main <cs_int>	<next_req>	<tot_exec_time> <option>
 */
public class Main {
	
	static ArrayList<MaekawaProcess> processes;
	static ArrayList<BlockingQueue<Message>> procQueues;
	
    public static void main(String[] args) {
		
		if (args.length != 4) {
			System.out.println("Usage: ./Main <cs_int> <next_req> <tot_exec_time> <option> ");
			System.exit(1);
		}
		
		int N = 9;
		int cs_int = Integer.parseInt(args[0]);
		int next_req = Integer.parseInt(args[1]);
		int tot_exec_time = Integer.parseInt(args[2]);
		int option = Integer.parseInt(args[3]);
		
		processes = new ArrayList<MaekawaProcess> ();
		procQueues = new ArrayList<BlockingQueue<Message>> ();
		
		//Create each processes' communication channels
		for(int i=0; i<N; ++i) {
			BlockingQueue<Message> queue = new ArrayBlockingQueue<Message>(N*10);
			procQueues.add(queue);
		}
		
		//Create the N=99 processes
		for(int pid=0; pid<N; ++pid) {
			MaekawaProcess proc = new MaekawaProcess(N, pid, procQueues.get(pid), cs_int);
			processes.add(proc);
			proc.populateQueueReferences(procQueues);
		}
		
		//Start the N processes in the "Initial" state as given in the MP
		for(int pid=0; pid<N; ++pid) {
			processes.get(pid).start();
		}
       
    }
}
