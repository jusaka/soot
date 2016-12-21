package soot.jimple.spark.pag;

import soot.SootMethod;
import soot.Type;

public class FakeBaseNode extends FakeNode{
	
	FakeBaseNode(PAG pag, Object newExpr, Type t, SootMethod m) {
		super(pag, newExpr, t, m);
	}
	protected FakeNode base;
    protected SparkField field;
}
