package tech.conexus.alkharidkiller;

public enum DoorType {
	AL_KHARID_PALACE_DOOR_LEFT(1513, 4, 20, -232, 0, 8, 128),
	AL_KHARID_PALACE_DOOR_RIGHT(1511, -4, 20, -232, 0, 0, 128);
	
	private int id;
	private int[] bounds;
	
	DoorType(int id, int... bounds) {
		this.id = id;
		this.bounds = bounds;
	}
	
	public int id() {
		return id;
	}
	
	public int[] bounds() {
		return bounds;
	}
}
