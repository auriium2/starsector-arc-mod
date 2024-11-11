package arc.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.DefenseUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;
import java.util.*;

//STARFICZ CREDIT
public class AIUtil {

    public static int orientation(Vector2f p, Vector2f q, Vector2f r) {
        float val = (q.y - p.y) * (r.x - q.x) - (q.x - p.x) * (r.y - q.y);
        if (val == 0)
            return 0; // collinear
        return (val > 0) ? 1 : 2; // clock or counterclockwise
    }

    public static List<ShipAPI> getConvexHull(List<ShipAPI> ships) {
        int n = ships.size();
        if (n < 3)
            return ships;

        List<ShipAPI> hull = new ArrayList<>();

        // Find the leftmost point
        int leftmost = 0;
        for (int i = 1; i < n; i++) {
            if (ships.get(i).getLocation().x < ships.get(leftmost).getLocation().x)
                leftmost = i;
        }

        int p = leftmost, q;
        do {
            hull.add(ships.get(p));
            q = (p + 1) % n;
            for (int i = 0; i < n; i++) {
                if (orientation(ships.get(p).getLocation(), ships.get(i).getLocation(), ships.get(q).getLocation()) == 2)
                    q = i;
            }
            p = q;
        } while (p != leftmost);

        return hull;
    }

    public static boolean checkInBounds(List<ShipAPI> points, Vector2f point){
        int i, j;
        boolean result = false;
        for (i = 0, j = points.size() - 1; i < points.size(); j = i++)
        {
            if ((points.get(i).getLocation().y > point.y) != (points.get(j).getLocation().y > point.y)
                    && (point.x < (points.get(j).getLocation().x - points.get(i).getLocation().x)
                    * (point.y - points.get(i).getLocation().y)
                    / (points.get(j).getLocation().y - points.get(i).getLocation().y) + points.get(i).getLocation().x))
            {
                result = !result;
            }
        }

        return result;
    }

    public static ArrayList<DamagingProjectileAPI> getAllProjectilesInRange(Vector2f loc, float radius) {
        ArrayList<DamagingProjectileAPI> entities = new ArrayList<>();

        Iterator<Object> iterator = Global.getCombatEngine().getAllObjectGrid().getCheckIterator(loc, radius * 2, radius * 2);
        while (iterator.hasNext()) {

            Object next = iterator.next();
            if (!(next instanceof DamagingProjectileAPI)) continue;
            if (MathUtils.getDistanceSquared(loc, ((DamagingProjectileAPI) next).getLocation()) > radius*radius) continue;

            entities.add((DamagingProjectileAPI) next);
        }
        return entities;
    }

    //THANKS STARFICZ
    public static float getFuturePointDamageEstimate(ShipAPI ship, Vector2f testPoint, float secondsToEstimate, boolean includeBeams){
        float MAX_SPEED_OF_PROJECTILE = 2000f;

        Set<DamagingProjectileAPI> nearbyUnguided = new HashSet<>();
        Set<MissileAPI> nearbyGuided = new HashSet<>();

        // Sort all projectiles into guided or unguided
        for (DamagingProjectileAPI threat : getAllProjectilesInRange(testPoint, secondsToEstimate * MAX_SPEED_OF_PROJECTILE)) {
            if (!threat.isFading() && threat.getOwner() != ship.getOwner()) {
                if (threat instanceof MissileAPI){
                    MissileAPI missile = (MissileAPI) threat;
                    if (!missile.isFlare()) {
                        if (!missile.isGuided())
                            nearbyUnguided.add(threat);
                        else
                            nearbyGuided.add(missile);
                    }
                }else{
                    nearbyUnguided.add(threat);
                }
            }
        }

        Set<DamagingProjectileAPI> estimatedHits = new HashSet<>();

        // do a line-circle collision check for unguided
        for (DamagingProjectileAPI unguided : nearbyUnguided){
            float radius = Misc.getTargetingRadius(unguided.getLocation(), ship, false);
            float maxSpeed = (unguided instanceof MissileAPI) ? ((MissileAPI) unguided).getMaxSpeed() : unguided.getMoveSpeed();
            Vector2f futureProjectileLocation = Vector2f.add(unguided.getLocation(), VectorUtils.resize(new Vector2f(unguided.getVelocity()), secondsToEstimate*maxSpeed), null);
            float hitDistance = MathUtils.getDistance(testPoint, unguided.getLocation()) - radius;
            float travelTime = hitDistance/unguided.getMoveSpeed();
            Vector2f futureTestPoint = Vector2f.add(testPoint, (Vector2f) new Vector2f(ship.getVelocity()).scale(travelTime), null);



            if (CollisionUtils.getCollides(unguided.getLocation(), futureProjectileLocation, futureTestPoint, radius)){
                estimatedHits.add(unguided);
            }
        }

        for (MissileAPI guided : nearbyGuided){
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
            } while (missileRotationSeconds < secondsToEstimate && Math.abs(missileRotationNeeded) > 1f);

            float radius = Misc.getTargetingRadius(missileCurrentLocation, ship, false);
            float missileStraightSeconds = (MathUtils.getDistance(missileCurrentLocation, testPoint)-radius) / guided.getMaxSpeed();

            if ((missileRotationSeconds + missileStraightSeconds < secondsToEstimate) && (missileRotationSeconds + missileStraightSeconds < guided.getMaxFlightTime() - guided.getFlightTime())){
                estimatedHits.add(guided);
            }
        }

