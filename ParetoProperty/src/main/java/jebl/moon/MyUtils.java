package jebl.moon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.Set;

import jebl.moon.GraftedRootedTree;
import jebl.evolution.coalescent.ExponentialGrowth;
import jebl.evolution.graphs.Edge;
import jebl.evolution.graphs.Node;
import jebl.evolution.taxa.MissingTaxonException;
import jebl.evolution.taxa.Taxon;
import jebl.evolution.trees.CladeSystem;
import jebl.evolution.trees.ConsensusTreeBuilder;
import jebl.evolution.trees.RootedSubtree;
import jebl.evolution.trees.RootedTree;
import jebl.evolution.trees.RootedTreeUtils;
import jebl.evolution.trees.TreeBuilderFactory;
import jebl.evolution.trees.TreeBuilderFactory.ConsensusMethod;
import jebl.evolution.trees.Utils;
import jebl.evolution.treesimulation.CoalescentIntervalGenerator;
import jebl.evolution.treesimulation.IntervalGenerator;
import jebl.evolution.treesimulation.TreeSimulator;

public class MyUtils {
	/**
	 * @param taxa 
	 * @return merged name of taxa
	 */
	public static Taxon TaxaSetToSinlge(Set<Taxon> taxa) {
		String rtn = "";
		Set<String> sort = new TreeSet<String>();
		for (Taxon t : taxa) {
			sort.add(t.getName());
		}
		for (String t : sort) {
			rtn += "|" + t;
		}
		rtn = rtn.substring(1);
		return Taxon.getTaxon(rtn);
	}

	/**
	 * @param name 
	 * @return set of taxa from name
	 */
	public static Set<Taxon> TaxaSingleToSet(Taxon taxum) {
		Set<Taxon> rtn = new HashSet<Taxon>();
		for (String t : taxum.getName().split("\\|")) {
			rtn.add(Taxon.getTaxon(t));
		}
		return rtn;
	}

	public static RootedTree mergeSubSolutions(Map<Set<Taxon>, RootedTree> subSolutions) {		
		int max = 0;
		Set<Taxon> rootKey = null;
		for (Set<Taxon> t : subSolutions.keySet()) {
			if (t.size() > max) {
				max = t.size();
				rootKey = t;
			}
		}		
		RootedTree baseTree = subSolutions.get(rootKey);	
		List<Taxon> taxaKey = new LinkedList<Taxon>();

		taxaKey.addAll(baseTree.getTaxa());		
		while (taxaKey.size() > 0) {
			Taxon target = taxaKey.remove(0);
			Set<Taxon> sTarget = TaxaSingleToSet(target);
			if (subSolutions.containsKey(sTarget)) {
				RootedTree subTree = subSolutions.get(sTarget);
				baseTree = new GraftedRootedTree(baseTree, subTree, target);
				taxaKey.addAll(subTree.getTaxa());
			}
		}		
		return baseTree;
	}

	/**
	 * 
	 * @param t1
	 * @param t2
	 * @return check t1 <= t2
	 */
	public static boolean isRefine(RootedTree t1, RootedTree t2) {
		Set<Set<Taxon>> t1C = getAllClades(t1);
		Set<Set<Taxon>> t2C = getAllClades(t2);
		return t2C.containsAll(t1C) ? true : false;
	}

	/**
	 * 
	 * @param t1
	 * @param t2
	 * @return check t1 = t2 | L(t1)
	 */
	public static boolean isDisplay(RootedTree t1, RootedTree t2) {
		RootedTree t2l = new RootedSubtree(t2, t1.getTaxa());
		Set<Set<Taxon>> t1C = getAllClades(t1);
		Set<Set<Taxon>> t2C = getAllClades(t2l);
		return t2C.equals(t1C) ? true : false;
	}

	public static boolean isEqual(RootedTree t1, RootedTree t2) {
		return getAllClades(t1).equals(getAllClades(t2));
	}

	public static RootedTree getCutTree(RootedTree cTree, Node cNode, RootedTree sTree) throws MissingTaxonException {
		if (cTree.isExternal(cNode)) {
			return null;
		}		
		Set<Node> mapChildren = new HashSet<Node>();
		for (Node node : cTree.getChildren(cNode)) {		
			mapChildren.add(MyUtils.LCAMapping(cTree, node, sTree));
		}
		return new RootedSubtreeFromNodes(sTree, mapChildren);		
	}

