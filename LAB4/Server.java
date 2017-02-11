import java.net.*;
import java.io.*;
import java.lang.Integer;

public class Server {
	public static void main(String[] args) {
		//check if arguments are correct
		if (args.length < 1) {
			System.out.println("Usage: <port>");
		}
		int port = Integer.valueOf(args[0]);	//port no.
		int maxConn = 10;	//maximum connection

		ServerSocket serverSocket = null;	//server socket
        Socket socket = null;	//client socket

        try{
        	serverSocket = new ServerSocket(port, maxConn); //create server socket
	        while (true) {
	            socket = serverSocket.accept();	//listen to new connection
	            new EmailThread(socket).start();	//create new thread to serve the connection
	        }
	    } catch (IOException e) {
	    	e.printStackTrace();
	    }
	}
}

class EmailThread extends Thread {
    private Socket socket;	//client socket

    public EmailThread(Socket clientSocket) {
        this.socket = clientSocket;
    }

    public void run() {
        DataInputStream in = null;	//input buffer
        DataOutputStream out = null;	//output buffer
        System.out.println("Connection established");
        String line, currUserName = null;

        try {
	        in = new DataInputStream(socket.getInputStream());	//get input buffer
            out = new DataOutputStream(socket.getOutputStream());	//get output buffer
            
            File currUserFile = null;	//current user spool file
            RandomAccessFile raf = null;	//handler for read write from file
        	
        	while (true) {
                line = in.readUTF();	//get command
                System.out.println(line);
                String[] cmds = line.split(" ");
                
                //if something went wrong
                if (line == null) {
                	break;
                } else {
                	// command LSTU
                    if (cmds[0].equals("LSTU")) {
                    	File folder = new File("Spool");	//get Spool folder
						File[] listOfFiles = folder.listFiles();	//list of files in the spool folder
						StringBuilder builder = new StringBuilder();
                        String uname;

                        // get the list of users
					    for (File entry : listOfFiles) {
                            uname = entry.getName();
					      builder.append(uname.substring(0,uname.length()-4)).append("\n");
					    }

					    //if no user available
                        if (listOfFiles.length == 0) {
                            builder.append("No users available.\n");
                        }
                    	out.writeUTF(builder.toString());	//send response
                    	out.writeUTF("###");
                    }

                    //command ADDU
                    else if (cmds[0].equals("ADDU")) {
                    	File file = new File("Spool/"+cmds[1]+".dat");	//get spool file for the user
                    	
                    	//check if user exist
                    	if (file.exists()) {
                    		out.writeUTF("Userid already present.\n");
       						out.writeUTF("###");
                    	}
                    	// create new spool file, if user does not exist
                    	else {
                    		file.createNewFile();
       						out.writeUTF("User created successfully.\n");
       						out.writeUTF("###");
                    	}
                    }

                    // command USER
                    else if (cmds[0].equals("USER")) {
                    	currUserFile = new File("Spool/"+cmds[1]+".dat");	//get current user spool file
                    	
                    	// if user file exist
                    	if (currUserFile.exists()) {
                            currUserName = cmds[1];
                    		raf = new RandomAccessFile(currUserFile,"r");	//open file for reading
                            String ln;
                    		int count = 0;
                    		// check if the file contains any message
                    		if (raf.length() > 0) {
                    			ln = raf.readLine();

                    			// check how many messages are there
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
                    	}
                    	// user file does not exist
                    	else {
                            System.out.println(cmds[1]+" does not exist.");
                    		out.writeUTF("User does not exist.\n");
                    		out.writeUTF("###");
                    	}
                    }

                    // command READM
                    else if (cmds[0].equals("READM")) {
                    	// check if the user already in sub-prompt
                        if (raf != null && currUserFile != null) {
                            String ln = raf.readLine();

                            // get the message from the spool file, if the file pointer points to any message
                    		if (ln != null) {
                    			StringBuilder bd = new StringBuilder();
                    			while(!ln.equals("###")) {
                    				bd.append(ln).append("\n");
                    				ln = raf.readLine();
                    			}
                    			out.writeUTF(bd.toString());
                                out.writeUTF("###");
                    		}
                    		// no messages, or file pointer at the end
                    		else {
                    			out.writeUTF("No More Mail\n");
                    			out.writeUTF("###");
                    		}
                    	}
                    	// SetUser command required first
                    	else {
                    		out.writeUTF("Error: current user not set.\n");
                    		out.writeUTF("###");
                    	}
                    }

                    // command DELM
                    else if (cmds[0].equals("DELM")) {
                    	// check if current user is set
                    	if (raf != null && currUserFile != null) {
                    		long pos = raf.getFilePointer();	//get current file pointer
                            String ln = raf.readLine();

                            // check if end of file encountered
                            if (ln != null) {
                                long epos = 0;

                                //skip the message to be deleted
                    			while(!ln.equals("###")) {
                    				ln = raf.readLine();
                    			}
                                epos = raf.getFilePointer();	//file pointer after the message to be deleted
                                raf.seek(0);

                                //temporary file
                                File tempFile = new File(cmds[1]+".tmp");
                                tempFile.createNewFile();

                                byte[] buf;
                                FileOutputStream fs = new FileOutputStream(tempFile);
                                
                                // copy the content of the user's spool file
                                // to temporary file till the message before the message
                                // to be deleted
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

                                // copy the rest of the messages after the message
                                // to be deleted
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
                                
                                currUserFile.delete();	// delete users spool file
                                fs.close();
                                tempFile.renameTo(currUserFile); // rename temp file to user's spool file
                                currUserFile = tempFile;
                                raf = new RandomAccessFile(currUserFile,"r");
                                raf.seek(pos); 	// set the file pointer
                    			out.writeUTF("Message Deleted\n");	// send the response
                                out.writeUTF("###");
                    		} else {	// file pointer at the end
                    			out.writeUTF("No More Mail\n");
                    			out.writeUTF("###");
                    		}
                    	} else {	// user not set
                    		out.writeUTF("Error: current user not set.\n");
                    		out.writeUTF("###");
                    	}
                    }

                    // command SEND
                    else if (cmds[0].equals("SEND")) {
                    	// check if user is set
                    	if (currUserFile != null && in != null) {
                    		String str = in.readUTF();
                    		File toFile = new File("Spool/"+cmds[1]+".dat");	// get spool file of the user to whom message to be sent
                    		
                    		// check if user exist
                    		if (toFile.exists()) {

                    			// append the message at the end of the user's spool file
                    			BufferedWriter tbr = new BufferedWriter(new FileWriter(toFile));
                    			tbr.append("From: "+currUserName+"\n");
                    			tbr.append("To: "+cmds[1]+"\n");
                    			tbr.append(str);
                    			tbr.append("\n");
                                tbr.close();
                                out.writeUTF("Message sent successfully.");
                                out.writeUTF("###");
                    		} else {	// user not exist
                    			out.writeUTF("User does not exist.\n");
                    			out.writeUTF("###");
                    		}
                    	}
                    	// user not set
                    	else {
                            out.writeUTF("Error: current user not set.\n");
                            out.writeUTF("###");
                    		System.out.println("current user does not exist.\n");
                    	}
                    }

                    // command DONEU
                    else if (cmds[0].equals("DONEU")) {
                    	out.writeUTF("User closed successfully.\n");
                        out.writeUTF("###");
                        currUserFile = null;
                        currUserName = null;
                    	if (raf != null) {
                    		raf.close();
                    		raf = null;
                    	}
                    }

                    // if command QUIT, break out of the while loop
                    else if (cmds[0].equals("QUIT")) {
                        out.writeUTF("Closed Connection with the server.");
                        out.writeUTF("###");
                    	break;
                    }

                    // invalid command
                    else {
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
	    		// close the resources
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