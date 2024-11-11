package arc.weapons.buster;

import arc.StopgapUtils;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.impl.combat.NegativeExplosionVisual;
import com.fs.starfarer.api.impl.combat.RiftCascadeMineExplosion;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.util.Misc;
import data.scripts.util.MagicLensFlare;
import data.scripts.util.MagicRender;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.WaveDistortion;
import org.dark.shaders.light.LightShader;
import org.dark.shaders.light.StandardLight;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class BusterOnHit implements OnHitEffectPlugin {

    static Color color = new Color(100, 50, 210, 255);
    static Color underglow = new Color(27, 45, 51, 255);
    static final Map<CombatEntityAPI, Long> cooldowns = new HashMap<>();
    
    public static void resonantExplosion(
            float chance,
            float baseDamage,
            float damageCoeff,
            Vector2f loc,
            ShipAPI source,
            CombatEntityAPI maybeTarget,
            float radiusVFX,
            float radiusDamage,
            float pitch,
            boolean ignore
    ) {

        if (MathUtils.getRandomNumberInRange(0, 100) > chance) return;

        if (maybeTarget != null) {
            long now = System.currentTimeMillis();
           if (cooldowns.containsKey(maybeTarget)) {
               long canExplodeAgain = cooldowns.get(maybeTarget);
               if (now > canExplodeAgain) {
                   cooldowns.put(maybeTarget, now + MathUtils.getRandomNumberInRange(500,1500));
               } else {
                   return;
               }
           } else {
               cooldowns.put(maybeTarget, now + MathUtils.getRandomNumberInRange(500,1500));
           }
        }

        MagicLensFlare.createSharpFlare(
                Global.getCombatEngine(),
                source,
                loc,
                50f,
                750f,
                source.getFacing(),
                Color.CYAN,
                Color.WHITE
        );

        //TODO: THANKS LOST SECTOR! CREDITS TO THEM!

        //light fx
        StandardLight light = new StandardLight();
        light.setLocation(loc);
        light.setIntensity(2.0f);
        light.setSize(radiusVFX * 3f);
        light.setColor(Color.WHITE);
        light.fadeOut(3f);
        LightShader.addLight(light);

        //distortion fx
        WaveDistortion wave = new WaveDistortion();
        wave.setLocation(loc);
        wave.setSize(radiusVFX * 0.7f);
        wave.setIntensity(source.getMass() * 0.10f);
        wave.fadeInSize(1.2f);
        wave.fadeOutIntensity(0.9f);
        DistortionShader.addDistortion(wave);

        NegativeExplosionVisual.NEParams p = RiftCascadeMineExplosion.createStandardRiftParams("riftcascade_minelayer", 200f);
        p.thickness = 5f;
        p.fadeOut = 0.3f;
        p.underglow = underglow;
        p.noiseMag = 0.5f;
        p.radius *= 0.75f + 0.5f * (float) Math.random();
        Vector2f neLoc = new Vector2f(loc);
        neLoc = Misc.getPointAtRadius(neLoc, p.radius * 0.1f);
        CombatEntityAPI e = Global.getCombatEngine().addLayeredRenderingPlugin(new NegativeExplosionVisual(p));
        e.getLocation().set(neLoc);

        final float flareAngle = MathUtils.getRandomNumberInRange(-15.0f, 15.0f);
        final float flareLength = MathUtils.getRandomNumberInRange(400.0f, 800.0f);

        MagicLensFlare.createSharpFlare(Global.getCombatEngine(), source, loc, 50f, flareLength, flareAngle, new Color(100, 150, 255), new Color(255, 200, 100, 25));

        //diable stuff

        if(MagicRender.screenCheck(0.25f, loc)) {


            for (int i = 0; i < MathUtils.getRandomNumberInRange(1, 3); i++) {
                MagicRender.battlespace(
                        Global.getSettings().getSprite("fx", "blast_shock1"),
                        new Vector2f(loc),
                        new Vector2f(),
                        new Vector2f(radiusVFX * 1.5f, radiusVFX * 1.5f),
                        new Vector2f(radiusVFX * (i + 2) * 2, radiusVFX * (i + 2) * 2),
                        MathUtils.getRandomNumberInRange(0, 360),
                        MathUtils.getRandomNumberInRange(-1, 1),
                        new Color(color.getRed(), color.getGreen(), color.getBlue(), 100),
                        true,
                        0, 0, 2, 1, 0,
                        0,
                        0.01f,
                        0.05f / (float)i,
                        CombatEngineLayers.JUST_BELOW_WIDGETS
                );
            }

            MagicRender.battlespace(
                    Global.getSettings().getSprite("fx", "blast_flash"),
                    new Vector2f(loc),
                    new Vector2f(),
                    new Vector2f(radiusVFX * 1.5f, radiusVFX * 1.5f),
                    new Vector2f(radiusVFX * 6, radiusVFX * 6),
                    MathUtils.getRandomNumberInRange(0, 360),
                    MathUtils.getRandomNumberInRange(-1, 1),
                    color,
                    false,
                    0, 0, 2, 1, 0,
                    0,
                    0.02f,
                    0.2f,
                    CombatEngineLayers.JUST_BELOW_WIDGETS
            );
            MagicRender.battlespace(
                    Global.getSettings().getSprite("fx", "blast_flash"),
                    new Vector2f(loc),
                    new Vector2f(),
                    new Vector2f(radiusVFX * 1, radiusVFX * 1),
                    new Vector2f(radiusVFX * 3, radiusVFX * 3),
                    MathUtils.getRandomNumberInRange(0, 360),
                    MathUtils.getRandomNumberInRange(-1, 1),
                    color,
                    false,
                    4, 1, 0f, 0.5f, 1,
                    0.0f,
                    0.05f,
                    0.6f,
                    CombatEngineLayers.JUST_BELOW_WIDGETS
            );





            for(int i=0; i<MathUtils.getRandomNumberInRange(4, 2 + 12); i++){



                Color boop = new Color(MathUtils.getRandomNumberInRange(150, 200), MathUtils.getRandomNumberInRange(0, 20), MathUtils.getRandomNumberInRange(150,200), MathUtils.getRandomNumberInRange(150,255));

                Global.getCombatEngine().spawnEmpArc(
                        source,
                        MathUtils.getRandomPointInCircle(
                                loc,
                                MathUtils.getRandomNumberInRange(100,500)
                        ),
                        source,
                        maybeTarget != null ? maybeTarget : source,
                        DamageType.ENERGY, 0f, baseDamage * damageCoeff / 4, 500f, "", 60f,boop, Color.BLACK);

                MagicLensFlare.createSharpFlare(
                        Global.getCombatEngine(),
                        source,
                        MathUtils.getRandomPointInCircle(loc, MathUtils.getRandomNumberInRange(200f, 700f)),
                        MathUtils.getRandomNumberInRange(2f, 50f),
                        MathUtils.getRandomNumberInRange(50f, 400f),
                        MathUtils.getRandomNumberInRange(0f, 180f),
                        boop,
                        boop
                );



            }
        }

        if (ignore) {

            StopgapUtils.getShipsWithinRange(loc, radiusDamage * 2).forEachRemaining(ship -> {

                Global.getCombatEngine().applyDamage(ship, loc, baseDamage * damageCoeff * 3f, DamageType.ENERGY, 1000f, true, false, ship, true);



            });
        }
            DamagingExplosionSpec spec = new DamagingExplosionSpec(
                    0.4f,
                    (radiusDamage + 100) * 1.5f,
                    radiusDamage,
                    baseDamage * damageCoeff,
                    baseDamage * damageCoeff * 0.3f,
                    CollisionClass.MISSILE_NO_FF,
                    CollisionClass.MISSILE_NO_FF,
                    2f,
                    2f,
                    2f,
                    20,
                    Color.WHITE,
                    Color.WHITE
            );

            spec.setDamageType(DamageType.ENERGY);
            spec.setShowGraphic(false);



            Global.getCombatEngine().spawnDamagingExplosion(spec, source, loc, false);




        //TODO giant boom

        Global.getSoundPlayer().playSound(
                "arc_degeneracy",
                pitch,
                1.0f,
                loc,
                loc
        );

    }

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (shieldHit) return;
        resonantExplosion(100, projectile.getDamageAmount(), 5, point, projectile.getSource(), target, 1000, 300f, MathUtils.getRandomNumberInRange(1f, 1.2f), false);
    }
}
