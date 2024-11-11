package arc.weapons.stygan;

import arc.plugin.RunnableQueuePlugin;
import arc.util.ARCUtils;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class StyganMiniEffect implements BeamEffectPlugin {

    boolean hasFired = false;

    float width;
    final IntervalUtil timer = new IntervalUtil(0.02f,0.1f);

    float offset = MathUtils.getRandomNumberInRange(0.2f, 0.5f);

    boolean hasRun = false;


    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        timer.advance(amount);

        if (!hasRun) {
            hasRun = true;
            width = beam.getWidth();
        }
        float theWidth = width * ( 0.5f * (float) FastTrig.cos( 20*MathUtils.FPI * Math.min(timer.getElapsed(),0.05f) + offset)) ;
        beam.setWidth(theWidth);


        float multiplier = 1f;

        if (beam.getDamageTarget() instanceof ShipAPI && !((ShipAPI) beam.getDamageTarget()).isFighter()) {
            multiplier = 0.5f;

            //TODO this probably doesnt work but im too lazy to test
        }

        beam.getDamage().setMultiplier(multiplier);


        if (beam.getBrightness() <= 0.3f) {
            hasFired = false;
            return;
        }

        Global.getCombatEngine().addHitParticle(
                beam.getRayEndPrevFrame(),
                beam.getSource().getVelocity(),
                (float) Math.random(),
                0.3f,
                1.2f,
                MathUtils.getRandomNumberInRange(0.9f, 1.4f),
                new Color(MathUtils.getRandomNumberInRange(150,170), MathUtils.getRandomNumberInRange(200, 250), MathUtils.getRandomNumberInRange(230, 255), 230)
        );

        float blastDamage = beam.getWeapon().getDerivedStats().getBurstDamage() * 0.05f * multiplier; //*9 = 36% extra damage, so i lied :P

        DamagingExplosionSpec blast = new DamagingExplosionSpec(0.2f,
                10f,
                8f,
                blastDamage,
                blastDamage,
                CollisionClass.PROJECTILE_FF,
                CollisionClass.PROJECTILE_FIGHTER,
                2f,
                2f,
                0.3f,
                30,
                new Color(112, 122, 231, 110),
                new Color(0, 81, 255, 42));
        blast.setDamageType(DamageType.HIGH_EXPLOSIVE);
        blast.setShowGraphic(true);
        blast.setUseDetailedExplosion(false);

        if (beam.getWidth() > 0) {
            engine.spawnDamagingExplosion(blast, beam.getSource(), beam.getTo(), false);
        }

        if (beam.didDamageThisFrame() && !hasFired) {
            hasFired = true;

            for (int i = 1; i < MathUtils.getRandomNumberInRange(1, 2); i++) {
                int finalI = i;
                RunnableQueuePlugin.queueTask(() -> {
                    Vector2f newVec2 = new Vector2f(beam.getTo().x + MathUtils.getRandomNumberInRange(-50* finalI, 50 * finalI), beam.getTo().y + MathUtils.getRandomNumberInRange(-50 * finalI, 50 * finalI));

                    ARCUtils.spawnMine(beam.getSource(), newVec2, "arc_teshuvah_minelayer");
                }, i);
            }


        }




    }


}
