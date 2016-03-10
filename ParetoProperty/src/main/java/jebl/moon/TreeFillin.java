package jebl.moon;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jebl.evolution.graphs.Node;
import jebl.evolution.taxa.MissingTaxonException;
import jebl.evolution.taxa.Taxon;
import jebl.evolution.trees.RootedSubtree;
import jebl.evolution.trees.RootedTree;

public class TreeFillin {
	private RootedTree t, S;
	private MyRootedTree T;
	
	public TreeFillin(RootedTree t, RootedTree S) throws MissingTaxonException {
		this.t = t;
		this.S = S;		
		iLCAFillin();
	}
	
	private void iLCAFillin() throws MissingTaxonException {
		T = new MyRootedTree(S);
		Set<Set<Taxon>> tC = MyUtils.getAllClades(t);		
		RootedTree SLt = MyUtils.setLength(new RootedSubtree(S, t.getTaxa()), 1.0);
		Set<Set<Taxon>> sLtC = MyUtils.getAllClades(SLt);		
		tC.removeAll(sLtC);
		for (Set<Taxon> c : tC) { 
			addILCA(T, c, false);
		}
	}
		
	private void addILCA(MyRootedTree T, Set<Taxon> A, boolean brute) throws MissingTaxonException {		
		Node lcaT = MyUtils.getCommonAncestorNodeTaxa(T, A);		
		Set<Node> chA = new HashSet<Node>();
		for (Taxon a : A) {
			List<Node> path = MyUtils.getPath(T, T.getNode(a), lcaT);
			chA.add(path.get(path.size() - 2));			
		}		 
		T.detachLCA(lcaT, new ArrayList<Node>(chA));
	}
	
	public RootedTree getSrcTree() {
		return t;
	}
	
	public RootedTree getiLCATree() throws MissingTaxonException {
		return T;
	}
	
	public RootedTree getConsensus() {
		return S;
	}
}
