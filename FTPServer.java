import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.StringTokenizer;

public class FTPServer {
	
	public static final String[] COMMANDS = {"TYPE", "SYST", "PORT", "RETR", "QUIT", "USER", "PASS", "CONNECT"};
	private static final String SET_USERNAME = "anonymous";
	private static final String SET_PASSWORD = "guest@";
	
	private static int control_port = 12996;
	
	static boolean logged_in = false;
	
	private static ServerSocket control_listener;
	private static Socket server_control;
	private static BufferedReader command_in;
	private static DataOutputStream reply_out;
	private static Socket server_data;
	private static DataOutputStream file_out;
	
	private static String current_command;
	private static String username;
	private static String password;
	private static String type_code;
	private static String host_address;
	private static int port_number;
	private static String pathname;
	
	
	public static void main(String[] args) throws IOException{
		for(String s:args){
			control_port = Integer.parseInt(s);
		}
		control_listener = new ServerSocket(control_port);
		
		server_control = control_listener.accept();
		command_in = new BufferedReader(new InputStreamReader(server_control.getInputStream()));
		reply_out = new DataOutputStream(server_control.getOutputStream());
		if(server_control.isConnected()){
//			if(command_in.readLine().equalsIgnoreCase("CONNECT")){
				System.out.print(FTPServerReply.START.reply());
				reply_out.writeBytes(FTPServerReply.START.reply());
		
		}
		while(true){
			if(authenicate()){
				acceptCommands();
			}
		}
	}
	
	
	static boolean authenicate() throws IOException{
		String userCommand = command_in.readLine();
		System.out.println(userCommand+"\r");
		StringTokenizer tokens = new StringTokenizer(userCommand);
		if(tokens.nextToken().equals("USER")){
			if(checkString(username = tokens.nextToken()) && username.equals(SET_USERNAME)){
				System.out.print(FTPServerReply.GOOD_USER.reply());
				reply_out.writeBytes(FTPServerReply.GOOD_USER.reply());
				
				String passCommand = command_in.readLine();
				System.out.println(passCommand+"\r");
				tokens = new StringTokenizer(passCommand);
				if(tokens.nextToken().equals("PASS")){
					if(checkString(password = tokens.nextToken()) && password.equals(SET_PASSWORD)){
						System.out.print(FTPServerReply.GOOD_PASS.reply());
						reply_out.writeBytes(FTPServerReply.GOOD_PASS.reply());
						logged_in = true;
						return true;
					}else{ /*bad password*/ return false; }
				}else{ /*bad pass command*/ return false;}
			}else{ /*bad username*/ return false; }
		}else{ /*bad user command*/ return false;}
	}
	static boolean checkString(String string){
		if(string == null){
			return false;
		}
		int i;
		char currentChar;
		for(i = 0; i<string.length(); i++){
			currentChar = string.charAt(i);
			if(currentChar > 127 || currentChar < 0){
				return false;
			}
		}
		return true;
	}
	
