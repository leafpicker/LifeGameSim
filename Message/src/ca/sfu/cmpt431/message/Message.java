package ca.sfu.cmpt431.message;

import java.io.Serializable;

public class Message implements Serializable{
	
	private static final long serialVersionUID = 1L;
	
	private int clientId;
	private int messageCode;
	
	public Message() {
		clientId = -1;
		messageCode = MessageCodeDictionary.SB_MESSAGE;
	}
	
	public Message(int cid, int mcode) {
		clientId = cid;
		messageCode = mcode;
	}
	
	public int getClientId() {
		return clientId;
	}
	
	public int getMessageCode() {
		return messageCode;
	}

}
