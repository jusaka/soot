package soot.jimple.spark.summary;

public enum BaseObjectType {
	This,
	Parameter,
	GapBaseObject,
	GapParameter,
	GapReturn,
	Return;
	public static BaseObjectType getTypeByValue(String value){
		switch(value){
		case "This":
			return BaseObjectType.This;
		case "Parameter":
			return BaseObjectType.Parameter;
		case "GapBaseObject":
			return BaseObjectType.GapBaseObject;
		case "GapParameter":
			return BaseObjectType.GapParameter;
		case "GapReturn":
			return BaseObjectType.GapReturn;
		default:
			return BaseObjectType.Return;
		}
	}
	public static boolean isGap(BaseObjectType baseObjectType){
		return baseObjectType==BaseObjectType.GapBaseObject||
				baseObjectType==BaseObjectType.GapParameter
				||baseObjectType==BaseObjectType.GapReturn;
	}
	public static boolean isParameter(BaseObjectType baseObjectType){
		return baseObjectType==BaseObjectType.GapParameter
				||baseObjectType==BaseObjectType.Parameter;
	}
}
