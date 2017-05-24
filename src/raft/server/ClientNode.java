package raft.server;

import raft.Raft;

public class ClientNode extends Server {
	public static final char NODE_TYPE = 'c';
	String mode;
	int waitLogIndex;

	public ClientNode(Raft raft, String hostname) {
		super(raft, hostname, ClientNode.NODE_TYPE);
		this.waitLogIndex = -1;
	}
	
	public void setMode(String mode) { this.mode = mode;}
	public String getMode() { return mode; }
	public void setWaitLogIndex(int waitLogIndex) { this.waitLogIndex = waitLogIndex; }
	public int getWaitLogIndex() { return waitLogIndex; }
}
