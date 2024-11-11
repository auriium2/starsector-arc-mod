package arc.shipsystems.whitespace;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import data.scripts.util.MagicRender;
import org.lazywizard.lazylib.FastTrig;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class WhitespaceFlareStats extends BaseShipSystemScript {
    //The opacity of the ship when in phase
    public static final float SHIP_ALPHA_MULT = 0.25f;

    //When does the ship "actually" enter phase? (0.5f would mean it's not in phase until after half of the charge-up. Leave as 0f for vanilla behaviour, which is instant)
    public static final float VULNERABLE_FRACTION = 0f;

    //What is our maximum time mult, before considering officer skills?
    public static final float MAX_TIME_MULT = 10f;
    
    //What is the maximum percentage of flux we can have and still activate the system?
    public static final float FLUX_CAP_FOR_USE = 0.91f;

    //These are for the speed and mobility bonuses: experiment as you see fit
    public static final float MAX_SPEED_MULT = 2.5f;
    public static final float ACCELERATION_MULT = 5f;
    public static final float MAX_TURN_SPEED_MULT = 2f;
    public static final float TURN_ACCELERATION_MULT = 4f;

    //These are just used to display our status messages, ignore them
    private Object STATUSKEY1 = new Object();
    private Object STATUSKEY2 = new Object();


    //Returns the maximum time mult we can achieve, including officer/player skills
    public static float getMaxTimeMult(MutableShipStatsAPI stats) {
        return 1f + (MAX_TIME_MULT - 1f) * stats.getDynamic().getValue(Stats.PHASE_TIME_BONUS_MULT);
    }

    //Determines how and when status messages should be displayed to the player
    private void maintainStatus(ShipAPI playerShip, State state, float effectLevel) {

        //Ensure we have a system
        ShipSystemAPI cloak  = playerShip.getSystem();
        if (cloak == null) return;

        //Status messages shown to the player in phase
        if (effectLevel > VULNERABLE_FRACTION) {
            Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY1,
                    cloak.getSpecAPI().getIconSpriteName(), cloak.getDisplayName(), "mobility increased", false);
            Global.getCombatEngine().maintainStatusForPlayerShip(STATUSKEY2,
                    cloak.getSpecAPI().getIconSpriteName(), cloak.getDisplayName(), "time flow altered", false);
        }
    }

    static final Color WISP_COLOR = new Color(222, 25, 25, 100);


    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {




        //Basic checks to avoid nullpointers and to know if we're the player
        ShipAPI ship = null;
        boolean player = false;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
            player = ship == Global.getCombatEngine().getPlayerShip();
            id = id + "_" + ship.getId();
        } else {
            return;
        }

        //Show status messages if we're the player, depending on the function declared earlier
        if (player) {
            maintainStatus(ship, state, effectLevel);
        }

        //No reason to run stat changes when paused
        if (Global.getCombatEngine().isPaused()) {
            return;
        }

        //If the cloak is off, remove the bonuses we've gotten
        if (state == State.COOLDOWN || state == State.IDLE) {
            unapply(stats, id);
            return;
        }


        ShipSystemAPI cloak = ship.getSystem();




        SpriteAPI sprite = ship.getSpriteAPI();
        float offsetX = sprite.getWidth()/2 - sprite.getCenterX();
        float offsetY = sprite.getHeight()/2 - sprite.getCenterY();

        float trueOffsetX = (float) FastTrig.cos(Math.toRadians(ship.getFacing()-90f))*offsetX - (float)FastTrig.sin(Math.toRadians(ship.getFacing()-90f))*offsetY;
        float trueOffsetY = (float)FastTrig.sin(Math.toRadians(ship.getFacing()-90f))*offsetX + (float)FastTrig.cos(Math.toRadians(ship.getFacing()-90f))*offsetY;


        MagicRender.battlespace(
                Global.getSettings().getSprite(ship.getHullSpec().getSpriteName()),
                new Vector2f(ship.getLocation().getX() + trueOffsetX, ship.getLocation().getY() + trueOffsetY),
                new Vector2f(0, 0),
                new Vector2f(ship.getSpriteAPI().getWidth(), ship.getSpriteAPI().getHeight()),
                new Vector2f(0, 0),
                ship.getFacing() - 90f,
                0f,
                WISP_COLOR,
                true,
                0f,
                0.1f,
                0.1f,
                CombatEngineLayers.BELOW_SHIPS_LAYER
        )
        ;


        //Sets our ship's alpha
        ship.setExtraAlphaMult(1f - (1f - SHIP_ALPHA_MULT) );
        ship.setApplyExtraAlphaToEngines(false);

        //Applies the time mult, both locally and (if we're the player ship) globally
        float shipTimeMult = 1f + (getMaxTimeMult(stats) - 1f);
        stats.getTimeMult().modifyMult(id, shipTimeMult);
        if (player) {
            Global.getCombatEngine().getTimeMult().modifyMult(id, 1f / shipTimeMult);
        } else {
            Global.getCombatEngine().getTimeMult().unmodify(id);
        }

        //Applies mobility bonuses
        stats.getMaxSpeed().modifyMult(id, 1f + (MAX_SPEED_MULT-1f)* effectLevel);
        stats.getMaxTurnRate().modifyMult(id,1f + (MAX_TURN_SPEED_MULT-1f)* effectLevel);
        stats.getTurnAcceleration().modifyMult(id,1f + (TURN_ACCELERATION_MULT-1f)* effectLevel);
        stats.getAcceleration().modifyMult(id,1f + (ACCELERATION_MULT-1f)* effectLevel);
        stats.getDeceleration().modifyMult(id,1f + (ACCELERATION_MULT-1f)* effectLevel);

        //funny
        stats.getEnergyAmmoRegenMult().modifyMult(id, 1.5f);
        stats.getBallisticAmmoRegenMult().modifyMult(id, 1.5f);

        if (ship.getOwner() != 0) {
            stats.getFluxDissipation().modifyMult(id, 1.5f); //fuck player stat
        }

    }


    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship = null;
        //boolean player = false;
        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
            //player = ship == Global.getCombatEngine().getPlayerShip();
            //id = id + "_" + ship.getId();
        } else {
            return;
        }

        //Reverts time mult
        Global.getCombatEngine().getTimeMult().unmodify(id);
        stats.getTimeMult().unmodify(id);

        //Unphases the ship and reverts to normal opacity
        ship.setPhased(false);
        ship.setExtraAlphaMult(1f);

        //Removes the mobility bonuses
		stats.getMaxSpeed().unmodify(id);
		stats.getMaxTurnRate().unmodify(id);
		stats.getTurnAcceleration().unmodify(id);
		stats.getAcceleration().unmodify(id);
		stats.getDeceleration().unmodify(id);
        stats.getFluxDissipation().unmodify(id); //fuck player stat

        stats.getEnergyAmmoRegenMult().unmodify(id );
        stats.getBallisticAmmoRegenMult().unmodify(id);

    }

    //We use another method to add this, so ignore this one
    public StatusData getStatusData(int index, State state, float effectLevel) {
        return null;
    }
    
        //Makes the system unusable while overloading and when flux is too high
    @Override
    public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
        if (ship.getFluxTracker().isOverloadedOrVenting()) {
            return false;
        } else if (ship.getFluxTracker().getCurrFlux() > ship.getFluxTracker().getMaxFlux() * (FLUX_CAP_FOR_USE)) {
            return false;
        }
        return super.isUsable(system, ship);
    }
    
}
