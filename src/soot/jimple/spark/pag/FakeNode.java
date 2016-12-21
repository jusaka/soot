package soot.jimple.spark.pag;

import soot.SootMethod;
import soot.Type;

public class FakeNode extends AllocNode{
	FakeNode(PAG pag, Object newExpr, Type t, SootMethod m) {
		super(pag, newExpr, t, m);
	}
	
	public void setFakeType(FakeType fakeType){
		this.fakeType=fakeType;
	}
	
	public FakeType getFakeType(){
		return fakeType;
	}

	public enum FakeType{
		THIS,
		PARAMETER,
		RETURN,
		GAP
	}
	
	protected FakeType fakeType;
	protected int index;
}
