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
import java.io.FileWriter;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Arrays;
import java.io.PrintWriter;

public class Main {
	public static void main(String[] args) {
		HashMap<String,String> hmap = new HashMap<String,String>();
		for (int i=0; i<args.length; i += 2) {
			hmap.put(args[i], args[i+1]);
		}
		Router rt = new Router(hmap);
		rt.route();
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
		// System.out.printf("%x %x %x %x\n", buf[start], buf[start+1], buf[start+2], buf[start+3]);
		for (int i=0; i<4; i++) {
			x = (x << 8) | (0xff & buf[start+i]);
		}
		// System.out.printf("%x %d\n",x,x);
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
			inFileName = hm.get("-f");
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

		HashMap<Integer,byte[]> lsa = new HashMap<Integer,byte[]>();
		HelloSender sender = new HelloSender(neighbors,ds,helloInt,id,map);
		LSA lsaSender = new LSA(ds, costs, neighbors, id, lsaInt, lsa);
		Topology plot = new Topology(lsa, neighbors, id, spfInt, outFileName);
		sender.start();
		lsaSender.start();
		plot.start();

		byte[] buffer = new byte[1024];
		HashMap<Integer,Integer> last_lsa = new HashMap<Integer,Integer>();	//stores counter for last lsa segment received
		int size = neighbors.size();
		for (int i=0; i<size; i++) {
			last_lsa.put(neighbors.get(i), -1);
		}
		//receiver
		DatagramPacket dp, ds_rcv;
		int length, sender_id, i, min, max;
		Random random = new Random();

		try {
			while (true) {
				ds_rcv = new DatagramPacket(buffer, 1024);
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
				} else if (buffer[0] == 0x1) {	//lsa message
					int seqno = Main.getInt(buffer,5);
					if (seqno > last_lsa.get(sender_id)) {
						lsa.put(sender_id,Arrays.copyOf(buffer,length));
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

//send lsa segments
class LSA extends Thread {
	private int interval;
	private int seqNo;
	private DatagramSocket ds = null;
	private ArrayList<Integer> neighbors = null;
	private HashMap<Integer,Integer> costs = null;
	private int id;
	HashMap<Integer,byte[]> lsaData = null;

	public LSA(DatagramSocket soc, HashMap<Integer,Integer> hm, ArrayList<Integer> al, int mid, int time,HashMap<Integer,byte[]> data) {
		ds = soc;
		neighbors = al;
		costs = hm;
		id = mid;
		interval = time;
		lsaData = data;
	}

	@Override
	public void run() {
		seqNo = 0;
		int i, entries;
		DatagramPacket dp;
		entries = neighbors.size();
		byte[] message = new byte[entries*8+13];
		// System.out.println("id: "+id+" main "+message.length);
		int m_count = 0;

		try {
			while(true) {
				m_count = 0;
				message[0] = 0x1;	//lsa datagram
				Main.fillBytes(message,1,id);//id
				Main.fillBytes(message,5,seqNo);//sequence no.
				// Main.fillBytes(message,i,entries); //no. of entries
				i = 13;
				for (int j=0; j<entries; j++) {
					if (costs.containsKey(neighbors.get(j))) {
						Main.fillBytes(message,i,neighbors.get(j));//neighbour id
						Main.fillBytes(message, i+4, costs.get(neighbors.get(j)));//cost
						i += 8;
						m_count++;
					}
				}
				lsaData.put(id, Arrays.copyOf(message,13+m_count*8));
				Main.fillBytes(message,9,m_count);
				for (int j=0; j<entries; j++) {

					// System.out.println("id: "+id+" count "+m_count);
					// System.out.printf("%d ------> %d\n",message.length, 13+m_count*8);
					dp = new DatagramPacket(message,13+m_count*8,new InetSocketAddress("localhost",10000+neighbors.get(j)));
					ds.send(dp);
				}
				Thread.sleep(1000*interval);
				seqNo++;
			}
		} catch (Exception e) {
			System.out.println("lsa sender stopped");
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
				Thread.sleep(1000*sleepTime);
			}
		} catch (Exception e) {
			System.out.println("Hello Sender stopped");
			e.printStackTrace();
		}
	}
}

class Topology extends Thread {
	private int interval;
	private ArrayList<Integer> neighbors = null;
	private HashMap<Integer,byte[]> lsas = null;
	private int id;
	private String outfile;
	private PrintWriter pw = null;

	public Topology(HashMap<Integer,byte[]> hm, ArrayList<Integer> al, int mid, int time, String file) {
		neighbors = al;
		lsas = hm;
		id = mid;
		interval = time;
		outfile = file;
	}

	@Override
	public void run() {
		Node root = new Node(id,0);
		try {
			pw = new PrintWriter(outfile);
			while(true) {
				Thread.sleep(1000*interval);
				createGraph(root);
			}
		} catch (Exception e) {
			System.out.println("Topology generator stopped");
			e.printStackTrace();
		}
	}

	private void createGraph(Node root) {
		int size = lsas.size();
		Integer temp_id;
		PriorityQueue<Node> q = new PriorityQueue<Node>(new NodeComparator());
		HashMap<Integer,Node> list = new HashMap<Integer,Node>();	//list contains all the nodes
		Node temp;

		for (Iterator itr = lsas.keySet().iterator(); itr.hasNext(); ) {
			temp_id = (Integer)itr.next();
			if (temp_id != id) {
				temp = new Node(temp_id);
				q.add(temp);
				list.put(temp_id, temp);
			}
		}
		temp = new Node(id,0);
		list.put(id, temp);
		// if (lsas.get(temp.id) != null) {
		// 	// System.out.println(lsas.get(temp.id));
		// 	// System.out.println("not null");
		// } else {
		// 	// System.out.println("null");
		// }
		byte[] succ;
		int cost;
		Node neigh = null;
		int sum;
		// int count = 0;
		while (temp != null) {
			succ = lsas.get(temp.id);
			if (succ != null) {
				// count++;

				int entry_count = Main.getInt(succ,9);
				for (int i=13, j=0; j<entry_count; j++, i+=8) {
					try {
						neigh = list.get(Main.getInt(succ,i));	//array out of bound
					} catch (Exception e) {
						e.printStackTrace();
						System.out.printf("%d %d %d %d\n", entry_count, i, j, succ.length);
						for (int k=0; k<succ.length; k++) {
							System.out.printf("%x ", succ[k]);
						}
						System.out.println();
						System.exit(0);
					}
					cost = Main.getInt(succ,i+4);
					if (temp.cost != Integer.MAX_VALUE) {
						// System.out.printf("-----------%d %d %d %d %d\n", temp.id, temp.cost, neigh.cost, cost, neigh.id);
						sum = temp.cost + cost;
						if (sum < neigh.cost) {
							// System.out.printf("inside %d %d %d %d\n", neigh.id, sum, temp.cost, cost);
							neigh.cost = sum;
							neigh.parent = temp;
							q.remove(neigh);
							q.add(neigh);
						}
					}
				}
			}
			temp = q.poll();
		}
		printGraph(list);
	}

	private void printGraph(HashMap<Integer,Node> list) {
		Node start = list.get(id);
		Node temp, curr;
		// FileWriter fw = null;
		try {
			// fw = new FileWriter(outfile, true);
			// fw.write("Routing Table for Node No. "+id+" at Time "+(System.currentTimeMillis()/1000));
			pw.println("Routing Table for Node No. "+id+" at Time "+(System.currentTimeMillis()/1000));
			// fw.write("\n");
			// fw.write("Destination\t Path\t Cost\n");
			pw.println("Destination\t Path\t Cost");
			for (Iterator itr = list.keySet().iterator(); itr.hasNext(); ) {
				temp = list.get(itr.next());
				curr = temp;
				ArrayList<Integer> path = new ArrayList<Integer>();

				while(curr != start && curr != null) {	//getting null pointer exception
					path.add(0,curr.id);
					curr = curr.parent;
				}
				if (path.size() > 0 && curr != null) {
					// fw.write(temp.id);
					pw.print(temp.id);
					// fw.write("\t");
					pw.print("\t");
					Iterator ii = path.iterator();
					// fw.write((Integer)ii.next());
					pw.print(ii.next());
					while(ii.hasNext()) {
						// fw.write("-"+ii.next());
						pw.print("-"+ii.next());
					}
					// fw.write("\t"+temp.cost+"\n");
					pw.print("\t"+temp.cost+"\n");
				}
			}
			// fw.write("\n");
			pw.println("");
			pw.flush();
		} catch (Exception e) {
			e.printStackTrace();
			try {
				// fw.close();
				pw.close();
			} catch (Exception ee) {
				ee.printStackTrace();
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