package arc.hullmod.microshunt.ai;

import arc.Index;
import arc.hullmod.ARCData;
import arc.hullmod.IHullmodPart;
import arc.hullmod.laminate.ArchotechLaminate;
import arc.hullmod.laminate.VentingGivesArmorPart;
import arc.util.ARCUtils;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;

import java.awt.*;
import java.util.List;


/**
 * Handles how the AI will decide when to use vent abilities
 */
public class VentAIPart implements IHullmodPart<ARCData> {


    final IntervalUtil debugText = new IntervalUtil(0.3f,0.3f);


    @Override
    public void advanceSafely(CombatEngineAPI engineAPI, ShipAPI ship, float timestep, ARCData customData) {
        debugText.advance(timestep);

        if (ship.getSystem().isActive()) return; //Don't activate using system
        if (ship == Global.getCombatEngine().getPlayerShip() && ship.getAI() == null) return; //dont use this on the player ship


        FluxTrackerAPI fluxTracker = ship.getFluxTracker();
        ShipwideAIFlags flags = ship.getAIFlags();
        float fluxLevel = fluxTracker.getFluxLevel();
        if (fluxTracker.isOverloadedOrVenting()) return;


        //check all these every tick
        //use armor to block shield damage fluxLevel > 0.05f && fluxLevel < 0.6f &&

        float damageMult = ARCUtils.clamp(
                ArchotechLaminate.MAX_DAMAGE_REDUCTION,
                ArchotechLaminate.MIN_DAMAGE_REDUCTION,
                ARCUtils.remap(
                        0f,
                        1,
                        ArchotechLaminate.MAX_DAMAGE_REDUCTION,
                        ArchotechLaminate.MIN_DAMAGE_REDUCTION,
                        fluxLevel
                )
        );



        for (WeaponAPI weaponAPI : ship.getUsableWeapons()) {
            if (weaponAPI.getSpec().hasTag("hyper") && weaponAPI.isFiring()) return;
            if (weaponAPI.getSpec().hasTag("strike") && weaponAPI.isFiring()) return;
            //if (weaponAPI.getSpec().hasTag("strike") && weaponAPI.isFiring()) return;

        }

        //todo MORE CONSERVATIVE for firgates

        float tooMuchArmorDamageThreshold = ARCUtils.decideBasedOnHullSize(
                ship,
                0.18f,
                0.18f,
                0.18f,
                0.2f
        );

        float durationToCheck = ship.getFluxTracker().getTimeToVent();

        float armorDamage = ARCUtils.armorDamagePossible(
                ship,
                1200,
                durationToCheck,
                damageMult
        );
/*
        CMUtils.getGuiDebug().putText(VentAIPart.class, ship.getId() + ship.hashCode(), armorDamage + "");

        if (debugText.intervalElapsed()) {
            Global.getCombatEngine().addFloatingText(ship.getLocation(), "incoming armor: " + (int)armorDamage, 30f, Color.RED, ship, 1f, 0.5f);
        }*/

        boolean tooMuchArmorDamage = armorDamage >= (ship.getArmorGrid().getArmorRating() * tooMuchArmorDamageThreshold) ;
        boolean tooMuchShieldDamage = ARCUtils.tooMuchShieldDamageIncoming(
                ship,
                MathUtils.getRandomNumberInRange(0.07f, 0.14f),
                1300f,
                durationToCheck
        );

/*

        if (tooMuchArmorDamage) {
            ship.setJitterUnder(ship, Color.RED, 4f, 4, 2f);
        }

        if (tooMuchShieldDamage) {
            ship.setJitterUnder(ship, Color.BLUE, 4f, 4, 2f);
        }
*/



        VentingGivesArmorPart.Data data  = (VentingGivesArmorPart.Data) ship.getCustomData().get(VentingGivesArmorPart.VENTING_ARMOR);

        if (data == null) return;
        if (data.cooldown != 0) return; //No point in doing any of this if our armor/gun isn't ready to vent yet






        if (fluxLevel > 0.05f && fluxLevel < 0.55f && tooMuchShieldDamage) {
            //anti shield vent

            if (tooMuchArmorDamage) {
                return;
            }

            FluxTrackerAPI fluxTracker1 = ship.getFluxTracker();

            //as long as the time is low...

            if (ship.getMutableStats().getVentRateMult().getModifiedValue() > 3 && ship.getFluxLevel() < 0.6f) {

                ship.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.DO_NOT_BACK_OFF, fluxTracker1.getTimeToVent() + 2f);
                ship.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.DO_NOT_BACK_OFF_EVEN_WHILE_VENTING, fluxTracker1.getTimeToVent() + 2f);
                ship.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.MANEUVER_TARGET, fluxTracker1.getTimeToVent() + 3f);
                //close that gap, these are ANGRY ships


