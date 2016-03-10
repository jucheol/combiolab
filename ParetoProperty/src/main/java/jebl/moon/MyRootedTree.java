package jebl.moon;

import jebl.evolution.graphs.Edge;
import jebl.evolution.graphs.Node;
import jebl.evolution.taxa.Taxon;
import jebl.evolution.trees.BaseEdge;
import jebl.evolution.trees.BaseNode;
import jebl.evolution.trees.RootedSubtree;
import jebl.evolution.trees.RootedTree;
import jebl.evolution.trees.Tree;
import jebl.util.AttributableHelper;

import java.util.*;

/**
 * A simple rooted tree providing some ability to manipulate the tree.
 *
 *   - Root an unrooted tree using an outgroup.
 *   - Remove internal node: all children of node are adopted by it's parent.
 *   - Split/Refine node by creating two new children and distributing the children to new nodes.
 *   - Re-root a rooted tree given an outgroup.

 * @author Joseph Heled
 * @version $Id: MutableRootedTree.java 935 2008-07-22 16:52:04Z rambaut $
 *
 */

public class MyRootedTree implements RootedTree {
	MyRootedTree() {  super(); }

	/**
	 * Construct a rooted tree from unrooted.
	 *
	 * @param tree      Unrooted tree to root
	 * @param outGroup  Node in tree assumed to be the outgroup
	 */
	public MyRootedTree(Tree tree, Node outGroup) {
		if( ! tree.isExternal(outGroup) ) throw new IllegalArgumentException("Outgroup must be a tip");

		// Adjacency of node to become new root.
		Node root = tree.getAdjacencies(outGroup).get(0);

		try {
			MyRootedNode newSubtreeRoot = rootAdjacenciesWith(tree, root, outGroup);


			// Add the outgroup in
			MyRootedNode out = (MyRootedNode)createExternalNode( tree.getTaxon(outGroup) );
			setLength(out, tree.getEdgeLength(outGroup, root));
			// Create new root
			ArrayList<MyRootedNode> rootChildren = new ArrayList<MyRootedNode>();
			rootChildren.add(out);
			rootChildren.add(newSubtreeRoot);
			//MutableRootedNode newRoot =
			this.createInternalNode( rootChildren );
			setLength(newSubtreeRoot,0);
		} catch (NoEdgeException e) {
			// bug
		}
	}

	/**
	 * Construct a rooted tree from an immutable rooted tree.
	 *
	 * @param tree      Rooted tree to copy
	 */
	public MyRootedTree(RootedTree tree) {
		try {
			rootNode = rootAdjacenciesWith(tree, tree.getRootNode(), null);
		} catch (NoEdgeException e) {
			// bug
		}
	}

	/**
	 *  Remove internal node. Move all children to their grandparent.
	 *  @param node  to be removed
	 */
	public void removeInternalNode(Node node) {
		assert ! isExternal(node) && getRootNode() != node;

		MyRootedNode parent = (MyRootedNode)getParent(node);
		for( Node n : getChildren(node) ) {
			parent.addChild((MyRootedNode)n);
		}
		parent.removeChild(node);
		internalNodes.remove(node);
	}

	/**
	 *  Insert a child node.
	 *  @param child to be added
	 *  @param parent into which it should be added
	 */
	public void addChild(Node child, Node parent) {
		((MyRootedNode)parent).addChild((MyRootedNode)child);
	}

	/**
	 *  Remove a child node.
	 *  @param child to be removed
	 *  @param parent from which it should be removed
	 */
	public void removeChild(Node child, Node parent) {
		((MyRootedNode)parent).removeChild((MyRootedNode)child);
	}

	/**
	 *  Set the root node.
	 *  @param root the new root node
	 */
	public void setRoot(Node root) {
		this.rootNode = (MyRootedNode)root;
	}

