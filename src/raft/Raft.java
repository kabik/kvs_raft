package raft;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import raft.server.ClientNode;
import raft.server.ClientNodesMap;
import raft.server.RaftNode;
import raft.server.RaftNodesMap;
import raft.server.Server;
import raft.state.*;
import raft.thread.*;
import stateMachine.KVS;
import stateMachine.StateMachine;
import raft.log.*;

public class Raft {
	static int TIMEOUT_MIN   = 2000;
	static int TIMEOUT_WIDTH = 1000;
	public static final int BACKLOG_SIZE = 1000;
	int serverNum;

	private RaftNodesMap raftNodesMap;
	private ClientNodesMap clientNodesMap;
	private State state;
	private StateMachine stateMachine;
	private int currentTerm, timeout, time, vote, commitIndex, lastApplied;
	private RaftNode votedFor, me;
	private Log log;
	private List<String> messageQueue = Collections.synchronizedList(new LinkedList<String>());

	private File configFile;

	ReceiveThread rThread;
	AppendEntryThread aeThread;
	TimeCountThread tcThread;
	LeaderElectThread leThread;
	CheckThread cThread;

	public Raft(String configFileName, String logFileName) {
		this.stateMachine = new KVS();
		this.configFile = new File(configFileName);
		this.log = new Log(logFileName);
		this.raftNodesMap = new RaftNodesMap();
		this.clientNodesMap = new ClientNodesMap();
	}

	// stateMachine
	public StateMachine getStateMachine() { return stateMachine; }

	// me
	public RaftNode getMe() { return me; }

	// state
	public synchronized void setState(State state) { this.state = state; }
	public synchronized State getState() { return state; }

	// apply log to KVS
	public void apply() {
		while (getCommitIndex() > getLastApplied() &&
				getLastApplied()+1 < log.size()) {
			getStateMachine().apply(log.get(getLastApplied()+1).getCommand());
			incrementLastApplied();
		}

		if (getState().isLeader()) {
			try {
				for (String key : clientNodesMap.getKeySet()) {
					ClientNode cn = clientNodesMap.get(key);
					int waitLogIndex = cn.getWaitIndex();
					if (cn.isWaitingForCommit() && 
							//waitLogIndex >= 0 &&
							getLastApplied() >= waitLogIndex) {
						send(cn, "C");
						cn.releaseWait();
					}
				}				
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	public int getServerNum() {
		return serverNum;
	}

	// log
	public Log getLog() {
		return log;
	}

	// message queue
	public void addMessage(String str) {
		messageQueue.add(str);
	}
	public String pollMessage() {
		try {
			return messageQueue.remove(0);
		} catch (NoSuchElementException e) {
			e.printStackTrace();
			return "";
		}
	}
	public boolean existMessage() {
		return !messageQueue.isEmpty();
	}
	public int getMessageQueueSize() { 
		return messageQueue.size();
	}

	// vote
	public synchronized RaftNode getVotedFor() {
		return votedFor;
	}
	public synchronized void resetVote() {
		getMe().setVotedForMe(false);
		for (String key: getRaftNodesMap().getKeySet()) {
			RaftNode rNode = getRaftNodesMap().get(key);
			rNode.setVotedForMe(false);
		}
		votedFor = null;
		vote = 0;
	}
	public synchronized void vote(RaftNode rNode) {
		votedFor = rNode;
	}
	public synchronized void beVoted(RaftNode rNode) {
		if (!rNode.hasVotedForMe()) {
			rNode.setVotedForMe(true);
			vote++;
		}
	}
	public synchronized int getVote() {
		return vote;
	}
	public synchronized boolean votedForSomeone() {
		return votedFor != null;
	}

	// currentTerm
	public synchronized int incrementCurrentTerm() {
		return ++currentTerm;
	}
	public synchronized int getCurrentTerm() {
		return currentTerm;
	}

	// timeout
	public int getTimeout() {
		return timeout;
	}

	// time
	public void resetTime() { 
		time = 0; 
	}
	public int incrementTime() { 
		return ++time;
	}
	public int getTime() {
		return time;
	}
	public void comebackTimeCount() {
		synchronized(tcThread) {
			tcThread.standFlag();
			tcThread.notifyAll();
		}
	}

	// commitIndex
	public synchronized int getCommitIndex() {
		return commitIndex;
	}
	public synchronized void setCommitIndex(int cIndex) { 
		this.commitIndex = cIndex;
	}

	// lastApplied
	public synchronized int getLastApplied() {
		return lastApplied;
	}
	public synchronized int incrementLastApplied() {
		return ++lastApplied;
	}

	// nextIndex
	public void resetNextIndex() {
		for (String key: getRaftNodesMap().getKeySet()) {
			RaftNode rNode = getRaftNodesMap().get(key);
			rNode.setNextIndex(log.size());
		}
	}

	// matchIndex
	public void resetMatchIndex() {
		for (String key: getRaftNodesMap().getKeySet()) {
			RaftNode rNode = getRaftNodesMap().get(key);
			rNode.setMatchIndex(0);
		}
	}

	// leaderElection
	public void stopLeaderElect() {
		leThread.sitFlag();
	}
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

	// raftMap
	public RaftNodesMap getRaftNodesMap() {
		return raftNodesMap;
	}

	// clientMap
	public ClientNodesMap getClientNodesMap() { 
		return clientNodesMap;
	}

	// initialization
	public void init() {
		try {
			BufferedReader br = new BufferedReader(new FileReader(configFile));

			String line = br.readLine();
			serverNum = Integer.parseInt(line);
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
					raftNodesMap.add(rNode.getIPAddress(), rNode);
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
		votedFor = null;
		commitIndex = -1;
		lastApplied = -1;

		resetNextIndex();

		// thread
		rThread = new ReceiveThread(this);
		tcThread = new TimeCountThread(this);
		leThread = new LeaderElectThread(this);
		aeThread = new AppendEntryThread(this);
		cThread = new CheckThread(this);
	}

	@Override
	public String toString() {
		return state + " currentTerm:" + currentTerm + " votedFor:" + votedFor + " vote:" + vote;
	}

	// iranai!
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
	 * @param canGoToNextTerm if true, term number will be incremented
	 */
	public void term(boolean canGoToNextTerm) {
		try {
			leThread.sitFlag();
			aeThread.sitFlag();
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (canGoToNextTerm) {
			incrementCurrentTerm();
		}

		resetTime();
		resetVote();

		if (state.isLeader()) {
			comebackAppendEntry();
		} else if (state.isCandidate()) {
			comebackLeaderElect();
			comebackTimeCount();
		} else if (state.isFollower()) {
			comebackTimeCount();
		}
		
		//System.out.println("term:" + getCurrentTerm() + " state:" + state);
	}

	public void start() {
		rThread.start();
		try {
			Thread.sleep(500);
		} catch (Exception e) {
			e.printStackTrace();
		}
		leThread.start();
		aeThread.start();
		cThread.start();
		tcThread.start();
	}
}
