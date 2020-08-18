package com.xrbpowered.doclet;

import static com.xrbpowered.doclet.WriterUtils.*;

import java.util.ArrayList;
import java.util.List;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.Doc;

public class ClassIndexWriter extends HtmlWriter {

	public static final String filename = "allclasses";
	
	public final List<ClassDoc> classList;
	
	public ClassIndexWriter(ClassDoc[] list) {
		classList = new ArrayList<>(list.length);
		for(ClassDoc cls : list)
			classList.add(cls);
		classList.sort(classSimpleNameSort);
	}
	
	@Override
	protected Doc doc() {
		return null;
	}

	@Override
	public void print() {
		printPageStart("Class Index");

		PackageLink link = PackageLink.root();
		out.println("<div class=\"index\"><p>");
		for(ClassDoc c : classList) {
			out.printf("<a href=\"%s\" title=\"%s\">%s</a>", link.relativeLink(c), c.qualifiedName(), c.simpleTypeName());
			out.println("<br/>");
		}
		out.println("</p></div>");
		
		printPageEnd();
	}

	@Override
	protected String getFilename() {
		return filename;
	}
	
}
