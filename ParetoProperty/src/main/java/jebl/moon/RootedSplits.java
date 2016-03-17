package jebl.moon;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import jebl.evolution.taxa.Taxon;

public class RootedSplits {
	private List<Set<Taxon>> splits;
	
	public RootedSplits() {
		splits = new LinkedList<Set<Taxon>>();
	}
	
	public void addSplit(Set<Taxon> split) {
		splits.add(split);
	}
	
	public List<Set<Taxon>> getSplits() {
		return splits;
	}
	
	public int getSize() {
		return splits.size();
	}
	
	public Set<Taxon> getSplit(int i) {
		return splits.get(i %  splits.size());
	}
	
	public Set<Taxon> getUnion() {
		Set<Taxon> rtn = new HashSet<Taxon>();
		for (Set<Taxon> split : splits) {
			rtn.addAll(split);
		}
		return rtn;
	}
		
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((splits == null) ? 0 : splits.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RootedSplits other = (RootedSplits) obj;
		if (splits == null) {
			if (other.splits != null)
				return false;
		} else if (!splits.equals(other.splits))
			return false;
		return true;
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
