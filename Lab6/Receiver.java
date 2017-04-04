import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.util.Random;
import java.lang.String;
import java.lang.Thread;

public class Receiver {

	public static void main(String[] args) {
            int len = args.length;
            int port = 1080;
            int maxPkts = 10;
            double perror = 0.001;	//packet drop probability;
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
            		// System.out.printf("perror %f\n",perror);
            	}
            }

            Random rgen = new Random();
            int NFE = 0;
            try {
                DatagramSocket ds = new DatagramSocket(port);
                long startTime = System.currentTimeMillis();
                byte[] buf = new byte[1024];
                for (int count=0; count < maxPkts;) {
                    DatagramPacket dp = new DatagramPacket(buf,1024);
                    ds.receive(dp);
                    
                    int seq = 0;
					seq = (0xff & buf[0]);
					seq = (seq << 8) | (0xff & buf[1]);
					seq = (seq << 8) | (0xff & buf[2]);
					seq = (seq << 8) | (0xff & buf[3]);
					if (rgen.nextDouble() <= perror) {
                    	// System.out.printf("packet %d dropped\n",seq);
                    	continue;
                    }
					// System.out.printf("\nrecieved %d\n", seq);
					if (debugMode) {
						System.out.printf("%d:\t Time Received: %d:00  Packet droped: false\n",seq,System.currentTimeMillis()-startTime);
					}

                    if (seq == NFE) {
                    	NFE++;
                    	// System.out.printf("received successfully %d\n",seq);
                    }
                    if (NFE == Integer.MAX_VALUE) {
                    	NFE = 0;
                    }
                    // String result = "ACK "+NFE;
                    byte[] arr = new byte[1024];
                    arr[0] = (byte)(NFE >> 24);
					arr[1] = (byte)(NFE >> 16);
					arr[2] = (byte)(NFE >> 8);
					arr[3] = (byte)(NFE);
					DatagramPacket dpSend = new DatagramPacket(arr,arr.length,dp.getSocketAddress());
                    ds.send(dpSend);
                    ++count;
                }
            } catch (Exception e) {
                System.out.println(e.toString());
            }
	}
}