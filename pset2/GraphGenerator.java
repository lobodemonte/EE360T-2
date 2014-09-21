package pset2;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.generic.*;

import pset2.CFG.Node;

/**
 *GraphGenerator for a CFG 
 *Note: Dummy end node has a position of -1 
 * @author eriklopez
 * 
 *CFG That Doesn't Exist in the Program:
 *D.main(0)->D.main(1)->
 *D.foo(0)-> D.foo(1) ... -> D.foo(6) ->
 *D.bar(0)-> D.bar(EXIT) -> D.main(8) -> D.main(EXIT)
 */

public class GraphGenerator {
	CFG createCFG(String className) throws ClassNotFoundException {
		CFG cfg = new CFG();
		JavaClass jc = Repository.lookupClass(className);
		ClassGen cg = new ClassGen(jc);
		ConstantPoolGen cpg = cg.getConstantPool();
		CFG.Node node = null;
		CFG.Node prevNode = null;
		CFG.Node ifNode=null;
		CFG.Node dummy = null;
		InstructionHandle holder=null;
		Instruction prevInst=null;
		Set<Node> staticInk=null;
		Set<CFG.Node> Entry=null;
		//Queue<InstructionHandle> queue = new LinkedList<InstructionHandle>();
		Map<Integer, Set<Node>> staticNodes = new HashMap<Integer, Set<Node>>();//Set<Node>>();
		Map<Integer, Set<Node>> staticEntry = new HashMap<Integer, Set<Node>>();
		//ArrayList<InstructionHandle> positions = new ArrayList<InstructionHandle>();
		for (org.apache.bcel.classfile.Method m: cg.getMethods()) {
			MethodGen mg = new MethodGen(m, cg.getClassName(), cpg);
			InstructionList il = mg.getInstructionList();
			InstructionHandle[] handles = il.getInstructionHandles();
			dummy = new CFG.Node(-1,m, jc );
			cfg.nodes.add(dummy);
			for (InstructionHandle ih: handles) {
				
				int position = ih.getPosition();
				node = new CFG.Node(position, m, jc);
				cfg.nodes.add(node);
				Instruction inst = ih.getInstruction();
				
				if (inst instanceof INVOKESTATIC){
					staticInk = staticNodes.get(((INVOKESTATIC) inst).getIndex());
					if (staticInk == null) {
					
						staticInk = new  HashSet<CFG.Node>();
						staticNodes.put(((INVOKESTATIC) inst).getIndex()+2, staticInk);
					}
					//System.out.println(staticInk);
					staticInk.add(node);
					//System.out.println(staticInk);
					staticNodes.put(((INVOKESTATIC) inst).getIndex()+2, staticInk );
						
				}	
				if (position == 0){
						Entry = staticEntry.get(m.getNameIndex());
						if (Entry == null) {
							Entry = new HashSet<CFG.Node>();
							staticEntry.put(m.getNameIndex(), Entry);
						}
						Entry.add(node);
						staticEntry.put(m.getNameIndex(), Entry );	
				}
				
				if (inst instanceof BranchInstruction){
						ifNode = node;
						holder =((BranchInstruction) inst).getTarget();
					
					}
					//if holder is not null and holder's position == current position of the inst
					//we found the target node of the branchInst
					if (holder != null && holder.getPosition() == position)
					{
						cfg.addEdge(ifNode.position, position, m, jc);
				
					}
					
					if (prevNode != null){
						//Add edge from previous node to current node if previous node not a returnInst
						if (!(prevInst instanceof INVOKESTATIC)){
							if (!(prevInst instanceof ReturnInstruction) && m == prevNode.method){
								cfg.addEdge(prevNode.position, node.position, m, jc);
							}
							//if CurrentInst is a Return, add edge from it to the dummy end node
							if ((inst instanceof ReturnInstruction) && m == prevNode.method){
								cfg.addEdge(node.position, dummy.position, m, jc);
							}
						}
					}
					
					prevNode = node;
					prevInst = inst;
					
				}
			
			//System.out.println("Static Nodes"+staticNodes);
		//System.out.println("StaticEntry: "+staticEntry);
		}
		
		for (Map.Entry<Integer, Set<Node>> mapStatic : staticNodes.entrySet()){
			
			
			Set<CFG.Node> hold = staticEntry.get(mapStatic.getKey());
			Set<CFG.Node> staticNodeHold = staticNodes.get(mapStatic.getKey());
			
			Iterator<CFG.Node> iterE = hold.iterator(); 
			Iterator<CFG.Node> iterN = staticNodeHold.iterator();
			
			
			CFG.Node holdE = iterE.next();
			CFG.Node holdN = iterN.next();
			while (iterN.hasNext()){	
			cfg.addEdge(holdN.position, holdN.method, holdN.clazz, holdE.position, holdE.method, holdE.clazz);
			iterN.next();
			
			}
		
		}
		
		return cfg;
	}
	
	public static void main(String[] a) throws ClassNotFoundException {
		CFG graph = new GraphGenerator().createCFG("pset2.D"); // example invocation of createCFG
		System.out.println(graph.toString());
		
		
	}
}
