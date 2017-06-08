package kvs;

import java.util.TreeMap;

import raft.Raft;

public class KVS {
	TreeMap<String, String> map = new TreeMap<String, String>();
	Raft raft;
	
	public KVS(String configFileName, String logFileSufix) {
		raft = new Raft(this, configFileName, logFileSufix);
		raft.init();
		raft.start();
	}
	
	public int size() {
		return map.size();
	}
	
	public void put(String key, String value) {
		//System.out.println("put " + key + " " + value);
		map.put(key, value);
	}
	public String get(String key) {
		return map.get(key);
	}
	public String remove(String key) {
		String ret = map.remove(key);
		return ret;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("--kvs--\n");
		for (String key : map.keySet()) {
			sb.append(key);
			sb.append(',');
			sb.append(map.get(key));
			sb.append('\n');
		}
		sb.append("-------");
		System.out.println(sb);
		
		return sb.toString();
	}
}
