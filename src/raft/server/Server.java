package raft.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

import raft.Raft;
import raft.thread.ProcessMessageThread;

public abstract class Server {
	private Raft raft;
	
	private InetAddress ip;
	private int receivePort;	// port number for receiving (if this server isn't me, this won't used)
	
	private Socket socket = null;
	private BufferedReader  in = null;
	private PrintWriter out = null;
	
	private char nodeType;

	public Server(Raft raft, String address, char nodeType) {
		this.raft = raft;
		//this.sendPort = this.receivePort = -1;
		this.nodeType = nodeType;
		try {
			this.ip = InetAddress.getByName(address);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public String toString() {
		return getHostname() + "::" + receivePort; // + "," + sendPort;
	}

	public void send(String str) throws IOException {
		if (out == null) {
			closeConnection();
			new ProcessMessageThread(raft, new Socket(getIPAddress(), getReceivePort())).start();
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		//System.out.println("send message with " + socket);
		synchronized (out) {
			out.println(str);
		}
	}
	public String receive() throws IOException {
		return in.readLine();
	}
	//public void markBR() throws IOException { in.mark(8196); }
	//public void resetBR() throws IOException { in.reset(); }

	synchronized public void openInput() throws IOException {
		//System.out.println("open input to " + getHostname());
		in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	}
	synchronized public void openOutput() throws IOException {
		//System.out.println("open output to " + getHostname());
		out = new PrintWriter(socket.getOutputStream(), true);
	}
	synchronized public void closeInput() throws IOException {
		if (in != null) {
			//System.out.println("close input to " + getHostname());
			in.close();
			in = null;
		}
	}
	synchronized public void closeOutput() throws IOException {
		if (out != null) {
			//System.out.println("close output to " + getHostname());
			out.close();
			out = null;
		}
	}
	synchronized public void closeSocket() throws IOException {
		if (socket != null) {
			//System.out.println("close socket to " + getHostname());
			System.out.println("close socket : " + socket);
			socket.close();
			socket = null;
		}
	}

	synchronized public void closeConnection() throws IOException {
		closeInput();
		closeOutput();
		closeSocket();
	}
	synchronized public void setIO(Socket socket) throws IOException {
		if (socket == null) {
			openIO();
			return;
		}
		if (in == null || out == null || socket == null) {
			closeInput();
			closeOutput();
			closeSocket();
			this.socket = socket;
			openInput();
			openOutput();
		}
	}
	synchronized public void openIO() throws IOException {
		if (in == null || out == null || socket == null) {
			closeInput();
			closeOutput();
			closeSocket();
			socket = new Socket(getHostname(), getReceivePort());
			openInput();
			openOutput();
		}
	}
	
	//public int socketReceivePort() { return socket.getPort(); }
	public boolean isConnected() { return socket != null && socket.isConnected(); }

	public void setRecievePort(int receivePort) { this.receivePort = receivePort; }
	//public void setSendPort(int sendPort) { this.sendPort = sendPort; }
	public int getReceivePort() { return receivePort; }
	//public int getSendPort() { return sendPort; }

	public char getNodeType() { return nodeType; }
	public String getHostname() { return ip.getHostName(); }
	public String getIPAddress() { return ip.getHostAddress(); }
}