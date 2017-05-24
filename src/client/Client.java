package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.net.Socket;
import java.util.ArrayList;

public class Client {
	static final int BULK_INPUT_MODE = 1;
	static final int ONE_BY_ONE_INPUT_MODE = 2;

	static final int TIMEOUT = 10000;

	//static final int RAFT_RECEIVE_PORT = 51111;
	static final int BACKLOG_SIZE = 1000;
	static final int BULK_MAX_ENTRY = 100;

	private Socket socket;
	private BufferedReader in;
	private PrintWriter out;

	//private ReceiveThread rvThread;
	//private SendThread sThread;

	private int mode;
	private int max_entry;
	private int raft_port;
	private String raft_address, chris_prefix = "172.16.10.";
	private int chris_no = 21;
	private ArrayList<String> inputList;
	private boolean waitCommit;

	private int commandNum = 0;
	private int count = 0;

	long start = -1;
	boolean finish = false;

	Client(int mode, int forwardingPort) {
		this.mode = mode;
		this.max_entry = (mode == BULK_INPUT_MODE) ? BULK_MAX_ENTRY : 1;
		this.raft_port = forwardingPort;
		this.raft_address = "172.16.10.13";
		this.inputList = new ArrayList<String>();
		this.waitCommit = false;
		try {
			setInput();
			openConnection();
			socket.setSoTimeout(TIMEOUT);

			//rvThread = new ReceiveThread(this);
			//rvThread.start();
			//sThread = new SendThread(this);
			//sThread.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public int getRaftPort() { return raft_port; }
	public String getRaftAddress() { return raft_address; }
	public void setRaftAddress(String raft_address) { this.raft_address = raft_address; }
	//public List<String> getInputQueue() { return inputList; }
	public int getMode() { return mode; }
	public int getMaxEntry() { return max_entry; }
	public boolean getWaitCommit() { return waitCommit; }
	public void setWaitCommit(boolean b) { this.waitCommit = b; }
	public int getCommandNum() { return commandNum; }
	public void incrementCount() { count++; }
	public void decrementCount() { count--; }
	public int getCount() { return count; }

	public void openConnection(Socket socket) throws IOException {
		System.out.println("open connection : " + socket.getRemoteSocketAddress());
		this.socket = socket;
		in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		out = new PrintWriter(socket.getOutputStream(), true);
	}
	public void openConnection() throws UnknownHostException, IOException {
		socket = new Socket(raft_address, raft_port);
		//System.out.println("open connection to " + socket.getRemoteSocketAddress());
		System.out.println("open connection : " + socket);
		in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		out = new PrintWriter(socket.getOutputStream(), true);
	}
	public void closeConnection() throws IOException {
		System.out.println("close connection to " + socket.getRemoteSocketAddress());
		in.close();
		System.out.println("in closed");
		out.close();
		System.out.println("out closed");
		socket.close();
		System.out.println("socket closed");
	}

	public String change_raft_host() {
		boolean success = false;
		while (!success) {
			try {
				closeConnection();

				int ones_place = chris_no % 10;
				ones_place = (ones_place + 1) % 5 + 1;

				chris_no = 20 + ones_place;
				raft_address = chris_prefix + (chris_no - 8);

				openConnection();

				success = true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return raft_address;
	}

	public void send(String str) throws UnknownHostException, IOException {
		out.println(str);
	}
	public String receive() throws IOException {
		return in.readLine();
	}

	public void setInput() {
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

		while(true) {
			try {
				//System.out.print("read command : ");
				String line = reader.readLine();

				if (line == null) {	break; }
				if (!line.isEmpty()) {
					inputList.add(line); // insert
					//System.out.println(line);
				}
			} catch (ConnectException e) {
				//e.printStackTrace();
				System.out.println("fale in connection to " + raft_address);
				change_raft_host();
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			commandNum++;
		}
		System.out.println("number of commmands = " + commandNum);
	}

	public void sendInput() {
		if (getWaitCommit() || finish) {
			return;
		}

		StringBuilder sb = new StringBuilder();
		sb.append("clientInput -1 ").append("wait").append(' ');

		try {
			if (getCount() < inputList.size()) {
				String str = inputList.get(getCount());
				sb.append(str);

				for (int i = 1; i < max_entry; i++) {
					incrementCount();
					if (getCount() < inputList.size())
						sb.append(',').append(inputList.get(count));
				}

				send(sb.toString());
				incrementCount();
				//System.out.println(sb);
			}

			// success
			//if (getMode() == Client.ONE_BY_ONE_INPUT_MODE)
			setWaitCommit(true);
			//System.out.println("send : " + sb.toString());
		} catch (IOException e) {
			//e.printStackTrace();
			System.out.println("send faled");
			//client.getInputQueue().addAll(0, tmpQueue);
			change_raft_host();
		} catch (IndexOutOfBoundsException e) {
			try {
				send(sb.toString());
				System.out.println(sb);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}
	public void process(String message) throws IOException {
		String strArr[] = message.split(" ");
		if (strArr[0].equals("commit")) {
			//if (strArr[0].equals("commit") && getWaitCommit()) {
			setWaitCommit(false);
			//incrementCount();

			if (getCount() % 2000 == 0) 
				System.out.println(socket.getInetAddress() + ":" + socket.getPort() + " > " + getCount() + " input end.");
			if (start < 0) { start = System.currentTimeMillis(); }
			if (getCount() >= getCommandNum() && !finish) {
				long end = System.currentTimeMillis();
				System.out.println(socket.getInetAddress() + ":" + socket.getPort() + " > it takes " + (end - start) + " ms.");
				setWaitCommit(true);
				finish = true;
			}
		} else if (strArr[0].equals("redirect")) {
			System.out.println("redirect to " + strArr[1]);
			setRaftAddress(strArr[1]);
			closeConnection();
			openConnection();
			setWaitCommit(false);

			for (int i = 0; i < max_entry; i++) {
				decrementCount();
			}
		}
	}

	private void run() throws IOException {
		while (true) {
			if (!finish) {
				sendInput();
				String str = receive();
				process(str);
			}
		}
	}

	public static void main(String args[]) {
		if (args.length < 2) {
			System.out.println("Please select mode: bulk/one_by_one.");
			System.out.println("Please select forwarding port.");
			System.exit(1);
		} else {
			int mode = -1;
			if (args[0].equalsIgnoreCase("one")) {
				mode = Client.ONE_BY_ONE_INPUT_MODE;
				System.out.println("Set mode one by one");
			} else if (args[0].equalsIgnoreCase("bulk")) {
				mode = Client.BULK_INPUT_MODE;
				System.out.println("Set mode bulk");
			} else {
				System.out.println("Please select mode: bulk/one");
				System.exit(1);
			}
			
			int forwardingPort = Integer.parseInt(args[1]);
			Client client = new Client(mode, forwardingPort);
			try {
				client.run();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}