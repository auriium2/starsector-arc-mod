package arc.hullmod.sheath;

import arc.Index;
import arc.hullmod.ARCData;
import arc.hullmod.IHullmodPart;
import arc.util.ARCUtils;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.ProjectileSpawnType;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;

import java.awt.*;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

public class VariableSheathEffectsPart implements IHullmodPart<ARCData> {

    static final String SHEATH_GENERAL = "arc_sheath_general";
    static final String SHEATH_ARMOR = "arc_sheath_armor";
    static final String SHEATH_BERSERK = "arc_sheath_berserk";

    static final Color AIMBOT = new Color(100, 140, 255, 250);
    static final float EFFECT_RAMP_TIME = 2.0f;
    static final Random rng = new Random();


    static final float REPAIR_AMOUNT = 0.6f;

    static void repairArmor(ShipAPI ship, float superMult) {

        ArmorGridAPI armorGrid = ship.getArmorGrid();

        
        int gridWidth = armorGrid.getGrid().length;
        int gridHeight = armorGrid.getGrid()[0].length;
        int cellCount = gridHeight * gridWidth;
        float max = armorGrid.getMaxArmorInCell();

        int xP = 0;
        int yP = 0;

        for (int i = 0; i < (1 + cellCount / 5); ++i) {
            xP = rng.nextInt(gridWidth);
            yP = rng.nextInt(gridHeight);

            if (armorGrid.getArmorValue(xP, yP) < max) {
                break;
            }
        }

        float totalRepaired = 0f;

        for (int x = xP - 1; x <= xP + 1; ++x) {
            if (x < 0 || x >= gridWidth) {
                continue;
            }

            for (int y = yP - 1; y <= yP + 1; ++y) {
                if (y < 0 || y >= gridHeight) {
                    continue;
                }

                float mult = (3f - Math.abs(x - xP) - Math.abs(y - yP)) / 3f;

                totalRepaired -= armorGrid.getArmorValue(x, y);
                armorGrid.setArmorValue(x, y, Math.min(max, armorGrid.getArmorValue(x, y) + REPAIR_AMOUNT * mult * superMult));
                totalRepaired += armorGrid.getArmorValue(x, y);
            }
        }
        //ship.syncWithArmorGridState();
        //ship.syncWeaponDecalsWithArmorDamage();
        Global.getCombatEngine().addFloatingDamageText(ship.getLocation(), totalRepaired, Color.GREEN, ship, ship);
    }

    final Color FLICKER_COLOR = new Color(129, 92, 92, 131);;
    final IntervalUtil INTERVAL = new IntervalUtil(0.4f, 0.4f);

    void brakefield(ShipAPI ship, double multi) {

        float amount = Global.getCombatEngine().getElapsedInLastFrame();

        List<CombatEntityAPI> entities = CombatUtils.getEntitiesWithinRange(ship.getLocation(), ship.getMass());

        for (CombatEntityAPI entity : entities) {
            if (entity.getOwner() == ship.getOwner()) {
                // Don't affect friendly entities
                continue;
            }

            double mod = entity.getMass() / 2000f * multi;

            mod = (1f - (1f / 2f)) + (mod / 2f);

            if ((entity instanceof DamagingProjectileAPI)
                    && (((DamagingProjectileAPI) entity).getSpawnType() == ProjectileSpawnType.BALLISTIC_AS_BEAM)) {
                float idealAngle = VectorUtils.getAngle(entity.getLocation(), ship.getLocation()) + 180f;
                if (idealAngle >= 360f) {
                    idealAngle -= 360f;
                }

                float rotationNeeded = MathUtils.getShortestRotation(entity.getFacing(), idealAngle);
                entity.setFacing((float) (entity.getFacing() + (rotationNeeded * mod / 60f)));
            } else {
                entity.getVelocity().scale((float) ((float) mod * multi));
            }
        }



    }

