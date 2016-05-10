package simulator;

import java.util.Set;
import java.util.Map.Entry;

import jebl.evolution.taxa.MissingTaxonException;
import jebl.evolution.taxa.Taxon;
import jebl.evolution.trees.RootedTree;
import jebl.moon.MyUtils;
import jebl.moon.RootedSplits;
import jebl.moon.SetOps;

public class RobinsonFoulds extends GeneTreeParsimony {
	public RobinsonFoulds(RootedTree[] instances) {
		super(instances);
	}

	protected int auxCost(Set<Taxon> X1, Set<Taxon> X2) {
		int cost = 0;
		Set<Taxon> xy = SetOps.Union(X1, X2);
		for (Entry<RootedSplits, Integer> entry : instSplitCount.entrySet()) {		
			Set<Taxon> cl = entry.getKey().getUnion();
			if (!xy.equals(cl) && xy.containsAll(cl) 
					&& SetOps.Cross(cl, X1).size() > 0 && SetOps.Cross(cl, X2).size() > 0) {
				cost += 2 * entry.getValue();
			}
		}
		return cost;
	}

	public static void main(String[] args) throws MissingTaxonException {
		Instance inst = new Instance(2, 3, 20);
		inst.showInstance();
		RobinsonFoulds solver = new RobinsonFoulds(inst.getComProfile());
		solver.runDynamicProgram();
		solver.showResults(false);
		System.out.println("Actual cost: " + MyUtils.getRFCost(inst.getComProfile(), solver.getSolution()));
	}
}