package raft.server;

import raft.Raft;

public class ClientNode extends Server {
	String mode;
	int waitIndex;
	int lastWaitIndex;

	public ClientNode(Raft raft, String hostname) {
		super(raft, hostname);
		this.waitIndex = -1;
		this.lastWaitIndex = -1;
	}
	
	public void setMode(String mode) {
		this.mode = mode;
	}
	public String getMode() {
		return mode;
	}
	public void setWaitIndex(int waitLogIndex) {
		this.waitIndex = waitLogIndex;
	}
	public int getWaitIndex() {
		return waitIndex;
	}
}
