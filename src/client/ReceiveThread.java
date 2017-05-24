package client;

import java.io.IOException;

public class ReceiveThread {
	Client client;
	public boolean _halt = false;
	
	long start;
	boolean finish = false;

	public ReceiveThread(Client client) {
		this.client = client;
		this.start = -1;
	}

	public void process(String message) throws IOException {
		String strArr[] = message.split(" ");
		if (strArr[0].equals("commit") && client.getWaitCommit()) {
			client.setWaitCommit(false);
			client.incrementCount();

			//if (client.getCount() % 100 == 0) 
				//System.out.println(client.getCount() + " input end.");
			if (start < 0) { start = System.currentTimeMillis(); }
			if (client.getCount() >= client.getCommandNum() && !finish) {
				System.out.println("commit time is " + (System.currentTimeMillis() - start));
				client.setWaitCommit(true);
				//System.exit(0);
				finish = true;
			}
		} else if (strArr[0].equals("redirect")) {
			System.out.println("redirect to " + strArr[1]);
			client.setRaftAddress(strArr[1]);
			client.closeConnection();
			client.openConnection();
			client.setWaitCommit(false);
		}
	}

	//@Override
	/*public void run()  {
		while(!_halt) {
			try {
				String str = client.receive();
				//System.out.println("message : " + str);
				process(str);
			} catch (Exception e) {
				//e.printStackTrace();
				System.out.println("receive faled");
				try {
					sleep(1000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		}
	}*/
}
