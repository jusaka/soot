package soot.jimple.spark.summary;

import soot.Type;

public class BaseObject {
	BaseObjectType baseObjectType;
	String type;
	int id;
	int index=-1;
	int gapId=-1;
	String methodSig;
	public BaseObject(String methodSig,int id,String type,BaseObjectType baseObjectType){
		this.methodSig=methodSig;
		this.baseObjectType=baseObjectType;
		this.type=type;
		this.id=id;
	}
	public int getID(){
		return this.id;
	}
	public int getIndex(){
		return this.index;
	}
	public int getGapId(){
		return this.gapId;
	}
	public String getTypeString(){
		return this.type;
	}
	public Type getType(){
		return ObjectUtils.getTypeFromString(type);
	}
	public BaseObjectType getBaseObjectType(){
		return this.baseObjectType;
	}
	public void setIndex(int index){
		this.index=index;
	}
	public void setGapId(int gapId){
		this.gapId=gapId;
	}
	public String getMethodSig(){
		return methodSig;
	}
	
	@Override
	public boolean equals(Object other){
		if(other==null||this==null) return false;
		if(other==this) return true;
		BaseObject otherObject=(BaseObject)other;
		if(!this.methodSig.equals(otherObject.methodSig)){
			return false;
		}
		if(this.id!=otherObject.id){
			return false;
		}
		if(!this.baseObjectType.equals(otherObject.baseObjectType)){
			return false;
		}
		if(!this.type.equals(otherObject.type)){
			return false;
		}
		if(gapId!=otherObject.gapId) return false;
		if(otherObject.index!=index){
			return false;
		}
		
		return true;
	}
	
	@Override
	public int hashCode(){
		int hashCode=((Integer)id).hashCode();
		hashCode=hashCode*31+methodSig.hashCode();
		hashCode=hashCode*31+baseObjectType.hashCode();
		hashCode=hashCode*31+type.hashCode();
		if(index!=-1){
			hashCode=hashCode*31+((Integer)index).hashCode();
		}
		if(gapId!=-1){
			hashCode=hashCode*31+((Integer)gapId).hashCode();
		}
		return hashCode;
	}

	public String toString(){
		String str="BaseObject "+id+" "+baseObjectType+" "+type;
		if(gapId!=-1){
			str+=" "+gapId;
		}
		if(index!=-1){
			str+=" "+index; 
		}
		return str+" in "+methodSig;
	}
	public boolean isGap(){
		return BaseObjectType.isGap(this.baseObjectType);
	}
}
