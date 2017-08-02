package tech.conexus.alkharidkiller;

public class Node {
	public int x;
	public int y;
	public int flags;
	public Node parent;
	public byte type; //will be used to do ladders / stairs
	
	public Node(int x, int y, int flags) {
		this.x = x;
		this.y = y;
		this.flags = flags;
		this.parent = null;
	}
	
	public Node copy() {
		return new Node(x, y, flags);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Node) {
			Node n = (Node)obj;
			return n.x == x && n.y == y;
		}
		return super.equals(obj);
	}
}
