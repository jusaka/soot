package soot.jimple.spark.summary;

public class FieldObject{
	BaseObject baseObject;
	String baseType;
	String accessPath;
	String type;
	boolean isNew=false;
	boolean hasField=false;
	public FieldObject(BaseObject baseObject,String type){
		this.baseObject=baseObject;
		this.type=type;
	}
	public FieldObject(BaseObject baseObject,String accessPath,String type){
		this(baseObject,type);
		if(accessPath!=null&&!accessPath.isEmpty()){
			this.accessPath=accessPath;
			hasField=true;
		}
	}
	
	public FieldObject(String baseType,String type){
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
		return str;
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
}
