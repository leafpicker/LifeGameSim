package ca.sfu.cmpt431.message.join;

import ca.sfu.cmpt431.message.Message;
import ca.sfu.cmpt431.message.MessageCodeDictionary;

public class JoinConfirmMsg extends Message {
	
	private static final long serialVersionUID = 1L;
	
	private int myId;

	public JoinConfirmMsg(int newId) {
		super(0, MessageCodeDictionary.JOIN_CONFIRM);
		this.myId = newId;
	}
	
	public int getMyId() {
		return myId;
	}

}
