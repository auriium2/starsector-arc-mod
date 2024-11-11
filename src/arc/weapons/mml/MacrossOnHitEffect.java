package arc.weapons.mml;

import arc.StopgapUtils;
import arc.plugin.RunnableQueuePlugin;
import arc.util.ARCUtils;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.impl.combat.DisintegratorEffect;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import com.fs.starfarer.api.util.Misc;
import org.dark.shaders.distortion.RippleDistortion;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class MacrossOnHitEffect implements OnHitEffectPlugin {


    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {


        float chance = 0;

        if (target instanceof ShipAPI) {
            chance = ARCUtils.decideBasedOnHullSize(
                    (ShipAPI)target,
                    2f,
                    3f,
                    4f,
                    6f
            );

        } else {
            chance = 3f;
        }

        if (chance > Math.random()) {
            explode(point, projectile, MathUtils.getRandomNumberInRange(0.7f, 2f), 1f);
        }






    }

    public static void explode(Vector2f explosionPoint, DamagingProjectileAPI projectile, float scale, float damageScale) {

        if (projectile.isExpired() || !Global.getCombatEngine().isInPlay(projectile)) return;

        final RippleDistortion ripple = new RippleDistortion(explosionPoint, new Vector2f());
        ripple.setSize(150f * scale);
        ripple.setIntensity(30f);
        ripple.setFrameRate(60f);
        ripple.fadeInSize(0.3f * scale);
        ripple.fadeOutIntensity(0.3f);

        Global.getCombatEngine().spawnExplosion(explosionPoint, new Vector2f(),
                new Color(131, 108, 206,180), (((float) Math.random() * 50f) + 100f) * Math.max(0.1f, 1f), 0.3f);

        //DistortionShader.addDistortion(ripple);
        /*MagicLensFlare.createSharpFlare(
                Global.getCombatEngine(),
                projectile.getSource(),
                explosionPoint,
                3f * scale,
                130f * scale,
                0,
                Color.PINK,
                Color.RED
        );*/

        DamagingExplosionSpec explosionSpec = new DamagingExplosionSpec(
                2f,
                100f * scale,
                50f * scale,
                projectile.getDamageAmount() * 0.8f * damageScale,
                projectile.getDamageAmount() * 1.2f * damageScale,
                CollisionClass.HITS_SHIPS_AND_ASTEROIDS,
                CollisionClass.NONE,
                0.2f,
                1f,
                0.2f,
                3,
                Color.PINK,
                Color.PINK
        );

        explosionSpec.setDamageType(DamageType.HIGH_EXPLOSIVE);
        explosionSpec.setShowGraphic(false);
        explosionSpec.setUseDetailedExplosion(false);

        StopgapUtils.getShipsWithinRange(explosionPoint, 120f).forEachRemaining(ship -> {
            if (ship.getOwner() != projectile.getOwner()) {

                boolean shatterArmor = true;
                if (ship.isPhased()) shatterArmor = false;
                if (ship.getShield() != null
                        && ship.getShield().isOn()
                        && MathUtils.isWithinRange(explosionPoint, ship.getShieldCenterEvenIfNoShield(), ship.getShieldRadiusEvenIfNoShield() )
                        && ship.getShield().isWithinArc(explosionPoint)) {

                    shatterArmor = false;

                }

                if (shatterArmor) {

                    for (int i = 0; i < MathUtils.getRandomNumberInRange(1, 4); i++) {
                        RunnableQueuePlugin.queueTask(() -> {
                            Vector2f armorTobreak = getNearestPointOnCollisionRadius(
                                    new Vector2f(
                                            (float) ((float)explosionPoint.x + Math.random() * 20f),
                                            (float) ((float)explosionPoint.y + Math.random() * 20f)
                                    ), ship);

                            Global.getCombatEngine().spawnExplosion(armorTobreak, ship.getVelocity(),
                                    new Color(151, 126, 255,180), (((float) Math.random() * 40f) + 5f) * Math.max(0.1f, 0.5f), 0.3f);


                            dealArmorDamage(projectile, ship, armorTobreak, 20);
                        }, i * 5 + 2);
                    }
                }

            }



        });
        Global.getCombatEngine().spawnExplosion(explosionPoint, new Vector2f(), Color.PINK, 10f, 2f);
        Global.getCombatEngine().spawnDamagingExplosion(explosionSpec, projectile.getSource(), explosionPoint);

        Global.getSoundPlayer().playSound("arc_macross_explode", 1.0f, 1f, explosionPoint, new Vector2f());
    }
    public static Vector2f getNearestPointOnCollisionRadius(Vector2f point, CombatEntityAPI target){
        Vector2f tPoint = target.getLocation();
        float range = 1f;
        for (int i = 0; i < 2000; i++) {
            tPoint = MathUtils.getPointOnCircumference(point, range, VectorUtils.getAngle(point, target.getLocation()));

            range += 5f;
            if (range>9999f) break;
            if (!CollisionUtils.isPointWithinCollisionCircle(tPoint, target))continue;
            break;
        }
        return tPoint;
    }

    public static void dealArmorDamage(DamagingProjectileAPI projectile, ShipAPI target, Vector2f point, float armorDamage) {
        CombatEngineAPI engine = Global.getCombatEngine();

        ArmorGridAPI grid = target.getArmorGrid();
        int[] cell = grid.getCellAtLocation(point);
        if (cell == null) return;

        int gridWidth = grid.getGrid().length;
        int gridHeight = grid.getGrid()[0].length;

        float damageTypeMult = DisintegratorEffect.getDamageTypeMult(projectile.getSource(), target);

        float damageDealt = 0f;
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                if ((i == 2 || i == -2) && (j == 2 || j == -2)) continue; // skip corners

                int cx = cell[0] + i;
                int cy = cell[1] + j;

                if (cx < 0 || cx >= gridWidth || cy < 0 || cy >= gridHeight) continue;

                float damMult = 1/30f;
                if (i == 0 && j == 0) {
                    damMult = 1/15f;
                } else if (i <= 1 && i >= -1 && j <= 1 && j >= -1) { // S hits
                    damMult = 1/15f;
                } else { // T hits
                    damMult = 1/30f;
                }

                float armorInCell = grid.getArmorValue(cx, cy);
                float damage = armorDamage * damMult * damageTypeMult;
                damage = Math.min(damage, armorInCell);
                if (damage <= 0) continue;

                target.getArmorGrid().setArmorValue(cx, cy, Math.max(0, armorInCell - damage));
                damageDealt += damage;
            }
        }

        if (damageDealt > 0) {
            if (Misc.shouldShowDamageFloaty(projectile.getSource(), target)) {
                engine.addFloatingDamageText(point, damageDealt, new Color(139, 103, 185), target, projectile.getSource());
            }
            target.syncWithArmorGridState();
        }
    }



}
