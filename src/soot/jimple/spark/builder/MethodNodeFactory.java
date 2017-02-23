/* Soot - a J*va Optimization Framework
 * Copyright (C) 2002 Ondrej Lhotak
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */

package soot.jimple.spark.builder;

import soot.jimple.spark.internal.ClientAccessibilityOracle;
import soot.jimple.spark.internal.SparkLibraryHelper;
import soot.jimple.spark.pag.*;
import soot.jimple.spark.summary.BaseObject;
import soot.jimple.spark.summary.BaseObjectType;
import soot.jimple.spark.summary.FieldObject;
import soot.jimple.spark.summary.MethodObjects;
import soot.jimple.spark.summary.ObjectUtils;
import soot.jimple.*;

import java.util.*;

import soot.*;
import soot.toolkits.scalar.Pair;
import soot.options.CGOptions;
import soot.options.Options;
import soot.shimple.*;

/**
 * Class implementing builder parameters (this decides what kinds of nodes
 * should be built for each kind of Soot value).
 * 
 * @author Ondrej Lhotak
 */
public class MethodNodeFactory extends AbstractShimpleValueSwitch {
	public MethodNodeFactory(PAG pag, MethodPAG mpag) {
		this.pag = pag;
		this.mpag = mpag;
		this.fieldObjectToNode=new HashMap<FieldObject,Node>();
		if(mpag.isFake()){
			setCurrentMethod(mpag.getMethodSig(),mpag.getMethodObjects());
		}else{
			setCurrentMethod(mpag.getMethod());
		}
	}
	
	
	/** Sets the method for which a graph is currently being built. */
	private void setCurrentMethod(SootMethod m) {
		
		method = m;
		if (!m.isStatic()) {
			SootClass c = m.getDeclaringClass();
			if (c == null) {
				throw new RuntimeException("Method " + m + " has no declaring class");
			}
			caseThis();
		}
		for (int i = 0; i < m.getParameterCount(); i++) {
			if (m.getParameterType(i) instanceof RefLikeType) {
				caseParm(i);
			}
		}
		Type retType = m.getReturnType();
		if (retType instanceof RefLikeType) {
			caseRet();
		}
	}
	
	private void setCurrentMethod(String methodSig,MethodObjects methodObjects){
		this.isFake=true;
		this.methodSig=methodSig;
		this.methodObjects=methodObjects;
		if(methodObjects.getThisObject()!=null){
			caseThis();
		}
		if(methodObjects.getParamIndexes()!=null){
			for(Integer index:methodObjects.getParamIndexes()){
				caseParm(index);
			}
		}
		if(methodObjects.getReturnObject()!=null){
			caseRet();
		}
	}

	public Node getNode(Value v) {
		v.apply(this);
		return getNode();
	}

