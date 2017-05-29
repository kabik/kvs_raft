package raft.server;

import java.util.TreeMap;

public class ClientNodesMap {
	private TreeMap<String, ClientNode> map = new TreeMap<String, ClientNode>();
	
	public ClientNodesMap() {}
	
	public synchronized ClientNode get(String key) {
		return map.get(key);
	}
	public synchronized void add(String key, ClientNode cNode) {
		map.put(key, cNode);
	}
	public synchronized void remove(String key) {
		map.remove(key);
	}
	public synchronized TreeMap<String, ClientNode> getCloneOfMap() {
		return new TreeMap<String, ClientNode>(map);
	}
}
