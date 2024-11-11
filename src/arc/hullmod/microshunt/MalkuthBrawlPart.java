package arc.hullmod.microshunt;

import arc.hullmod.IHullmodPart;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;
import java.util.*;

import static arc.util.AIUtil.*;


public class MalkuthBrawlPart implements IHullmodPart<Void> {
    private static final boolean DEBUG_ENABLED = true;

    // Reloaded per frame
    private ShipAPI ship;
    private CombatEngineAPI engine;
    private String personality;
    private float targetRange;
    private float timeToEstimate;
    private AiOverhaulData data;

    public static final Map<String, Float> LOW_DAMAGE;
    static{
        HashMap<String, Float> tmp = new HashMap<>();
        tmp.put(Personalities.TIMID, 150f);
        tmp.put(Personalities.CAUTIOUS, 200f);
        tmp.put(Personalities.STEADY, 250f);
        tmp.put(Personalities.AGGRESSIVE, 300f);
        tmp.put(Personalities.RECKLESS, 350f);
        LOW_DAMAGE = Collections.unmodifiableMap(tmp);
    }

    public static final Map<String, Float> MEDIUM_DAMAGE;
    static{
        HashMap<String, Float> tmp = new HashMap<>();
        tmp.put(Personalities.TIMID, 400f);
        tmp.put(Personalities.CAUTIOUS, 500f);
        tmp.put(Personalities.STEADY, 600f);
        tmp.put(Personalities.AGGRESSIVE, 700f);
        tmp.put(Personalities.RECKLESS, 800f);
        MEDIUM_DAMAGE = Collections.unmodifiableMap(tmp);
    }

    public static final Map<String, Float> HIGH_DAMAGE;
    static{
        HashMap<String, Float> tmp = new HashMap<>();
        tmp.put(Personalities.TIMID, 750f);
        tmp.put(Personalities.CAUTIOUS, 1000f);
        tmp.put(Personalities.STEADY, 1250f);
        tmp.put(Personalities.AGGRESSIVE, 1500f);
        tmp.put(Personalities.RECKLESS, 1750f);
        HIGH_DAMAGE = Collections.unmodifiableMap(tmp);
    }

    @Override
    public void advanceSafely(CombatEngineAPI engineAPI, ShipAPI shipAPI, float timestep, Void customData) {

    }

    @Override
    public boolean hasData() {
        return false;
    }

    @Override
    public boolean makesNewData() {
        return false;
    }

    @Override
    public Void makeNew() {
        return null;
    }

    @Override
    public String makeKey() {
        return null;
    }

    private static class AiOverhaulData {
        public IntervalUtil tracker = new IntervalUtil(0.5F, 1F);
        public Map<ShipAPI, Map<String, Float>> nearbyEnemies = new HashMap<>();
        public ShipAPI target;
        public Vector2f shipTargetPoint;
        public float shipStrafeAngle;
        public boolean ventingHardFlux, ventingSoftFlux, rechargeCharges;
    }

    public void loadPersistentVariables(ShipAPI ship){
        this.ship = ship;
        this.engine = Global.getCombatEngine();

        this.personality = (ship.getCaptain() != null && ship.getCaptain().getPersonalityAPI() != null) ? ship.getCaptain().getPersonalityAPI().getId() : Personalities.STEADY;

        timeToEstimate = ship.getPhaseCloak().getChargeDownDur() + ship.getPhaseCloak().getCooldown();

        // Calculate target ranges for different personalities
        int workingWeapons = 0;
        float maxRange = 0, avgRange = 0, minRange = Float.POSITIVE_INFINITY;

        for (WeaponAPI weapon: ship.getAllWeapons()){
            if(!weapon.isDecorative() && !weapon.hasAIHint(WeaponAPI.AIHints.PD) && weapon.getType() != WeaponAPI.WeaponType.MISSILE){
                float currentRange = weapon.getRange();
                workingWeapons += 1;
                avgRange += currentRange;
                minRange = Math.min(currentRange, minRange);
                maxRange = Math.max(currentRange, maxRange);
            }
        }
        avgRange = avgRange/workingWeapons;

        switch (personality) {
            case Personalities.TIMID:
                targetRange = maxRange;
                break;
            case Personalities.CAUTIOUS:
            case Personalities.STEADY:
                targetRange = avgRange;
                break;
            case Personalities.AGGRESSIVE:
            case Personalities.RECKLESS:
                targetRange = minRange;
                break;
        }

        String id = ship.getId();
        Map<String, Object> customCombatData = Global.getCombatEngine().getCustomData();
        if (customCombatData.get("aioverhaul_data" + id) instanceof AiOverhaulData)
            data = (AiOverhaulData) customCombatData.get("aioverhaul_data" + id);
        else {
            data = new AiOverhaulData();
            customCombatData.put("aioverhaul_data" + id, data);
        }

    }


