package client.createInput;

import java.util.TreeSet;

public class CreateInput {
	public static void main(String args[]) {
		TreeSet<String> set = new TreeSet<String>();
		int MAX = Integer.parseInt(args[0]);
		
		while (set.size() < MAX) {
			StringBuilder keySB = new StringBuilder();
			for (int i = 0; i < 8; i++) {
				int add = (int)( Math.random() * 25 );
				keySB.append((char)('a'+add));
			}
			
			String key = keySB.toString();
			
			int value = set.size() + 1;
			StringBuilder valueSB = new StringBuilder();
			for (int i = 0; i < 8; i++) {
				if (value > 0) {
					valueSB.append(value % 10);
				} else {
					valueSB.append('0');
				}
				value /= 10;
			}
			
			if (set.contains(key)) {
			} else {
				set.add(key);
				System.out.println("put " + key + " " + valueSB.reverse());
			}
		}
	}
}
