package arc.weapons.sunspear;

import arc.StopgapUtils;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.util.MagicLensFlare;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.Iterator;
import java.util.Random;

public class SunspearBeamEffect implements BeamEffectPlugin {

    final IntervalUtil intervalUtil = new IntervalUtil(0.1f, 0.22f);


    RippleDistortion cachedRippleDistortion = new RippleDistortion();
    boolean shouldCachedRippleDistortionExist = false;

    public SunspearBeamEffect() {
        DistortionShader.addDistortion(cachedRippleDistortion);
        cachedRippleDistortion.setCurrentFrame(0f);
    }

    static final Random RANDOM = new Random();

    @Override
    public void advance(float amount, CombatEngineAPI combatEngineAPI, BeamAPI beamAPI) {

        if (combatEngineAPI.isPaused()) return;

        Vector2f beamTarget = beamAPI.getTo();

        Global.getCombatEngine().addSmoothParticle(
                beamAPI.getTo(),
                new Vector2f(),
                5f,
                10f,
                2f,
                beamAPI.getCoreColor()
        );

        intervalUtil.advance(amount);
        cachedRippleDistortion.advance(amount);
        if (!shouldCachedRippleDistortionExist) {
            shouldCachedRippleDistortionExist = true;
            cachedRippleDistortion.setSize(700f);
            cachedRippleDistortion.setIntensity(150f);
            cachedRippleDistortion.setFrameRate(10f);
            cachedRippleDistortion.fadeInSize(3F);
            cachedRippleDistortion.fadeOutIntensity(150f);

           // Sys.alert("CALLED", "called");
        }

        if (shouldCachedRippleDistortionExist) {
           cachedRippleDistortion.setLocation(beamAPI.getRayEndPrevFrame());
        }

        float brightness = beamAPI.getBrightness();

        if (brightness == 0f) {
            cachedRippleDistortion.setLocation(new Vector2f());
            cachedRippleDistortion.setCurrentFrame(0);
            cachedRippleDistortion.setIntensity(0);
        }

        if (beamAPI.getBrightness() <= 0.6) return;


        MagicLensFlare.createSharpFlare(
                combatEngineAPI,
                beamAPI.getSource(),
                beamAPI.getRayEndPrevFrame(),
                12 * brightness,
                (float) (Math.pow(520, 0.5) * Math.pow(brightness, 2)),
                MathUtils.getRandomNumberInRange(0f, 180f),
                new Color(255,105,50),
                new Color(255,200,160)
        );

        if (beamAPI.getBrightness() != 1f) return;
        if (!intervalUtil.intervalElapsed()) return;


        Vector2f vel = MathUtils.getRandomPointInCircle(beamTarget, 2f);
        float blastDamage = beamAPI.getWeapon().getDerivedStats().getBurstDamage() * 0.05f;

        DamagingExplosionSpec blast = new DamagingExplosionSpec(0.2f,
                160f,
                90f,
                blastDamage,
                blastDamage * 0.2f,
                CollisionClass.PROJECTILE_FF,
                CollisionClass.PROJECTILE_FIGHTER,
                4f,
                4f,
                0.6f,
                40,
                new Color(255,0,0),
                new Color(255,0,0,20));
        blast.setDamageType(DamageType.ENERGY);
        blast.setShowGraphic(true);
        blast.setDetailedExplosionFlashColorCore(new Color(255,0,0));
        blast.setDetailedExplosionFlashColorFringe(new Color(255,0,0,20));
        blast.setUseDetailedExplosion(true);
        blast.setDetailedExplosionRadius(140f);
        blast.setDetailedExplosionFlashRadius(190f);
        blast.setDetailedExplosionFlashDuration(0.4f);

        for (Iterator<ShipAPI> it = StopgapUtils.getShipsWithinRange(beamTarget, 300f); it.hasNext(); ) {
            ShipAPI ship = it.next();
            CombatUtils.applyForce(ship, new Vector2f(RANDOM.nextFloat() * 20, RANDOM.nextFloat() * 20), 900.0f);
        }






        for (int i = 0; i < 2; i++) {

            Vector2f blastPoint = MathUtils.getRandomPointInCircle(beamTarget, 300f);
            combatEngineAPI.spawnDamagingExplosion(blast, beamAPI.getSource(), blastPoint, false);

            combatEngineAPI.addNebulaParticle(
                    blastPoint,
                    new Vector2f(0,0),
                    MathUtils.getRandomNumberInRange(100f, 120f),
                    MathUtils.getRandomNumberInRange(1.5f, 2.0f),
                    0.2f,
                    0.2f,
                    MathUtils.getRandomNumberInRange(0.8f, 1.1f),
                    new Color(255,0,0,120),
                    false
            );

            MagicLensFlare.createSharpFlare(
                    combatEngineAPI,
                    beamAPI.getSource(),
                    MathUtils.getRandomPointInCircle(blastPoint, 100f),
                    10,
                    290,
                    MathUtils.getRandomNumberInRange(0f, 180f),
                    new Color(255,105,50),
                    new Color(255,200,160)
            );
        }

        Global.getSoundPlayer().playSound("explosion_from_damage", 1.3f, 0.6f, beamTarget, vel);
        Global.getSoundPlayer().playSound("hit_heavy_energy", 1.25f, 0.6f, beamTarget, vel);

    }
}
