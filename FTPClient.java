import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.StringTokenizer;

public class FTPClient {
	
	private static final String[] REQUESTS = {"CONNECT", "GET", "QUIT"};
	private static final String SET_USER = "USER anonymous\r\n";
	private static final String SET_PASS = "PASS guest@\r\n";
	private static final String SET_SYST = "SYST\r\n";
	private static final String SET_TYPE_CODE = "TYPE I\r\n";
	
	private static boolean connected;
	private static Socket client_control;
	private static DataOutputStream command_out;
	private static BufferedReader reply_in;
	private static ServerSocket data_listener;
	private static int file_num = 1;
	
	private static String current_request;
	private static String server_host;
	private static int server_port;
	private static String pathname;
	private static int port_num = 1985;
	
	private static String error_token;
	
	private static String reply_code;
	private static String reply_text;
	
	
	public static void main(String[] args) throws UnknownHostException, IOException{
		for(String s:args){
			port_num = Integer.parseInt(s);
		}
		
		
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String input;
		while((input = br.readLine()) != null){
			System.out.println(input);
			if(checkRequest(input)){
				processRequest();
			}else{
				System.out.println("ERROR -- "+error_token);
				continue;
			}
		}
	}
	
//************************************************Client Request Syntax Check************************************************
	static boolean checkRequest(String input){
		StringTokenizer tokens = new StringTokenizer(input);
		int i;
		String request = tokens.nextToken();
		for(i=0; i<REQUESTS.length; i++){
			if(request.equalsIgnoreCase(REQUESTS[i])){
				break;
			}
		}
		if(i > 2){
			error_token = "request";
			return false;
		}
		if(tokens.hasMoreTokens()){
			switch(i){
			case 0:  // CONNECT
				current_request = REQUESTS[0];
				String serverHost = tokens.nextToken();
				if(checkServerHost(serverHost)){
					server_host = serverHost;
					if(tokens.hasMoreTokens()){
						String serverPort = tokens.nextToken();
						if(checkServerPort(serverPort)){
							server_port = Integer.parseInt(serverPort);
							return true;
						}else{
							error_token = "server-port"; return false;
						}
					}else{
						error_token = "server-port"; return false;
					}
				}else{
					error_token = "server-host"; return false;
				}
			case 1:  // GET
				current_request = REQUESTS[1];
				if(!connected){
					error_token = "expecting CONNECT"; return false;
				}
				String pathName = tokens.nextToken("\r\n");
				i=0; 
				while(pathName.charAt(i) == 32){
					i++;
				}
				pathName = pathName.substring(i);
				if(checkPathname(pathName)){
					pathname = pathName;
					return true;
				}else{
					error_token = "pathname"; return false;
				}
			default:
				error_token = "request"; return false;
			}
		}else{
			switch(i){
			case 0:
				error_token = "server-host";
				return false;
			case 1:
				error_token = "pathname";
				return false;
			case 2:
				if(input.equals(request)){   // check for whitespace after QUIT
					current_request = REQUESTS[2];
					return true;
				}else{
					error_token = "request";
					return false;
				}
			default:
				error_token = "request";
				return false;
			}
		}
	}
	
	
	static boolean checkServerHost(String serverHost){
		StringTokenizer sHostTokens = new StringTokenizer(serverHost, "."); //check domain(s)
		int i=0;
		while(sHostTokens.hasMoreTokens()){ //check elements
			String token = sHostTokens.nextToken();
			if(checkA(token.charAt(0))){
				for(i=1; i<token.length(); i++){
					if(!checkA(token.charAt(i)) && !checkD(token.charAt(i))){
						if(token.charAt(i) == 45){continue;}  //my server name is Josephs-MacBook-Pro-2
						return false;
					}
				}
			}else{
				return false;
			}
		}
		return true;
	}
	static boolean checkServerPort(String serverPort){
		int serverP = Integer.parseInt(serverPort);
		if(serverP >= 0 && serverP <=65535){
			return true;
		}
		return false;
	}
	static boolean checkPathname(String pathname){
		if(pathname == null){
			return false;
		}
		int i;
		char currentChar;
		for(i=0; i<pathname.length(); i++){
			currentChar = pathname.charAt(i);
			if(currentChar > 127 || currentChar < 0){
				return false;
			}
		}
		return true;
	}
	
