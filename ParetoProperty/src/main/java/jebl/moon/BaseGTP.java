package jebl.moon;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jebl.evolution.graphs.Node;
import jebl.evolution.taxa.Taxon;
import jebl.evolution.trees.RootedTree;
import jebl.evolution.trees.Utils;

public abstract class BaseGTP {
	private boolean pareto;
	private Set<RootedSplitsCost> minimal;
	private Set<Taxon> leaves;
	private RootedTree solutionTree;
	private Map<Set<Taxon>, Set<RootedSplitsCost>> subOptimals;
	private Map<Set<Taxon>, RootedSplits> solutionClusters;
	private Map<Set<Taxon>, Set<RootedSplits>> instSplits;
	protected Map<RootedSplits, Integer> instSplitCount;	
	abstract protected int auxCost(Set<Taxon> X, Set<Taxon> Y);

	protected Set<RootedSplitsCost> subCost(Set<Taxon> Z) {
		Set<RootedSplitsCost> rtn = null;
		if (Z.size() > 1) {
			if (!subOptimals.containsKey(Z)) {
				subOptimals.put(Z, new HashSet<RootedSplitsCost>());
				Set<Set<Taxon>> pZ = null;
				if (pareto) {
					pZ = SetOps.powerset(Z);
					pZ.remove(Collections.EMPTY_SET);
					pZ.remove(Z);		
				}
				else {
					pZ = new HashSet<Set<Taxon>>();
					Set<RootedSplits> rSet = new HashSet<RootedSplits>(instSplits.get(Z));
					for (RootedSplits rs : rSet) {
						pZ.addAll(rs.getSplits());
					}
				}										
				int costZ, min = Integer.MAX_VALUE;
				Set<RootedSplitsCost> tempOptimals = new HashSet<RootedSplitsCost>();
				while (pZ.size() > 0) {
					Set<Taxon> X = new HashSet<Taxon>(pZ.iterator().next());
					Set<Taxon> Y = new HashSet<Taxon>(Z);
					Y.removeAll(X);
					pZ.remove(X);
					pZ.remove(Y);
					int costX = 0, costY = 0;
					if (subCost(X) != null) {
						costX = subCost(X).iterator().next().getCost();
					}
					if (subCost(Y) != null) {
						costY = subCost(Y).iterator().next().getCost();
					}
					costZ = costX + costY + auxCost(X, Y);				
					if (costZ <= min) {
						min = costZ;
						RootedSplitsCost rsc = new RootedSplitsCost();
						rsc.addSplit(X);
						rsc.addSplit(Y);
						rsc.setCost(costZ);
						tempOptimals.add(rsc);					
					}
				}			
				for (RootedSplitsCost rsc : tempOptimals) {
					if (rsc.getCost() == min) {
						subOptimals.get(Z).add(rsc);
					}
				}
			}
			rtn = subOptimals.get(Z);
		}		// else return null as equivalent to 0
		return rtn;
	}

	public BaseGTP(List<RootedTree> instances) {
		leaves = new HashSet<Taxon>();		
		instSplitCount = new HashMap<RootedSplits, Integer>();
		instSplits = new HashMap<Set<Taxon>, Set<RootedSplits>>();
		subOptimals = new HashMap<Set<Taxon>, Set<RootedSplitsCost>>();
		solutionClusters = new HashMap<Set<Taxon>, RootedSplits>();
		for (RootedTree tree : instances) {
			leaves.addAll(tree.getTaxa());
			addRootedSplits(tree);
		}
	}

	private void addRootedSplits(RootedTree tree) {
		for (Node pa : tree.getInternalNodes()) {
			RootedSplits rs = new RootedSplits();
			for (Node ch : tree.getChildren(pa)) {
				rs.addSplit(MyUtils.getDescendantTaxa(tree, ch));
			}
			if (instSplitCount.containsKey(rs)) {
				instSplitCount.put(rs, instSplitCount.get(rs)+1);
				instSplits.get(rs.getUnion()).add(rs);
			}
			else {
				instSplitCount.put(rs, 1);
				Set<RootedSplits> tmp = new HashSet<RootedSplits>();
				tmp.add(rs);
				instSplits.put(rs.getUnion(), tmp);
			}
		}
	}

	private void setSolutionClusters(Set<Taxon> paCluster) {
		if (paCluster.size() > 1) {
			RootedSplits children = subOptimals.get(paCluster).iterator().next();				
			solutionClusters.put(paCluster, children);
			for (Set<Taxon> chCluster : children.getSplits()) {
				setSolutionClusters(chCluster);
			}
		}
	}

	public void solveDynamicProgram(boolean coPareto) {
		this.pareto = coPareto;
		minimal = subCost(leaves);
		setSolutionClusters(leaves);		
		solutionTree = new RootedTreeFromClusterSplits(solutionClusters, leaves);	
	}

	public RootedTree getSolution() {
		return solutionTree;
	}

	public int getTotalCost() {
		return minimal.iterator().next().getCost();
	}
	
	public void showResults() {
		System.out.println("Species gene tree cost: " + minimal.iterator().next().getCost());
		System.out.println(Utils.asText(solutionTree));
	}
}