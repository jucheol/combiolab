package simulator;

import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import jebl.evolution.taxa.MissingTaxonException;
import jebl.evolution.taxa.Taxon;
import jebl.evolution.trees.RootedTree;
import jebl.moon.MyUtils;
import jebl.moon.RootedSplits;

public class GeneDuplication extends GeneTreeParsimony {
	public GeneDuplication(RootedTree[] instances) {
		super(instances);
	}

	protected int auxCost(Set<Taxon> X, Set<Taxon> Y) {
		int cost = 0;
		Set<Taxon> union = new HashSet<Taxon>(X);
		union.addAll(Y);
		for (Entry<RootedSplits, Integer> entry : instSplitCount.entrySet()) {			
			if (union.containsAll(entry.getKey().getUnion())) {
				for (Set<Taxon> split : entry.getKey().getSplits()) {
					if (!X.containsAll(split) && !Y.containsAll(split)) {
						cost += entry.getValue();
						break;
					}
				}
			}
		}
		return cost;
	}

	public static void main(String[] args) throws MissingTaxonException {
		Instance inst = new Instance(2, 3, 10);
		inst.showInstance();
		GeneDuplication solver = new GeneDuplication(inst.getIncomProfile());
		solver.runDynamicProgram();
		solver.showResults();
		System.out.println("Pareto solution: " + MyUtils.isRefine(inst.getConsensusTree(), solver.getSolution()));
	}
}