    public void advanceInCombat(ShipAPI ship, float amount) {

        CombatEngineAPI engine = Global.getCombatEngine();

        if(!ship.isAlive() || ship.getParentStation() != null || engine == null || !engine.isEntityInPlay(ship)){
            return;
        }

        loadPersistentVariables(ship);

        if(data.target == null || !data.target.isAlive()){
            getOptimalTarget();
            return;
        }


        // Calculate Decision Flags
        float futureDamage = getFuturePointDamageEstimate(ship, ship.getLocation(), timeToEstimate, true);

        Color test = Color.blue;
        boolean lowFutureDamage = futureDamage > LOW_DAMAGE.get(personality);
        boolean mediumFutureDamage = futureDamage > MEDIUM_DAMAGE.get(personality);
        boolean highFutureDamage = futureDamage > HIGH_DAMAGE.get(personality);

        if(DEBUG_ENABLED) {
            test = lowFutureDamage ? Color.green : test;
            test = mediumFutureDamage ? Color.yellow : test;
            test = highFutureDamage ? Color.red : test;
            engine.addSmoothParticle(ship.getLocation(), ship.getVelocity(), 200f, 100f, 0.1f, test);
        }

        float totalFlux = ship.getCurrFlux();
        float hardFlux = ship.getFluxTracker().getHardFlux();
        float maxFlux = ship.getMaxFlux();

        float softFluxLevel = (totalFlux-hardFlux)/(maxFlux-hardFlux);

        if (!data.ventingSoftFlux && softFluxLevel > 0.3f)
            data.ventingSoftFlux = true;
        if (data.ventingSoftFlux && softFluxLevel < 0.1f)
            data.ventingSoftFlux = false;

        float targetVulnerability = (float) Math.pow((Math.max(data.target.getFluxLevel(), (1-data.target.getHullLevel()))), 2);

        if (!data.ventingHardFlux && ship.getHardFluxLevel() > ((1f - targetVulnerability) * 0.5f + targetVulnerability * 0.85f))
            data.ventingHardFlux = true;
        if (data.ventingHardFlux && ship.getHardFluxLevel() < 0.01f)
            data.ventingHardFlux = false;
/*

        if (!data.rechargeCharges && lowestWeaponAmmoLevel(ship) < 0.1f)
            data.rechargeCharges = true;
        if (data.rechargeCharges && lowestWeaponAmmoLevel(ship) > 0.5f)
            data.rechargeCharges = false;
*/

        boolean wantToPhase = false;
        if(data.ventingHardFlux){
            if (lowFutureDamage || ship.getEngineController().isFlamedOut()){
                if(ship.getFluxLevel() < 0.60f && ship.isPhased())
                    wantToPhase = true;
                if(ship.getFluxLevel() < 0.45f && !ship.isPhased())
                    wantToPhase = true;
            }
            if (mediumFutureDamage || ship.getEngineController().isFlamedOut()){
                if(ship.getFluxLevel() < 0.80f && ship.isPhased())
                    wantToPhase = true;
                if(ship.getFluxLevel() < 0.65f && !ship.isPhased())
                    wantToPhase = true;
            }
            if (highFutureDamage){
                wantToPhase = true;
            }

        } else{
            if(MathUtils.getDistance(ship.getLocation(), data.target.getLocation()) < targetRange + Misc.getTargetingRadius(ship.getLocation(), data.target, false)) {
                if ((mediumFutureDamage || data.ventingSoftFlux || data.rechargeCharges))
                    wantToPhase = true;

            } else{
                if (lowFutureDamage || data.ventingSoftFlux || data.rechargeCharges || ship.getHardFluxLevel() < 0.1f || ship.getEngineController().isFlamedOut())
                    wantToPhase = true;
            }
        }

        if (!engine.isUIAutopilotOn() || engine.getPlayerShip() != ship) {
            if (ship.isPhased() ^ wantToPhase)
                ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
            else
                ship.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK);
        }

