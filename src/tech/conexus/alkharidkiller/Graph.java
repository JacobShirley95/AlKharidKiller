package tech.conexus.alkharidkiller;

import java.awt.Point;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Graph {
	private int[][] coords;
	private Point base = new Point(0, 0);
	private int width;
	private int height;
	
	private static final byte WALL_LEFT = 1;
	private static final byte WALL_UP = 1 << 1;
	private static final byte WALL_RIGHT = 1 << 2;
	private static final byte WALL_DOWN = 1 << 3;
	
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
	
	public Graph(Point base, int width, int height) {
		this.base = base;
		this.width = width;
		this.height = height;
		coords = new int[width][height];
	}
	
	public Graph(int width, int height) {
		this(new Point(0, 0), width, height);
	}
	
	public void addRect(Point p, Point p2) {
		p = globalToLocal(p);
		p2 = globalToLocal(p2);
		
		int w = p2.x - p.x;
		int h = p2.y - p.y;
	
		for (int y = p.y; y < p.y + h; y++)
			coords[p.x][y] |= WALL_LEFT;
		if (p.x > 0)
			for (int y = p.y; y < p.y + h; y++)
				coords[p.x - 1][y] |= WALL_RIGHT;
		
		for (int y = p.y; y < p.y + h; y++)
			coords[p.x + w - 1][y] |= WALL_RIGHT;
		if (p.x + w < width - 1)
			for (int y = p.y; y < p.y + h; y++)
				coords[p.x + w][y] |= WALL_LEFT;
		
		for (int x = p.x; x < p.x + w; x++)
			coords[x][p.y] |= WALL_UP;
		if (p.y > 0)
			for (int x = p.x; x < p.x + w; x++)
				coords[x][p.y - 1] |= WALL_DOWN;
		
		for (int x = p.x; x < p.x + w; x++)
			coords[x][p.y + h - 1] |= WALL_DOWN;
		if (p.y > 0)
			for (int x = p.x; x < p.x + w; x++)
				coords[x][p.y + h] |= WALL_UP;
		
		/*for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				if (coords[x][y] == WALL_UP) {
					System.out.print("u");
				} else if (coords[x][y] == WALL_LEFT) {
					System.out.print("l");
				} else if (coords[x][y] == WALL_DOWN) {
					System.out.print("d");
				} else if (coords[x][y] == WALL_RIGHT) {
					System.out.print("r");
				} else {
					System.out.print(" ");
				}
			}
			System.out.println();
		}*/
		
		
		/*coords[p.x][p.y] = WALL_BOTH;
		coords[p.x + w - 1][p.y] = WALL_BOTH;
		coords[p.x + w - 1][p.y + h - 1] = WALL_BOTH;
		coords[p.x][p.y + h - 1] = WALL_BOTH;*/
	}
	
	public void path(Point source, Point target) {
		boolean[][] visited = new boolean[width][height];
		Point[][] added_by = new Point[width][height];
		LinkedList<Point> frontier = new LinkedList<Point>();
		
		target = globalToLocal(target);
		
		Point current = globalToLocal(source);
		added_by[current.x][current.y] = null;
		
		frontier.add(current);
		
		while (!frontier.isEmpty()) {
			current = frontier.removeFirst();
			
			visited[current.x][current.y] = true;
			
			if (current.x == target.x && current.y == target.y) {
				System.out.println("FOUND A WAY");
				Point p = target;
				List<Point> path = new ArrayList<>();
				path.add(target);
				while (added_by[p.x][p.y] != null) {
					p = added_by[p.x][p.y];
					path.add(p);
				}
				//System.out.println(path);
				
				for (int y = 0; y < height; y++) {
					for (int x = 0; x < width; x++) {
						if (path.contains(new Point(x, y))) {
							System.out.print("x");
						} else if (coords[x][y] == WALL_UP) {
							System.out.print("u");
						} else if (coords[x][y] == WALL_LEFT) {
							System.out.print("l");
						} else if (coords[x][y] == WALL_DOWN) {
							System.out.print("d");
						} else if (coords[x][y] == WALL_RIGHT) {
							System.out.print("r");
						} else {
							System.out.print(" ");
						}
					}
					System.out.println();
				}

				return;
			}
			
			List<Point> neighbours = getNeighbours(current);
			
			for (Point n : neighbours) {
				if (!visited[n.x][n.y] && !frontier.contains(n)) {
					//System.out.println(n);
					added_by[n.x][n.y] = current;
					frontier.add(n);
				}
			}
		}
	}
	
	public void setDoor(Point p, int value) {
		p = globalToLocal(p);
		if ((coords[p.x][p.y] & WALL_LEFT) > 0) {
			coords[p.x][p.y] = 0;
			coords[p.x - 1][p.y] = 0;
		} else if ((coords[p.x][p.y] & WALL_RIGHT) > 0) {
			coords[p.x][p.y] = 0;
			coords[p.x + 1][p.y] = 0;
		}
	}
	
	private List<Point> getNeighbours(Point local) {
		List<Point> ps = new ArrayList<>();
		
		if (local.x < 0 || local.y < 0 || local.x >= width || local.y >= height) {
				return ps;
		}
		
		if (local.x < width - 1 && (coords[local.x][local.y] & WALL_RIGHT) == 0)
			ps.add(new Point(local.x + 1, local.y));
		
		if (local.x > 0 && (coords[local.x][local.y] & WALL_LEFT) == 0)
			ps.add(new Point(local.x - 1, local.y));
		
		if (local.y < height - 1 && (coords[local.x][local.y] & WALL_DOWN) == 0)
			ps.add(new Point(local.x, local.y + 1));
		
		if (local.y > 0 && (coords[local.x][local.y] & WALL_UP) == 0)
			ps.add(new Point(local.x, local.y - 1));
		
		if (local.x > 0 && local.y > 0 && (coords[local.x][local.y] & WALL_UP) == 0 &&
		   (coords[local.x][local.y] & WALL_LEFT) == 0)
			ps.add(new Point(local.x - 1, local.y - 1));
		
		if (local.x < width - 1 && local.y > 0 && (coords[local.x][local.y] & WALL_UP) == 0 &&
			(coords[local.x][local.y] & WALL_RIGHT) == 0)
			ps.add(new Point(local.x + 1, local.y - 1));
		
		if (local.x < width - 1 && local.y < height - 1 && (coords[local.x][local.y] & WALL_DOWN) == 0 &&
		    (coords[local.x][local.y] & WALL_RIGHT) == 0 &&
			(coords[local.y + 1][local.y + 1] & (WALL_LEFT | WALL_UP)) == 0)
			ps.add(new Point(local.x + 1, local.y + 1));
		
		if (local.x > 0 && local.y < height - 1 && (coords[local.x][local.y] & WALL_DOWN) == 0 &&
		    (coords[local.x][local.y] & WALL_LEFT) == 0)
			ps.add(new Point(local.x - 1, local.y + 1));
		
		return ps;
	}
	
	public Point globalToLocal(Point p) {
		return new Point(p.x - base.x, p.y - base.y);
	}
	
	/*private class Node {
		private Node prev;

		public Node(Node prev) {
			this.prev = prev;
		}
	}*/
}
