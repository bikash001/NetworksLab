import java.util.ArrayList;
import java.util.Random;
import java.lang.Math;

public class Main {
	public static void main(String[] args) {
		int users = 0;
		int windowSize = 2;
		double pktGenRate = 0;
		int maxPkts = 0;
		for (int i=0; i<len; i++) {
        	if (args[i].equals("-W")) {
        		windowSize = Integer.valueOf(args[++i]);
        	} else if (args[i].equals("-p")) {
        		pktGenRate = Double.valueOf(args[++i]);
        	} else if (args[i].equals("-N")) {
        		users = Integer.valueOf(args[++i]);
        	} else if (args[i].equals("-M")) {
        		maxPkts = Integer.valueOf(args[++i]);
        	} else {
        		System.out.println("Usage: [-p double] [-N integer] [-W integer] [-M integer]");
        	}
        }
	}
}

class User {
	private int counter;
	private int userId;
	private int wsize;
	private double pktGenRate;
	private Random gen;
	private int pkts;
	private int retransmit;
	private int totalPktSent;
	private int totalSlotUsed;
	public User(int id, double rate, int w){
		counter = 0;
		userId = id;
		pkts = 0;
		pktGenRate = rate;
		gen = new Random();
		wsize = w;
		retransmit = 0;
		totalPktSent = 0;
		totalSlotUsed = 0;
	}
	public int get() {
		return counter;
	}
	public User set(int k) {
		counter = k;
		return this;
	}
	public User inc() {
		counter++;
		return this;
	}
	public User dec() {
		counter--;
		return this;
	}
	public boolean zero() {
		return counter == 0;
	}
	public int getId() {
		return userId;
	}
	public boolean sentUnsuccess() {
		if (pkts < 2) {
			wsize = (int)(Math.min(265, wsize*2));
			counter = gen.nextInt(wsize);
			retransmit++;
			totalSlotUsed++;
			if (retransmit > 10) {
				return true;
			}
		}
		return false;
	}
	public void sentSuccess() {
		pkts--;
		wsize = (int)(Math.max(2,wsize * 0.75));
		retransmit = 0;
		totalPktSent++;
	}
	public boolean sendPkt() {
		if (pkts < 2 && counter == 0) {
			totalSlotUsed++;
			return true;
		} else {
			return false;
		}
	}
	public User genPkt() {
		if (gen.nextDouble() <= pktGenRate) {
			if (pkts < 2){
				pkts++;
			}
		}
		return this;
	}
	public int pktSent() {
		return totalPktSent;
	}
	public int slotUsed() {
		return totalSlotUsed;
	}
}

class Aloha {
	private int totalUser, windowSize, maxPkts;
	private Double pktGenRate;
	private int totalSlotUsed;
	private int totalPktSent;
	private int slotCount;
	private ArrayList<User> users = null;
	public Aloha(int n, int m, int w, Double p) {
		totalUser = n;
		windowSize = w;
		maxPkts = m;
		slotCount = 0;
		pktGenRate = p;
		totalPktSent = 0;
		totalSlotUsed = 0;
	}

	private void stats() {
		for (int i=0; i<totalUser; i++) {
			totalPktSent += users.get(i).pktSent();
			totalSlotUsed += users.get(i).slotUsed();
		}
		double avg = (double)totalSlotUsed / totalPktSent;
		double thpt = (double)totalPktSent / slotCount;
		System.out.printf("%d, %d, %f, %f, %f\n", totalUser, windowSize, pktGenRate, thpt, avg);
	}

	public void start() {
		users = new ArrayList<User>(totalUser);
		for (int i=0; i<totalUser) {
			users.add(new User(i,pktGenRate,windowSize));
		}
		int totalTransmits=0;
		int userNo = 0;

		for (int simTime=0; simTime < maxPkts; slotCount++) {
			totalTransmits = 0;
			for (int i=0; i<totalUser; i++) {
				if (users.get(i).genPkt().sendPkt()) {
					totalTransmits++;
					userNo = i;
				}
			}

			if (totalTransmits == 1) {
				users(userNo).sentSuccess();
				simTime++;
				for (int i=0; i<totalUser; i++) {
					if (i!=userNo && users.get(i).sentUnsuccess()) {
						stats();
					}	
				}
			} else {
				for (int i=0; i<totalUser; i++) {
					if (users.get(i).sentUnsuccess()) {
						stats();
					}
				}
			}
		}
	}
}