package soot.jimple.spark.summary;

public class FieldObject{
	String methodSig;
	BaseObject baseObject;
	String baseType;
	String accessPath;
	String type;
	boolean isNew=false;
	boolean hasField=false;
	public FieldObject(String methodSig,BaseObject baseObject,String type){
		this.methodSig=methodSig;
		this.baseObject=baseObject;
		this.type=type;
	}
	public FieldObject(String methodSig,BaseObject baseObject,String accessPath,String type){
		this(methodSig,baseObject,type);
		if(accessPath!=null&&!accessPath.isEmpty()){
			this.accessPath=accessPath;
			hasField=true;
		}
	}
	
	public FieldObject(String methodSig,String baseType,String type){
		this.methodSig=methodSig;
		this.baseType=baseType;
		this.type=type;
		isNew=true;
	}
	@Override
	public boolean equals(Object other){
		if(other==null||this==null) return false;
		if(other==this) return true;
		FieldObject otherObject=(FieldObject)other;
		
		if(!this.validate()||!otherObject.validate()) return false;
		if(!this.methodSig.equals(otherObject.methodSig)){
			return false;
		}
		if(!this.type.equals(otherObject.type)){
			return false;
		}
		if(isNew){
			if(!this.baseType.equals(otherObject.baseType)){
				return false;
			}
		}else{
			if(!this.baseObject.equals(otherObject.baseObject)){
				return false;
			}
			if(hasField){
				if(!this.accessPath.equals(otherObject.accessPath)){
					return false;
				}
			}
		}
		return true;
	}
	private boolean validate(){
		if(isNew){
			if(!hasField&&baseObject==null&&baseType!=null&&(accessPath==null||accessPath.isEmpty())){
				return true;
			}
			return false;
		}else{
			if(baseObject==null||baseType!=null){
				return false;
			}
			if((hasField&&!(accessPath==null||accessPath.isEmpty()))||!hasField&&(accessPath==null||accessPath.isEmpty())){
					return true;
			}
			return false;
		}
	}
	
	@Override
	public int hashCode(){
		int hashCode=type.hashCode();
		hashCode=hashCode*31+methodSig.hashCode();
		if(isNew){
			hashCode=hashCode*31+baseType.hashCode();
		}else{
			hashCode=hashCode*31+baseObject.hashCode();
			if(hasField){
				hashCode=hashCode*31+accessPath.hashCode();
			}
		}
		return hashCode;
	}
	
	
	public String toString(){
		String str="FieldObject:";
		if(isNew){
			str+=baseType;
		}else{
			str+=baseObject.id;
		}
		if(accessPath!=null){
			str+=accessPath;
		}
		str+=" "+type;
		return str+" in "+methodSig;
	}
	public String getFieldString(){
		String str="";
		if(isNew){
			str+="new "+baseType;
		}else{
			str+=baseObject.id;
		}
		if(accessPath!=null){
			str+=accessPath;
		}
		return str;
	}
	public String getFieldType(){
		return this.type;
	}
	public boolean isGapBase(){
		return !isNew&&!hasField&&baseObject.baseObjectType==BaseObjectType.GapBaseObject;
	}
	public boolean isNew(){
		return isNew;
	}
	public boolean hasField(){
		return hasField;
	}
	public String getBaseType(){
		return baseType;
	}
	public String getAccessPath(){
		return accessPath;
	}
	public BaseObject getBaseObject(){
		return baseObject;
	}
	public String getMethodSig(){
		return methodSig;
	}
}
