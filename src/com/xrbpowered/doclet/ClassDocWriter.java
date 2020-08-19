package com.xrbpowered.doclet;

import static com.xrbpowered.doclet.WriterUtils.*;

import java.io.PrintStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.AnnotationValue;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.ConstructorDoc;
import com.sun.javadoc.Doc;
import com.sun.javadoc.ExecutableMemberDoc;
import com.sun.javadoc.FieldDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.ParamTag;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.ProgramElementDoc;
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
			String.format("<a href=\"%s.html\">%s</a>", PackageDocWriter.filename, getPackageName())
		);
		
		printClassSignature();
		printTypeParamComments(cls.typeParamTags());
		printInfoCard();
		
		// class comment
		printSince(cls);
		printDeprecatedInfo(cls);
		printCommentPar(cls.inlineTags());
		printSeeTags(cls);

		// summary
		boolean sum = false;
		out.println("<div class=\"summary\">");
		out.println("<h2>Summary</h2>");
		sum |= printInnerClasses();
		
		// do not sort enum constants!
		sum |= printFieldList("Enum constants", Arrays.asList(cls.enumConstants()), true);
		
		ArrayList<FieldDoc> allFields = new ArrayList<>();
		collectInheritedFields(cls, allFields, null);
		allFields.sort(memberSort);
		sum |= printSummaryFields("Constants", allFields, Modifier.STATIC | Modifier.FINAL, 0);
		sum |= printSummaryFields("Static Fields", allFields, Modifier.STATIC, Modifier.FINAL);
		sum |= printSummaryFields("Instance Fields", allFields, 0, Modifier.STATIC);
		
		Arrays.sort(cls.constructors(), methodSort);
		sum |= printSummaryMethods("Constructors", Arrays.asList(cls.constructors()), 0, 0, null);

		ArrayList<MethodDoc> allMethods = new ArrayList<>();
		HashMap<MethodDoc, MethodDoc> overrides = new HashMap<>();
		collectInheritedMethods(cls, allMethods, overrides);
		allMethods.sort(methodSort);
		sum |= printSummaryMethods("Abstract Methods", allMethods, Modifier.ABSTRACT, Modifier.STATIC, overrides);
		sum |= printSummaryMethods(cls.isInterface() ? "Interface Methods" : "Instance Methods", allMethods, 0, Modifier.ABSTRACT | Modifier.STATIC, overrides);
		sum |= printSummaryMethods("Static Methods", allMethods, Modifier.STATIC, 0, overrides);
		
		if(!sum) {
			out.println("<p class=\"overrides\">Nothing to show.</p>");
		}
		
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
				printMethodDetails(con, null);
		}
		for(MethodDoc met : cls.methods()) {
			printMethodDetails(met, overrides.get(met));
		}
		out.println("</div>");
		printPageEnd();
	}
	
	private void printClassSignature() {
		out.print("<pre>");
		printAnnotations(cls);
		if(cls.isAnnotationType())
			out.print(cls.modifiers().replace("interface", "@interface"));
		else
			out.print(cls.modifiers());
		if(!cls.isInterface() && !cls.isAnnotationType())
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
		printKnownImplementing(true);
		printKnownImplementing(false);
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
		if(c==cls) {
			out.print(cls.name());
			printTypeParams(cls.typeParameters());
		}
		else
			out.print(typeString(t));
	}

	private void printClassHierarchy() {
		if(cls.isInterface())
			return;
		out.println("<dt>Hierarchy:</dt><dd>");
		printHierarchy(cls);
		out.println("</dd>");
	}
	
	private void collectSuperInterfaces(ClassDoc c, List<ClassDoc> out, HashSet<String> uniques) {
		if(uniques==null)
			uniques = new HashSet<>();
		if(c.superclass()!=null) {
			ClassDoc sup = c.superclass();
			if(sup.isInterface() && !uniques.contains(sup)) {
				uniques.add(sup.qualifiedName());
				out.add(sup);
			}
			collectSuperInterfaces(sup, out, uniques);
		}
		for(ClassDoc sup : c.interfaces()) {
			if(sup.isInterface() && !uniques.contains(sup)) {
				uniques.add(sup.qualifiedName());
				out.add(sup);
			}
			collectSuperInterfaces(sup, out, uniques);
		}
	}
	
	private void printSuperinterfaces() {
		List<ClassDoc> list = new ArrayList<>();
		collectSuperInterfaces(cls, list, null);
		printPlainClassList("All "+(cls.isInterface() ? "superinterfaces" : "implemented interfaces"), list);
	}
	
	private void printKnownSubclasses() {
		if(cls.isInterface())
			return;
		List<ClassDoc> list = new ArrayList<>();
		for(ClassDoc c : Doclet.listedClasses) {
			if(c.superclass()==this.cls)
				list.add(c);
		}
		printPlainClassList("Known direct subclasses", list);
	}

	private void printKnownImplementing(boolean interfaces) {
		if(!cls.isInterface())
			return;
		List<ClassDoc> list = new ArrayList<>();
		for(ClassDoc c : Doclet.listedClasses) {
			if(c.isInterface()==interfaces) {
				for(ClassDoc i : c.interfaces()) {
					if(i==this.cls) {
						list.add(c);
						break;
					}
				}
			}
		}
		printPlainClassList("Known direct "+(interfaces ? "subinterfaces" : "implementing classes"), list);
	}

	private void printPlainClassList(String dt, List<ClassDoc> list) {
		if(!list.isEmpty()) {
			list.sort(classSort);
			out.print("<dt>");
			out.print(dt);
			out.print(":</dt><dd>");
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

	private boolean printSummaryFields(String title, List<? extends FieldDoc> list, int mods, int noMods) {
		ArrayList<FieldDoc> fields = new ArrayList<>();
		for(FieldDoc met : list) {
			int m = met.modifierSpecifier();
			if((m&mods)==mods && (m&noMods)==0)
				fields.add(met);
		}
		return printFieldList(title, fields, false);
	}

	private boolean printSummaryMethods(String title, List<? extends ExecutableMemberDoc> list,
			int mods, int noMods, HashMap<MethodDoc, MethodDoc> overrides) {
		ArrayList<ExecutableMemberDoc> mets = new ArrayList<>();
		for(ExecutableMemberDoc met : list) {
			int m = met.modifierSpecifier();
			if(!cls.isInterface() && met.containingClass().isInterface())
				m |= Modifier.ABSTRACT;
			if((m&mods)==mods && (m&noMods)==0)
				mets.add(met);
		}
		return printMethodList(title, mets, overrides);
	}

	private boolean printInnerClasses() {
		if(cls.innerClasses().length==0)
			return false;
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
		return true;
	}
	
	private void collectInheritedMethods(ClassDoc c, List<MethodDoc> out, HashMap<MethodDoc, MethodDoc> overrides) {
		for(MethodDoc cm : c.methods()) {
			boolean overriden = false;
			for(MethodDoc m : out) {
				if(m.overrides(cm)) {
					if(!overrides.containsKey(m))
						overrides.put(m, cm);
					overriden = true;
					break;
				}
			}
			if(!overriden)
				out.add(cm);
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
	
	private boolean  printFieldList(String title, List<? extends FieldDoc> list, boolean enumConstants) {
		if(list.isEmpty())
			return false;
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

			if(isDeprecated(fld))
				out.print("<br/><span class=\"depr\">Deprecated</span>");
			else if(inherited)
				out.printf("<br/>Inherited from <code>%s</code>.", typeString(fld.containingClass()));
			else {
				Tag[] info = fld.firstSentenceTags();
				if(info.length>0) {
					out.print("<br/>");
					printCommentText(info);
				}
			}
			out.println("</td></tr>");
			
			i++;
		}
		out.println("</table>");
		out.println("</div>");
		return true;
	}
	
	private boolean printMethodList(String title, List<? extends ExecutableMemberDoc> list, HashMap<MethodDoc, MethodDoc> overrides) {
		if(list.isEmpty())
			return false;
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
			
			if(isDeprecated(met))
				out.print("<br/><span class=\"depr\">Deprecated</span>");
			else if(defaultConstructor)
				out.print("<br/>Default constructor.");
			else if(inherited)
				out.printf("<br/>Inherited from <code>%s</code>.", typeString(met.containingClass()));
			else {
				Tag[] info = met.firstSentenceTags();
				if(info.length>0) {
					out.print("<br/>");
					printCommentText(info);
				}
				else {
					ExecutableMemberDoc copy = getReplacementDoc(met, overrides.get(met));
					info = copy.firstSentenceTags();
					if(info.length>0) {
						out.print("<br/>");
						printCommentText(info);
					}
				}
			}
			out.println("</td></tr>");
			
			i++;
		}
		out.println("</table>");
		out.println("</div>");
		return true;
	}
	
	private void printFieldDetails(FieldDoc fld) {
		String anchor = memberAnchor(fld);
		out.println("<div class=\"member\">");
		out.printf("<h3><a class=\"alink\" id=\"%s\" href=\"#%s\">%s</a></h3>\n", anchor, anchor, fld.name());
		
		out.print("<pre>");
		printAnnotations(fld);
		out.print(fld.modifiers());
		out.print(" ");
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
		printDeprecatedInfo(fld);
		printCommentPar(fld.inlineTags());
		
		printSeeTags(fld);
		out.println("</div>");
	}
	
	private void printMethodDetails(ExecutableMemberDoc met, MethodDoc overriden) {
		String anchor = methodAnchor(met);
		out.println("<div class=\"member\">");
		out.printf("<h3><a class=\"alink\" id=\"%s\" href=\"#%s\">%s</a></h3>\n", anchor, anchor, met.name());
		
		out.print("<pre>");
		printAnnotations(met);
		out.print(met.modifiers());
		out.print(" ");
		printTypeParams(met.typeParameters());
		if(met.isMethod()) {
			out.print(typeString(((MethodDoc) met).returnType()));
			out.print(" ");
		}
		out.printf("<span class=\"name\">%s</span> (", met.name());
		printMethodSignature(met, true, false);
		out.println(");</pre>");
		
		printSince(met);
		
		if(overriden!=null) {
			out.printf("<p class=\"overrides\">%s <code>%s</code>.",
					overriden.isAbstract() || overriden.containingClass().isInterface() ? "Implements" : "Overrides",
					memberLink(overriden));
			
			if(met.inlineTags().length==0 && met.tags().length==0) {
				MethodDoc copy = overriden;
				if(copy.inlineTags().length>0 || copy.tags().length>0) {
					out.print(" Copied description:");
					met = copy;
				}
			}
			out.println("</p>");
		}
		else if(met.inlineTags().length==0 && met.tags().length==0) {
			met = getReplacementDoc(met, null);
		}

		printDeprecatedInfo(met);
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

	protected String annotationValueString(AnnotationValue v) {
		Object obj = v.value();
		if(obj instanceof Type)
			return typeString((Type) obj);
		else if(obj instanceof FieldDoc)
			return memberLink((FieldDoc) obj);
		else if(obj instanceof AnnotationDesc)
			return typeString(((AnnotationDesc) obj).annotationType());
		else if(obj instanceof AnnotationValue[]) {
			StringBuilder sb = new StringBuilder();
			sb.append("{");
			AnnotationValue[] vals = (AnnotationValue[]) obj;
			boolean first = true;
			for(AnnotationValue val : vals) {
				if(!first) sb.append(", ");
				sb.append(annotationValueString(val));
				first = false;
			}
			sb.append("}");
			return sb.toString();
		}
		else
			return obj.toString();
	}

	protected void printAnnotations(ProgramElementDoc doc) {
		for(AnnotationDesc ann : doc.annotations()) {
			out.print(typeString(ann.annotationType()));
			if(ann.elementValues().length>0) {
				out.print("(");
				boolean first = true;
				for(AnnotationDesc.ElementValuePair ev : ann.elementValues()) {
					if(!first) out.print(", ");
					out.printf("%s=%s", ev.element().name(), annotationValueString(ev.value()));
					first = false;
				}
				out.print(")");
			}
			out.println();
		}
	}
	
	protected void printDeprecatedInfo(ProgramElementDoc doc) {
		if(isDeprecated(doc)) {
			out.println("<div class=\"depr\"><p><span class=\"depr\">Deprecated.</span>");
			for(Tag t : doc.tags("@deprecated")) {
				printCommentPar(t.inlineTags());
			}
			out.println("</div>");
		}
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
