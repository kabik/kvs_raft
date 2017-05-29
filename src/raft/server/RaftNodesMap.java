package raft.server;

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
	public synchronized TreeMap<String, RaftNode> getMap() {
		return map;
	}
	/*public synchronized TreeMap<String, RaftNode> getCloneOfMap() {
	return new TreeMap<String, RaftNode>(map);
	}*/
}
