import java.net.DatagramSocket;
import java.net.DatagramPacket;

public class UdpServer {
	public static void main(String[] args) {
            if (args.length < 1) {
                System.out.println("Usage: <port>");
                System.exit(0);
            }
            try {
                int port = Integer.valueOf(args[0]);
                DatagramSocket ds = new DatagramSocket(port);
                byte[] buf = new byte[1024];
                while (true) {
                    DatagramPacket dp = new DatagramPacket(buf,1024);
                    ds.receive(dp);
                    String str = new String(dp.getData(),0,dp.getLength());
                    System.out.println("Received: "+str);
                    String result = calculate(str);
                    DatagramPacket dpSend = new DatagramPacket(result.getBytes(),result.length(),dp.getSocketAddress()); 
                    ds.send(dpSend);
                }
            } catch (Exception e) {
                System.out.println(e.toString());
            }
	}
    public static String calculate(String str) {
        String[] arr = str.split(" ");
        switch (arr[0]) {
            case "add":
                return Integer.toString(Integer.valueOf(arr[1]) + Integer.valueOf(arr[2]));
            case "sub":
                return Integer.toString(Integer.valueOf(arr[1]) - Integer.valueOf(arr[2]));
            case "mul":
                return Integer.toString(Integer.valueOf(arr[1]) * Integer.valueOf(arr[2]));
            case "div":
                if (Integer.valueOf(arr[2]) == 0)
                    return "can't divide by zero";
                return Integer.toString(Integer.valueOf(arr[1]) / Integer.valueOf(arr[2]));
            case "exp":
                return Integer.toString((int)(Math.pow(Integer.valueOf(arr[1]),Integer.valueOf(arr[2]))));
            default:
                return "invalid expression";
        }
    }
}
