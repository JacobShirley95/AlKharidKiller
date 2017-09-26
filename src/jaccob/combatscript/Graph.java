package jaccob.combatscript;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.powerbot.script.Tile;

public class Graph {
	private Node[][] nodes;
	private Point base = new Point(0, 0);
	private int width;
	private int height;
	
	public static final int WALL_NORTHWEST = 0x1;
	public static final int WALL_NORTH = 0x2;
	public static final int WALL_NORTHEAST = 0x4;
	public static final int WALL_EAST = 0x8;
	public static final int WALL_SOUTHEAST = 0x10;
	public static final int WALL_SOUTH = 0x20;
	public static final int WALL_SOUTHWEST = 0x40;
	public static final int WALL_WEST = 0x80;
	public static final int OBJECT_TILE = 0x100;
	public static final int DECORATION_BLOCK = 0x40000;
	public static final int OBJECT_BLOCK = 0x200000;
	public static final int DOOR_OPEN = 0x400000;
	public static final int DOOR_CLOSED = 0x800000;
	public static final int BLOCKED = OBJECT_TILE | OBJECT_BLOCK | DECORATION_BLOCK;
	
	public Graph(Point base, int width, int height) {
		this.base = base;
		this.width = width;
		this.height = height;
		this.nodes = new Node[width][height];
	}
	
	public Graph(Point base, int[][] data) {
		this.base = base;
		this.width = data.length;
		this.height = data[0].length;
		
		this.nodes = new Node[width][height];
		
		for (int x = 0; x < data.length; x++) {
			for (int y = 0; y < data[x].length; y++) {
				this.nodes[x][y] = new Node(x, y, data[x][y]);
			}
		}
	}
	
	public Graph(int width, int height) {
		this(new Point(0, 0), width, height);
	}
	
	public List<Node> path(Point source, Point target) {
		boolean[][] visited = new boolean[width][height];
		LinkedList<Node> frontier = new LinkedList<Node>();
		
		source = globalToLocal(source);
		target = globalToLocal(target);
		
		if (target.x < 0 || target.y < 0)
			return null;
		
		Node targetNode = nodes[target.x][target.y];
		
		Node curr = nodes[source.x][source.y].copy();
		curr.parent = null;
		
		frontier.add(curr);
		
		while (!frontier.isEmpty()) {
			curr = frontier.removeFirst();
			
			visited[curr.x][curr.y] = true;
			
			if (curr.x == target.x && curr.y == target.y) {
				List<Node> path = new ArrayList<>();
				while (curr != null) {
					path.add(curr);
					curr = curr.parent;
				}
				Collections.reverse(path);
				
				return path;
			}
			
			List<Node> neighbours = getNeighbours(curr, targetNode);
			
			for (Node n : neighbours) {
				if (!visited[n.x][n.y] && !frontier.contains(n)) {
					Node cp = n.copy();
					cp.parent = curr;
					frontier.add(cp);
				}
			}
		}
		return null;
	}
	
