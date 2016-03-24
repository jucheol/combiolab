package simulator;

import java.util.Set;

import jebl.evolution.taxa.MissingTaxonException;
import jebl.evolution.taxa.Taxon;
import jebl.evolution.trees.RootedTree;
import jebl.moon.MyUtils;
import jebl.moon.RootedSplits;
import jebl.moon.SetOps;

public class GeneDuplication extends GeneTreeParsimony {
	public GeneDuplication(RootedTree[] instances) {
		super(instances);
	}

	protected int auxCost(Set<Taxon> X1, Set<Taxon> X2) {
		int cost = 0;
		Set<Taxon> union = SetOps.Union(X1, X2);
		for (RootedSplits q : instSplitCount.keySet()) {
			if (union.containsAll(q.getUnion())) {
				for (int i = 0; i < q.getSize(); i++) {
					if (!X1.containsAll(q.getSplit(i)) && !X2.containsAll(q.getSplit(i))) {
						cost += instSplitCount.get(q);
						break;
					}
				}
			}
		}
		return cost;
	}

	public static void main(String[] args) throws MissingTaxonException {
		Instance inst = new Instance(2, 3, 2);
		inst.showInstance();
		GeneDuplication solver = new GeneDuplication(inst.getIncomProfile());
		solver.runDynamicProgram();
		solver.showResults(false);
		System.out.println("Actual cost: " + MyUtils.GeneDuplicationCost(inst.getIncomProfile(), solver.getSolution()));
		System.out.println("Pareto solution: " + MyUtils.isRefine(inst.getConsensusTree(), solver.getSolution()));
	}
}