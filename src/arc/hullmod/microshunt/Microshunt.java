package arc.hullmod.microshunt;

import arc.VentType;
import arc.hullmod.ARCBaseHullmod;
import arc.hullmod.IHullmodPart;
import arc.hullmod.microshunt.ai.TurnIntoIEDAIPart;
import arc.util.ARCUtils;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;
import java.util.*;

import static arc.hullmod.VentAcceleratorPart.BASE_VENT_BONUS;
import static arc.hullmod.VentAcceleratorPart.MAX_VENT_BONUS;

//offensive core
public class Microshunt extends ARCBaseHullmod {



	static final Map<HullSize,Float> FLUX_REDUCTION = new HashMap<>(); static {
		FLUX_REDUCTION.put(HullSize.FIGHTER, -50f);
		FLUX_REDUCTION.put(HullSize.FRIGATE, -55f);
		FLUX_REDUCTION.put(HullSize.DESTROYER, -45f);
		FLUX_REDUCTION.put(HullSize.CRUISER, -40f);
		FLUX_REDUCTION.put(HullSize.CAPITAL_SHIP, -35f);
	}
	static final Map<HullSize, Float> MAX_TIME_MULT = new HashMap<>(); static {
		MAX_TIME_MULT.put(HullSize.FIGHTER, 1.8f);
		MAX_TIME_MULT.put(HullSize.FRIGATE, 1.6f);
		MAX_TIME_MULT.put(HullSize.DESTROYER, 1.4f);
		MAX_TIME_MULT.put(HullSize.CRUISER, 1.3f);
		MAX_TIME_MULT.put(HullSize.CAPITAL_SHIP, 1.1f);
	}
	
	static final Set<String> BAD_HULLMODS = new HashSet<>(); static {
		BAD_HULLMODS.add("safetyoverrides");
		BAD_HULLMODS.add("heavyarmor");
		BAD_HULLMODS.add("fluxcoil");
		BAD_HULLMODS.add("fluxdistributor");
		BAD_HULLMODS.add("fluxbreakers");
		BAD_HULLMODS.add("eis_aquila");
		BAD_HULLMODS.add("eis_vanagloria");
		BAD_HULLMODS.add("converted_hangar");
		BAD_HULLMODS.add("roider_fighterClamps");

	}

	static final float OVERLOAD_TIME = 40f;
	static final float UNFOLD_RATE = 2.2f;

	static final float SUPPLY = 150f;
	static final float REPAIR_TIME = -15f;
	static final float SHIELD_EFF = 10f;
	static final float HULL_MULTI = 3.5f; //reduce by this

	static final String BAD_HULLMOD_NOTIFICATION_SOUND = "cr_allied_critical";

	@SuppressWarnings("unchecked")
	public Microshunt() {
		super(new IHullmodPart[]{
				new TurnIntoIEDAIPart(),
				new IEDPart()
		});
	}


	@Override
	public boolean shouldAddDescriptionToTooltip(HullSize hullSize, ShipAPI ship, boolean isForModSpec) {
		return true;
	}



	VentType ventType = VentType.LAMINATE; //TODO we're leaking memory lol
	public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
		//good
		stats.getEnergyWeaponFluxCostMod().modifyPercent(id, FLUX_REDUCTION.get(hullSize));
		stats.getBallisticWeaponFluxCostMod().modifyPercent(id, FLUX_REDUCTION.get(hullSize));
		stats.getShieldUnfoldRateMult().modifyFlat(id, UNFOLD_RATE);


		//bad
		if (hullSize == HullSize.FIGHTER) {
			stats.getOverloadTimeMod().modifyPercent(id, -OVERLOAD_TIME); //lol get trolled
		} else {
			stats.getOverloadTimeMod().modifyPercent(id, OVERLOAD_TIME);
		}

		stats.getSuppliesPerMonth().modifyPercent(id, SUPPLY);
		stats.getShieldAbsorptionMult().modifyPercent(id, SHIELD_EFF);
		stats.getCombatWeaponRepairTimeMult().modifyPercent(id, -REPAIR_TIME);
		stats.getHullBonus().modifyMult(id, 1/HULL_MULTI);

		boolean doSwitch = true;

		for (VentType toCompare : HULLMODS_NEXT.keySet()) {
			if (stats.getVariant().getHullMods().contains(toCompare.ID)) doSwitch = false;
		}

