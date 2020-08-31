package com.xrbpowered.doclet;

import static com.xrbpowered.doclet.Options.*;

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
import com.sun.javadoc.SourcePosition;

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
		Doclet.rootDoc.printNotice("... Copying style files");
		copyFileFallback(cssFile, defaultCSS, new File(root, "doc.css"));
		copyFileFallback(jsFile, defaultJS, new File(root, "doc.js"));
	}
	
	private static void copyDocFiles(PackageDoc pkg, String notice, File destDir) {
		// package must contain package-info.java or package.html in order to have source position
		SourcePosition pos = pkg.position();
		if(pos==null)
			return;
		File srcDir = new File(pos.file().getParentFile(), "doc-files");
		if(srcDir.isDirectory()) {
			Doclet.rootDoc.printNotice(notice);
			copyDir(srcDir, destDir);
		}
	}
	
	public static void copyDocFiles(PackageDoc pkg) {
		copyDocFiles(pkg, "... Copying doc files", new File(PackageLink.getPackageDir(pkg.name()), "doc-files"));
	}

	public static void copyOverviewDocFiles(PackageDoc pkg) {
		copyDocFiles(pkg, "... Copying overview doc files", new File(root, "doc-files"));
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

	public static void copyDir(File src, File dest) {
		if(!dest.exists())
			dest.mkdirs();
		File[] files = src.listFiles();
		for(File f : files) {
			String name = f.getName();
			File df = new File(dest, name);
			if(f.isDirectory()) {
				if(!name.startsWith("."))
					copyDir(f, df);
			}
			else {
				copyFile(f, df);
			}
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
				Doclet.rootDoc.printWarning(String.format("File not found: %s\nUsing default replacement.\n", src.getAbsolutePath()));
		}
		copyResource(fallbackClassPath, dest);
	}

}
