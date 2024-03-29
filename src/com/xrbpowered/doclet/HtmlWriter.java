package com.xrbpowered.doclet;

import static com.xrbpowered.doclet.WriterUtils.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.Doc;
import com.sun.javadoc.ExecutableMemberDoc;
import com.sun.javadoc.MemberDoc;
import com.sun.javadoc.PackageDoc;
import com.sun.javadoc.ParameterizedType;
import com.sun.javadoc.SeeTag;
import com.sun.javadoc.Tag;
import com.sun.javadoc.Type;
import com.sun.javadoc.TypeVariable;
import com.sun.javadoc.WildcardType;

public abstract class HtmlWriter {

	public PrintStream out;
	private ByteArrayOutputStream bytes = null;
	
	public abstract void print();
	protected abstract Doc doc();
	protected abstract String getFilename();

	protected PrintStream beginTmpOut() {
		bytes = new ByteArrayOutputStream();
		PrintStream old = out;
		out = new PrintStream(bytes);
		return old;
	}
	
	protected String endTmpOut(PrintStream old) {
		String s;
		try {
			s = bytes.toString("UTF-8");
		} catch(UnsupportedEncodingException e) {
			s = "";
		}
		out.close();
		out = old;
		return s;
	}
	
	protected void printPageStart(String title, String... navLinks) {
		String rootLink = link().rootLink();
		
		// page header
		out.printf("<!DOCTYPE html>\n<html>\n<head>\n<title>%s</title>\n", title);
		if(Options.date)
			out.printf("<meta name=\"date\" content=\"%s\">\n", currentDate());
		out.println("<meta charset=\"UTF-8\" />");
		out.println("<meta name=\"viewport\" content=\"width=device-width; initial-scale=1.0; maximum-scale=1.0; user-scalable=0\" />");
		out.printf("<link rel=\"stylesheet\" href=\"%sdoc.css\" />\n", rootLink);
		out.printf("<script src=\"%sdoc.js\"></script>\n", rootLink);
		out.println("</head>");
		
		// start page
		out.println("<body class=\"bg\">");
		out.println("<button id=\"upBtn\" title=\"Scroll to top\" onclick=\"scrollUp()\">&#129093;</button>");
		out.println("<div class=\"body\"><div class=\"page\">");
		
		// nav bar
		out.printf("<div class=\"nav\">\n<a href=\"%sindex.html\">%s</a>\n", rootLink, Options.docTitle);
		boolean hasLinks = (navLinks!=null && navLinks.length>0);
		if(hasLinks) {
			for(String link : navLinks) {
				out.println("&#11208; ");
				out.print(link);
			}
		}
		if(hasLinks || !title.equals(Options.docTitle)) {
			out.println("&#11208; ");
			out.println(title);
		}
		out.println("</div>");
		
		out.print(smallerTitle() ? "<h1 class=\"smaller\">" : "<h1>");
		out.printf("<a class=\"toplink\" href=\"%s.html\">%s</a></h1>", getFilename(), title.replaceAll("\\.", ".<wbr/>"));
	}
	
	protected void printPageEnd() {
		out.println("</div></div></body>\n</html>");
	}
	
	protected boolean smallerTitle() {
		return false;
	}
	
	protected String getPackageName() {
		return "";
	}
	
	protected PackageLink link() {
		return PackageLink.forPackage(getPackageName());
	}

