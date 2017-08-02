package tech.conexus.alkharidkiller;

import java.awt.Point;
import java.util.ArrayList;
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

public class CustomPath {
	private List<Node> path;
	private List<Node> doorNodes;
	private List<Tile[]> pathSegments;
	private Graph graph;
	private List<DoorType> doors = new ArrayList<DoorType>();
	private Tile destination;
	private ClientContext ctx;

	public CustomPath(ClientContext ctx, Graph graph, Tile destination, boolean openDoors) {
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
	
	private int[] getDoorBounds(int id) {
		for (DoorType dT : doors) {
			if (dT.id() == id) 
				return dT.bounds();
		}
		return null;
	}
	
	public List<Node> getDoorNodes() {
		return doorNodes;
	}
	
	private void calculatePath(ClientContext ctx, boolean openDoors) {
		Tile base = ctx.game.mapOffset();
		Tile myPos = ctx.players.local().tile();
		
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
	
	private int[] fixBounds(int[] bounds, int orientation) {
		if (bounds == null)
			return null;
		
		int x1 = bounds[0];
		int x2 = bounds[1];
		
		int z1 = bounds[4];
		int z2 = bounds[5];
		
		switch (orientation) {
		case 0: break;
		case 1: 
			bounds[0] = 128 - z1;
			bounds[1] = 128 - z2;
			
			bounds[4] = 128 - x1;
			bounds[5] = 128 - x2;
			
			break;
		case 2: 
			bounds[0] = 128 - bounds[0];
			bounds[1] = 128 - bounds[1];
			break;
		case 3: 
			bounds[0] = z1;
			bounds[1] = z2;
			
			bounds[4] = x1;
			bounds[5] = x2;
			
			break;
		}
		return bounds;
	}
	
	public void traverse() {
		for (int i = 0; i < pathSegments.size(); i++) {
			boolean goingToDoor = i < pathSegments.size() - 1;
			
			Tile[] tiles = pathSegments.get(i);
			if (tiles.length > 0) {
				TilePath tP = ctx.movement.newTilePath(tiles);
				if (ctx.movement.distance(tP.end()) >= 6) {
					Tile doorTile = tiles[tiles.length - 1];
					GameObject obj = ctx.objects.select().at(doorTile).peek();
					
					if (!goingToDoor || (goingToDoor && !obj.inViewport())) {
						System.out.println("About to traverse");
						
						while (tP.traverse(EnumSet.of(TraversalOption.SPACE_ACTIONS, TraversalOption.HANDLE_RUN))) {
						
							Condition.wait(new Callable<Boolean>() {
								@Override
								public Boolean call() throws Exception {
									return ctx.movement.distance(ctx.movement.destination()) < 4;
								}
							});
						}
						System.out.println("fin");
					}
				}
			}
			
			if (goingToDoor) {
				Tile doorTile = tiles[tiles.length - 1];
				GameObject obj = ctx.objects.select().at(doorTile).peek();
				
				int id = obj.id();
				int[] bounds = fixBounds(getDoorBounds(id), obj.orientation());

				if (bounds != null) {
					System.out.println("Clicking door");
					obj.bounds(bounds);
					if (obj.interact("Open")) {
						for (int tries = 0; tries < 3; tries++) {
							if (Condition.wait(new Callable<Boolean>() {
								@Override
								public Boolean call() throws Exception {
									return ctx.objects.select().at(doorTile).peek().id() != id;
								}
							}, 300)) {
								System.out.println("Done");
								break;
							}
						}
					}
				}
			}
		}
		System.out.println("finished 2");
	}
}
