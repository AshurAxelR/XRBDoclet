package com.xrbpowered.doclet;

import java.util.HashSet;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.LanguageVersion;
import com.sun.javadoc.PackageDoc;
import com.sun.javadoc.RootDoc;

public class Doclet {

	public static HashSet<ClassDoc> listedClasses = new HashSet<>();
	public static HashSet<PackageDoc> listedPackages = new HashSet<>();

	public static RootDoc rootDoc;
	
	public static boolean start(RootDoc root) {
		rootDoc = root;
		Options.loadOptions(root.options());
		root.printNotice("Using XRB powered custom doclet.");
		FileUtils.createRoot();
		
		for(PackageDoc pkg : root.specifiedPackages()) {
			if(Options.isOverview(pkg))
				continue;
			listedPackages.add(pkg);
		}
		for(ClassDoc cls : root.classes())
			listedClasses.add(cls);

		new PackageIndexWriter(root).createFile();
		new ClassIndexWriter(root.classes()).createFile();
		
		for(PackageDoc pkg : root.specifiedPackages()) {
			if(Options.isOverview(pkg))
				continue;
			root.printNotice(pkg.name());
			new PackageDocWriter(pkg).createFile();
			FileUtils.copyDocFiles(pkg);
		}
		
		for(ClassDoc cls : root.classes()) {
			root.printNotice(cls.qualifiedName());
			new ClassDocWriter(cls).createFile();
		}

		FileUtils.copyStyleFiles();
		return true;
	}
	
	public static int optionLength(String option) {
		return Options.optionLength(option);
	}

	public static LanguageVersion languageVersion() {
		return LanguageVersion.JAVA_1_5;
	}
}
