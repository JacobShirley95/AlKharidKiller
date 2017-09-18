package jaccob.alkharidkiller;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.powerbot.script.rt4.GameObject;
import org.powerbot.script.rt4.GameObject.Type;
import org.powerbot.script.rt4.GeItem;
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
import org.powerbot.script.rt6.LocalPath;

@Script.Manifest(name = "CombatScript", description = "Kills any pre-defined warrior and opens doors/gates", properties = "client=4; topic=0;")
public class CombatScript extends PollingScript<ClientContext> implements PaintListener{
	private static final boolean SHOULD_LOOT = false;
	private static final int FOOD_ID = 333;
	
	private static final int STATS_WIDGET_ID = 320;
	private static final int STATS_ATTACK_ID = 1;
	private static final int STATS_STRENGTH_ID = 2;
	private static final int STATS_DEFENCE_ID = 3;
	private static final int STATS_HP_ID = 9;
	
	private static final int TRAINING_MODE = STATS_STRENGTH_ID;

	private static final Area BANK_AREA = new Area(new Tile(3269, 3164), new Tile(3271, 3164), new Tile(3269, 3170),
			new Tile(3271, 3170));

	//new int[] {7323}, new int[][] {new int[] {-28, 28, -168, 0, -36, 36}}, 
	enum CombatZone {
		AL_KHARID(new Area(new Tile(3287, 3167), new Tile(3303, 3177)), 
				  new DoorType[] {DoorType.AL_KHARID_PALACE_DOOR_LEFT, 
						  		  DoorType.AL_KHARID_PALACE_DOOR_RIGHT}),
		
		CHICKENS(new Area(new Tile(3225, 3296), new Tile(3233, 3300)), 
				  new DoorType[] {DoorType.GATE_1, 
						  		  DoorType.GATE_2,
						  		  DoorType.DOOR});
		
		public Area area;
		public DoorType[] doors;

		CombatZone(Area area, DoorType[] doors) {
			this.area = area;
			this.doors = doors;
		}
	}
	
	enum NpcType {
		CHICKEN(new int[] {2692, 2693}, new int[] {-20, 20, -68, 0, -20, 20}),
		WARRIOR(new int[] {7323}, new int[] {-28, 28, -168, 0, -36, 36});
		
		public int[] ids;
		public int[] bounds;
		
		NpcType(int[] ids, int[] bounds) {
			this.ids = ids;
			this.bounds = bounds;
		}
	}
	
	enum ScriptArea {
		AL_KHARID(CombatZone.AL_KHARID, new NpcType[] {NpcType.WARRIOR}, new int[] {205, 207, 209, 211, 213, 215, 217, 219}, true),
		LUM_CHICKENS(CombatZone.CHICKENS, new NpcType[] {NpcType.CHICKEN}, new int[] {314}, false);
		
		public CombatZone zone;
		public NpcType[] npcs;
		public int[] lootIds;
		public boolean banking;
		
		private ScriptArea(CombatZone zone, NpcType[] npcs, int[] lootIds, boolean banking) {
			this.zone = zone;
			this.npcs = npcs;
			this.lootIds = lootIds;
			this.banking = banking;
		}
	}
	
	ScriptArea SCR = ScriptArea.LUM_CHICKENS;
	CombatZone zone = SCR.zone;
	NpcType[] npcs = SCR.npcs;
	int[] lootValues = new int[SCR.lootIds.length];
	
	private int pickedUpValue = 0;

	public CombatScript() {

	}

	@Override
	public void start() {
		Condition.sleep(1000);
		ctx.input.speed(-50);
		ctx.camera.pitch(true);
		
		for (int i = 0; i < lootValues.length; i++) {
			lootValues[i] = new GeItem(SCR.lootIds[i]).price;
		}
		//walkToCombat();
		//left 3285, 3171
		//walkTo(new Tile(3290, 3164), doors, 6);
	}