	/**
	 *
	 * @param node     Node to refine
	 * @param leftSet  indices of children in the left new subtree.
	 */
	public void refineNode(Node node, int[] leftSet) {
		List<Node> allChildren = getChildren(node);

		List<Node> left = new ArrayList<Node>();
		List<Node> right = new ArrayList<Node>();

		for( int n : leftSet ) {
			left.add(allChildren.get(n));
		}
		for( Node n : allChildren ) {
			if( !left.contains(n) ) {
				right.add(n);
			}
		}
		internalNodes.remove(node);
		MyRootedNode saveRoot = rootNode;

		MyRootedNode lnode = (left.size() > 1) ? createInternalNode(left) : (MyRootedNode)left.get(0);
		MyRootedNode rnode = (right.size() > 1) ? createInternalNode(right) : (MyRootedNode)right.get(0);

		List<MyRootedNode> nodes = new ArrayList<MyRootedNode>(2);
		nodes.add(lnode);
		nodes.add(rnode);
		((MyRootedNode)node).replaceChildren(nodes);

		rootNode = saveRoot;
	}

	/**
	 *  Re-root tree using an outgroup.
	 * @param outGroup
	 * @param attributeNames Move those attributes (if they exist in node) to their previous parent. The idea is to
	 * preserve "branch" attributes which we now store in the child since only "node" properties are supported.
	 */
	public void reRootWithOutgroup(Node outGroup, Set<String> attributeNames) {
		assert isExternal(outGroup);
		reRoot((MyRootedNode)getAdjacencies(outGroup).get(0), attributeNames);
	}

	/**
	 * Construct a rooted sub-tree from unrooted. Done recursivly: Given an internal node N and one adjacency A to become
	 * the new parent, recursivly create subtrees for all adjacencies of N (ommiting A) using N as parent, and return
	 * an internal node with all subtrees as children. A tip simply creates an external node and returns it.
	 *
	 * @param tree   Unrooted source tree
	 * @param node   span sub-tree from this node
	 * @param parent adjacency of node which serves as the parent.
	 * @return  rooted subtree.
	 * @throws NoEdgeException
	 */
	private MyRootedNode rootAdjacenciesWith(Tree tree, Node node, Node parent) throws NoEdgeException {
		if( tree.isExternal(node) ) {
			return (MyRootedNode)createExternalNode( tree.getTaxon(node) );
		}

		List<Node> children = new ArrayList<Node>();
		for( Node adj : tree.getAdjacencies(node) ) {
			if( adj == parent ) continue;
			MyRootedNode rootedAdj = rootAdjacenciesWith(tree, adj, node);
			setLength(rootedAdj, tree.getEdgeLength(adj, node));
			children.add(rootedAdj);
		}
		return createInternalNode(children);
	}

	/**
	 * Similar to  rootAdjaceincesWith.
	 * @param node
	 * @param attributeNames
	 */
	private void reRoot(MyRootedNode node, Set<String> attributeNames) {
		MyRootedNode parent = (MyRootedNode)getParent(node);
		if( parent == null) {
			return;
		}
		double len = getLength(node);
		parent.removeChild(node);
		reRoot(parent, attributeNames);
		if( parent == getRootNode() ) {
			rootNode = node;
		}

		if( parent.getChildren().size() == 1 ) {
			parent = (MyRootedNode)parent.getChildren().get(0);
			len += parent.getLength();
		}

		node.addChild(parent);
		parent.setLength(len);
		node.setParent(null);

		if( attributeNames != null ) {
			for( String name : attributeNames ) {
				Object s = node.getAttribute(name);
				if( s != null ) {
					parent.setAttribute(name, s);
					node.removeAttribute(name);
				}
			}
		}
	}

	/**
	 * Creates a new external node with the given taxon. See createInternalNode
	 * for a description of how to use these methods.
	 * @param taxon the taxon associated with this node
	 * @return the created node reference
	 */
	public Node createExternalNode(Taxon taxon) {
		MyRootedNode node = new MyRootedNode(taxon);
		externalNodes.put(taxon, node);
		return node;
	}

