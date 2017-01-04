/* Soot - a J*va Optimization Framework
 * Copyright (C) 2002,2003 Ondrej Lhotak
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

package soot.jimple.spark.solver;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import soot.Context;
import soot.Kind;
import soot.Local;
import soot.MethodOrMethodContext;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.spark.pag.AllocNode;
import soot.jimple.spark.pag.FakeNode;
import soot.jimple.spark.pag.FakeVarNode;
import soot.jimple.spark.pag.MethodPAG;
import soot.jimple.spark.pag.Node;
import soot.jimple.spark.pag.PAG;
import soot.jimple.spark.pag.StringConstantNode;
import soot.jimple.spark.pag.VarNode;
import soot.jimple.spark.sets.P2SetVisitor;
import soot.jimple.spark.sets.PointsToSetInternal;
import soot.jimple.spark.summary.BaseObject;
import soot.jimple.spark.summary.BaseObjectType;
import soot.jimple.spark.summary.FieldObject;
import soot.jimple.spark.summary.GapDefinition;
import soot.jimple.spark.summary.MethodObjects;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.CallGraphBuilder;
import soot.jimple.toolkits.callgraph.ContextManager;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.callgraph.FakeEdge;
import soot.jimple.toolkits.callgraph.OnFlyCallGraphBuilder;
import soot.jimple.toolkits.callgraph.ReachableMethods;
import soot.jimple.toolkits.callgraph.VirtualCallSite;
import soot.options.Options;
import soot.toolkits.scalar.Pair;
import soot.util.queue.QueueReader;


/** The interface between the pointer analysis engine and the on-the-fly
 * call graph builder.
 * @author Ondrej Lhotak
 */

public class OnFlyCallGraph {
    private final OnFlyCallGraphBuilder ofcgb;
    private final ReachableMethods reachableMethods;
    private final QueueReader<MethodOrMethodContext> reachablesReader;
    private final QueueReader<Edge> callEdges;
    private final CallGraph callGraph;
    private final QueueReader<FakeEdge> fakeCallEdges;

    public ReachableMethods reachableMethods() { return reachableMethods; }
    public CallGraph callGraph() { return callGraph; }

    public OnFlyCallGraph( PAG pag, boolean appOnly ) {
        this.pag = pag;
        callGraph = new CallGraph();
        Scene.v().setCallGraph( callGraph );
        ContextManager cm = CallGraphBuilder.makeContextManager(callGraph);
        reachableMethods = Scene.v().getReachableMethods();
        ofcgb = new OnFlyCallGraphBuilder( cm, reachableMethods, appOnly );
        reachablesReader = reachableMethods.listener();
        callEdges = cm.callGraph().listener();
        fakeCallEdges=cm.callGraph().fakeListener();
    }
    public void build() {
        ofcgb.processReachables();
        processReachables();
        processCallEdges();
        processFakeCallEdges();
    }
    private void processReachables() {
        reachableMethods.update();
        while(reachablesReader.hasNext()) {
            MethodOrMethodContext m = (MethodOrMethodContext) reachablesReader.next();
            MethodPAG mpag = MethodPAG.v( pag, m.method() );
            mpag.build();
            mpag.addToPAG(m.context());
        }
    }
    private void processCallEdges() {
        while(callEdges.hasNext()) {
            Edge e = (Edge) callEdges.next();
            MethodPAG amp = MethodPAG.v( pag, e.tgt() );
            amp.build();
            amp.addToPAG( e.tgtCtxt() );
            pag.addCallTarget( e );
        }
    }
    
    private void processFakeCallEdges() {
        while(fakeCallEdges.hasNext()) {
        	FakeEdge fakeEdge=fakeCallEdges.next();
        	MethodPAG amp;
        	if(fakeEdge.tgtFake()){
        		amp=MethodPAG.v(pag, fakeEdge.getTgtSig(),fakeEdge.getTgtMethodObjects());
        	}else{
        		amp=MethodPAG.v(pag, fakeEdge.getTgt());
        	} 
        	amp.build();
        	amp.addToPAG(null);
        	pag.addCallTarget( fakeEdge );
        }
    }

    public OnFlyCallGraphBuilder ofcgb() { return ofcgb; }

    public void updateNode(FakeVarNode fvn){
    	PointsToSetInternal p2set = fvn.getP2Set().getNewSet();
		FieldObject fieldObject = (FieldObject) fvn.getVariable();
		BaseObject baseObject = fieldObject.getBaseObject();
		p2set.forall(new P2SetVisitor() {
			public final void visit(Node n) {
				 ofcgb.addType( baseObject, fvn, n.getType(), (AllocNode) n);
			}
		}); 
    }
    
