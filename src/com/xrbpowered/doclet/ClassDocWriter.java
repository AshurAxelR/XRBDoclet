package com.xrbpowered.doclet;

import static com.xrbpowered.doclet.WriterUtils.*;

import java.io.PrintStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.ConstructorDoc;
import com.sun.javadoc.Doc;
import com.sun.javadoc.ExecutableMemberDoc;
import com.sun.javadoc.FieldDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.ParamTag;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.Tag;
import com.sun.javadoc.ThrowsTag;
import com.sun.javadoc.Type;

public class ClassDocWriter extends HtmlWriter {

	public final ClassDoc cls;
	
	public ClassDocWriter(ClassDoc cls) {
		this.cls = cls;
	}

	@Override
	protected Doc doc() {
		return cls;
	}
	
	@Override
	public void print() {
		printPageStart(cls.name(),
			String.format("<a href=\"index.html\">%s</a>", getPackageName())
		);
		
		printClassSignature();
		printTypeParamComments(cls.typeParamTags());
		printInfoCard();
		
		// class comment
		printSince(cls);
		printCommentPar(cls.inlineTags());
		printSeeTags(cls);

		// summary
		out.println("<div class=\"summary\">");
		out.println("<h2>Summary</h2>");
		printInnerClasses();
		
		// do not sort enum constants!
		printFieldList("Enum constants", Arrays.asList(cls.enumConstants()), true);
		
		ArrayList<FieldDoc> allFields = new ArrayList<>();
		collectInheritedFields(cls, allFields, null);
		allFields.sort(memberSort);
		printSummaryFields("Constants", allFields, Modifier.STATIC | Modifier.FINAL, 0);
		printSummaryFields("Static Fields", allFields, Modifier.STATIC, Modifier.FINAL);
		printSummaryFields("Instance Fields", allFields, 0, Modifier.STATIC);
		
		Arrays.sort(cls.constructors(), methodSort);
		printSummaryMethods("Constructors", Arrays.asList(cls.constructors()), 0, 0);

		ArrayList<MethodDoc> allMethods = new ArrayList<>();
		collectInheritedMethods(cls, allMethods, null);
		allMethods.sort(methodSort);
		printSummaryMethods("Abstract Methods", allMethods, Modifier.ABSTRACT, Modifier.STATIC);
		printSummaryMethods(cls.isInterface() ? "Interface Methods" : "Instance Methods", allMethods, 0, Modifier.ABSTRACT | Modifier.STATIC);
		printSummaryMethods("Static Methods", allMethods, Modifier.STATIC, 0);
		
		out.println("</div>");
		
		// details
		out.println("<div class=\"details\">");
		for(FieldDoc fld : cls.enumConstants()) {
			printFieldDetails(fld);
		}
		for(FieldDoc fld : cls.fields()) {
			printFieldDetails(fld);
		}
		for(ConstructorDoc con : cls.constructors()) {
			if(!isDefaultConstructor(con))
				printMethodDetails(con);
		}
		for(MethodDoc met : cls.methods()) {
			printMethodDetails(met);
		}
		out.println("</div>");
		printPageEnd();
	}
	
	private void printClassSignature() {
		out.printf("<pre>%s", cls.modifiers());
		if(!cls.isInterface())
			out.print(cls.isEnum() ? " enum" : " class");
		out.printf(" <span class=\"name\">%s</span>", cls.name());
		printTypeParams(cls.typeParameters());
		out.println();
		if(cls.superclass()!=null
				&& !cls.superclass().qualifiedName().equals("java.lang.Object")
				&& !cls.superclass().qualifiedName().equals("java.lang.Enum")) {
			out.print("extends ");
			out.print(typeString(cls.superclassType()));
			out.println();
		}
		if(cls.interfaces().length>0) {
			out.print(cls.isInterface() ? "extends " : "implements ");
			for(int i=0; i<cls.interfaceTypes().length; i++) {
				if(i>0) out.print(", ");
				out.print(typeString(cls.interfaceTypes()[i]));
			}
			out.println();
		}
		out.println("</pre>");
	}
	
