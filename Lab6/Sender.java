import java.net.*;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.lang.Thread;
import java.util.TimerTask;
import java.util.Random;
import java.util.Timer;
import java.util.HashMap;
import java.lang.System;
import java.util.Iterator;

public class Sender {
	public static int toAck = 0;
	public static int lastSent = 0;
	public static HashMap<Integer,Timeout> timerList;
	public static HashMap<Integer,Integer> resends;
	public static long rtt = 0;
	public static long totalAcked;
	public static int maxPkts = 100;
	public static DatagramSocket ds = null;
	public static Timer timer = null;
	public static int totalTrans = 0;
	public static int pktGenRate = 1;
	public static int pktLen = 100;
	public static boolean debug = false;
	public static long startTime;
	    
  	public static void main(String[] args) {
	    int port = 1080;
	    int maxBuffSize = 100;
	    int wsize = 10;
	    InetAddress ip = null;
	    timerList = new HashMap<Integer,Timeout>();
	    try {
		    for (int i=0; i<args.length; ++i) {
		    	if (args[i].equals("-d")) {
		    		debug = true;
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
			e.printStackTrace();
		}

	    ConcurrentLinkedQueue<byte[]> queBuffer = new ConcurrentLinkedQueue<byte[]>();
	    timer = new Timer();
	    resends = new HashMap<Integer,Integer>();
	    PacketGen generator = new PacketGen(queBuffer, pktLen, maxBuffSize);
	    timer.schedule(generator,0,1000/pktGenRate);
	   	
	    try {
			ds = new DatagramSocket();
			startTime = System.currentTimeMillis();
			byte[] buff = null;
			Recv receiver = new Recv(ds,timerList,resends,queBuffer);
			receiver.start();
	    	int pktNo = 0;
	    	Iterator<byte[]> itr = queBuffer.iterator();
	    	for (int i=0; i < 10; ) {
	        		
	        	if (itr.hasNext() && (lastSent-toAck+1) < wsize) {
	        		System.out.println("has");
	        		i++;
		        	buff = itr.next();
		        	DatagramPacket dpSend = new DatagramPacket(buff,buff.length,ip,port);
		        	ds.send(dpSend);
		        	Timeout tout = new Timeout(timerList, ++lastSent, resends,dpSend);
		        	timerList.put(lastSent,tout);
		        	timer.schedule(tout, 100);
		        	// System.out.println(buff);
		        	resends.put(pktNo, 0);
		        	if (pktNo == Integer.MAX_VALUE) {
		        		pktNo = 0;
		        	} else {
		        		pktNo++;
		        	}
		        	totalTrans++;
		        }
			}
			while (totalAcked < maxPkts) {
				if (itr.hasNext() && lastSent-toAck+1 < wsize) {
					buff = itr.next();
		        	DatagramPacket dpSend = new DatagramPacket(buff,buff.length,ip,port);
		        	ds.send(dpSend);
		        	Timeout tout = new Timeout(timerList, ++lastSent, resends, dpSend);
		        	timerList.put(lastSent,tout);
		        	timer.schedule(tout, 2 * rtt);
		        	resends.put(pktNo,0);
		        	if (pktNo == Integer.MAX_VALUE) {
		        		pktNo = 0;
		        	} else {
		        		pktNo++;
		        	}
		        	totalTrans++;
				}
			}
	      
	    } catch(Exception e){
	    	e.printStackTrace();
	    }
	}

	public static void finalStatus() {
		System.out.printf("Packet Generation Rate: %d\n", pktGenRate);
		System.out.printf("Packet Length: %d\n", pktLen);
		System.out.printf("Retransmission Ration: %f\n", (float)totalTrans/totalAcked);
		System.out.printf("Average RTT: %d ms\n", rtt);
	}
}

class Recv extends Thread {
	private DatagramSocket ds;
	private HashMap<Integer,Timeout> tasks;
	private HashMap<Integer,Integer> resends;
	private ConcurrentLinkedQueue<byte[]> queBuffer;
	public Recv(DatagramSocket soc, HashMap<Integer,Timeout> ts,HashMap<Integer,Integer> rs, ConcurrentLinkedQueue<byte[]> buf) {
		ds = soc;
		tasks = ts;
		resends = rs;
		queBuffer = buf;
	}

	public void run() {
		byte[] buff = new byte[1024];
		long timediff;
		long totalPktAcked = 0;
		try {
			while (true) {
				DatagramPacket dpRecv = new DatagramPacket(buff,1024); 
				ds.receive(dpRecv);
				int seq = 0;
				seq = buff[0];
				seq = (seq << 8) + buff[1];
				seq += (seq << 8) + buff[2];
				seq += (seq << 8) + buff[3];
				// System.out.printf("%x",buff[0]);
				// System.out.printf("%x",buff[1]);
				// System.out.printf("%x",buff[2]);
				// System.out.printf("%x",buff[3]);
				System.out.printf("\nreceived %d\n", seq);
				if (Sender.debug) {
					if (tasks.containsKey(Sender.toAck)) {
						timediff = tasks.get(Sender.toAck).getRtt();
						Sender.rtt = ((Sender.rtt * totalPktAcked) + timediff) / (totalPktAcked + 1);
						System.out.printf("%d:\t Time Generated: %d:00 RTT: %d\t Number of Attempts: %d\n",seq,tasks.get(Sender.toAck).genTime(),timediff,resends.get(Sender.toAck)+1);
						totalPktAcked++;
						tasks.remove(Sender.toAck);
					}
					resends.remove(Sender.toAck);
					queBuffer.poll();
					Sender.totalAcked++;
					Sender.toAck++;
				}

				for (; Sender.toAck < seq; Sender.toAck++) {
					if (tasks.containsKey(Sender.toAck)) {
						timediff = tasks.get(Sender.toAck).getRtt();
						Sender.rtt = ((Sender.rtt * totalPktAcked) + timediff) / (totalPktAcked + 1);
						totalPktAcked++;
						tasks.remove(Sender.toAck);
					}
					resends.remove(Sender.toAck);
					queBuffer.poll();
					Sender.totalAcked++;
				}
				if (totalPktAcked >= Sender.maxPkts) {
					Sender.finalStatus();
					System.exit(0);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

class PacketGen extends TimerTask {
	private ConcurrentLinkedQueue<byte[]> queBuffer;
	private Random rand;
	private int length;
	private int seqNo = 0;
	private int buffSize;
	public PacketGen(ConcurrentLinkedQueue<byte[]> buff, int len, int maxBuff) {
		queBuffer = buff;
		rand = new Random();
		length = len;
		buffSize = maxBuff;
	}

	@Override
	public void run() {
		if (queBuffer.size() < buffSize) {
			byte[] arr = new byte[length];
			rand.nextBytes(arr);
			arr[0] = (byte)(seqNo >> 24);
			arr[1] = (byte)(seqNo >> 16);
			arr[2] = (byte)(seqNo >> 8);
			arr[3] = (byte)(seqNo);
			queBuffer.add(arr);
			if (seqNo == Integer.MAX_VALUE) {
				seqNo = 0;
			} else {
				++seqNo;
			}
		}
	}
}

class Timeout extends TimerTask {
	private int seq;
	private HashMap<Integer,Timeout> tasks;
	public long sendTime;
	private HashMap<Integer,Integer> resends;
	private DatagramPacket dp;
	public Timeout(HashMap<Integer,Timeout> timerList, int sn, HashMap<Integer,Integer> rs, DatagramPacket ds) {
		tasks = timerList;
		seq = sn;
		resends = rs;
		dp = ds;
		sendTime = System.currentTimeMillis();
	}

	@Override
	public void run() {
		for (int i=Sender.toAck; i <= seq; ++i) {
			if (tasks.containsKey(i)) {
				tasks.get(i).resend(i);
				tasks.remove(i);
			}
		}
	}

	public void resend(Integer i) {
		this.cancel();
		if (resends.containsKey(i)) {
			Integer val = resends.get(i);
			if (val >= 5) {
				Sender.finalStatus();
				System.exit(0);
			}
			val++;
			try {		
				Sender.ds.send(dp);
			} catch (Exception e) {
				e.printStackTrace();
			}
			Timeout ts = new Timeout(tasks,seq,resends,dp); 
			Sender.timer.schedule(ts,Sender.rtt * 2);
			tasks.put(i,ts);
			Sender.totalTrans++;
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