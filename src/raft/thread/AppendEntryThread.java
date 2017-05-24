package raft.thread;

import java.io.IOException;

import raft.*;
import raft.server.*;

public class AppendEntryThread extends AbstractThread {
	// heartbeat
	//public static final int APPENDENTRY_INTERVAL = 1;
	public static final int HEARTBEAT_INTERVAL = 50;
	public static final int MAX_ENTRY = 1;	// The number of entries in one RPC

	public AppendEntryThread(Raft raft) {
		super(raft);
	}

	private void appendEntry(RaftNode rNode) throws IOException {
		//if (rNode.isWaitingForAcception()) { return; }

		//long start = System.nanoTime();

		int prevLogIndex = rNode.getNextIndex() - 1;
		int prevLogTerm = (prevLogIndex < 0) ? -1 : raft.getLog().get(prevLogIndex).getTerm();

		/* format
		 * <command> <leader's term> <prevLogIndex> <prevLogTerm> <leaderCommit> <log entry,log entry...>
		 */
		StringBuilder basicMessageSB = new StringBuilder();
		basicMessageSB.append(ProcessMessageThread.APPEND_ENTRY_STR).append(' ').append(raft.getCurrentTerm())
		.append(' ').append(prevLogIndex)
		.append(' ').append(prevLogTerm).append(' ').append(raft.getCommitIndex()).append(' ');
		
		if (rNode.isWaitingForAcception()) {
			raft.send(rNode, basicMessageSB.toString());
			return;
		}

		// entries
		StringBuilder entrySB = new StringBuilder();
		int index = rNode.getNextIndex() - 1;
		for (int i = 0; i < MAX_ENTRY && raft.getLog().lastIndex() > index; i++) {
			index++;
			if (i > 0) { entrySB.append(','); }
			entrySB.append(raft.getLog().get(index));
		}

		//System.out.println("send Append Entry RPC to " + rNode.getHostname() + " " + entrySB.toString());
		if (entrySB.length() > 0) { System.out.println(entrySB); } //
		String message = basicMessageSB.append(entrySB).toString();

		long start = System.currentTimeMillis();
		
		raft.send(rNode, message);
		rNode.setSentIndex(Math.min(raft.getLog().lastIndex(), index));

		long end = System.currentTimeMillis();
		if (end - start > 10)
			System.out.println("AERPCs " + (end - start) + " ms "+ rNode.getIPAddress());
		//long end = System.nanoTime();
		//long microSec = TimeUnit.MICROSECONDS.convert( System.nanoTime() - start, TimeUnit.NANOSECONDS );
		//System.out.println( microSec + " microsec passed" );
	}

	@Override
	public void run()  {
		long start = System.currentTimeMillis();
		while(!_halt) {
			while (!raft.getState().isLeader() || !flag) {
				try {
					synchronized(this) { wait(); }
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			long end = System.currentTimeMillis();
			boolean boo = false;
			int size = raft.getLog().size();
			if ((end - start) >= HEARTBEAT_INTERVAL) {
				boo = true;
			} else {
				for (RaftNode rNode: raft.getRaftNodeMap().values()) {
					if (rNode.getNextIndex() < size) {
						boo = true;
						break;
					}
				}
			}

			if (boo) {
				for (RaftNode rNode: raft.getRaftNodeMap().values()) {
					try {
						appendEntry(rNode);
					} catch (IOException e) {
						//e.printStackTrace();
						System.out.println("fail in sending Append Entry RPC to " + rNode);
						try {
							rNode.closeConnection();
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					}
				}
				start = end;
			}
		}
	}
}

