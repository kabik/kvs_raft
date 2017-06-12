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
	public static final int SOCKET_TIMEOUT = 0;
	
	private Raft raft;
	
	private InetAddress ip;
	private int receivePort;

	private Socket socket = null;
	private BufferedReader  in = null;
	private PrintWriter out = null;

	public Server(Raft raft, String address) {
		this.raft = raft;
		try {
			this.ip = InetAddress.getByName(address);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public String toString() {
		return getHostname() + "::" + receivePort;
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
		synchronized (out) {
			out.println(str);
		}
	}
	public String receive() throws IOException {
		return in.readLine();
	}

	synchronized public void openInput() throws IOException {
		in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	}
	synchronized public void openOutput() throws IOException {
		out = new PrintWriter(socket.getOutputStream(), true);
	}
	synchronized public void closeInput() throws IOException {
		if (in != null) {
			in.close();
			in = null;
		}
	}
	synchronized public void closeOutput() throws IOException {
		if (out != null) {
			out.close();
			out = null;
		}
	}
	synchronized public void closeSocket() throws IOException {
		if (socket != null) {
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
			socket.setSoTimeout(SOCKET_TIMEOUT);
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
			socket.setSoTimeout(SOCKET_TIMEOUT);
			openInput();
			openOutput();
		}
	}
	
	public boolean isConnected() {
		return socket != null && socket.isConnected();
	}

	public void setRecievePort(int receivePort) {
		this.receivePort = receivePort;
	}
	public int getReceivePort() {
		return receivePort;
	}

	public String getHostname() {
		return ip.getHostName();
	}
	public String getIPAddress() {
		return ip.getHostAddress();
	}
}