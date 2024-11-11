package arc.shipsystems.boost;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.ShipEngineControllerAPI.ShipEngineAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//import org.apache.log4j.private static Logger;
public class Boost extends BaseShipSystemScript {

    public static final float MAX_TURN_BONUS = 50f;
    public static final float TURN_ACCEL_BONUS = 50f;
    public static final float INSTANT_BOOST_FLAT = 300f;
    public static final float INSTANT_BOOST_MULT = 5f;
    public static final Map<HullSize, Float> SPEED_FALLOFF_PER_SEC = new HashMap<>();
    public static final Map<HullSize, Float> FORWARD_PENALTY = new HashMap<>(); // from base 0.75
    public static final Map<HullSize, Float> REVERSE_PENALTY = new HashMap<>(); // from base 0.75
    public static final Map<HullSize, Float> IN_OVERRIDE = new HashMap<>();
    public static final Map<HullSize, Float> ACTIVE_OVERRIDE = new HashMap<>();
    public static final Map<HullSize, Float> OUT_OVERRIDE = new HashMap<>();
    public static final Map<HullSize, Integer> USES_OVERRIDE = new HashMap<>();
    public static final Map<HullSize, Float> REGEN_OVERRIDE = new HashMap<>();

    private static final Map<HullSize, Float> EXTEND_TIME = new HashMap<>();
    private static final Map<HullSize, Float> MAX_FRAC_OUT = new HashMap<>();
    private static final Map<HullSize, Float> BOOST_MULT = new HashMap<>();

    static {
    	SPEED_FALLOFF_PER_SEC.put(HullSize.FIGHTER, 0.70f);
        SPEED_FALLOFF_PER_SEC.put(HullSize.FRIGATE, 0.55f); 
        SPEED_FALLOFF_PER_SEC.put(HullSize.DESTROYER, 0.375f);
        SPEED_FALLOFF_PER_SEC.put(HullSize.CRUISER, 0.25f);
        SPEED_FALLOFF_PER_SEC.put(HullSize.CAPITAL_SHIP, 0.225f); 

        FORWARD_PENALTY.put(HullSize.FIGHTER, 0.40f); 
        FORWARD_PENALTY.put(HullSize.FRIGATE, 0.35f); 
        FORWARD_PENALTY.put(HullSize.DESTROYER, 0.25f);
        FORWARD_PENALTY.put(HullSize.CRUISER, 0.2f); 
        FORWARD_PENALTY.put(HullSize.CAPITAL_SHIP, 0.2f);

        REVERSE_PENALTY.put(HullSize.FIGHTER, 0.45f); 
        REVERSE_PENALTY.put(HullSize.FRIGATE, 0.45f); 
        REVERSE_PENALTY.put(HullSize.DESTROYER, 0.45f);
        REVERSE_PENALTY.put(HullSize.CRUISER, 0.35f); 
        REVERSE_PENALTY.put(HullSize.CAPITAL_SHIP, 0.4f); 

        IN_OVERRIDE.put(HullSize.FIGHTER, 0.2f);
        IN_OVERRIDE.put(HullSize.FRIGATE, 0.2f);
        IN_OVERRIDE.put(HullSize.DESTROYER, 0.2f);
        IN_OVERRIDE.put(HullSize.CRUISER, 0.2f);
        IN_OVERRIDE.put(HullSize.CAPITAL_SHIP, 0.2f);

        ACTIVE_OVERRIDE.put(HullSize.FIGHTER, 0.2f);
        ACTIVE_OVERRIDE.put(HullSize.FRIGATE, 0.2f);
        ACTIVE_OVERRIDE.put(HullSize.DESTROYER, 0.2f);
        ACTIVE_OVERRIDE.put(HullSize.CRUISER, 0.2f);
        ACTIVE_OVERRIDE.put(HullSize.CAPITAL_SHIP, 0.2f);

        OUT_OVERRIDE.put(HullSize.FIGHTER, 0.6f);
        OUT_OVERRIDE.put(HullSize.FRIGATE, 0.6f);
        OUT_OVERRIDE.put(HullSize.DESTROYER, 0.8f);
        OUT_OVERRIDE.put(HullSize.CRUISER, 0.9f);
        OUT_OVERRIDE.put(HullSize.CAPITAL_SHIP, 1f);

        USES_OVERRIDE.put(HullSize.FIGHTER, 3);
        USES_OVERRIDE.put(HullSize.FRIGATE, 3);
        USES_OVERRIDE.put(HullSize.DESTROYER, 2);
        USES_OVERRIDE.put(HullSize.CRUISER, 2);
        USES_OVERRIDE.put(HullSize.CAPITAL_SHIP, 2);

        REGEN_OVERRIDE.put(HullSize.FIGHTER, 0.5f);
        REGEN_OVERRIDE.put(HullSize.FRIGATE, 0.3f);
        REGEN_OVERRIDE.put(HullSize.DESTROYER, 0.125f);
        REGEN_OVERRIDE.put(HullSize.CRUISER, 0.1f);
        REGEN_OVERRIDE.put(HullSize.CAPITAL_SHIP, 0.075f);

        EXTEND_TIME.put(HullSize.FIGHTER, 0.1f);
        EXTEND_TIME.put(HullSize.FRIGATE, 0.1f);
        EXTEND_TIME.put(HullSize.DESTROYER, 0.1f);
        EXTEND_TIME.put(HullSize.CRUISER, 0.1f);
        EXTEND_TIME.put(HullSize.CAPITAL_SHIP, 0.1f);

        MAX_FRAC_OUT.put(HullSize.FIGHTER, 0.15f / OUT_OVERRIDE.get(HullSize.FIGHTER));
        MAX_FRAC_OUT.put(HullSize.FRIGATE, 0.15f / OUT_OVERRIDE.get(HullSize.FRIGATE));
        MAX_FRAC_OUT.put(HullSize.DESTROYER, 0.15f / OUT_OVERRIDE.get(HullSize.DESTROYER));
        MAX_FRAC_OUT.put(HullSize.CRUISER, 0.15f / OUT_OVERRIDE.get(HullSize.CRUISER));
        MAX_FRAC_OUT.put(HullSize.CAPITAL_SHIP, 0.15f / OUT_OVERRIDE.get(HullSize.CAPITAL_SHIP));

        BOOST_MULT.put(HullSize.FIGHTER, 3f);
        BOOST_MULT.put(HullSize.FRIGATE, 3f);
        BOOST_MULT.put(HullSize.DESTROYER, 3f);
        BOOST_MULT.put(HullSize.CRUISER, 3f);
        BOOST_MULT.put(HullSize.CAPITAL_SHIP, 3f);
    }

