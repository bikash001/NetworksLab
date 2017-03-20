import java.util.ArrayList;
import java.util.Random;
import java.lang.Math;

public class Main {
	public static void main(String[] args) {
		int users = 0;
		int windowSize = 2;
		double pktGenRate = 0;
		int maxPkts = 0;
		for (int i=0; i<args.length; i++) {
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
        Aloha aloha = new Aloha(users,maxPkts,windowSize,pktGenRate);
        aloha.start();
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
	private boolean resend;
	public User(int id, double rate, int w){
		counter = 0;
		resend = false;
		userId = id;
		pkts = 0;
		pktGenRate = rate;
		gen = new Random();
		wsize = w;
		retransmit = 0;
		totalPktSent = 0;
		totalSlotUsed = 0;
	}
	public int getId() {
		return userId;
	}
	public boolean sentUnsuccess() {
		if (pkts <= 2 && pkts > 0) {
			totalSlotUsed += pkts;
			if (counter == 0) {
				counter = gen.nextInt(wsize);
				wsize = (int)(Math.min(265, wsize*2));
				// System.out.println("wsizeInc "+wsize+", cnt= "+counter);
				if (retransmit > 100) {
					return true;
				}
			}
		}
		return false;
	}
	public void sentSuccess() {
		wsize = (int)(Math.max(2,wsize * 0.75));
		// System.out.println("wsizeDec "+wsize);
		retransmit = 0;
		resend = false;
		totalSlotUsed += pkts;
		pkts--;
		totalPktSent++;
	}
	public boolean sendPkt() {
		if (counter > 0) {
			counter--;
		}
		if (counter == 0) {
			if (resend) {
				retransmit++;
			}
			if (pkts <= 2 && pkts > 0) {
				resend = true;
				return true;
			} else {
				return false;
			}
		}
		return false;
	}
	public User genPkt() {
		if (gen.nextDouble() <= pktGenRate && pkts < 2) {
			pkts++;
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
		// System.out.printf("%d, %d\n", totalPktSent, slotCount);
		double avg = (double)totalSlotUsed / totalPktSent;
		double thpt = (double)totalPktSent / slotCount;
		System.out.printf("%d %d %f %f %f\n", totalUser, windowSize, pktGenRate, thpt, avg);
	}

	public void start() {
		users = new ArrayList<User>(totalUser);
		for (int i=0; i<totalUser; i++) {
			users.add(new User(i,pktGenRate,windowSize));
		}
		int totalTransmits=0;
		int userNo = 0;

		int counter=0;
		int simTime;
		
		for (simTime=0; simTime < maxPkts; slotCount++) {
			counter++;
			totalTransmits = 0;

			//send packets
			for (int i=0; i<totalUser; i++) {
				if (users.get(i).genPkt().sendPkt()) {
					totalTransmits++;
					userNo = i;
				}
			}

			if (totalTransmits == 1) {
				users.get(userNo).sentSuccess();
				simTime++;
				for (int i=0; i<totalUser; i++) {
					if (i != userNo) {
						if (users.get(i).sentUnsuccess()) {
							stats();
							return;
						}
					}	
				}
			} else {
				for (int i=0; i<totalUser; i++) {
					if (users.get(i).sentUnsuccess()) {
						stats();
						return;
					}
				}
			}
		}
		stats();
	}
}