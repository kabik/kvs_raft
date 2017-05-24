package check;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.TreeMap;

public class KVSOnly {

	public static void main(String[] args) {
		long start = System.currentTimeMillis();
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		TreeMap<String, String> map = new TreeMap<String, String>();

		while(true) {
			try {
				String line = reader.readLine();
				if (line == null) {	break; }
				
				String s[] = line.split(" ");
				map.put(s[1], s[2]);
				System.out.println(line);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
		long end = System.currentTimeMillis();
		System.out.println((end - start) + " ms");
	}

}
