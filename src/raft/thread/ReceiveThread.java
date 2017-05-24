package raft.thread;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import raft.*;

public class ReceiveThread extends AbstractThread {
	public ReceiveThread(Raft raft) {
		super(raft);
	}

	@Override
	public void run() {
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(raft.getMe().getReceivePort(), Raft.BACKLOG_SIZE);
			//System.out.println("Open Input : port="
					//+ serverSocket.getLocalPort());
			while (true) {
				Socket socket = serverSocket.accept();
				System.out.println("new connection : " + socket);
				new ProcessMessageThread(raft, socket).start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (serverSocket != null) {
					serverSocket.close();
				}
			} catch (IOException e) {}
		}
	}

	/*
	private ServerSocket serverSocket = null;
	private Socket receiveSocket;
	private DataInputStream dis;
	private int port;

	public ReceiveThread(Raft m, int port) {
		super(m);
		this.port = port;
	}

	private void openReceive() throws IOException {
		receiveSocket = serverSocket.accept();
		dis = new DataInputStream(receiveSocket.getInputStream());
	}
	private void closeReceive() throws IOException {
		dis.close();
		receiveSocket.close();
	}
	private String receive() throws IOException {
		return dis.readUTF();
	}

	@Override
	public void run()  {
		try {
			serverSocket = new ServerSocket(port, Raft.BACKLOG_SIZE);
		} catch (IOException e) {
			e.printStackTrace();
		}
		while(!_halt) {
			try {
				System.out.println("open input at port " + port);
				openReceive();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			while (true) {
				try {
					String str = receive();
					//System.out.println(str);
					if (!str.isEmpty()) {
						m.addMessage(str);
						m.comebackProcessMessage();
					}
				} catch (Exception e) {
					//e.printStackTrace();
					try {
						closeReceive();
						break;
					} catch (IOException e1) {
						e1.printStackTrace();
						System.exit(1);
					}
				}
			}
		}
	}
	*/
}