        CombatFleetManagerAPI fleetManager = engine.getFleetManager(ship.getOwner());
        CombatTaskManagerAPI taskManager = (fleetManager != null) ? fleetManager.getTaskManager(ship.isAlly()) : null;
        CombatFleetManagerAPI.AssignmentInfo assignmentInfo = (taskManager != null) ? taskManager.getAssignmentFor(ship) : null;
        CombatAssignmentType assignmentType = (assignmentInfo != null) ? assignmentInfo.getType() : null;

        if (data.shipTargetPoint != null && (assignmentType == CombatAssignmentType.SEARCH_AND_DESTROY || assignmentType == null)){
            Vector2f shipStrafePoint = MathUtils.getPointOnCircumference(ship.getLocation(), ship.getCollisionRadius(), data.shipStrafeAngle);
            strafeToPoint(shipStrafePoint);
            ship.setShipTarget(data.target);

            float damageEstimateVent = getFuturePointDamageEstimate(ship, ship.getLocation(), ship.getFluxTracker().getTimeToVent(), true);
            if(data.ventingHardFlux && getPointDanger(ship.getLocation()) == 0f && damageEstimateVent < LOW_DAMAGE.get(personality)){

                ship.giveCommand(ShipCommand.VENT_FLUX, null, 0);
            }else{
                ship.blockCommandForOneFrame(ShipCommand.VENT_FLUX);
            }
        }

        if(DEBUG_ENABLED){
            if (data.shipTargetPoint != null) {
                engine.addSmoothParticle(data.shipTargetPoint, ship.getVelocity(), 50f, 5f, 0.1f, Color.blue);
                Vector2f shipStrafePoint = MathUtils.getPointOnCircumference(ship.getLocation(), ship.getCollisionRadius(), data.shipStrafeAngle);
                engine.addSmoothParticle(shipStrafePoint, ship.getVelocity(), 50f, 5f, 0.1f, Color.blue);
            }
        }



