package raft.server;

import java.util.ArrayList;
import java.util.TreeMap;

public class RaftNodesMap {
	private TreeMap<String, RaftNode> map = new TreeMap<String, RaftNode>();
	
	public RaftNodesMap() {}
	
	public synchronized RaftNode get(String key) {
		return map.get(key);
	}
	public synchronized void add(String key, RaftNode cNode) {
		map.put(key, cNode);
	}
	public synchronized void remove(String key) {
		map.remove(key);
	}
	public synchronized ArrayList<String> getKeySet() {
		return new ArrayList<String>(map.keySet());
	}
	@Override public synchronized String toString() {
		StringBuilder sb = new StringBuilder();
		for (RaftNode rNode: map.values()) {
			sb.append(rNode).append('\n');
		}
		return sb.toString();
	}
}
