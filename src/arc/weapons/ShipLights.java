package arc.weapons;

import arc.Index;
import arc.hullmod.ARCData;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;

import java.awt.*;

public class ShipLights implements EveryFrameWeaponEffectPlugin {

    static final Color ASSAULT = new Color(255, 255, 255, 255);
    static final Color REPAIR = new Color(136, 234, 71, 218);
    static final Color BERSERK = new Color(96, 22, 22, 204);

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (Global.getCombatEngine().isPaused()) {
            return;
        }

        ShipAPI ship = weapon.getShip();
        if (ship == null) {
            return;
        }

        //Brightness based on flux under normal conditions
        float currentBrightness = Math.min(ship.getFluxLevel() + 0.1f, 1.0f);
		boolean isVenting = ship.getFluxTracker().isVenting();
		if(isVenting){
			currentBrightness = 0.5f;
		}
		if(ship.getFluxTracker().isOverloaded()){
			currentBrightness = 0f;
		}
		
        //No glows on wrecks or in refit
        if ( ship.isPiece() || !ship.isAlive() || ship.getOriginalOwner() == -1) {
            currentBrightness = 0f;
        }

        //Switches to the proper sprite
        if (currentBrightness > 0) {
            weapon.getAnimation().setFrame(1);
        } else {
            weapon.getAnimation().setFrame(0);
        }

        ARCData data = (ARCData) ship.getCustomData().get(Index.ARC_DATA);

        if (data == null) return;

        Color colorToUse = null;

        switch (data.mode) {

            case ASSAULT:
                colorToUse = ASSAULT; break;
            case REPAIR:
                colorToUse = REPAIR; break;
            case BERSERK:
                colorToUse = BERSERK; break;

        }

        colorToUse = new Color(colorToUse.getRed(), colorToUse.getGreen(), (int)colorToUse.getBlue(), (int)(255 * Math.min(1f, currentBrightness)));



        //And finally actually apply the color
        weapon.getSprite().setColor(colorToUse);
    }
}