                ship.giveCommand(ShipCommand.VENT_FLUX, null, 0);
            } else {
                //back down, but still armor vent

                //ship.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.BACK_OFF, 2f);
                ship.giveCommand(ShipCommand.VENT_FLUX, null, 0);
            }


            return;
        }


        if (flags.hasFlag(ShipwideAIFlags.AIFlags.DO_NOT_USE_SHIELDS) && fluxLevel < 0.55f && fluxLevel > 0.04f && (Math.random() < 0.6f)) { //super efficient damper) {

            if (tooMuchArmorDamage) {

                return;
            }


            FluxTrackerAPI fluxTracker1 = ship.getFluxTracker();

            //as long as the time is low...

            //TODO instead of 2 select by personality
            if (ship.getMutableStats().getVentRateMult().getModifiedValue() > 2 && ship.getFluxLevel() < 0.65f) {

                ship.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.DO_NOT_BACK_OFF, fluxTracker1.getTimeToVent() + 2f);
                ship.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.DO_NOT_BACK_OFF_EVEN_WHILE_VENTING, fluxTracker1.getTimeToVent() + 2f);
                ship.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.MANEUVER_TARGET, fluxTracker1.getTimeToVent() + 3f);
                //close that gap, these are ANGRY ships


                ship.giveCommand(ShipCommand.VENT_FLUX, null, 0);
            } else {
                //back down, but still armor vent

                ship.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.BACK_OFF, 2f);
                ship.giveCommand(ShipCommand.VENT_FLUX, null, 0);
            }


            return;



        }

        //inverse curve to prioritize low flux and penalize high flux
        float ventingNeed = ARCUtils.pow(fluxLevel + 0.9, -1.5f);

        //we never want to get down to hull, but this can remain

        float hullLevel = ship.getHullLevel();
        float hullFactor = ARCUtils.decideBasedOnHullSize(
                ship,
                ARCUtils.pow(hullLevel,2f),
                hullLevel,
                ARCUtils.pow(hullLevel,0.6f),
                ARCUtils.pow(hullLevel,0.4f)
        );

        //situational danger

        float dangerFactor=0;


        List<ShipAPI> nearbyEnemies = AIUtils.getNearbyEnemies(ship, 2000f);
        for (ShipAPI enemy : nearbyEnemies) {
            //TODO this should consider ship weapons rather than hull size


            switch (enemy.getHullSize()) {
                case CAPITAL_SHIP:
                    dangerFactor+= Math.max(
                            0, 3 - MathUtils.getDistanceSquared(
                                            enemy.getLocation(), ship.getLocation()
                            ) /1000 * 1000
                    );
                    break;
                case CRUISER:
                    dangerFactor+= Math.max(0,2.25f-(MathUtils.getDistanceSquared(enemy.getLocation(), ship.getLocation())/1000000));
                    break;
                case DESTROYER:
                    dangerFactor+= Math.max(0,1.5f-(MathUtils.getDistanceSquared(enemy.getLocation(), ship.getLocation())/1000000));
                    break;
                case FRIGATE:
                    dangerFactor+= Math.max(0,1f-(MathUtils.getDistanceSquared(enemy.getLocation(), ship.getLocation())/1000000));
                    break;
                default:
                    dangerFactor+= Math.max(0,0.5f-(MathUtils.getDistanceSquared(enemy.getLocation(), ship.getLocation())/640000));
                    break;
            }
        }

        float decisionLevel = (ventingNeed * hullFactor+1) /(dangerFactor+1);

        if (fluxLevel < 0.05f) return; //silly bug

        //TODO AND no armor AND
        //TODO right now this lets shots through





        if (decisionLevel >=MathUtils.getRandomNumberInRange(1.1f, 1.6f)) {


            FluxTrackerAPI fluxTracker1 = ship.getFluxTracker();

            //as long as the time is low...

            if (ship.getMutableStats().getVentRateMult().getModifiedValue() > 3 && ship.getFluxLevel() < 0.6f) {

                ship.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.DO_NOT_BACK_OFF, fluxTracker1.getTimeToVent() + 2f);
                ship.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.DO_NOT_BACK_OFF_EVEN_WHILE_VENTING, fluxTracker1.getTimeToVent() + 2f);
                //ship.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.MANEUVER_TARGET, fluxTracker1.getTimeToVent() + 3f);
                //ship.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.MANEUVER_RANGE_FROM_TARGET, fluxTracker1.getTimeToVent() + 1f, 500f); //TODO
                ship.getAIFlags().removeFlag(ShipwideAIFlags.AIFlags.BACK_OFF);

                //close that gap, these are ANGRY ships


                ship.giveCommand(ShipCommand.VENT_FLUX, null, 0);
            } else {
                //back down, but still armor vent

                ship.getAIFlags().setFlag(ShipwideAIFlags.AIFlags.BACK_OFF, 2f);
                ship.giveCommand(ShipCommand.VENT_FLUX, null, 0);
            }


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