        data.tracker.advance(amount);
        if (data.tracker.intervalElapsed()) {
            getOptimalTarget();
        }

    }

    private void getOptimalTarget(){
        // Cache any newly detected enemies
        List<ShipAPI> foundEnemies = AIUtils.getNearbyEnemies(ship, 5000f);
        for(ShipAPI foundEnemy: foundEnemies){
            if(!data.nearbyEnemies.containsKey(foundEnemy) && foundEnemy.isAlive() && !foundEnemy.isFighter()){
                Map<String, Float> shipStats = getShipStats(foundEnemy, targetRange);
                data.nearbyEnemies.put(foundEnemy, shipStats);
            }
        }

        Set<ShipAPI> deadEnemies = new HashSet<>();
        for (ShipAPI enemy : data.nearbyEnemies.keySet()){
            if (!enemy.isAlive())
                deadEnemies.add(enemy);
        }
        data.nearbyEnemies.keySet().removeAll(deadEnemies);

        // Calculate ship strafe locations
        if (data.nearbyEnemies.size() >= 1 ) {
            if(this.data.ventingHardFlux) {
                data.shipTargetPoint = getBackingOffStrafePoint();
            } else {
                Vector2f offensiveStrafePoint = getOffensiveStrafePoint();
                data.shipTargetPoint = offensiveStrafePoint != null ? offensiveStrafePoint : getBackingOffStrafePoint();
            }
            if (data.shipTargetPoint != null) {
                float currentShipStrafeAngle = VectorUtils.getAngle(ship.getLocation(), data.shipTargetPoint);
                float maxLocalDodgeAngle = 75f;
                float degreeDelta = 10f;
                float distanceDelta = 100f;
                float estimatedDamage = getFuturePointDamageEstimate(ship, ship.getLocation(), timeToEstimate, true);
                float currentSumDistance = getSumDistance(ship.getLocation());
                float currentTargetDistance = MathUtils.getDistanceSquared(MathUtils.getPointOnCircumference(ship.getLocation(), 100, currentShipStrafeAngle), ship.getLocation());
                float newShipStrafeAngle = currentShipStrafeAngle;

                float currentDistance = ship.getCollisionRadius();
                while (estimatedDamage > 50f && currentDistance < 500f){
                    float currentAngle = currentShipStrafeAngle - maxLocalDodgeAngle;
                    while(currentAngle < currentShipStrafeAngle + maxLocalDodgeAngle){
                        Vector2f potentialPoint = MathUtils.getPointOnCircumference(ship.getLocation(), currentDistance, currentAngle);
                        float potentialFutureDamage = getFuturePointDamageEstimate(ship, potentialPoint, timeToEstimate, false);
                        boolean furtherAndBackingOff = false;
                        boolean closerAndAttacking = false;
                        if (data.ventingHardFlux){
                            furtherAndBackingOff = getSumDistance(potentialPoint) > currentSumDistance && data.ventingHardFlux;
                        } else{
                            float targetDistanceSquared = MathUtils.getDistanceSquared(data.target.getLocation(), potentialPoint);
                            closerAndAttacking = targetDistanceSquared < currentTargetDistance && targetDistanceSquared > Math.pow(data.target.getCollisionRadius()*2,2);
                        }

                        if ((closerAndAttacking || furtherAndBackingOff) && potentialFutureDamage < estimatedDamage) {
                            newShipStrafeAngle = currentAngle;
                            estimatedDamage = potentialFutureDamage;
                        }
                        currentAngle += degreeDelta;
                    }
                    currentDistance += distanceDelta;
                }
                data.shipStrafeAngle = newShipStrafeAngle;
            }
        }
    }


    private Vector2f getBackingOffStrafePoint(){

        float degreeDelta = 5f;

        List<Vector2f> potentialPoints = MathUtils.getPointsAlongCircumference(ship.getLocation(), ship.getCollisionRadius()*2, (int) (360f/degreeDelta), 0);
        Vector2f safestPoint = null;
        float furthestPointSumDistance = 0;
        for (Vector2f potentialPoint : potentialPoints) {
            float currentPointSumDistance = getSumDistance(potentialPoint);
            if(currentPointSumDistance > furthestPointSumDistance){
                furthestPointSumDistance = currentPointSumDistance;
                safestPoint = potentialPoint;
            }
        }

        return safestPoint;
    }

    private float getSumDistance(Vector2f potentialPoint){
        float currentPointSumDistance = 0;
        for(ShipAPI enemy : data.nearbyEnemies.keySet()){
            currentPointSumDistance += MathUtils.getDistance(enemy, potentialPoint);
        }
        return currentPointSumDistance;
    }

    private Vector2f getOffensiveStrafePoint(){

        float maxAngle = 60f;

        float degreeDelta = 5f;

        //get the all the "outside" ships
        List<ShipAPI> enemyShipsOnConvexHull = getConvexHull(new ArrayList<>(data.nearbyEnemies.keySet()));

        Map<ShipAPI, List<Vector2f>> targetEnemys = new HashMap<>();

        // add all the target points from enemies on the "outside edges"
        for(ShipAPI enemy : enemyShipsOnConvexHull){
            Vector2f pokeFactor = VectorUtils.getDirectionalVector(enemy.getLocation(), ship.getLocation());
            // Remove ships if they are in the "center"
            if (checkInBounds(enemyShipsOnConvexHull, Vector2f.add(new Vector2f(enemy.getLocation()), pokeFactor, null)) ||
                    checkInBounds(enemyShipsOnConvexHull, Vector2f.add(new Vector2f(enemy.getLocation()), (Vector2f) pokeFactor.scale(-1f), null))){
                continue;
            }
            float optimalRange = targetRange + Misc.getTargetingRadius(ship.getLocation(), enemy, false);

            List<Vector2f> potentialPoints = new ArrayList<>();
            float enemyAngle = VectorUtils.getAngle(enemy.getLocation(), ship.getLocation());
            float currentAngle = enemyAngle - maxAngle;
            while(currentAngle < enemyAngle + maxAngle){
                potentialPoints.add(MathUtils.getPointOnCircumference(enemy.getLocation(), optimalRange, currentAngle));
                currentAngle += degreeDelta;
            }
            targetEnemys.put(enemy, potentialPoints);
            //targetEnemys.put(enemy, MathUtils.getPointsAlongCircumference(enemy.getLocation(), optimalRange, (int) (360f/degreeDelta), 0));
        }

        // remove the points that fall into the ranges of other enemy ships, or requires ship to fly through other ships
        for(ShipAPI enemy : data.nearbyEnemies.keySet()){
            for(Map.Entry<ShipAPI, List<Vector2f>> targetEnemy : targetEnemys.entrySet()){
                List<Vector2f> pointsToRemove = new ArrayList<>();
                for(Vector2f potentialPoint : targetEnemy.getValue()){
                    float optimalRange = targetRange + Misc.getTargetingRadius(ship.getLocation(), enemy, false);
                    float keepoutRadius = MathUtils.getDistance(ship.getLocation(), enemy.getLocation()) > optimalRange ? (float) (optimalRange * 0.8) : enemy.getCollisionRadius()*1.2f;
                    if(CollisionUtils.getCollides(potentialPoint, ship.getLocation(), enemy.getLocation(), keepoutRadius)){
                        pointsToRemove.add(potentialPoint);
                    }
                }
                targetEnemy.getValue().removeAll(pointsToRemove);
            }
        }

        // from the points left, find the safest target to attack and where to strafe to
        Vector2f optimalStrafePoint = null;
        float currentDanger = Float.POSITIVE_INFINITY;

        for(Map.Entry<ShipAPI, List<Vector2f>> targetEnemy : targetEnemys.entrySet()) {
            for (Vector2f potentialPoint : targetEnemy.getValue()) {
                float pointDanger = getPointDanger(potentialPoint);

                if(potentialPoint == null || engine == null){
                    int test = 5;
                }

                if(DEBUG_ENABLED){
                    engine.addFloatingText(potentialPoint, String.valueOf(pointDanger), 10, Color.white, null, 0, 0);
                }

                if (pointDanger < currentDanger) {
                    optimalStrafePoint = potentialPoint;
                    data.target = targetEnemy.getKey();
                    currentDanger = pointDanger;
                }
            }
        }
        return optimalStrafePoint;
    }

    private float getPointDanger(Vector2f testPoint){
        float currentPointDanger = 0f;
        if (testPoint == null)
            return 0f;

        for (ShipAPI enemy: data.nearbyEnemies.keySet()) {

            float currentTargetBias = (enemy == data.target && MathUtils.getDistance(ship.getLocation(), enemy.getLocation()) < targetRange * 1.2) ? 0.1f : 1f;

            float highestDPSAngle = enemy.getFacing() + data.nearbyEnemies.get(enemy).get("HighestDPSAngle");
            float alpha = Math.abs(MathUtils.getShortestRotation(VectorUtils.getAngle(enemy.getLocation(), testPoint), highestDPSAngle))/180f;

            float DPSDanger = (1f - alpha) * data.nearbyEnemies.get(enemy).get("HighestDPS") + alpha * data.nearbyEnemies.get(enemy).get("LowestDPS");

            float shipDistance = MathUtils.getDistance(enemy.getLocation(), testPoint);
            float currentlyOccluded = 1;
            for (ShipAPI otherEnemy: data.nearbyEnemies.keySet()) {
                if (otherEnemy != enemy && CollisionUtils.getCollides(enemy.getLocation(), testPoint, otherEnemy.getLocation(), otherEnemy.getCollisionRadius())){
                    currentlyOccluded = 0;
                }
            }
            if(shipDistance < data.nearbyEnemies.get(enemy).get("MaxRange")){
                currentPointDanger += (DPSDanger - shipDistance/100) * (1-enemy.getFluxLevel()) * enemy.getHullLevel() * currentTargetBias * currentlyOccluded;
            }
        }
        return currentPointDanger;
    }

    private Map<String, Float> getShipStats(ShipAPI newEnemy, float defaultRange){

        float deltaAngle = 1f;
        float currentRelativeAngle = 0f;
        Map<String, Float> highLowDPS = new HashMap<>();
        float highestDPS = 0;
        float lowestDPS = Float.POSITIVE_INFINITY;
        float highestDPSAngle = 0;
        float lowestDPSAngle = 0;
        float maxRange = 0;

        while(currentRelativeAngle <= 360){
            float potentialDPS = 0;
            for (WeaponAPI weapon: newEnemy.getAllWeapons()){
                if(!weapon.isDecorative() && weapon.getType() != WeaponAPI.WeaponType.MISSILE){
                    if((Math.abs(weapon.getArcFacing() - currentRelativeAngle) < weapon.getArc()/2) && (defaultRange < weapon.getRange())){
                        potentialDPS += Math.max(weapon.getDerivedStats().getDps(), weapon.getDerivedStats().getBurstDamage());
                    }
                    maxRange = Math.max(weapon.getRange(), maxRange);
                }
            }
            if(potentialDPS > highestDPS){
                highestDPS = potentialDPS;
                highestDPSAngle = currentRelativeAngle;
            }
            if(potentialDPS < lowestDPS){
                lowestDPS = potentialDPS;
                lowestDPSAngle = currentRelativeAngle;
            }

            currentRelativeAngle += deltaAngle;
        }

        highLowDPS.put("HighestDPS", highestDPS);
        highLowDPS.put("LowestDPS", lowestDPS);
        highLowDPS.put("HighestDPSAngle", highestDPSAngle);
        highLowDPS.put("LowestDPSAngle", lowestDPSAngle);
        highLowDPS.put("MaxRange", maxRange);
        return highLowDPS;
    }

    private void strafeToPoint(Vector2f strafePoint){
        float strafeAngle = VectorUtils.getAngle(ship.getLocation(), strafePoint);
        float rotAngle = MathUtils.getShortestRotation(ship.getFacing(), strafeAngle);



        if (rotAngle < 67.5f && rotAngle > -67.5f) {
            ship.giveCommand(ShipCommand.ACCELERATE, null, 0);
            ship.blockCommandForOneFrame(ShipCommand.ACCELERATE_BACKWARDS);
            if(DEBUG_ENABLED) {
                Vector2f point = MathUtils.getPointOnCircumference(ship.getLocation(), 100f, ship.getFacing());
                engine.addSmoothParticle(point, ship.getVelocity(), 30f, 1f, 0.1f, Color.green);
            }
        }

        if (rotAngle > 112.5f || rotAngle < -112.5f){
            ship.giveCommand(ShipCommand.ACCELERATE_BACKWARDS, null,0);
            ship.blockCommandForOneFrame(ShipCommand.ACCELERATE);
            if(DEBUG_ENABLED) {
                Vector2f point = MathUtils.getPointOnCircumference(ship.getLocation(), 100f, ship.getFacing()+180f);
                engine.addSmoothParticle(point, ship.getVelocity(), 30f, 1f, 0.1f, Color.green);
            }
        }

        if (rotAngle > 22.5f && rotAngle < 157.5f){
            ship.giveCommand(ShipCommand.STRAFE_LEFT, null,0);
            ship.blockCommandForOneFrame(ShipCommand.STRAFE_RIGHT);
            if(DEBUG_ENABLED) {
                Vector2f point = MathUtils.getPointOnCircumference(ship.getLocation(), 100f, ship.getFacing()+90f);
                engine.addSmoothParticle(point, ship.getVelocity(), 30f, 1f, 0.1f, Color.green);
            }
        }

        if (rotAngle < -22.5f && rotAngle > -157.5f){
            ship.giveCommand(ShipCommand.STRAFE_RIGHT, null,0);
            ship.blockCommandForOneFrame(ShipCommand.STRAFE_LEFT);
            if(DEBUG_ENABLED) {
                Vector2f point = MathUtils.getPointOnCircumference(ship.getLocation(), 100f, ship.getFacing()-90f);
                engine.addSmoothParticle(point, ship.getVelocity(), 30f, 1f, 0.1f, Color.green);
            }
        }
    }

}
