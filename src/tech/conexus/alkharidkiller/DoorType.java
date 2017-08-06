package tech.conexus.alkharidkiller;

import java.util.Arrays;

public enum DoorType {
	AL_KHARID_PALACE_DOOR_LEFT(1513, 4, 20, -232, 0, 8, 128),
	AL_KHARID_PALACE_DOOR_RIGHT(1511, -4, 20, -232, 0, 8, 128);
	
	private int id;
	private int[] bounds;
	
	DoorType(int id, int... bounds) {
		this.id = id;
		this.bounds = bounds;
	}
	
	public int id() {
		return id;
	}
	
	public int[] bounds(int orientation) {
		return fixBounds(orientation);
	}
	
	private int[] fixBounds(int orientation) {
		if (bounds == null)
			return null;
		
		int[] bounds = Arrays.copyOf(this.bounds, this.bounds.length);
		
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
			bounds[0] = 128 - x1;
			bounds[1] = 128 - x2;
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
}
