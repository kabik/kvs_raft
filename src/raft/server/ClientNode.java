package raft.server;

import raft.Raft;

public class ClientNode extends Server {
	private String mode;
	private int waitIndex;
	private boolean isWaitingForCommit;

	public ClientNode(Raft raft, String hostname) {
		super(raft, hostname);
		this.waitIndex = -1;
		this.isWaitingForCommit = false;
	}
	
	public void setMode(String mode) {
		this.mode = mode;
	}
	public String getMode() {
		return mode;
	}
	public void waitForCommit(int waitLogIndex) {
		this.waitIndex = waitLogIndex;
		this.isWaitingForCommit = true;
	}
	public void waitForCommit() {
		this.isWaitingForCommit = true;
	}
	public void releaseWait() {
		this.isWaitingForCommit = false;
	}
	public int getWaitIndex() {
		return waitIndex;
	}
	public boolean isWaitingForCommit() {
		return isWaitingForCommit;
	}
}
