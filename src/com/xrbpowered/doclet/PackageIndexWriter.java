package com.xrbpowered.doclet;

import static com.xrbpowered.doclet.WriterUtils.*;

import java.util.ArrayList;
import java.util.List;

import com.sun.javadoc.Doc;
import com.sun.javadoc.PackageDoc;
import com.sun.javadoc.Tag;

public class PackageIndexWriter extends HtmlWriter {

	public final List<PackageDoc> pkgList;
	
	public PackageIndexWriter(PackageDoc[] list) {
		pkgList = new ArrayList<>(list.length);
		for(PackageDoc cls : list)
			pkgList.add(cls);
		pkgList.sort(packageSort);
	}

	@Override
	public void print() {
		printPageStart(Options.docTitle);

		out.println("<p><a href=\"allclasses.html\">List of all classes</a></p>");

		out.println("<h2>Packages</h2>");

		PackageLink link = PackageLink.root();
		out.println("<table>");
		for(PackageDoc pkg : pkgList) {
			out.print("<tr><td>");
			out.printf("<a href=\"%sindex.html\" title=\"%s\">%s</a>",
					link.relativeLink(pkg.name()), pkg.name(), pkg.name());
			out.println("</td><td>");
			Tag[] info = pkg.firstSentenceTags();
			if(info!=null && info.length>0)
				printCommentText(info);
			out.println("</td></tr>");
		}
		out.println("</table>");
		
		printPageEnd();
	}

	@Override
	protected Doc doc() {
		return null;
	}

	@Override
	protected String getFilename() {
		return "index";
	}

}