	/**
	 * Once a SimpleRootedTree has been created, the node stucture can be created by
	 * calling createExternalNode and createInternalNode. First of all createExternalNode
	 * is called giving Taxon objects for the external nodes. Then these are put into
	 * sets and passed to createInternalNode to create a parent of these nodes. The
	 * last node created using createInternalNode is automatically the root so when
	 * all the nodes are created, the tree is complete.
	 *
	 * @param children the child nodes of this nodes
	 * @return the created node reference
	 */
	public MyRootedNode createInternalNode(List<? extends Node> children) {
		MyRootedNode node = new MyRootedNode(children);

		for (Node child : children) {
			((MyRootedNode)child).setParent(node);
		}

		internalNodes.add(node);

		rootNode = node;
		return node;
	}


	/**
	 * @param node the node whose height is being set
	 * @param height the height
	 */
	public void setHeight(Node node, double height) {
		lengthsKnown = false;
		heightsKnown = true;

		// If a single height of a single node is set then
		// assume that all nodes have heights and by extension,
		// branch lengths as well as these will be calculated
		// from the heights
		hasLengths = true;
		hasHeights = true;

		((MyRootedNode)node).setHeight(height);
	}

	/**
	 * @param node the node whose branch length (to its parent) is being set
	 * @param length the length
	 */
	public void setLength(Node node, double length) {
		heightsKnown = false;
		lengthsKnown = true;

		// If a single length of a single branch is set then
		// assume that all branch have lengths and by extension,
		// node heights as well as these will be calculated
		// from the lengths
		hasLengths = true;
		hasHeights = true;

		((MyRootedNode)node).setLength(length);
	}

	/**
	 * @param node the node whose children are being requested.
	 * @return the list of nodes that are the children of the given node.
	 *         The list may be empty for a terminal node (a tip).
	 */
	public List<Node> getChildren(Node node) {
		return new ArrayList<Node>(((MyRootedNode)node).getChildren());
	}

	/**
	 * @return Whether this tree has node heights available
	 */
	public boolean hasHeights() {
		return hasHeights;
	}

	/**
	 * @param node the node whose height is being requested.
	 * @return the height of the given node. The height will be
	 *         less than the parent's height and greater than it children's heights.
	 */
	public double getHeight(Node node) {
		if (!hasHeights) throw new IllegalArgumentException("This tree has no node heights");
		if (!heightsKnown) calculateNodeHeights();
		return ((MyRootedNode)node).getHeight();
	}

	/**
	 * @return Whether this tree has branch lengths available
	 */
	public boolean hasLengths() {
		return hasLengths;
	}

	/**
	 * @param node the node whose branch length (to its parent) is being requested.
	 * @return the length of the branch to the parent node (0.0 if the node is the root).
	 */
	public double getLength(Node node) {
		if (!hasLengths) throw new IllegalArgumentException("This tree has no branch lengths");
		if (!lengthsKnown) calculateBranchLengths();
		return ((MyRootedNode)node).getLength();
	}

	/**
	 * @param node the node whose parent is requested
	 * @return the parent node of the given node, or null
	 *         if the node is the root node.
	 */
	public Node getParent(Node node) {
		return ((MyRootedNode)node).getParent();
	}

	public Edge getParentEdge(Node node) {
		return ((MyRootedNode)node).getEdge();
	}
	/**
	 * The root of the tree has the largest node height of
	 * all nodes in the tree.
	 *
	 * @return the root of the tree.
	 */
	public Node getRootNode() {
		return rootNode;
	}

	public boolean isRoot(Node node) {
		return node == rootNode;
	}

	/**
	 * @return a set of all nodes that have degree 1.
	 *         These nodes are often refered to as 'tips'.
	 */
	public Set<Node> getExternalNodes() {
		return new LinkedHashSet<Node>(externalNodes.values());
	}

	/**
	 * @return a set of all nodes that have degree 2 or more.
	 *         These nodes are often refered to as internal nodes.
	 */
	public Set<Node> getInternalNodes() {
		return new LinkedHashSet<Node>(internalNodes);
	}

	/**
	 * @return the set of taxa associated with the external
	 *         nodes of this tree. The size of this set should be the
	 *         same as the size of the external nodes set.
	 */
	public Set<Taxon> getTaxa() {
		return new LinkedHashSet<Taxon>(externalNodes.keySet());
	}

