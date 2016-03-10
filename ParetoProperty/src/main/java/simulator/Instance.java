package simulator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import jebl.evolution.taxa.MissingTaxonException;
import jebl.evolution.taxa.Taxon;
import jebl.evolution.trees.ConsensusTreeBuilder;
import jebl.evolution.trees.RootedSubtree;
import jebl.evolution.trees.RootedTree;
import jebl.evolution.trees.TreeBuilderFactory;
import jebl.evolution.trees.Utils;
import jebl.evolution.trees.TreeBuilderFactory.ConsensusMethod;
import jebl.moon.MyUtils;

public class Instance {
	private double stripProb = 0.2;
	private RootedTree conTree;
	private RootedTree[] comProfile, incomProfile;

	public Instance(int depth, int branch, int profSize) throws MissingTaxonException {
		comProfile = new RootedTree[profSize];
		incomProfile = new RootedTree[profSize];		
		boolean reWork;					
		do {
			reWork = false;
			for (int i = 0; i < profSize; i++) {
				comProfile[i] = MyUtils.genRandomBinaryTree(depth, branch);
			}				
			@SuppressWarnings("unchecked")
			ConsensusTreeBuilder<RootedTree> builder = TreeBuilderFactory.buildRooted(comProfile, 1.0, ConsensusMethod.GREEDY);
			conTree = builder.build();			
			if (conTree.getInternalNodes().size() < (int) (Math.pow(branch, depth) - 1) / (branch - 1)) {
				reWork = true;					
			}				
		} while (reWork);
		conTree = MyUtils.setLength(conTree, 1.0);
		List<Taxon> taxa = new ArrayList<Taxon>(conTree.getTaxa());
		for (int i = 0; i < profSize; i++) {				
			Set<Taxon> subTaxa = new HashSet<Taxon>();								
			for (Taxon t : taxa) {
				if (ThreadLocalRandom.current().nextDouble() > stripProb) {
					subTaxa.add(t);
				}					
			}				
			incomProfile[i] = new RootedSubtree(comProfile[i], subTaxa);
		}
	}
	
	public RootedTree getConsensusTree() {
		return conTree;
	}

	public RootedTree[] getIncomProfile() {
		return incomProfile;
	}
	
	public void showInstance() throws MissingTaxonException {
		System.out.println("==================== Incompelete Profile ====================");
		for (RootedTree tree : incomProfile) System.out.println(Utils.asText(tree));		
		System.out.println("==================== Given Consensus Tree ====================");
		System.out.println(Utils.asText(conTree));
	}
	
	public static void main(String[] args) throws MissingTaxonException {
		Instance test = new Instance(2, 3, 10);
		test.showInstance();
	}
}
