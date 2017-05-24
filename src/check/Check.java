package check;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Check {
	public static void main(String args[]) throws IOException {
		BufferedReader bf = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("same check");
		
		String preLine = bf.readLine();
		String line;
		int count[] = new int[10001];
		
		while ((line = bf.readLine()) != null) {
			String lines[] = line.split(" ");
			count[Integer.parseInt(lines[2])]++;
			
			if (lines.equals(preLine)) {
				System.out.println("same : " + line);
			}
			
			preLine = line;
		}
		
		for (int i = 1; i <= 10000; i++) {
			//if (count[i] == 0) {
				System.out.println("count[" + i + "] = " + count[i]);
			//}
		}
		
		System.out.println("end");
	}
}
