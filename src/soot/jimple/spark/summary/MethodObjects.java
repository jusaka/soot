package soot.jimple.spark.summary;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import soot.RefType;
import soot.SootMethod;
import soot.Type;
import soot.jimple.spark.pag.AllocDotField;
import soot.jimple.spark.pag.AllocNode;
import soot.jimple.spark.pag.FakeBaseNode;
import soot.jimple.spark.pag.FakeNode;
import soot.jimple.spark.pag.Node;
import soot.jimple.spark.pag.PAG;
import soot.jimple.spark.pag.SparkField;
import soot.jimple.spark.sets.P2SetVisitor;
import soot.jimple.spark.sets.PointsToSetInternal;
import soot.toolkits.scalar.Pair;

public class MethodObjects {
	private SootMethod method;
	private Map<FieldObject,Set<FieldObject>> summaries;
	private Map<Integer,BaseObject> baseObjects;
	private Map<Integer,GapDefinition> gaps;
	private String methodSig;
	private int lastBaseObjectId;
	private int lastGapId;
	public MethodObjects(){
		summaries=new HashMap<FieldObject,Set<FieldObject>>();
		baseObjects=new HashMap<Integer,BaseObject>();
		gaps=new HashMap<Integer,GapDefinition>();
		this.lastBaseObjectId=0;
	}
	public MethodObjects(String methodSig){
		this();
		this.methodSig=methodSig;
	}
	public MethodObjects(String methodSig,int lastGapId){
		this(methodSig);
		this.lastGapId=lastGapId;
	}
	public GapDefinition createGap(String signature) {
		int gapID=lastGapId++;
		GapDefinition gd = new GapDefinition(gapID, signature);
		this.gaps.put(gapID,gd);
		return gd;
	}
	public BaseObject createBaseObject(FakeNode fakeNode) {
		int baseObjectID=lastBaseObjectId++;
		Type type=fakeNode.getType();
		Pair pair=(Pair)fakeNode.getNewExpr();
		BaseObjectType baseObjectType=(BaseObjectType)pair.getO1();
		BaseObject baseObject=new BaseObject(baseObjectID,type.toString(),baseObjectType);
		if(baseObjectType==BaseObjectType.Parameter){
			baseObject.setIndex((Integer)pair.getO2());
		}else if(baseObjectType==BaseObjectType.GapReturn||
				baseObjectType==BaseObjectType.GapBaseObject){
			baseObject.setGapId(((GapDefinition)pair.getO2()).getID());
		}else if(baseObjectType==BaseObjectType.GapParameter){
			Pair o2=(Pair)pair.getO2();
			baseObject.setGapId(((GapDefinition)o2.getO1()).getID());
			baseObject.setIndex((Integer)o2.getO2());
		}
		baseObjects.put(baseObjectID, baseObject);
		return baseObject;
	}
	public boolean addSummary(FakeNode node,PointsToSetInternal p2set,PAG pag){
		boolean[] isAdded=new boolean[1];
		p2set.forall( new P2SetVisitor() {
        public final void visit( Node n ) { 
        	FieldObject src=getFieldObject(n,pag);
        	FieldObject dest=getFieldObject(node,pag);
        	if(src.equals(dest)&&src.baseObject!=null){
        		if(BaseObjectType.isGap(src.baseObject.baseObjectType)) return;
        		else if(!src.hasField) return;
        	}
        	Set<FieldObject> srcSet=summaries.get(dest);
        	if(srcSet==null){
        		srcSet=new HashSet<FieldObject>();
        		summaries.put(dest, srcSet);
        	}
        	if(!srcSet.contains(src)){
        		srcSet.add(src);
        		isAdded[0]=true;
        	}
        	if(handleFields((AllocNode)n,node,pag)){
        		isAdded[0]=true;
        	}
        }} );
		return isAdded[0];
	}
	private boolean handleFields(AllocNode src,FakeNode dest,PAG pag){
		boolean isAdded=false;
		for(AllocDotField allocDotField:src.getAllFieldRefs()){
			SparkField field=allocDotField.getField();
			if(field.getType() instanceof RefType&&
					addSummary(dest.getFakeBaseNode(field),allocDotField.getP2Set(),pag)){
				isAdded=true;
			}
		}
		return isAdded;
	}
	