	public static RootedTree getCutSubTree(RootedTree cTree, Node cNode, RootedTree sTree, RootedTree gTree) throws MissingTaxonException {
		if (cTree.isExternal(cNode)) {
			return null;
		}		
		Set<Node> mapChildren = new HashSet<Node>();
		for (Node node : cTree.getChildren(cNode)) {		
			mapChildren.add(MyUtils.LCAMapping(cTree, node, sTree));			
		}
		MyRootedTree cutTree = new MyRootedTree(new RootedSubtreeFromNodes(sTree, mapChildren));
		cutTree.setLength(1.0);
		Set<Taxon> remain = new HashSet<Taxon>();
		for (Taxon taxum : cutTree.getTaxa()) {
			Set<Taxon> taxa = MyUtils.TaxaSingleToSet(taxum);
			taxa.retainAll(gTree.getTaxa());
			if (taxa.size() > 0) remain.add(taxum);
		}		
		if (remain.size() > 0) return new RootedSubtree(cutTree, remain);
		else return null;
	}	

	public static int getMaxOutDegree(RootedTree tree) {
		int max = -1;
		for (Node node : tree.getInternalNodes()) {
			int outDegree;
			if (tree.isRoot(node)) outDegree = node.getDegree();
			else outDegree = node.getDegree() - 1;
			if (outDegree > max) max = outDegree;
		}
		return max;
	}

	public static Set<Set<Taxon>> getStrictConsensusClades(RootedTree[] trees) {
		Set<Set<Taxon>> clades = new HashSet<Set<Taxon>>(getAllClades(trees[0]));
		for (int i = 1; i < trees.length; i++) {
			clades.retainAll(MyUtils.getAllClades(trees[i]));
		}
		return clades;
	}

	public static RootedTree genRandomBinary(String prefix, int numTaxa) {		
		ExponentialGrowth exponentialGrowth = new ExponentialGrowth();
		exponentialGrowth.setN0(10);
		exponentialGrowth.setGrowthRate(0.1);
		IntervalGenerator intervals = new CoalescentIntervalGenerator(exponentialGrowth);
		TreeSimulator sim = new TreeSimulator(prefix, numTaxa);
		return new MyRootedTree(sim.simulate(intervals, true));		
	}

	public static RootedTree genRandomBinaryTree(int depth, int branch) {
		RootedTree tree = null;
		for (int i = 0; i < depth; i++) {
			tree = growTree(tree, branch);
		}
		return tree;
	}

	private static RootedTree growTree(RootedTree tree, int branch) {
		if (tree == null) {
			return genRandomBinary("L", branch);
		}
		Set<Taxon> taxa = tree.getTaxa();
		for (Taxon taxum : taxa) {
			RootedTree subtree = genRandomBinary(taxum.toString(), branch);
			tree = new GraftedRootedTree(tree, subtree, taxum);
			tree = setLength(tree, 1.0);			
		}
		return tree;
	}

	public static Set<Taxon> getDescendantTaxa(RootedTree tree, Node node) {		
		Set<Taxon> tipTaxa = new HashSet<Taxon>();
		if (tree.isExternal(node)) {
			tipTaxa.add(tree.getTaxon(node));
		}
		else {
			for (Node tip : RootedTreeUtils.getDescendantTips(tree, node)) {
				tipTaxa.add(tree.getTaxon(tip));
			}
		}
		return tipTaxa;
	}

	public static Node LCAMapping(RootedTree src, Node target, RootedTree dst) throws MissingTaxonException {
		Set<Taxon> taxa = getDescendantTaxa(src, target);
		taxa.retainAll(dst.getTaxa());
		return getCommonAncestorNodeTaxa(dst, taxa);
	}

	public static List<Node> LCAMappingPath(RootedTree src, List<Node> path, RootedTree dst) throws MissingTaxonException {
		List<Node> rPath = new LinkedList<Node>();
		for (Node target : path) rPath.add(LCAMapping(src, target, dst));
		return rPath;
	}

	public static int GeneDuplicationCost(RootedTree gTree, RootedTree sTree) throws MissingTaxonException {
		int cost = 0;
		for (Node pa : gTree.getInternalNodes()) {			
			Node paMap = LCAMapping(gTree, pa, sTree);
			for (Node ch : gTree.getChildren(pa)) {
				Node chMap = LCAMapping(gTree, ch, sTree);
				if (paMap.equals(chMap)) {
					cost++;
					break;
				}
			}
		}
		return cost;
	}

