package arc.weapons.umbra;

import arc.weapons.ArcEveryFrameWeaponEffect;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;



public class UmbraEveryFrame extends ArcEveryFrameWeaponEffect implements OnFireEffectPlugin {

    int increaseIfGreater = 0;

    float pitch = MathUtils.getRandomNumberInRange(0.8f, 1.2f);



    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {

        if (weapon.isDisabled()) return;
        if (weapon.getShip().getFluxTracker().isOverloadedOrVenting()) return;

        if (weapon.getChargeLevel() > 0 && !weapon.isDisabled()) {

            Global.getSoundPlayer().playLoop("arc_umbra_fire", weapon, pitch, 1f, weapon.getLocation(), new Vector2f(), 0, 0.035f);

        }





    }

    public void onFire(final DamagingProjectileAPI damagingProjectileAPI, final WeaponAPI weaponAPI, final CombatEngineAPI combatEngineAPI) {
        if (Math.random() > 0.75) {
            combatEngineAPI.spawnExplosion(damagingProjectileAPI.getLocation(), damagingProjectileAPI.getVelocity(), Color.WHITE, 1.5f, 0.1f);
        }
        else {
            combatEngineAPI.spawnExplosion(damagingProjectileAPI.getLocation(), damagingProjectileAPI.getVelocity(), Color.WHITE, 15.0f, 0.1f);
        }
        combatEngineAPI.addSmoothParticle(damagingProjectileAPI.getLocation(), damagingProjectileAPI.getVelocity(), 45.0f, 1.0f, 0.2f, Color.PINK);
        for (int i = 0; i < MathUtils.getRandomNumberInRange(1,2); ++i) {
            final float n = weaponAPI.getCurrAngle() + ((float)Math.random() * 5 - (float)Math.random() * 5 * 0.5f);
            String s = "arc_umbra_pellet";
            if (weaponAPI.getId().equals("arc_teshuvah_leftarm")) s = "arc_umbra_mini_pellet";

            DamagingProjectileAPI proj = (DamagingProjectileAPI) combatEngineAPI.spawnProjectile(
                    weaponAPI.getShip(),
                    weaponAPI,
                    s,
                    damagingProjectileAPI.getLocation(),
                    n,
                    null
            );
            proj.getVelocity().scale((float)Math.random() * 0.4f + 0.8f);
        }
        Global.getCombatEngine().removeEntity(damagingProjectileAPI);
    }


}
