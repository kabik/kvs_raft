package raft.log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Collections;

public class Log {
	public static final int GROUP_COMMIT_SIZE = 1;

	String filename = "/tmp/kvs_raft/log_";
	private FileOutputStream fos;

	private File logfile;
	private List<Entry> entryList;
	private int commit_count;
	private int preWrittenIndex;
	private int writtenIndex;

	public Log(String surfix) {
		filename = filename + surfix;
		logfile = new File(filename);
		commit_count = 0;
		preWrittenIndex = -1;
		writtenIndex = -1;

		entryList = Collections.synchronizedList(new LinkedList<Entry>());
		try {
			fos = new FileOutputStream(logfile, true);
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line;
			while ((line = br.readLine()) != null) {
				String[] s = line.split(":", 2);
				entryList.add(new Entry(Integer.parseInt(s[0]), s[1]));
			}
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// write all line over the file
	/* shuuseishuusei */
	private synchronized void writeAll() {
		System.out.println("writeAll"); //
		boolean success = false;
		while (!success) {
			try {
				fos.close();
				fos = new FileOutputStream(logfile, false);
				for (Entry entry : entryList) {
					fos.write((entry.toString()+'\n').getBytes());
				}
				sync();
				success = true;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public synchronized String toString() {
		StringBuilder sb = new StringBuilder();
		for (Entry e : entryList) {
			sb.append(e).append("\n");
		}
		return sb.toString();
	}

	// add entries to log without checking (for client input)
	public synchronized int add(Entry...entries) {
		try {
			for (Entry entry : entries) {
				entryList.add(entry);
				commit_count++;
				preWrittenIndex++;
			}
			if (commit_count >= GROUP_COMMIT_SIZE) {
				sync();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return entryList.size() - 1;
	}
	// add entries to log with checking (for append Entries RPC)
	public synchronized int add(int startIndex, Entry...entries) {
		try {
			int newEntryIndex = startIndex;
			for (Entry entry : entries) {
				if (!match(newEntryIndex, entry.getTerm())) {
					removeAfter(newEntryIndex);
				}
				entryList.add(entry);
				commit_count++;
				preWrittenIndex++;
			}
			if (commit_count >= GROUP_COMMIT_SIZE) {
				sync();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return writtenIndex;
	}
	
	public synchronized Entry remove() {
		Entry ret = entryList.remove(lastIndex());
		writeAll();
		return ret;
	}
	// remove entries after index
	public synchronized void removeAfter(int index) {
		boolean flag = false;
		while (entryList.size() > index) {
			entryList.remove(lastIndex());
			flag = true;
		}
		if (flag) { writeAll(); }
		preWrittenIndex = writtenIndex = entryList.size() - 1;
	}
	public void sync() throws IOException {
		///*
		List<Entry> entries = entryList.subList(writtenIndex + 1, size());
		for (Entry entry : entries) {
			fos.write((entry.toString()+'\n').getBytes());
		}
		fos.getFD().sync();
		//*/
		commit_count = 0;
		writtenIndex = preWrittenIndex;
	}
	public String getFilename() { return this.filename; }
	public Entry get(int index) { return entryList.get(index); }
	public Entry getLastEntry() { return entryList.get(entryList.size()-1); }
	public boolean match(int index, int term) {
		return index < 0 || entryList.size() < index || ( entryList.size() > index && entryList.get(index).getTerm() == term );
	}
	public boolean isEmpty() { return entryList.isEmpty(); }
	public synchronized int size() { return entryList.size(); }
	public int lastIndex() { return entryList.size() - 1; }
	public int lastLogTerm() { return (entryList.size() > 0) ? entryList.get(lastIndex()).getTerm() : -1; }

	public int getWrittenIndex() { return writtenIndex; }
}
