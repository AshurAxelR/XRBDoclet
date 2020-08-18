package com.xrbpowered.doclet;

import java.util.Comparator;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.ExecutableMemberDoc;
import com.sun.javadoc.MemberDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.PackageDoc;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.Type;

public class WriterUtils {

	public static final Comparator<PackageDoc> packageSort = new Comparator<PackageDoc>() {
		@Override
		public int compare(PackageDoc o1, PackageDoc o2) {
			return o1.name().compareTo(o2.name());
		}
	};

	public static final Comparator<ClassDoc> classSort = new Comparator<ClassDoc>() {
		@Override
		public int compare(ClassDoc o1, ClassDoc o2) {
			int res = o1.name().compareTo(o2.name());
			if(res==0)
				res = packageSort.compare(o1.containingPackage(), o2.containingPackage());
			return res;
		}
	};

	public static final Comparator<ClassDoc> classSimpleNameSort = new Comparator<ClassDoc>() {
		@Override
		public int compare(ClassDoc o1, ClassDoc o2) {
			int res = o1.simpleTypeName().compareTo(o2.simpleTypeName());
			if(res==0) {
				ClassDoc enc1 = o1.containingClass();
				ClassDoc enc2 = o1.containingClass();
				if(enc1!=null || enc2!=null) {
					res = Boolean.compare(enc1==null, enc2==null);
					if(res==0)
						res = classSort.compare(enc1, enc2);
				}
				if(res==0)
					res = packageSort.compare(o1.containingPackage(), o2.containingPackage());
			}
			return res;
		}
	};

	public static final Comparator<MemberDoc> memberSort = new Comparator<MemberDoc>() {
		@Override
		public int compare(MemberDoc o1, MemberDoc o2) {
			int res = -Boolean.compare(Doclet.listedClasses.contains(o1.containingClass()), Doclet.listedClasses.contains(o2.containingClass()));
			if(res==0)
				res = o1.name().compareTo(o2.name());
			return res;
		}
	};
	
	public static final Comparator<ExecutableMemberDoc> methodSort = new Comparator<ExecutableMemberDoc>() {
		@Override
		public int compare(ExecutableMemberDoc o1, ExecutableMemberDoc o2) {
			int res = memberSort.compare(o1, o2);
			if(res==0)
				res = o1.signature().compareTo(o2.signature());
			return res;
		}
	};
	
	public static String methodAnchor(ExecutableMemberDoc met) {
		StringBuilder sb = new StringBuilder();
		sb.append(met.name());
		Parameter[] params = met.parameters();
		for(int i=0; i<params.length; i++) {
			sb.append("-");
			Type type = params[i].type();
			if(type.isPrimitive())
				sb.append(type.typeName());
			else {
				ClassDoc cls = type.asClassDoc();
				if(cls==null)
					sb.append(type.simpleTypeName());
				else
					sb.append(cls.qualifiedName());
			}
			if(met.isVarArgs() && i==params.length-1 && !type.dimension().isEmpty())
				sb.append("...");
			else
				sb.append(type.dimension().replaceAll("\\[\\]", ":A"));
		}
		sb.append("-");
		return sb.toString();
	}

	public static String memberAnchor(MemberDoc mem) {
		if(mem instanceof ExecutableMemberDoc)
			return methodAnchor((ExecutableMemberDoc) mem);
		else
			return mem.name();
	}

	public static ExecutableMemberDoc getReplacementDoc(ExecutableMemberDoc met, MethodDoc overriden) {
		if(overriden!=null)
			return overriden;
		else {
			// TODO special case: static Enum.values and Enum.valueOf
			if(met.isMethod() && met.containingClass().isEnum()) {
				if(met.isStatic() && met.name().equals("values") && met.signature().equals("()")) {
				}
				else if(met.isStatic() && met.name().equals("valueOf") && met.signature().equals("(java.lang.String)")) {
				}
			}
		}
		return met; // no replacement, return original
	}
	
}
