package arc.weapons;

import arc.Index;
import arc.hullmod.ARCData;
import arc.util.ARCUtils;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;

import java.awt.*;

import static arc.weapons.ShipLights.*;

public class ShipGlow implements EveryFrameWeaponEffectPlugin {

    //THIS ONE IS IMPORTANT
    //TODO use variable focus for colors

    //private static final float[] COLOR_NORMAL = {255f/255f, 140f/255f, 80f/255f};
    private static final float MIN_OPACITY = 0.2f;
    private static final float SCROLL_TIME = 1f;
    private static final float GLOW_HEIGHT = 0.1f;
    //private static final float TRIGGER_PERCENTAGE = 0.3f;

    private boolean runOnce = false;
    float HEIGHT;
    float TEX_HEIGHT_MULT;
    float time;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {

        if (Global.getCombatEngine().isPaused()) {
            return;
        }
        ShipAPI ship = weapon.getShip();
        if (ship == null) {
            return;
        }

        //get mode
		
        if (!runOnce) {
            weapon.getAnimation().setFrame(1);
            HEIGHT = weapon.getSprite().getHeight();
            float WIDTH = weapon.getSprite().getWidth();
			TEX_HEIGHT_MULT = weapon.getSprite().getTextureHeight();
            weapon.getAnimation().setFrame(0);
            runOnce = true;
        }
		
		time = (time + amount / SCROLL_TIME) % SCROLL_TIME;
		


        //Brightness based on flux under normal conditions
        float fluxBrightness = ship.getFluxTracker().getFluxLevel();
		float systemBrightness = MIN_OPACITY;

        //If we are in overdrive, we glow even more
        if (ship.getSystem().isActive()){
            systemBrightness = Math.max(MIN_OPACITY,Math.min(ship.getSystem().getEffectLevel(), 1f));
        }

        //No glows on wrecks or in refit
        if ( ship.isPiece() || !ship.isAlive() || ship.getOriginalOwner() == -1) {
            fluxBrightness = 0f;
            systemBrightness = 0f;
        }

        //Switches to the proper sprite
        if (fluxBrightness > 0 || systemBrightness > 0) {
            weapon.getAnimation().setFrame(1);
        } else {
            weapon.getAnimation().setFrame(0);
        }
		
		// scrolling


		SpriteAPI sprite=weapon.getSprite();
		float scrollPercent = time / SCROLL_TIME;
				
		sprite.setTexY(ARCUtils.lerp(TEX_HEIGHT_MULT,0f, scrollPercent));
		float height = GLOW_HEIGHT;
		if(scrollPercent < GLOW_HEIGHT){
			height = ARCUtils.lerp(0f,GLOW_HEIGHT,scrollPercent/GLOW_HEIGHT);
		}
		else if(scrollPercent >= 1f - GLOW_HEIGHT){
			height = ARCUtils.lerp(0f,GLOW_HEIGHT,(1f-scrollPercent)/GLOW_HEIGHT);
		}
		
		sprite.setTexHeight(height * TEX_HEIGHT_MULT);
		sprite.setHeight(height * HEIGHT);
		float y = ARCUtils.lerp(0f, HEIGHT, scrollPercent);
		sprite.setCenterY(y - HEIGHT/2);
		
		sprite.setAdditiveBlend();

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

        colorToUse = new Color(colorToUse.getRed(), colorToUse.getGreen(), colorToUse.getBlue(), 255);


        //And finally actually apply the color
        sprite.setColor(colorToUse);

        //Jitter! Jitter based on our maximum jitter distance and our flux level
        //if (systemBrightness > 0 || fluxBrightness > 0.8) {
        //    Vector2f randomOffset = MathUtils.getRandomPointInCircle(new Vector2f(sprite.getWidth() / 2f, sprite.getHeight() / 2f), MAX_JITTER_DISTANCE);
        //    sprite.setCenter(randomOffset.x, randomOffset.y);
        //}
    }
}