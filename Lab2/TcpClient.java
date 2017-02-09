import java.io.*;
import java.net.*;
import java.util.Scanner;

public class TcpClient {
	public static void main(String[] args) {
		if (args.length < 2) {
			System.out.println("Usage: <host> <port>");
		}
		int port = Integer.valueOf(args[1]);
		Scanner scanner = new Scanner(System.in);
		String file;
		int size;
		System.out.print("Enter <filename> <N>: ");
		file = scanner.next();
		size = scanner.nextInt();
		try{
			Socket client = new Socket(args[0], port);
			DataOutputStream out = new DataOutputStream(client.getOutputStream());
			out.writeUTF(file+" "+Integer.toString(size));
			DataInputStream in =new DataInputStream(client.getInputStream());
			// System.out.println("Server: "+in.readUTF());
			byte[] msg = new byte[6];
			int rsize = in.read(msg,0,6);
			String str = new String(msg, 0, rsize);
			if (rsize == 6 && str.equals("SORRY!")) {
				System.out.println("Server says that the file does not exist.");
			} else {
				FileOutputStream fs = new FileOutputStream("./"+file+"1");
				fs.write(msg, 0, rsize);
				while (in.available() != 0) {
					fs.write(in.read());
				}
				fs.close();
			}
			in.close();
			out.close();
			client.close();
		} catch(Exception e) {
			System.out.println(e.toString());
		}
	}
}