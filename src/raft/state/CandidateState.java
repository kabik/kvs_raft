package raft.state;

public class CandidateState extends State {
	public CandidateState() {
		super("Candidate");
	}
	@Override
	public boolean isLeader() {
		return false;
	}
	@Override
	public boolean isCandidate() { 
		return true; 
	}
	@Override
	public boolean isFollower() {
		return false;
	}
}
