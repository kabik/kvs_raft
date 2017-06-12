package stateMachine;

import java.util.TreeMap;

import raft.Raft;

public class KVS extends StateMachine {
	TreeMap<String, String> map = new TreeMap<String, String>();
	Raft raft;
	
	public KVS() {
	}
	
	public void apply(String commandStr) {
		String s[] = commandStr.split(" ");
		if (s[0].equals("put")) {
			put(s[1], s[2]);
		} else if (s[0].equals("delete")) {
			remove(s[1]);
		} else if (s[0].equals("get")) {
			get(s[1]);
		}
	}
	
	private void put(String key, String value) {
		map.put(key, value);
	}
	private String get(String key) {
		return map.get(key);
	}
	private String remove(String key) {
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