	/**
	 * @param node the node whose associated taxon is being requested.
	 * @return the taxon object associated with the given node, or null
	 *         if the node is an internal node.
	 */
	public Taxon getTaxon(Node node) {
		return ((MyRootedNode)node).getTaxon();
	}

	/**
	 * @param node the node
	 * @return true if the node is of degree 1.
	 */
	public boolean isExternal(Node node) {
		return ((MyRootedNode)node).getChildren().size() == 0;
	}

	/**
	 * @param taxon the taxon
	 * @return the external node associated with the given taxon, or null
	 *         if the taxon is not a member of the taxa set associated with this tree.
	 */
	public Node getNode(Taxon taxon) {
		return externalNodes.get(taxon);
	}

	public void renameTaxa(Taxon from, Taxon to) {
		MyRootedNode node = (MyRootedNode)externalNodes.get(from);
		node.setTaxa(to);

		externalNodes.remove(from);
		externalNodes.put(to, node);
	}

	/**
	 * Returns a list of edges connected to this node
	 *
	 * @param node
	 * @return the set of nodes that are attached by edges to the given node.
	 */
	public List<Edge> getEdges(Node node) {
		List<Edge> edges = new ArrayList<Edge>();
		for (Node adjNode : getAdjacencies(node)) {
			edges.add(((MyRootedNode)adjNode).getEdge());

		}
		return edges;
	}

	/**
	 * Returns an array of 2 nodes which are the nodes at either end of the edge.
	 *
	 * @param edge
	 * @return an array of 2 edges
	 */
	public Node[] getNodes(Edge edge) {
		for (Node node : getNodes()) {
			if (((MyRootedNode)node).getEdge() == edge) {
				return new Node[] { node, ((MyRootedNode)node).getParent() };
			}
		}
		return null;
	}


	/**
	 * @param node
	 * @return the set of nodes that are attached by edges to the given node.
	 */
	public List<Node> getAdjacencies(Node node) {
		return ((MyRootedNode)node).getAdjacencies();
	}

	/**
	 * Returns the Edge that connects these two nodes
	 *
	 * @param node1
	 * @param node2
	 * @return the edge object.
	 * @throws jebl.evolution.graphs.Graph.NoEdgeException
	 *          if the nodes are not directly connected by an edge.
	 */
	public Edge getEdge(Node node1, Node node2) throws NoEdgeException {
		if (((MyRootedNode)node1).getParent() == node2) {
			return ((MyRootedNode)node1).getEdge();
		} else if (((MyRootedNode)node2).getParent() == node1) {
			return ((MyRootedNode)node2).getEdge();
		} else {
			throw new NoEdgeException();
		}
	}

	/**
	 * @param node1
	 * @param node2
	 * @return the length of the edge connecting node1 and node2.
	 * @throws jebl.evolution.graphs.Graph.NoEdgeException
	 *          if the nodes are not directly connected by an edge.
	 */
	public double getEdgeLength(Node node1, Node node2) throws NoEdgeException {
		if (((MyRootedNode)node1).getParent() == node2) {
			if (heightsKnown) {
				return ((MyRootedNode)node2).getHeight() - ((MyRootedNode)node1).getHeight();
			} else {
				return ((MyRootedNode)node1).getLength();
			}
		} else if (((MyRootedNode)node2).getParent() == node1) {
			if (heightsKnown) {
				return ((MyRootedNode)node1).getHeight() - ((MyRootedNode)node2).getHeight();
			} else {
				return ((MyRootedNode)node2).getLength();
			}
		} else {
			throw new NoEdgeException();
		}
	}

	/**
	 * @return the set of all nodes in this graph.
	 */
	public Set<Node> getNodes() {
		Set<Node> nodes = new LinkedHashSet<Node>(internalNodes);
		nodes.addAll(externalNodes.values());
		return nodes;
	}

	/**
	 * @return the set of all edges in this graph.
	 */
	public Set<Edge> getEdges() {
		Set<Edge> edges = new LinkedHashSet<Edge>();
		for (Node node : getNodes()) {
			if (node != getRootNode()) {
				edges.add(((MyRootedNode)node).getEdge());
			}

		}
		return edges;
	}