	private void printInfoCard() {
		PrintStream oldOut = beginTmpOut(); 
		printClassHierarchy();
		printSuperinterfaces();
		if(cls.containingClass()!=null) {
			out.print("<dt>Enclosing class:</dt><dd>");
			out.print(classLink(cls.containingClass()));
			out.println("</dd>");
		}
		printKnownSubclasses();
		String infoCard = endTmpOut(oldOut);
		if(!infoCard.isEmpty()) {
			out.println("<div class=\"infocard\"><dl>");
			out.print(infoCard);
			out.println("</dl></div>");
		}
	}

	private void printHierarchy(Type t) {
		ClassDoc c = t.asClassDoc();
		if(c!=null && c.superclass()!=null) {
			printHierarchy(c.superclassType());
			out.println(" &#11208;");
		}
		out.print("<code>");
		if(c==cls) {
			out.print(cls.name());
			printTypeParams(cls.typeParameters());
		}
		else
			out.print(typeString(t));
		out.print("</code>");
	}

	private void printClassHierarchy() {
		if(cls.isInterface())
			return;
		out.println("<dt>Hierarchy:</dt><dd>");
		printHierarchy(cls);
		out.println("</dd>");
	}
	
	private void collectSuperInterfaces(ClassDoc c, List<ClassDoc> out, HashSet<ClassDoc> uniques) {
		if(uniques==null)
			uniques = new HashSet<>();
		if(c.superclass()!=null) {
			ClassDoc sup = c.superclass();
			if(sup.isInterface() && !uniques.contains(sup))
				out.add(sup);
			collectSuperInterfaces(sup, out, uniques);
		}
		for(ClassDoc sup : c.interfaces()) {
			if(sup.isInterface() && !uniques.contains(sup))
				out.add(sup);
			collectSuperInterfaces(sup, out, uniques);
		}
	}
	
	private void printSuperinterfaces() {
		List<ClassDoc> list = new ArrayList<>();
		collectSuperInterfaces(cls, list, null);
		list.sort(classSort);
		
		if(!list.isEmpty()) {
			out.printf("<dt>All %s:</dt><dd>", cls.isInterface() ? "superinterfaces" : "implemented interfaces");
			printPlainClassList(list);
			out.println("</dd>");
		}
	}
	
	private void printKnownSubclasses() {
		if(cls.isInterface())
			return;
		List<ClassDoc> list = new ArrayList<>();
		for(ClassDoc c : Doclet.listedClasses) {
			if(c.superclass()==this.cls)
				list.add(c);
		}
		if(!list.isEmpty()) {
			list.sort(classSort);
			out.print("<dt>Known direct subclasses:</dt><dd>");
			printPlainClassList(list);
			out.println("</dd>");
		}
	}
	
	private void printPlainClassList(List<ClassDoc> list) {
		boolean first = true;
		for(ClassDoc c : list) {
			if(!first) out.print(", ");
			out.print(classLink(c));
			printTypeParams(c.typeParameters(), true);
			first = false;
		}
	}

	private void printSummaryFields(String title, List<? extends FieldDoc> list, int mods, int noMods) {
		ArrayList<FieldDoc> fields = new ArrayList<>();
		for(FieldDoc met : list) {
			int m = met.modifierSpecifier();
			if((m&mods)==mods && (m&noMods)==0)
				fields.add(met);
		}
		printFieldList(title, fields, false);
	}

	private void printSummaryMethods(String title, List<? extends ExecutableMemberDoc> list, int mods, int noMods) {
		ArrayList<ExecutableMemberDoc> mets = new ArrayList<>();
		for(ExecutableMemberDoc met : list) {
			int m = met.modifierSpecifier();
			if(!cls.isInterface() && met.containingClass().isInterface())
				m |= Modifier.ABSTRACT;
			if((m&mods)==mods && (m&noMods)==0)
				mets.add(met);
		}
		printMethodList(title, mets);
	}

