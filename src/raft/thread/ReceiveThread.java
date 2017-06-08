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
}
