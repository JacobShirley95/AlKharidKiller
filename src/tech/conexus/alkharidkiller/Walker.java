package tech.conexus.alkharidkiller;

import java.awt.Point;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.powerbot.script.Condition;
import org.powerbot.script.Filter;
import org.powerbot.script.Tile;
import org.powerbot.script.rt4.BasicQuery;
import org.powerbot.script.rt4.ClientContext;
import org.powerbot.script.rt4.GameObject;
import org.powerbot.script.rt4.Path.TraversalOption;

public class Walker {
	private Tile destination;
	private List<DoorType> doorTypes;
	private boolean openDoors;
	private ClientContext ctx;

	public Walker(ClientContext ctx, Tile destination) {
		this.ctx = ctx;
		this.destination = destination;
		this.doorTypes = new ArrayList<>();
	}
	
	public void addDoors(DoorType... doorType) {
		for (DoorType dT : doorType)
			this.doorTypes.add(dT);
	}
	
	private Graph getGraph(boolean handleDoors) {
		Tile base = ctx.game.mapOffset();
		
		int[][] flags = ctx.client().getCollisionMaps()[ctx.game.floor()].getFlags();
		
		int[][] arr = new int[flags.length][flags[0].length];
		
		for (int x = 0; x < arr.length; x++) {
			for (int y = 0; y < arr[x].length; y++) {
				arr[x][y] = flags[x][y];
			}
		}
		
		if (handleDoors) {
			int[] ids = new int[doorTypes.size()];
			for (int i = 0; i < doorTypes.size(); i++) {
				ids[i] = doorTypes.get(i).id();
			}
			
			BasicQuery<GameObject> objs = ctx.objects.select().id(ids);
			objs.each(new Filter<GameObject>() {
				@Override
				public boolean accept(GameObject o) {
					for (DoorType dT : doorTypes) {
						if (dT.id() == o.id()) {
							Tile t = o.tile();
							
							arr[t.x() - base.x()][t.y() - base.y()] &= ~Graph.WALL_EAST;
							arr[t.x() - base.x()][t.y() - base.y()] &= ~Graph.WALL_WEST;
							arr[t.x() - base.x()][t.y() - base.y()] &= ~Graph.WALL_NORTH;
							arr[t.x() - base.x()][t.y() - base.y()] &= ~Graph.WALL_SOUTH;
							
							arr[t.x() - base.x()][t.y() - base.y()] |= Graph.DOOR_CLOSED;
						}
					}
					
					return true;
				}
			});
		}
		
		return new Graph(new Point(base.x(), base.y()), arr);
	}
	
	public LocalDoorPath getPath() {
		Graph g = getGraph(true);
		//g.drawGraph(null);
		
		LocalDoorPath cp = new LocalDoorPath(ctx, g, destination, true);
		cp.addDoors(doorTypes);
		
		return cp;
	}
	
	public void start(int timeout) {
		Graph g = getGraph(true);
		//g.drawGraph(null);
		
		LocalDoorPath cp = new LocalDoorPath(ctx, g, destination, true);
		cp.addDoors(doorTypes);
		while (cp.traverse()) { Condition.sleep(200); }
	}
}
