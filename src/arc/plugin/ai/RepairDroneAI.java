package arc.plugin.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.Point;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.HashMap;
import java.util.Random;

//Thanks SunDog - ICE
public class RepairDroneAI extends BaseAI {

    private final ShipAPI mothership;
    private ShipAPI target;
    private final DroneLauncherShipSystemAPI system;
    private Vector2f targetOffset;
    private final Point cellToFix = new Point();
    private final Random rng = new Random();
    private ArmorGridAPI armorGrid;
    private float max;
    private float cellSize;
    private int gridWidth;
    private int gridHeight;
    private int cellCount;
    private int ticksLeftToMx = 0;
    private boolean doingMx = false;
    private boolean returning = false;
    private float dontRestoreAmmoUntil;
    private float targetFacingOffset = Float.MIN_VALUE;

    private static final HashMap<ShipAPI, Float> peakCrRecovery = new HashMap<>();
    private static final HashMap<ShipAPI, Integer> mxAssistTracker = new HashMap<>();
    private static final HashMap<ShipAPI, Float> mxPriorities = new HashMap<>();
    private static final float mxPriorityUpdateFrequency = 2f;
    private static float timeOfMxPriorityUpdate = 2f;

    private static final float REPAIR_RANGE = 200f;
    private static final float ROAM_RANGE = 3000f;
    private static final float REPAIR_AMOUNT = 0.6f;
    private static final float CR_PEAK_TIME_RECOVERY_RATE = 2.5f;
    private static final float FLUX_PER_MX_PERFORMED = 1f;
    private static final float COOLDOWN_PER_OP_OF_AMMO_RESTORED = 35f; // In seconds

    private static final Color SPARK_COLOR = new Color(255, 223, 128);
    private static final String SPARK_SOUND_ID = "system_emp_emitter_loop";
    private static final float SPARK_DURATION = 0.2f;
    private static final float SPARK_BRIGHTNESS = 1f;
    private static final float SPARK_MAX_RADIUS = 7f;
    private static final float SPARK_CHANCE = 0.17f;
    private static final float SPARK_SPEED_MULTIPLIER = 500f;
    private static final float SPARK_VOLUME = 1f;
    private static final float SPARK_PITCH = 1f;

    public static Vector2f toAbsolute(CombatEntityAPI entity, Vector2f point) {
        Vector2f retVal = new Vector2f(point);
        VectorUtils.rotate(retVal, entity.getFacing(), retVal);
        Vector2f.add(retVal, entity.getLocation(), retVal);
        return retVal;
    }

