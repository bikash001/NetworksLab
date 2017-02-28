import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.util.Random;
import java.lang.String;

public class Receiver {

	public static void main(String[] args) {
            int len = args.length;
            int port = 1080;
            int maxPkts = 10;
            double perror = 0.00001;
            boolean debugMode = false;
            int maxSeqNo = 255;

            for (int i=0; i<len; i++) {
            	if (args[i].equals("-d")) {
            		debugMode = true;
            	} else if (args[i].equals("-p")) {
            		port = Integer.valueOf(args[++i]);
            	} else if (args[i].equals("-n")) {
            		maxPkts = Integer.valueOf(args[++i]);
            	} else if (args[i].equals("-e")) {
            		perror = Double.valueOf(args[++i]);
            	}
            }

            Random rgen = new Random();
            int NFE = 0;
            try {
                DatagramSocket ds = new DatagramSocket(port);
                byte[] buf = new byte[1024];
                for (int count=0; count < maxPkts;) {
                    DatagramPacket dp = new DatagramPacket(buf,1024);
                    ds.receive(dp);
                    if (rgen.nextDouble() <= perror) {
                    	continue;
                    }
                    
                    String str = new String(dp.getData(),0,dp.getLength());
                    // System.out.println("Received: "+str);
                    if (Integer.valueOf(str.substring(0,str.indexOf(' '))) == NFE) {
                    	NFE++;
                    }
                    if (NFE == maxSeqNo) {
                    	NFE = 0;
                    }
                    String result = "ACK "+NFE;
                    DatagramPacket dpSend = new DatagramPacket(result.getBytes(),result.length(),dp.getSocketAddress());
                    ds.send(dpSend);
                    ++count;
                }
            } catch (Exception e) {
                System.out.println(e.toString());
            }
	}
}