package com.xrbpowered.doclet;

public class Options {

	public static String outPath = ".";
	public static String docTitle = "API Reference";
	
	public static void loadOptions(String[][] options) {
		for(String[] opt : options) {
			switch(opt[0]) {
				case "-d":
					outPath = opt[1];
					break;
				case "-doctitle":
					docTitle = opt[1];
					break;
			}
		}
	}
	
	public static int optionLength(String opt) {
		switch(opt) {
			case "-d":
			case "-doctitle":
				return 2;
			default:
				return 0;
		}
	}

}
