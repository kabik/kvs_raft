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

	public static final String APPEND_ENTRY_STR = "appendEntry";
	public static final String ACCEPT_AE_STR = "acceptAERPC";
	public static final String REJECT_AE_STR = "rejectAERPC";
	public static final String REQUEST_VOTE_STR = "requestVote";
	public static final String ACCEPT_RVRPC_STR = "acceptRVRPC";
	public static final String CLIENT_INPUT_STR = "clientInput";

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
				//System.out.println(str); ///
				String sArr[] = str.split(" ", 3);

				String order = sArr[0];
				int senderTerm = Integer.parseInt(sArr[1]);
				String contents = (sArr.length < 3) ? "" : sArr[2];

				// find later term
				if (senderTerm > raft.getCurrentTerm()) {
					raft.setState(new FollowerState());
					raft.vote((RaftNode)server);
					while (senderTerm > raft.getCurrentTerm()) { raft.term(true); }				
				}

				if (order.equals(APPEND_ENTRY_STR)) {
					String asArr[] = contents.split(" ", 4);
					int prevLogIndex = Integer.parseInt(asArr[0]);
					int prevLogTerm = Integer.parseInt(asArr[1]);
					int leaderCommit = Integer.parseInt(asArr[2]);
					String newEntriesStr = (asArr.length < 4) ? "" : asArr[3];
					
					// heartbeat
					if (raft.getState().isCandidate() && senderTerm == raft.getCurrentTerm()) {
						raft.stopLeaderElect();
						raft.setState(new FollowerState());
						raft.vote((RaftNode)server);
						raft.term(false);
						continue;
					} else {
						raft.resetTime();
					}
					
					//if (!newEntriesStr.isEmpty()) { System.out.println(newEntriesStr); }//

					if (raft.getVotedFor() != null && server != raft.getVotedFor() && senderTerm <= raft.getCurrentTerm()) {
						System.out.println("illegal leader sent me appendEntry PRC : "
								+ server.getHostname() + " sender's term is " + senderTerm);
						System.out.println("I have voted for " + raft.getVotedFor());
					}

					if (senderTerm < raft.getCurrentTerm() || !raft.getLog().match(prevLogIndex, prevLogTerm)) {
						// reject
						System.out.println("reject AE");
						if (senderTerm < raft.getCurrentTerm()) 
							System.out.println("term : "  + raft.getCurrentTerm());
						try {
							raft.send(server, REJECT_AE_STR + " " + raft.getCurrentTerm());
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
							raft.send(server, ACCEPT_AE_STR + " " + raft.getCurrentTerm() + " " + writtenIndex);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}

					// commitIndex
					if (leaderCommit > raft.getCommitIndex()) {
						raft.setCommitIndex(Math.min(leaderCommit, raft.getLog().lastIndex()));
					}
				} else if (order.equals(ACCEPT_AE_STR)) {
					// success appendEntry RPC
					RaftNode sender = (RaftNode)server;
					if (sender.isWaitingForAcception()) {
						sender.setNextIndex(sender.getSentIndex()+1);
						sender.setMatchIndex(sender.getSentIndex());

						raft.comebackCheckThread();
					}
					sender.setWrittenIndex(Integer.parseInt(contents));
					//System.out.println(raft.getRaftNodesMap()); ////
				} else if (order.equals(REJECT_AE_STR)) { // fail appendEntry RPC
					RaftNode sender = (RaftNode)server;
					sender.decrementNextIndex();
					sender.initSentIndex();
				} else if (order.equals(REQUEST_VOTE_STR)) {
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
						raft.setVotedForTerm(senderTerm);
						try {
							raft.send(sender, ACCEPT_RVRPC_STR + " " + raft.getCurrentTerm());
						} catch (IOException e) {
							e.printStackTrace();
						}
					}// else {} // not vote
				} else if (order.equals(ACCEPT_RVRPC_STR)) {
					RaftNode sender = (RaftNode)server;
					raft.beVoted(sender);
				} else if (order.equals(CLIENT_INPUT_STR)) {
					//System.out.println(str);//
					ClientNode cNode = (ClientNode)server;

					if (raft.getState().isLeader()) {
						String contentsStr[] = contents.split(" ", 2);
						//String mode = contentsStr[0];
						String entriesStr[] = contentsStr[1].split(",");

						Entry entries[] = new Entry[entriesStr.length];
						for (int i = 0; i < entriesStr.length; i++) {
							if (!entriesStr[i].isEmpty())
								entries[i] = new Entry(raft.getCurrentTerm(), entriesStr[i]);
						}
						int lastIndex = raft.getLog().add(entries);
						cNode.setWaitLogIndex(lastIndex);

						raft.comebackCheckThread();
					} else {
						raft.send(cNode, "redirect " + raft.getVotedFor().getIPAddress());
					}
				} else {
					System.out.println("illegal command:" + order);
				}
				//System.out.println("-----------");
			}
		} catch (Exception e) {
			e.printStackTrace();
			try {
				System.out.println("close connection with " + server);
				server.closeConnection();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		System.out.println("end process message ");
	}
}