	/** Adds the edges required for this statement to the graph. */
	final public void handleStmt(Stmt s) {
		if (s.containsInvokeExpr()) {
			return;
		}
		s.apply(new AbstractStmtSwitch() {
			final public void caseAssignStmt(AssignStmt as) {
				Value l = as.getLeftOp();
				Value r = as.getRightOp();
				if (!(l.getType() instanceof RefLikeType))
					return;
				assert r.getType() instanceof RefLikeType : "Type mismatch in assignment " + as + " in method "
						+ method.getSignature();
				l.apply(MethodNodeFactory.this);
				Node dest = getNode();
				r.apply(MethodNodeFactory.this);
				Node src = getNode();
				if (l instanceof InstanceFieldRef) {
					((InstanceFieldRef) l).getBase().apply(MethodNodeFactory.this);
					pag.addDereference((VarNode) getNode());
				}
				if (r instanceof InstanceFieldRef) {
					((InstanceFieldRef) r).getBase().apply(MethodNodeFactory.this);
					pag.addDereference((VarNode) getNode());
				}
				if (r instanceof StaticFieldRef) {
					StaticFieldRef sfr = (StaticFieldRef) r;
					SootFieldRef s = sfr.getFieldRef();
					if (pag.getOpts().empties_as_allocs()) {
						if (s.declaringClass().getName().equals("java.util.Collections")) {
							if (s.name().equals("EMPTY_SET")) {
								src = pag.makeAllocNode(RefType.v("java.util.HashSet"), RefType.v("java.util.HashSet"),
										method);
							} else if (s.name().equals("EMPTY_MAP")) {
								src = pag.makeAllocNode(RefType.v("java.util.HashMap"), RefType.v("java.util.HashMap"),
										method);
							} else if (s.name().equals("EMPTY_LIST")) {
								src = pag.makeAllocNode(RefType.v("java.util.LinkedList"),
										RefType.v("java.util.LinkedList"), method);
							}
						} else if (s.declaringClass().getName().equals("java.util.Hashtable")) {
							if (s.name().equals("emptyIterator")) {
								src = pag.makeAllocNode(RefType.v("java.util.Hashtable$EmptyIterator"),
										RefType.v("java.util.Hashtable$EmptyIterator"), method);
							} else if (s.name().equals("emptyEnumerator")) {
								src = pag.makeAllocNode(RefType.v("java.util.Hashtable$EmptyEnumerator"),
										RefType.v("java.util.Hashtable$EmptyEnumerator"), method);
							}
						}
					}
				}
				mpag.addInternalEdge(src, dest);
			}

			final public void caseReturnStmt(ReturnStmt rs) {
				if (!(rs.getOp().getType() instanceof RefLikeType))
					return;
				rs.getOp().apply(MethodNodeFactory.this);
				Node retNode = getNode();
				mpag.addInternalEdge(retNode, caseRet());
			}

			final public void caseIdentityStmt(IdentityStmt is) {
				if (!(is.getLeftOp().getType() instanceof RefLikeType))
					return;
				Value leftOp = is.getLeftOp();
				Value rightOp = is.getRightOp();
				leftOp.apply(MethodNodeFactory.this);
				Node dest = getNode();
				rightOp.apply(MethodNodeFactory.this);
				Node src = getNode();
				mpag.addInternalEdge(src, dest);

				// in case library mode is activated add allocations to any
				// possible type of this local and
				// parameters of accessible methods
				int libOption = pag.getCGOpts().library();
				if (libOption != CGOptions.library_disabled && (accessibilityOracle.isAccessible(method))) {
					if (rightOp instanceof IdentityRef) {
						Type rt = rightOp.getType();
						rt.apply(new SparkLibraryHelper(pag, src, method));
					}
				}

			}

			final public void caseThrowStmt(ThrowStmt ts) {
				ts.getOp().apply(MethodNodeFactory.this);
				mpag.addOutEdge(getNode(), pag.nodeFactory().caseThrow());
			}
		});
	}

	final public Node getNode() {
		return (Node) getResult();
	}
	
	
	final public void handleMethodObjects(MethodObjects methodObjects){
		for(BaseObject baseObject:methodObjects.getBaseObjects()){
    		caseBaseObject(baseObject);
    	}
    	Map<FieldObject,Set<FieldObject>> destToSourceMap=methodObjects.getSummaries();
    	for(FieldObject dest:destToSourceMap.keySet()){
    		Set<FieldObject> sources=destToSourceMap.get(dest);
    		for(FieldObject source:sources){
    			addSummary(source,dest);
    		}
    	}
	}

