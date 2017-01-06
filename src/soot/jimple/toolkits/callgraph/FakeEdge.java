package soot.jimple.toolkits.callgraph;

import soot.SootMethod;
import soot.jimple.spark.summary.GapDefinition;
import soot.jimple.spark.summary.MethodObjects;

public class FakeEdge {
	private SootMethod src;
	private String srcSig;
	private SootMethod tgt;
	private String tgtSig;
	private GapDefinition gap;
	private MethodObjects srcMethodObjects;
	private MethodObjects tgtMethodObjects;
	private boolean fakeSrc = true;
	private boolean fakeTgt = true;

	public FakeEdge(SootMethod src, String srcSig, MethodObjects srcMethodObjects, SootMethod tgt, String tgtSig,
			MethodObjects tgtMethodObjects, GapDefinition gap) {
		
		this.src = src;
		this.srcSig = srcSig;
		this.srcMethodObjects = srcMethodObjects;
		this.tgt = tgt;
		this.tgtSig = tgtSig;
		this.tgtMethodObjects = tgtMethodObjects;
		this.gap = gap;
		if (src == null) {
			fakeSrc = true;
		}else{
			this.srcSig=src.getSignature();
		}
		if (tgtMethodObjects == null) {
			fakeTgt = false;
		}
		if(tgt!=null){
			this.tgtSig=tgt.getSignature();
		}
	}
	public boolean isValid(){
		if((src==null&&srcMethodObjects==null)||(tgt==null&&tgtMethodObjects==null)){
			return false;
		}
		return true;
	}

	private FakeEdge nextByGap = this;
	private FakeEdge prevByGap = this;

	public void insertAfterByGap(FakeEdge other) {
		nextByGap = other.nextByGap;
		nextByGap.prevByGap = this;
		other.nextByGap = this;
		prevByGap = other;
	}

	public void insertBeforeByUnit(FakeEdge other) {
		prevByGap = other.prevByGap;
		prevByGap.nextByGap = this;
		other.prevByGap = this;
		nextByGap = other;
	}

	public GapDefinition srcGap() {
		return this.gap;
	}

	public SootMethod getTgt() {
		return tgt;
	}

	public SootMethod getSrc() {
		return src;
	}

	public String getTgtSig() {
		return tgtSig;
	}
	
	public String getSrcSig() {
		return srcSig;
	}
	
	public MethodObjects getTgtMethodObjects(){
		return tgtMethodObjects;
	}
	
	public MethodObjects getSrcMethodObjects(){
		return srcMethodObjects;
	}
	
	public boolean tgtFake(){
		return fakeTgt;
	}

	public boolean srcFake(){
		return fakeSrc;
	}
	
	
	@Override
	public boolean equals(Object other) {
		if (other == null || !(other instanceof FakeEdge))
			return false;
		if (this == other)
			return true;
		FakeEdge fakeEdge = (FakeEdge) other;
		if (gap.equals(fakeEdge.gap) && srcSig.equals(fakeEdge.srcSig) && tgtSig.equals(fakeEdge.tgtSig)) {
			return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		int hashCode = gap.hashCode();
		hashCode = hashCode * 31 + srcSig.hashCode();
		hashCode = hashCode * 31 + tgtSig.hashCode();
		return hashCode;
	}
	FakeEdge nextByGap() {
        return nextByGap;
    }
}
