package soot.jimple.spark.summary;

import java.util.HashMap;
import java.util.Map;

public class ClassObjects {
	private String className;
	private Map<Integer,GapDefinition> gaps;
	private Map<String,MethodObjects> methodObjectsMap;
	private int lastGapId=0;
	public ClassObjects(String className){
		this.className=className;
		gaps=new HashMap<Integer,GapDefinition>();
		methodObjectsMap=new HashMap<String,MethodObjects>();
	}
	public void merge(MethodObjects methodObjects){
		String methodSig=methodObjects.getMethodSig();
		methodObjectsMap.put(methodSig,methodObjects);
		gaps.putAll(methodObjects.getGaps());
	}
	public String getClassName(){
		return this.className;
	}
	public boolean analyzed(String methodSig){
		return methodObjectsMap.containsKey(methodSig);
	}
	public GapDefinition getGap(int i){
		return gaps.get(i);
	}
	public void addGap(GapDefinition gap){
		int id=gap.getID();
		if((id+1)>lastGapId){
			lastGapId=id+1;
		}
		if(gaps.containsKey(id)){
			throw new RuntimeException("Gap conflicts");
		}
		gaps.put(id, gap);
	}
	public Map<String,MethodObjects> getMethodObjectsMap(){
		return methodObjectsMap;
	}
	public MethodObjects getMethodObjects(String methodSig){
		return methodObjectsMap.get(methodSig);
	}
	public boolean isEmpty(){
		return gaps.isEmpty()&&methodObjectsMap.isEmpty();
	}
	public void setLastGapId(int lastGapId){
		this.lastGapId=lastGapId;
	}
	public int getLastGapId(){
		return this.lastGapId;
	}
	public void loadMethodGaps(){
		for(String methodSig:methodObjectsMap.keySet()){
			MethodObjects methodObjects=methodObjectsMap.get(methodSig);
			for(BaseObject baseObject:methodObjects.getBaseObjects()){
				if(baseObject.gapId!=-1){
					if(gaps.containsKey(baseObject.gapId)){
						methodObjects.addGap(gaps.get(baseObject.gapId));
					}else{
						throw new RuntimeException("Error gap dependency");
					}
				}
			}
		}
	}
	
}
