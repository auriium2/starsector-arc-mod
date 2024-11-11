package arc.plugin;

import arc.StopgapUtils;
import arc.util.ARCUtils;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.ProjectileSpecAPI;
import com.fs.starfarer.api.util.Misc;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.WaveDistortion;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;
import java.util.*;

public class PenetrationPlugin implements EveryFrameCombatPlugin {

    static final Map<DamagingProjectileAPI, Vector2f> projToLastPositions = new HashMap<>();


    @Override
    public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {

    }

    @SuppressWarnings("unchecked")
    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        CombatEngineAPI engine = Global.getCombatEngine();

        for (DamagingProjectileAPI proj : Global.getCombatEngine().getProjectiles()) {
            ProjectileSpecAPI spec = proj.getProjectileSpec();
            if (spec == null) continue;
            String id = spec.getId();

            boolean isSuper = id.equals("arc_dusk_super_shot");
            if (!isSuper) continue;

            Vector2f lastPos = projToLastPositions.computeIfAbsent(proj, CombatEntityAPI::getLocation);
            Vector2f currentPos = proj.getLocation();

            Vector2f searchPoint = (Vector2f) Vector2f.add(currentPos, lastPos, null).scale(0.5f);
            Iterator<ShipAPI> targets = StopgapUtils.getShipsWithinRange(searchPoint, (proj.getMoveSpeed() * amount + proj.getProjectileSpec().getWidth()) * 0.2f);

            //THANKS VIC
            targets.forEachRemaining(ship -> {
                if (ship.equals(proj.getSource())) return;
                if (ship.getOwner() == proj.getSource().getOwner()) return;

                List<ShipAPI> alreadyHit = (List<ShipAPI>) proj.getCustomData().get("arc_damaged_already");
                if (alreadyHit == null) alreadyHit = new ArrayList<>();
                if (alreadyHit.contains(ship)) return; //dont do anything if we've already touched this mf


                boolean shieldHit = false;
                Vector2f collisionPoint = null;
                if (ship.getShield() != null
                        && ship.getShield().isOn()
                        && MathUtils.isWithinRange(proj.getLocation(), ship.getShieldCenterEvenIfNoShield(), ship.getShieldRadiusEvenIfNoShield() + (proj.getProjectileSpec().getWidth() * 0.5f))
                        && ship.getShield().isWithinArc(proj.getLocation())) {
                    shieldHit = true;
                    float angle = VectorUtils.getAngle(ship.getShield().getLocation(), proj.getLocation());
                    collisionPoint = MathUtils.getPointOnCircumference(ship.getShield().getLocation(), ship.getShield().getRadius(), angle);
                }

                boolean shouldPenShield = ship.getFluxLevel() > 0.8f;

                if (!shieldHit || shouldPenShield) {
                    if (ship.getExactBounds() == null) {
                        collisionPoint = proj.getLocation();
                    } else {
                        collisionPoint = CollisionUtils.getCollisionPoint(lastPos, currentPos, ship);
                    }
                    if (collisionPoint == null) {
                        if (ship.isFighter()) {
                            if (ship.getOwner() == proj.getOwner()) return;
                            collisionPoint = ship.getLocation();
                        } else if (CollisionUtils.isPointWithinBounds(proj.getLocation(), ship)) {
                            collisionPoint = proj.getLocation();
                        }
                    }
                }

                if (collisionPoint == null) {
                    return;
                }

                //we have touched them
                alreadyHit.add(ship);
                proj.setCustomData("arc_damaged_already", alreadyHit);

                //Yes, i am aware that this will cause the dusk to do 1 + the multiplied amount. No, i do not care.
                float multiplier = ARCUtils.decideBasedOnHullSize(ship, 1f, 1f, 1.5f, 2f, 2.5f);

                Global.getCombatEngine().applyDamage(ship,
                        collisionPoint,
                        proj.getDamage().getDamage() * multiplier,
                        shieldHit ? DamageType.KINETIC : DamageType.ENERGY,
                        proj.getEmpAmount(),
                        shouldPenShield,
                        false,
                        proj.getSource());
                float force = (proj.getDamageAmount() * 0.1f);
                CombatUtils.applyForce(proj, proj.getVelocity(), force);

                WaveDistortion wave = new WaveDistortion(collisionPoint, new Vector2f(0, 0));
                wave.setIntensity(6f);
                wave.setSize(300f);
                wave.flip(true);
                wave.setLifetime(0f);
                wave.fadeOutIntensity(0.35f);
                wave.setLocation(proj.getLocation());
                DistortionShader.addDistortion(wave);



                engine.spawnExplosion(
                        collisionPoint,
                        new Vector2f(0, 0),
                        new Color(255, 255, 255, 255),
                        40f,
                        0.5f);

                engine.spawnExplosion(
                        collisionPoint,
                        new Vector2f(0, 0),
                        new Color(0, 255, 225, 125),
                        80f,
                        0.75f);

                engine.addSmoothParticle(
                        collisionPoint,
                        new Vector2f(),
                        500,
                        2f,
                        0.5f,
                        new Color(158, 255, 255, 125));

                engine.addHitParticle(
                        collisionPoint,
                        new Vector2f(),
                        800,
                        2f,
                        0.35f,
                        new Color(158, 255, 255, 255));

                engine.addHitParticle(
                        collisionPoint,
                        new Vector2f(),
                        1200,
                        2f,
                        0.2f,
                        new Color(195, 255, 255, 255));


                Vector2f nebulaSpeed1 = (Vector2f) Misc.getUnitVectorAtDegreeAngle(proj.getAngularVelocity() + MathUtils.getRandomNumberInRange(0f, 90f)).scale(MathUtils.getRandomNumberInRange(5f, 15f));
                Vector2f nebulaSpeed2 = (Vector2f) Misc.getUnitVectorAtDegreeAngle(proj.getAngularVelocity() + MathUtils.getRandomNumberInRange(90f, 180f)).scale(MathUtils.getRandomNumberInRange(5f, 15f));
                Vector2f nebulaSpeed3 = (Vector2f) Misc.getUnitVectorAtDegreeAngle(proj.getAngularVelocity() + MathUtils.getRandomNumberInRange(180f, 270f)).scale(MathUtils.getRandomNumberInRange(5f, 15f));
                Vector2f nebulaSpeed4 = (Vector2f) Misc.getUnitVectorAtDegreeAngle(proj.getAngularVelocity() + MathUtils.getRandomNumberInRange(270f, 360f)).scale(MathUtils.getRandomNumberInRange(5f, 15f));
                Vector2f nebulaSpeed5 = (Vector2f) Misc.getUnitVectorAtDegreeAngle(proj.getAngularVelocity()).scale(0f);

                Global.getCombatEngine().addNebulaSmokeParticle(collisionPoint, nebulaSpeed1, 80f, 2.5f, 0.2f, 0.2f, (2f + MathUtils.getRandomNumberInRange(-0.5f, 0.5f)), new Color(153, 95, 67, 125));
                Global.getCombatEngine().addNebulaSmokeParticle(collisionPoint, nebulaSpeed2, 80f, 2.5f, 0.2f, 0.2f, (2f + MathUtils.getRandomNumberInRange(-0.5f, 0.5f)), new Color(83, 51, 25, 125));
                Global.getCombatEngine().addNebulaSmokeParticle(collisionPoint, nebulaSpeed3, 80f, 2.5f, 0.2f, 0.2f, (2f + MathUtils.getRandomNumberInRange(-0.5f, 0.5f)), new Color(111, 56, 7, 125));
                Global.getCombatEngine().addNebulaSmokeParticle(collisionPoint, nebulaSpeed4, 80f, 2.5f, 0.2f, 0.2f, (2f + MathUtils.getRandomNumberInRange(-0.5f, 0.5f)), new Color(134, 107, 53, 125));
                Global.getCombatEngine().addNebulaSmokeParticle(collisionPoint, nebulaSpeed5, 80f, 2f, 0.2f, 0.2f, 0.4f, new Color(172, 255, 230, 173));

                if (shieldHit && !shouldPenShield && !ship.isFighter()) {
                    Global.getCombatEngine().removeEntity(proj);
                }


            });

            projToLastPositions.computeIfPresent(proj, (p,loc) -> currentPos);

        }
    }

    @Override
    public void renderInWorldCoords(ViewportAPI viewport) {

    }

    @Override
    public void renderInUICoords(ViewportAPI viewport) {

    }

    @Override
    public void init(CombatEngineAPI engine) {

    }
}
