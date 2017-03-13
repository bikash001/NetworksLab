import java.net.*;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.Thread;
import java.util.TimerTask;
import java.util.Random;
import java.util.Timer;
import java.util.HashMap;
import java.lang.System;
import java.util.Iterator;
import java.lang.Math;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.ReentrantLock;

public class Sender {
	public static  int toAck = 0;
	public static int pktNo = 0;
	public static ConcurrentHashMap<Integer,Timeout> timerList;
	public static ConcurrentHashMap<Integer,Integer> resends;
	public static volatile double rtt = 0;
	public static long totalAcked = 0;
	public static int maxPkts = 100;
	public static DatagramSocket ds = null;
	public static Timer timer = null;
	public static int totalTrans = 0;
	public static int pktGenRate = 1;
	public static int pktLen = 100;
	public static boolean debug = false;
	public static long startTime;
	public static Recv receiver = null;
	private static final ReentrantLock lock = new ReentrantLock();
	private static final ReentrantLock timerlock = new ReentrantLock();
	    
  	public static void main(String[] args) {
	    int port = 1080;
	    int maxBuffSize = 100;
	    int wsize = 10;
	    InetAddress ip = null;
	    timerList = new ConcurrentHashMap<Integer,Timeout>();
	    try {
		    for (int i=0; i<args.length; ++i) {
		    	if (args[i].equals("-d")) {
		    		debug = true;
		    		// System.out.println("debug Mode");
		    	} else if (args[i].equals("-s")) {
		    		ip = InetAddress.getByName(args[++i]);
		    	} else if (args[i].equals("-p")) {
		    		port = Integer.valueOf(args[++i]);
		    	} else if (args[i].equals("-l")) {
		    		pktLen = Integer.valueOf(args[++i]);
		    	} else if (args[i].equals("-r")) {
		    		pktGenRate = Integer.valueOf(args[++i]);
		    	} else if (args[i].equals("-n")) {
		    		maxPkts = Integer.valueOf(args[++i]);
		    	} else if (args[i].equals("-w")) {
		    		wsize = Integer.valueOf(args[++i]);
		    	} else if (args[i].equals("-b")) {
		    		maxBuffSize = Integer.valueOf(args[++i]);
		    	}
		    }
		} catch (Exception e) {
			// e.printStackTrace();
		}
		// HashMap<Integer,byte[]> queBuffer = new HashMap<Integer,byte[]>();
		ConcurrentHashMap<Integer,byte[]> queBuffer = new ConcurrentHashMap<Integer,byte[]>();
	    timer = new Timer();
	    resends = new ConcurrentHashMap<Integer,Integer>();
	    PacketGen generator = new PacketGen(queBuffer, pktLen, maxBuffSize);
	    Timer genTimer = new Timer();
	    genTimer.schedule(generator,0,1000/pktGenRate);
	   	
	    try {
			ds = new DatagramSocket();
			startTime = System.currentTimeMillis();
			byte[] buff = null;
			receiver = new Recv(ds,timerList,resends,queBuffer);
			receiver.start();
	    	
	    	for (; pktNo < 10; ) {
	    		getLock();
	        	if (queBuffer.containsKey(pktNo) && (pktNo-toAck) < wsize) {
	        		buff = queBuffer.get(pktNo);
		        	DatagramPacket dpSend = new DatagramPacket(buff,buff.length,ip,port);
		        	ds.send(dpSend);
		        	Timeout tout = new Timeout(timerList, pktNo, resends,dpSend);
		        	timerList.put(pktNo,tout);
		        	timer.schedule(tout, 100);
		        	resends.put(pktNo, 0);
		        	if (pktNo == Integer.MAX_VALUE) {
		        		pktNo = 0;
		        	} else {
		        		pktNo++;
		        	}
		        	totalTrans++;
		        }
		        releaseLock();
			}
			while (totalAcked < maxPkts) {
				getLock();
				if (queBuffer.containsKey(pktNo) && pktNo-toAck < wsize) {
					buff = queBuffer.get(pktNo);
		        	DatagramPacket dpSend = new DatagramPacket(buff,buff.length,ip,port);
		        	ds.send(dpSend);
		        	Timeout tout = new Timeout(timerList, pktNo, resends, dpSend);
		        	timerList.put(pktNo,tout);
		        	timer.schedule(tout, Math.max((long)(2 * rtt),2));
		        	resends.put(pktNo,0);
		        	if (pktNo == Integer.MAX_VALUE) {
		        		pktNo = 0;
		        	} else {
		        		pktNo++;
		        	}
		        	totalTrans++;
				} else {
					// System.out.printf("length %d\n",queBuffer.size());
				}
				releaseLock();
			}
	      
	    } catch(Exception e){
	    	// e.printStackTrace();
	    }
	}

	public static void getTimerLock() {
		timerlock.lock();
	}

	public static void releaseTimerLock() {
		timerlock.unlock();
	}

	public static void getLock() {
		lock.lock();
	}

	public static void releaseLock() {
		lock.unlock();
	}

	public static void finalStatus() {
		timer.cancel();
		receiver.stop();
		System.out.printf("Packet Generation Rate: %d\n", pktGenRate);
		System.out.printf("Packet Length: %d\n", pktLen);
		System.out.printf("Retransmission Ratio: %f\n", (float)totalTrans/totalAcked);
		System.out.printf("Average RTT: %f ms\n", rtt);
	}
}

