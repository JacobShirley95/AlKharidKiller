package jaccob.combatscript;

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
import java.util.Iterator;
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
import org.powerbot.script.rt4.Magic;
import org.powerbot.script.Area;
import org.powerbot.script.Condition;
import org.powerbot.script.Filter;
import org.powerbot.script.Locatable;
import org.powerbot.script.PaintListener;
import org.powerbot.script.PollingScript;
import org.powerbot.script.Random;
import org.powerbot.script.Script;
import org.powerbot.script.Tile;
import org.powerbot.script.Viewable;
import org.powerbot.script.rt4.Actor;
import org.powerbot.script.rt4.BasicQuery;
import org.powerbot.script.rt4.ClientContext;
import org.powerbot.script.rt4.Combat.Style;
import org.powerbot.script.rt4.Constants;
import org.powerbot.script.rt4.Game;
import org.powerbot.script.rt4.Game.Tab;
import org.powerbot.script.rt4.Npc;
import org.powerbot.script.rt4.Path;
import org.powerbot.script.rt4.Path.TraversalOption;
import org.powerbot.script.rt4.Player;
import org.powerbot.script.rt4.PlayerQuery;
import org.powerbot.script.rt4.Skills;
import org.powerbot.script.rt6.LocalPath;
import org.powerbot.script.rt6.Menu;

@Script.Manifest(name = "CombatScript", description = "Kills any pre-defined warrior and opens doors/gates", properties = "client=4; topic=0;")
public class CombatScript extends PollingScript<ClientContext> implements PaintListener{
	private static final boolean SHOULD_LOOT = true;
	private static final boolean PICK_UP_ARROW_CLUMPS = false;
	
	private static final boolean TELE_AWAY = true;
	
	private static final int ARROW_ID = 884;
	private static final int FOOD_ID = 379; //1965
	
	private static final int STATS_WIDGET_ID = 320;
	private static final int STATS_ATTACK_ID = 1;
	private static final int STATS_STRENGTH_ID = 2;
	private static final int STATS_DEFENCE_ID = 3;
	private static final int STATS_RANGED_ID = 4;
	private static final int STATS_MAGIC_ID = 6;
	private static final int STATS_HP_ID = 9;
	
	private static final int TRAINING_MODE = STATS_DEFENCE_ID;

	//new int[] {7323}, new int[][] {new int[] {-28, 28, -168, 0, -36, 36}}, 
	enum CombatZone {
		AL_KHARID(new Area(new Tile(3287, 3167), new Tile(3303, 3177)), 
				  new DoorType[] {DoorType.AL_KHARID_PALACE_DOOR_LEFT, 
						  		  DoorType.AL_KHARID_PALACE_DOOR_RIGHT}, true),
		
		CHICKEN_PEN(new Area(new Tile(3225, 3296), new Tile(3233, 3300)), 
				  new DoorType[] {DoorType.GATE_1, 
						  		  DoorType.GATE_2,
						  		  DoorType.DOOR}, false),
		
		GIANT_SPIDERS(new Area(new Tile(2122, 5269), new Tile(2127, 5273)), new DoorType[] {}, false),
		
		MOSS_GIANTS(new Area(new Tile(3158, 9899), new Tile(3170, 9910)), new DoorType[] {}, true);
		
		public Area area;
		public DoorType[] doors;
		public boolean multiCombat;

		CombatZone(Area area, DoorType[] doors, boolean multiCombat) {
			this.area = area;
			this.doors = doors;
			this.multiCombat = multiCombat;
		}
	}
	
	enum NpcType {
		CHICKEN(new int[] {2692, 2693}, new int[] {-20, 20, -68, 0, -20, 20}),
		WARRIOR(new int[] {3103}, new int[] {-28, 28, -168, 0, -36, 36}),
		GIANT_SPIDER(new int[] {2477}, new int[] {-20, 20, -68, 0, -20, 20}),
		MOSS_GIANT(new int[] {2090, 2091, 2092, 2093}, new int[] {-28, 28, -168, 0, -36, 36});
		