	/**
	 * The set of external edges. This is a pretty inefficient implementation because
	 * a new set is constructed each time this is called.
	 * @return the set of external edges.
	 */
	public Set<Edge> getExternalEdges() {
		Set<Edge> edges = new LinkedHashSet<Edge>();
		for (Node node : getExternalNodes()) {
			edges.add(((MyRootedNode)node).getEdge());
		}
		return edges;
	}

	/**
	 * The set of internal edges. This is a pretty inefficient implementation because
	 * a new set is constructed each time this is called.
	 * @return the set of internal edges.
	 */
	public Set<Edge> getInternalEdges() {
		Set<Edge> edges = new LinkedHashSet<Edge>();
		for (Node node : getInternalNodes()) {
			if (node != getRootNode()) {
				edges.add(((MyRootedNode)node).getEdge());
			}
		}
		return edges;
	}

	/**
	 * @param degree the number of edges connected to a node
	 * @return a set containing all nodes in this graph of the given degree.
	 */
	public Set<Node> getNodes(int degree) {
		Set<Node> nodes = new LinkedHashSet<Node>();
		for (Node node : getNodes()) {
			// Account for no anncesstor of root, assumed by default in getDegree
			final int deg = node.getDegree();
			if (deg == degree) nodes.add(node);
		}
		return nodes;
	}

	/**
	 * Set the node heights from the current branch lengths.
	 */
	private void calculateNodeHeights() {

		if (!lengthsKnown) {
			throw new IllegalArgumentException("Can't calculate node heights because branch lengths not known");
		}

		nodeLengthsToHeights(rootNode, 0.0);

		double maxHeight = 0.0;
		for (Node externalNode : getExternalNodes()) {
			if (((MyRootedNode)externalNode).getHeight() > maxHeight) {
				maxHeight = ((MyRootedNode)externalNode).getHeight();
			}
		}

		for (Node node : getNodes()) {
			((MyRootedNode)node).setHeight(maxHeight - ((MyRootedNode)node).getHeight());
		}

		heightsKnown = true;
	}

	/**
	 * Set the node heights from the current node branch lengths. Actually
	 * sets distance from root so the heights then need to be reversed.
	 */
	private void nodeLengthsToHeights(MyRootedNode node, double height) {

		double newHeight = height;

		if (node.getLength() > 0.0) {
			newHeight += node.getLength();
		}

		node.setHeight(newHeight);

		for (Node child : node.getChildren()) {
			nodeLengthsToHeights((MyRootedNode)child, newHeight);
		}
	}

	/**
	 * Calculate branch lengths from the current node heights.
	 */
	protected void calculateBranchLengths() {

		if (!hasLengths) {
			throw new IllegalArgumentException("Can't calculate branch lengths because node heights not known");
		}

		nodeHeightsToLengths(rootNode, getHeight(rootNode));

		lengthsKnown = true;
	}

	/**
	 * Calculate branch lengths from the current node heights.
	 */
	private void nodeHeightsToLengths(MyRootedNode node, double height) {
		final double h = node.getHeight();
		node.setLength(h >= 0 ? height - h : 1);

		for (Node child : node.getChildren()) {
			nodeHeightsToLengths((MyRootedNode)child, node.getHeight());
		}

	}

	public void setConceptuallyUnrooted(boolean intent) {
		conceptuallyUnrooted = intent;
	}

	public boolean conceptuallyUnrooted() {
		return conceptuallyUnrooted;
	}

	// Attributable IMPLEMENTATION

	public void setAttribute(String name, Object value) {
		if (helper == null) {
			helper = new AttributableHelper();
		}
		helper.setAttribute(name, value);
	}

	public Object getAttribute(String name) {
		if (helper == null) {
			return null;
		}
		return helper.getAttribute(name);
	}

	public void removeAttribute(String name) {
		if( helper != null ) {
			helper.removeAttribute(name);
		}
	}

	public Set<String> getAttributeNames() {
		if (helper == null) {
			return Collections.emptySet();
		}
		return helper.getAttributeNames();
	}

	public Map<String, Object> getAttributeMap() {
		if (helper == null) {
			return Collections.emptyMap();
		}
		return helper.getAttributeMap();
	}

