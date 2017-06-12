package raft.thread;

import raft.*;

public class TimeCountThread extends AbstractThread {
	private static final int INTERVAL = 1;
	
	private long start = -1;
	
	public TimeCountThread(Raft raft) {
		super(raft);
	}

	@Override
	public void run() {
		try {
			while(!_halt) {
				while (raft.getState().isLeader() || !flag) {
					raft.resetTime();
					synchronized(this) { wait(); }
					sleep(INTERVAL);
				}
				
				if (start < 0 && raft.getLog().size() > 1) {
					start = System.currentTimeMillis();
				}
				
				if (raft.getTime() % 100 == 0) {
					//System.out.println("time:"+m.getTime() + " term: " + m.getCurrentTerm() + " state:" + m.getState());
				}
				if (raft.incrementTime() >= raft.getTimeout()) {
					System.out.println("timeout");
					raft.runFor();
				}
				sleep(INTERVAL);
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
