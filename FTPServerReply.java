
public enum FTPServerReply {
	GOOD_FILE("150", "File status okay."), 
	GOOD_COMMAND("200", "Command OK."), 
	TYPE_SET_I("200", "Type set to I."),
	TYPE_SET_A("200", "Type set to A."),
	//the host-port is added on later, at which time, the CRLF is added
	GOOD_PORT("200", "Port command successful"),
	SYST("215", "UNIX Type: L8."),
	START("220", "COMP 431 FTP server ready."),
	BYE("221", "Goodbye."),
	GOOD_PASS("230", "Guest login OK."),
	GOOD_RETR("250", "Requested file action completed."),
	GOOD_USER("331", "Guest access OK, send password."),
	BAD_DATA_CONNECT("425", "Can not open data connection."),
	BAD_COMMAND("500", "Syntax error, command unrecognized."),
	BAD_PARAMETER("501", "Syntax error in parameter."),
	BAD_SEQUENCE("503", "Bad sequence of commands."),
	NO_LOGIN("530", "Not logged in."),
	BAD_FILE("550", "File not found or access denied."),
	FOREIGN_COMMAND("502", "Command not implemented.");

	
	public String reply_code;
	public String reply_text;
	private final String CRLF = "\r\n";
	
	
	FTPServerReply(String replyCode, String replyText){
		reply_code = replyCode;
		if(replyText.equals("Port command successful")){
			reply_text = replyText;
		}else{
			reply_text = replyText.concat(CRLF);
//			reply_text = replyText.concat("\n");
		}
	}
	
	String reply(){
		return reply_code+" "+reply_text;
	}
}