	public void createFile() {
		try {
			File dir = PackageLink.getPackageDir(getPackageName());
			if(!dir.exists())
				dir.mkdirs();
			File file = new File(dir, getFilename()+".html");
			out = new PrintStream(file);
			print();
			out.close();
		}
		catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void printNothingHere() {
		out.println("<p class=\"overrides\">Nothing to show.</p>");
	}
	
	public void printSince(Doc doc) {
		for(Tag t : doc.tags("@since")) {
			out.printf("<p class=\"since\">Since: %s</p>\n", t.text());
		}
	}
	
	public void printSeeTags(Doc doc) {
		if(doc.seeTags().length>0) {
			out.print("<h5>See also</h5>\n<p class=\"ind\">");
			boolean first = true;
			for(SeeTag t : doc.seeTags()) {
				if(!first) out.print(", ");
				out.print(tagLink(t));
				first = false;
			}
			out.println("</p>");
		}
	}

	public String packageLink(PackageDoc pkg) {
		return packageLink(pkg, null);
	}

	public String packageLink(PackageDoc pkg, String label) {
		if(label==null || label.isEmpty())
			label = pkg.name();
		if(Doclet.listedPackages.contains(pkg))
			return String.format("<a href=\"%s\" title=\"%s\">%s</a>",
					link().relativeLink(pkg.name()), pkg.name(), pkg.name());
		else
			return String.format("<a class=\"extern\" title=\"%s\">%s</a>", pkg.name(), pkg.name());
	}

	public String classLink(ClassDoc cls) {
		return classLink(cls, true, null);
	}

	public String classLink(ClassDoc cls, String label) {
		return classLink(cls, true, label);
	}

	public String classLink(ClassDoc cls, boolean params, String label) {
		String name = cls.name();
		if(cls.isAnnotationType())
			name = "@"+name;
		if(label==null || label.isEmpty())
			label = name;
		String pstr = params ? typeParamsString(cls.typeParameters(), true) : "";
		if(Doclet.listedClasses.contains(cls))
			return String.format("<a href=\"%s\" title=\"%s\">%s</a>%s",
					link().relativeLink(cls), cls.qualifiedName(), name, pstr);
		else
			return String.format("<a class=\"extern\" title=\"%s\">%s</a>%s", cls.qualifiedName(), name, pstr);
	}

	public String memberLink(MemberDoc mem) {
		return memberLink(mem, null);
	}

	public String memberLink(MemberDoc mem, String label) {
		ClassDoc cls = mem.containingClass();
		boolean sameClass = doc()==cls || mem.isEnumConstant();
		
		String title = mem.qualifiedName();
		String name = sameClass ? mem.name() : String.format("%s.%s", cls.name(), mem.name());
		if(mem instanceof ExecutableMemberDoc) {
			ExecutableMemberDoc met = (ExecutableMemberDoc) mem;
			title += met.signature();
			name += met.flatSignature();
		}
		if(label==null || label.isEmpty())
			label = name;
		
		if(Doclet.listedClasses.contains(cls)) {
			return String.format("<a href=\"%s#%s\" title=\"%s\">%s</a>",
					sameClass ? "" : link().relativeLink(cls), memberAnchor(mem), title, label);
		}
		else
			return String.format("<a class=\"extern\" title=\"%s\">%s</a>", title, label);
	}

	public String typeString(Type type) {
		return typeString(type, false, true);
	}

	private void appendTypeVarBounds(StringBuilder sb, String keyword, Type[] bounds) {
		if(bounds.length>0) {
			sb.append(" ");
			sb.append(keyword);
			sb.append(" ");
			for(int i=0; i<bounds.length; i++) {
				if(i>0) sb.append(" & ");
				sb.append(typeString(bounds[i], false, true));
			}
		}
	}
	
	public String typeString(Type type, boolean isVarArg) {
		return typeString(type, isVarArg, true);
	}
	
	public String typeString(Type type, boolean isVarArg, boolean compact) {
		// Some methods can return null type, which should be handled outside this method.
		// Checking here only for safety.
		if(type==null)
			return "";
		
		StringBuilder sb = new StringBuilder(); 
		WildcardType wt = type.asWildcardType();
		if(wt!=null) {
			sb.append(type.typeName());
			appendTypeVarBounds(sb, "extends", wt.extendsBounds());
			appendTypeVarBounds(sb, "super", wt.superBounds());
			return sb.toString();
		}
		TypeVariable tv = type.asTypeVariable();
		if(tv!=null) {
			String n = type.typeName();
			sb.append(n);
			if(!compact)
				appendTypeVarBounds(sb, "extends", tv.bounds());
			return sb.toString();
		}
		
		if(type.isPrimitive())
			sb.append(type.typeName());
		else {
			ClassDoc cls = type.asClassDoc();
			if(cls==null)
				sb.append(type.simpleTypeName());
			else
				sb.append(classLink(cls, false, null));
		}
		
		ParameterizedType ptype = type.asParameterizedType();
		if(ptype!=null)
			sb.append(typeParamsString(ptype.typeArguments(), compact));
		
		if(isVarArg && !type.dimension().isEmpty())
			sb.append("...");
		else
			sb.append(type.dimension());
		
		return sb.toString();
	}

	public String typeParamsString(Type[] tpars) {
		return typeParamsString(tpars, false);
	}

	public String typeParamsString(Type[] tpars, boolean compact) {
		if(tpars!=null && tpars.length>0) {
			StringBuilder sb = new StringBuilder();
			sb.append("&lt;");
			for(int i=0; i<tpars.length; i++) {
				if(i>0) sb.append(", ");
				sb.append(typeString(tpars[i], false, compact));
			}
			sb.append("&gt;");
			return sb.toString();
		}
		else
			return "";
	}
	
	public String tagLink(SeeTag see) {
		String codeFmt = see.name().equals("@linkplain") ? "%s" : "<code>%s</code>";
		if(see.referencedMember()!=null) {
			return String.format(codeFmt, memberLink(see.referencedMember(), see.label()));
		}
		else {
			if(see.referencedMemberName()!=null)
				codeFmt = String.format(codeFmt, "%s."+see.referencedMemberName());
			
			ClassDoc c = see.referencedClass();
			if(c!=null)
				return String.format(codeFmt, classLink(c, true, see.label()));
			else {
				PackageDoc pkg = see.referencedPackage();
				if(pkg!=null)
					return String.format(codeFmt, packageLink(pkg, see.label()));
				else {
					String s = see.text();
					int hash = s.indexOf('#');
					if(hash>=0)
						s = s.substring(0, hash);
					return String.format(codeFmt, s);
				}
			}
		}
	}

	private static final Pattern blockTags = Pattern.compile("(p)|(h\\d)|(ul)|(ol)|(dl)|(hr)|(pre)|(blockquote)|(table)|(div)", Pattern.CASE_INSENSITIVE);
	private static final Pattern scriptTag = Pattern.compile("script", Pattern.CASE_INSENSITIVE);
	private static final Pattern scriptStrip = Pattern.compile("\\<script.*?\\</script.*?\\>", Pattern.CASE_INSENSITIVE+Pattern.MULTILINE+Pattern.DOTALL);
	
	private static int findHtmlTags(String s, Pattern tagPattern) {
		Matcher m = tagPattern.matcher(s);
		int n = s.length();
		int start = 0;
		while(start<n) {
			int tagStart = s.indexOf('<', start);
			if(tagStart<0 || tagStart==n)
				return -1;
			m.region(tagStart+1, n);
			if(m.lookingAt())
				return tagStart;
			else
				start = tagStart+1;
		}
		return 0;
	}
	
	public void printCommentText(Tag[] tags, boolean stopOnBlock) {
		for(Tag t : tags) {
			if(t instanceof SeeTag)
				out.print(tagLink((SeeTag) t));
			else if(t.kind().equals("@code")) {
				String s = t.text()
					.replaceAll("\\&", "&amp;")
					.replaceAll("\\<", "&lt;")
					.replaceAll("\\>", "&gt;");
				out.printf("<code>%s</code>", s);
			}
			else {
				String s = t.text();
				if(t.name().equals("@literal"))
					s = s.trim();
				if(findHtmlTags(s, scriptTag)>=0) {
					Doclet.rootDoc.printWarning("Not allowed to have <script> in comments. Did you forget &lt; or {@code}?");
					s = scriptStrip.matcher(s).replaceAll("");
				}
				if(stopOnBlock) {
					int end = findHtmlTags(s, blockTags);
					if(end>=0) {
						out.print(s.substring(0, end));
						return;
					}
				}
				out.print(s);
			}
		}
	}

	public void printCommentLine(Tag[] tags) {
		printCommentText(tags, true);
	}

	public void printCommentPar(Tag[] tags) {
		out.print("<div class=\"comment\"><p>"); // using only opening <p> within this block (for back/compat)
		printCommentText(tags, false);
		out.println("</div>");
	}
	
	public static String currentDate() {
		return new SimpleDateFormat("yyyy-MM-dd").format(Calendar.getInstance().getTime());
	}
}
