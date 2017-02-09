import java.net.*;
import java.io.*;
import java.lang.Integer;

public class Server {
	public static void main(String[] args) {
		if (args.length < 1) {
			System.out.println("Usage: <port>");
		}
		int port = Integer.valueOf(args[0]);
		int maxConn = 10;

		ServerSocket serverSocket = null;
        Socket socket = null;

        try{
        	serverSocket = new ServerSocket(port, maxConn);
	        while (true) {
	            socket = serverSocket.accept();
	            new EmailThread(socket).start();
	        }
	    } catch (IOException e) {
	    	e.printStackTrace();
	    }
	}
}

class EmailThread extends Thread {
    private Socket socket;

    public EmailThread(Socket clientSocket) {
        this.socket = clientSocket;
    }

    public void run() {
        DataInputStream in = null;
        DataOutputStream out = null;
        System.out.println("Connection established");
        String line, currUserName = null;
        try {
	        in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            File currUserFile = null;
            RandomAccessFile raf = null;
        	while (true) {
                line = in.readUTF();
                System.out.println(line);
                String[] cmds = line.split(" ");
                if (line == null) {
                	break;
                } else {
                    if (cmds[0].equals("LSTU")) {
                    	File folder = new File("Spool");
						File[] listOfFiles = folder.listFiles();
						StringBuilder builder = new StringBuilder();
                        String uname;
					    for (File entry : listOfFiles) {
                            uname = entry.getName();
					      builder.append(uname.substring(0,uname.length()-4)).append("\n");
					    }
                        if (listOfFiles.length == 0) {
                            builder.append("No users available.\n");
                        }
                    	out.writeUTF(builder.toString());
                    	out.writeUTF("###");
                    } else if (cmds[0].equals("ADDU")) {
                    	File file = new File("Spool/"+cmds[1]+".dat");
                    	if (file.exists()) {
                    		out.writeUTF("Userid already present.\n");
       						out.writeUTF("###");
                    	} else {
                    		file.createNewFile();
       						out.writeUTF("User created successfully.\n");
       						out.writeUTF("###");
                    	}
                    } else if (cmds[0].equals("USER")) {
                    	currUserFile = new File("Spool/"+cmds[1]+".dat");
                    	if (currUserFile.exists()) {
                            currUserName = cmds[1];
                    		raf = new RandomAccessFile(currUserFile,"r");
                            String ln;
                    		int count = 0;
                    		if (raf.length() > 0) {
                    			ln = raf.readLine();
	                    		while(ln != null) 	{
	                    			if (ln.equals("###")) {
	                    				count++;
	                    			}
	                    			ln = raf.readLine();
	                    		}
	                    	}
                    		raf.seek(0);
                    		out.writeUTF("Welcome to your mailbox.\nThere are "+count+" messages in your mailbox.\n");
       						out.writeUTF("###");
                    	} else {
                            System.out.println(cmds[1]+" does not exist.");
                    		out.writeUTF("User does not exist.\n");
                    		out.writeUTF("###");
                    	}
                    } else if (cmds[0].equals("READM")) {
                        if (raf != null && currUserFile != null) {
                            String ln = raf.readLine();
                    		if (ln != null) {
                    			StringBuilder bd = new StringBuilder();
                    			while(!ln.equals("###")) {
                    				bd.append(ln).append("\n");
                    				ln = raf.readLine();
                    			}
                    			out.writeUTF(bd.toString());
                                out.writeUTF("###");
                    		} else {
                    			out.writeUTF("No More Mail\n");
                    			out.writeUTF("###");
                    		}
                    	} else {
                    		out.writeUTF("Error: current user not set.\n");
                    		out.writeUTF("###");
                    	}
                    } else if (cmds[0].equals("DELM")) {
                    	if (raf != null && currUserFile != null) {
                    		long pos = raf.getFilePointer();
                            String ln = raf.readLine();
                            if (ln != null) {
                                long epos = 0;
                    			while(!ln.equals("###")) {
                    				ln = raf.readLine();
                    			}
                                epos = raf.getFilePointer();
                                raf.seek(0);
                                File tempFile = new File(cmds[1]+".tmp");
                                tempFile.createNewFile();
                                byte[] buf;
                                FileOutputStream fs = new FileOutputStream(tempFile);
                                if (pos > Integer.MAX_VALUE) {
                                    long diff = pos;
                                    buf = new byte[Integer.MAX_VALUE];
                                    while (diff > Integer.MAX_VALUE) {
                                        raf.readFully(buf);
                                        fs.write(buf);
                                        diff -= Integer.MAX_VALUE;
                                    }
                                    if (diff > 0) {
                                        raf.readFully(buf,0,(int)diff);
                                        fs.write(buf,0,(int)diff);
                                        break;
                                    }
                                } else {
                                    buf = new byte[(int)pos];
                                    raf.readFully(buf);
                                    fs.write(buf);
                                }
                                raf.seek(epos);
                                long len = raf.length() - epos;
                                if (len > 0) {
                                    if (len > Integer.MAX_VALUE) {
                                        buf = new byte[Integer.MAX_VALUE];
                                        while (len > Integer.MAX_VALUE) {
                                            raf.readFully(buf);
                                            fs.write(buf);
                                            len -= Integer.MAX_VALUE;
                                        }
                                        if (len > 0) {
                                            raf.readFully(buf,0,(int)len);
                                            fs.write(buf,0,(int)len);
                                            break;
                                        }
                                    } else {
                                        buf = new byte[(int)len];
                                        raf.readFully(buf);
                                        fs.write(buf);
                                    }
                                }
                                raf.close();
                                currUserFile.delete();
                                fs.close();
                                tempFile.renameTo(currUserFile);
                                currUserFile = tempFile;
                                raf = new RandomAccessFile(currUserFile,"r");
                                raf.seek(pos);
                    			out.writeUTF("Message Deleted\n");
                                out.writeUTF("###");
                    		} else {
                    			out.writeUTF("No More Mail\n");
                    			out.writeUTF("###");
                    		}
                    	} else {
                    		out.writeUTF("Error: current user not set.\n");
                    		out.writeUTF("###");
                    	}
                    } else if (cmds[0].equals("SEND")) {
                    	if (currUserFile != null && in != null) {
                    		String str = in.readUTF();
                    		File toFile = new File("Spool/"+cmds[1]+".dat");
                    		if (toFile.exists()) {
                    			BufferedWriter tbr = new BufferedWriter(new FileWriter(toFile));
                    			tbr.append("From: "+currUserName+"\n");
                    			tbr.append("To: "+cmds[1]+"\n");
                    			tbr.append(str);
                    			tbr.append("\n");
                                tbr.close();
                                out.writeUTF("Message sent successfully.");
                                out.writeUTF("###");
                    		} else {
                    			out.writeUTF("User does not exist.\n");
                    			out.writeUTF("###");
                    		}
                    	} else {
                            out.writeUTF("Error: current user not set.\n");
                            out.writeUTF("###");
                    		System.out.println("current user does not exist.\n");
                    	}
                    } else if (cmds[0].equals("DONEU")) {
                    	out.writeUTF("User closed successfully.\n");
                        out.writeUTF("###");
                        currUserFile = null;
                        currUserName = null;
                    	if (raf != null) {
                    		raf.close();
                    		raf = null;
                    	}
                    } else if (cmds[0].equals("QUIT")) {
                        out.writeUTF("Closed Connection with the server.");
                        out.writeUTF("###");
                    	break;
                    } else {
                        System.out.println("invalid command.");
                    	out.writeUTF("INVALID COMMAND\n");
                    	out.writeUTF("###");
                    }
                }
	        }
	    } catch (IOException e) {
	    	e.printStackTrace();
	    } finally {
	    	try {
	    		if (in != null) {
	    			in.close();
	    		}
	    		if (out != null) {
	    			out.close();
	    		}
	    		socket.close();
	    	} catch (IOException ioe) {
	    		return;
	    	}
	    }
    }
}