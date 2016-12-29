package soot.jimple.spark.pag;

import soot.SootMethod;
import soot.Type;

public class FakeBaseNode extends FakeNode{
	
	public FakeBaseNode(PAG pag, Object newExpr, Type t, SootMethod m,FakeNode base,SparkField field) {
		super(pag, newExpr, t, m);
		this.base=base;
		this.field=field;
	}
	public FakeNode getBase(){
		return base;
	}
	public SparkField getField(){
		return field;
	}
	protected FakeNode base;
    protected SparkField field;
}