    @Override
    public void advanceSafely(CombatEngineAPI engineAPI, ShipAPI shipAPI, float timestep, ARCData customData) {



        //timescale
        /*if (INTERVAL.intervalElapsed()) {
            SpriteAPI sprite = shipAPI.getSpriteAPI();
            float offsetX = sprite.getWidth() / 2 - sprite.getCenterX();
            float offsetY = sprite.getHeight() / 2 - sprite.getCenterY();

            float trueOffsetX = (float) FastTrig.cos(Math.toRadians(shipAPI.getFacing() - 90f)) * offsetX - (float) FastTrig.sin(Math.toRadians(shipAPI.getFacing() - 90f)) * offsetY;
            float trueOffsetY = (float) FastTrig.sin(Math.toRadians(shipAPI.getFacing() - 90f)) * offsetX + (float) FastTrig.cos(Math.toRadians(shipAPI.getFacing() - 90f)) * offsetY;

            MagicRender.battlespace(
                    Global.getSettings().getSprite(shipAPI.getHullSpec().getSpriteName()),
                    new Vector2f(shipAPI.getLocation().getX() + trueOffsetX, shipAPI.getLocation().getY() + trueOffsetY),
                    new Vector2f(0, 0),
                    new Vector2f(shipAPI.getSpriteAPI().getWidth(), shipAPI.getSpriteAPI().getHeight()),
                    new Vector2f(0, 0),
                    shipAPI.getFacing() - 90f,
                    0f,
                    new Color(155, 90, 120, 155),
                    true,
                    0f,
                    0f,
                    0f,
                    0f,
                    0f,
                    0.1f,
                    0.1f,
                    0.5f,
                    CombatEngineLayers.BELOW_SHIPS_LAYER);
        }*/


        //timescale
        float timeMult = ARCUtils.clamp(
                1f,
                1.4f,
                ARCUtils.remap(
                        0.1f,
                        1.3f,
                        1.0f,
                        1.4f,
                        shipAPI.getFluxLevel()
                )
        );

        shipAPI.getMutableStats().getTimeMult().modifyMult("cum", timeMult);


        if (shipAPI.isHoldFire() && !customData.holdFireBefore) { //if you tap X

            shipAPI.setHoldFire(false);



            //state machine

            if (customData.mode == ARCData.Mode.ASSAULT) {
                customData.mode = ARCData.Mode.BERSERK;
            } else if (customData.mode == ARCData.Mode.BERSERK) {
                customData.mode = ARCData.Mode.REPAIR;
            } else if (customData.mode == ARCData.Mode.REPAIR) {
                customData.mode = ARCData.Mode.ASSAULT;
            }

            customData.lastMode = customData.mode;


        }




        customData.holdFireBefore = shipAPI.isHoldFire();


        INTERVAL.advance(timestep);

        if (customData.lastMode != customData.mode) { //reset on swap system
            customData.modeAbilityScale = 0f;
        }

        customData.modeAbilityScale += timestep / EFFECT_RAMP_TIME;
        if (customData.modeAbilityScale > 1f) {
            customData.modeAbilityScale = 1f;
        }



        // modes

        float totalFlux = shipAPI.getCurrFlux();
        float hardFlux = shipAPI.getFluxTracker().getHardFlux();
        float maxFlux = shipAPI.getMaxFlux();

        float softFluxLevel = (totalFlux-hardFlux)/(maxFlux-hardFlux);
        float fluxLevel = shipAPI.getFluxLevel();



        //bullshit OP faction (can still damage through this since it regens super slow without extra flux, but otherwise acts like Hel Carapace)
        if (customData.mode == ARCData.Mode.REPAIR) {
            if (softFluxLevel > 0.1f) { //we have enough to do super repairs
                shipAPI.getFluxTracker().decreaseFlux(maxFlux / 120f); //succc
                repairArmor(shipAPI, 3f);
            } else {
                repairArmor(shipAPI, customData.modeAbilityScale * 0.5f); //slow repairs
            }

            brakefield(shipAPI, Math.min(fluxLevel - 0.3f, 0f));

            //you cannot
            shipAPI.blockCommandForOneFrame(ShipCommand.USE_SYSTEM);
            shipAPI.blockCommandForOneFrame(ShipCommand.VENT_FLUX);
            shipAPI.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK);

            //be slow
            shipAPI.getMutableStats().getAcceleration().modifyPercent(SHEATH_ARMOR, -40f);
            shipAPI.getMutableStats().getMaxSpeed().modifyPercent(SHEATH_ARMOR, -40f);
            shipAPI.getMutableStats().getMaxTurnRate().modifyPercent(SHEATH_ARMOR, -40f);
            shipAPI.getMutableStats().getTurnAcceleration().modifyPercent(SHEATH_ARMOR, -40f);

            shipAPI.getShield().toggleOff();
            shipAPI.setDefenseDisabled(true);



            shipAPI.setAlphaMult(0.4f);
            //shipAPI.setJitter(shipAPI, FLICKER_COLOR, 0.7f, 10, 25f, 50f);



        } else {
            shipAPI.getMutableStats().getAcceleration().unmodify(SHEATH_ARMOR);
            shipAPI.getMutableStats().getMaxSpeed().unmodify(SHEATH_ARMOR);
            shipAPI.getMutableStats().getMaxTurnRate().unmodify(SHEATH_ARMOR);
            shipAPI.getMutableStats().getTurnAcceleration().unmodify(SHEATH_ARMOR);

            shipAPI.setDefenseDisabled(false);

            shipAPI.setAlphaMult(1f);
        }




        if (customData.mode == ARCData.Mode.BERSERK) {






            if (softFluxLevel > 0.1f) { //we have enough to do super warp
                shipAPI.getMutableStats().getTimeMult().modifyMult(SHEATH_BERSERK,  (customData.modeAbilityScale + 1f) * 5f);
                shipAPI.getFluxTracker().decreaseFlux(maxFlux / 200f); //succc

            } else {
                shipAPI.getMutableStats().getTimeMult().modifyMult(SHEATH_BERSERK, (fluxLevel + 1f) * 1.1f);
            }

            //ANGRY
            shipAPI.setWeaponGlow(1f, AIMBOT, EnumSet.of(WeaponAPI.WeaponType.BALLISTIC, WeaponAPI.WeaponType.ENERGY));

        } else {
            shipAPI.setWeaponGlow(0f, AIMBOT, EnumSet.of(WeaponAPI.WeaponType.BALLISTIC, WeaponAPI.WeaponType.ENERGY));
            shipAPI.getMutableStats().getTimeMult().unmodify(SHEATH_BERSERK);
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
