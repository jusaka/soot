package soot.jimple.spark.pag;

import java.util.HashMap;
import java.util.Map;

import soot.SootMethod;
import soot.Type;
import soot.toolkits.scalar.Pair;

public class FakeNode extends AllocNode{
	public FakeNode(PAG pag,Object pair, Type t, SootMethod m){
		super(pag, pair, t, m);
	}
	
	public FakeNode(PAG pag,Pair pair, Type t, SootMethod m){
		super(pag, pair, t, m);
	}
	public String toString() {
		return "FakeNode "+getNumber()+" "+((Pair)newExpr).getO1()+" "+((Pair)newExpr).getO2();
	}
	
	public FakeBaseNode getFakeBaseNode(SparkField field){
		FakeBaseNode baseNode=fieldNodes.get(field);
		if(baseNode==null){
			baseNode=new FakeBaseNode(this.pag,this.getNewExpr(),
					field.getType(),this.getMethod(),this,field);
			fieldNodes.put(field, baseNode);
		}
		return baseNode;
	}
    protected Map<SparkField, FakeBaseNode> fieldNodes=new HashMap<>();
}
