package raft.log;

public class Entry {
	private int term;
	private String command;
	
	public Entry(int term, String command) {
		this.term = term;
		this.command = command;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(term).append(':').append(command);
		return sb.toString();
	}
	
	public int getTerm() {
		return term;
	}
	public String getCommand() {
		return command;
	}
}
