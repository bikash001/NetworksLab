import java.net.*;
import java.util.Scanner;

public class UdpClient {
  public static void main(String[] args) {
      if (args.length < 2) {
          System.out.println("Usage: <host> <port>");
      }
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