	public Node detachChildren(Node node, List<Integer> split) {
		assert( split.size() > 1 );

		List<Node> allChildren = getChildren(node);

		List<Node> detached = new ArrayList<Node>();

		for( int n : split ) {
			detached.add(allChildren.get(n));
		}

		MyRootedNode saveRoot = rootNode;

		for( Node n : allChildren ) {
			if( detached.contains(n) ) {
				((MyRootedNode)node).removeChild(n);
			}
		}

		MyRootedNode dnode = createInternalNode(detached);
		((MyRootedNode)node).addChild(dnode);

		rootNode = saveRoot;

		return dnode;
	}

	//My codes are from here ---------------  
	
	public void removeRoot() {
		MyRootedNode saveRoot = rootNode;		
		for (Node child : rootNode.getChildren()) {
			if (!isExternal(child)) removeInternalNode(child);
		}		
		rootNode = saveRoot;
	}

	public Node detachLCA(Node node, List<Node> split) {
		assert( split.size() > 1 );

		MyRootedNode saveRoot = rootNode;

		for( Node n : split ) {            
			((MyRootedNode)node).removeChild(n);
		}

		MyRootedNode dnode = createInternalNode(split);
		dnode.setLength(1.0);
		((MyRootedNode)node).addChild(dnode);        
		rootNode = saveRoot;

		return dnode;
	}    

	/**
	 * subTree should not have any taxa which is in taxa set of the original tree
	 */

	public void subDivideAndAttachSubtree(Edge edge, RootedTree subTree, boolean keepRootClade) throws NoEdgeException {
		MyRootedNode saveRoot = rootNode;
		
		MyRootedNode pa = (MyRootedNode) getNodes(edge)[1];		
		MyRootedNode ch = (MyRootedNode) getNodes(edge)[0];
		pa.removeChild(ch);

		List<Node> subRoots = new LinkedList<Node>();
		
		if (subTree.getTaxa().size() > 1) {				
			subRoots.add(rootAdjacenciesWith(subTree, subTree.getRootNode(), null));				
		}
		else {					
			subRoots.add(createExternalNode(subTree.getTaxa().iterator().next()));				
		}			

		MyRootedNode subRoot = createInternalNode(subRoots);
		for (Node node : subRoots) {			
			if (!isExternal(node) && !keepRootClade) removeInternalNode(node);
			else setLength(node, 1.0);
		}    	
		subRoot.addChild(ch);		
		pa.addChild(subRoot);
		setLength(1.0);
		
		rootNode = saveRoot;
	}

	public void subDivideAndAttachSubtrees(Edge edge, RootedTree srcTree, List<Node> srcPath, Set<Taxon> except) throws Exception {
		MyRootedNode saveRoot = rootNode;		
		MyRootedNode pa = (MyRootedNode) getNodes(edge)[1];		
		LinkedList<Node> path = new LinkedList<Node>();
		path.add((MyRootedNode) getNodes(edge)[0]);
		for (int i = 1; i < srcPath.size() - 1; i++) {			
			Set<Taxon> taxa = MyUtils.getDescendantTaxa(srcTree, srcPath.get(i));
			taxa.removeAll(except);
			taxa.removeAll(MyUtils.getDescendantTaxa(srcTree, srcPath.get(i-1)));
			if (taxa.size() > 0) {
				List<Node> subRoots = new LinkedList<Node>();
				subRoots.add(path.getLast());
				pa.removeChild(path.getLast());
				boolean removeClade = false;
				if (taxa.size() > 1) {
					RootedTree subTree = new RootedSubtree(srcTree, taxa);
					MyRootedNode subRoot = rootAdjacenciesWith(subTree, subTree.getRootNode(), null);
					subRoot.setLength(1.0);
					subRoots.add(subRoot);
					if (!MyUtils.getAllClades(srcTree).contains(subTree.getTaxa())) {					
						removeClade = true;
					}				
				}
				else {				
					Node newCh = createExternalNode(taxa.iterator().next());
					((MyRootedNode) newCh).setLength(1.0);
					subRoots.add(newCh);								
				}			
				MyRootedNode newNode = createInternalNode(subRoots);			
				for (Node node : subRoots) {			
					if (!isExternal(node) && removeClade) removeInternalNode(node);
				}
				newNode.setLength(1.0);
				path.add(newNode);
				pa.addChild(newNode);
			}
		}		
		rootNode = saveRoot;
	}
	