    public static float getArmorPercent(ShipAPI ship) {
        float acc = 0;
        int width = ship.getArmorGrid().getGrid().length;
        int height = ship.getArmorGrid().getGrid()[0].length;

        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                acc += ship.getArmorGrid().getArmorFraction(x, y);
            }
        }

        return acc / (width * height);
    }

    public static Vector2f toRelative(CombatEntityAPI entity, Vector2f point) {
        Vector2f retVal = new Vector2f(point);
        Vector2f.sub(retVal, entity.getLocation(), retVal);
        VectorUtils.rotate(retVal, -entity.getFacing(), retVal);
        return retVal;
    }


    private static void updateMxPriorities(ShipAPI mothership) {
        mxAssistTracker.clear();
        mxPriorities.clear();

        for (ShipAPI ship : Global.getCombatEngine().getShips()) {
            if (ship.isAlive() && ship.isDrone() && ship.getHullSpec().getHullId().contentEquals("arc_chesed")) {
                addMxAssistance(ship.getShipTarget(), 1);
            }
        }

        for (ShipAPI ship : AIUtils.getNearbyAllies(mothership, ROAM_RANGE)) {
            if (!ship.isDrone() && !ship.isFighter()) {
                mxPriorities.put(ship, getMxPriority(ship));
                //Global.getCombatEngine().addFloatingText(s.getLocation(), ((Float)mxPriorities.get(s)).toString(), 40, Color.green, s, 1, 5);
            }
        }

        //        Utils.print(
        //                "   Priority:" + (Float)mxPriorities.get(Global.getCombatEngine().getPlayerShip()) +
        //                "   Armor:" + getArmorPercent(Global.getCombatEngine().getPlayerShip()) +
        //                "   Ordnance:" + getExpendedOrdnancePoints(Global.getCombatEngine().getPlayerShip()) +
        //                "   MxAssist:" + getMxAssistance(Global.getCombatEngine().getPlayerShip()) +
        //                "   PeakCR:" + getSecondsTilCrLoss(Global.getCombatEngine().getPlayerShip()));
        timeOfMxPriorityUpdate = mxPriorityUpdateFrequency + Global.getCombatEngine().getTotalElapsedTime(false);
    }

    private static void addMxAssistance(ShipAPI ship, int amount) {
        if (ship != null) {
            if (!mxAssistTracker.containsKey(ship)) {
                mxAssistTracker.put(ship, amount);
            } else {
                mxAssistTracker.put(ship, (mxAssistTracker.get(ship)) + amount);
            }
        }
    }

    private static float getSecondsTilCrLoss(ShipAPI ship) {
        float secondsTilCrLoss = 0f;

        if (ship.losesCRDuringCombat()) {
            if (peakCrRecovery.containsKey(ship)) {
                secondsTilCrLoss += peakCrRecovery.get(ship);
            }

            secondsTilCrLoss += ship.getHullSpec().getNoCRLossTime() - ship.getTimeDeployedForCRReduction();

        } else {
            secondsTilCrLoss = Float.MAX_VALUE;
        }

        return Math.max(0f, secondsTilCrLoss);
    }

    private static float getMxPriority(ShipAPI ship) {
        float priority = 0f;

        for (WeaponAPI weapon : ship.getUsableWeapons()) {
            if (!weapon.usesAmmo()) continue;
            if (weapon.getAmmoPerSecond() > 0) continue;

            priority += (1f - weapon.getAmmo() / (float) weapon.getMaxAmmo()) * weapon.getSpec().getOrdnancePointCost(null);
        }

        Integer toGet = mxAssistTracker.get(ship);
        if (toGet == null) toGet = 0;

        priority /= (2f + toGet);

        FleetMemberAPI member = ship.getFleetMember();
        float fp = member == null ? 0f : member.getFleetPointCost();

        float totalArmor = 0;
        int width = ship.getArmorGrid().getGrid().length;
        int height = ship.getArmorGrid().getGrid()[0].length;

        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                totalArmor += ship.getArmorGrid().getArmorFraction(x, y);
            }
        }

        float armorPercent = totalArmor / (width * height);

        if (ship.getHullSpec().getHullId().startsWith("arc_")) {

            priority += 1f * (1f - armorPercent) * fp;
        }


        if (ship == Global.getCombatEngine().getPlayerShip()) {
            priority *= 2f;
        }

        return priority;
    }

    @Override
    public void evaluateCircumstances() {
        ticksLeftToMx--;

        if (timeOfMxPriorityUpdate <= Global.getCombatEngine().getTotalElapsedTime(false) || timeOfMxPriorityUpdate > Global.getCombatEngine().getTotalElapsedTime(false) + mxPriorityUpdateFrequency) {
            updateMxPriorities(mothership);
        }

        ShipAPI previousTarget = target;
        setTarget(chooseTarget());

        if (returning) {
            targetOffset = toRelative(target, system.getLandingLocation(self));
        } else if (target != previousTarget || ticksLeftToMx < 1) {
            ticksLeftToMx = 5;

            do {
                targetOffset = MathUtils.getRandomPointInCircle(target.getLocation(), target.getCollisionRadius());
            } while (!CollisionUtils.isPointWithinBounds(targetOffset, target));

            targetOffset = toRelative(target, targetOffset);

            armorGrid = target.getArmorGrid();
            max = armorGrid.getMaxArmorInCell();
            cellSize = armorGrid.getCellSize();
            gridWidth = armorGrid.getGrid().length;
            gridHeight = armorGrid.getGrid()[0].length;
            cellCount = gridWidth * gridHeight;
        }

        if (!target.isPhased() && !returning && MathUtils.getDistance(self, target) < REPAIR_RANGE && mxPriorities.containsKey(target) && mxPriorities.get(target) > 0) {
            performMaintenance();
        } else {
            doingMx = false;
        }

    }

    private void performMaintenance() {
        for (int i = 0; i < (1 + cellCount / 5); ++i) {
            cellToFix.setX(rng.nextInt(gridWidth));
            cellToFix.setY(rng.nextInt(gridHeight));

            if (armorGrid.getArmorValue(cellToFix.getX(), cellToFix.getY()) < max) {
                break;
            }
        }

        Vector2f at = armorGrid.getLocation(cellToFix.getX(), cellToFix.getY());
        for (int i = 0; (i < 10) && !CollisionUtils.isPointWithinBounds(at, target); ++i) {
            at = MathUtils.getRandomPointInCircle(target.getLocation(), target.getCollisionRadius());
        }

        if (dontRestoreAmmoUntil <= Global.getCombatEngine().getTotalElapsedTime(false)) {

            WeaponAPI winner = null;
            float lowestAmmo = 1f;

            for (WeaponAPI weapon : target.getUsableWeapons()) {
                if (!weapon.usesAmmo()) continue;
                if (weapon.getAmmoPerSecond() > 0f) continue;

                float ammo = weapon.getAmmo() / (float) weapon.getMaxAmmo();
                if (ammo < lowestAmmo) {
                    lowestAmmo = ammo;
                    winner = weapon;
                }
            }

            if (winner == null) {
                dontRestoreAmmoUntil = Global.getCombatEngine().getTotalElapsedTime(false) + 1f;
            } else {
                float op = winner.getSpec().getOrdnancePointCost(null);
                int ammoToRestore = (int) Math.max(1, Math.floor(winner.getMaxAmmo() / op));
                ammoToRestore = Math.min(ammoToRestore, winner.getMaxAmmo() - winner.getAmmo());
                winner.setAmmo(winner.getAmmo() + ammoToRestore);
                float basicCooldown = COOLDOWN_PER_OP_OF_AMMO_RESTORED * ((ammoToRestore / (float) winner.getMaxAmmo()) * op);
                float hullPenalty = target.getHullSpec().getHullId().startsWith("arc_") ? 1f : 2f;
                dontRestoreAmmoUntil = Global.getCombatEngine().getTotalElapsedTime(false) + basicCooldown * hullPenalty;
            }

        }

        self.getFluxTracker().setCurrFlux(self.getFluxTracker().getCurrFlux() + FLUX_PER_MX_PERFORMED);

        doingMx = true;
    }

    private void repairArmor() {

        float totalRepaired = 0f;

        for (int x = cellToFix.getX() - 1; x <= cellToFix.getX() + 1; ++x) {
            if (x < 0 || x >= gridWidth) {
                continue;
            }

            for (int y = cellToFix.getY() - 1; y <= cellToFix.getY() + 1; ++y) {
                if (y < 0 || y >= gridHeight) {
                    continue;
                }

                float mult = (3f - Math.abs(x - cellToFix.getX()) - Math.abs(y - cellToFix.getY())) / 3f;

                totalRepaired -= armorGrid.getArmorValue(x, y);
                armorGrid.setArmorValue(x, y, Math.min(max, armorGrid.getArmorValue(x, y) + REPAIR_AMOUNT * mult));
                totalRepaired += armorGrid.getArmorValue(x, y);
            }
        }//bug?
        //((Ship) target).syncWithArmorGridState();
        //((Ship) target).syncWeaponDecalsWithArmorDamage();
        Global.getCombatEngine().addFloatingDamageText(self.getLocation(), totalRepaired, Color.GREEN, self, self);
    }

    private void maintainCR(float amount) {
        if (target.losesCRDuringCombat()) {
            float peakTimeRecovered = 0f;

            if (!peakCrRecovery.containsKey(target)) {
                peakCrRecovery.put(target, 0f);
            } else {
                peakTimeRecovered = peakCrRecovery.get(target);
            }

            float t = target.getTimeDeployedForCRReduction() - peakTimeRecovered - target.getHullSpec().getNoCRLossTime();
            t *= target.getHullSpec().getHullId().startsWith("arc_") ? 1f : 0.5f;

            peakTimeRecovered += Math.max(t, 0f);
            peakTimeRecovered += amount * (CR_PEAK_TIME_RECOVERY_RATE + target.getHullSpec().getCRLossPerSecond());
            peakTimeRecovered = Math.min(peakTimeRecovered, target.getTimeDeployedForCRReduction());

            target.getMutableStats().getPeakCRDuration().modifyFlat("sun_ice_drone_mx_repair", peakTimeRecovered);

            peakCrRecovery.put(target, peakTimeRecovered);
        }
    }

    private ShipAPI chooseTarget() {
        if (needsRefit() || system.getDroneOrders() == DroneLauncherShipSystemAPI.DroneOrders.RECALL) {
            returning = true;
            self.getFluxTracker().setCurrFlux(self.getFluxTracker().getMaxFlux());
            return mothership;
        } else {
            returning = false;
        }

        if (mothership.getShipTarget() != null && mothership.getOwner() == mothership.getShipTarget().getOwner() && !mothership.getShipTarget().isDrone() && !mothership.getShipTarget().isFighter()) {
            return mothership.getShipTarget();
        } else if (system.getDroneOrders() == DroneLauncherShipSystemAPI.DroneOrders.DEPLOY) {
            return mothership;
        }

        float record = 0f;
        ShipAPI leader = null;

        for (ShipAPI s : mxPriorities.keySet()) {

            if (s.getOwner() != this.self.getOwner() || s.isDrone() || s.isFighter()) {
                continue;
            }

            float score = mxPriorities.get(s) / (500f + MathUtils.getDistance(this.self, s));

            if (score > record) {
                record = score;
                leader = s;
            }
        }

        return (leader == null) ? mothership : leader;
    }

    private void setTarget(ShipAPI ship) {
        if (target == ship) {
            return;
        }
        this.self.setShipTarget(target = ship);
    }

    public RepairDroneAI(ShipAPI drone, ShipAPI mothership, DroneLauncherShipSystemAPI system) {
        super(drone);
        this.mothership = mothership;
        this.system = system;

        circumstanceEvaluationTimer.setInterval(0.8f, 1.2f);

        float init = COOLDOWN_PER_OP_OF_AMMO_RESTORED * (0.3f + (float) Math.random() * 0.3f); // initial 10.5~21 CD
        dontRestoreAmmoUntil = Global.getCombatEngine().getTotalElapsedTime(false) + init;
    }

    @Override
    public void advance(float amount) {
        super.advance(amount);

        if (target == null) {
            return;
        }

        if (doingMx) {
            if (target.getHullSpec().getHullId().startsWith("arc_")) {
                repairArmor();
            }

            boolean considerCR = true;


            maintainCR(amount);
        } else if (returning && !self.isLanding() && MathUtils.getDistance(self, mothership) < mothership.getCollisionRadius()) {
            self.beginLandingAnimation(mothership);
        }

        Vector2f to = toAbsolute(target, targetOffset);
        float distance = MathUtils.getDistance(self, to);

        //Snap to ship
        if (doingMx) {
            if (distance < 100f) {
                float f = (1f - distance / 100f) * 0.2f;
                self.getLocation().x = (to.x * f + self.getLocation().x * (2f - f)) / 2f;
                self.getLocation().y = (to.y * f + self.getLocation().y * (2f - f)) / 2f;
                self.getVelocity().x = (target.getVelocity().x * f + self.getVelocity().x * (2f - f)) / 2f;
                self.getVelocity().y = (target.getVelocity().y * f + self.getVelocity().y * (2f - f)) / 2f;
            }
        }

        //TODO pid controller

        if (doingMx && distance < 25f) {
            Global.getSoundPlayer().playLoop(SPARK_SOUND_ID, self, SPARK_PITCH, SPARK_VOLUME, self.getLocation(), self.getVelocity());

            if (targetFacingOffset == Float.MIN_VALUE) {
                targetFacingOffset = self.getFacing() - target.getFacing();
            } else {
                self.setFacing(MathUtils.clampAngle(targetFacingOffset + target.getFacing()));
            }

            Global.getSoundPlayer().playLoop(SPARK_SOUND_ID, self, SPARK_PITCH, SPARK_VOLUME, self.getLocation(), self.getVelocity());
            if (Math.random() < SPARK_CHANCE) {
                Vector2f loc = new Vector2f(self.getLocation());
                loc.x += cellSize * 0.5f - cellSize * (float) Math.random();
                loc.y += cellSize * 0.5f - cellSize * (float) Math.random();

                Vector2f vel = new Vector2f(self.getVelocity());
                vel.x += (Math.random() - 0.5f) * SPARK_SPEED_MULTIPLIER;
                vel.y += (Math.random() - 0.5f) * SPARK_SPEED_MULTIPLIER;

                Global.getCombatEngine().addHitParticle(loc, vel, (SPARK_MAX_RADIUS * (float) Math.random() + SPARK_MAX_RADIUS), SPARK_BRIGHTNESS, SPARK_DURATION * (float) Math.random() + SPARK_DURATION, SPARK_COLOR);
            }
        } else {
            targetFacingOffset = Float.MIN_VALUE;
            float angleDif = MathUtils.getShortestRotation(self.getFacing(), VectorUtils.getAngle(self.getLocation(), to));

            if (Math.abs(angleDif) < 30f) {
                accelerate();
            } else {
                turnToward(to);
                decelerate();
            }
            strafeToward(to);
        }
    }

    @Override
    public boolean needsRefit() {
        return self.getFluxTracker().isOverloaded();
    }
}