package main;

import raft.Raft;

public class Main {
	Main(String configFileName, String logFileName) {
		Raft raft = new Raft(configFileName, logFileName);
		raft.init();
		raft.start();
	}

	public static void main(String[] args) {
		if (args.length <= 1) {
			System.out.println("config filename or log filename is none.");
			System.exit(0);
		}
		new Main(args[0], args[1]);
	}
}
