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
		
		printClassList("Interfaces", pkg.interfaces());
		printClassList("Enums", pkg.enums());
		printClassList("Classes", pkg.ordinaryClasses());
		printClassList("Exceptions", pkg.exceptions());
		printClassList("Errors", pkg.errors());
		printClassList("Annotations", pkg.annotationTypes());

		out.println("</div>");
		
		printPageEnd();
	}
	
	@Override
	protected boolean smallerTitle() {
		return true;
	}
	
	private void printClassList(String title, ClassDoc[] list) {
		if(list.length==0)
			return;
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
			out.printf("<code><span class=\"name\">%s</span>", classLink(c));
			printTypeParams(c.typeParameters(), true); // TODO compact type parameters
			out.print("</code>");
			out.print("</td><td>\n");
			Tag[] info = c.firstSentenceTags();
			if(info!=null && info.length>0)
				printCommentText(info);
			out.println("</td></tr>");
			
			i++;
		}
		out.println("</table>");
		out.println("</div>");
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
