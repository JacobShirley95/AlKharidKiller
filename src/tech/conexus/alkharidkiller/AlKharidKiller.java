package tech.conexus.alkharidkiller;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import org.powerbot.script.rt4.GameObject;
import org.powerbot.script.rt4.GameObject.Type;
import org.powerbot.script.rt4.GroundItem;
import org.powerbot.script.rt4.Interactive;
import org.powerbot.script.rt4.Item;
import org.powerbot.script.rt4.ItemQuery;
import org.powerbot.script.Area;
import org.powerbot.script.Condition;
import org.powerbot.script.Filter;
import org.powerbot.script.PaintListener;
import org.powerbot.script.PollingScript;
import org.powerbot.script.Random;
import org.powerbot.script.Script;
import org.powerbot.script.Tile;
import org.powerbot.script.Viewable;
import org.powerbot.script.rt4.Actor;
import org.powerbot.script.rt4.BasicQuery;
import org.powerbot.script.rt4.ClientContext;
import org.powerbot.script.rt4.Constants;
import org.powerbot.script.rt4.Game;
import org.powerbot.script.rt4.Game.Tab;
import org.powerbot.script.rt4.Npc;
import org.powerbot.script.rt4.Path;
import org.powerbot.script.rt4.Path.TraversalOption;
import org.powerbot.script.rt4.Player;
import org.powerbot.script.rt4.Skills;

@Script.Manifest(name = "AlKharidKillar", description = "Kills al kharid warriors and loots", properties = "client=4; topic=0;")
public class AlKharidKiller extends PollingScript<ClientContext> implements PaintListener{

	private DoorType[] doors = new DoorType[] { DoorType.AL_KHARID_PALACE_DOOR_LEFT, DoorType.AL_KHARID_PALACE_DOOR_RIGHT };
	
	private final static int[] WARRIOR_BOUNDS = {-28, 28, -168, 0, -36, 36};
	
	private static final int[] HERB_IDS = new int[] {205, 207, 209, 211, 213, 215, 217, 219};
	
	private static final int WARRIOR_ID = 7323;
	private static final int FOOD_ID = 333;
	
	private static final int STATS_WIDGET_ID = 320;
	private static final int STATS_ATTACK_ID = 1;
	private static final int STATS_STRENGTH_ID = 2;
	private static final int STATS_DEFENCE_ID = 3;
	private static final int STATS_HP_ID = 9;
	
	private static final int TRAINING_MODE = STATS_STRENGTH_ID;

	private static final Area BANK_AREA = new Area(new Tile(3269, 3164), new Tile(3271, 3164), new Tile(3269, 3170),
			new Tile(3271, 3170));
	private static final Area WARRIOR_AREA = new Area(new Tile(3288, 3168), new Tile(3297, 3168), new Tile(3288, 3175),
			new Tile(3297, 3175));
	private static final Area COMBAT_AREA = new Area(new Tile(3282, 3159), new Tile(3303, 3177));

	public AlKharidKiller() {

	}

	@Override
	public void start() {
		Condition.sleep(1000);
		ctx.camera.pitch(true);
		//walkToCombat();
		//left 3285, 3171
		//walkTo(new Tile(3290, 3164), doors, 6);
	}

	@Override
	public void poll() {
		eat();

		if (!inMotion()) {
			loot();
			if (!inCombat()) {
				bank();

				attack();
			} else {
				run();
				antiban();
			}
		} else {

		}
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
		walkPath(new LocalDoorPath(ctx, destination, doors, true).calculatePath(), distanceToNextClick);
	}
	
	private void walkTo(Tile destination, DoorType[] doors, int distanceToNextClick) {
		LocalDoorPath path = new LocalDoorPath(ctx, destination, doors, true).calculatePath();
		walkPath(path, distanceToNextClick);
	}
	
	private BasicQuery<Npc> getEnemies() {
		BasicQuery<Npc> enemies = ctx.npcs.select().id(WARRIOR_ID).nearest().limit(6).select(Interactive.doSetBounds(WARRIOR_BOUNDS)).viewable();
		if (Math.random() > 0.8) {
			enemies.shuffle();
		}
		enemies.select(new Filter<Npc>() {
			@Override
			public boolean accept(Npc npc) {
				Actor interaction = npc.interacting();
				if (interaction instanceof Player) {
					Player p = (Player)interaction;
					if (!p.interacting().equals(npc)) {
						
						return true;
					} else {
						return false;
					}
				}
				
				return npc.healthPercent() > 0;
			}
		});
		
		System.out.println("I");
		System.out.println(enemies.peek().interacting());
		System.out.println(enemies.peek().interacting() instanceof Player);
		
		return enemies;
	}
	
