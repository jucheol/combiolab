package jebl.moon;

import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import jebl.evolution.taxa.Taxon;
import jebl.evolution.trees.RootedTree;

public class GeneDuplication extends BaseGTP {
	public GeneDuplication(List<RootedTree> instances) {
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
}