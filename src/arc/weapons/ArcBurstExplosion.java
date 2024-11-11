package arc.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ProximityExplosionEffect;
import com.fs.starfarer.api.impl.combat.NegativeExplosionVisual;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public abstract class ArcBurstExplosion extends NegativeExplosionVisual.NEParams implements ProximityExplosionEffect {


    protected final Color color;
    protected final float size;


    public ArcBurstExplosion(Color color) {
        this.color = color;
        this.size = 200f;
    }

    public ArcBurstExplosion(Color color, float size) {
        this.color = color;
        this.size = size;
    }

    public void onExplosion(final DamagingProjectileAPI damagingProjectileAPI, final DamagingProjectileAPI damagingProjectileAPI2) {
        final CombatEngineAPI combatEngine = Global.getCombatEngine();
        combatEngine.addSmoothParticle(
                damagingProjectileAPI2.getLocation(),
                new Vector2f(0.0f, 0.0f),
                size,
                2.0f,
                0.07f,
                Color.WHITE
        );

        combatEngine.addHitParticle(
                damagingProjectileAPI2.getLocation(),
                new Vector2f(0.0f, 0.0f)
                , size - 50f,
                1.5f,
                0.4f,
                color
        );

        combatEngine.addHitParticle(
                damagingProjectileAPI2.getLocation(),
                new Vector2f(0.0f, 0.0f),
                (size / 2f) - 25f,
                1.5f,
                0.4f,
                Color.WHITE
        );

    }

/*
    public static void spawnStandardRift(final DamagingProjectileAPI damagingProjectileAPI, final NegativeExplosionVisual.NEParams neParams) {
        final CombatEngineAPI combatEngine = Global.getCombatEngine();
        damagingProjectileAPI.addDamagedAlready((CombatEntityAPI)damagingProjectileAPI.getSource());
        final CombatEntityAPI combatEntityAPI = null;
        for (int i = 0; i < 2; ++i) {
            final NegativeExplosionVisual.NEParams standardRiftParams;
            final NegativeExplosionVisual.NEParams neParams2 = standardRiftParams = createStandardRiftParams("armaa_valkazard_torso_chaosburst_minelayer", 10.0f);
            standardRiftParams.radius *= 0.75f + 0.5f * (float)Math.random();
            neParams2.withHitGlow = (combatEntityAPI == null);
            final Vector2f pointAtRadius = Misc.getPointAtRadius(new Vector2f((ReadableVector2f)damagingProjectileAPI.getLocation()), neParams2.radius * 0.4f);
            final CombatEntityAPI addLayeredRenderingPlugin = combatEngine.addLayeredRenderingPlugin((CombatLayeredRenderingPlugin)new NegativeExplosionVisual(neParams2));
            addLayeredRenderingPlugin.getLocation().set((ReadableVector2f)pointAtRadius);
            if (combatEntityAPI != null) {
                final float distance = Misc.getDistance(combatEntityAPI.getLocation(), pointAtRadius);
                final Vector2f unitVectorAtDegreeAngle = Misc.getUnitVectorAtDegreeAngle(Misc.getAngleInDegrees(pointAtRadius, combatEntityAPI.getLocation()));
                unitVectorAtDegreeAngle.scale(distance / (neParams2.fadeIn + neParams2.fadeOut) * 0.7f);
                addLayeredRenderingPlugin.getVelocity().set((ReadableVector2f)unitVectorAtDegreeAngle);
            }
        }
    }

    public static NegativeExplosionVisual.NEParams createStandardRiftParams(final String s, final float n) {
        Color glowColor = new Color(129, 55, 220, 155);
        final Object projectileSpec = Global.getSettings().getWeaponSpec(s).getProjectileSpec();
        if (projectileSpec instanceof MissileSpecAPI) {
            glowColor = ((MissileSpecAPI)projectileSpec).getGlowColor();
        }
        return createStandardRiftParams(glowColor, n);
    }

    public static NegativeExplosionVisual.NEParams createStandardRiftParams(final Color color, final float radius) {
        final NegativeExplosionVisual.NEParams neParams = new NegativeExplosionVisual.NEParams();
        neParams.hitGlowSizeMult = 0.75f;
        neParams.spawnHitGlowAt = 0.0f;
        neParams.noiseMag = 1.0f;
        neParams.fadeIn = 0.1f;
        neParams.underglow = new Color(82, 25, 225, 100);
        neParams.withHitGlow = true;
        neParams.radius = radius;
        neParams.color = color;
        return neParams;
    }*/

}
