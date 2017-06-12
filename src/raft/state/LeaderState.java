package raft.state;

public class LeaderState extends State {
	public LeaderState() {
		super("Leader");
	}
	@Override
	public boolean isLeader() {
		return true;
	}
	@Override
	public boolean isCandidate() {
		return false;
	}
	@Override
	public boolean isFollower() {
		return false;
	}
}