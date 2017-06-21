package raft.thread;

import java.io.IOException;
import java.net.Socket;

import raft.*;
import raft.log.Entry;
import raft.server.ClientNode;
import raft.server.RaftNode;
import raft.server.Server;
import raft.state.FollowerState;

public class ProcessMessageThread extends AbstractThread {
	public static final int INTERVAL = 1;

	public static final char APPEND_ENTRY_CHAR = 'a';
	public static final char ACCEPT_AE_CHAR = 'A';
	public static final char REJECT_AE_CHAR = 'R';
	public static final char REQUEST_VOTE_CHAR = 'v';
	public static final char ACCEPT_RVRPC_CHAR = 'V';
	public static final char CLIENT_INPUT_CHAR = 'i';
	public static final char CLIENT_RESNEDREQUEST_CHAR = 's';

	private Socket socket;

	public ProcessMessageThread(Raft raft, Socket socket) {
		super(raft);
		this.socket = socket;
	}

	@Override
	public void run() {
		String ipAddress = socket.getRemoteSocketAddress().toString().split(":")[0].substring(1);

		Server server = raft.getRaftNodesMap().get(ipAddress);
		if (server == null || (server != null && server.isConnected())) {
			String key = ipAddress + ':' + socket.getPort();
			ClientNode cNode = raft.getClientNodesMap().get(key);
			if (cNode == null) {
				cNode = new ClientNode(raft, ipAddress);
				cNode.setRecievePort(socket.getPort());
				raft.getClientNodesMap().add(key, cNode);
			}
			server = cNode;
		}
		try {
			server.setIO(socket);
		} catch (IOException e) {
			e.printStackTrace();
		}

		String str = "";
		try {
			while(!_halt && !(str = server.receive()).isEmpty()) {
				String sArr[] = str.split(" ", 3);

				char order = sArr[0].charAt(0);
				int senderTerm = Integer.parseInt(sArr[1]);
				String contents = (sArr.length < 3) ? "" : sArr[2];

				// find later term
				if (senderTerm > raft.getCurrentTerm()) {
					raft.setState(new FollowerState());
					raft.vote((RaftNode)server);
					while (senderTerm > raft.getCurrentTerm()) { raft.term(true); }				
				}

				if (order == APPEND_ENTRY_CHAR) {
					String asArr[] = contents.split(" ", 4);
					int prevLogIndex = Integer.parseInt(asArr[0]);
					int prevLogTerm = Integer.parseInt(asArr[1]);
					int leaderCommit = Integer.parseInt(asArr[2]);
					String newEntriesStr = (asArr.length < 4) ? "" : asArr[3];

					// heartbeat
					if (raft.getState().isCandidate() && senderTerm == raft.getCurrentTerm()) {
						raft.stopLeaderElect();
						raft.setState(new FollowerState());
						raft.term(false);
						raft.vote((RaftNode)server);
						continue;
					} else {
						raft.resetTime();
					}

					if (raft.getVotedFor() != null
							&& server != raft.getVotedFor()
							&& senderTerm <= raft.getCurrentTerm()) {
						System.out.println("Illegal leader sent me an appendEntry PRC : "
								+ server.getHostname() + " sender's term is " + senderTerm);
						System.out.println("I have voted for " + raft.getVotedFor());
					}

					if (senderTerm < raft.getCurrentTerm() || !raft.getLog().match(prevLogIndex, prevLogTerm)) {
						// reject
						System.out.println("reject AE");
						if (senderTerm < raft.getCurrentTerm()) 
							System.out.println("term : "  + raft.getCurrentTerm());
						try {
							raft.send(server, REJECT_AE_CHAR + " " + raft.getCurrentTerm());
						} catch (IOException e) {
							e.printStackTrace();
						}
					} else {
						// accept
						String entriesStrs[] = newEntriesStr.split(",");
						int num_of_entries = (entriesStrs[0].isEmpty()) ? 0 : entriesStrs.length;

						Entry entries[] = new Entry[num_of_entries];
						for (int i = 0; i < num_of_entries; i++) {
							String entryStr[] = entriesStrs[i].split(":");
							int newEntryTerm = Integer.parseInt(entryStr[0]);
							entries[i] = new Entry(newEntryTerm, entryStr[1]);
						}
						int writtenIndex = raft.getLog().add(prevLogIndex + 1, entries);
						if (num_of_entries > 0) {
							raft.comebackCheckThread(); // iru?
						}
						try {
							raft.send(server, ACCEPT_AE_CHAR + " " + raft.getCurrentTerm() + " " + writtenIndex);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}

					// commitIndex
					if (leaderCommit > raft.getCommitIndex()) {
						raft.setCommitIndex(Math.min(leaderCommit, raft.getLog().lastIndex()));
					}
				} else if (order == ACCEPT_AE_CHAR) {
					// success appendEntry RPC
					RaftNode sender = (RaftNode)server;
					if (sender.isWaitingForAcception()) {
						sender.setNextIndex(sender.getSentIndex()+1);
						sender.setMatchIndex(sender.getSentIndex());

						raft.comebackCheckThread();
					}
					sender.setWrittenIndex(Integer.parseInt(contents));
				} else if (order == REJECT_AE_CHAR) {
					// fail appendEntry RPC
					RaftNode sender = (RaftNode)server;
					sender.decrementNextIndex();
					sender.initSentIndex();
				} else if (order == REQUEST_VOTE_CHAR) {
					RaftNode sender = (RaftNode)server;
					String asArr[] = contents.split(" ");
					int lastLogIndex = Integer.parseInt(asArr[0]);
					int lastLogTerm = Integer.parseInt(asArr[1]);
					if (senderTerm > raft.getCurrentTerm() ||
							(senderTerm == raft.getCurrentTerm()
							&& !raft.getState().isLeader()
							&& !raft.getState().isCandidate()
							&& (!raft.votedForSomeone() || raft.getVotedFor() == sender)
							&& ((lastLogTerm > raft.getLog().lastLogTerm() 
									|| (lastLogTerm == raft.getLog().lastLogTerm()	&& lastLogIndex >= raft.getLog().lastIndex() ) )))){
						raft.resetTime();
						raft.vote(sender);
						try {
							raft.send(sender, ACCEPT_RVRPC_CHAR + " " + raft.getCurrentTerm());
						} catch (IOException e) {
							e.printStackTrace();
						}
					}// else {} // not vote
				} else if (order == ACCEPT_RVRPC_CHAR) {
					RaftNode sender = (RaftNode)server;
					raft.beVoted(sender);
				} else if (order == CLIENT_INPUT_CHAR) {
					ClientNode cNode = (ClientNode)server;

					if (raft.getState().isLeader()) {
						String entriesStr[] = contents.split(",");

						Entry entries[] = new Entry[entriesStr.length];
						for (int i = 0; i < entriesStr.length; i++) {
							if (!entriesStr[i].isEmpty())
								entries[i] = new Entry(raft.getCurrentTerm(), entriesStr[i]);
						}
						int lastIndex = raft.getLog().add(entries);
						cNode.waitForCommit(lastIndex);

						raft.comebackCheckThread();
					} else {
						raft.send(cNode, "redirect " + raft.getVotedFor().getIPAddress());
					}
				} else if (order == CLIENT_RESNEDREQUEST_CHAR) {
					ClientNode cNode = (ClientNode)server;
					
					if (raft.getState().isLeader()) {
						cNode.waitForCommit();
						System.out.println("resend");
					} else {
						raft.send(cNode, "redirect " + raft.getVotedFor().getIPAddress());
						System.out.println("redirect");
					}
				} else {
					System.out.println("illegal command:" + order);
				}
			}
		} catch (Exception e) {
			//e.printStackTrace();
			try {
				System.out.println("close connection");
				server.closeConnection();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		//System.out.println("end process message");
	}
}
