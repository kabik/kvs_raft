package raft.state;

public abstract class State {
	private String name;

	protected State(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}

	public String getName() {
		return name; 
	}
	public abstract boolean isLeader();
	public abstract boolean isCandidate();
	public abstract boolean isFollower();
}