	private void printInnerClasses() {
		if(cls.innerClasses().length==0)
			return;
		out.println("<div class=\"summary-item\">");
		out.printf("<h5>%s</h5>\n", "Nested Classes");
		out.println("<table>");
		for(ClassDoc c : cls.innerClasses()) {
			out.print("<tr><td class=\"mods\">");
			
			out.print("<code>");
			String mods = Modifier.toString(c.modifierSpecifier() & ~Modifier.PUBLIC);
			if(!mods.isEmpty()) {
				out.print(mods);
				if(!c.isInterface()) out.print(" ");
			}
			if(!c.isInterface())
				out.print(" class");
			out.print("</code>");
			out.println("</td><td>");
			
			out.printf("<code><span class=\"name\">%s</span>", classLink(c));
			printTypeParams(c.typeParameters());
			out.print("</code>\n");
			
			Tag[] info = c.firstSentenceTags();
			if(info!=null && info.length>0) {
				out.print("<br/>");
				printCommentText(info);
			}
			out.println("</td></tr>");
		}
		out.println("</table>");
		out.println("</div>");
	}
	
	private void collectInheritedMethods(ClassDoc c, List<MethodDoc> out, HashSet<MethodDoc> overrides) {
		if(overrides==null)
			overrides = new HashSet<>();
		for(MethodDoc m : c.methods()) {
			if(overrides.contains(m))
				continue;
			out.add(m); 
			if(m.overriddenClass()!=null)
				overrides.add(m.overriddenMethod());
			// FIXME also needs the list of implemented abstract/iface methods to work correctly
		}
		if(c.superclass()!=null)
			collectInheritedMethods(c.superclass(), out, overrides);
		for(ClassDoc imp : c.interfaces())
			collectInheritedMethods(imp, out, overrides);
	}
	
	private void collectInheritedFields(ClassDoc c, List<FieldDoc> out, HashSet<String> hideMask) {
		if(hideMask==null)
			hideMask = new HashSet<>();
		for(FieldDoc m : c.fields()) {
			if(hideMask.contains(m))
				continue;
			out.add(m);
			hideMask.add(m.name());
		}
		if(c.superclass()!=null)
			collectInheritedFields(c.superclass(), out, hideMask);
		for(ClassDoc imp : c.interfaces())
			collectInheritedFields(imp, out, hideMask);
	}
	
	public static boolean isDefaultConstructor(ExecutableMemberDoc met) {
		return met.isConstructor() && (met.position()==null || met.position().line()==met.containingClass().position().line());
	}
	
	private void printFieldList(String title, List<? extends FieldDoc> list, boolean enumConstants) {
		if(list.isEmpty())
			return;
		out.println("<div class=\"summary-item\">");
		out.printf("<h5>%s</h5>\n", title);
		out.println("<table>");
		
		int count = list.size();
		boolean startedUnknowns = false;
		int i=0;
		for(FieldDoc fld : list) {
			boolean inherited = fld.containingClass()!=cls;
			boolean unknown = inherited && !Doclet.listedClasses.contains(fld.containingClass());
			
			if(unknown) {
				if(!startedUnknowns) {
					out.printf("<tr class=\"inherited toggle\" onclick=\"toggleExt(this)\">"+
							"<td colspan=\"2\">Show all inherited fields (%d more)</td></tr>\n", count-i);
					out.print("<tr class=\"inherited ext sep hide\">");
					startedUnknowns = true;
				}
				else
					out.print("<tr class=\"inherited ext hide\">");
			}
			else
				out.print(inherited ?"<tr class=\"inherited\">" : "<tr>");

			if(!enumConstants) {
				out.println("<td class=\"mods\">");
				out.print("<code>");
				String mods = Modifier.toString(fld.modifierSpecifier() & ~Modifier.PUBLIC);
				if(!mods.isEmpty()) {
					out.print(mods);
					out.print(" ");
				}
				out.print(typeString(fld.type()));
				out.println("</code></td>");
			}
			
			out.print("<td>");
			if(unknown)
				out.printf("<code><span class=\"name\"><a class=\"extern\">%s</a></span>", fld.name());
			else if(inherited)
				out.printf("<code><span class=\"name\"><a href=\"%s.html#%s\">%s</a></span>",
						fld.containingClass().name(), memberAnchor(fld), fld.name());
			else
				out.printf("<code><span class=\"name\"><a href=\"#%s\">%s</a></span>",
						memberAnchor(fld), fld.name());
			out.print("</code>\n");

			if(inherited)
				out.printf("<br/>Inherited from <code>%s</code>.", typeString(fld.containingClass()));
			else {
				Tag[] info = fld.firstSentenceTags();
				if(info!=null && info.length>0) {
					out.print("<br/>");
					printCommentText(info);
				}
			}
			out.println("</td></tr>");
			
			i++;
		}
		out.println("</table>");
		out.println("</div>");
	}
	
