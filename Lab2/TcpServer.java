import java.net.*;
import java.io.*;

public class TcpServer {
	public static void main(String[] args) {
		if (args.length < 1) {
			System.out.println("Usage: <port>");
		}
		int port = Integer.valueOf(args[0]);
		try {
			ServerSocket server = new ServerSocket(port);
			while (true) {
				Socket client = server.accept();
				DataInputStream in = new DataInputStream(client.getInputStream());
				String msg = in.readUTF();
				String[] options = msg.split(" ");
				File file = new File(options[0]);
				DataOutputStream out = new DataOutputStream(client.getOutputStream());
				if (file.exists()) {
					int size = Integer.valueOf(options[1]);
					byte[] buf = new byte[size];
					FileInputStream is = new FileInputStream(options[0]);
					is.read(buf, 0, size);
					out.write(buf,0,size);
					is.close();
				} else {
					out.writeBytes("SORRY!");
				}
				in.close();
				out.close();
				client.close();
			}
		} catch (Exception e) {
			System.out.println(e.toString());
		}
	}
}