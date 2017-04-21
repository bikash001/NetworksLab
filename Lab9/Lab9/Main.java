import java.util.Random;
import java.lang.Math;
import java.io.PrintWriter;

public class Main {
	public static void main(String[] args) {
		Simulation sm = new Simulation(args);
		sm.simulate();
    }
}

class Simulation {
	private int rws = 1024;	//receiver window size 1mb
	private int mss = 1;	//sender's mss 1kb
	private double cw;	//congestion window
	private double init_multiplier;
	private double cw_thld = 512;	//congestion threshold, default 50% of receiver window size
	private double exp_multiplier = 1;	//exponential growth multiplier
	private double lin_multiplier = 1;	//linear growth multiplier
	private double to_multiplier;	//timeout multiplier
	private double prob;	//probability of receiving ACK before timeout
	private int total_segment;
	private String file;
	private Random rgen;

	public Simulation(String[] args) {
		for (int i=0; i<args.length; i++) {
        	if (args[i].equals("-i")) {
        		init_multiplier = Double.valueOf(args[++i]);
        	} else if (args[i].equals("-m")) {
        		exp_multiplier = Double.valueOf(args[++i]);
        	} else if (args[i].equals("-n")) {
        		lin_multiplier = Double.valueOf(args[++i]);
        	} else if (args[i].equals("-f")) {
        		to_multiplier = Double.valueOf(args[++i]);
        	} else if (args[i].equals("-s")) {
        		prob = Double.valueOf(args[++i]);
        	} else if (args[i].equals("-T")) {
        		total_segment = Integer.valueOf(args[++i]);
        	} else if (args[i].equals("-o")) {
        		file = args[++i];
        	} else {
        		System.out.println("Usage: [-i double] [-m double] [-n double] [-f double] [-s double] [-T int] [-o file_name]");
        		System.exit(0);
        	}
        }
        rgen = new Random();
        cw = (int)(init_multiplier * mss);
	}

	public void simulate() {
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(file);
		} catch (Exception e) {
			System.out.println(e);
			System.exit(0);
		}
		int counter = 0;
		int seg_sent;
		int success = 0;
		while (counter < total_segment) {
			seg_sent = (int)Math.ceil(cw/mss);
			success = 0;
			for (int j=0; j<seg_sent; j++) {
				if (rgen.nextDouble() > prob) {
					if (cw < cw_thld) {
						cw = Math.min(cw+exp_multiplier*mss, rws);
					} else {
						cw = Math.min(cw+lin_multiplier*mss*mss/cw, rws);
					}
					success++;
				} else {
					cw_thld = cw / 2;
					cw = Math.max(1, to_multiplier*cw);
					break;
				}
			}
			pw.println(cw);
			counter += success;
		}
		pw.close();
	}
}