	private void printMethodList(String title, List<? extends ExecutableMemberDoc> list) {
		if(list.isEmpty())
			return;
		out.println("<div class=\"summary-item\">");
		out.printf("<h5>%s</h5>\n", title);
		out.println("<table>");
		
		int count = list.size();
		String[] mods = new String[count];
		int i = 0;
		boolean hasMods = false;
		for(ExecutableMemberDoc met : list) {
			int m = met.modifierSpecifier() & ~Modifier.PUBLIC;
			if(!cls.isInterface() && met.containingClass().isInterface())
				m |= Modifier.ABSTRACT;
			mods[i] = Modifier.toString(m);
			if(met.isMethod() || !mods[i].isEmpty() || met.typeParameters().length>0)
				hasMods = true;
			i++;
		}
		
		boolean startedUnknowns = false;
		i = 0;
		for(ExecutableMemberDoc met : list) {
			boolean inherited = met.containingClass()!=cls;
			boolean unknown = inherited && !Doclet.listedClasses.contains(met.containingClass());
			boolean defaultConstructor = isDefaultConstructor(met);
			
			if(unknown) {
				if(!startedUnknowns) {
					out.printf("<tr class=\"inherited toggle\" onclick=\"toggleExt(this)\">"+
							"<td colspan=\"2\">Show all inherited methods (%d more)</td></tr>\n", count-i);
					out.print("<tr class=\"inherited ext sep hide\">");
					startedUnknowns = true;
				}
				else
					out.print("<tr class=\"inherited ext hide\">");
			}
			else
				out.print(inherited ?"<tr class=\"inherited\">" : "<tr>");

			if(hasMods) {
				out.println("<td class=\"mods\">");
				out.print("<code>");
				if(!mods[i].isEmpty()) {
					out.print(mods[i]);
					out.print(" ");
				}
				printTypeParams(met.typeParameters());
				if(met.isMethod())
					out.print(typeString(((MethodDoc) met).returnType()));
				out.println("</code></td>");
			}
			
			out.print("<td>");
			if(unknown || defaultConstructor)
				out.printf("<code><span class=\"name\"><a class=\"extern\">%s</a></span> (", met.name());
			else if(inherited)
				out.printf("<code><span class=\"name\"><a href=\"%s.html#%s\">%s</a></span> (",
						met.containingClass().name(), methodAnchor(met), met.name());
			else
				out.printf("<code><span class=\"name\"><a href=\"#%s\">%s</a></span> (",
						methodAnchor(met), met.name());
			printMethodSignature(met, false, unknown);
			out.print(")</code>\n");
			
			if(defaultConstructor)
				out.print("<br/>Default constructor.");
			else if(inherited)
				out.printf("<br/>Inherited from <code>%s</code>.", typeString(met.containingClass()));
			else {
				Tag[] info = met.firstSentenceTags();
				if(info!=null && info.length>0) {
					out.print("<br/>");
					printCommentText(info);
				}
			}
			out.println("</td></tr>");
			
			i++;
		}
		out.println("</table>");
		out.println("</div>");
	}
	