		public int[] ids;
		public int[] bounds;
		
		NpcType(int[] ids, int[] bounds) {
			this.ids = ids;
			this.bounds = bounds;
		}
	}
	
	enum ScriptArea {
		AL_KHARID(CombatZone.AL_KHARID, 
				  new NpcType[] {NpcType.WARRIOR}, 
				  new int[] {205, 207, 209, 211, 213, 215, 217, 219}, 
				  true,
				  new Area(new Tile(3269, 3164), new Tile(3271, 3164), new Tile(3269, 3170),
							new Tile(3271, 3170))),
		LUM_CHICKENS(CombatZone.CHICKEN_PEN,
					 new NpcType[] {NpcType.CHICKEN}, 
					 new int[] {884}, //314 = feather, 526 = bones, 884 = iron arrows
					 false,
					 null),
		
		GIANT_SPIDERS(CombatZone.GIANT_SPIDERS, 
					  new NpcType[] {NpcType.GIANT_SPIDER},
					  new int[] {},
					  false,
					  null),
		
		MOSS_GIANTS(CombatZone.MOSS_GIANTS,
				  new NpcType[] {NpcType.MOSS_GIANT},
				  new int[] {594, 562, 561, 563},
				  false,
				  null);
		
		public CombatZone zone;
		public NpcType[] npcs;
		public int[] lootIds;
		public boolean banking;
		public Area bankArea;
		
		private ScriptArea(CombatZone zone, NpcType[] npcs, int[] lootIds, boolean banking, Area bankArea) {
			this.zone = zone;
			this.npcs = npcs;
			this.lootIds = lootIds;
			this.banking = banking;
			this.bankArea = bankArea;
		}
	}
	
	ScriptArea SCR = ScriptArea.AL_KHARID;
	CombatZone zone = SCR.zone;
	NpcType[] npcs = SCR.npcs;
	int[] lootValues = new int[SCR.lootIds.length];
	
	private int pickedUpValue = 0;
	
	enum ScriptMode {
		IDLE, MOVING, TARGETTING, IN_COMBAT
	}
	
	ScriptMode mode = ScriptMode.IDLE;
	Npc targetted = null;
	Npc hovering = null;

	public CombatScript() {

	}

	@Override
	public void start() {
		Condition.sleep(1000);
		ctx.input.speed(5);
		ctx.camera.pitch(true);
		
		switch (TRAINING_MODE) {
		case STATS_ATTACK_ID: ctx.combat.style(Style.ACCURATE); break;
		case STATS_STRENGTH_ID: ctx.combat.style(Style.AGGRESSIVE); break;
		case STATS_DEFENCE_ID: ctx.combat.style(Style.DEFENSIVE); break;
		}
		
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

		switch (mode) {
		case IDLE: break;
		case IN_COMBAT: break;
		case MOVING: break;
		case TARGETTING: break;
		}
		
		if (SHOULD_LOOT && loot())
			return;
		
		//if (!inMotion()) {
			if (!inCombat()) {
				System.out.println("Searching...");
				mode = ScriptMode.IDLE;
				
				pickUpArrows();
				
				if (ctx.players.local().healthPercent() < 20) {
					System.out.println("Health too low, logging out.");
					ctx.controller.stop();
					return;
				}
				
				bank();
				
				//System.out.println("Start attack");
				
				attack();
				
				//System.out.println("End attack");
			} else {
				hoverNext();
				antiban();
				mode = ScriptMode.IN_COMBAT;
				run();

				//antiban();
			}
		//}
	}
	
	private void pickUpArrows() {
		if (PICK_UP_ARROW_CLUMPS) {
			BasicQuery<GroundItem> items = ctx.groundItems.select().id(ARROW_ID).select(new Filter<GroundItem>() {
				@Override
				public boolean accept(GroundItem item) {
					return item.stackSize() > 5;
				}
			}).nearest().viewable();
			
			if (!items.isEmpty()) {
				GroundItem item = items.peek();
				
				handlePathToEntity(item.tile(), item);

				
				if (item.interact("Take", item.name())) {
					Condition.wait(new Callable<Boolean>() {
						@Override
						public Boolean call() throws Exception {
							return !item.valid();
						}
					}, 100, 30);
				}
			}
		}
	}
	
