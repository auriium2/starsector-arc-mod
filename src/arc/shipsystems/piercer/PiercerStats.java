package arc.shipsystems.piercer;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

//stolen from Ice
public class PiercerStats extends BaseShipSystemScript {
    //boolean within = false;
    final IntervalUtil damageEvery = new IntervalUtil(0.05f, 0.1f); //stop lagging


    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {

        if (state == State.IN) {
            ShipAPI ship = (ShipAPI) stats.getEntity();
            ship.getEngineController().getShipEngines().forEach(ShipEngineControllerAPI.ShipEngineAPI::repair);
        }

        if (state == State.IDLE) {
            stats.getArmorDamageTakenMult().modifyMult("me", 0.3f);
            stats.getHullDamageTakenMult().modifyMult("me", 0.3f);
            ShipAPI ship = (ShipAPI) stats.getEntity();

            ship.setJitterUnder(ship, Color.BLUE, 2f, 4, 3);
        } else {
            stats.getArmorDamageTakenMult().unmodify("me");
            stats.getHullDamageTakenMult().unmodify("me");
        }

        if (state == ShipSystemStatsScript.State.OUT) {
            stats.getMaxSpeed().unmodify(id);
            stats.getMaxTurnRate().unmodify(id);

        } else {
            ShipAPI ship = (ShipAPI) stats.getEntity();

            ship.setJitter(ship, Color.CYAN, 7f, 4, 5f);

            stats.getMaxSpeed().modifyMult(id, 10f * effectLevel); //this is silly lol
            stats.getAcceleration().modifyFlat(id, 250f * effectLevel);
            stats.getMaxTurnRate().modifyMult(id, 1f - 0.5f * effectLevel);
            stats.getTurnAcceleration().modifyMult(id, 1f - 0.5f * effectLevel);


            WeaponAPI drill = ship.getAllWeapons().get(8);
            ship.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.DO_NOT_VENT);

            if (drill.isDisabled()) {
                ship.getFluxTracker().forceOverload(1f);
                return;
            }

            ship.setMass(10000 * effectLevel); //TODO LOL



            Vector2f at = drill.getLocation();
            at.x += (float) Math.random() * 40f - 20f;
            at.y += (float) Math.random() * 40f - 20f;
            for (CombatEntityAPI ast : Global.getCombatEngine().getAsteroids()) {
                if (MathUtils.getDistance(ast.getLocation(), at) < ast.getCollisionRadius() * 0.5f) {
                    Global.getCombatEngine().applyDamage(ast, at, ast.getHitpoints(), drill.getSpec().getDamageType(), 0, true, true, ship);
                }
            }



            for (ShipAPI target : AIUtils.getNearbyEnemies(ship, 100)) {
                if (target.isPhased()) {
                    continue;
                }
                if (CollisionUtils.isPointWithinBounds(at, target)) {
                    ship.getHullSpec().getEngineSpec().getMaxSpeed();


                    float coef = (ship.getVelocity().lengthSquared() / 600 / 600 );
                    float damage = ship.getVelocity().length() * coef * 0.4f; //4% less pls


                    CombatUtils.applyForce(target, (Vector2f) ship.getVelocity().scale(0.98f), 40f);
                    Global.getCombatEngine().applyDamage(target, at, damage, drill.getSpec().getDamageType(), 0f, true, true, ship);

                    stats.getWeaponDamageTakenMult().modifyMult(id, 0f);
                    stats.getEngineDamageTakenMult().modifyMult(id, 0f);

                    Global.getCombatEngine().spawnExplosion(at, // Location
                            target.getVelocity(), // Velocity
                            new Color(MathUtils.getRandomNumberInRange(200, 255), 100, MathUtils.getRandomNumberInRange(100, 200)), // Color
                            20f + (float) Math.random() * 50f, // Size
                            0.5f + (float) Math.random() * 0.5f); // Duration

                    if (coef < 0.6f) {
                        Global.getSoundPlayer().playSound("hit_heavy_energy", Math.max(4 - (4*coef), 0.6f),  Math.max(coef-0.2f,0), ship.getLocation(), target.getVelocity());

                    } else {
                        Global.getSoundPlayer().playSound("collision_ships", Math.max(4 - (4*coef), 0.6f), Math.max(coef-0.2f,0), ship.getLocation(), target.getVelocity());

                    }


                } else {
                    stats.getWeaponDamageTakenMult().unmodify();
                    stats.getEngineDamageTakenMult().unmodify();
                }
            }
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {

        stats.getEntity().setMass(45); //TODO dont hardcode this lol
        stats.getMaxSpeed().unmodify(id);
        stats.getMaxTurnRate().unmodify(id);
        stats.getTurnAcceleration().unmodify(id);
        stats.getAcceleration().unmodify(id);
        stats.getDeceleration().unmodify(id);
        stats.getWeaponDamageTakenMult().unmodify(id);
        stats.getEngineDamageTakenMult().unmodify(id);
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        return new StatusData("lol", false);
    }
}