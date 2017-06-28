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
	static final int SOCKET_TIMEOUT = 500;
	static final int BACKLOG_SIZE = 100;

	private Socket socket;
	private BufferedReader in;
	private PrintWriter out;

	private int maxEntry;
	private int raft_port;
	private String raft_address, server_prefix = "172.16.10.";
	private int server_no = 21;
	private ArrayList<String> inputList;
	private boolean waitCommit;

	private int commandNum = 0;
	private int count = 0;
	
	private int lastCommitIndex = -1;

	long start = -1;
	boolean finish = false;

	Client(int maxEntry, int forwardingPort) {
		this.maxEntry = maxEntry;
		this.raft_port = forwardingPort;
		this.raft_address = "172.16.10.13";
		this.inputList = new ArrayList<String>();
		this.waitCommit = false;
		try {
			setInput();
			openConnection();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public int getRaftPort() {
		return raft_port;
	}
	public String getRaftAddress() {
		return raft_address;
	}
	public void setRaftAddress(String raft_address) {
		this.raft_address = raft_address;
	}
	public int getMaxEntry() {
		return maxEntry;
	}
	public boolean getWaitCommit() {
		return waitCommit;
	}
	public void setWaitCommit(boolean b) { 
		this.waitCommit = b;
	}
	public int getCommandNum() {
		return commandNum;
	}
	public void incrementCount() {
		count++;
	}
	public void decrementCount() {
		count--; 
	}
	public int getCount() {
		return count;
	}

	public void openConnection(Socket socket) throws IOException {
		this.socket = socket;
		socket.setSoTimeout(SOCKET_TIMEOUT);
		in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		out = new PrintWriter(socket.getOutputStream(), true);
	}
	public void openConnection() throws UnknownHostException, IOException {
		socket = new Socket(raft_address, raft_port);
		socket.setSoTimeout(SOCKET_TIMEOUT);
		in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		out = new PrintWriter(socket.getOutputStream(), true);
	}
	public void closeConnection() throws IOException {
		socket.close();
	}

	public String change_raft_host() {
		boolean success = false;
		while (!success) {
			try {
				closeConnection();

				int ones_place = server_no % 10;
				ones_place = (ones_place + 1) % 5 + 1;

				server_no = 20 + ones_place;
				raft_address = server_prefix + (server_no - 8);

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
				String line = reader.readLine();

				if (line == null) {	break; }
				if (!line.isEmpty()) {
					inputList.add(line);
				}
			} catch (ConnectException e) {
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
		sb.append("i -1 ").append(' ');

		try {
			if (getCount() < inputList.size()) {
				String str = inputList.get(getCount());
				sb.append(str);

				for (int i = 1; i < maxEntry; i++) {
					incrementCount();
					if (getCount() < inputList.size())
						sb.append(',').append(inputList.get(count));
				}

				send(sb.toString());
				incrementCount();
			}

			setWaitCommit(true);
		} catch (IOException e) {
			System.out.println("send faled");
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
		if (strArr[0].equals("C")) {
			int commitIndex = Integer.parseInt(strArr[1]);
			if (commitIndex > lastCommitIndex) {
				lastCommitIndex = commitIndex;
				setWaitCommit(false);	
			}
			
			if (getCount() % 2000 == 0) 
				System.out.println(socket.getInetAddress() + ":" + socket.getPort() +
						" > " + getCount() + " input end.");
			if (start < 0) 
				start = System.currentTimeMillis();
			if (getCount() >= getCommandNum() && !finish) {
				long end = System.currentTimeMillis();
				System.out.println(socket.getInetAddress() + ":" + socket.getPort() +
						" > it takes " + (end - start) + " ms.");
				setWaitCommit(true);
				finish = true;
				closeConnection();
			}
		} else if (strArr[0].equals("redirect")) {
			setRaftAddress(strArr[1]);
			closeConnection();
			openConnection();
			setWaitCommit(false);

			for (int i = 0; i < maxEntry; i++) {
				decrementCount();
			}
		}
	}

	private void run() {
		while (!finish) {
			sendInput();
			try {
				String str = receive();
				process(str);
			} catch (IOException e) {
				System.out.println("Receive Exception");
				if (getWaitCommit()) {
					try {
						send("s -1 ");
						System.out.println("Request resend");
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}
		}
	}

	public static void main(String args[]) {
		if (args.length < 2) {
			System.out.println("Please select a number of entries per one put: 1~");
			System.out.println("Please select forwarding port.");
			System.exit(1);
		} else {
			int maxEntry = Integer.parseInt(args[0]);
			int forwardingPort = Integer.parseInt(args[1]);
			new Client(maxEntry, forwardingPort).run();
		}
	}
}