	private int pathDistanceTo(Locatable loc) {
		return new LocalDoorPath(ctx, loc.tile(), true).calculatePath().getLength();
	}
	
	private void walkPath(LocalDoorPath path, int distanceToNextClick, boolean hunt) {
		while (!ctx.controller.isStopping() && path.traverse()) {
			Condition.wait(new Callable<Boolean>() {
				@Override
				public Boolean call() throws Exception {
					if (hunt && attemptAttack(getEnemies(true)))
						return true;
					
					return ctx.movement.distance(ctx.movement.destination()) < distanceToNextClick;
				}
			}, 300, 200);
		}
	}
	
	private void walkTo(Tile destination, List<DoorType> doors, int distanceToNextClick, boolean hunt) {
		walkPath(new LocalDoorPath(ctx, destination, doors, true).calculatePath(), distanceToNextClick, hunt);
	}
	
	private void walkTo(Tile destination, DoorType[] doors, int distanceToNextClick, boolean hunt) {
		LocalDoorPath path = new LocalDoorPath(ctx, destination, doors, true).calculatePath();
		walkPath(path, distanceToNextClick, hunt);
	}
	
	private Filter<Npc> hiddenNpcFilter() {
		return new Filter<Npc>() {
			@Override
			public boolean accept(Npc npc) {
				Tile t = npc.tile();
				PlayerQuery<Player> players = ctx.players.select().at(t);
				BasicQuery<Npc> npcs = ctx.npcs.select().at(t);
				return players.isEmpty() && npcs.size() == 1;
			}
		};
	}
	