class Recv extends Thread {
	private DatagramSocket ds;
	private ConcurrentHashMap<Integer,Timeout> tasks;
	private ConcurrentHashMap<Integer,Integer> resends;
	private ConcurrentHashMap<Integer,byte[]> queBuffer;
	// private HashMap<Integer,byte[]> queBuffer;
	public Recv(DatagramSocket soc, ConcurrentHashMap<Integer,Timeout> ts,ConcurrentHashMap<Integer,Integer> rs, ConcurrentHashMap<Integer,byte[]> buf) {
		ds = soc;
		tasks = ts;
		resends = rs;
		queBuffer = buf;
	}

	public void run() {
		byte[] buff = new byte[1024];
		long timediff;
		long totalPktAcked = 0;
		int value;
		double localRtt;
		try {
			while (true) {
				DatagramPacket dpRecv = new DatagramPacket(buff,1024);
				ds.receive(dpRecv);
				int seq = 0;
				ByteBuffer bb = ByteBuffer.wrap(buff);
                seq = bb.getInt();
				value = seq - 1;
				if (Sender.debug) {
					Sender.getLock();
					Sender.getTimerLock();
					if (tasks.containsKey(value)) {
						Timeout tt = tasks.get(value);
						timediff = tt.getRtt();
						double avg = (Sender.rtt * totalPktAcked) + timediff;
						localRtt =  (avg / (totalPktAcked + 1));
						Sender.rtt = localRtt;
						try {
							System.out.printf("%d:\t Time Generated: %d:00 RTT: %d ms\t Number of Attempts: %d\n",value,tt.genTime(),timediff,resends.get(value)+1);
						} catch(Exception eee) {}
						totalPktAcked++;
						tasks.remove(value);
						resends.remove(value);
						queBuffer.remove(value);
						Sender.totalAcked++;
					}
					Sender.releaseTimerLock();
					Sender.releaseLock();
				}

				for (int j = Sender.toAck; j <= value; j++) {
					Sender.getTimerLock();
					Sender.getLock();
					if (tasks.containsKey(j)) {
						timediff = tasks.get(j).getRtt();
						double avg = (Sender.rtt * totalPktAcked) + timediff;
						localRtt =  (avg / (totalPktAcked + 1));
						Sender.rtt = localRtt;
						totalPktAcked++;
						tasks.remove(j);
						resends.remove(j);
						queBuffer.remove(j);
						Sender.totalAcked++;
					}
					Sender.releaseTimerLock();
					Sender.releaseLock();
				}
				Sender.toAck = seq;
				if (totalPktAcked >= Sender.maxPkts) {
					Sender.finalStatus();
					// System.out.println("max acked");
					System.exit(0);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

class PacketGen extends TimerTask {
	private ConcurrentHashMap<Integer,byte[]> queBuffer;
	// private HashMap<Integer,byte[]> queBuffer;
	private Random rand;
	private int length;
	private int seqNo = 0;
	private int buffSize;
	public PacketGen(ConcurrentHashMap<Integer,byte[]> buff, int len, int maxBuff) {
		queBuffer = buff;
		rand = new Random();
		length = len;
		buffSize = maxBuff;
	}

	@Override
	public void run() {
		Sender.getLock();
		if (queBuffer.size() < buffSize) {
			byte[] arr = new byte[length];
			rand.nextBytes(arr);
			arr[0] = (byte)(seqNo >> 24);
			arr[1] = (byte)(seqNo >> 16);
			arr[2] = (byte)(seqNo >> 8);
			arr[3] = (byte)(seqNo);
			queBuffer.put(seqNo, arr);
			if (seqNo == Integer.MAX_VALUE) {
				seqNo = 0;
			} else {
				++seqNo;
			}
		}
		Sender.releaseLock();
	}
}

class Timeout extends TimerTask {
	private int seq;
	private ConcurrentHashMap<Integer,Timeout> tasks;
	public long sendTime;
	private ConcurrentHashMap<Integer,Integer> resends;
	private DatagramPacket dp;
	public Timeout(ConcurrentHashMap<Integer,Timeout> timerList, int sn, ConcurrentHashMap<Integer,Integer> rs, DatagramPacket ds) {
		tasks = timerList;
		seq = sn;
		resends = rs;
		dp = ds;
		sendTime = System.currentTimeMillis();
	}

	@Override
	public void run() {
		// Sender.timer.cancel();
		// Sender.timer = new Timer();
		// System.out.println("timeout -------- "+seq);
		// if (seq < Sender.toAck) {
		// 	this.cancel();
		// 	// // System.out.println("prev rem "+Sender.toAck);
		// 	tasks.remove(seq);
		// 	resends.remove(seq);
		// } else {
		// 	// System.out.println("not removing "+Sender.toAck);
			for (int i=seq; i <= Sender.pktNo; ++i) {
				Sender.getTimerLock();
				if (tasks.containsKey(i)) {
					tasks.get(i).resend();
				}
				Sender.releaseTimerLock();
			}
		// }
	}

	public void resend() {
		this.cancel();
		tasks.remove(seq);
		if (resends.containsKey(seq)) {
			Integer val = resends.get(seq);
			if (val >= 5) {
				Sender.finalStatus();
				// System.out.println("more than 5  "+seq);
				System.exit(0);
			}
			val++;
			try {		
				Sender.ds.send(dp);
			} catch (Exception e) {
				e.printStackTrace();
			}
			Timeout ts = new Timeout(tasks,seq,resends,dp); 
			Sender.timer.schedule(ts,Math.max((long)(Sender.rtt * 2),2));
			tasks.put(seq,ts);
			Sender.totalTrans++;
			// System.out.printf("resending--> %d\n",seq);
			resends.replace(seq,resends.get(seq)+1);
		}
	}

	public long genTime() {
		return sendTime - Sender.startTime;
	}

	public long getRtt() {
		this.cancel();
		return System.currentTimeMillis() - sendTime;
	}
}