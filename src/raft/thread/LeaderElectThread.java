package raft.thread;

import java.io.IOException;

import raft.*;
import raft.server.RaftNode;
import raft.state.*;

public class LeaderElectThread extends AbstractThread {
	public static final int INTERVAL = 1;

	public LeaderElectThread(Raft raft) {
		super(raft);
	}

	public void init() {
		raft.resetVote();
		raft.vote(raft.getMe());
		raft.beVoted(raft.getMe());
		for (String key: raft.getRaftNodesMap().getKeySet()) {
			RaftNode rNode = raft.getRaftNodesMap().get(key);
			rNode.setRVRPCsent(false);
			rNode.initSentIndex();
		}
	}

	@Override
	public void run() {
		while(!_halt) {
			while (!raft.getState().isCandidate() || !flag) {
				try {
					//System.out.println("leader elect thread sleep");
					synchronized (this) { wait(); }
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				init();
			}
			for (String key: raft.getRaftNodesMap().getKeySet()) {
				RaftNode rNode = raft.getRaftNodesMap().get(key);
				if (!rNode.hasSentRVRPC() && !rNode.isMe()) {
					// command, candidate's term, candidate's IP, lastLogIndex, lastLogTerm
					//System.out.println("send RV RPC to " + rNode);
					try {
						raft.send(rNode, ProcessMessageThread.REQUEST_VOTE_CHAR + " "+ raft.getCurrentTerm() +
								" " + raft.getLog().lastIndex() + " " + raft.getLog().lastLogTerm());
						rNode.setRVRPCsent(true);
					} catch (IOException e) {
						System.out.println("fail in sending Request Vote RPC to " + rNode);
						//e.printStackTrace();
					}
				}
			}
			if (raft.getVote() > raft.getServerNum() / 2) {
				System.out.println("victory");
				raft.setState(new LeaderState());
				raft.term(false);
				raft.resetNextIndex();
				raft.resetMatchIndex();
				init();
			}

			try {
				sleep(INTERVAL);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}