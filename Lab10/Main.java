import java.util.HashMap;
import java.lang.Thread;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import javafx.util.Pair;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.net.InetSocketAddress;
import java.util.Random;
import java.util.Iterator;

public class Main {
	public static void main(String[] args) {
		HashMap<String,String> hmap = new HashMap<String,String>();
		for (int i=0; i<args.length; i += 2) {
			hmap.put(args[i], args[i+1]);
		}
	}

	public static int fillBytes(byte[] buf, int start, int num) {
		buf[start] = (byte)(num >> 24);
		buf[start+1] = (byte)(num >> 16);
		buf[start+2] = (byte)(num >> 8);
		buf[start+3] = (byte)(num);
		return start+4;
	}

	public static int getInt(byte[] buf, int start) {
		int x = 0;
		for (int i=0; i<4; i++) {
			x = x << 8;
			x = x | buf[start+i];
		}
		return x;
	}
}

class Router {
	private int id, helloInt = 1, lsaInt = 5, spfInt = 20;
	private String inFileName, outFileName;
	private ArrayList<Integer> neighbors = null;
	private DatagramSocket ds = null;

	public Router(HashMap<String,String> hm) {
		String temp = hm.get("-h");
		if (temp != null) {
			helloInt = Integer.valueOf(temp);
		}
		temp = hm.get("-a");
		if (temp != null) {
			lsaInt = Integer.valueOf(temp);
		}
		temp = hm.get("-s");
		if (temp != null) {
			spfInt = Integer.valueOf(temp);
		}
		try {
			id = Integer.valueOf(hm.get("-i"));
			inFileName = hm.get("-i");
			outFileName = hm.get("-o");
			if (inFileName == null || outFileName == null) {
				System.out.println("Error: Invalid input.");
				System.exit(0);
			}
		} catch(Exception e) {
			System.out.println("Error: Invalid input.");
			System.exit(0);
		}
	}

