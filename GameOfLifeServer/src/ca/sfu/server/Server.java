package ca.sfu.server;
import java.io.IOException;
import java.util.ArrayList;

import ca.sfu.cmpt431.facility.Board;
import ca.sfu.cmpt431.facility.BoardOperation;
import ca.sfu.cmpt431.facility.Comrade;
import ca.sfu.cmpt431.facility.Outfits;
import ca.sfu.cmpt431.message.Message;
import ca.sfu.cmpt431.message.MessageCodeDictionary;
import ca.sfu.cmpt431.message.join.JoinRequestMsg;
import ca.sfu.cmpt431.message.join.JoinSplitMsg;
import ca.sfu.cmpt431.message.leave.LeaveReceiverMsg;
import ca.sfu.cmpt431.message.merge.MergeLastMsg;
import ca.sfu.cmpt431.message.regular.RegularBoardReturnMsg;
import ca.sfu.cmpt431.message.regular.RegularNextClockMsg;
import ca.sfu.cmpt431.message.regular.RegularOutfitMsg;
import ca.sfu.network.MessageReceiver;
import ca.sfu.network.MessageSender;
import ca.sfu.network.SynchronizedMsgQueue.MessageWithIp;


public class Server{
	
	protected static final int LISTEN_PORT = 6560;
	private MessageReceiver Receiver;

	private ArrayList<MessageSender> newClientSender = new ArrayList<MessageSender>();
	private ArrayList<Comrade>  regedClientSender = new ArrayList<Comrade>();
	
	private ArrayList<Integer> toLeave = new ArrayList<Integer>();
	
	private int waiting4confirm = 0;
	private int nextClock = 0;
	
	private int status;
	
	private int phase;
	private static final int COMPUTE = 0;
	private static final int ADD = 1;
	private static final int LEAVE = 2;
	
	//for test!
	private static boolean TEST = false; //default: false
	private static final int lowerbound = 8; //default: 1
	private static int test_Cycle = 0;
	
	private static final boolean AUTOMATION = false;
	private static final int automation_cycle = 20;
	private static final int upperbound = Integer.MAX_VALUE;
	
	private static final boolean ClientRandom = false;
	
	/* UI widgets */
	MainFrame frame = null;
	InformationPanel infoPanel = null;
	
	public Server() throws IOException {
		Receiver = new MessageReceiver(LISTEN_PORT);
		status = 0;
	}
	
