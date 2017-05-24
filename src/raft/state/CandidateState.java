package raft.state;

public class CandidateState extends State {
	public CandidateState() {
		super("Candidate");
	}
	@Override
	public boolean isCandidate() { return true; }
}
