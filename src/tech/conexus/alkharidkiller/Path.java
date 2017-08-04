package tech.conexus.alkharidkiller;

import java.util.EnumSet;

import org.powerbot.script.Tile;
import org.powerbot.script.rt4.Path.TraversalOption;

public abstract class Path {
	public abstract Tile start();
	public abstract Tile end();
	
	public abstract boolean traverse(EnumSet<TraversalOption> options);
	
	public boolean traverse() {
		return traverse(EnumSet.of(TraversalOption.HANDLE_RUN, TraversalOption.SPACE_ACTIONS));
	}
}
