package raft.server;

import raft.Raft;

public class ClientNode extends Server {
	public static final char NODE_TYPE = 'c';
	String mode;
	int waitIndex;
	int lastWaitIndex;

	public ClientNode(Raft raft, String hostname) {
		super(raft, hostname, ClientNode.NODE_TYPE);
		this.waitIndex = -1;
		this.lastWaitIndex = -1;
	}
	
	public void setMode(String mode) { this.mode = mode;}
	public String getMode() { return mode; }
	//public void setWaitLogIndex(int waitLogIndex) { this.waitLogIndex = waitLogIndex; }
	public void setWaitIndex(int waitLogIndex) {
		/*if (waitLogIndex > 0) {
			lastWaitIndex = waitLogIndex;
		}
		*/
		this.waitIndex = waitLogIndex;
	}
	public int getWaitIndex() { return waitIndex; }
	
	 // debug
	public int lastWaitIndex() { return lastWaitIndex; }
}
