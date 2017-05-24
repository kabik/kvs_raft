package create_input;

import java.util.TreeSet;

public class Create_Inputfile {
	public static void main(String args[]) {
		TreeSet<String> set = new TreeSet<String>();
		int MAX = Integer.parseInt(args[0]);
		
		//int count = 0;
		while (set.size() < MAX) {
			//System.out.print(set.size()+" ");
			
			StringBuilder keySB = new StringBuilder();
			for (int i = 0; i < 8; i++) {
				int add = (int)( Math.random() * 25 );
				keySB.append((char)('a'+add));
			}
			
			String key = keySB.toString();
			
			//int value = (int)( Math.random() * 100000 );			
			int value = set.size() + 1;
			StringBuilder valueSB = new StringBuilder();
			for (int i = 0; i < 10; i++) {
				if (value > 0) {
					valueSB.append(value % 10);
				} else {
					valueSB.append('0');
				}
				value /= 10;
			}
			
			if (set.contains(key)) {
			//	count++;
			} else {
				set.add(key);
				System.out.println("put " + key + " " + valueSB.reverse());
			}
		}
		//System.out.println(count);
	}
}
