package tech.conexus.alkharidkiller;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.Callable;

import org.powerbot.script.Condition;
import org.powerbot.script.Locatable;
import org.powerbot.script.Tile;
import org.powerbot.script.Viewable;
import org.powerbot.script.rt4.ClientContext;
import org.powerbot.script.rt4.GameObject;
import org.powerbot.script.rt4.Path;
import org.powerbot.script.rt4.Path.TraversalOption;
import org.powerbot.script.rt4.TilePath;

public class LocalDoorPath {
	private List<Node> path;
	private List<Node> doorNodes;
	private List<Tile[]> pathSegments;
	private Graph graph;
	private List<DoorType> doors = new ArrayList<DoorType>();
	private Tile destination;
	private ClientContext ctx;
	private TilePath curPath;
	private int curPathIndex;

	public LocalDoorPath(ClientContext ctx, Graph graph, Tile destination, boolean openDoors) {
		this.graph = graph;
		this.ctx = ctx;
		this.destination = destination;
		this.doorNodes = new ArrayList<>();
		
		calculatePath(ctx, openDoors);
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
				segmentList.add(base.derive(n.x, n.y));
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
			if (!door.inViewport())
				ctx.camera.turnTo(door, 50);
			
			for (int tries = 0; tries < 3; tries++) {
				if (door.interact("Open", door.name())) {
					if (Condition.wait(new Callable<Boolean>() {
						@Override
						public Boolean call() throws Exception {
							return !door.valid();
						}
					}, 300)) {
						return true;
					}
				}
			}
		}
		
		return false;
	}
	
	//EnumSet.of(TraversalOption.SPACE_ACTIONS, TraversalOption.HANDLE_RUN)
	public boolean traverse(EnumSet<TraversalOption> options) {
		boolean goingToDoor = curPathIndex < pathSegments.size() - 1;
		
		Tile[] tiles = pathSegments.get(curPathIndex);
		if (tiles.length > 0) {
			Tile doorTile = tiles[tiles.length - 1];
			GameObject door = ctx.objects.select().id(doorIds()).at(doorTile).peek();
			
			if (curPath == null)
				curPath = ctx.movement.newTilePath(tiles);
			
			if (goingToDoor) {
				if (!door.inViewport())
					return curPath.traverse(options);
			} else
				return curPath.traverse(options);
		}
		
		if (goingToDoor && openDoor(tiles[tiles.length - 1])) {
			curPathIndex++;
			
			return true;
		}

		return false;
	}
	
	public boolean traverse() {
		return traverse(EnumSet.of(TraversalOption.HANDLE_RUN, TraversalOption.SPACE_ACTIONS));
	}
}