	public boolean handleFields(FakeNode node,PAG pag){
		return handleFields(node,node,pag);
	}
	private FieldObject getFieldObject(Node n,PAG pag){
		FieldObject object;
		if(n instanceof FakeNode){
			FakeNode cur=(FakeNode)n;
			Stack<SparkField> stack=new Stack<SparkField>();
			while(cur instanceof FakeBaseNode){
				FakeBaseNode node=(FakeBaseNode) cur;
				stack.push(node.getField());
				cur=node.getBase();
			}
			BaseObject baseObject=pag.baseObjects.get(cur);
			StringBuilder sb=new StringBuilder();
			while(!stack.isEmpty()){
				sb.append("."+stack.pop().toString());
			}
			if(baseObject!=null){
				String accessPath=sb.length()==0?null:sb.toString();
				object=new FieldObject(baseObject,accessPath,n.getType().toString());
			}else{
				throw new RuntimeException("Something maybe wrong");
			}
		}else{
    		AllocNode node=(AllocNode) n;
    		object=new FieldObject(node.getType().toString(),n.getType().toString());
    	}
		return object;
	}
	public void compact(){
		Set<FieldObject> needToRemove=new HashSet<FieldObject>();
		for(FieldObject dest:summaries.keySet()){
			if(summaries.get(dest).size()==0){
				needToRemove.add(dest);
			}else if(summaries.get(dest).size()==1){
				for(FieldObject src:summaries.get(dest)){
					if(src.equals(dest)){
						needToRemove.add(dest);
					}
				}
			}
			
		}
		for(FieldObject dest:needToRemove){
			summaries.remove(dest);
		}
//		Set<Pair<FieldObject,FieldObject>> removeSet=new HashSet<Pair<FieldObject,FieldObject>>();
//		for(FieldObject dest:summaries.keySet()){
//			for(FieldObject src:summaries.get(dest)){
//				for(FieldObject temp:summaries.get(src)){
//					if(summaries.get(dest).contains(temp)){
//						Pair<FieldObject,FieldObject> pair=new Pair<FieldObject,FieldObject>(temp,dest);
//						removeSet.add(pair);
//					}
//				}
//			}
//		}
//		for(Pair<FieldObject,FieldObject> pair:removeSet){
//			if(summaries.get(pair.getO2())!=null){
//				summaries.get(pair.getO2()).remove(pair.getO1());
//				if(summaries.get(pair.getO2()).isEmpty()){
//					summaries.remove(pair.getO2());
//				}
//			}
//		}
	}
	public void setMethod(SootMethod method){
		if(this.method==null){
			this.method=method;
		}
	}
	public SootMethod getMethod(){
		return method;
	}
	public String getMethodSig(){
		return methodSig;
	}
	public int getLastGapId(){
		return this.lastGapId;
	}
	public int getLastBaseObjectId(){
		return this.lastBaseObjectId;
	}
	public Map<Integer,GapDefinition> getGaps(){
		return this.gaps;
	}
	public Map<FieldObject,Set<FieldObject>> getSummaries(){
		return this.summaries;
	}
	public BaseObject getBaseObject(int i){
		return baseObjects.get(i);
	}
	public Collection<BaseObject> getBaseObjects(){
		return baseObjects.values();
	}
	public void addBaseObject(BaseObject baseObject){
		int id=baseObject.getID();
		if((id+1)>lastBaseObjectId){
			lastBaseObjectId=id+1;
		}
		baseObjects.put(baseObject.getID(),baseObject);
	}
	public void addSummary(FieldObject dest,Set<FieldObject> source){
		Set<FieldObject> sourceSet;
		if(summaries.containsKey(dest)){
			sourceSet=summaries.get(dest);
		}else{
			sourceSet=new HashSet<FieldObject>();
			summaries.put(dest, sourceSet);
		}
		sourceSet.addAll(source);
	}
	public void addSummary(FieldObject dest,FieldObject source){
		Set<FieldObject> sourceSet;
		if(summaries.containsKey(dest)){
			sourceSet=summaries.get(dest);
		}else{
			sourceSet=new HashSet<FieldObject>();
			summaries.put(dest, sourceSet);
		}
		sourceSet.add(source);
	}
	
	public boolean isEmpty(){
		return summaries.isEmpty();
	}
	public void addGap(GapDefinition gap){
		int id=gap.getID();
		if((id+1)>lastGapId){
			lastGapId=id+1;
		}
		if(!gaps.containsKey(id)){
			gaps.put(id, gap);
		}
	}
}
