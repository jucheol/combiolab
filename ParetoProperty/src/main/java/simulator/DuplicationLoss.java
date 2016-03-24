package simulator;

import java.util.Set;

import jebl.evolution.taxa.MissingTaxonException;
import jebl.evolution.taxa.Taxon;
import jebl.evolution.trees.RootedTree;
import jebl.moon.MyUtils;
import jebl.moon.RootedSplits;
import jebl.moon.SetOps;

public class DuplicationLoss extends GeneTreeParsimony {
	public DuplicationLoss(RootedTree[] instances) {
		super(instances);
	}

	protected int auxCost(Set<Taxon> X1, Set<Taxon> X2) {
		int cost = 0;
		Set<Taxon> union = SetOps.Union(X1, X2);
		for (RootedSplits q : instSplitCount.keySet()) {
			boolean abort = false;
			for (int i = 0; i < q.getSize() && !abort; i++) {
				for (int j = 0; j < q.getSize() && !abort; j++) {
					Set<Taxon> Xj = (j == 0) ? X1 : X2;
					boolean c1 = (Xj.containsAll(q.getSplit(i)) && (union.containsAll(q.getSplit(i + 1))));
					boolean c2 = (SetOps.Cross(q.getSplit(i + 1), X1).size() > 0) 
							&& (SetOps.Cross(q.getSplit(i + 1), X2).size() > 0);
					if (!c1 || c2) {
						cost += instSplitCount.get(q);						
						abort = true;
					}
				}
			}
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
		for (int i = 0; i < 100000; i++) {
			Instance inst = new Instance(2, 3, 20);
			//		inst.showInstance();
			DuplicationLoss solver = new DuplicationLoss(inst.getComProfile());
			solver.runDynamicProgram();
			//		solver.showResults();
			//		System.out.println("Actual cost: " + (MyUtils.LossesCost(inst.getIncomProfile(), solver.getSolution())
			//			+ MyUtils.GeneDuplicationCost(inst.getIncomProfile(), solver.getSolution())));
			System.out.println("Pareto solution: " + MyUtils.isRefine(inst.getConsensusTree(), solver.getSolution()));
		}
	}
}