	protected void startServer() throws IOException, ClassNotFoundException, InterruptedException
	{
		// UI

//		Board b = BoardOperation.LoadFile("Patterns/HerschelLoop2.lg");
//		Board b = BoardOperation.LoadFile("Patterns/oscillator1.lg");
//		Board b = BoardOperation.LoadFile("Patterns/HerschelLoop.lg");
		Board b = new Board(256, 256);
		b = BoardOperation.Randomize(b, 0.1);

		System.out.println("UI");
		frame = new MainFrame(b, 800, 800);
		infoPanel = new InformationPanel();
		
		MessageWithIp m;
		int result = -1;
		
		while(true) {
			if(!Receiver.isEmpty()) {
				m = Receiver.getNextMessageWithIp();
				
//				System.out.println(status);
				
				switch(status) {
					//waiting for first client
					case 0:
						handleNewAddingLeaving(m,1);
						
						handlePending(2);
						//send it the outfit
						
						Outfits o;
						if(ClientRandom){
							o = new Outfits(0,nextClock,0,0,new Board());
							o.myBoard.bitmap = null;
							o.myBoard.height = b.height;
							o.myBoard.width = b.width;
							regedClientSender.get(0).sender.sendMsg(new RegularOutfitMsg(-1, -1, o));
						}
						else
							regedClientSender.get(0).sender.sendMsg(new RegularOutfitMsg(-1, -1, new Outfits(0,nextClock,0,0,b)));
						waiting4confirm++;
						status = 2;
						break;
					
					case 1:
						if(TEST){
							handleNewAddingLeaving(m,1);
							
							if(regedClientSender.size()<lowerbound){
								//waiting for more clients
								if(!newClientSender.isEmpty()){
									handlePending(2);
								}
//								status = 2; //waiting for a confirm
								break;
							}
						}
						else{
							//error
						}
						break;
						
					//wait for the confirm
					//start a cycle
					case 2:
						if(handleNewAddingLeaving(m,2))
							break;
										
						handleConfirm(m,3); //expect only one message responding for JoinOutfitsMsg
						
						if(!newClientSender.isEmpty()){
							if(handlePending(2))
//							status = 2;
								break;
						}
						
						if(waiting4confirm == 0){
							//send you a start
							
							if(TEST){
								if(regedClientSender.size()<lowerbound){
									//waiting for more clients
									if(!newClientSender.isEmpty()){
										if(handlePending(2))
//										status = 2;
											break;
									}
									status = 1; //waiting for a new client
									break;
								}
							}
							
//							System.out.println("sending start");
							
							infoPanel.setCycleNum(frame.automataPanel.getCycle());
							
							for (Comrade var : regedClientSender) {
								var.sender.sendMsg(new RegularNextClockMsg(nextClock));
								waiting4confirm++;
							}
							
							System.out.println("Start time: " + System.currentTimeMillis());
							
							status = 3;
							phase = COMPUTE;
						}
						
						break;
						
					//waiting for the client to send the result back
					//handle new adding or
					//restart next cycle
					case 3:
						if(handleNewAddingLeaving(m,3))
							break;
						
						if(phase == COMPUTE){
							handleNewBoardInfo(m,b,3);
							if(waiting4confirm!=0){
								break;
							}
							
//							Thread.sleep(50);
							if(!TEST)
								frame.repaint();
							
							if(AUTOMATION){
								test_Cycle++;
								if(test_Cycle%automation_cycle == 0)
								{
									System.out.println("Time "+test_Cycle+":"+System.currentTimeMillis());
									if((upperbound-lowerbound+1)*automation_cycle==test_Cycle)
										System.exit(0);
								}
								frame.automataPanel.setCycle(test_Cycle);
							}
							else if(TEST){
								test_Cycle++;
								if(test_Cycle == 10)
								{
									System.out.println(System.currentTimeMillis());
									System.exit(0);
								}
								frame.automataPanel.setCycle(test_Cycle);
							}
							
							
							infoPanel.setCellNum(frame.automataPanel.getCell());
							infoPanel.setLifeNum(frame.automataPanel.getAlive());
							
							infoPanel.setTargetNum("localhost");
							
							phase = LEAVE;
//							BoardOperation.Print(b);
//							System.out.println("repaint");
						}
						
						if(phase == LEAVE){
							
							//deal with the confirm
							//manage the heap
							if(((Message)m.extracMessage()).getMessageCode()==MessageCodeDictionary.REGULAR_CONFIRM){
								System.out.println("result:"+result);
								if(result == 1){
									int s = regedClientSender.size();
									
//									System.out.println(regedClientSender.get(s-2).id);
									
									regedClientSender.get(s-1).sender.sendMsg(new LeaveReceiverMsg(MessageCodeDictionary.ID_SERVER, 0, ""));
									regedClientSender.get(s-1).sender.close();
									regedClientSender.remove(s-1);
									
									Comrade c = regedClientSender.get(s-2);
									regedClientSender.remove(s-2);
									regedClientSender.add(0, c);
								}
								else if(result == 2){
									int s = regedClientSender.size();
									
//									System.out.println(regedClientSender.get(s-2).id);
									//you can leave now
									regedClientSender.get(s-2).sender.sendMsg(new LeaveReceiverMsg(MessageCodeDictionary.ID_SERVER, 0, ""));
									
									Comrade c = regedClientSender.get(s-1);
									regedClientSender.remove(s-1);
									
									int id = regedClientSender.get(s-2).id;
									regedClientSender.get(s-2).sender.close();
									regedClientSender.remove(s-2);
									
									c.id = id;
									regedClientSender.add(0, c);
									
								}
								else if(result == 3){
									int cid = toLeave.get(0);
									int index = findClient(cid);
									int s = regedClientSender.size();
									
									int new_cid = regedClientSender.get(s-1).id;
									int new_port = regedClientSender.get(s-1).port;
									String new_ip = regedClientSender.get(s-1).ip;
									
									//send your outfit to the last node
									regedClientSender.get(index).sender.sendMsg(new LeaveReceiverMsg(new_cid, new_port, new_ip));
									regedClientSender.get(index).sender.close();
									regedClientSender.set(index, regedClientSender.get(s-1));
									regedClientSender.get(index).id = cid;
									
									System.out.println("replace:"+regedClientSender.get(index).port);
									
									regedClientSender.remove(s-1);
									Comrade c = regedClientSender.get(s-2);
									regedClientSender.remove(s-2);
									regedClientSender.add(0, c);
									
									for(int i=0; i<regedClientSender.size(); i++){
										System.out.print(regedClientSender.get(i).port+" ");
									}
									System.out.println();
									
									//wait for confirm
									result = 6;
									break;
								}
								else if(result == 0){
									//get the confirm
									//nothing to do
								}
								else if(result == 6){
									//do nothing
								}
								else{
									//error
									System.out.println("error.");
								}
								toLeave.remove(0);
							}
							
							infoPanel.setClientNum(regedClientSender.size());
							
							if((result=handleLeaving())!=-1){
								//4, no client now, go to status 0 pls
								if(result == 4){
									toLeave.remove(0);
									System.out.println("go to status 0");
									status = 0;
								}
								else{
									//wait for a confirm
									status = 3;
								}
							}
							
							if(toLeave.isEmpty() && status != 0)
								phase = ADD;
							else
								break;
						}
						
						
						//handle adding
						if(handlePending(2)){
							status = 2;
							break;
						}
						
						//start
						if(waiting4confirm==0){
							for (Comrade var : regedClientSender) {
								
								if(TEST){
									if(regedClientSender.size()<lowerbound){
										//waiting for more clients
										if(!newClientSender.isEmpty()){
											if(handlePending(2))
//											status = 2;
												break;
										}
										status = 1; //waiting for a new client
										break;
									}
								}
								
								var.sender.sendMsg(new RegularNextClockMsg(nextClock));
								waiting4confirm++;
							}
//							System.out.println("sending start");
							
							infoPanel.setCycleNum(frame.automataPanel.getCycle());
							phase = COMPUTE;
						}
						break;
					default:
						break;
				}
			}
		}
	}
	
