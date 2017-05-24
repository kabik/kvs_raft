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

	String filename = "/tmp/kvs_on_raft_kajiwara/log_";
	private FileOutputStream fos;

	private File logfile;
	private List<Entry> list;
	private int commit_count;
	private int preWrittenIndex;
	private int writtenIndex;

	public Log(String surfix) {
		filename = filename + surfix;
		logfile = new File(filename);
		commit_count = 0;
		preWrittenIndex = -1;
		writtenIndex = -1;

		list = Collections.synchronizedList(new LinkedList<Entry>());
		try {
			fos = new FileOutputStream(logfile, true);
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line;
			while ((line = br.readLine()) != null) {
				String[] s = line.split(":", 2);
				list.add(new Entry(Integer.parseInt(s[0]), s[1]));
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
	private void writeAll() {
		System.out.println("writeAll"); //
		boolean success = false;
		while (!success) {
			try {
				fos.close();
				fos = new FileOutputStream(logfile, false);
				for (Entry entry : list) {
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
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (Entry e : list) {
			sb.append(e).append("\n");
		}
		return sb.toString();
	}

	// add entries to log without checking (for client input) (no write)
	/*public int add(Entry...entries) {
		for (Entry entry : entries) {
			list.add(entry);
			commit_count++;
			preWrittenIndex++;
		}
		return list.size() - 1;
	}
	// add entries to log with checking (for append Entries RPC) (no write)
	public int add(int startIndex, Entry...entries) {
		int newEntryIndex = startIndex;
		for (Entry entry : entries) {
			if (!match(newEntryIndex, entry.getTerm())) {
				removeAfter(newEntryIndex);
			}
			list.add(entry);
			commit_count++;
			preWrittenIndex++;
		}
	}*/
	// add entries to log without checking (for client input)
	public int add(Entry...entries) {
		try {
			for (Entry entry : entries) {
				list.add(entry);
				//fos.write((entry.toString()+'\n').getBytes()); //sync() de wirte shiteru
				commit_count++;
				preWrittenIndex++;
			}
			if (commit_count >= GROUP_COMMIT_SIZE) {
				sync();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return list.size() - 1;
	}
	// add entries to log with checking (for append Entries RPC)
	public int add(int startIndex, Entry...entries) {
		try {
			int newEntryIndex = startIndex;
			for (Entry entry : entries) {
				if (!match(newEntryIndex, entry.getTerm())) {
					removeAfter(newEntryIndex);
				}
				list.add(entry);
				//fos.write((entry.toString()+'\n').getBytes());
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
	
	public Entry remove() {
		Entry ret = list.remove(lastIndex());
		writeAll();
		return ret;
	}
	// remove entries after index
	public void removeAfter(int index) {
		boolean flag = false;
		while (list.size() > index) {
			list.remove(lastIndex());
			flag = true;
		}
		if (flag) { writeAll(); }
		preWrittenIndex = writtenIndex = list.size() - 1;
	}
	public void sync() throws IOException {
		/*
		List<Entry> entries = list.subList(writtenIndex + 1, list.size());
		for (Entry entry : entries) {
			fos.write((entry.toString()+'\n').getBytes());
		}
		fos.getFD().sync();
		*/
		commit_count = 0;
		writtenIndex = preWrittenIndex;
	}
	public String getFilename() { return this.filename; }
	public Entry get(int index) { return list.get(index); }
	public Entry getLastEntry() { return list.get(list.size()-1); }
	public boolean match(int index, int term) {
		return index < 0 || list.size() < index || ( list.size() > index && list.get(index).getTerm() == term );
	}
	public boolean isEmpty() { return list.isEmpty(); }
	public int size() { return list.size(); }
	public int lastIndex() { return list.size() - 1; }
	public int lastLogTerm() { return (list.size() > 0) ? list.get(lastIndex()).getTerm() : -1; }

	public int getWrittenIndex() { return writtenIndex; }
}
