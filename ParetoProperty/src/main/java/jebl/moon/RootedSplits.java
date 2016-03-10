package jebl.moon;

import java.util.HashSet;
import java.util.Set;

import jebl.evolution.taxa.Taxon;

public class RootedSplits {
	private Set<Set<Taxon>> splits;
	
	public RootedSplits() {
		splits = new HashSet<Set<Taxon>>();
	}
	
	public void addSplit(Set<Taxon> split) {
		splits.add(split);
	}
	
	public Set<Set<Taxon>> getSplits() {
		return splits;
	}
	
	public Set<Taxon> getUnion() {
		Set<Taxon> rtn = new HashSet<Taxon>();
		for (Set<Taxon> split : splits) {
			rtn.addAll(split);
		}
		return rtn;
	}
	
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		RootedSplits so = (RootedSplits) o;

		if (!splits.equals(so.splits)) return false;

		return true;
	}
	
	public int hashCode() {
		return splits.hashCode();
	}
	
	public static void main(String[] args) throws Exception {
		RootedSplits A = new RootedSplits();
		RootedSplits B = new RootedSplits();
		Set<Taxon> a = new HashSet<Taxon>();
		a.add(Taxon.getTaxon("a"));
		Set<Taxon> b = new HashSet<Taxon>();
		b.add(Taxon.getTaxon("b"));
		A.addSplit(a);
		A.addSplit(b);
		B.addSplit(a);
		B.addSplit(b);
		Set<RootedSplits> rs = new HashSet<RootedSplits>();
		rs.add(A);
		rs.add(B);
		System.out.println(rs.size());
	}
}
