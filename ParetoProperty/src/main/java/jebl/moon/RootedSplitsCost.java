package jebl.moon;

public class RootedSplitsCost extends RootedSplits {
	private int cost;
	
	public RootedSplitsCost() {
		super();
		cost = 0;
	}
	
	public void setCost(int cost) {
		this.cost = cost;
	}
	
	public int getCost() {
		return cost;
	}
}