	//store all the adding request into an array
	protected boolean handleNewAddingLeaving(MessageWithIp m, int nextStatus) throws IOException{
		//check if m is a new adding request message
		Message msg = (Message) m.extracMessage();
		if(msg.getMessageCode()==MessageCodeDictionary.JOIN_REQUEST){
			JoinRequestMsg join = (JoinRequestMsg)m.extracMessage();
			newClientSender.add(new MessageSender(m.getIp(), join.clientPort));
			System.out.println("adding a new client to pending list");
			//if it is a new adding request, we need to go to nextStatus
			//most time it should be the same status
			status = nextStatus;
			return true;
		}
		else if(msg.getMessageCode()==MessageCodeDictionary.REGULAR_BOARD_RETURN){
			RegularBoardReturnMsg r = (RegularBoardReturnMsg)msg;
			if(r.isLeaving){			
				toLeave.add(msg.getClientId());
				System.out.println("client " + msg.getClientId() + " want to leave, pending now");
				return false;
			}
		}
		return false;
	}
	
	protected int handleLeaving() throws IOException{
		if(toLeave.isEmpty())
			return -1;
		
		int index = 0;
		
		System.out.println("to Leave");
		for(int i=0; i<toLeave.size(); i++){
			System.out.print(toLeave.get(i)+" ");
			if(findClient(toLeave.get(i))>findClient(toLeave.get(index)))
				index = i;
		}
		int cid = toLeave.get(index);
		
		toLeave.remove(index);
		toLeave.add(0, cid);
		
		System.out.println("now handling "+cid);
		
		if(newClientSender.size()!=0){
			//ask a new client to replace it immediately
			regedClientSender.get(findClient(cid)).sender.sendMsg(new LeaveReceiverMsg(cid, newClientSender.get(0).hostListenningPort, newClientSender.get(0).hostIp));
			regedClientSender.get(findClient(cid)).sender.close();
			regedClientSender.set(findClient(cid), new Comrade(cid, newClientSender.get(0).hostListenningPort, newClientSender.get(0).hostIp, newClientSender.get(0)));
			newClientSender.remove(0);
			
			System.out.println("new adding, replace");
			//confirm
			return 0;
		}
		else if(regedClientSender.size()==1){
			//there is only one client and no adding
			//ask him to leave directly
			regedClientSender.get(0).sender.sendMsg(new LeaveReceiverMsg(MessageCodeDictionary.ID_SERVER, 0, ""));
			regedClientSender.get(findClient(cid)).sender.close();
			regedClientSender.remove(findClient(cid));
			
			System.out.println("only one client, leave directly");
			
			//no confirm, everything done, but you need to wait for a client to start
			return 4;
		}
		else if(isLastPair(cid)!=-1){
			//it is the last node, or the pair of last node
			//ask the last pair merge
			int s = regedClientSender.size();
			int pair_index = (s%2==0)?((s-4)>=0?(s-4):-1):0;
			System.out.println("pair index:"+pair_index);
			
			if(pair_index!=-1 && isLastPair(regedClientSender.get(pair_index).id)!=-1)
				pair_index = -1; //you pair can not be your neighbour, occurs when there is 2 clients
			
			System.out.println("pair index:"+pair_index);
			
			int pair_cid = -1;
			String pair_ip = "";
			int pair_port = -1;
			if(pair_index!=-1){
				pair_cid = regedClientSender.get(pair_index).id;
				pair_ip = regedClientSender.get(pair_index).ip;
				pair_port = regedClientSender.get(pair_index).port;
			}
			
			System.out.println("last pair handle pending:"+pair_cid+","+cid);
			regedClientSender.get(regedClientSender.size()-1-isLastPair(cid)).sender.sendMsg(new MergeLastMsg(pair_cid, pair_ip, pair_port));
			//wait for a confirm, still need a LeaveReceiverMsg
			return isLastPair(cid)+1; //1 if last or 2 if second last
		}
		else{
			//ask the last node merge first,give it a new pair id
			//ask the last node to replace
			int s = regedClientSender.size();
			
			int pair_index = (s%2==0)?((s-4)>=0?(s-4):-1):0;
			
			int pair_cid = -1;
			String pair_ip = "";
			int pair_port = -1;
			if(pair_index!=-1){
				pair_cid = regedClientSender.get(pair_index).id;
				pair_ip = regedClientSender.get(pair_index).ip;
				pair_port = regedClientSender.get(pair_index).port;
			}
			
			System.out.println("normal merge handle pending(p/u):"+pair_cid+","+cid);
			regedClientSender.get(regedClientSender.size()-1).sender.sendMsg(new MergeLastMsg(pair_cid, pair_ip, pair_port));
			//wait for a confirm, still need a LeaveReceiverMsg
			return 3;
		}
	}
	
