package tech.conexus.alkharidkiller;

import java.awt.Point;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import org.powerbot.script.rt4.GameObject;
import org.powerbot.script.rt4.GroundItem;
import org.powerbot.script.rt4.Interactive;
import org.powerbot.script.rt4.Item;
import org.powerbot.script.rt4.ItemQuery;
import org.powerbot.script.Area;
import org.powerbot.script.Condition;
import org.powerbot.script.Filter;
import org.powerbot.script.PollingScript;
import org.powerbot.script.Random;
import org.powerbot.script.Script;
import org.powerbot.script.Tile;
import org.powerbot.script.Viewable;
import org.powerbot.script.rt4.BasicQuery;
import org.powerbot.script.rt4.ClientContext;
import org.powerbot.script.rt4.Game.Tab;
import org.powerbot.script.rt4.Npc;
import org.powerbot.script.rt4.Path;
import org.powerbot.script.rt4.Path.TraversalOption;
import org.powerbot.script.rt4.Skills;

@Script.Manifest(name = "AlKharidKillar", description = "Kills al kharid warriors and loots", properties = "client=4; topic=0;")
public class AlKharidKiller extends PollingScript<ClientContext> {

	private DoorType[] doors = new DoorType[] { DoorType.AL_KHARID_PALACE_DOOR_LEFT,
			DoorType.AL_KHARID_PALACE_DOOR_RIGHT };
	
	private static final int[] HERB_IDS = new int[] {203, 205, 207, 209, 211, 213, 215, 217, 219};
	
	private static final int WARRIOR_ID = 7323;
	private static final int FOOD_ID = 333;
	
	private static final int STATS_WIDGET_ID = 320;
	private static final int STATS_ATTACK_ID = 1;
	private static final int STATS_STRENGTH_ID = 2;
	private static final int STATS_DEFENCE_ID = 3;
	private static final int STATS_HP_ID = 9;
	
	private static final int TRAINING_MODE = STATS_DEFENCE_ID;

	private static final Area BANK_AREA = new Area(new Tile(3269, 3164), new Tile(3271, 3164), new Tile(3269, 3170),
			new Tile(3271, 3170));
	private static final Area WARRIOR_AREA = new Area(new Tile(3288, 3168), new Tile(3297, 3168), new Tile(3288, 3175),
			new Tile(3297, 3175));

	public AlKharidKiller() {

	}

	@Override
	public void start() {
		Condition.sleep(1000);
		ctx.camera.pitch(true);
	}

	@Override
	public void poll() {
		/*run();
		eat();

		if (!inMotion()) {
			pickup();
			if (!inCombat()) {
				bank();

				attack();
			} else {
				antiban();
			}
		} else {

		}*/
	}
	
	private void walkPath(LocalDoorPath path, int distanceToNextClick) {
		while (!ctx.controller.isStopping() && path.traverse()) {
			Condition.wait(new Callable<Boolean>() {
				@Override
				public Boolean call() throws Exception {
					return ctx.movement.distance(ctx.movement.destination()) < distanceToNextClick;
				}
			});
		}
	}
	
	private void walkTo(Tile destination, List<DoorType> doors, int distanceToNextClick) {
		walkPath(new LocalDoorPath(ctx, destination, doors, true), distanceToNextClick);
	}
	
	private void walkTo(Tile destination, DoorType[] doors, int distanceToNextClick) {
		walkPath(new LocalDoorPath(ctx, destination, doors, true), distanceToNextClick);
	}
	
	private void attack() {
		BasicQuery<Npc> enemies = ctx.npcs.select().id(WARRIOR_ID).nearest().limit(6).viewable();
		if (Math.random() > 0.8) {
			enemies.shuffle();
		}
		enemies.select(new Filter<Npc>() {
			@Override
			public boolean accept(Npc npc) {
				return npc.healthPercent() > 0;
			}
		});
		if (enemies.isEmpty()) {
			System.out.println("no warriors about, clicking on mm");
			enemies = ctx.npcs.select().id(WARRIOR_ID);
			
			Tile target = enemies.peek().tile().derive(-2 + ((int)Math.random() * 4), -2 + ((int)Math.random() * 4));

			walkTo(target, doors, 6);
		} else {
			Npc first = enemies.peek();
	
			if (Math.random() > 0.8) {
				ctx.camera.turnTo(first, (int) (Math.random() * 70));
			}
	
			handlePathToEntity(first.tile(), first);
	
			if (first.interact("Attack")) {
				Condition.wait(new Callable<Boolean>() {
					@Override
					public Boolean call() throws Exception {
						return inCombat();
					}
				}, 300);
			}
		}
	}

