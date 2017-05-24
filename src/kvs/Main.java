package kvs;

import kvs.KVS;

public class Main {
	KVS kvs;

	Main(String configFileName, String logFileName) {
		kvs = new KVS(configFileName, logFileName);
	}

	public static void main(String[] args) {
		if (args.length <= 1) {
			System.out.println("config filename or log filename is none.");
			System.exit(0);
		}
		new Main(args[0], args[1]);
	}
}
