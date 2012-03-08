package ca.sfu.cmpt431.facility;

import java.io.Serializable;

import ca.sfu.network.MessageSender;

public class Comrade  implements Serializable{
	
	private static final long serialVersionUID = 1L;
	
	public int id;
	public MessageSender sender;
	
	public Comrade(int id, MessageSender sender) {
		this.id = id;
		this.sender = sender;
	}
	
}
