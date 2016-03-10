package simulator;

import jebl.evolution.trees.RootedTree;

public abstract class CostMetric {
	abstract protected int getCost(RootedTree t1, RootedTree t2);
	
	public int getCost(RootedTree S, RootedTree[] T) {
		int cost = 0;
		for (RootedTree t : T) {
			cost += this.getCost(S, t);
		}
		return cost;
	}
}