	public void route() {
		helloInt *= 1000;
		int nodes, links;
		BufferedReader br = null;
		HashMap<Integer,Pair<Integer,Integer>> map = new HashMap<Integer,Pair<Integer,Integer>>();
		neighbors = new ArrayList<Integer>();
		HashMap<Integer, Integer> costs = new HashMap<Integer, Integer>();

		try {
			br = new BufferedReader(new FileReader(inFileName));
			String line;
			String[] words;
			line = br.readLine();
			words = line.split(" ");
			nodes = Integer.valueOf(words[0]);
			links = Integer.valueOf(words[1]);
			Integer nodeId;

			while(br.ready()) {
				line = br.readLine();
				words = line.split(" ");
				
				if (id == Integer.valueOf(words[0])) {
					nodeId = Integer.valueOf(words[1]);
					neighbors.add(nodeId);
					map.put(nodeId,
					 new Pair<Integer,Integer>(Integer.valueOf(words[2]), Integer.valueOf(words[3])));
				} else if (id == Integer.valueOf(words[1])) {
					nodeId = Integer.valueOf(words[0]);
					neighbors.add(nodeId);
					map.put(nodeId,
					 new Pair<Integer,Integer>(Integer.valueOf(words[2]), Integer.valueOf(words[3])));
				}
			}
			br.close();

			ds = new DatagramSocket(10000+id);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}

		HelloSender sender = new HelloSender(neighbors,ds,helloInt,id,map);
		byte[] buffer = new byte[neighbors.size()*4+13];
		HashMap<Integer,Integer> last_lsa = new HashMap<Integer,Integer>();
		int size = neighbors.size();
		for (int i=0; i<size; i++) {
			last_lsa.put(neighbors.get(i), -1);
		}
		//receiver
		DatagramPacket dp, ds_rcv;
		int length, sender_id, i, min, max;
		HashMap<Integer,byte[]> lsa = new HashMap<Integer,byte[]>();
		Random random = new Random();

		try {
			while (true) {
				ds_rcv = new DatagramPacket(buffer, buffer.length);
				ds.receive(ds_rcv);
				// String str = new String(ds_rcv.getData(),0,ds_rcv.getLength());
				length = ds_rcv.getLength();
				sender_id = Main.getInt(buffer,1);

				if (buffer[0] == 0x0) {	//received hello msg
					byte[] msg = new byte[13];
					msg[0] = 0x2;
					i=1;
					Main.fillBytes(msg, i, id);
					Main.fillBytes(msg, i+4, sender_id);
					min = map.get(sender_id).getKey();
					max = map.get(sender_id).getValue();
					Main.fillBytes(msg, i+8, min+random.nextInt(max-min+1));
					dp = new DatagramPacket(msg,msg.length,new InetSocketAddress("localhost",10000+sender_id));
					ds.send(dp);
				} else if (buffer[0] == 0x1) {
					int seqno = Main.getInt(buffer,5);
					if (seqno > last_lsa.get(sender_id)) {
						lsa.put(sender_id,buffer.clone());
						for (int j=0; j<size; j++) {
							if (neighbors.get(j) != sender_id) {
								dp = new DatagramPacket(buffer,length,new InetSocketAddress("localhost",10000+neighbors.get(j)));
								ds.send(dp);
							}
						}
					}
				} else if (buffer[0] == 0x2) {
					costs.put(sender_id, Main.getInt(buffer,9));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

class LSA extends Thread {
	private int interval;
	private int seqNo;
	private DatagramSocket ds = null;
	private ArrayList<Integer> neighbors = null;
	private HashMap<Integer,Integer> costs = null;
	private int id;

	public LSA(DatagramSocket soc, HashMap<Integer,Integer> hm, ArrayList<Integer> al, int mid, int time) {
		ds = soc;
		neighbors = al;
		costs = hm;
		id = mid;
		interval = time;
	}

	@Override
	public void run() {
		seqNo = 0;
		int i, entries;
		DatagramPacket dp;
		entries = neighbors.size();
		byte[] message = new byte[entries*4+13];

		try {
			while(true) {
				message[0] = 0x1;	//lsa datagram
				i = Main.fillBytes(message,1,id);//id
				i = Main.fillBytes(message,i,seqNo);//sequence no.
				i = Main.fillBytes(message,i,entries); //no. of entries
				for (int j=0; j<entries; j++, i=i+8) {
					Main.fillBytes(message,i,neighbors.get(j));//neighbour id
					Main.fillBytes(message, i+4, costs.get(neighbors.get(j)));//cost
				}
				for (int j=0; j<entries; j++) {
					dp = new DatagramPacket(message,message.length,new InetSocketAddress("localhost",10000+neighbors.get(i)));
					ds.send(dp);
				}
				Thread.sleep(interval);
				seqNo++;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

class HelloSender extends Thread {
	private DatagramPacket dp = null;
	private int size;
	private DatagramSocket ds = null;
	private ArrayList<Integer> neighbors = null;
	private long sleepTime;
	private int id;
	private HashMap<Integer,Pair<Integer,Integer>> hm;

	public HelloSender(ArrayList<Integer> nb, DatagramSocket d, long time, int id_val, HashMap<Integer,Pair<Integer,Integer>> mp) {
		ds = d;
		neighbors = nb;
		size = nb.size();
		sleepTime = time;
		id = id_val;
		hm = mp;
	}

	@Override
	public void run() {
		try {;
			byte[] mbytes = new byte[5];
			mbytes[0] = 0x0;	//hello datagram
			Main.fillBytes(mbytes, 1, id);
			while (true) {
				for (int i=0; i<size; i++) {
					dp = new DatagramPacket(mbytes,mbytes.length,new InetSocketAddress("localhost",10000+neighbors.get(i)));
					ds.send(dp);
				}
				Thread.sleep(sleepTime);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

class Topology extends Thread {
	private int interval;
	private ArrayList<Integer> neighbors = null;
	private HashMap<Integer,byte[]> lsas = null;
	private int id;

	public Topology(HashMap<Integer,byte[]> hm, ArrayList<Integer> al, int mid, int time) {
		neighbors = al;
		lsas = hm;
		id = mid;
		interval = time;
	}

	@Override
	public void run() {
		Node root = new Node(id,0);
		while(true) {
			Thread.sleep(interval);
			createGraph(root);
		}
	}

	private void createGraph(root) {
		int size = lsas.size();
		Integer temp_id;
		PriorityQueue<Node> q = PriorityQueue<Node>(size, new NodeComparator());
		HashMap<Integer,Node> list = new HashMap<Integer,Node>();
		Node temp;

		for (Iterator itr = lsas.keySet().iterator(); itr.hasNext(); ) {
			temp_id = itr.next();
			if (temp_id != id) {
				temp = new Node(temp_id);
				q.add(temp);
				list.put(temp_id, temp);
			}
		}
		temp = new Node(id,0);
		list.put(id, temp);
		byte[] succ;
		int cost;
		Node neigh;
		int sum;
		while (temp != null) {
			succ = lsas.get(temp.id);
			for (int i=13; i<succ.length; i+=8) {
				neigh = list.get(Main.getInt(i));
				cost = Main.getInt(i+4);
				sum = temp.cost + cost;
				if (sum < neigh.cost) {
					neigh.cost = sum;
					neigh.parent = temp;
					q.remove(neigh);
					q.add(neigh);
				}
			}
			temp = q.poll();
		}
		printGraph(list);
	}

	private void printGraph(HashMap<Integer,Node> list) {
		Node start = list.get(id);
		Node temp, curr;
		
		for (Iterator itr = list.keySet().iterator(); itr.hasNext(); ) {
			temp = itr.next();
			curr = temp;
			ArrayList<Integer> path = new ArrayList<Integer>();

			while(curr != start) {
				path.add(0,curr.id);
				curr = curr.parent;
			}

		}
	}
}

class NodeComparator implements Comparator<Node> {
	@Override
	public int compare(Node x, Node y) {
		if (x.cost < y.cost) {
			return -1;
		} else if (x.cost > y.cost) {
			return 1;
		}
		return 0;
	}
}

class Node {
	public int id;
	public int cost;
	public Node parent;
	public Node(int id) {
		this.id = id;
		cost = Integer.MAX_VALUE;
		parent = null;
	}
	public Node(int id, int cost) {
		this.id = id;
		parent = null;
		this.cost = cost;
	}
}