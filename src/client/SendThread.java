package client;

/*
 * not used
 */
public class SendThread {
	static final int INTERVAL = 1;

	Client client;
	public boolean _halt = false;

	public SendThread(Client client) {
		this.client = client;
	}

	/*

	public void run()  {
		while(!_halt) {
			while (client.getInputQueue().size() == 0 ||
					(client.getMode() == Client.ONE_BY_ONE_INPUT_MODE && client.getWaitCommit()) ) {
				// 
			}
			sendInput();

			/*try {
				while (client.getInputQueue().size() == 0 ||
						(client.getMode() == Client.ONE_BY_ONE_INPUT_MODE && client.getWaitCommit()) ) {
					sleep(INTERVAL);
				}
				sendInput();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	*/
}
