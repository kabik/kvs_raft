package raft;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;

import kvs.KVS;
import raft.server.ClientNode;
import raft.server.ClientNodesMap;
import raft.server.RaftNode;
import raft.server.Server;
import raft.state.*;
import raft.thread.*;
import raft.log.*;

public class Raft {
	static int TIMEOUT_MIN   = 2000;
	static int TIMEOUT_WIDTH = 1000;
	public static final int BACKLOG_SIZE = 10000;
	int maxNum;			// max number of servers

	private TreeMap<String, RaftNode> raftNodeMap = new TreeMap<String, RaftNode>();
	private ClientNodesMap clientNodesMap;
	private State state;
	KVS kvs;
	private int currentTerm, timeout, time, vote, votedForTerm, commitIndex, lastApplied;
	private RaftNode votedFor, me, leader;
	private Log log;
	private List<String> messageQueue = Collections.synchronizedList(new LinkedList<String>());

	private File configFile;

	Map<Integer, ProcessMessageThread> pmThreads;
	ReceiveThread rThread;
	AppendEntryThread aeThread;
	TimeCountThread tcThread;
	LeaderElectThread leThread;
	CheckThread cThread;

	public Raft(KVS kvs, String configFileName, String logFileSufix) {
		this.kvs = kvs;
		this.configFile = new File(configFileName);
		this.log = new Log(logFileSufix);
		this.clientNodesMap = new ClientNodesMap();
	}

	// KVS
	public KVS getKVS() { return kvs; }

	// me
	public RaftNode getMe() { return me; }

	// state
	public synchronized void setState(State state) { this.state = state; }
	public synchronized State getState() { return state; }

