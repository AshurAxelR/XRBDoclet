package com.xrbpowered.doclet;

import static com.xrbpowered.doclet.WriterUtils.*;

import java.util.ArrayList;
import java.util.List;

import com.sun.javadoc.Doc;
import com.sun.javadoc.PackageDoc;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.Tag;

public class PackageIndexWriter extends HtmlWriter {

	public static final String filename = "index";
	
	public final RootDoc root;
	public final List<PackageDoc> pkgList;
	public PackageDoc overview = null;
	
	public PackageIndexWriter(RootDoc root) {
		this.root = root;
		pkgList = new ArrayList<>(root.specifiedPackages().length);
		for(PackageDoc pkg : root.specifiedPackages()) {
			if(Options.isOverview(pkg))
				overview = pkg;
			else
				pkgList.add(pkg);
		}
		pkgList.sort(packageSort);
		
		FileUtils.createPackageList(pkgList);
		if(overview!=null)
			FileUtils.copyOverviewDocFiles(overview);
	}

	@Override
	public void print() {
		printPageStart(Options.docTitle);

		out.println("<div class=\"infocard\"><p><a href=\"allclasses.html\">List of all classes</a></p></div>");
		
		if(overview!=null) {
			printCommentPar(overview.inlineTags());
			printSeeTags(overview);
		}

		// list of packages
		out.println("<h2>Packages</h2>");

		if(pkgList.isEmpty()) {
			printNothingHere();
		}
		else {
			PackageLink link = PackageLink.root();
			out.println("<table>");
			for(PackageDoc pkg : pkgList) {
				out.print("<tr><td>");
				out.printf("<a href=\"%s%s.html\" title=\"%s\">%s</a>",
						link.relativeLink(pkg.name()), PackageDocWriter.filename, pkg.name(), pkg.name());
				out.println("</td><td>");
				Tag[] info = pkg.firstSentenceTags();
				if(info!=null && info.length>0)
					printCommentLine(info);
				out.println("</td></tr>");
			}
			out.println("</table>");
		}
		
		if(Options.date)
			out.printf("<p class=\"date\">Generated %s</p>\n", currentDate());
		
		printPageEnd();
	}

	@Override
	protected Doc doc() {
		return null;
	}

	@Override
	protected String getFilename() {
		return filename;
	}

}
