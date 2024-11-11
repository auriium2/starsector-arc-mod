// Credits to Nia Tahl for base code

package arc.shipsystems.overdrive;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;

public class Overdrive extends BaseShipSystemScript {

    private static final float SPEED_BOOST = 250f;
	private static final float TURNRATE_MULT = 1.2f;
	private static final float DAMAGE_MULT = 1.5f;
	private static final float TIME_MULT = 2.0f;

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		stats.getTimeMult().modifyMult(id, TIME_MULT);
		stats.getEnergyWeaponFluxCostMod().modifyMult(id, 1f / (1f + effectLevel));
		stats.getEnergyRoFMult().modifyMult(id, 1f + effectLevel);
		stats.getBallisticRoFMult().modifyMult(id, 1f + effectLevel);
		stats.getBallisticWeaponFluxCostMod().modifyMult(id, 1f / (1f + effectLevel));
		stats.getMaxSpeed().modifyFlat(id, SPEED_BOOST * effectLevel);
		stats.getAcceleration().modifyFlat(id, SPEED_BOOST * 1.5f * effectLevel);
		stats.getMaxTurnRate().modifyMult(id,TURNRATE_MULT*effectLevel);
        stats.getEmpDamageTakenMult().modifyMult(id, DAMAGE_MULT);
        stats.getArmorDamageTakenMult().modifyMult(id, DAMAGE_MULT);
        stats.getHullDamageTakenMult().modifyMult(id, DAMAGE_MULT);		

    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {

        stats.getEnergyRoFMult().unmodify(id);
        stats.getEnergyWeaponFluxCostMod().unmodify(id);
        stats.getBallisticRoFMult().unmodify(id);
        stats.getBallisticWeaponFluxCostMod().unmodify(id);
        stats.getMaxSpeed().unmodify(id);
        stats.getAcceleration().unmodify(id);
		stats.getMaxTurnRate().unmodify(id);
		
        stats.getEmpDamageTakenMult().unmodify(id);
        stats.getHullDamageTakenMult().unmodify(id);
        stats.getArmorDamageTakenMult().unmodify(id);	
		stats.getTimeMult().unmodify(id);		
    }
}