	// apply log to KVS
	public void apply() {
		while (getCommitIndex() > getLastApplied() && getLastApplied()+1 < log.size()) {

			String s[] = log.get(getLastApplied()+1).getCommand().split(" ");
			if (s[0].equals("put")) {
				kvs.put(s[1], s[2]);
			} else if (s[0].equals("delete")) {
				kvs.remove(s[1]);
			}
			incrementLastApplied();
		}

		if (getState().isLeader()) {
			try {
				TreeMap<String, ClientNode> cloneMap = clientNodesMap.getCloneOfMap();
				for (ClientNode cn : cloneMap.values()) {
					int waitLogIndex = cn.getWaitLogIndex();
					if (waitLogIndex >= 0 && getLastApplied() >= waitLogIndex) {
						send(cn, "commit");
					}
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	// max number of servers
	public int getMaxNum() { return maxNum; }

	// log
	public Log getLog() { return log; }

	// message queue
	public void addMessage(String str) { messageQueue.add(str); } //add last
	public String pollMessage() {
		try {
			return messageQueue.remove(0);
		} catch (NoSuchElementException e) {
			e.printStackTrace();
			return "";
		}
	}
	public boolean existMessage() { return !messageQueue.isEmpty(); }
	public int getMessageQueueSize() { return messageQueue.size(); } // for debugging

	// vote
	public synchronized RaftNode getVotedFor() { return votedFor; }
	public synchronized void resetVote() {
		getMe().setVotedForMe(false);
		for (RaftNode node : raftNodeMap.values()) {
			node.setVotedForMe(false);
		}
		leader = null;
		votedFor = null;
		vote = 0;
	}
	public synchronized void vote(RaftNode node) { votedFor = node; }
	public synchronized void beVoted(RaftNode node) {
		if (!node.hasVotedForMe()) {
			node.setVotedForMe(true);
			vote++;
		}
		//System.out.println(vote + " votes");
	}
	public synchronized int getVote() { return vote; }
	public synchronized int getVotedForTerm() { return (votedForSomeone()) ? votedForTerm : -1; }
	public synchronized void setVotedForTerm(int t) { this.votedForTerm = t; }
	public synchronized boolean votedForSomeone() { return votedFor != null; }

	// currentTerm
	public synchronized int incrementCurrentTerm() { return ++currentTerm; }
	public synchronized int getCurrentTerm() { return currentTerm; }

	// timeout
	public int getTimeout() { return timeout; }

	// time
	public void resetTime() { time = 0; }
	public int incrementTime() { return ++time; }
	public int getTime() { return time; }
	public void comebackTimeCount() {
		synchronized(tcThread) {
			tcThread.standFlag();
			tcThread.notifyAll();
		}
	}

	// commitIndex
	public synchronized int getCommitIndex() { return commitIndex; }
	public synchronized void setCommitIndex(int cIndex) { this.commitIndex = cIndex; }

	// lastApplied
	public synchronized int getLastApplied() { return lastApplied; }
	public synchronized int incrementLastApplied() { return ++lastApplied; }

	// nextIndex
	public void resetNextIndex() {
		for (RaftNode node: raftNodeMap.values()) {
			node.setNextIndex(log.size());
		}
	}

	// matchIndex
	public void resetMatchIndex() {
		for (RaftNode node: raftNodeMap.values()) {
			node.setMatchIndex(0);
		}
	}

	// ProcessMessage
	public void comebackProcessMessage() {
		for (ProcessMessageThread pmThread : pmThreads.values()) {
			synchronized (pmThread) {
				pmThread.standFlag();
				pmThread.notifyAll();
			}
		}
	}

	// leaderElection
	public void stopLeaderElect() { leThread.sitFlag(); }
	public void comebackLeaderElect() {
		synchronized (leThread) {
			leThread.standFlag();
			leThread.notifyAll();
		}
	}

	// heartbeat
	public void comebackAppendEntry() {
		synchronized (aeThread) {
			aeThread.standFlag();
			aeThread.notifyAll();
		}
	}

	// check
	public void comebackCheckThread() {
		synchronized (cThread) {
			cThread.standFlag();
			cThread.notifyAll();
		}
	}

	// serverMap
	public synchronized TreeMap<String, RaftNode> getRaftNodeMap() { return raftNodeMap; }
	public synchronized boolean leaderCheckByIP(String key) { return raftNodeMap.get(key) == leader; }

	// clientMap
	public ClientNodesMap getClientNodesMap() { return clientNodesMap; }

	// initialization
	public void init() {
		try {
			BufferedReader br = new BufferedReader(new FileReader(configFile));

			String line = br.readLine();
			maxNum = Integer.parseInt(line);
			while((line = br.readLine()) != null) {
				if (line.length() == 0) continue;

				String s[] = line.split("::", 0);
				String portStr = s[1];
				RaftNode rNode = new RaftNode(this, s[0]);
				rNode.setRecievePort(Integer.parseInt(portStr));
				if (s.length > 2 && s[2].equals("p")) {
					rNode.setPriority(1);
				}
				if (rNode.isMe()) {
					me = rNode;
				} else {
					raftNodeMap.put(rNode.getIPAddress(), rNode);
				}
			}
			br.close();
		} catch(FileNotFoundException e) {
			e.printStackTrace();
		} catch(IOException e) {
			e.printStackTrace();
		} catch(Exception e) {
			e.printStackTrace();
		}

		state = new FollowerState();
		timeout = (int)( Math.random() * TIMEOUT_WIDTH + TIMEOUT_MIN ) - TIMEOUT_WIDTH * me.getPriority();
		time = 0;
		currentTerm = 0;
		votedForTerm = -1;
		votedFor = null;
		leader = null;
		commitIndex = -1;
		lastApplied = -1;

		resetNextIndex();

		// thread
		rThread = new ReceiveThread(this);
		pmThreads = new TreeMap<Integer, ProcessMessageThread>();
		tcThread = new TimeCountThread(this);
		leThread = new LeaderElectThread(this);
		aeThread = new AppendEntryThread(this);
		cThread = new CheckThread(this);
	}

	public void showServersInfo() {
		System.out.println(maxNum + " Servers infomation");
		System.out.println("this:"+this);
		for (Server server: raftNodeMap.values()) {
			System.out.println("  " + server);
		}
	}

	@Override
	public String toString() {
		return state + " currentTerm:" + currentTerm + " votedFor:" + votedFor + " vote:" + vote;
	}

	public void send(Server server, String str) throws IOException {
		server.send(str);
	}

	// become candidate and run for leader election
	public void runFor() {
		setState(new CandidateState());
		leThread.init();
		term(true);
	}

	/**
	 * @param next if true, term number will be incremented
	 */
	public void term(boolean next) {
		try {
			leThread.sitFlag();
			aeThread.sitFlag();
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (next) {
			incrementCurrentTerm();
			System.out.println("next term:" + getCurrentTerm() + " state:" + state);
		} else {
			//System.out.println("term:" + getCurrentTerm() + " state:" + state);
		}
		resetTime();
		resetVote();

		if (state.isLeader()) {
			comebackAppendEntry();
			//comebackClientInput();
		} else if (state.isCandidate()) {
			comebackLeaderElect();
			comebackTimeCount();
		} else if (state.isFollower()) {
			comebackTimeCount();
		}
	}

	public void run() {
		rThread.start();
		try {
			Thread.sleep(500);
			//openSend();
		} catch (Exception e) {
			e.printStackTrace();
		}
		leThread.start();
		aeThread.start();
		cThread.start();
		tcThread.start();
	}
}
