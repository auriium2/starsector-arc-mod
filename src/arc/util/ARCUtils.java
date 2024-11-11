package arc.util;

import arc.StopgapUtils;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.Misc;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.WaveDistortion;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ARCUtils {


    public static void spawnGarbage(final ShipAPI shipAPI, final Vector2f locale, final Vector2f direction, String weapon) {



        final DamagingProjectileAPI missileAPI = (DamagingProjectileAPI)Global.getCombatEngine().spawnProjectile(
                shipAPI,
                null,
                weapon,
                locale,
                VectorUtils.getFacing(direction) + ((float)Math.random() * 5 - (float)Math.random() * 5 * 0.5f),
                (Vector2f) direction.scale(MathUtils.getRandomNumberInRange(1f,1.5f))
        );
        if (shipAPI != null) {
            Global.getCombatEngine().applyDamageModifiersToSpawnedProjectileWithNullWeapon(shipAPI, WeaponAPI.WeaponType.MISSILE, false, missileAPI.getDamage());
        }
    }

    public static float[] shine(float value) {
        float adjustedValue = value % 0.75f; // Adjust value to cycle every 0.5 increment

        float[] rgb = new float[3];

        // Red
        rgb[0] = (float) Math.sin(adjustedValue * Math.PI * 2); // Full cycle every 0.5 increment
        rgb[0] = Math.max(0, rgb[0]); // Ensure it's not negative
        rgb[0] = Math.min(1, rgb[0]); // Ensure it's not greater than 1

        // Green
        rgb[1] = (float) Math.sin((adjustedValue + 0.5) * Math.PI * 2); // Full cycle every 0.5 increment with phase shift
        rgb[1] = Math.max(0, rgb[1]); // Ensure it's not negative
        rgb[1] = Math.min(1, rgb[1]); // Ensure it's not greater than 1

        // Blue
        rgb[2] = (float) Math.sin((adjustedValue + 0.25) * Math.PI * 2); // Full cycle every 0.5 increment with phase shift
        rgb[2] = Math.max(0, rgb[2]); // Ensure it's not negative
        rgb[2] = Math.min(1, rgb[2]); // Ensure it's not greater than 1

        return rgb;
    }

    public static void spawnMine(final ShipAPI shipAPI, final Vector2f vector2f, String weapon) {
        final MissileAPI missileAPI = (MissileAPI)Global.getCombatEngine().spawnProjectile(shipAPI, null, weapon, vector2f, (float)Math.random() * 360.0f, null);
        if (shipAPI != null) {
            Global.getCombatEngine().applyDamageModifiersToSpawnedProjectileWithNullWeapon(shipAPI, WeaponAPI.WeaponType.MISSILE, false, missileAPI.getDamage());
        }
        final float n = 0.05f;
        missileAPI.getVelocity().scale(0.0f);
        missileAPI.fadeOutThenIn(n);
        missileAPI.setFlightTime(missileAPI.getMaxFlightTime() - 0.0f);
        missileAPI.addDamagedAlready(shipAPI);
        missileAPI.setNoMineFFConcerns(true);
    }

    public static float lerp(float x, float y, float alpha) {
        return (1f - alpha) * x + alpha * y;
    }
    public static float remap(float x1, float y1, float x2, float y2, float alpha) {
        return lerp(x2, y2, invlerp(x1, y1, alpha));
    }
    public static float invlerp(float x, float y, float alpha) {
        return (alpha - x) / (y - alpha);
    }
    public static float remapExponential(float x1, float y1, float x2, float y2, float alpha, float exponent) {
        // Ensure exponent is greater than 0 to avoid invalid calculations
        if (exponent <= 0) {
            throw new IllegalArgumentException("Exponent must be greater than 0");
        }
        // Calculate the normalized position (t) between x1 and y1
        float t = invlerp(x1, y1, alpha);
        // Apply exponential transformation to t
        float expT = (float) Math.pow(t, exponent);
        // Interpolate between x2 and y2 using the exponential factor
        float result = x2 + (y2 - x2) * expT;
        // Clamp the result to ensure it stays within x2 and y2 bounds
        if (x2 < y2) {
            return Math.max(x2, Math.min(result, y2));
        } else {
            return Math.max(y2, Math.min(result, x2));
        }
    }

	public static float clamp(float min, float max, float value) {
		return value < min ? min : Math.min(value, max);
	}

    public static float pow(final double a, final double b) {
        final long tmp = Double.doubleToLongBits(a);
        final long tmp2 = (long)(b * (tmp - 4606921280493453312L)) + 4606921280493453312L;
        return (float) Double.longBitsToDouble(tmp2);


    }

    public static String slashesOf(Map<ShipAPI.HullSize, Float> map) {
        StringBuilder compound = new StringBuilder();

        int i = 0;

        for (Map.Entry<ShipAPI.HullSize, Float> entry : map.entrySet()) {
            if (i == 0) {
                i++;
                continue;
            }

            compound.append(Math.abs(entry.getValue())).append("%").append("/");
        }

        return compound.toString();
    }


    @Deprecated
    public static <T> T decideBasedOnHullSize(ShipAPI shipAPI, T frigate, T destroyer, T cruiser, T cap) {
        switch (shipAPI.getHullSize()) {
            case DESTROYER:
                return destroyer;
            case CRUISER:
                return cruiser;
            case CAPITAL_SHIP:
                return cap;
            default:
                return frigate;
        }
    }

    public static <T> T decideBasedOnHullSize(ShipAPI shipAPI, T small, T frigate, T destroyer, T cruiser, T cap) {
        switch (shipAPI.getHullSize()) {
            case FIGHTER:
                return small;
            case DESTROYER:
                return destroyer;
            case CRUISER:
                return cruiser;
            case CAPITAL_SHIP:
                return cap;
            default:
                return frigate;
        }
    }


    public static void spawnWave (Vector2f loc, Vector2f vel, float size, float intensity, boolean flip, float angle, float arc, float edgeSmooth, float fadeIn, float last, float fadeOut, float growthTime, float shrinkTime){

        WaveDistortion wave = new WaveDistortion(loc, vel);

        wave.setIntensity(intensity);
        wave.setSize(size);
        wave.setArc(angle-arc/2,angle+arc/2);
        if(edgeSmooth!=0){
            wave.setArcAttenuationWidth(edgeSmooth);
        }
        wave.flip(flip);
        if(fadeIn!=0){
            wave.fadeInIntensity(fadeIn);
        }
        wave.setLifetime(last);
        if(fadeOut!=0){
            wave.setAutoFadeIntensityTime(fadeOut);
//            wave.fadeOutIntensity(fadeOut);
        } else {
            wave.setAutoFadeIntensityTime(99);
        }
        if(growthTime!=0){
            wave.fadeInSize(growthTime);
        }
        if(shrinkTime!=0){
            wave.setAutoFadeSizeTime(shrinkTime);
//            wave.fadeOutSize(shrinkTime);
        } else {
            wave.setAutoFadeSizeTime(99);
        }
        DistortionShader.addDistortion(wave);
    }

    public static boolean tooMuchShieldDamageIncoming (ShipAPI ship, float threshhold, float radiusToCheck, float durationToCheck) {

        if (ship.getShield() == null) return false;
        if (ship.getShield().isOff()) return false; //lol

        float totalDamage = 0f;

        for (DamagingProjectileAPI proj : getProjectilesInRange(ship.getLocation(), radiusToCheck)) {
            if (proj.getOwner() == ship.getOwner()) {
                continue;
            }

            //guided missiles may always hit, TODO change this to ignore near misses or use partial derivatives
            if (proj instanceof MissileAPI && ((MissileAPI)proj).isGuided()) {
                MissileAPI guided = (MissileAPI) proj;
                Vector2f testPoint = ship.getLocation();

                // for guided, do some complex math to figure out the time it takes to hit
                float missileTurningRadius = (float) (guided.getMaxSpeed() / (guided.getMaxTurnRate() * Math.PI / 180));
                float missileCurrentAngle = VectorUtils.getFacing(guided.getVelocity());
                Vector2f missileCurrentLocation = guided.getLocation();
                float missileTargetAngle = VectorUtils.getAngle(missileCurrentLocation, testPoint);
                float missileRotationNeeded = MathUtils.getShortestRotation(missileCurrentAngle, missileTargetAngle);
                Vector2f missileRotationCenter = MathUtils.getPointOnCircumference(guided.getLocation(), missileTurningRadius, missileCurrentAngle + (missileRotationNeeded > 0 ? 90 : -90));

                float missileRotationSeconds = 0;
                do {
                    missileRotationSeconds += Math.abs(missileRotationNeeded)/guided.getMaxTurnRate();
                    missileCurrentAngle = missileTargetAngle;
                    missileCurrentLocation = MathUtils.getPointOnCircumference(missileRotationCenter, missileTurningRadius, missileCurrentAngle + (missileRotationNeeded > 0 ? -90 : 90));

                    missileTargetAngle = VectorUtils.getAngle(missileCurrentLocation, testPoint);
                    missileRotationNeeded = MathUtils.getShortestRotation(missileCurrentAngle, missileTargetAngle);
                } while (missileRotationSeconds < durationToCheck && Math.abs(missileRotationNeeded) > 1f);

                float radius = Misc.getTargetingRadius(missileCurrentLocation, ship, false);
                float missileStraightSeconds = (MathUtils.getDistance(missileCurrentLocation, testPoint)-radius) / guided.getMaxSpeed();

                if ((missileRotationSeconds + missileStraightSeconds < durationToCheck) && (missileRotationSeconds + missileStraightSeconds < guided.getMaxFlightTime() - guided.getFlightTime())){
                    totalDamage = totalDamage + ( proj.getDamage().getDamage() * proj.getDamage().getType().getShieldMult() * ship.getShield().getFluxPerPointOfDamage()  );
                }

            } else {
                Vector2f testPoint = ship.getLocation();
                float radius = Misc.getTargetingRadius(proj.getLocation(), ship, false);
                float maxSpeed = (proj instanceof MissileAPI) ? ((MissileAPI) proj).getMaxSpeed() : proj.getMoveSpeed();
                Vector2f futureProjectileLocation = Vector2f.add(proj.getLocation(), VectorUtils.resize(new Vector2f(proj.getVelocity()), durationToCheck*maxSpeed), null);
                float hitDistance = MathUtils.getDistance(testPoint, proj.getLocation()) - radius;
                float travelTime = hitDistance/proj.getMoveSpeed();
                Vector2f futureTestPoint = Vector2f.add(testPoint, (Vector2f) new Vector2f(ship.getVelocity()).scale(travelTime), null);

                boolean collides = CollisionUtils.getCollides(proj.getLocation(), futureProjectileLocation, futureTestPoint, radius);
                if (collides) {
                    totalDamage = totalDamage + ( proj.getDamage().getDamage() * proj.getDamage().getType().getShieldMult() * ship.getShield().getFluxPerPointOfDamage()  );
                }

            }


        }


        //If our total incoming damage is higher than our threshhold, we return true. Otherwise, we return false
        return totalDamage >= ship.getMaxFlux() * threshhold;
    }


    public static float calculateDamageToArmor(DamageAPI damage, float armorRating, float damperCoefficient) {
        float damageAmount = damage.getDamage() * damage.getType().getArmorMult() * damperCoefficient;
        damageAmount = damageAmount * (damageAmount / (damageAmount + armorRating));

        return damageAmount;
    }

    public static List<DamagingProjectileAPI> getProjectilesInRange(Vector2f location, float range)
    {
        List<DamagingProjectileAPI> projectiles = new ArrayList<>();

        for (DamagingProjectileAPI tmp : Global.getCombatEngine().getProjectiles())
        {
            if (MathUtils.isWithinRange(tmp.getLocation(), location, range))
            {
                projectiles.add(tmp);
            }
        }

        //TODO sort by closest

        return projectiles;
    }



    //optimize this shits (vectorize and cache)
    //could do some quadtree stuff lol

    public static float armorDamagePossible(ShipAPI ship, float radiusToCheck, float durationToCheck, float multiplyAllDamageBy) {

        float armorRatingForCalculation = ship.getArmorGrid().getArmorRating();
        float totalDamage = 0f;

        for (DamagingProjectileAPI proj : getProjectilesInRange(ship.getLocation(), radiusToCheck)) {
            if (proj.getOwner() == ship.getOwner()) {
                continue;
            }

            //guided missiles may always hit, TODO change this to ignore near misses or use partial derivatives
            if (proj instanceof MissileAPI && ((MissileAPI)proj).isGuided()) {


                MissileAPI guided = (MissileAPI) proj;
                Vector2f testPoint = ship.getLocation();

                // for guided, do some complex math to figure out the time it takes to hit
                float missileTurningRadius = (float) (guided.getMaxSpeed() / (guided.getMaxTurnRate() * Math.PI / 180));
                float missileCurrentAngle = VectorUtils.getFacing(guided.getVelocity());
                Vector2f missileCurrentLocation = guided.getLocation();
                float missileTargetAngle = VectorUtils.getAngle(missileCurrentLocation, testPoint);
                float missileRotationNeeded = MathUtils.getShortestRotation(missileCurrentAngle, missileTargetAngle);
                Vector2f missileRotationCenter = MathUtils.getPointOnCircumference(guided.getLocation(), missileTurningRadius, missileCurrentAngle + (missileRotationNeeded > 0 ? 90 : -90));

                float missileRotationSeconds = 0;
                do {
                    missileRotationSeconds += Math.abs(missileRotationNeeded)/guided.getMaxTurnRate();
                    missileCurrentAngle = missileTargetAngle;
                    missileCurrentLocation = MathUtils.getPointOnCircumference(missileRotationCenter, missileTurningRadius, missileCurrentAngle + (missileRotationNeeded > 0 ? -90 : 90));

                    missileTargetAngle = VectorUtils.getAngle(missileCurrentLocation, testPoint);
                    missileRotationNeeded = MathUtils.getShortestRotation(missileCurrentAngle, missileTargetAngle);
                } while (missileRotationSeconds < durationToCheck && Math.abs(missileRotationNeeded) > 1f);

                float radius = Misc.getTargetingRadius(missileCurrentLocation, ship, false);
                float missileStraightSeconds = (MathUtils.getDistance(missileCurrentLocation, testPoint)-radius) / guided.getMaxSpeed();

                if ((missileRotationSeconds + missileStraightSeconds < durationToCheck) && (missileRotationSeconds + missileStraightSeconds < guided.getMaxFlightTime() - guided.getFlightTime())){



                    totalDamage = totalDamage + calculateDamageToArmor(proj.getDamage(), armorRatingForCalculation, multiplyAllDamageBy);

                }

                continue;
            }



            Vector2f projDest = Vector2f.add(
                    (Vector2f)(new Vector2f(proj.getVelocity()).scale(durationToCheck)),
                    proj.getLocation(),
                    new Vector2f(0f, 0f)
            );

            Vector2f loc = ship.getLocation();
            float radius = ship.getCollisionRadius();

            if (ship.getShield() != null) {
                loc = ship.getShield().getLocation();
                radius = ship.getCollisionRadius();
            }

            boolean collides = CollisionUtils.getCollides(
                    proj.getLocation(),
                    projDest,
                    loc,
                    radius
            );

            if (!collides) continue;
            totalDamage = totalDamage + calculateDamageToArmor(proj.getDamage(), armorRatingForCalculation, multiplyAllDamageBy);

        }

        //count weapons
        for (Iterator<ShipAPI> it = StopgapUtils.getShipsWithinRange(ship.getLocation(), radiusToCheck); it.hasNext(); ) {
            ShipAPI enemy = it.next();

            if (enemy.getOwner() == ship.getOwner()) continue; //it's not an emey
            if (!enemy.isAlive() || enemy.getFluxTracker().isOverloadedOrVenting() || enemy.getFluxLevel() > 0.9 || enemy.isPiece()) continue;

            for (WeaponAPI weaponAPI : enemy.getAllWeapons()) {
                //we don't care about disabled weapons or out of range weapons
                if (weaponAPI.isDisabled()) continue;
                if (MathUtils.getDistanceSquared(ship.getLocation(), enemy.getLocation()) > weaponAPI.getRange() * weaponAPI.getRange()) continue;

                float trueAngle = VectorUtils.getAngle(enemy.getLocation(), ship.getLocation());
                float desireAngle = weaponAPI.getCurrAngle();

                //ignore guns that cannot hit us
                if (Math.abs(trueAngle-desireAngle) > 7) continue;

                if (weaponAPI.isBeam()) {
                    totalDamage = totalDamage + calculateDamageToArmor(weaponAPI.getDamage(), armorRatingForCalculation, multiplyAllDamageBy * 0.5f);

                    continue;
                }


                Vector2f directional = VectorUtils.getDirectionalVector(
                        weaponAPI.getLocation(),
                        ship.getLocation()
                );

                directional.scale(weaponAPI.getProjectileSpeed());
                directional.scale(durationToCheck);

                Vector2f projDest = Vector2f.add(
                        ship.getLocation(),
                        directional,
                        new Vector2f(0f, 0f)
                );

                boolean collides = CollisionUtils.getCollides(
                        weaponAPI.getLocation(),
                        projDest,
                        ship.getLocation(),
                        ship.getCollisionRadius()
                );

                //If it doesn't collide, ignore it
                if (!collides) {
                    continue;
                }


                //we care about beams

                //we care about insta hits, but weigh them less
                if (weaponAPI.getProjectileSpeed() > 1200) { //TODO and it can hit us right now
                    totalDamage = totalDamage + calculateDamageToArmor(weaponAPI.getDamage(), armorRatingForCalculation, multiplyAllDamageBy * 0.5f);
                }

            }
        }



        return totalDamage;
    }





    //credits starficz





}