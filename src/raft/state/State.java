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
	
	public String getName() { return name; }
	public boolean isLeader()		{ return false;}
	public boolean isCandidate()	{ return false;}
	public boolean isFollower()		{ return false;}
}