	private void attack() {
		BasicQuery<Npc> enemies = getEnemies();
		
		if (enemies.isEmpty()) {
			System.out.println("no warriors about, clicking on mm");
			enemies = ctx.npcs.select().id(WARRIOR_ID).shuffle();
			
			Tile target = enemies.peek().tile().derive(-2 + ((int)Math.random() * 4), -2 + ((int)Math.random() * 4));
			
			target = COMBAT_AREA.getClosestTo(target);
			
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
	
	private void hoverNext() {
		BasicQuery<Npc> enemies = getEnemies();
		if (!enemies.isEmpty()) {
			Npc first = enemies.peek();
			first.hover();
		}
	}

	private void run() {
		if (ctx.movement.energyLevel() >= 25) {
			ctx.movement.running(true);
		}
	}
	
	private void checkGroundObject() {
		ctx.objects.select().viewable().shuffle().peek().interact("Examine");
	}
	
	private void examineRandom() {
		GameObject obj = ctx.objects.select().select(new Filter<GameObject>() {
			@Override
			public boolean accept(GameObject o) {
				System.out.println();
				if (o.type().equals(Type.INTERACTIVE) && !Arrays.asList(o.actions()).contains("Examine"))
					return true;
				return false;
			}
			
		}).viewable().shuffle().peek();
		
		System.out.println("Examining: " + obj.name());
		
		obj.interact("Examine");
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
			hoverSkill(TRAINING_MODE);
		} else if (Math.random() > 0.98) {
			ctx.game.tab(Tab.INVENTORY);
		} else if (Math.random() > 0.98) {
			hoverNext();
		} else if (Math.random() > 0.995) {
			examineRandom();
		}
	}
	
	private void hoverSkill(int id) {
		ctx.game.tab(Tab.STATS);
		ctx.widgets.widget(320).component(id).hover();
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
		walkTo(WARRIOR_AREA.getRandomTile(), doors, 6);
	}

	private void bank() {
		if (ctx.inventory.select().count() >= 28) {
			walkTo(BANK_AREA.getRandomTile(), doors, 6);

			if (ctx.bank.open())
				if (ctx.bank.depositInventory())
					ctx.bank.withdraw(FOOD_ID, 14);

			walkToCombat();
		}
	}

	private void loot() {
		BasicQuery<GroundItem> items = ctx.groundItems.select().id(HERB_IDS).nearest().viewable();
		
		if (Math.random() > 0.8)
			items.shuffle();
		
		if (items.size() > 0) {
			GroundItem herb = items.peek();

			handlePathToEntity(herb.tile(), herb);

			for (int tries = 0; tries < 3; tries++) {
				if (herb.valid()) {
					int c = ctx.inventory.select().id(herb.id()).size();
					if (herb.interact("Take", herb.name())) {
						ctx.game.tab(Tab.INVENTORY);
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
	
	private int lastXP = -1;
	private int startXP = -1;
	
	private int getXPPerHour() {
		if (startXP == -1) {
			startXP = ctx.skills.experience(Constants.SKILLS_STRENGTH);
		}
		
		long runTime = getRuntime();
		int currentExp = ctx.skills.experience(Constants.SKILLS_STRENGTH);
		int expGain = currentExp - startXP;
		int expPh = (int) (3600000d / (long) runTime * (double) (expGain));
		
		return expPh;
	}

	private void handlePathToEntity(Tile tile, Interactive entity) {
		LocalDoorPath path = new LocalDoorPath(ctx, tile, doors, true);
		path.calculatePath();
		
		if (path.getDoorNodes().isEmpty()) {
			return;
		}

		while (!ctx.controller.isStopping() && path.traverse()) {
			if (entity.inViewport() && tile.matrix(ctx).reachable()) {
				System.out.println(entity);
				break;
			} else {
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

	@Override
	public void repaint(Graphics g) {
		Graphics2D g2 = (Graphics2D)g;
		g2.setColor(Color.red);
		g2.drawString("Strength EXP/Hr: " + getXPPerHour(), 10, 30);
	}
}