	public void drawGraph(List<Node> path) {
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				if (path != null && path.contains(new Node(x, y, -1))) {
					System.out.print('X');
				} else if ((nodes[x][y].flags & DOOR_CLOSED) == DOOR_CLOSED) {
					System.out.print('C');
				} else if ((nodes[x][y].flags & DOOR_OPEN) == DOOR_OPEN) {
					System.out.print('O');
				} /*else if ((coords[x][y] & OBJECT_TILE) == OBJECT_TILE) {
					System.out.print(' ');
				} */else if ((nodes[x][y].flags & (WALL_EAST | WALL_SOUTH)) == (WALL_EAST | WALL_SOUTH)) {
					System.out.print('M');
				} else if ((nodes[x][y].flags & WALL_EAST) == WALL_EAST) {
					System.out.print('E');
				} else if ((nodes[x][y].flags & WALL_WEST) == WALL_WEST) {
					System.out.print('W');
				} else if ((nodes[x][y].flags & WALL_NORTH) == WALL_NORTH) {
					System.out.print('N');
				} else if ((nodes[x][y].flags & WALL_SOUTH) == WALL_SOUTH) {
					System.out.print('S');
				} else if ((nodes[x][y].flags & WALL_NORTHWEST) == WALL_NORTHWEST) {
					System.out.print('Y');
				} else if ((nodes[x][y].flags & WALL_NORTHEAST) == WALL_NORTHEAST) {
					System.out.print('G');
				} else if ((nodes[x][y].flags & WALL_SOUTHWEST) == WALL_SOUTHWEST) {
					System.out.print('H');
				} else if ((nodes[x][y].flags & WALL_SOUTHEAST) == WALL_SOUTHEAST) {
					System.out.print('J');
				} else {
					System.out.print(' ');
				}
			}
			System.out.println();
		}
	}
	
	public Node getNodeFromGlobal(Point p) {
		p = globalToLocal(p);
		return nodes[p.x][p.y];
	}
	
	public boolean isBlocked(Node node) {
		return (node.flags & BLOCKED) != 0;
	}
	
	public boolean isBlocked(Node node, Node endNode) {
		return !node.equals(endNode) && (node.flags & BLOCKED) != 0;
	}
	
	private List<Node> getNeighbours(Node local, Node targetNode) {
		List<Node> ps = new ArrayList<>();
		
		if (local.x < 0 || local.y < 0 || local.x >= width || local.y >= height) {
			return ps;
		}
		
		//System.out.println(local.x);
		if (local.x + 1 < width 
			&& ((local.flags & WALL_EAST) == 0 || (nodes[local.x + 1][local.y].flags & DOOR_CLOSED) != 0)
			&& !isBlocked(nodes[local.x + 1][local.y], targetNode))
				ps.add(nodes[local.x + 1][local.y]);
		
		if (local.x > 0 
			&& ((local.flags & WALL_WEST) == 0 || (nodes[local.x - 1][local.y].flags & DOOR_CLOSED) != 0)
		    && !isBlocked(nodes[local.x - 1][local.y], targetNode))
				ps.add(nodes[local.x - 1][local.y]);
		
		if (local.y + 1 < height
			&& ((local.flags & WALL_NORTH) == 0 || (nodes[local.x][local.y + 1].flags & DOOR_CLOSED) != 0)
			&& !isBlocked(nodes[local.x][local.y + 1], targetNode))
				ps.add(nodes[local.x][local.y + 1]);
		
		if (local.y > 0 
			&& ((local.flags & WALL_SOUTH) == 0 || (nodes[local.x][local.y - 1].flags & DOOR_CLOSED) != 0)
			&& !isBlocked(nodes[local.x][local.y - 1], targetNode))
			ps.add(nodes[local.x][local.y - 1]);
		
		if (local.x > 0 && local.y > 0 && (local.flags & (WALL_SOUTHWEST | WALL_SOUTH | WALL_WEST)) == 0 &&
		   !isBlocked(nodes[local.x - 1][local.y - 1], targetNode) &&
		   (nodes[local.x][local.y - 1].flags & WALL_WEST) == 0 &&
		   (nodes[local.x - 1][local.y].flags & WALL_SOUTH) == 0)
			ps.add(nodes[local.x - 1][local.y - 1]);
		
		if (local.x + 1 < width && local.y > 0 && (local.flags & (WALL_SOUTHEAST | WALL_SOUTH | WALL_EAST)) == 0 &&
			!isBlocked(nodes[local.x + 1][local.y - 1], targetNode) &&
			(nodes[local.x][local.y - 1].flags & WALL_EAST) == 0 &&
			(nodes[local.x + 1][local.y].flags & WALL_SOUTH) == 0)
			ps.add(nodes[local.x + 1][local.y - 1]);
		
		if (local.x + 1< width && local.y + 1< height && (local.flags & (WALL_NORTH | WALL_EAST | WALL_NORTHEAST)) == 0 &&
			!isBlocked(nodes[local.x + 1][local.y + 1], targetNode) &&
		    (nodes[local.x][local.y + 1].flags & WALL_EAST) == 0 &&
			(nodes[local.x + 1][local.y].flags & WALL_NORTH) == 0)
			ps.add(nodes[local.x + 1][local.y + 1]);
		
		if (local.x > 0 && local.y + 1 < height && (local.flags & (WALL_NORTH | WALL_WEST | WALL_NORTHWEST)) == 0 &&
			!isBlocked(nodes[local.x - 1][local.y + 1], targetNode) &&
		    (nodes[local.x][local.y + 1].flags & WALL_WEST) == 0 &&
		    (nodes[local.x - 1][local.y].flags & WALL_NORTH) == 0)
			ps.add(nodes[local.x - 1][local.y + 1]);
		
		return ps;
	}
	
	public Point globalToLocal(Point p) {
		return new Point(p.x - base.x, p.y - base.y);
	}
}