	private void printFieldDetails(FieldDoc fld) {
		String anchor = memberAnchor(fld);
		out.println("<div class=\"member\">");
		out.printf("<h3><a class=\"alink\" id=\"%s\" href=\"#%s\">%s</a></h3>\n", anchor, anchor, fld.name());
		
		out.printf("<pre>%s ", fld.modifiers());
		out.print(typeString(fld.type()));
		out.print(" ");
		out.printf("<span class=\"name\">%s</span>", fld.name());
		String val = fld.constantValueExpression();
		if(val!=null && !val.isEmpty()) {
			out.print(" = ");
			out.print(val);
		}
		out.println(";</pre>");
		
		printSince(fld);
		printCommentPar(fld.inlineTags());
		
		printSeeTags(fld);
		out.println("</div>");
	}
	
	private void printMethodDetails(ExecutableMemberDoc met) {
		String anchor = methodAnchor(met);
		out.println("<div class=\"member\">");
		out.printf("<h3><a class=\"alink\" id=\"%s\" href=\"#%s\">%s</a></h3>\n", anchor, anchor, met.name());
		
		out.printf("<pre>%s ", met.modifiers());
		printTypeParams(met.typeParameters());
		if(met.isMethod()) {
			out.print(typeString(((MethodDoc) met).returnType()));
			out.print(" ");
		}
		out.printf("<span class=\"name\">%s</span> (", met.name());
		printMethodSignature(met, true, false);
		out.println(");</pre>");
		
		if(met.isMethod()) {
			MethodDoc ovr = ((MethodDoc) met).overriddenMethod();
			if(ovr!=null)
				out.printf("<p><i>Overrides</i> <code>%s</code></p>", memberLink(ovr));
			// TODO add "implements method" link
		}
		
		printSince(met);
		printCommentPar(met.inlineTags());
		
		printTypeParamComments(met.typeParamTags());
		if(met.paramTags().length>0) {
			out.println("<h5>Parameters</h5>");
			out.println("<dl class=\"code\">");
			for(ParamTag t : met.paramTags()) {
				out.printf("<dt><code>%s</code></dt><dd>", t.parameterName());
				printCommentText(t.inlineTags());
				out.println("</dd>");
			}
			out.println("</dl>");
		}
		if(met.isMethod()) {
			for(Tag t : met.tags("@return")) {
				out.print("<h5>Returns</h5>\n<p class=\"ind\">");
				printCommentText(t.inlineTags());
			}
		}
		if(met.throwsTags().length>0) {
			out.println("<h5>Throws</h5>");
			out.println("<dl class=\"code\">");
			for(ThrowsTag t : met.throwsTags()) {
				out.printf("<dt><code>%s</code></dt><dd>",
						t.exceptionType()==null ? t.exceptionName() : typeString(t.exceptionType()));
				printCommentText(t.inlineTags());
				out.println("</dd>");
			}
			out.println("</dl>");
		}
		
		printSeeTags(met);
		out.println("</div>");
	}
	
	private void printTypeParamComments(ParamTag[] ptags) {
		if(ptags.length>0) {
			out.println("<h5>Type parameters</h5>");
			out.println("<dl class=\"code\">");
			for(ParamTag t : ptags) {
				out.printf("<dt><code>%s</code></dt><dd>", t.parameterName());
				printCommentText(t.inlineTags());
				out.println("</dd>");
			}
			out.println("</dl>");
		}
	}
	
	private void printMethodSignature(ExecutableMemberDoc met, boolean multiline, boolean skipNames) {
		Parameter[] pars = met.parameters();
		if(pars!=null && pars.length>0) {
			if(multiline)
				out.println();
			for(int i=0; i<pars.length; i++) {
				Parameter p = pars[i];
				if(multiline)
					out.print("\t");
				else if(i>0)
					out.print(", ");
				out.print(typeString(p.type(), met.isVarArgs() && i==pars.length-1));
				if(!skipNames) {
					out.print(" ");
					out.print(p.name());
				}
				if(multiline)
					out.print((i<pars.length-1) ? ",\n" : "\n");
			}
		}
	}
	
	@Override
	protected String getPackageName() {
		return cls.containingPackage().name();
	}
	
	@Override
	protected String getFilename() {
		return cls.name();
	}
}
