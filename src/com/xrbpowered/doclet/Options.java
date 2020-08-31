package com.xrbpowered.doclet;

import com.sun.javadoc.PackageDoc;

public class Options {

	public static final String baseClassPath = "com/xrbpowered/doclet";
	public static final String defaultCSS = "resources/doc.css";
	public static final String defaultJS = "resources/doc.js";

	public static String outPath = ".";
	public static String docTitle = "API Reference";
	public static String cssFile = null;
	public static String jsFile = null;
	public static String overviewPkg = "overview";
	
	public static boolean date = false;
	
	public static void loadOptions(String[][] options) {
		for(String[] opt : options) {
			switch(opt[0]) {
				case "-d":
					outPath = opt[1];
					break;
				case "-doctitle":
					docTitle = opt[1];
					break;
				case "-css":
					cssFile = opt[1];
					break;
				case "-js":
					jsFile = opt[1];
					break;
				case "-overview":
					overviewPkg = opt[1];
					break;
				case "-date":
					date = true;
					break;
			}
		}
	}
	
	public static int optionLength(String opt) {
		switch(opt) {
			case "-d":
			case "-doctitle":
			case "-css":
			case "-js":
			case "-overview":
				return 2;
			case "-date":
				return 1;
			default:
				return 0;
		}
	}
	
	public static boolean isOverview(PackageDoc pkg) {
		return pkg.name().equals(overviewPkg);
	}

}
