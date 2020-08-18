package com.xrbpowered.doclet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;

import com.sun.javadoc.PackageDoc;

import static com.xrbpowered.doclet.Options.*;

public abstract class FileUtils {
	
	private static File root;
	
	public static void createRoot() {
		root = new File(outPath);
		if(!root.exists() && !root.mkdirs())
			throw new RuntimeException(new FileNotFoundException(outPath));
	}
	
	public static void createPackageList(List<PackageDoc> pkgList) {
		try {
			File file = new File(root, "package-list");
			PrintStream out = new PrintStream(file);
			for(PackageDoc pkg : pkgList) {
				out.println(pkg.name());
			}
			out.close();
		}
		catch(IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void copyStyleFiles() {
		copyFileFallback(cssFile, defaultCSS, new File(root, "doc.css"));
		copyFileFallback(jsFile, defaultJS, new File(root, "doc.js"));
	}
	
	private static byte[] loadBytes(InputStream s) throws IOException {
		DataInputStream in = new DataInputStream(s);
		byte bytes[] = new byte[in.available()];
		in.readFully(bytes);
		in.close();
		return bytes;
	}
	
	private static void saveBytes(OutputStream s, byte[] bytes) throws IOException {
		DataOutputStream out = new DataOutputStream(s);
		out.write(bytes);
		out.close();
	}
	
	public static String getContent(File file) {
		try {
			InputStream in = new FileInputStream(file);
			return new String(loadBytes(in));
		}
		catch(IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void copyResource(String classPath, File dest) {
		try {
			ClassLoader cl = FileUtils.class.getClassLoader();
			InputStream in = cl.getResourceAsStream(Options.baseClassPath+"/"+classPath);
			if(in==null)
				throw new FileNotFoundException(classPath);
			OutputStream out = new FileOutputStream(dest);
			saveBytes(out, loadBytes(in));
		}
		catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void copyFile(File src, File dest) {
		try {
			InputStream in = new FileInputStream(src);
			OutputStream out = new FileOutputStream(dest);
			saveBytes(out, loadBytes(in));
		}
		catch(IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void copyFileFallback(String srcFilePath, String fallbackClassPath, File dest) {
		if(srcFilePath!=null) {
			if(srcFilePath.equalsIgnoreCase("none"))
				return;
			File src = new File(srcFilePath);
			if(src.exists()) {
				copyFile(src, dest);
				return;
			}
			else
				System.err.printf("File not found: %s\nUsing default replacement.\n", src.getAbsolutePath());
		}
		copyResource(fallbackClassPath, dest);
	}

}
