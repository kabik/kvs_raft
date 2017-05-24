package raft.state;

public class FollowerState extends State {
	public FollowerState() {
		super("Follower");
	}
	@Override
	public boolean isFollower() { return true; }
}