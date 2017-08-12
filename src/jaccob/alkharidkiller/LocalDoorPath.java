package jaccob.alkharidkiller;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.Callable;

import org.powerbot.script.Condition;
import org.powerbot.script.Filter;
import org.powerbot.script.Locatable;
import org.powerbot.script.Tile;
import org.powerbot.script.Viewable;
import org.powerbot.script.rt4.BasicQuery;
import org.powerbot.script.rt4.ClientContext;
import org.powerbot.script.rt4.GameObject;
import org.powerbot.script.rt4.Path.TraversalOption;
import org.powerbot.script.rt4.TilePath;

public class LocalDoorPath extends Path{
	private List<Node> path;
	private List<Node> doorNodes;
	private List<Tile[]> pathSegments;
	private Graph graph;
	private List<DoorType> doors = new ArrayList<DoorType>();
	private Tile destination;
	private ClientContext ctx;
	private TilePath curPath;
	private int curPathIndex = 0;
	private boolean openDoors = false;
	private boolean finished = false;

	public LocalDoorPath(ClientContext ctx, Tile destination, boolean openDoors) {
		this.ctx = ctx;
		this.destination = destination;
		this.doorNodes = new ArrayList<>();
		this.graph = null;
		this.openDoors = openDoors;
	}
	
	public LocalDoorPath(ClientContext ctx, Tile destination, List<DoorType> doors, boolean openDoors) {
		this(ctx, destination, openDoors);
		
		this.doors = doors;
	}
	
	public LocalDoorPath(ClientContext ctx, Tile destination, DoorType[] doors, boolean openDoors) {
		this(ctx, destination, openDoors);
		
		Collections.addAll(this.doors, doors);
	}
	
	@Override
	public Tile start() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Tile end() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public int getLength() {
		int c = 0;
		for (Tile[] tiles : pathSegments)
			c += tiles.length;
		return c;
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
			int[] ids = doorIds();
			
			BasicQuery<GameObject> objs = ctx.objects.select().id(ids);
			
			objs.each(new Filter<GameObject>() {
				@Override
				public boolean accept(GameObject o) {
					
					for (DoorType dT : doors) {
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
	
	public void addDoors(DoorType[] doors) {
		for (DoorType dT : doors) {
			this.doors.add(dT);
		}
	}
	
	public void addDoors(List<DoorType> doors) {
		this.doors.addAll(doors);
	}
	
	private int[] getDoorBounds(int id, int orientation) {
		for (DoorType dT : doors) {
			if (dT.id() == id) 
				return dT.bounds(orientation);
		}
		return null;
	}
	
	public List<Node> getDoorNodes() {
		return doorNodes;
	}
	
	private int[] doorIds() {
		int[] ids = new int[doors.size()];
		for (int i = 0; i < ids.length; i++)
			ids[i] = doors.get(i).id();
		return ids;
	}
	
	public LocalDoorPath calculatePath() {
		this.graph = getGraph(openDoors);
		this.finished = false;
		calculatePath(ctx, openDoors);
		
		return this;
	}
	
	private void calculatePath(ClientContext ctx, boolean openDoors) {
		Tile base = ctx.game.mapOffset();
		Tile myPos = ctx.players.local().tile();
		
		this.curPathIndex = 0;
		this.curPath = null;
		this.pathSegments = new ArrayList<>();
		
		List<Node> path = graph.path(new Point(myPos.x(), myPos.y()), new Point(destination.x(), destination.y()));
		if (path != null) {
			List<Tile> segmentList = new ArrayList<>();
				
			for (Node n : path) {
				Tile t = base.derive(n.x, n.y);
				segmentList.add(t);
				if (openDoors && (n.flags & Graph.DOOR_CLOSED) != 0) {
					doorNodes.add(n);
					
					Tile[] segments = new Tile[segmentList.size()];
					segments = segmentList.toArray(segments);
					pathSegments.add(segments);
					segmentList.clear();
				}
			}
			
			Tile[] segments = new Tile[segmentList.size()];
			segments = segmentList.toArray(segments);
			pathSegments.add(segments);
		}
	}
	
	private boolean openDoor(Tile doorTile) {
		GameObject door = ctx.objects.select().id(doorIds()).at(doorTile).peek();
		if (!door.valid())
			return true;
		
		int id = door.id();
		int[] bounds = getDoorBounds(id, door.orientation());

		if (bounds != null) {
			door.bounds(bounds);
			
			if (!door.inViewport()) {
				ctx.camera.turnTo(door, 50); 
			}
			
			for (int tries = 0; tries < 3; tries++) {
				if (door.interact("Open", door.name())) {
					if (Condition.wait(new Callable<Boolean>() {
						@Override
						public Boolean call() throws Exception {
							return !door.valid();
						}
					}, 100, 30)) {
						return true;
					}
				}
			}
		}
		
		return false;
	}
	
	//EnumSet.of(TraversalOption.SPACE_ACTIONS, TraversalOption.HANDLE_RUN)
	public boolean traverse(EnumSet<TraversalOption> options) {
		if (finished || pathSegments.isEmpty()) 
			return false;
		
		boolean goingToDoor = curPathIndex < pathSegments.size() - 1;
		
		Tile[] tiles = pathSegments.get(curPathIndex);
		
		if (curPathIndex == pathSegments.size() - 1 && destination.matrix(ctx).onMap())
			finished = true;
		
		if (tiles.length > 0) {
			Tile lastTile = tiles[tiles.length - 1];
			GameObject door = ctx.objects.select().id(doorIds()).at(lastTile).peek();

			if (curPath == null)
				curPath = ctx.movement.newTilePath(tiles);
			
			if (goingToDoor) {
				int[] bounds = getDoorBounds(door.id(), door.orientation());
				if (bounds != null)
					door.bounds(bounds);
				
				if (!door.inViewport())
					return curPath.traverse(options);
			} else {
				return curPath.traverse(options);
			}
		}
		
		if (goingToDoor && openDoor(tiles[tiles.length - 1])) {
			curPathIndex++;
			curPath = null;
			
			return true;
		}

		return false;
	}
	
	public boolean traverse() {
		return traverse(EnumSet.of(TraversalOption.HANDLE_RUN, TraversalOption.SPACE_ACTIONS));
	}
}