    private static final Color ENGINE_COLOR_STANDARD = new Color(10, 226, 255);
    private static final Color CONTRAIL_COLOR_STANDARD = new Color(100, 162, 255, 75);
    private static final Color BOOST_COLOR_STANDARD = new Color(175, 198, 255, 200);

    private static final Vector2f ZERO = new Vector2f();
    private final Object ENGINEKEY1 = new Object();
    private final Object ENGINEKEY2 = new Object();

    private final Map<Integer, Float> engState = new HashMap<>();

    private boolean started = false;
    private boolean ended = false;
    private final IntervalUtil interval = new IntervalUtil(0.015f, 0.015f);
    private float boostScale = 0.75f;
    private float boostVisualDir = 0f;
    private boolean boostForward = false;

//    private private static Logger log = Global.getprivate static Logger(yrxp_AegisCruiserPurgeStats.class);   
    
    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) {
            return;
        }

        
        float shipRadius = ship.getCollisionRadius();
        float amount = Global.getCombatEngine().getElapsedInLastFrame();
        if (Global.getCombatEngine().isPaused()) {
            amount = 0f;
        }

        Color ENGINE_COLOR = ENGINE_COLOR_STANDARD;
        Color CONTRAIL_COLOR = CONTRAIL_COLOR_STANDARD;
        Color BOOST_COLOR = BOOST_COLOR_STANDARD;
        float afterimageIntensity = 1f;

        ship.getEngineController().fadeToOtherColor(ENGINEKEY1, ENGINE_COLOR, CONTRAIL_COLOR, effectLevel, 1f);
        ship.getEngineController().extendFlame(ENGINEKEY2, 0f, 1f * effectLevel, 3f * effectLevel);

        
