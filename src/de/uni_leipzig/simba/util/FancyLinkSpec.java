package de.uni_leipzig.simba.util;

/**
 * @author Tommaso Soru <tsoru@informatik.uni-leipzig.de>
 *
 */
public class FancyLinkSpec {
	
	public static String display(String linkspec) {
		String out = "";
		
		int indent = 0;
		for(int i=0; i<linkspec.length(); i++) {
			String s = "" + linkspec.charAt(i);
			switch(s) {
			case "(":
				s = "(\n" + tabs(++indent);
				break;
			case ")":
				s = "\n" + tabs(--indent) + ")";
				break;
			case ",":
				s = ",\n" + tabs(indent);
				break;
			}
			out += s;
		}
		
		return out;
	}
	
	private static String tabs(int n) {
		String out = "";
		for(int i=0; i<n; i++)
			out += '\t';
		return out;
	}
	
	public static void main(String[] args) {
		String fancyLS = display("OR(levenshtein(x.title,y.authors)|0.9041,OR(cosine(x.authors,y.title)|0.8744,"
				+ "OR(levenshtein(x.title,y.authors)|0.8390,jaccard(x.title,y.title)|0.2662)|0.2662)|0.3453)>=0.6516");
		System.out.println(fancyLS);
	}
	
}