	public static int GeneDuplicationCost(RootedTree[] gTrees, RootedTree sTree) throws MissingTaxonException {
		int rtn = 0;
		for (RootedTree gTree : gTrees) {
			rtn += GeneDuplicationCost(gTree, sTree);
		}
		return rtn;
	}

	public static Set<Node> getNodes(RootedTree t, Set<Taxon> taxa) {
		Set<Node> rtn = new HashSet<Node>();
		for (Taxon leaf : taxa) {
			rtn.add(t.getNode(leaf));
		}
		return rtn;
	}

	public static int LossesCost(RootedTree gTree, RootedTree sTree) throws MissingTaxonException {
		int cost = 0;
		RootedTree sTree1 = new RootedSubtreeFromNodes(sTree, getNodes(sTree, gTree.getTaxa()));
		for (Node pa : gTree.getInternalNodes()) {			
			Node paMap = LCAMapping(gTree, pa, sTree1);
			boolean isSkip = false;
			for (Node ch : gTree.getChildren(pa)) {
				Node chMap = LCAMapping(gTree, ch, sTree1);
				if (paMap.equals(chMap)) {
					isSkip = true;
					break;
				}
			}
			if (!isSkip) {
				for (Node ch : gTree.getChildren(pa)) {
					Node chMap = LCAMapping(gTree, ch, sTree1);
					cost += Math.abs(MyUtils.getPathLength(sTree1, chMap, paMap) - 1);
				}
			}
		}
		return cost;
	}

	public static int LossesCost(RootedTree[] gTrees, RootedTree sTree) throws MissingTaxonException {
		int rtn = 0;
		for (RootedTree gTree : gTrees) {
			rtn += LossesCost(gTree, sTree);
		}
		return rtn;
	}

	public static List<Node> getPath(RootedTree tree, Node from, Node to) {
		List<Node> path = null;
		List<Node> FtoRoot = new LinkedList<Node>();
		List<Node> TtoRoot = new LinkedList<Node>();
		Node tmp = from;
		while (tmp != null) {
			FtoRoot.add(0, tmp);
			tmp = tree.getParent(tmp);
		}
		tmp = to;
		while (tmp != null) {
			TtoRoot.add(0, tmp);
			tmp = tree.getParent(tmp);
		}
		Node lca = null;
		int lcaPos = 0;
		while (lcaPos < FtoRoot.size() && lcaPos < TtoRoot.size() && FtoRoot.get(lcaPos).equals(TtoRoot.get(lcaPos))) {
			lca = FtoRoot.get(lcaPos++);
		}		
		lcaPos--;
		if (lca.equals(to)) {
			path = FtoRoot.subList(lcaPos, FtoRoot.size());
			Collections.reverse(path);
		}
		else if (lca.equals(from)) {
			path = TtoRoot.subList(lcaPos, TtoRoot.size());
		}
		else {
			path = FtoRoot.subList(lcaPos, FtoRoot.size());
			Collections.reverse(path);
			path.addAll(TtoRoot.subList(lcaPos + 1, TtoRoot.size()));
		}
		return path;
	}

	public static int getPathLength(RootedTree tree, Node from, Node to) {
		return getPath(tree, from, to).size() - 1;
	}

	public static Node getCommonAncestorNodeTaxa(RootedTree T, Set<Taxon> A) throws MissingTaxonException {
		Set<Node> Anodes = new HashSet<Node>(RootedTreeUtils.getTipsForTaxa(T, A));		
		return RootedTreeUtils.getCommonAncestorNode(T, Anodes);
	}

	public static Set<Set<Taxon>> getAllClades(RootedTree T) {
		Set<Set<Taxon>> clades = new HashSet<Set<Taxon>>();
		CladeSystem C = new CladeSystem();
		C.add(T);
		for (int i = 0; i < C.getCladeCount(); i++) {
			clades.add(C.getClade(i));
		}
		return clades;
	}

	public static RootedTree setLength(RootedTree T, double len) {
		MyRootedTree tree = new MyRootedTree(T);
		tree.setLength(len);
		return tree;
	}

