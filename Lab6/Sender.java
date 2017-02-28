import java.net.*;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.lang.Thread;

public class Sender {
  public static void main(String[] args) {
    boolean debug = false;
    int port = 1080;
    int pktLen = 100;
    int pktGenRate = 10;
    int maxPkts = 1000;
    int maxBuffSize = 100;

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
    	} else if (args[i].equals("-b")) {
    		maxBuffSize = Integer.valueOf(args[++i]);
    	}
    }

    ConcurrentLinkedQueue<byte[]> queBuffer = new ConcurrentLinkedQueue();


    try {
      DatagramSocket ds = new DatagramSocket();
      InetAddress ip = InetAddress.getByName(args[0]);
      int port = Integer.valueOf(args[1]);
      String str;
      byte[] buf = new byte[1024];
      Scanner in = new Scanner(System.in);
      System.out.print("Enter command: ");
      str = in.nextLine();
      do {
        DatagramPacket dpSend = new DatagramPacket(str.getBytes(),str.length(),ip,port);
        ds.send(dpSend);
        DatagramPacket dpRecv = new DatagramPacket(buf,1024); 
        ds.receive(dpRecv);
        String msg = new String(dpRecv.getData(),0,dpRecv.getLength());
        System.out.println("Answer from server: "+msg);
        System.out.print("Enter command: ");
        str = in.nextLine();
      } while (!str.isEmpty());
      
    } catch(Exception e){
    	System.out.println(e.toString());
    }
  }
}

class PacketGen extends Thread {
	private ConcurrentLinkedQueue<byte[]> queBuffer;
	public PacketGen(ConcurrentLinkedQueue<byte[]> buff) {
		queBuffer = buff;
	}

	public void run() {
		
	}
}