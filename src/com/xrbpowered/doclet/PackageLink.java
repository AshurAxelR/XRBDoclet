package com.xrbpowered.doclet;

import java.io.File;
import java.util.HashMap;

import com.sun.javadoc.ClassDoc;

public class PackageLink {

	private static HashMap<String, PackageLink> packages = new HashMap<>();
	
	public final String pkg;
	private HashMap<String, String> relativeLinks = new HashMap<>();

	public PackageLink(String pkg) {
		this.pkg = pkg;
	}
	
	public String relativeLink(String dst) {
		if(dst==null)
			dst = "";
		String link = relativeLinks.get(dst);
		if(link==null) {
			link = calcLink(pkg, dst);
			relativeLinks.put(dst, link);
		}
		return link;
	}
	
	public String relativeLink(ClassDoc cls) {
		return String.format("%s%s.html", relativeLink(cls.containingPackage().name()), cls.name());
	}
	
	public String rootLink() {
		return relativeLink("");
	}
	
	public static PackageLink forPackage(String pkg) {
		if(pkg==null)
			pkg = "";
		PackageLink link = packages.get(pkg);
		if(link==null) {
			link = new PackageLink(pkg);
			packages.put(pkg, link);
		}
		return link;
	}

	public static PackageLink root() {
		return forPackage("");
	}
	
	public static File getPackageDir(String pkg) {
		return new File(Options.outPath, root().relativeLink(pkg));
	}
	
	public static int commonPrefixLength(String s1, String s2) {
		int n = Math.min(s1.length(), s2.length());
		for(int i=0; i<n; i++) {
			if(s1.charAt(i)!=s2.charAt(i))
				return i;
		}
		return n;
	}
	
	private static String calcLink(String src, String dst) {
		StringBuilder link = new StringBuilder();
		
		int slen = src.length();
		int dlen = dst.length();
		
		int pref = commonPrefixLength(src, dst);
		if(pref==slen && pref==dlen)
			return "";
		
		if(pref<slen) {
			int i=pref;
			if(src.charAt(pref)=='.') i++;
			while(i>=0) {
				link.append("../");
				i = src.indexOf('.', i+1);
			}
		}
		if(pref<dlen) {
			if(dst.charAt(pref)=='.') pref++;
			link.append(dst.substring(pref).replace('.', '/'));
			link.append("/");
		}
		return link.toString();
	}

}