	public static MyRootedTree addILCA(MyRootedTree T, Set<Taxon> A) throws MissingTaxonException {
		MyRootedTree rT = new MyRootedTree(T);		
		Node lca = getCommonAncestorNodeTaxa(rT, A);
		Set<Node> chA = new HashSet<Node>();
		for (Taxon a : A) {
			List<Node> path = getPath(rT, rT.getNode(a), lca);
			chA.add(path.get(path.size() - 2));			
		}		
		if (chA.size() < rT.getChildren(lca).size()) {
			rT.detachLCA(lca, new ArrayList<Node>(chA));
		}		
		return rT;
	}

	public static RootedTree iLCAFillin(RootedTree t, RootedTree S) throws MissingTaxonException {
		MyRootedTree T = new MyRootedTree(S);
		Set<Set<Taxon>> tC = getAllClades(t);
		RootedTree SLt = setLength(new RootedSubtree(S, t.getTaxa()), 1.0);
		Set<Set<Taxon>> sLtC = getAllClades(SLt);
		tC.removeAll(sLtC);
		for (Set<Taxon> c : tC) T = addILCA(T, c);
		T.setLength(1.0);
		return T;
	}

	public static Set<Taxon> getCrossTaxa(RootedTree T1, RootedTree T2) {
		Set<Taxon> cap = new HashSet<Taxon>(T1.getTaxa());
		cap.retainAll(T2.getTaxa());		
		return cap;
	}

	public static RootedTree getMinusStrictCon(RootedTree T1, RootedTree T2) {
		Set<Taxon> X = getCrossTaxa(T1, T2);		
		RootedTree[] array = { setLength(new RootedSubtree(T1, X), 1.0), setLength(new RootedSubtree(T2, X), 1.0) };
		@SuppressWarnings("unchecked")
		ConsensusTreeBuilder<RootedTree> builder = TreeBuilderFactory.buildRooted(array, 1.0, ConsensusMethod.GREEDY);
		return builder.build();
	}

	public static RootedTree getMaximalRefined(RootedTree T, RootedTree B) throws MissingTaxonException {
		RootedTree TX = setLength(new RootedSubtree(T, B.getTaxa()), 1.0);
		if (RootedTreeUtils.equal(TX, B)) return T;
		else {
			MyRootedTree rT = new MyRootedTree(T);
			Set<Set<Taxon>> violate = getAllClades(TX);
			violate.removeAll(getAllClades(B));
			for (Set<Taxon> clade : violate) {
				Node lca = MyUtils.getCommonAncestorNodeTaxa(rT, clade);
				rT.removeInternalNode(lca);
			}
			return rT;
		}
	}	

	public static List<Node> getEdgeMapping(RootedTree src, Edge e, RootedTree dst) throws MissingTaxonException {
		Node[] incs = src.getNodes(e);
		Node lcaCh = LCAMapping(src, incs[0], dst);
		Node lcaPa = LCAMapping(src, incs[1], dst);
		List<Node> path = getPath(dst, lcaCh, lcaPa);		
		return path;		
	}

	public static Edge getLCAEdgeMapping(RootedTree src, Edge e, RootedTree dst) throws MissingTaxonException, Exception {
		Node[] incs = src.getNodes(e);
		Node lcaCh = LCAMapping(src, incs[0], dst);
		Node lcaPa = LCAMapping(src, incs[1], dst);
		return dst.getEdge(lcaCh, lcaPa);
	}

	public static RootedTree getStrictConMerger(List<RootedTree> P) throws MissingTaxonException, Exception {
		LinkedList<RootedTree> input = new LinkedList<RootedTree>(P);
		while (input.size() > 2) {
			int maxI = 0, maxJ = input.size() - 1, maxTaxa = 0;
			for (int i = 0; i < input.size(); i++) {
				for (int j = 0; j < input.size(); j++) {
					if (i != j) {						
						int numTaxa = getCrossTaxa(input.get(i), input.get(j)).size();
						if (numTaxa > maxTaxa) {
							maxTaxa = numTaxa;
							maxI = i;
							maxJ = j;
						}
					}
				}
			}
			RootedTree SCM = getStrictConMerger(input.get(maxI), input.get(maxJ));
			if (SCM != null) {
				input.add(SCM);
				input.remove(maxI);
				input.remove(maxJ);
			}
			else return null;
		}
		return getStrictConMerger(input.getFirst(), input.getLast());
	}

