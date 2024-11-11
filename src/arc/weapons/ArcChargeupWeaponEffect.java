package arc.weapons;

import arc.util.ARCUtils;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;

public class ArcChargeupWeaponEffect extends ArcBaseEveryFrameWeaponEffect{

    final IntervalUtil cooldownSmokeInterval = new IntervalUtil(0.1f, 0.2f);
    final IntervalUtil chargeBeamInterval = new IntervalUtil(0.025f, 0.05f);
    boolean hasFired = false;

    final Color color;
    final float externalParticleSizeCoef;
    final boolean particlesWhileFiring;

    public ArcChargeupWeaponEffect(Color color, float externalParticleSizeCoef, boolean particlesWhileFiring) {
        this.color = color;
        this.externalParticleSizeCoef = externalParticleSizeCoef;
        this.particlesWhileFiring = particlesWhileFiring;
    }

    public ArcChargeupWeaponEffect() {
        this.color = new Color(255, 255, 255, 255);
        this.externalParticleSizeCoef = 1f;
        this.particlesWhileFiring = true;
    }

    public void advance(final float n, final CombatEngineAPI combatEngineAPI, final WeaponAPI weaponAPI) {

        super.advance(n, combatEngineAPI, weaponAPI);

        final ShipAPI ship = weaponAPI.getShip();
        if (combatEngineAPI.isPaused() || weaponAPI.getShip().getOriginalOwner() == -1 || ship.getFluxTracker().isOverloaded() || weaponAPI.isDisabled()) {
            return;
        }

        List<Vector2f> offsets = weaponAPI.getSpec().getTurretFireOffsets();

        if (weaponAPI.getSlot().isHardpoint()) {
            offsets = weaponAPI.getSpec().getHardpointFireOffsets();
        }



        float particleSizeCoef = ARCUtils.decideBasedOnHullSize(
                ship,
                0.2f,
                0.7f,
                0.7f,
                0.85f,
                1f
        );

        for (Vector2f vec : offsets) {
            float x = vec.x;
            float y = vec.y;

            //COOLDOWN EFFECT
            if (weaponAPI.getChargeLevel() > 0.0f && weaponAPI.getCooldownRemaining() > 0.0f && particlesWhileFiring) {

                cooldownSmokeInterval.advance(n);
                if (cooldownSmokeInterval.intervalElapsed()) {

                    Vector2f vector2f = new Vector2f(weaponAPI.getLocation());
                    Vector2f vector2f2 = new Vector2f(x, y);
                    VectorUtils.rotate(vector2f2, weaponAPI.getCurrAngle(), vector2f2);
                    Vector2f.add(vector2f2, vector2f, vector2f);
                    float n2 = (float) MathUtils.getRandomNumberInRange(10, 30);
                    Global.getCombatEngine().addSmokeParticle(vector2f, vector2f2, n2 * weaponAPI.getChargeLevel(), 1.0f * weaponAPI.getChargeLevel(), 1.0f * weaponAPI.getChargeLevel(), new Color(1.0f, 1.0f, 1.0f, 0.7f));
                }
            }


            //CHARGE UP EFFECT
            if (weaponAPI.isFiring()) {
                float chargeLevel = weaponAPI.getChargeLevel();
                float n5 = 1.0f;

                chargeLevel = Math.max(0, chargeLevel);
                chargeLevel = Math.min(255, chargeLevel);
                Color risingChargeColor = new Color((int)(color.getRed() * chargeLevel), (int)(color.getGreen() * chargeLevel), (int)(color.getBlue() * chargeLevel), (int)(chargeLevel * 255));
                if (!this.hasFired) {
                    //Global.getSoundPlayer().playLoop("beamchargeMeso", weaponAPI, 1.0f, 1.0f, weaponAPI.getLocation(), weaponAPI.getShip().getVelocity());

                    chargeBeamInterval.advance(n);
                    if (chargeBeamInterval.intervalElapsed()) {
                        Vector2f vector2f3 = new Vector2f(weaponAPI.getLocation());
                        Vector2f vector2f4 = new Vector2f(x, y);
                        VectorUtils.rotate(vector2f4, weaponAPI.getCurrAngle(), vector2f4);
                        Vector2f.add(vector2f4, vector2f3, vector2f3);
                        MathUtils.getPoint(weaponAPI.getLocation(), 18.5f, weaponAPI.getCurrAngle());
                        Vector2f velocity = weaponAPI.getShip().getVelocity();
                        combatEngineAPI.addHitParticle(
                                vector2f3,
                                velocity,
                                MathUtils.getRandomNumberInRange(
                                        (20.0f * n5) * particleSizeCoef,
                                        (chargeLevel * n5 * 60.0f + 20.0f) * particleSizeCoef
                                ),
                                MathUtils.getRandomNumberInRange(0.5f, 0.5f + chargeLevel),
                                MathUtils.getRandomNumberInRange(0.1f, 0.1f + chargeLevel / 10.0f),
                                risingChargeColor
                        );
                        combatEngineAPI.addSwirlyNebulaParticle(
                                vector2f3,
                                new Vector2f(0.0f, 0.0f),
                                MathUtils.getRandomNumberInRange(
                                        (20.0f * n5) * particleSizeCoef,
                                        (chargeLevel * n5 * 60.0f + 20.0f) * particleSizeCoef
                                ),
                                1.2f,
                                0.15f,
                                0.0f,
                                0.35f * chargeLevel,
                                risingChargeColor,
                                false
                        );


                        Vector2f randomPointInCircle = MathUtils.getRandomPointInCircle(new Vector2f(), 35.0f * chargeLevel);
                        Vector2f vector2f5 = new Vector2f();
                        Vector2f.sub(vector2f3, new Vector2f(randomPointInCircle), vector2f5);
                        Vector2f.add(velocity, randomPointInCircle, randomPointInCircle);

                        for (int i = 0; i < 5; ++i) {
                            combatEngineAPI.addHitParticle(
                                    vector2f5,
                                    randomPointInCircle,
                                    MathUtils.getRandomNumberInRange(2.0f * particleSizeCoef, (chargeLevel * 2.0f + 2.0f) * particleSizeCoef),
                                    MathUtils.getRandomNumberInRange(0.5f, 0.5f + chargeLevel),
                                    MathUtils.getRandomNumberInRange(0.75f, 0.75f + chargeLevel / 4.0f),
                                    risingChargeColor
                            );
                        }
                    }
                }
                if (chargeLevel == 1.0f) {
                    final Vector2f vector2f6 = new Vector2f(weaponAPI.getLocation());
                    final Vector2f vector2f7 = new Vector2f(x, y);
                    VectorUtils.rotate(vector2f7, weaponAPI.getCurrAngle(), vector2f7);
                    Vector2f.add(vector2f7, vector2f6, vector2f6);
                    MathUtils.getPoint(weaponAPI.getLocation(), 18.5f, weaponAPI.getCurrAngle());
                    final Vector2f velocity2 = weaponAPI.getShip().getVelocity();
                    this.hasFired = true;
                    combatEngineAPI.addHitParticle(
                            vector2f6,
                            velocity2,
                            MathUtils.getRandomNumberInRange((20.0f * n5) * particleSizeCoef, chargeLevel * n5 * 140.0f * particleSizeCoef + 20.0f),
                            MathUtils.getRandomNumberInRange(0.5f, 0.5f + chargeLevel),
                            MathUtils.getRandomNumberInRange(0.1f, 0.1f + chargeLevel / 10.0f),
                            risingChargeColor
                    );
                }
            } else {
                this.hasFired = false;
            }
        }



    }

}
