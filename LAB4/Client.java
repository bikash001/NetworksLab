import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
	public static void main(String[] args) {
		
		// Check if valid argument are provided
		if (args.length < 2) {
			System.out.println("Usage: <host> <port>");
		}

		int port = Integer.valueOf(args[1]);	//port no.
		Scanner scanner = new Scanner(System.in);	//object for taking input from terminal
		Socket socket = null;	//client socket
		DataOutputStream out = null;	//output buffer of the socket
		DataInputStream in = null;	//input buffer of the socket
		String line;
		
		try {
			socket = new Socket(args[0], port);	//create Socket
			in = new DataInputStream(socket.getInputStream()); //get input buffer
			out = new DataOutputStream(socket.getOutputStream());	//get output buffer
			String txt = null;
			
			//run the email client until user commands 'Quit'
			while (true) {
				System.out.print("Main-Prompt>");
				line = scanner.nextLine();	//get command from the user
				String[] cmdList = line.split(" ");

				//if the command is "Listusers"
				if (cmdList[0].equals("Listusers")) {
					out.writeUTF("LSTU");	//send the command to server
					printResponse(in);	//get server's response
				}

				//if command is "Quit"
				else if (cmdList[0].equals("Quit")) {
					out.writeUTF("QUIT");	//send the command
					printResponse(in);	//get server's response
					break;
				}

				//if command is "Adduser"
				else if (cmdList[0].equals("Adduser")) {
					if (cmdList.length > 1) {	//check if userid is provided
						out.writeUTF("ADDU "+cmdList[1]);	//send the command
						printResponse(in);	//get server's response
					} else {	//userid not provided
						System.out.print("\nInvalid input\nMain-Prompt>");
					}
				}

				//SetUser command
				else if (cmdList[0].equals("SetUser")) {
					if (cmdList.length > 1) {	//check if userid is provided
						out.writeUTF("USER "+cmdList[1]);	//send command
						txt = in.readUTF();	//get response from server
						if (txt != null) {
							boolean userExist = true;

							//check if user exist
							if (txt.equals("User does not exist.\n")) {
								userExist = false;
							}

							//print the server's response
							while(!txt.equals("###")) {
								System.out.print(txt);
								txt = in.readUTF();
							}
							System.out.println("");

							//Run subprompt untill user commands "Done"
							while (userExist) {
								System.out.print("Sub-Prompt-"+cmdList[1]+">");
								String subcmdLine = scanner.nextLine();	//get user input
								String[] subcmds = subcmdLine.split(" ");
								
								//Read command
								if (subcmds[0].equals("Read")) {
									out.writeUTF("READM");
									printResponse(in);
								}

								//Delete command
								else if (subcmds[0].equals("Delete")) {
									out.writeUTF("DELM");
									printResponse(in);
								}

								//Send command
								else if (subcmds[0].equals("Send")) {
									//check if command is valid
									if (subcmds.length >= 2) {
										out.writeUTF("SEND "+subcmds[1]);	//send command to server
										System.out.print("Type-Subject: ");
										String subject = scanner.nextLine();	//get message subject
										System.out.print("Type-Message: ");
										StringBuilder builder = new StringBuilder("Subject: ");
										builder.append(subject).append("\n");
										String msg = scanner.nextLine();

										//get message content
										while (!msg.endsWith("###")) {
											builder.append(msg).append("\n");
											msg = scanner.nextLine();
										}
										builder.append(msg.substring(0,msg.length()-3)).append("\n###");
										int strLen = builder.length();
										int start = 0;
										while (strLen > 60000) {
											out.writeUTF(builder.substring(start,start+60000));
											start += 60000;
											strLen -= 60000;
										}
										if (strLen != 0) {
											out.writeUTF(builder.substring(start,start+strLen));	//send the message
										}
										printResponse(in);	//print response
									} else {	//invalid command
										System.out.print("INVALID INPUT");
									}
								}

								//Done command
								else if (subcmds[0].equals("Done")) {
									out.writeUTF("DONEU");
									printResponse(in);
									break;
								}

								//invalid command
								else {
									System.out.println("INVALID INPUT");
								}
							}
						}
					}
				} else {
					System.out.println("INVALID INPUT");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			//close the resources
			try{
				if (in != null) {
					in.close();
				}
				if (out != null) {
					out.close();
				}
				if (socket != null) {
					socket.close();
				}
			} catch (IOException ioe) {
			}
		}
	}

	//method to print the response from the server
	public static void printResponse(DataInputStream in) {
		try {
			String txt = in.readUTF();
			System.out.print("Server Response:\n");
			while(!txt.equals("###")) {
				System.out.print(txt);
				txt = in.readUTF();
			}
			System.out.println("");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}