	static boolean checkA(char letter){ 
		if(letter >= 65 && letter <= 122){
			if(letter > 90 && letter < 97){
				return false;
			}else{
				return true;
			}
		}else{
			return false;
		}
	}
	static boolean checkD(char digit){
		if(digit >= 48 && digit <= 57){
			return true;
		}else{
			return false;
		}
	}
//***************************************************************************************************************************

	
//*************************************************Client Request Processing*************************************************	
	static void processRequest(){
		switch(current_request){
		case "CONNECT":
//			System.out.println(current_request+" accepted for FTP server at host "+server_host+" and port "+server_port);
			try {
				//for the greeting
				if(client_control != null){
					if(client_control.isBound()){
						client_control.close();
						command_out.close();
						reply_in.close();
					}
				}
				client_control = new Socket(server_host, server_port);
				command_out = new DataOutputStream(client_control.getOutputStream());
				reply_in = new BufferedReader(new InputStreamReader(client_control.getInputStream()));
				System.out.println(current_request+" accepted for FTP server at host "+server_host+" and port "+server_port);
				processServerReply(reply_in.readLine());
				if(Integer.parseInt(reply_code) > 399){break;}
				
				System.out.print(SET_USER);
				command_out.writeBytes(SET_USER);
				processServerReply(reply_in.readLine());
				if(Integer.parseInt(reply_code) > 399){break;}
				
				System.out.print(SET_PASS);
				command_out.writeBytes(SET_PASS);
				processServerReply(reply_in.readLine());
				if(Integer.parseInt(reply_code) > 399){break;}
				connected = true;
				
				System.out.print(SET_SYST);
				command_out.writeBytes(SET_SYST);
				processServerReply(reply_in.readLine());
				if(Integer.parseInt(reply_code) > 399){break;}
				
				System.out.print(SET_TYPE_CODE);
				command_out.writeBytes(SET_TYPE_CODE);
				processServerReply(reply_in.readLine());
				if(Integer.parseInt(reply_code) > 399){break;}
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
//				e.printStackTrace();
				System.out.println("CONNECT failed");
			}
			break;
		case "GET":
			if(!connected){
				break;
			}
			System.out.println(current_request+" accepted for "+pathname);
			try {
				String myIP = InetAddress.getLocalHost().getHostAddress();
				myIP = myIP.replace('.', ',');
				
				port_num++;
				int portNum1 = port_num/256;
				int portNum2 = port_num%256;
				String hostPort = myIP+","+Integer.toString(portNum1)+","+Integer.toString(portNum2);
				
				data_listener = new ServerSocket(port_num);
				
				System.out.println("PORT "+hostPort+"\r");
				command_out.writeBytes("PORT "+hostPort+"\r\n"); command_out.flush();
//				Socket client_data = data_listener.accept();
				processServerReply(reply_in.readLine());
				if(Integer.parseInt(reply_code) > 399){break;}
				
				
				
//				BufferedReader data_in = new BufferedReader(new InputStreamReader(client_data.getInputStream()));
				System.out.println("RETR "+pathname+"\r");
				command_out.writeBytes("RETR "+pathname+"\r\n"); command_out.flush();
				processServerReply(reply_in.readLine());
				if(Integer.parseInt(reply_code) > 399){break;}	
				
				Socket client_data = data_listener.accept();
				BufferedReader data_in = new BufferedReader(new InputStreamReader(client_data.getInputStream()));
				
				File output_file = new File("./retr_files/file"+Integer.toString(file_num++));
				FileOutputStream file_out = new FileOutputStream(output_file);
				while(data_in.ready()){
					file_out.write(data_in.read());
				}
				
				client_data.close();
				
				processServerReply(reply_in.readLine());
				if(Integer.parseInt(reply_code) > 399){break;}
			} catch (IOException e) {
				System.out.println("GET failed, FTP-data port not allocated.");
			}
			break;
		case "QUIT":
			System.out.println(current_request+" accepted, terminating FTP client");
			try {
				System.out.println("QUIT\r");
				command_out.writeBytes("QUIT\r\n");
				processServerReply(reply_in.readLine());
				if(Integer.parseInt(reply_code) > 399){break;}
				System.exit(0);
			} catch (IOException e) { //idk what to do here
				// TODO Auto-generated catch block
//				e.printStackTrace();
				System.out.println("QUIT failed.");
			}
			break;
		}
	}
//***************************************************************************************************************************

	static void processServerReply(String reply){
		String[] serverReply = reply.split(" ", 2);
		for(int i=0; i<serverReply.length; i++){
			if(i == 0){
				reply_code = serverReply[i];
			}else if(i == 1){
				reply_text = serverReply[i];
			}
		}
		System.out.println("FTP reply "+reply_code+" accepted. Text is : "+reply_text);
	}
}
