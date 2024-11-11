package arc.shipsystems.piercer;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.WeaponUtils;
import org.lwjgl.util.vector.Vector2f;

public class PiercerAI implements ShipSystemAIScript {

    static final float FLUX_CAP_COEF = 0.1f;
    static final float INTERVAL_TO_SECOND_COEF = 1f;


    final IntervalUtil timer = new IntervalUtil(0.3f, 0.3f);


    ShipSystemAPI system;
    ShipAPI ship;
    ShipAPI victim;
    float timeOfTargetAquisition;

    WeaponAPI drill; //WHAT THE FUCK UGHHHHH

    private float getScore(ShipAPI self, ShipAPI victim) {
        if (!victim.isAlive()) return 0f;
        if (victim.isFighter()) return 0f;

        //size * distance to prioritize bigger targets who are farther
        return Math.max(0f, (victim.getCollisionRadius()) * MathUtils.getDistance(self, victim));
    }

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.system = system;
        for (WeaponAPI weaponAPI : ship.getAllWeapons()) {
            if (weaponAPI.getId().contentEquals("arc_mura_drill")) {
                drill = weaponAPI;
            }
        }
    }

    boolean hasHurtedThisTarget = false;

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {


        timer.advance(amount);
        float time = Global.getCombatEngine().getTotalElapsedTime(false);

        //when not trying to fuck someone, update the next best person to fuck
        if (!system.isActive() && timer.intervalElapsed()) {
            float score = 0f;

            victim = null;
            for (ShipAPI candidate : WeaponUtils.getEnemiesInArc(drill)) {
                float newScore = getScore(ship, candidate);

                if (newScore > 0f && newScore > score) {
                    victim = candidate;
                    hasHurtedThisTarget = false; //we are going for another bombing run
                    score = newScore;
                    timeOfTargetAquisition = time;
                }
            }
        }

        if (!system.isActive() && victim != null) {
            float fluxPerInterval = 50 * INTERVAL_TO_SECOND_COEF; //200 flux per second to interval
            float distancePerInterval = ship.getMaxSpeed() * INTERVAL_TO_SECOND_COEF;
            float distanceToTarget = MathUtils.getDistance(ship.getLocation(), victim.getLocation());

            float intervalsRequiredToMove = distanceToTarget / distancePerInterval;
            float fluxGenerated = fluxPerInterval * intervalsRequiredToMove;


            if (fluxGenerated + ship.getCurrFlux() < ship.getMaxFlux()) {
                ship.useSystem();
            }

        }


        if (system.isActive()) { //run away at high flux



            if (ship.getFluxLevel() > 0.9f) {
                ship.useSystem();
            } else if (ship.getFluxLevel() > 0.7f) {
                ship.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.DO_NOT_PURSUE);
                ship.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.BACK_OFF);
                ship.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.BACK_OFF_MIN_RANGE, 9, 400f);
            } else {
                Vector2f to = victim.getLocation();

                float angleDif = MathUtils.getShortestRotation(ship.getFacing(), VectorUtils.getAngle(ship.getLocation(), to));
                ShipCommand direction = (angleDif > 0f) ? ShipCommand.TURN_LEFT : ShipCommand.TURN_RIGHT;
                ship.giveCommand(direction, to, 0);
            }

            if (MathUtils.isPointWithinCircle(ship.getLocation(), victim.getLocation(), victim.getCollisionRadius())) {
                hasHurtedThisTarget = true;
            }



        }

    }



}
