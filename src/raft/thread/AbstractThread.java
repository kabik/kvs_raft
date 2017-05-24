package raft.thread;

import java.lang.Thread;

import raft.*;

public abstract class AbstractThread extends Thread {	
	protected Raft raft;
	protected boolean _halt = false, flag = true;
	
	public AbstractThread(Raft raft) {
		this.raft = raft;
	}
	
	public void halt() { _halt = true; }	
	public void standFlag() { flag = true; }
	public void sitFlag() { flag = false; }
}