	public void addToRoot(Taxon l) {
		MyRootedNode saveRoot = rootNode;
		MyRootedNode leaf = (MyRootedNode) createExternalNode(l);
		leaf.setLength(1.0);
		saveRoot.addChild(leaf);		
		rootNode = saveRoot;		
	}
	
	public void addToRoot(RootedTree subTree, boolean keepRootClade) throws Exception {
		MyRootedNode saveRoot = rootNode;
		MyRootedNode subRoot = rootAdjacenciesWith(subTree, subTree.getRootNode(), null);
		subRoot.setLength(1.0);
		saveRoot.addChild(subRoot);
		if (!keepRootClade) removeInternalNode(subRoot);
		rootNode = saveRoot;
	}
	
	public void setLength(double len) {
		for (Node node : getNodes()) {
			((MyRootedNode) node).setLength(len);
		}
	}
	
	// PRIVATE members

	private AttributableHelper helper = null;

	protected MyRootedNode rootNode = null;
	protected final Set<Node> internalNodes = new LinkedHashSet<Node>();
	private final Map<Taxon, Node> externalNodes = new LinkedHashMap<Taxon, Node>();

	private boolean heightsKnown = false;
	private boolean lengthsKnown = false;

	private boolean hasHeights = false;
	private boolean hasLengths = false;

	private boolean conceptuallyUnrooted = false;

	private class MyRootedNode extends BaseNode {
		public MyRootedNode(Taxon taxon) {
			this.children = Collections.unmodifiableList(new ArrayList<Node>());
			this.taxon = taxon;
		}

		public MyRootedNode(List<? extends Node> children) {
			this.children = Collections.unmodifiableList(new ArrayList<Node>(children));
			this.taxon = null;
		}


		public void removeChild(Node node) {
			List<Node> c = new ArrayList<Node>(children);
			c.remove(node);
			children = Collections.unmodifiableList(c);
		}

		public void addChild(MyRootedNode node) {
			List<Node> c = new ArrayList<Node>(children);
			c.add(node);
			node.setParent(this);
			children = Collections.unmodifiableList(c);
		}

		public void replaceChildren(List<MyRootedNode> nodes) {
			for( MyRootedNode n : nodes ) {
				n.setParent(this);
			}
			children = Collections.unmodifiableList(new ArrayList<Node>(nodes));
		}


		public Node getParent() {
			return parent;
		}

		public void setParent(Node parent) {
			this.parent = parent;
		}

		public List<Node> getChildren() {
			return children;
		}

		public double getHeight() {
			return height;
		}

		// height above latest tip
		public void setHeight(double height) {
			this.height = height;
		}

		// length of branch to parent
		public double getLength() {
			return length;
		}

		public void setLength(double length) {
			this.length = length;
		}

		public int getDegree() {
			return children.size() + (this==rootNode?0:1);
		}

		/**
		 * returns the edge connecting this node to the parent node
		 * @return the edge
		 */
		public Edge getEdge() {
			if (edge == null) {
				edge = new BaseEdge() {
					public double getLength() {
						return length;
					}
				};
			}

			return edge;
		}

		/**
		 * For a rooted tree, getting the adjacencies is not the most efficient
		 * operation as it makes a new set containing the children and the parent.
		 * @return the adjacaencies
		 */
		public List<Node> getAdjacencies() {
			List<Node> adjacencies = new ArrayList<Node>();
			if (children != null) adjacencies.addAll(children);
			if (parent != null) adjacencies.add(parent);
			return adjacencies;
		}

		public Taxon getTaxon() {
			return taxon;
		}

		public void setTaxa(Taxon to) {
			taxon = to;
		}

		private List<Node> children;
		private Taxon taxon;

		private Node parent;
		private double height;
		private double length;

		private Edge edge = null;

	}
}