	private int findClient(int cid){
		for(int i=0; i<regedClientSender.size(); i++){
			if(regedClientSender.get(i).id == cid){
				return i;
			}
		}
		return -1;
	}
	
	private int isLastPair(int cid){
		int s = regedClientSender.size();
		if(regedClientSender.get(s-1).id == cid)
			return 0;
		else if(regedClientSender.get(s-2).id == cid)
			return 1;
		else
			return -1;
	}
	
	//deal with the pending adding request
	//manage the heap
	protected boolean handlePending(int st) throws IOException{
		
		//if in automatin test
		if(AUTOMATION && TEST){
			int num_pc = test_Cycle/automation_cycle + lowerbound;
			
			if(num_pc>upperbound)
				return false;
			else if(regedClientSender.size()==num_pc){
				return false;
			}
		}
		
		if(!newClientSender.isEmpty()){
			int cid = regedClientSender.size();
			
			//manage the heap
			if(cid!=0){ //not the first client
				Comrade c = regedClientSender.get(0); //get it down one level
				
				//c is the pair
				int mode;
				if((((int)(Math.log(2*cid+1)/Math.log(2)))%2)!=0)
					mode = MessageCodeDictionary.SPLIT_MODE_HORIZONTAL;
				else
					mode = MessageCodeDictionary.SPLIT_MODE_VERTICAL;
				
//				System.out.println(cid);
//				System.out.println((Math.log(2*cid+1)/Math.log(2))%2);
//				System.out.println("mode"+mode);
				System.out.println("Sending a split command to "+ c.id+", new client id: "+cid+", split mode: "+(mode==0?"vertical":"horizontal"));
				
//				System.out.println("send JoinSplitMsg");
//				System.out.println(newClientSender.get(0).hostIp);
				c.sender.sendMsg(new JoinSplitMsg(cid, newClientSender.get(0).hostListenningPort, newClientSender.get(0).hostIp, mode));
				
				regedClientSender.remove(0);
				regedClientSender.add(c);
				regedClientSender.add(new Comrade(cid, newClientSender.get(0).hostListenningPort, newClientSender.get(0).hostIp, newClientSender.get(0)));
				waiting4confirm++;
			}
			else{
				regedClientSender.add(new Comrade(cid, newClientSender.get(0).hostListenningPort, newClientSender.get(0).hostIp, newClientSender.get(0)));
				//regedClientSender.get(cid).sender.sendMsg(new RegularConfirmMsg(-1));
			}
			
			//remove the pending one
			newClientSender.remove(0);
			
			
			System.out.println("register a new client");
			
			infoPanel.setClientNum(regedClientSender.size());
			
			status = st;
			return true;
		}
		return false;
	}
	
	protected void handleNewBoardInfo(MessageWithIp m, Board b, int nextStatus){
		waiting4confirm--;
//		System.out.println("getting a result");
		
		if(waiting4confirm==0)
			status = nextStatus;
		
		if(TEST) {
			return;
		}
		
		RegularBoardReturnMsg r = (RegularBoardReturnMsg)m.extracMessage();
		BoardOperation.Merge(b, r.board, r.top, r.left);
//		b = (Board)m.extracMessage();
	}
	
	//getting a new confirm message, if there is no waiting confirm, go to nextStatus
	protected void handleConfirm(MessageWithIp m, int nextStatus){
		waiting4confirm--;
//		System.out.println("getting a confirm");
		if(waiting4confirm==0)
			status = nextStatus;
	}
}