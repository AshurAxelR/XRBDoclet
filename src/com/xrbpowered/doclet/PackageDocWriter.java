package com.xrbpowered.doclet;

import java.lang.reflect.Modifier;
import java.util.Arrays;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.Doc;
import com.sun.javadoc.PackageDoc;
import com.sun.javadoc.Tag;

import static com.xrbpowered.doclet.WriterUtils.*;

public class PackageDocWriter extends HtmlWriter {

	public static final String filename = "package-summary"; 
	
	public final PackageDoc pkg;
	
	public PackageDocWriter(PackageDoc pkg) {
		this.pkg = pkg;
	}

	@Override
	public void print() {
		printPageStart(getPackageName());
		printSince(pkg);
		printCommentPar(pkg.inlineTags());
		printSeeTags(pkg);
		
		out.println("<div class=\"summary\">");
		out.println("<h2>Summary</h2>");
		
		boolean sum = false;
		sum |= printClassList("Interfaces", pkg.interfaces());
		sum |= printClassList("Enums", pkg.enums());
		sum |= printClassList("Classes", pkg.ordinaryClasses());
		sum |= printClassList("Exceptions", pkg.exceptions());
		sum |= printClassList("Errors", pkg.errors());
		sum |= printClassList("Annotations", pkg.annotationTypes());
		if(!sum)
			printNothingHere();

		out.println("</div>");
		
		printPageEnd();
	}
	
	@Override
	protected boolean smallerTitle() {
		return true;
	}
	
	private boolean printClassList(String title, ClassDoc[] list) {
		if(list.length==0)
			return false;
		Arrays.sort(list, classSort);
		out.println("<div class=\"summary-item\">");
		out.printf("<h5>%s</h5>\n", title);
		out.println("<table>");
		
		int count = list.length;
		String[] mods = new String[count];
		int i = 0;
		boolean hasMods = false;
		for(ClassDoc c : list) {
			mods[i] = Modifier.toString(c.modifierSpecifier() & ~(Modifier.PUBLIC | Modifier.STATIC | Modifier.INTERFACE));
			if(!mods[i].isEmpty())
				hasMods = true;
			i++;
		}
		
		i=0;
		for(ClassDoc c : list) {
			out.print("<tr>");
			if(hasMods) {
				out.print("<td class=\"mods\">");
				if(!mods[i].isEmpty()) {
					out.print("<code>");
					out.print(mods[i]);
					out.print("</code>");
				}
				out.println("</td>");
			}
			out.println("<td class=\"mods\">");
			out.printf("<code><span class=\"name\">%s</span>%s", classLink(c, false), typeParamsString(c.typeParameters()));
			out.print("</code>");
			out.print("</td><td>\n");
			if(isDeprecated(c))
				out.print("<span class=\"depr\">Deprecated</span>");
			else {
				Tag[] info = c.firstSentenceTags();
				if(info.length>0)
					printCommentLine(info);
			}
			out.println("</td></tr>");
			
			i++;
		}
		out.println("</table>");
		out.println("</div>");
		return true;
	}
	
	@Override
	protected String getPackageName() {
		return pkg.name();
	}

	@Override
	protected Doc doc() {
		return pkg;
	}

	@Override
	protected String getFilename() {
		return filename;
	}

}
