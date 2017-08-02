package tech.conexus.alkharidkiller;
import java.awt.Point;
import java.util.List;
import java.util.function.Consumer;

import org.powerbot.script.rt4.GameObject;
import org.powerbot.script.Filter;
import org.powerbot.script.PollingScript;
import org.powerbot.script.Script;
import org.powerbot.script.Tile;
import org.powerbot.script.rt4.ClientContext;
import org.powerbot.script.rt4.Item;
import org.powerbot.script.rt4.Path;


@Script.Manifest(name = "AlKharidKillar", description = "Kills al kharid warriors and loots", properties = "client=4; topic=0;")
public class AlKharidKiller extends PollingScript<ClientContext>{

	public AlKharidKiller() {

	}
	
	@Override
	public void start() {
		int[][] flags = ctx.client().getCollisionMaps()[ctx.game.floor()].getFlags();
		
		int width = flags.length;
		int height = flags[0].length;
		
		char[][] output = new char[width][height];
		for (int y = 0; y < height; y++) {
			for (int x = width - 1; x >= 0; x--) {
				if ((flags[x][y] & OBJECT_BLOCK) == OBJECT_BLOCK) {
					System.out.print('O');
				} else if ((flags[x][y] & OBJECT_TILE) == OBJECT_TILE) {
					System.out.print('T');
				} else if ((flags[x][y] & (WALL_EAST | WALL_SOUTH)) == (WALL_EAST | WALL_SOUTH)) {
					System.out.print('M');
				} else if ((flags[x][y] & WALL_EAST) == WALL_EAST) {
					System.out.print('E');
				} else if ((flags[x][y] & WALL_WEST) == WALL_WEST) {
					System.out.print('W');
				} else if ((flags[x][y] & WALL_NORTH) == WALL_NORTH) {
					System.out.print('N');
				} else if ((flags[x][y] & WALL_SOUTH) == WALL_SOUTH) {
					System.out.print('S');
				} else if ((flags[x][y] & WALL_NORTHWEST) == WALL_NORTHWEST) {
					System.out.print('Y');
				} else if ((flags[x][y] & WALL_NORTHEAST) == WALL_NORTHEAST) {
					System.out.print('G');
				} else if ((flags[x][y] & WALL_SOUTHWEST) == WALL_SOUTHWEST) {
					System.out.print('H');
				} else if ((flags[x][y] & WALL_SOUTHEAST) == WALL_SOUTHEAST) {
					System.out.print('J');
				} else {
					System.out.print(' ');
				}
			}
			System.out.println();
		}
	}
	
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
	
	@Override
	public void poll() {
		List<GameObject> objs = ctx.objects.get();
				
		for (GameObject o : objs) {
			//closed = 1511, open = 1512
			if (o.name().toLowerCase().contains("door")) {
				System.out.println(o.tile());
			}
		}

		//Path p = ctx.movement.findPath(new Tile(3287, 3178));
		Tile t = ctx.players.local().tile();
		Tile base = ctx.game.mapOffset();
		
		//System.out.println(base.toString());
		/*System.out.println("1: "+(ctx.client().getCollisionMaps()[0].getFlags()[t.x() - base.x()][t.y() - base.y()] & WALL_EAST));
		System.out.println("2: "+(ctx.client().getCollisionMaps()[0].getFlags()[t.x() - base.x()][t.y() - base.y()] & WALL_WEST));*/
		//System.out.println(p.start());
	}
	
	/*public static void main(String[] args) {
		Graph g = new Graph(20, 20);
		g.addRect(new Point(1, 2), new Point(11, 10));
		g.setDoor(new Point(10, 3), 0);
		g.path(new Point(0, 0), new Point(5, 5));
	}*/
}