	@Override
	public void poll() {
		eat();

		//if (!inMotion()) {
			if (!inCombat()) {
				if (SHOULD_LOOT && loot())
					return;
				
				bank();
				attack();
			} else {
				run();

				antiban();
			}
		//}
	}
	
	private void walkPath(LocalDoorPath path, int distanceToNextClick) {
		while (!ctx.controller.isStopping() && path.traverse()) {
			Condition.wait(new Callable<Boolean>() {
				@Override
				public Boolean call() throws Exception {
					return ctx.movement.distance(ctx.movement.destination()) < distanceToNextClick;
				}
			}, 300, 200);
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
		BasicQuery<Npc> enemies = filterNpcs(npcs).nearest().limit(6).select(new Filter<Npc>() {
			@Override
			public boolean accept(Npc npc) {
				npc.bounds(getBounds(npc.id(), npcs));
				
				return true;
			}
		}).viewable();
		
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
		
		return enemies;
	}
	
	private int[] getBounds(int id, NpcType[] npcs) {
		for (NpcType npc : npcs) {
			for (int id2 : npc.ids)
				if (id2 == id)
					return npc.bounds;
		}
		return null;
	}
	
	private BasicQuery<Npc> filterNpcs(NpcType[] npcs) {
		BasicQuery<Npc> q = ctx.npcs.select();
		for (NpcType npc : npcs) {
			q.id(npc.ids);
		}
		
		return q;
	}
	
	private void attack() {
		BasicQuery<Npc> enemies = getEnemies();
		
		if (enemies.isEmpty()) {
			System.out.println("no warriors about, clicking on mm");
			enemies = filterNpcs(npcs).select(new Filter<Npc>() {
				@Override
				public boolean accept(Npc npc) {
					if (npc.healthPercent() == 0)
						return false;
					
					return true;
				}
				
			}).shuffle();
			
			Tile target = enemies.peek().tile().derive(-2 + ((int)Math.random() * 4), -2 + ((int)Math.random() * 4));
			
			target = zone.area.getClosestTo(target);
			
			walkTo(target, zone.doors, 6);
		} else {
			Map<Npc, Integer> distances = new HashMap<>();
			
			if (enemies.size() > 1) {
				enemies.forEach(new Consumer<Npc>() {
					@Override
					public void accept(Npc npc) {
						distances.put(npc, new LocalDoorPath(ctx, npc.tile(), zone.doors, true).calculatePath().getLength());
					}
				});

				enemies.sort(new Comparator<Npc>() {
					@Override
					public int compare(Npc n1, Npc n2) {
						return distances.get(n1) - distances.get(n2);
					}
				});
			}
			
			Npc first = enemies.peek();
	
			if (Math.random() > 0.8) {
				//ctx.camera.turnTo(first, (int) (Math.random() * 70));
			}
			
			handlePathToEntity(first.tile(), first);
			
			if (first.interact("Attack", first.name())) {
				if (Condition.wait(new Callable<Boolean>() {
					@Override
					public Boolean call() throws Exception {
						return ctx.players.local().interacting().valid();
					}
				}, 50, 10)) {
					Condition.wait(new Callable<Boolean>() {
						@Override
						public Boolean call() throws Exception {
							return first.inCombat() || first.interacting().valid();
						}
					}, 300);
				}
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
				if (o.name() != null && !o.name().equals("null") && o.type().equals(Type.INTERACTIVE))
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
		} else if (Math.random() > 0.998) {
			examineRandom();
		}
	}
	
	private void hoverSkill(int id) {
		ctx.game.tab(Tab.STATS);
		ctx.widgets.widget(320).component(id).hover();
		Condition.sleep((int)(1500 + (Math.random() * 2000)));
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
		walkTo(zone.area.getRandomTile(), zone.doors, 6);
	}

	private void bank() {
		if (SCR.banking) {
			if (ctx.inventory.select().count() >= 28) {
				walkTo(BANK_AREA.getRandomTile(), zone.doors, 6);
				
				Condition.sleep(2000);
	
				if (ctx.bank.open())
					if (ctx.bank.depositInventory())
						ctx.bank.withdraw(FOOD_ID, 7);
	
				walkToCombat();
			}
		}
	}

	private boolean loot() {
		BasicQuery<GroundItem> items = ctx.groundItems.select().id(SCR.lootIds).nearest().viewable();
		
		if (Math.random() > 0.8)
			items.shuffle();
		
		if (items.size() > 0) {
			GroundItem herb = items.peek();

			handlePathToEntity(herb.tile(), herb);

			for (int tries = 0; tries < 3; tries++) {
				if (herb.valid() && herb.tile().matrix(ctx).reachable()) {
					int c = ctx.inventory.select().id(herb.id()).count(true);
					int id = herb.id();
					if (herb.interact("Take", herb.name())) {
						ctx.game.tab(Tab.INVENTORY);
						if (Condition.wait(new Callable<Boolean>() {
							@Override
							public Boolean call() throws Exception {
								return ctx.inventory.select().id(herb.id()).count(true) > c;
							}
						}, 300)) {
							System.out.println("Picked up: " + herb.name());
							int[] ids = SCR.lootIds;
							for (int i = 0; i < ids.length; i++) {
								if (id == ids[i]) {
									pickedUpValue += lootValues[i] * herb.stackSize();
									break;
								}
							}
							Condition.sleep(500);
							return true;
						}
					}
				}
			}
		}
		
		return false;
	}
	
	private int lastXP = -1;
	private int startXP = -1;
	
	private int getXPPerHour() {
		if (startXP == -1) {
			startXP = ctx.skills.experience(Constants.SKILLS_ATTACK);
		}
		
		long runTime = getRuntime();
		int currentExp = ctx.skills.experience(Constants.SKILLS_ATTACK);
		int expGain = currentExp - startXP;
		int expPh = (int) (3600000d / (long) runTime * (double) (expGain));
		
		return expPh;
	}
	
	private int getMoneyPerHour() {
		return 0;
	}

	private void handlePathToEntity(Tile tile, Interactive entity) {
		if (tile.matrix(ctx).reachable())
			return;
		
		LocalDoorPath path = new LocalDoorPath(ctx, tile, zone.doors, true);
		path.calculatePath();
		
		if (path.getDoorNodes().isEmpty()) {
			return;
		}

		while (!ctx.controller.isStopping() && path.traverse()) {
			if (entity.inViewport() && tile.matrix(ctx).reachable()) {
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
		final Player player = ctx.players.local();
		if (player.interacting().valid()) {
			return Condition.wait(new Callable<Boolean>() {
				@Override
				public Boolean call() throws Exception {
					return player.inCombat();
				}
			}, 100, 20);
		}
		return false;
	}
	
	public static String formatInterval(final long interval, boolean millisecs )
	{
	    final long hr = TimeUnit.MILLISECONDS.toHours(interval);
	    final long min = TimeUnit.MILLISECONDS.toMinutes(interval) %60;
	    final long sec = TimeUnit.MILLISECONDS.toSeconds(interval) %60;
	    final long ms = TimeUnit.MILLISECONDS.toMillis(interval) %1000;
	    if( millisecs ) {
	        return String.format("%02d:%02d:%02d.%03d", hr, min, sec, ms);
	    } else {
	        return String.format("%02d:%02d:%02d", hr, min, sec );
	    }
	}

	@Override
	public void repaint(Graphics g) {
		Graphics2D g2 = (Graphics2D)g;
		g2.setColor(Color.green);
		
		long runtime = getRuntime();
		
		g2.drawString("Time running: " + formatInterval(runtime, false), 10, 30);
		g2.drawString("Xp/Hr: " + getXPPerHour(), 10, 50);
		g2.drawString("Money made: " + pickedUpValue + " gp", 10, 70);
	}
}
