package raft.thread;

import raft.*;
import raft.server.RaftNode;

public class CheckThread extends AbstractThread {
	//private static final int INTERVAL = 50;
	//public static final int GROUP_COMMIT_TIMEOUT = 10000; // ms
	
	public CheckThread(Raft raft) {
		super(raft);
	}
	
	@Override
	public void run() {
		//long start = System.currentTimeMillis();
		try {
			while(!_halt) {
				while (raft.getCommitIndex() == raft.getLog().lastIndex() && raft.getCommitIndex() == raft.getLastApplied()) {
					synchronized (this) {
						wait();
					}
				}
				if (raft.getCommitIndex() > raft.getLastApplied()) {					
					raft.apply();
				}
				if (raft.getState().isLeader()) {
					outside: for (int n = raft.getLog().size()-1;
							n > raft.getCommitIndex() && raft.getLog().get(n).getTerm() == raft.getCurrentTerm();
							n--) {
						int count = 1; // own
						//for (RaftNode rNode : raft.getRaftNodesMap().getMap().values()) {
						for (String key: raft.getRaftNodesMap().getKeySet()) {
							RaftNode rNode = raft.getRaftNodesMap().get(key);
							//if (rNode.getMatchIndex() >= n) {
							if (rNode.getWrittenIndex() >= n) {
								count++;
							}
							if (count > raft.getServerNum() / 2) {
								raft.setCommitIndex(n);
								break outside;
							}
						}
						if (count > raft.getServerNum() / 2) {
							raft.setCommitIndex(n);
							break outside;
						}
					}
				}
				
				/*long end = System.currentTimeMillis();
				if ((end - start) > GROUP_COMMIT_TIMEOUT) {
					try {
						raft.getLog().sync();
						start = end;
					} catch (IOException e) {
						e.printStackTrace();
					}
				}*/
				
				//sleep(INTERVAL);
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}