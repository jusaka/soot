package soot.jimple.spark.summary;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import soot.ArrayType;
import soot.BooleanType;
import soot.ByteType;
import soot.CharType;
import soot.DoubleType;
import soot.FloatType;
import soot.IntType;
import soot.LongType;
import soot.RefType;
import soot.Scene;
import soot.ShortType;
import soot.SootClass;
import soot.SootField;
import soot.Type;

public class ObjectUtils {
	public static Type getTypeFromString(String type) {
		// Reduce arrays
		int numDimensions = 0;
		while (type.endsWith("[]")) {
			numDimensions++;
			type = type.substring(0, type.length() - 2);
		}
		
		// Generate the target type
		final Type t;
		if (type.equals("int"))
			t = IntType.v();
		else if (type.equals("long"))
			t = LongType.v();
		else if (type.equals("float"))
			t = FloatType.v();
		else if (type.equals("double"))
			t = DoubleType.v();
		else if (type.equals("boolean"))
			t = BooleanType.v();
		else if (type.equals("char"))
			t = CharType.v();
		else if (type.equals("short"))
			t = ShortType.v();
		else if (type.equals("byte"))
			t = ByteType.v();
		else
			t = RefType.v(type);
		
		if (numDimensions == 0)
			return t;
		return ArrayType.v(t, numDimensions);
	}
	public static SootField safeGetField(String fieldSig) {
		if (fieldSig == null || fieldSig.equals(""))
			return null;
		
		SootField sf = Scene.v().grabField(fieldSig);
		if (sf != null)
			return sf;
		
		// This field does not exist, so we need to create it
		String className = fieldSig.substring(1);
		className = className.substring(0, className.indexOf(":"));
		SootClass sc = Scene.v().getSootClassUnsafe(className);
		if (sc.resolvingLevel() < SootClass.SIGNATURES
				&& !sc.isPhantom()) {
			System.err.println("WARNING: Class not loaded: " + sc);
			return null;
		}
		
		String type = fieldSig.substring(fieldSig.indexOf(": ") + 2);
		type = type.substring(0, type.indexOf(" "));
		
		String fieldName = fieldSig.substring(fieldSig.lastIndexOf(" ") + 1);
		fieldName = fieldName.substring(0, fieldName.length() - 1);
		
		return Scene.v().makeFieldRef(sc, fieldName,
				getTypeFromString(type), false).resolve();
	}
	
	public static List<String> splitAccessPath(String accessPath){
		Pattern pattern = Pattern.compile("(?<=\\[)(.+?)(?=\\])");
		Matcher match = pattern.matcher(accessPath);
		List<String> splitedAP = new ArrayList<String>();
		while (match.find()) {
			splitedAP.add(match.group());
		}
		return splitedAP;
	}
	public static String getTargetSig(String type,String signature){
		String subSig=Scene.v().signatureToSubsignature(signature);
		return "<"+type+": "+subSig+">";
	}
}
