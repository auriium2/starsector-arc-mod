package arc.hullmod.laminate;

import arc.hullmod.ARCBaseHullmod;
import arc.hullmod.IHullmodPart;
import arc.hullmod.VentAcceleratorPart;
import arc.hullmod.microshunt.ai.VentAIPart;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.*;

//defensive core
public class ArchotechLaminate extends ARCBaseHullmod {

    static final String ARCHOTECH_LAMINATE = "arc_archotechlaminate";
    static final float EMP_RES_BONUS = -80f;

    public static final float MIN_FLUX_LEVEL = 0.05f;
    public static final float MAX_FLUX_LEVEL = 0.7f;
    public static final float MIN_DAMAGE_REDUCTION = 0.95f;
    public static final float MAX_DAMAGE_REDUCTION = 0.1f;
    public static final float MIN_DELTA_REPAIR = 0.1f;
    public static final float MAX_DELTA_REPAIR = 0.7f;
    public static final float ARMOR_BOOST = 250f;
    public static final float BEAM_RES = 0.6f;


    @SuppressWarnings("unchecked")
    public ArchotechLaminate() {
        super(new IHullmodPart[]{
                new VentAcceleratorPart(),
                new VentingGivesArmorPart(),
                new VentAIPart()
        });
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, ShipAPI.HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        float pad = 2f;
        Color h = Misc.getHighlightColor();

        tooltip.addPara("", pad, h, "");
        tooltip.addSectionHeading("Description", Alignment.MID, 0f);
        tooltip.addPara("", pad, h, "");

        tooltip.addPara("• Increase armor by a flat %s", pad, Misc.getPositiveHighlightColor(), (int) Math.abs(ARMOR_BOOST) + "");
        tooltip.addPara("• The adaptive nature of the armor reduces EMP damage by %s", pad, Misc.getPositiveHighlightColor(), (int) Math.abs(EMP_RES_BONUS) + "%");
        tooltip.addPara("", pad, h, "");

        tooltip.addSectionHeading("Passive Ability - Chroma Field", Misc.getPositiveHighlightColor(), Misc.getStoryDarkColor(), Alignment.MID, 0f);

        tooltip.addPara("", pad, h, "");
        tooltip.addPara("• When the ship is venting, generate a damper that reduces damage based on the flux vented and lingers after venting", pad, h, "");
        tooltip.addPara("• When venting %s of total flux capacity, reduce damage by %s, scaling down as flux increases", pad, Misc.getPositiveHighlightColor(), (int)(MIN_FLUX_LEVEL * 100f) + "%", (int)(MIN_DAMAGE_REDUCTION * 100f) + "%");
        tooltip.addPara("• When venting %s of total flux capacity, increase beam res by a flat %s", pad, Misc.getPositiveHighlightColor(), (int)(MIN_FLUX_LEVEL * 100f) + "%", (int)(BEAM_RES * 100f) + "%");
        tooltip.addPara("• %s, scaling exponentially with flux dissipated", pad, h, "Repair armor density");
        tooltip.addPara("", pad, h, "");
        tooltip.addPara("• Above %s of total flux capacity venting will do nothing", pad, Misc.getGrayColor(), ""  + (int)(MAX_FLUX_LEVEL * 100));
    }
    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getArmorBonus().modifyFlat(ARCHOTECH_LAMINATE, ARMOR_BOOST);
        stats.getEmpDamageTakenMult().modifyPercent(ARCHOTECH_LAMINATE, EMP_RES_BONUS);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {

        /*if (!ship.hasListenerOfClass(WhitespaceRegen.class)) {
            ship.addListener(new WhitespaceRegen());
        }*/

    }
}
