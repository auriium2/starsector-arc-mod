package arc.weapons.umbra;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.lwjgl.util.vector.Vector2f;

public class UmbraOnHit implements OnHitEffectPlugin {

    @Override
    public void onHit(DamagingProjectileAPI damagingProjectileAPI, CombatEntityAPI target, Vector2f vector2f, boolean b, ApplyDamageResultAPI applyDamageResultAPI, CombatEngineAPI combatEngineAPI) {
        //pop after 10 shots


        if (!b && target instanceof ShipAPI && ((ShipAPI) target).isFighter()) {

            combatEngineAPI.applyDamage(target, vector2f, damagingProjectileAPI.getDamageAmount() * 2f, DamageType.ENERGY, 20f, false, true, damagingProjectileAPI.getSource(), false);
            return;

        }





    }
}