		if(doSwitch){

			ventType = HULLMODS_NEXT.get(ventType);
			stats.getVariant().addMod(ventType.ID);
		}

	}

	@Override
	public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
		boolean shouldSoundError = false;

		ArrayList<String> deletionList = new ArrayList<>();

		for (String s : ship.getVariant().getNonBuiltInHullmods()) {
			if (BAD_HULLMODS.contains(s)) {
				deletionList.add(s);
			}
		}

		if (deletionList.size() > 0) {
			ship.getVariant().addMod("ML_incompatibleHullmodWarning");
			shouldSoundError = true;
		}
		for (String s : deletionList) {
			ship.getVariant().removeMod(s);
		}

		if (shouldSoundError) {
			Global.getSoundPlayer().playUISound(BAD_HULLMOD_NOTIFICATION_SOUND, 0.7f, 1f);
		}

	}



	@Override
	public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
		float pad = 2f;

		Color h = Misc.getHighlightColor();
		Color good= Misc.getPositiveHighlightColor();
		Color bad = Misc.getNegativeHighlightColor();

		tooltip.addPara("", pad);
		tooltip.addSectionHeading("Description", Alignment.MID, 0f);
		tooltip.addPara("", pad);
		tooltip.addPara("• %s weapons have %s reduced flux costs", pad, good, "All weapons", ARCUtils.slashesOf(FLUX_REDUCTION));
		tooltip.addPara("• Shield deployment is %s faster", pad, good, (int)(UNFOLD_RATE * 100) + "%");
		tooltip.addPara("• Supply usage increased by %s", pad, bad, ((int)(SUPPLY)) + "%");
		tooltip.addPara("• Hull durability decreased by", pad, bad, ((int)(HULL_MULTI)) + "x");
		tooltip.addPara("• Weapon repair time increased by %s", pad, bad, ((int)(REPAIR_TIME)) + "%");
		tooltip.addPara("• Shield efficiency decreased by %s", pad, bad, ((int)(SHIELD_EFF)) + "%");

		tooltip.addPara("This ship can %s", pad, Misc.getStoryBrightColor(), "access Archotech Research weapons and fighters");

		tooltip.addPara("", pad, h, "");

		tooltip.addSectionHeading("Passive Ability - Hyper Vent", Misc.getPositiveHighlightColor(), Misc.getStoryDarkColor(), Alignment.MID, 0f);
		tooltip.addPara("", pad, h, "");
		tooltip.addPara("Venting instead diverts power to the microshunt granting %s to the ARC ship", pad, good, "unique utilities");
		tooltip.addPara("", pad, h, "");
		tooltip.addPara("Base venting speed increased to %s.", pad, h, "" + (int) BASE_VENT_BONUS + "%");
		tooltip.addPara("Venting speed while at max flux decreased to %s.", pad, h, "" + (int)MAX_VENT_BONUS + "%");
		tooltip.addPara("Scales with Archotech Research's venting based technologies", pad);
		tooltip.addPara("", pad, h, "");

		tooltip.addSectionHeading("Passive Ability - Iono Hyperdense Shield", Misc.getPositiveHighlightColor(), Misc.getStoryDarkColor(), Alignment.MID, 0f);
		tooltip.addPara("", pad, h, "");
		tooltip.addPara("Venting instead diverts power to the microshunt granting %s to the ARC ship", pad, good, "unique utilities");
		tooltip.addPara("", pad, h, "");
		tooltip.addPara("Base venting speed increased to %s.", pad, h, "" + (int) BASE_VENT_BONUS + "%");
		tooltip.addPara("Venting speed while at max flux decreased to %s.", pad, h, "" + (int)MAX_VENT_BONUS + "%");
		tooltip.addPara("Scales with Archotech Research's venting based technologies", pad);
		tooltip.addPara("", pad, h, "");


		tooltip.addSectionHeading("Incompatibilities", Misc.getGrayColor(), Misc.getDarkHighlightColor(), Alignment.MID, 0f);
		tooltip.addPara("", pad, h, "");
		TooltipMakerAPI text = tooltip.beginImageWithText("graphics/ARC/icons/arc_incompatible.png", 40);
		text.addPara("This ship cannot install the following hullmods", 0f);
		text.addPara("• Safeties Overrides", Misc.getNegativeHighlightColor(), 0f);
		text.addPara("• Heavy Armor", Misc.getNegativeHighlightColor(), 0f);
		text.addPara("• Converted Hangars", Misc.getNegativeHighlightColor(), 0f);
		text.addPara("• Resistant Flux Conduits", Misc.getNegativeHighlightColor(), 0f);

		if (Global.getSettings().getModManager().isModEnabled("apex_design")) {
			text.addPara("• Nanolaminate Plating", Misc.getNegativeHighlightColor(), 0f);
			text.addPara("• Cryocooled Armor Lattice", Misc.getNegativeHighlightColor(), 0f);
		}
		if (Global.getSettings().getModManager().isModEnabled("roider")) {
			text.addPara("• Fighter Clamps", Misc.getNegativeHighlightColor(), 0f);
		}
		if (Global.getSettings().getModManager().isModEnabled("timid_xiv")) {
			text.addPara("• Aquila Reactor", Misc.getNegativeHighlightColor(), 0f);
			text.addPara("• Vanagloria Ionized Armor", Misc.getNegativeHighlightColor(), 0f);
		}

		tooltip.addImageWithText(0f);
		tooltip.addPara("", pad, h, "");









	}



	static final Map<VentType, VentType> HULLMODS_NEXT = new HashMap<>(); static {
		HULLMODS_NEXT.put(VentType.LAMINATE, VentType.LAMINATE);
	}


	boolean runOnce = false;

	float counter =0;

	@Override
	public void advanceInCombat(ShipAPI ship, float amount) {
		super.advanceInCombat(ship, amount);
		counter += amount;



		CombatEngineAPI combat = Global.getCombatEngine();

		if (combat.isPaused()) return;

		if (ship.getAllWeapons().stream().anyMatch(w -> {
			return w.getDisplayName().contains("tenebre") && w.isFiring(); //TODO
		})) {
			ship.blockCommandForOneFrame(ShipCommand.USE_SYSTEM);
		}


		//TODO is this bad to do every frame? oh well..
		final ShieldAPI shield = ship.getShield();
		final float radius = ship.getHullSpec().getShieldSpec().getRadius();
		String inner;
		String outer;
		if (radius >= 256.0f) {
			inner = "graphics/ARC/fx/hexshield256.png";
			outer = "graphics/ARC/fx/shields256ring.png";
		}
		else if (radius >= 128.0f) {
			inner = "graphics/ARC/fx/hexshield128.png";
			outer = "graphics/ARC/fx/shields128ring.png";
		}
		else {
			inner = "graphics/ARC/fx/hexshield64.png";
			outer = "graphics/ARC/fx/shields64ring.png";
		}




		if (ship.getShield() != null) {
			ReturnType type = oscillateShieldColor(0, counter);

			//TODO: adaptive shield


			ship.setJitterShields(true);
			ship.getShield().setInnerColor(type.color);

			ship.setJitterUnder(ship, type.color, 2f * type.alpha, (int)(3f * type.alpha), 1);

			shield.setRadius(radius, inner, outer);
			shield.setInnerRotationRate(-1.5f); //super slow
			shield.setRingRotationRate(1.5f); //do not rotate, use circle
		}






		//  Burst Surger / Variable Surger


		//   Venting AI






	}

	// Useless methods


	class ReturnType {
		Color color;
		float alpha;

		public ReturnType(Color color, float alpha) {
			this.color = color;
			this.alpha = alpha;
		}
	}

	public ReturnType oscillateShieldColor(double bias, float elapsedTime) {

		// Define the oscillation speed (ensure this constant is defined appropriately)

		// Clamp bias to [0, 2] to ensure it stays within the defined states
		bias = Math.max(0, Math.min(2, bias));

		// Define the three base colors
		int r0 = 120, g0 = 120, b0 = 143; // Light cyan
		int r1 = 33, g1 = 59, b1 = 82;   // Light blue
		int r2 = 8,  g2 = 24, b2 = 37;   // Dark greyish-blue

		// Define the base alpha values without oscillation
		float alpha0 = 0.5f;
		float alpha1 = 0.3f;
		float alpha2 = 0.4f;

		// Initialize variables for the blended color and alpha
		int blendedRed, blendedGreen, blendedBlue;
		float blendedAlpha;
		float factor;

		if (bias <= 1) {
			// Interpolate between color0 and color1
			factor = (float) bias; // bias is between 0 and 1

			blendedRed   = (int) (r0 + factor * (r1 - r0));
			blendedGreen = (int) (g0 + factor * (g1 - g0));
			blendedBlue  = (int) (b0 + factor * (b1 - b0));

			blendedAlpha = alpha0 + factor * (alpha1 - alpha0);
		} else {
			// Interpolate between color1 and color2
			factor = (float) (bias - 1); // bias is between 0 and 1

			blendedRed   = (int) (r1 + factor * (r2 - r1));
			blendedGreen = (int) (g1 + factor * (g2 - g1));
			blendedBlue  = (int) (b1 + factor * (b2 - b1));

			blendedAlpha = alpha1 + factor * (alpha2 - alpha1);
		}

		// Clamp the blended color components to [0, 255]
		blendedRed   = Math.max(0, Math.min(255, blendedRed));
		blendedGreen = Math.max(0, Math.min(255, blendedGreen));
		blendedBlue  = Math.max(0, Math.min(255, blendedBlue));

		// Calculate the oscillation factor
		float oscillationFactor = (float) Math.sin(elapsedTime * 0.7);

		// Modify color based on oscillation
		int oscillatingRed   = Math.max(0, Math.min(255, blendedRed + (int) (oscillationFactor * 10f)));
		int oscillatingGreen = Math.max(0, Math.min(255, blendedGreen + (int) (oscillationFactor * 10f)));
		int oscillatingBlue  = Math.max(0, Math.min(255, blendedBlue + (int) (oscillationFactor * 5f)));

		Color oscillatingColor = new Color(oscillatingRed, oscillatingGreen, oscillatingBlue);

		// Apply oscillation to alpha
		float oscillatingAlpha = blendedAlpha + 0.2f * oscillationFactor; // Adjust the multiplier as needed

		// Clamp alpha to [0, 1]
		oscillatingAlpha = Math.max(0f, Math.min(0.7f, oscillatingAlpha));

		return new ReturnType(oscillatingColor, oscillatingAlpha);
	}


	@Override
	public boolean affectsOPCosts() {
		return true;
	}
	@Override
	public boolean isApplicableToShip(ShipAPI ship) {
		return false; //TODO never
	}

}