        float estimatedDamage = 0f;
        // sum up projectile damage
        for(DamagingProjectileAPI hit : estimatedHits){
            Global.getCombatEngine().addSmoothParticle(hit.getLocation(), hit.getVelocity(), 30f, 5f, 0.1f, Color.magenta);
            estimatedDamage += damageAfterArmor(hit.getDamageType(), hit.getDamageAmount(), ship) + hit.getEmpAmount()/4;
        }

        if (!includeBeams)
            return estimatedDamage;

        // Handle beams with line-circle collision checks
        // TODO: check if getBeams() can be replaced with something like getAllProjectilesInRange() that uses the object grid
        List<BeamAPI> nearbyBeams = Global.getCombatEngine().getBeams();
        for (BeamAPI beam : nearbyBeams) {
            if (beam.getSource().getOwner() != ship.getOwner() && CollisionUtils.getCollides(beam.getFrom(), beam.getTo(), testPoint, Misc.getTargetingRadius(beam.getFrom(), ship, false))) {
                float damage;
                float emp = beam.getWeapon().getDerivedStats().getEmpPerSecond();
                if (beam.getWeapon().getDerivedStats().getSustainedDps() < beam.getWeapon().getDerivedStats().getDps()) {
                    damage = beam.getWeapon().getDerivedStats().getBurstDamage() / beam.getWeapon().getDerivedStats().getBurstFireDuration();
                } else {
                    damage = beam.getWeapon().getDerivedStats().getDps();
                }
                estimatedDamage += damageAfterArmor(beam.getWeapon().getDamageType(), damage, ship) + emp/4;
            }
        }

        return estimatedDamage;
    }

    public static float getWeakestTotalArmor(ShipAPI ship){
        if (ship == null || !Global.getCombatEngine().isEntityInPlay(ship)) {
            return 0f;
        }
        ArmorGridAPI armorGrid = ship.getArmorGrid();

        org.lwjgl.util.Point worstPoint = DefenseUtils.getMostDamagedArmorCell(ship);
        if(worstPoint != null){
            float totalArmor = 0;
            for (int x = 0; x < armorGrid.getGrid().length; x++) {
                for (int y = 0; y < armorGrid.getGrid()[x].length; y++) {
                    if(x >= worstPoint.getX()-2 && x <= worstPoint.getX()+2 && y >= worstPoint.getY()-2 && y <= worstPoint.getY()+2){
                        totalArmor += Math.max(armorGrid.getArmorValue(worstPoint.getX(), worstPoint.getY())/2, armorGrid.getMaxArmorInCell() * 0.025f);
                    }
                    if(x >= worstPoint.getX()-1 && x <= worstPoint.getX()+1 && y >= worstPoint.getY()-1 && y <= worstPoint.getY()+1){
                        totalArmor += Math.max(armorGrid.getArmorValue(worstPoint.getX(), worstPoint.getY())/2, armorGrid.getMaxArmorInCell() * 0.025f);
                    }
                }
            }
            return totalArmor;
        } else{
            return armorGrid.getMaxArmorInCell() * 9.5f;
        }
    }

    public static float damageAfterArmor(DamageType damageType, float damage, ShipAPI ship){
        float armorValue = getWeakestTotalArmor(ship);
        float armorDamage;
        switch (damageType) {
            case FRAGMENTATION:
                armorDamage = damage * 0.25f;
                break;
            case KINETIC:
                armorDamage = damage * 0.5f;
                break;
            case HIGH_EXPLOSIVE:
                armorDamage = damage * 2f;
                break;
            default:
                armorDamage = damage;
                break;
        }

        float damageMultiplier = Math.max(armorDamage / (armorValue + armorDamage),  0.15f);

        return (damage * damageMultiplier);
    }

}
