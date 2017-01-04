package soot.jimple.spark.pag;

import soot.SootMethod;
import soot.Type;

public class FakeVarNode extends LocalVarNode{
	FakeVarNode(PAG pag, Object variable, Type t, SootMethod m,String methodSig) {
		super(pag, variable, t, m);
		this.methodSig=methodSig;
		if(m==null){
			isFake=true;
		}else{
			isFake=false;
			methodSig=m.getSignature();
		}
		
	}
	
    public String toString() {
    	return "FakeVarNode "+getNumber()+" ("+variable+") in method "+method;
    }
    public String getMethodSig(){
    	return this.methodSig;
    }
    boolean isFake=false;
    String methodSig;
}