	static void acceptCommands() throws IOException{
		String[] replies = new String[2];
		while(true){
			int i;
			String parameter = "";
			if(server_control.isConnected()){
				String comm = command_in.readLine();
				if(comm == null){
					server_control = control_listener.accept();
					command_in = new BufferedReader(new InputStreamReader(server_control.getInputStream()));
					reply_out = new DataOutputStream(server_control.getOutputStream());
					System.out.print(FTPServerReply.START.reply());
					reply_out.writeBytes(FTPServerReply.START.reply());
					continue;
				}
				System.out.println(comm+"\r");
				StringTokenizer tokens = new StringTokenizer(comm);
				comm = tokens.nextToken();
				for(i=0; i<COMMANDS.length; i++){
					if(comm.equalsIgnoreCase(COMMANDS[i])){
						current_command = COMMANDS[i];
						break;
					}
				}
				if(tokens.hasMoreTokens()){
					parameter = tokens.nextToken();
				}
				switch(i){
				case 0:    // TYPE
					type_code = parameter;
					System.out.print(FTPServerReply.TYPE_SET_I.reply());
					reply_out.writeBytes(FTPServerReply.TYPE_SET_I.reply());
					break;
				case 1:    // SYST
					System.out.print(FTPServerReply.SYST.reply());
					reply_out.writeBytes(FTPServerReply.SYST.reply());
					break;
				case 2:    // PORT
					reply_out.writeBytes(processHostPort(parameter));
					break;
				case 3:    // RETR
					replies = retrieve(parameter);
					if(replies[1] == null){
						System.out.print(replies[0]);
						reply_out.writeBytes(replies[0]);
						continue;
					}else{
						System.out.print(replies[0]);
						reply_out.writeBytes(replies[0]);
						System.out.print(replies[1]);
						reply_out.writeBytes(replies[1]);
					}
					break;
				case 4:    // QUIT
					System.out.print(FTPServerReply.BYE.reply());
					reply_out.writeBytes(FTPServerReply.BYE.reply());
					System.exit(0);
					break;
				case 5:	   // USER
					username = parameter;
					System.out.print(FTPServerReply.GOOD_USER.reply());
					reply_out.writeBytes(FTPServerReply.GOOD_USER.reply());
					break;
				case 6:    // PASS
					password = parameter;
					System.out.print(FTPServerReply.GOOD_PASS.reply());
					reply_out.writeBytes(FTPServerReply.GOOD_PASS.reply());
					break;
				default:
					System.out.print(FTPServerReply.FOREIGN_COMMAND.reply());
					reply_out.writeBytes(FTPServerReply.FOREIGN_COMMAND.reply());
				}
			}
		}
	}
	static String processHostPort(String p){
		int i, numOfHostPortDigits = 6, digit;
		int[] hostPortDigits = new int[numOfHostPortDigits];
		StringTokenizer hostPort = new StringTokenizer(p);
		for(i=0; i<numOfHostPortDigits; i++){
			digit = Integer.parseInt(hostPort.nextToken(","));
			if(digit >= 0 && digit <= 255){
				hostPortDigits[i] = digit;
			}
		}
		host_address = Integer.toString(hostPortDigits[0])+"."+Integer.toString(hostPortDigits[1])+"."+
				Integer.toString(hostPortDigits[2])+"."+Integer.toString(hostPortDigits[3]);
		port_number = (hostPortDigits[4]*256) + hostPortDigits[5];
		
		try {
			server_data = new Socket(InetAddress.getByName(host_address), port_number);
			file_out = new DataOutputStream(server_data.getOutputStream());
		} catch (UnknownHostException e) {
			System.out.print(FTPServerReply.BAD_DATA_CONNECT.reply());
			return FTPServerReply.BAD_DATA_CONNECT.reply();
		} catch (IOException e) {
			System.out.print(FTPServerReply.BAD_DATA_CONNECT.reply());
			return FTPServerReply.BAD_DATA_CONNECT.reply();
		}
		System.out.println(FTPServerReply.GOOD_PORT.reply()+" ("+host_address+","+Integer.toString(port_number)+").\r");
		return FTPServerReply.GOOD_PORT.reply()+" ("+host_address+","+Integer.toString(port_number)+").\r\n";
	}
	static String[] retrieve(String p){
		String[] replies = new String[2];
		pathname = p;
			try {
				int bite;
				FileInputStream fin = new FileInputStream(pathname);
				replies[0] = FTPServerReply.GOOD_FILE.reply();
				while((bite = fin.read()) != -1){
					file_out.write(bite);
				}
				replies[1] = FTPServerReply.GOOD_RETR.reply();
				file_out.close();
			} catch (FileNotFoundException e) {
				replies[0] = FTPServerReply.BAD_FILE.reply();
			} catch (IOException e) {
				replies[0] = FTPServerReply.BAD_FILE.reply();
			}
		return replies;
	}

	

	

}