	final public Node caseBaseObject(BaseObject baseObject) {
		
		FieldObject fieldObject = new FieldObject(this.method==null?methodSig:method.getSignature(),baseObject, baseObject.getTypeString());
		if(fieldObjectToNode.containsKey(fieldObject)) return fieldObjectToNode.get(fieldObject);
		Type fieldType = ObjectUtils.getTypeFromString(fieldObject.getFieldType());
		FakeVarNode fakeVarNode = pag.makeFakeVarNode(fieldObject, fieldType, method,methodSig);
		fieldObjectToNode.put(fieldObject,fakeVarNode);
		switch (baseObject.getBaseObjectType()) {
		case This:
			mpag.addInternalEdge(caseThis(), fakeVarNode);
			break;
		case Parameter:
			int index = baseObject.getIndex();
			mpag.addInternalEdge(caseParm(index), fakeVarNode);
			break;
		case Return:
			mpag.addInternalEdge(fakeVarNode, caseRet());
			break;
		default:
			break;
		}
		return fakeVarNode;
	}

	final public Node caseFieldObject(FieldObject fieldObject) {
		if(fieldObjectToNode.containsKey(fieldObject)) return fieldObjectToNode.get(fieldObject);
		Node result;
		if (fieldObject.isNew()) {
			Type baseType = ObjectUtils.getTypeFromString(fieldObject.getBaseType());
			result=pag.makeAllocNode(baseType, baseType, method);
		} else if (!fieldObject.hasField()) {
			Type fieldType = ObjectUtils.getTypeFromString(fieldObject.getFieldType());
			FakeVarNode fakeVarNode = pag.makeFakeVarNode(fieldObject, fieldType, method, methodSig);
			result=fakeVarNode;
		} else {
			String accessPath = fieldObject.getAccessPath();
			List<String> splitedAP = ObjectUtils.splitAccessPath(accessPath);
			BaseObject baseObject = fieldObject.getBaseObject();
			FieldObject src = new FieldObject(this.method==null?methodSig:method.getSignature(),baseObject, baseObject.getTypeString());
			Type srcType = ObjectUtils.getTypeFromString(src.getFieldType());
			FakeVarNode srcNode = pag.makeFakeVarNode(src, srcType, method, methodSig);
			
			FakeVarNode destNode=null;
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < splitedAP.size(); i++) {
				
				String fieldSig = splitedAP.get(i);
				SootField field = ObjectUtils.safeGetField(fieldSig);
				FieldRefNode fieldRefNode = pag.makeFieldRefNode(srcNode, field);
				pag.addDereference(srcNode);
				sb.append(".[");
				sb.append(fieldSig);
				sb.append("]");
				
				FieldObject dest=new FieldObject(this.method==null?methodSig:method.getSignature(),baseObject,sb.toString(),field.getType().toString());
				destNode= pag.makeFakeVarNode(dest, field.getType(), method, methodSig);
				mpag.addInternalEdge(fieldRefNode, destNode);
				srcNode=destNode;
			}
			result=srcNode;
		}
		fieldObjectToNode.put(fieldObject, result);
		return result;
	}

	final public void addSummary(FieldObject src, FieldObject dest) {
		Node srcNode=caseFieldObject(src);
		Node destNode=caseFieldObject(dest);
		mpag.addInternalEdge(srcNode, destNode);
	}

	final public Node caseThis() {
		VarNode ret;
		if(isFake){
			BaseObject baseObject=methodObjects.getThisObject();
			ret = pag.makeLocalVarNode(new Pair<String, String>(methodSig, PointsToAnalysis.THIS_NODE),
					baseObject.getType(), null);
		}else{
			ret = pag.makeLocalVarNode(new Pair<SootMethod, String>(method, PointsToAnalysis.THIS_NODE),
					method.getDeclaringClass().getType(), method);
			caseSourceNode(ret, new Pair<BaseObjectType, SootMethod>(BaseObjectType.This, method),
					method.getDeclaringClass().getType());
		}
		ret.setInterProcTarget();
		return ret;
	}

	final public Node caseParm(int index) {
		VarNode ret;
		if(isFake){
			BaseObject baseObject=methodObjects.getParamObject(index);
			ret = pag.makeLocalVarNode(new Pair<String, Integer>(methodSig, new Integer(index)),
					baseObject.getType(), null);
		}else{
			ret = pag.makeLocalVarNode(new Pair<SootMethod, Integer>(method, new Integer(index)),
					method.getParameterType(index), method);
			caseSourceNode(ret, new Pair<BaseObjectType, Integer>(BaseObjectType.Parameter, new Integer(index)),
					method.getParameterType(index));
		}
		ret.setInterProcTarget();
		
		return ret;
	}

	final public void casePhiExpr(PhiExpr e) {
		Pair<Expr, String> phiPair = new Pair<Expr, String>(e, PointsToAnalysis.PHI_NODE);
		Node phiNode = pag.makeLocalVarNode(phiPair, e.getType(), method);
		for (Value op : e.getValues()) {
			op.apply(MethodNodeFactory.this);
			Node opNode = getNode();
			mpag.addInternalEdge(opNode, phiNode);
		}
		setResult(phiNode);
	}

	final public Node caseRet() {
		VarNode ret;
		if(isFake){
			BaseObject baseObject=methodObjects.getReturnObject();
			ret = pag.makeLocalVarNode(new Pair<String, Integer>(methodSig,PointsToAnalysis.RETURN_NODE), baseObject.getType(),
					null);
		}else{
			ret = pag.makeLocalVarNode(Parm.v(method, PointsToAnalysis.RETURN_NODE), method.getReturnType(),
					method);
			caseSourceNode(ret, new Pair<BaseObjectType, SootMethod>(BaseObjectType.Return, method),
							method.getReturnType());
		}
		ret.setInterProcSource();
		return ret;
	}

	final public Node caseArray(VarNode base) {
		return pag.makeFieldRefNode(base, ArrayElement.v());
	}
	/* End of public methods. */
	/* End of package methods. */

	// OK, these ones are public, but they really shouldn't be; it's just
	// that Java requires them to be, because they override those other
	// public methods.
	@Override
	final public void caseArrayRef(ArrayRef ar) {
		caseLocal((Local) ar.getBase());
		setResult(caseArray((VarNode) getNode()));
	}

	final public void caseCastExpr(CastExpr ce) {
		Pair<Expr, String> castPair = new Pair<Expr, String>(ce, PointsToAnalysis.CAST_NODE);
		ce.getOp().apply(this);
		Node opNode = getNode();
		Node castNode = pag.makeLocalVarNode(castPair, ce.getCastType(), method);
		mpag.addInternalEdge(opNode, castNode);
		setResult(castNode);
	}

	@Override
	final public void caseCaughtExceptionRef(CaughtExceptionRef cer) {
		setResult(pag.nodeFactory().caseThrow());
	}

	@Override
	final public void caseInstanceFieldRef(InstanceFieldRef ifr) {
		if (pag.getOpts().field_based() || pag.getOpts().vta()) {
			setResult(pag.makeGlobalVarNode(ifr.getField(), ifr.getField().getType()));
		} else {
			setResult(pag.makeLocalFieldRefNode(ifr.getBase(), ifr.getBase().getType(), ifr.getField(), method));
		}
	}

	@Override
	final public void caseLocal(Local l) {
		setResult(pag.makeLocalVarNode(l, l.getType(), method));
	}

	@Override
	final public void caseNewArrayExpr(NewArrayExpr nae) {
		setResult(pag.makeAllocNode(nae, nae.getType(), method));
	}

	private boolean isStringBuffer(Type t) {
		if (!(t instanceof RefType))
			return false;
		RefType rt = (RefType) t;
		String s = rt.toString();
		if (s.equals("java.lang.StringBuffer"))
			return true;
		if (s.equals("java.lang.StringBuilder"))
			return true;
		return false;
	}

	@Override
	final public void caseNewExpr(NewExpr ne) {
		if (pag.getOpts().merge_stringbuffer() && isStringBuffer(ne.getType())) {
			setResult(pag.makeAllocNode(ne.getType(), ne.getType(), null));
		} else {
			setResult(pag.makeAllocNode(ne, ne.getType(), method));
		}
	}

	@Override
	final public void caseNewMultiArrayExpr(NewMultiArrayExpr nmae) {
		ArrayType type = (ArrayType) nmae.getType();
		AllocNode prevAn = pag.makeAllocNode(new Pair<Expr, Integer>(nmae, new Integer(type.numDimensions)), type,
				method);
		VarNode prevVn = pag.makeLocalVarNode(prevAn, prevAn.getType(), method);
		mpag.addInternalEdge(prevAn, prevVn);
		setResult(prevAn);
		while (true) {
			Type t = type.getElementType();
			if (!(t instanceof ArrayType))
				break;
			type = (ArrayType) t;
			AllocNode an = pag.makeAllocNode(new Pair<Expr, Integer>(nmae, new Integer(type.numDimensions)), type,
					method);
			VarNode vn = pag.makeLocalVarNode(an, an.getType(), method);
			mpag.addInternalEdge(an, vn);
			mpag.addInternalEdge(vn, pag.makeFieldRefNode(prevVn, ArrayElement.v()));
			prevAn = an;
			prevVn = vn;
		}
	}

	@Override
	final public void caseParameterRef(ParameterRef pr) {
		setResult(caseParm(pr.getIndex()));
	}

	@Override
	final public void caseStaticFieldRef(StaticFieldRef sfr) {
		setResult(pag.makeGlobalVarNode(sfr.getField(), sfr.getField().getType()));
	}

	@Override
	final public void caseStringConstant(StringConstant sc) {
		AllocNode stringConstant;
		if (pag.getOpts().string_constants() || Scene.v().containsClass(sc.value)
				|| (sc.value.length() > 0 && sc.value.charAt(0) == '[')) {
			stringConstant = pag.makeStringConstantNode(sc.value);
		} else {
			stringConstant = pag.makeAllocNode(PointsToAnalysis.STRING_NODE, RefType.v("java.lang.String"), null);
		}
		VarNode stringConstantLocal = pag.makeGlobalVarNode(stringConstant, RefType.v("java.lang.String"));
		pag.addEdge(stringConstant, stringConstantLocal);
		setResult(stringConstantLocal);
	}

	@Override
	final public void caseThisRef(ThisRef tr) {
		setResult(caseThis());
	}

	@Override
	final public void caseNullConstant(NullConstant nr) {
		setResult(null);
	}

	final public void caseSourceNode(VarNode dest, Pair pair, Type type) {
		MethodObjects methodObjects = Options.v().method_objects();
		if (methodObjects != null && method.getSignature().equals(methodObjects.getMethodSig())
				&& type instanceof RefType && !pag.varToFakeNode.containsKey(dest)) {
			methodObjects.setMethod(method);
			FakeNode src = pag.makeFakeNode(pag, pair, type, method);
			if (!pag.varToFakeNode.containsKey(dest)) {
				pag.varToFakeNode.put(dest, src);
				pag.needToAdd.add(dest);
			}
		}
	}

	@Override
	final public void caseClassConstant(ClassConstant cc) {
		AllocNode classConstant = pag.makeClassConstantNode(cc);
		VarNode classConstantLocal = pag.makeGlobalVarNode(classConstant, RefType.v("java.lang.Class"));
		pag.addEdge(classConstant, classConstantLocal);
		setResult(classConstantLocal);
	}

	@Override
	final public void defaultCase(Object v) {
		throw new RuntimeException("failed to handle " + v);
	}
	
	protected Map<FieldObject,Node> fieldObjectToNode;
	protected PAG pag;
	protected MethodPAG mpag;
	protected SootMethod method;
	protected String methodSig;
	protected MethodObjects methodObjects;
	protected boolean isFake;
	protected ClientAccessibilityOracle accessibilityOracle = Scene.v().getClientAccessibilityOracle();
}