	private void run() {
		if (ctx.movement.energyLevel() >= 33) {
			ctx.movement.running(true);
		}
	}

	private void eat() {
		if (ctx.players.local().healthPercent() < 50) {
			ctx.game.tab(Tab.INVENTORY);
			ItemQuery<Item> items = ctx.inventory.select().action("Eat");
			if (items.count() == 0) {
				bank();
			} else {
				items.shuffle().peek().click();
				Condition.sleep(1000);
			}
		}
	}
	
	//320 = stats tab

	private void antiban() {
		if (Math.random() > 0.99) {
			ctx.camera.angle((int)(Math.random() * 360));
		} 
		if (Math.random() > 0.99) {
			moveRandomly(5, 300);
		} else if (Math.random() > 0.99) {
			ctx.game.tab(Tab.STATS);
			ctx.widgets.widget(320).component(TRAINING_MODE).hover();
		} else if (Math.random() > 0.98) {
			ctx.game.tab(Tab.INVENTORY);
		}
	}

	/**
	 * Author - Enfilade Moves the mouse a random distance between minDistance
	 * and maxDistance from the current position of the mouse by generating
	 * random vector and then multiplying it by a random number between
	 * minDistance and maxDistance. The maximum distance is cut short if the
	 * mouse would go off screen in the direction it chose.
	 * 
	 * @param minDistance
	 *            The minimum distance the cursor will move
	 * @param maxDistance
	 *            The maximum distance the cursor will move (exclusive)
	 */
	public void moveRandomly(final int minDistance, final int maxDistance) {
		double xvec = Math.random();
		if (Random.nextInt(0, 2) == 1) {
			xvec = -xvec;
		}
		double yvec = Math.sqrt(1 - xvec * xvec);
		if (Random.nextInt(0, 2) == 1) {
			yvec = -yvec;
		}
		double distance = maxDistance;
		Point p = ctx.input.getLocation();
		int maxX = (int) Math.round(xvec * distance + p.x);
		distance -= Math.abs((maxX - Math.max(0, Math.min(ctx.game.dimensions().getWidth(), maxX))) / xvec);
		int maxY = (int) Math.round(yvec * distance + p.y);
		distance -= Math.abs((maxY - Math.max(0, Math.min(ctx.game.dimensions().getHeight(), maxY))) / yvec);
		if (distance < minDistance) {
			return;
		}
		distance = Random.nextInt(minDistance, (int) distance);
		ctx.input.move((int) (xvec * distance) + p.x, (int) (yvec * distance) + p.y);
	}

	private void walkToCombat() {
		LocalDoorPath path = new LocalDoorPath(ctx, WARRIOR_AREA.getRandomTile(), doors, true);
		walkPath(path, 6);
	}

	private void bank() {
		if (ctx.inventory.select().count() >= 28) {
			walkTo(BANK_AREA.getRandomTile(), doors, 6);

			ctx.bank.open();
			ctx.bank.depositInventory();
			ctx.bank.withdraw(FOOD_ID, 14);

			walkToCombat();
		}
	}

	private void loot() {
		BasicQuery<GroundItem> items = ctx.groundItems.select().select(new Filter<GroundItem>() {
			@Override
			public boolean accept(GroundItem gI) {
				return gI.name().contains("Grimy");
			}
		}).nearest().viewable();
		
		if (items.size() > 0) {
			GroundItem herb = items.peek();

			handlePathToEntity(herb.tile(), herb);

			for (int tries = 0; tries < 3; tries++) {
				if (herb.valid()) {
					int c = ctx.inventory.select().id(herb.id()).size();
					if (herb.interact("Take", herb.name())) {
						if (Condition.wait(new Callable<Boolean>() {
							@Override
							public Boolean call() throws Exception {
								return ctx.inventory.select().id(herb.id()).size() > c;
							}
						}, 300)) {
							System.out.println("Picked up: " + herb.name());
							Condition.sleep(500);
							break;
						}
					}
				}
			}
		}
	}

	private void handlePathToEntity(Tile tile, Interactive entity) {
		LocalDoorPath path = new LocalDoorPath(ctx, tile, doors, true);
		
		if (path.getDoorNodes().size() == 0)
			return;

		while (!ctx.controller.isStopping() && path.traverse()) {
			if (entity.inViewport() && tile.matrix(ctx).reachable())
				break;
			else {
				Condition.sleep(1000);
			}
		}
	}

	private boolean inMotion() {
		return ctx.players.local().inMotion();
	}

	private boolean inCombat() {
		return ctx.players.local().interacting().valid();
	}
}