	public static RootedTree getStrictConMerger(RootedTree T1, RootedTree T2) throws MissingTaxonException, Exception {		
		Set<Taxon> X = getCrossTaxa(T1, T2);
		if (X.size() < 3) {			
			return null;
		}
		else {
			MyRootedTree TSC = new MyRootedTree(getMinusStrictCon(T1, T2));			
			RootedTree T1_ = getMaximalRefined(T1, TSC);
			RootedTree T2_ = getMaximalRefined(T2, TSC);
			MyRootedTree T1__ = new MyRootedTree(T1_);
			MyRootedTree T2__ = new MyRootedTree(T2_);
			Map<Edge, Node[]> bothMap = new HashMap<Edge, Node[]>();
			Map<Edge, List<Node>> T1Map = new HashMap<Edge, List<Node>>();
			Map<Edge, List<Node>> T2Map = new HashMap<Edge, List<Node>>();
			for (Edge edge : TSC.getEdges()) {
				List<Node> path1 = getEdgeMapping(TSC, edge, T1__);
				List<Node> path2 = getEdgeMapping(TSC, edge, T2__);
				if (path1.size() > 2 && path2.size() > 2) {					
					Node[] pair = { path1.get(path1.size()-2), path2.get(path2.size()-2) };
					bothMap.put(edge, pair);
					for (int i = 1; i < path1.size()-2; i++) T1__.removeInternalNode(path1.get(i));					
					for (int i = 1; i < path2.size()-2; i++) T2__.removeInternalNode(path2.get(i));
				}
				else if (path1.size() > 2) {					
					T1Map.put(edge, path1);
				}
				else if (path2.size() > 2) {
					T2Map.put(edge, path2);
				}				
			}			
			for (Entry<Edge, Node[]> entry : bothMap.entrySet()) {
				Set<Taxon> t1Leaf = getMinusTaxa(MyUtils.getDescendantTaxa(T1__, entry.getValue()[0]), TSC.getTaxa());				
				Set<Taxon> t2Leaf = getMinusTaxa(MyUtils.getDescendantTaxa(T2__, entry.getValue()[1]), TSC.getTaxa());				
				if (t1Leaf.size() > 0) TSC.subDivideAndAttachSubtree(entry.getKey(), new RootedSubtree(T1__, t1Leaf), getAllClades(T1_).contains(t1Leaf));
				if (t2Leaf.size() > 0) TSC.subDivideAndAttachSubtree(entry.getKey(), new RootedSubtree(T2__, t2Leaf), getAllClades(T2_).contains(t2Leaf));						
			}						
			for (Entry<Edge, List<Node>> entry : T1Map.entrySet()) {				
				TSC.subDivideAndAttachSubtrees(entry.getKey(), T1__, entry.getValue(), TSC.getTaxa());			
			}
			for (Entry<Edge, List<Node>> entry : T2Map.entrySet()) {
				TSC.subDivideAndAttachSubtrees(entry.getKey(), T2__, entry.getValue(), TSC.getTaxa());
			}
			Set<Taxon> minus1, minus2;
			minus1 = getMinusTaxa(T1_, TSC);
			if (minus1.size() > 1) {
				RootedTree subT = getMinusSubtree(T1_, TSC);				
				TSC.addToRoot(subT, getAllClades(T1_).contains(subT.getTaxa()));
			}
			else if (minus1.size() == 1) {
				TSC.addToRoot(minus1.iterator().next());
			}
			minus2 = getMinusTaxa(T2_, TSC);
			if (minus2.size() > 1) {
				RootedTree subT = getMinusSubtree(T2_, TSC);
				TSC.addToRoot(subT, getAllClades(T2_).contains(subT.getTaxa()));
			}
			else if (minus2.size() == 1) {
				TSC.addToRoot(minus2.iterator().next());			
			}
			return TSC;
		}
	}

	public static Set<Taxon> getMinusTaxa(Set<Taxon> base, Set<Taxon> minus) {
		Set<Taxon> leaf = new HashSet<Taxon>(base);
		leaf.removeAll(minus);
		return leaf;
	}

	public static Set<Taxon> getMinusTaxa(RootedTree base, RootedTree minus) {		
		return getMinusTaxa(base.getTaxa(), minus.getTaxa());
	}

	public static RootedTree getMinusSubtree(RootedTree base, RootedTree minus) {
		Set<Taxon> leaf = getMinusTaxa(base, minus);		
		if (leaf.size() > 0) {			
			MyRootedTree tree = new MyRootedTree(new RootedSubtree(base, leaf));
			tree.setLength(1.0);
			return tree;
		}
		else return null;
	}
}