//        log.info("STATE: " + state);
        
        if (!ended) {
            /* Unweighted direction calculation for visual purposes - 0 degrees is forward */
            Vector2f direction = new Vector2f();
            if (ship.getEngineController().isAccelerating()) {
                direction.y += 1f;
            } else if (ship.getEngineController().isAcceleratingBackwards() || ship.getEngineController().isDecelerating()) {
                direction.y -= 1f;
            }
            if (ship.getEngineController().isStrafingLeft()) {
                direction.x -= 1f;
            } else if (ship.getEngineController().isStrafingRight()) {
                direction.x += 1f;
            }
            if (direction.length() <= 0f) {
                direction.y = 1f;
            }
            boostVisualDir = MathUtils.clampAngle(VectorUtils.getFacing(direction) - 90f);
        }

        if (state == State.IN) {
            if (!started) {
                Global.getSoundPlayer().playSound("system_phase_skimmer", 1f, 1f, ship.getLocation(), ZERO);

                started = true;
            }

            List<ShipEngineAPI> engList = ship.getEngineController().getShipEngines();
            for (int i = 0; i < engList.size(); i++) {
                ShipEngineAPI eng = engList.get(i);
                if (eng.isSystemActivated()) {
                    float targetLevel = getSystemEngineScale(eng, boostVisualDir) * 0.4f;
                    Float currLevel = engState.get(i);
                    if (currLevel == null) {
                        currLevel = 0f;
                    }
                    if (currLevel > targetLevel) {
                        currLevel = Math.max(targetLevel, currLevel - (amount / EXTEND_TIME.get(ship.getHullSize())));
                    } else {
                        currLevel = Math.min(targetLevel, currLevel + (amount / EXTEND_TIME.get(ship.getHullSize())));
                    }
                    engState.put(i, currLevel);
                    ship.getEngineController().setFlameLevel(eng.getEngineSlot(), currLevel);
                }
            }
        }

        if (state == State.OUT || state == State.IN  || state == State.ACTIVE) {
            /* Black magic to counteract the effects of maneuvering penalties/bonuses on the effectiveness of this system */
            float decelMult = Math.max(0.5f, Math.min(2f, stats.getDeceleration().getModifiedValue() / stats.getDeceleration().getBaseValue()));
            float adjFalloffPerSec = SPEED_FALLOFF_PER_SEC.get(ship.getHullSize()) * (float) Math.pow(decelMult, 0.5);
            float maxDecelPenalty = 1f / decelMult;

            stats.getMaxTurnRate().unmodify(id);
            stats.getDeceleration().modifyMult(id, this.lerp(1f, maxDecelPenalty, effectLevel));
            stats.getTurnAcceleration().modifyPercent(id, TURN_ACCEL_BONUS * effectLevel);

            if (boostForward) {
                ship.giveCommand(ShipCommand.ACCELERATE, null, 0);
                ship.blockCommandForOneFrame(ShipCommand.ACCELERATE_BACKWARDS);
                ship.blockCommandForOneFrame(ShipCommand.DECELERATE);
            } else {
//                ship.blockCommandForOneFrame(ShipCommand.ACCELERATE);
            }

            /* Quickly unapply the instant repair buff */
            stats.getCombatEngineRepairTimeMult().unmodify(id);

            if (amount > 0f) {
                ship.getVelocity().scale((float) Math.pow(adjFalloffPerSec, amount));
            }

//            interval.advance(amount);
//            if (interval.intervalElapsed()) {
                float randRange = (float) Math.sqrt(shipRadius) * 0.5f * afterimageIntensity * boostScale;
                Vector2f randLoc = MathUtils.getRandomPointInCircle(ZERO, randRange);
                Color afterimageColor = new Color(CONTRAIL_COLOR.getRed(), CONTRAIL_COLOR.getGreen(), CONTRAIL_COLOR.getBlue(),
                        this.clamp255(Math.round(0.15f * afterimageIntensity * CONTRAIL_COLOR.getAlpha())));
                ship.addAfterimage(afterimageColor, randLoc.x, randLoc.y, -ship.getVelocity().x, -ship.getVelocity().y,
                        randRange, 0f, 0.1f, 0.5f, true, false, false);
//            }

            List<ShipEngineAPI> engList = ship.getEngineController().getShipEngines();
            for (int i = 0; i < engList.size(); i++) {
                ShipEngineAPI eng = engList.get(i);
                if (eng.isSystemActivated()) {
                    float targetLevel = getSystemEngineScale(eng, boostVisualDir) * effectLevel;
                    if (targetLevel >= (1f - MAX_FRAC_OUT.get(ship.getHullSize()))) {
                        targetLevel = 1f;
                    } else {
                        targetLevel = targetLevel / (1f - MAX_FRAC_OUT.get(ship.getHullSize()));
                    }
                    engState.put(i, targetLevel);
                    ship.getEngineController().setFlameLevel(eng.getEngineSlot(), targetLevel);
                }
            }
        }
        
        if (state == State.ACTIVE) {
        	

        	
            stats.getMaxTurnRate().modifyPercent(id, MAX_TURN_BONUS);
            stats.getTurnAcceleration().modifyPercent(id, TURN_ACCEL_BONUS * effectLevel);

            ship.getEngineController().getExtendLengthFraction().advance(amount * 2f);
            ship.getEngineController().getExtendWidthFraction().advance(amount * 2f);
            ship.getEngineController().getExtendGlowFraction().advance(amount * 2f);
            List<ShipEngineAPI> engList = ship.getEngineController().getShipEngines();
            for (int i = 0; i < engList.size(); i++) {
                ShipEngineAPI eng = engList.get(i);
                if (eng.isSystemActivated()) {
                    float targetLevel = getSystemEngineScale(eng, boostVisualDir);
                    Float currLevel = engState.get(i);
                    if (currLevel == null) {
                        currLevel = 0f;
                    }
                    if (currLevel > targetLevel) {
                        currLevel = Math.max(targetLevel, currLevel - (amount / EXTEND_TIME.get(ship.getHullSize())));
                    } else {
                        currLevel = Math.min(targetLevel, currLevel + (amount / EXTEND_TIME.get(ship.getHullSize())));
                    }
                    engState.put(i, currLevel);
                    ship.getEngineController().setFlameLevel(eng.getEngineSlot(), currLevel);
                }
            }
        }

        if (state == State.OUT || state == State.IN  || state == State.ACTIVE) {
            if (!ended) {
                Vector2f direction = new Vector2f();
                boostForward = false;
                boostScale = 0.75f;
                if (ship.getEngineController().isAccelerating()) {
                    direction.y += 0.75f - FORWARD_PENALTY.get(ship.getHullSize());
                    boostScale -= FORWARD_PENALTY.get(ship.getHullSize());
                    boostForward = true;
                } else if (ship.getEngineController().isAcceleratingBackwards() || ship.getEngineController().isDecelerating()) {
                    direction.y -= 0.75f - REVERSE_PENALTY.get(ship.getHullSize());
                    boostScale -= REVERSE_PENALTY.get(ship.getHullSize());
                }
                if (ship.getEngineController().isStrafingLeft()) {
                    direction.x -= 1f;
                    boostScale += 0.25f;
                    boostForward = false;
                } else if (ship.getEngineController().isStrafingRight()) {
                    direction.x += 1f;
                    boostScale += 0.25f;
                    boostForward = false;
                } else {
                	//If no direction, get random direction

                	if(MathUtils.getRandomNumberInRange(0, 1)> 0){
                        ship.giveCommand(ShipCommand.STRAFE_RIGHT, null, 0);
                        direction.x += 1f;
                	}else {
                        ship.giveCommand(ShipCommand.STRAFE_LEFT, null, 0);
                        direction.x -= 1f;
                	}
                	
                    boostScale += 0.25f;
                    boostForward = false;
                }
                
                
                if (direction.length() <= 0f) {
                    direction.y = 0.75f - FORWARD_PENALTY.get(ship.getHullSize());
                    boostScale -= FORWARD_PENALTY.get(ship.getHullSize());
                }
                direction.normalise();
                VectorUtils.rotate(direction, ship.getFacing() - 90f, direction);
                direction.scale(((ship.getMaxSpeedWithoutBoost() * INSTANT_BOOST_MULT) + INSTANT_BOOST_FLAT) * boostScale);
                Vector2f.add(ship.getVelocity(), direction, ship.getVelocity());
                ended = true;

                float duration = (float) Math.sqrt(shipRadius) / 25f;
                ship.getEngineController().getExtendLengthFraction().advance(1f);
                ship.getEngineController().getExtendWidthFraction().advance(1f);
                ship.getEngineController().getExtendGlowFraction().advance(1f);
                for (ShipEngineAPI eng : ship.getEngineController().getShipEngines()) {
                    float level = 1f;
                    if (eng.isSystemActivated()) {
                        level = getSystemEngineScale(eng, boostVisualDir);
                    }
                    if ((eng.isActive() || eng.isSystemActivated()) && (level > 0f)) {
                        Color bigBoostColor = new Color(
                                this.clamp255(Math.round(0.1f * ENGINE_COLOR.getRed())),
                                this.clamp255(Math.round(0.1f * ENGINE_COLOR.getGreen())),
                                this.clamp255(Math.round(0.1f * ENGINE_COLOR.getBlue())),
                                this.clamp255(Math.round(0.3f * ENGINE_COLOR.getAlpha() * level)));
                        Color boostColor = new Color(BOOST_COLOR.getRed(), BOOST_COLOR.getGreen(), BOOST_COLOR.getBlue(),
                        		this.clamp255(Math.round(BOOST_COLOR.getAlpha() * level)));
                        Global.getCombatEngine().spawnExplosion(eng.getLocation(), ZERO, bigBoostColor,
                                BOOST_MULT.get(ship.getHullSize()) * 4f * boostScale * eng.getEngineSlot().getWidth(), duration);
                        Global.getCombatEngine().spawnExplosion(eng.getLocation(), ZERO, boostColor,
                                BOOST_MULT.get(ship.getHullSize()) * 2f * boostScale * eng.getEngineSlot().getWidth(), 0.15f);
                    }
                }

                float soundScale = (float) Math.sqrt(boostScale);
                switch (ship.getHullSize()) {
                    case FRIGATE:
                        Global.getSoundPlayer().playSound("engine_disabled", 1f, 1f * soundScale, ship.getLocation(), ZERO);
                        break;
                    default:
                    case DESTROYER:
                        Global.getSoundPlayer().playSound("engine_disabled", 0.9f, 1.1f * soundScale, ship.getLocation(), ZERO);
                        break;
                    case CRUISER:
                        Global.getSoundPlayer().playSound("engine_disabled", 0.8f, 1.2f * soundScale, ship.getLocation(), ZERO);
                        break;
                    case CAPITAL_SHIP:
                        Global.getSoundPlayer().playSound("engine_disabled", 0.7f, 1.3f * soundScale, ship.getLocation(), ZERO);
                        break;
                }
            }
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (ship == null) {
            return;
        }

        started = false;
        ended = false;
        boostScale = 0.75f;
        boostVisualDir = 0f;
        boostForward = false;
        engState.clear();

        stats.getCombatEngineRepairTimeMult().unmodify(id);
        stats.getEngineDamageTakenMult().unmodify(id);

        stats.getTimeMult().unmodify(id);
        String globalId = id + "_" + ship.getId();
        Global.getCombatEngine().getTimeMult().unmodify(globalId);

        stats.getMaxTurnRate().unmodify(id);
        stats.getDeceleration().unmodify(id);
        stats.getTurnAcceleration().unmodify(id);

        ship.setJitter(this, ENGINE_COLOR_STANDARD, 0f, 5, 0f, 13f);
        ship.setJitterUnder(this, ENGINE_COLOR_STANDARD, 0f, 25, 0f, 17f);
    }

    @Override
    public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
       
        return true;
    }

    @Override
    public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
        if (ship != null) {
            if (ship.getEngineController().isFlamedOut()) {
                return "FLAMED OUT";
            }
        }
        return null;
    }

    @Override
    public float getInOverride(ShipAPI ship) {
        if (ship != null) {
            return IN_OVERRIDE.get(ship.getHullSize());
        }
        return -1;
    }

    @Override
    public float getActiveOverride(ShipAPI ship) {
        if (ship != null) {
            return ACTIVE_OVERRIDE.get(ship.getHullSize());
        }
        return -1;
    }

    @Override
    public float getOutOverride(ShipAPI ship) {
        if (ship != null) {
            return OUT_OVERRIDE.get(ship.getHullSize());
        }
        return -1;
    }

    @Override
    public int getUsesOverride(ShipAPI ship) {
        if (ship != null) {
            return USES_OVERRIDE.get(ship.getHullSize());
        }
        return -1;
    }

    @Override
    public float getRegenOverride(ShipAPI ship) {
        if (ship != null) {
            return REGEN_OVERRIDE.get(ship.getHullSize());
        }
        return -1;
    }

    public float lerp(float x, float y, float alpha) {
        return (1f - alpha) * x + alpha * y;
    }

    public int clamp255(int x) {
        return Math.max(0, Math.min(255, x));
    }

    private static float getSystemEngineScale(ShipEngineAPI engine, float direction) {
        float engAngle = engine.getEngineSlot().getAngle();
        if (Math.abs(MathUtils.getShortestRotation(engAngle, direction)) > 100f) {
            return 1f;
        } else {
            return 0f;
        }
    }
}
