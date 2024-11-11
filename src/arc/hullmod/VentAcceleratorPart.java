package arc.hullmod;

import arc.Index;
import arc.util.ARCUtils;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;

/**
 * Handles the "vent acceleration" effect
 */
public class VentAcceleratorPart implements IHullmodPart<ARCData> {

    public static final float BASE_VENT_BONUS = 400f;
    public static final float MAX_VENT_BONUS = -30f;
    static final String VENT_BOOST = "arc_vent_boost";

    @Override
    public void advanceSafely(CombatEngineAPI engineAPI, ShipAPI shipAPI, float timestep, ARCData customData) {





        float ventMult = ARCUtils.clamp(
                MAX_VENT_BONUS,
                BASE_VENT_BONUS,
                ARCUtils.remap(
                        0f,
                        1.6f,
                        BASE_VENT_BONUS,
                        MAX_VENT_BONUS,
                        shipAPI.getFluxLevel()
                )
        );


        if (shipAPI.getFluxTracker().isVenting()) return;
        if (customData.ventAbilityCooldown > 0) {
            shipAPI.getMutableStats().getVentRateMult().unmodify(VENT_BOOST);

            return;
        }


        shipAPI.getMutableStats().getVentRateMult().modifyPercent(VENT_BOOST, ventMult);

        if (shipAPI == engineAPI.getPlayerShip()) {
            engineAPI.maintainStatusForPlayerShip(
                            VENT_BOOST,
                            "graphics/icons/hullsys/temporal_shell.png",
                            "Microshunt Whitespace Sheath",
                            "Vent rate at " + shipAPI.getMutableStats().getVentRateMult().getModifiedValue() + "%", false
                    );
        }


    }

    @Override
    public boolean hasData() {
        return true;
    }

    @Override
    public boolean makesNewData() {
        return true;
    }

    @Override
    public ARCData makeNew() {
        return new ARCData();
    }

    @Override
    public String makeKey() {
        return Index.ARC_DATA;
    }
}
