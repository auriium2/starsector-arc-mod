package arc.deco;

import arc.weapons.ArcChargeupWeaponEffect;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;

import java.awt.*;

public class DeveikuthAnimation extends ArcChargeupWeaponEffect {

    private boolean runOnce=false, hidden=false;
    private SpriteAPI barrel;
    private float barrelwidth=0, recoil=0;

    public DeveikuthAnimation() {
        super(new Color(51, 77, 134, 255), 2, false);
    }

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {


        super.advance(amount, engine, weapon);

        if(engine.isPaused() || hidden || weapon.getShip().getOriginalOwner()==-1){return;}

        if(!runOnce){
            runOnce=true;
            if(weapon.getSlot().isHidden()){
                hidden=true;
                return;
            } else {
                barrel=weapon.getBarrelSpriteAPI();
                barrelwidth=barrel.getWidth()/2;
                return;
            }
        }
        if(weapon.getChargeLevel()==1){
            recoil=1;
        } else {
            recoil=Math.max(0, recoil-(amount));
        }
        float maxRecoil = 5;
        barrel.setCenterX(barrelwidth-(recoil* maxRecoil));
    }
}
