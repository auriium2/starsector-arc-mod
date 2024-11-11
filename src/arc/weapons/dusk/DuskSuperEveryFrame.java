package arc.weapons.dusk;

import arc.weapons.ArcEveryFrameWeaponEffect;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

public class DuskSuperEveryFrame extends ArcEveryFrameWeaponEffect {

    int increaseIfGreater = 0;

    float pitch = MathUtils.getRandomNumberInRange(0.8f, 1.2f);



    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {

        if (weapon.isDisabled()) return;
        if (weapon.getShip().getFluxTracker().isOverloadedOrVenting()) return;

        if (weapon.getChargeLevel() > 0 && !weapon.isDisabled() && !weapon.isFiring()) {

            Global.getSoundPlayer().playLoop("arc_dusk_spinal_charge", weapon, pitch, 1f, weapon.getLocation(), new Vector2f(), 0, 0.035f);

        }





    }


}