	private BasicQuery<Npc> getEnemies(boolean randomise) {
		BasicQuery<Npc> enemies = filterNpcs(npcs).select(hiddenNpcFilter()).nearest().select(new Filter<Npc>() {
			@Override
			public boolean accept(Npc npc) {
				int[] b = getBounds(npc.id(), npcs);
				if (b != null) {
					npc.bounds(b);
					
					return true;
				}
				
				return false;
			}
		}).viewable();
		
		enemies.select(new Filter<Npc>() {
			@Override
			public boolean accept(Npc npc) {
				if (npc.healthPercent() > 0) {
					Actor interaction = npc.interacting();
					if (interaction instanceof Player) {
						Player p = (Player)interaction;
						if (!p.interacting().equals(npc)) {
							return true;
						} else {
							if (!p.equals(ctx.players.local()) && isMultiCombat(npc))
								return true;
							
							return false;
						}
					}
					
					return true;
				}
				
				return false;
			}
		});
		
		if (enemies.size() > 1) {
			Map<Npc, Integer> distances = new HashMap<>();
			
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
		
		if (randomise && Math.random() > 0.9) {
			enemies.shuffle();
		}
		
		Player myPlayer = ctx.players.local();
		
		enemies.sort(new Comparator<Npc>() {
			@Override
			public int compare(Npc npc1, Npc npc2) {
				int i1 = npc1.interacting().equals(myPlayer) ? 1 : 0;
				int i2 = npc2.interacting().equals(myPlayer) ? 1 : 0;
				if (i1 > i2)
					System.out.println(i1);
				return i2 - i1;
			}
			
		});
		
		/*
		
		enemies.sort(new Comparator<Npc>() {
			@Override
			public int compare(Npc npc1, Npc npc2) {
				boolean multiCombat = isMultiCombat(npc1);
				boolean multiCombat2 = isMultiCombat(npc2);
				
				if (multiCombat && multiCombat2) {
					boolean interacting1 = npc1.interacting().valid() && !npc1.interacting().equals(myPlayer);
					boolean interacting2 = npc2.interacting().valid() && !npc2.interacting().equals(myPlayer);
					if ((interacting1 && interacting2) || (!interacting1 && !interacting2)) 
						return 0;
					
					if (interacting1 && !interacting2)
						return 1;
					
					if (!interacting1 && interacting2)
						return -1;
				}
				
				return 0;
			}
		});
		*/
		
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
	
	private boolean attackable(Npc npc) {
		return npc != null && npc.valid() && npc.inViewport() && !npc.interacting().valid();
	}
	
	private boolean attemptAttack(BasicQuery<Npc> enemies) {
		Npc target = hovering;
		Player myPlayer = ctx.players.local();

		if (!attackable(target) || (!isMultiCombat(target) && enemies.peek().interacting().equals(myPlayer))) {
			target = getBestTarget(enemies);
		}
		
		final Npc first = target;
		
		handlePathToEntity(first.tile(), first);
		
		if (first.interact("Attack", first.name())) {
			targetted = first;
			hovering = null;
			if (Condition.wait(new Callable<Boolean>() {
				@Override
				public Boolean call() throws Exception {
					return myPlayer.interacting().valid();
				}
			}, 50, 10)) {
				Condition.sleep(300);
				boolean mC = isMultiCombat(target);
				mode = ScriptMode.TARGETTING;
				return Condition.wait(new Callable<Boolean>() {
					@Override
					public Boolean call() throws Exception {
						if (!myPlayer.interacting().valid()) {
							hovering = targetted;
							return true;
						}
						
						hoverNext();
						if (!first.tile().matrix(ctx).reachable() || !first.valid() || first.healthPercent() == 0)
							return true;
						
						if (mC && myPlayer.inMotion())
							return false;
						
						return first.inCombat() || first.interacting().valid();
					}
				}, 300);
			} else {
			}
		}
		
		return false;
	}
	
	private boolean attack() {
		BasicQuery<Npc> enemies = getEnemies(true);
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
			
			Tile t = null;
			if (!enemies.isEmpty())
				t = enemies.peek().tile();
			else
				t = zone.area.getRandomTile();
			
			Tile target = t.derive(-2 + ((int)Math.random() * 4), -2 + ((int)Math.random() * 4));
			
			target = zone.area.getClosestTo(target);
			
			walkTo(target, zone.doors, 6, true);
		} else {
			return attemptAttack(enemies);
		}
		
		return true;
	}
	
	private boolean isMultiCombat(Npc npc) {
		return zone.multiCombat;
	}
	
	private void moveMouseNear(Npc npc) {
		Point p = ctx.input.getLocation();
		Point npcPos = npc.centerPoint();

		if (npcPos.x > 0) {
			ctx.input.move(npcPos.x + (-5 + ((int)(Math.random() * 10))), npcPos.y + (-5 + ((int)(Math.random() * 10))));
		}
	}
	
	private Npc getBestTarget(BasicQuery<Npc> npcs) {
		Iterator<Npc> npcsI = npcs.iterator();
		Player local = ctx.players.local();
		while (npcsI.hasNext()) {
			Npc npc = npcsI.next();
			if (!npc.interacting().valid() || npc.interacting().equals(local))
				return npc;
		}
		
		return npcs.peek();
	}
	
	private boolean hoverNext() {
		Actor a = ctx.players.local().interacting();
		if (a.combatLevel() <= 3 || a.healthPercent() < 30) {
			if (!attackable(hovering) || Math.floor(pathDistanceTo(hovering)) > 6) {
				BasicQuery<Npc> enemies = getEnemies(false).select(new Filter<Npc>() {
					@Override
					public boolean accept(Npc npc) {
						return !npc.equals(targetted);
					}
				});
				if (!enemies.isEmpty()) {
					Npc first = getBestTarget(enemies);
					hovering = first;
					if (ctx.menu.indexOf(Menu.filter("Attack")) < 0) {
						moveMouseNear(first);
					}
					
					return true;
				}
			}
			if (attackable(hovering)) {
				if (ctx.menu.indexOf(Menu.filter("Attack")) < 0) {
					moveMouseNear(hovering);
				}
				return true;
			}
		}
		
		return false;
	}
	
	private static final void sleep(long ms) {
		try {
			Thread.currentThread().sleep(ms);
		} catch (InterruptedException e) {
			e.printStackTrace();
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
			boolean inCombat = inCombat();
			ctx.game.tab(Tab.INVENTORY);
			ItemQuery<Item> items = ctx.inventory.select().action("Eat");
			if (items.count() == 0) {
				bank();
				if (!SCR.banking && TELE_AWAY) {
					System.out.println("TELE AWAY");
					ctx.game.tab(Tab.MAGIC);
					ctx.magic.cast(Magic.Spell.VARROCK_TELEPORT);
					ctx.controller.stop();
					return;
				}
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
		} else if (Math.random() > 0.98) {
			ctx.game.tab(Tab.INVENTORY);
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
		walkTo(zone.area.getRandomTile(), zone.doors, 6, false);
	}

	private void bank() {
		if (SCR.banking && ctx.inventory.select().count() >= 28) {
			walkTo(SCR.bankArea.getRandomTile(), zone.doors, 6, false);
			
			Condition.sleep(2000);

			if (ctx.bank.open())
				if (ctx.bank.depositInventory())
					ctx.bank.withdraw(FOOD_ID, 7);

			walkToCombat();
		}
	}

	private boolean loot() {
		if (ctx.inventory.select().count() == 28)
			return false;
		
		BasicQuery<GroundItem> items = ctx.groundItems.select().id(SCR.lootIds).nearest().viewable();
		
		if (Math.random() > 0.8)
			items.shuffle();
		
		if (items.size() > 0) {
			GroundItem item = items.peek();

			handlePathToEntity(item.tile(), item);

			for (int tries = 0; tries < 3; tries++) {
				if (item.valid() && item.tile().matrix(ctx).reachable()) {
					int id = item.id();
					if (item.interact("Take", item.name())) {
						ctx.game.tab(Tab.INVENTORY);
						if (Condition.wait(new Callable<Boolean>() {
							@Override
							public Boolean call() throws Exception {
								return !item.valid();
							}
						}, 300)) {
							System.out.println("Picked up: " + item.name());
							int[] ids = SCR.lootIds;
							for (int i = 0; i < ids.length; i++) {
								if (id == ids[i]) {
									pickedUpValue += lootValues[i] * item.stackSize();
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
	
	private int getXPFromCombatStyle() {
		switch (TRAINING_MODE) {
		case STATS_ATTACK_ID: return ctx.skills.experience(Constants.SKILLS_ATTACK);
		case STATS_STRENGTH_ID: return ctx.skills.experience(Constants.SKILLS_STRENGTH);
		case STATS_MAGIC_ID: return ctx.skills.experience(Constants.SKILLS_MAGIC);
		case STATS_RANGED_ID: return ctx.skills.experience(Constants.SKILLS_RANGE); 
		case STATS_DEFENCE_ID: return ctx.skills.experience(Constants.SKILLS_DEFENSE);
		}
		return 0;
	}
	
	private String getXPString() {
		if (startXP == -1) {
			startXP = getXPFromCombatStyle();
		}
		
		long runTime = getRuntime();
		int currentExp = getXPFromCombatStyle();
		int expGain = currentExp - startXP;
		int expPh = (int) (3600000d / (long) runTime * (double) (expGain));
		
		return (currentExp - startXP) + " (" + expPh + " / Hr)";
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
			boolean multiCombat = isMultiCombat((Npc) player.interacting());
			boolean b = false;
			long t = System.currentTimeMillis() + (player.inMotion() ? 5000 : 2000);
			while (System.currentTimeMillis() < t) {
				Actor interacting = player.interacting();
				if (!interacting.valid() || interacting.healthPercent() == 0) {
					break;
				}
				
				if (!multiCombat && !interacting.interacting().equals(player))
					break;
				
				if (player.inCombat() || player.animation() != -1) {
					b = true;
					break;
				}
			}
			
			return b;
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
		g2.drawString("Xp: " + getXPString(), 10, 50);
		g2.drawString("Money made: " + pickedUpValue + " gp", 10, 70);
	}
}
