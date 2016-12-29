package soot.jimple.spark.summary;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

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
	private Map<FieldObject,Set<FieldObject>> summaries;
	private Map<Integer, BaseObject> baseObjects;
	private Map<Integer, GapDefinition> gaps;
	public MethodObjects(){
		summaries=new HashMap<FieldObject,Set<FieldObject>>();
		baseObjects=new HashMap<Integer,BaseObject>();
		gaps=new HashMap<Integer, GapDefinition>();
	}
	public GapDefinition getOrCreateGap(int gapID, String signature) {
		GapDefinition gd = this.gaps.get(gapID);
		if (gd == null) {
			gd = new GapDefinition(gapID, signature);
			this.gaps.put(gapID, gd);
		}
		if (gd.getSignature() == null || gd.getSignature().isEmpty())
			gd.setSignature(signature);
		else if (!gd.getSignature().equals(signature))
			throw new RuntimeException("Gap signature mismatch detected");
		
		return gd;
	}
	public BaseObject getOrCreatepBaseObject(int baseObjectID, String type,BaseObjectType baseObjectType) {
		BaseObject baseObject=this.baseObjects.get(baseObjectID);
		if(baseObject==null){
			baseObject=new BaseObject(baseObjectID,type,baseObjectType);
			this.baseObjects.put(baseObjectID, baseObject);
		}
		if(baseObject.getBaseObjectType()==null){
			baseObject.setBaseObejctType(baseObjectType);
		}
		if(baseObject.getType()==null||baseObject.getType().isEmpty()){
			baseObject.setType(type);
		}
		if(baseObject.getBaseObjectType()!=(baseObjectType)||!baseObject.getType().equals(type)){
			throw new RuntimeException("More than one base object match into one id");
		}
		return baseObject;
	}
	public void addSummary(FakeNode node,PointsToSetInternal p2set,PAG pag){
		p2set.forall( new P2SetVisitor() {
        public final void visit( Node n ) { 
        	FieldObject src=getFieldObject(n,pag);
        	FieldObject dest=getFieldObject(node,pag);
        	if(src.equals(dest)&&src.baseObject!=null&&src.baseObject.isGap()){
        		return;
        	}
        	Set<FieldObject> srcSet=summaries.get(dest);
        	if(srcSet==null){
        		srcSet=new HashSet<FieldObject>();
        		summaries.put(dest, srcSet);
        	}
			srcSet.add(src);
        	handleFields((AllocNode)n,node,pag);
        }} );
	}
	private void handleFields(AllocNode src,FakeNode dest,PAG pag){
		for(AllocDotField allocDotField:src.getAllFieldRefs()){
			SparkField field=allocDotField.getField();
			addSummary(dest.getFakeBaseNode(field),allocDotField.getP2Set().getNewSet(),pag);
		}
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
    		object=new FieldObject(node.getType(),n.getType().toString());
    	}
		return object;
	}
	public void compact(){
		Set<Pair<FieldObject,FieldObject>> removeSet=new HashSet<Pair<FieldObject,FieldObject>>();
		for(FieldObject dest:summaries.keySet()){
			for(FieldObject src:summaries.get(dest)){
				for(FieldObject temp:summaries.get(src)){
					if(summaries.get(dest).contains(temp)){
						Pair<FieldObject,FieldObject> pair=new Pair<FieldObject,FieldObject>(temp,dest);
						removeSet.add(pair);
					}
				}
			}
		}
		for(Pair<FieldObject,FieldObject> pair:removeSet){
			if(summaries.get(pair.getO2())!=null){
				summaries.get(pair.getO2()).remove(pair.getO1());
				if(summaries.get(pair.getO2()).isEmpty()){
					summaries.remove(pair.getO2());
				}
			}
		}
	}
}
