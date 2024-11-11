package arc.hullmod.laminate;

import arc.hullmod.IHullmodPart;
import arc.plugin.RunnableQueuePlugin;
import arc.util.ARCUtils;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import data.scripts.util.MagicUI;

import java.awt.*;


public class VentingGivesArmorPart implements IHullmodPart<VentingGivesArmorPart.Data> {

    public static final String VENTING_ARMOR =  "arc_venting_armor";
    public static final float COOLDOWN = 320f;

    private static final Color SPARK_COLOR = new Color(255, 223, 128);
    private static final String SPARK_SOUND_ID = "system_emp_emitter_loop";
    private static final float SPARK_DURATION = 0.2f;
    private static final float SPARK_BRIGHTNESS = 1f;
    private static final float SPARK_MAX_RADIUS = 7f;
    private static final float SPARK_CHANCE = 0.17f;
    private static final float SPARK_SPEED_MULTIPLIER = 500f;
    private static final float SPARK_VOLUME = 1f;
    private static final float SPARK_PITCH = 1f;

    enum VentType {
        NOT,
        BLOCK,
        REGEN
    }

    public static class Data {
        boolean startedVenting = false;
        public float lastMultiplier = 1.0f;
        public float lastRepairMultiplier = 1.0f;

        public float cooldown = 0;

    }


    @Override
    public void advanceSafely(CombatEngineAPI engineAPI, ShipAPI shipAPI, float timestep, Data customData) {

        float fillLevel = (COOLDOWN - customData.cooldown) / COOLDOWN;

        float damageMult = ARCUtils.clamp(
                ArchotechLaminate.MAX_DAMAGE_REDUCTION,
                ArchotechLaminate.MIN_DAMAGE_REDUCTION,
                ARCUtils.remap(
                        0.1f,
                        1.3f,
                        ArchotechLaminate.MAX_DAMAGE_REDUCTION,
                        ArchotechLaminate.MIN_DAMAGE_REDUCTION,
                        shipAPI.getFluxLevel()
                )
        );

        float repairMult = ARCUtils.remapExponential(
                0.1f,
                1.0f,
                ArchotechLaminate.MIN_DELTA_REPAIR,
                ArchotechLaminate.MAX_DELTA_REPAIR,
                shipAPI.getFluxLevel(),
                3f
        );



        MagicUI.drawInterfaceStatusBar(
                shipAPI,
                fillLevel,
                Color.GREEN,
                Color.WHITE,
                fillLevel,
                "CHROMA",
                100 - (int) ((damageMult) * 100)
        );




        customData.cooldown = customData.cooldown - 1;
        if (customData.cooldown < 0) customData.cooldown = 0;


        float fluxLevel = shipAPI.getFluxLevel();
        if (shipAPI.getFluxTracker().isVenting()) {


            if (!customData.startedVenting && fluxLevel > 0.05 && fluxLevel < 0.9f && customData.cooldown == 0) {
                customData.startedVenting = true;
                customData.lastMultiplier = damageMult;
                customData.lastRepairMultiplier = repairMult;


                RunnableQueuePlugin.shipsGlowing.add(shipAPI);
                shipAPI.getMutableStats().getHullDamageTakenMult().modifyMult(VENTING_ARMOR, damageMult);
                shipAPI.getMutableStats().getArmorDamageTakenMult().modifyMult(VENTING_ARMOR, damageMult);

                Global.getSoundPlayer().playSound("system_damper", 1.1f, 1f, shipAPI.getLocation(), shipAPI.getVelocity());
                //Global.getSoundPlayer().playSound("system_damper", 1.1f, 1f, shipAPI.getLocation(), shipAPI.getVelocity());


            }

            if (customData.cooldown > 0 || !customData.startedVenting) return;

            if (customData.lastRepairMultiplier > 0.3) {
                //play effects

                //fu logic?


                if (engineAPI.getPlayerShip() == shipAPI) {
                    engineAPI
                            .maintainStatusForPlayerShip(
                                    VENTING_ARMOR,
                                    "graphics/icons/hullsys/damper_field.png",
                                    "Chroma Field",
                                    "Damage taken multiplied by " + customData.lastMultiplier + "%", false
                            );
                }

            }
        } else {
            if (customData.startedVenting) {

                customData.startedVenting = false;
                customData.cooldown = COOLDOWN;
                customData.lastMultiplier = 1f;

                //unmodify damage absorption

                RunnableQueuePlugin.queueTask(() -> {
                    if (customData.startedVenting) return; //otherwise..

                    //iframes
                    RunnableQueuePlugin.shipsGlowing.remove(shipAPI);
                    shipAPI.getMutableStats().getHullDamageTakenMult().unmodifyMult(VENTING_ARMOR);
                    shipAPI.getMutableStats().getArmorDamageTakenMult().unmodifyMult(VENTING_ARMOR);
                    float repaired = regenArmor(shipAPI, customData.lastRepairMultiplier);

                    if (customData.lastRepairMultiplier > 0.5f) {
                        Global.getCombatEngine().addFloatingDamageText(shipAPI.getLocation().translate(20, 20), repaired, Color.GREEN, shipAPI, shipAPI);
                    }


                }, 12); //Let's be generous


            }
        }
    }


    private float regenArmor(ShipAPI ship, float addCoef_mp) {
        //First, calculates average armor
        int maxX = ship.getArmorGrid().getLeftOf() + ship.getArmorGrid().getRightOf();
        int maxY = ship.getArmorGrid().getAbove() + ship.getArmorGrid().getBelow();

        float totalRepaired = 0;

        for (int ix = 0; ix < maxX; ix++) {
            for (int iy = 0; iy < maxY; iy++) {
                float armorHere = ship.getArmorGrid().getArmorValue(ix,iy);
                float missing = ship.getArmorGrid().getMaxArmorInCell() - armorHere;
                float missing_dot_coef = missing * addCoef_mp;
                totalRepaired += missing_dot_coef;
                ship.getArmorGrid().setArmorValue(ix, iy, armorHere + missing_dot_coef);
            }
        }

        return totalRepaired;

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
    public Data makeNew() {
        return new Data();
    }

    @Override
    public String makeKey() {
        return VENTING_ARMOR;
    }
}
