package raft.server;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;

import raft.Raft;

/*
 * This class have informations of other Raft nodes.
 */
public class RaftNode extends Server {
	public static final char NODE_TYPE = 'r';
	
	private int nextIndex;
	private int matchIndex;
	private int sentIndex;
	private int writtenIndex;
	private int priority;
	private boolean isme;		// whether this is host
	private boolean votedForMe;
	private boolean rvrpc_sent; // whether Request Vote RPC has been sent

	/*
	 * address is an IPAddress or a hostname
	 */
	public RaftNode(Raft raft, String address) {
		super(raft, address, RaftNode.NODE_TYPE);

		this.matchIndex = 0;
		this.writtenIndex = -1;
		this.votedForMe = false;
		this.rvrpc_sent = false;

		this.isme = false;
		try {
			java.util.Enumeration<NetworkInterface> enuIfs = NetworkInterface.getNetworkInterfaces();
			while (enuIfs.hasMoreElements()) {
				NetworkInterface ni = enuIfs.nextElement();
				java.util.Enumeration<InetAddress> enuIfs2 = ni.getInetAddresses();
				while (enuIfs2.hasMoreElements()) {
					InetAddress subNi = enuIfs2.nextElement();
					String subNiStr = subNi.toString().substring(1);
					if (subNiStr.equals(address) || subNiStr.equals(getIPAddress())) {
						this.isme = true;
					}
				}
			}
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	public boolean isMe() { return isme; }

	public synchronized int getNextIndex() { return nextIndex; }
	public synchronized void setNextIndex(int nIndex) { this.nextIndex = nIndex; }
	public synchronized int decrementNextIndex() {
		if (--nextIndex < 0)
			nextIndex = 0;
		return nextIndex;
	}

	public synchronized int getMatchIndex() { return matchIndex; }
	public synchronized void setMatchIndex(int mIndex) { this.matchIndex = mIndex; }
	public synchronized int incrementMatchIndex() { return ++matchIndex; }

	public synchronized void initSentIndex() { this.sentIndex = getNextIndex() - 1; }
	public synchronized void setSentIndex(int sentIndex) { this.sentIndex = sentIndex; }
	public synchronized int getSentIndex() { return sentIndex; }	
	
	public synchronized void setWrittenIndex(int preWrittenIndex) { this.writtenIndex = preWrittenIndex; }
	public synchronized int getWrittenIndex() { return writtenIndex; }

	public synchronized void setVotedForMe(boolean b) { this.votedForMe = b; }
	public synchronized boolean hasVotedForMe() { return votedForMe; }

	public void setRVRPCsent(boolean b) { this.rvrpc_sent = b; }
	public boolean hasSentRVRPC() { return rvrpc_sent; }

	public synchronized boolean isWaitingForAcception() { return sentIndex >= nextIndex; }
	
	public synchronized void setPriority(int priority) { this.priority = priority; }
	public synchronized int getPriority() { return priority; }
}