    public void updatedNode( VarNode vn ) {
        Object r = vn.getVariable();
        if(vn instanceof FakeVarNode&&((FieldObject)r).isGapBase()){
    		updateNode((FakeVarNode)vn);
    	}
        if( !(r instanceof Local) ) return;
        final Local receiver = (Local) r;
        final Context context = vn.context();

        PointsToSetInternal p2set = vn.getP2Set().getNewSet();
        if( ofcgb.wantTypes( receiver ) ) {
        	final boolean[] visit=new boolean[1];
            p2set.forall( new P2SetVisitor() {
            public final void visit( Node n ) { 
            	if(n instanceof FakeNode){
            		visit[0]=true;
            	}
            }} );
            if(visit[0]){
        		List<VirtualCallSite> virtualCallSites=ofcgb.getVirtualCallSites(receiver);
        		for(VirtualCallSite virtualCallSite:virtualCallSites){
        			SootMethod targetMethod=virtualCallSite.iie().getMethod();
        			SootClass targetClass=targetMethod.getDeclaringClass();
        			//TODO
        			if(!(targetMethod.isPublic()||targetMethod.isProtected())
        					||targetMethod.isFinal()||targetClass.isFinal()
        					||(virtualCallSite.kind()==Kind.SPECIAL&&targetClass.isAbstract())){
        				ofcgb.addType( receiver, context, targetMethod.getDeclaringClass().getType(), null );
        			}else{
        				handleGapCall(virtualCallSite,vn);
        			}
        		}
            }else{
                p2set.forall( new P2SetVisitor() {
                public final void visit( Node n ) { 
                    ofcgb.addType( receiver, context, n.getType(), (AllocNode) n );
                }} );
            }
        }
        
        if( ofcgb.wantStringConstants( receiver ) ) {
            p2set.forall( new P2SetVisitor() {
            public final void visit( Node n ) {
                if( n instanceof StringConstantNode ) {
                    String constant = ((StringConstantNode)n).getString();
                    ofcgb.addStringConstant( receiver, context, constant );
                } else {
                    ofcgb.addStringConstant( receiver, context, null );
                }
            }} );
        }
    }
    private void handleGapCall(VirtualCallSite site,VarNode baseNode){
    	MethodObjects methodObjects=Options.v().method_objects();
    	GapDefinition gapDefinition;
    	if(!gaps.containsKey(site)){
    		SootMethod method=site.iie().getMethod();
        	if(!method.isNative()&&(site.kind()==Kind.VIRTUAL||site.kind()==Kind.SPECIAL)){
        		gapDefinition=methodObjects.createGap(method.getSignature());
        		gaps.put(site, gapDefinition);
        	}else{
        		if(!site.iie().getMethod().isNative()){
        			System.out.println(site.kind());
        		}
        		return;
        	}
    	}else{
    		gapDefinition=gaps.get(site);
    	}
    	Local base=(Local)site.iie().getBase();
    	Pair<BaseObjectType,GapDefinition> basePair=new Pair<BaseObjectType,GapDefinition>(BaseObjectType.GapBaseObject,gapDefinition);
    	handleGapCall(site,baseNode,basePair,base);
    	
    	for(int i=0;i<site.iie().getArgCount();i++){
    		Value arg=site.iie().getArg(i);
    		if(arg.getType() instanceof RefType){
    			Pair<GapDefinition,Integer> gapIndex=new Pair<GapDefinition,Integer>(gapDefinition,i);
    			Pair<BaseObjectType,Pair<GapDefinition,Integer>> argPair
        		=new Pair<BaseObjectType,Pair<GapDefinition,Integer>>(BaseObjectType.GapParameter,gapIndex);
    			VarNode argNode=pag.makeLocalVarNode( (Local) arg,  arg.getType(), site.container() );
    			handleGapCall(site,argNode,argPair,arg);
    		}
    	}
    	if(site.stmt() instanceof AssignStmt){
    		AssignStmt as=(AssignStmt) site.stmt();
    		if(as.getLeftOp().getType() instanceof RefType){
    			Local ret=(Local) as.getLeftOp();
    			Pair<BaseObjectType,GapDefinition> returnPair=new Pair<BaseObjectType,GapDefinition>(BaseObjectType.GapReturn,gapDefinition);
    			FakeNode fakeNode=pag.makeFakeNode(pag, returnPair,site.iie().getType(), site.container());
            	VarNode retNode=pag.makeLocalVarNode(ret,ret.getType(), site.container());
            	retNode.makeP2Set().getNewSet().add(fakeNode);
            	pag.needToAdd.add(retNode);
    		}
    	}
    }
    
    private void handleGapCall(VirtualCallSite site,VarNode node,Pair pair,Value value){
    	PointsToSetInternal set=node.getP2Set();
    	FakeNode fakeNode=pag.makeFakeNode(pag, pair,value.getType(), site.container());
    	if(Options.v().method_objects().addDestToSource(fakeNode, set, pag)){
    		set.clear();
    		set.add(fakeNode);
        	pag.needToAdd.add(node);
    	}
    }
    
    /** Node uses this to notify PAG that n2 has been merged into n1. */
    public void mergedWith( Node n1, Node n2 ) {
    }

    /* End of public methods. */
    /* End of package methods. */

    private PAG pag;
    private Map<VirtualCallSite,GapDefinition> gaps=new HashMap<VirtualCallSite,GapDefinition>();
    
}



