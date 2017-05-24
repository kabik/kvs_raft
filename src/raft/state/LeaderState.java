package raft.state;

public class LeaderState extends State {
	public LeaderState() {
		super("Leader");
	}
	@Override
	public boolean isLeader() { return true; }
}