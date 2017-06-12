package raft.state;

public class FollowerState extends State {
	public FollowerState() {
		super("Follower");
	}
	@Override
	public boolean isLeader() {
		return false;
	}
	@Override
	public boolean isCandidate() {
		return false;
	}
	@Override
	public boolean isFollower() {
		return true; 
	}
}