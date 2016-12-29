package soot.jimple.spark.summary;

public class BaseObject {
	BaseObjectType baseObjectType;
	String type;
	int id;
	public BaseObject(int id,String type,BaseObjectType baseObjectType){
		this.baseObjectType=baseObjectType;
		this.type=type;
		this.id=id;
	}
	public String getType(){
		return this.type;
	}
	public void setType(String type){
		this.type=type;
	}
	public BaseObjectType getBaseObjectType(){
		return this.baseObjectType;
	}
	public void setBaseObejctType(BaseObjectType baseObjectType){
		this.baseObjectType=baseObjectType;
	}
	
	@Override
	public boolean equals(Object other){
		if(other==null||this==null) return false;
		if(other==this) return true;
		BaseObject otherObject=(BaseObject)other;
		if(this.baseObjectType.equals(otherObject.baseObjectType)&&
				this.type.equals(otherObject.type)&&this.id==otherObject.id){
			return true;
		}
		return false;
	}
	
	@Override
	public int hashCode(){
		int hashCode=id;
		hashCode=hashCode*31+baseObjectType.hashCode();
		hashCode=hashCode*31+type.hashCode();
		return hashCode;
	}
	public boolean isGap(){
		return baseObjectType==BaseObjectType.GapBaseObject||
				baseObjectType==BaseObjectType.GapParameter
				||baseObjectType==BaseObjectType.Return;
	}
}
