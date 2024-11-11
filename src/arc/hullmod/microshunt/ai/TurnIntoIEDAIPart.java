package arc.hullmod.microshunt.ai;

import arc.Index;
import arc.hullmod.ARCData;
import arc.hullmod.IHullmodPart;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;


public class TurnIntoIEDAIPart implements IHullmodPart<ARCData> {

    static float VELOCITY_DAMPING_FACTOR = 1f;


    private static ShipAPI findBestTarget(ShipAPI ship) {
        ShipAPI largest = null;
        float size, largestSize = 0f;
        List<ShipAPI> enemies = AIUtils.getEnemiesOnMap(ship);
        for (ShipAPI tmp : enemies) {
            if (tmp.getOwner() == ship.getOwner() || tmp.isHulk() || tmp.isShuttlePod() || tmp.isFighter()
                    || tmp.isDrone()) {
                continue;
            }
            size = tmp.getCollisionRadius();
            if (size > largestSize) {
                largest = tmp;
                largestSize = size;
            }
        }
        return largest;
    }

    private static Vector2f quad(float a, float b, float c) {
        Vector2f solution = null;
        if (Float.compare(Math.abs(a), 0) == 0) {
            if (Float.compare(Math.abs(b), 0) == 0) {
                solution = (Float.compare(Math.abs(c), 0) == 0) ? new Vector2f(0, 0) : null;
            } else {
                solution = new Vector2f(-c / b, -c / b);
            }
        } else {
            float d = b * b - 4 * a * c;
            if (d >= 0) {
                d = (float) Math.sqrt(d);
                float e = 2 * a;
                solution = new Vector2f((-b - d) / e, (-b + d) / e);
            }
        }
        return solution;
    }

    private static Vector2f intercept(Vector2f point, float speed, float acceleration, float maxspeed, Vector2f target,
                                      Vector2f targetVel) {
        Vector2f difference = new Vector2f(target.x - point.x, target.y - point.y);

        float s = speed;
        float a = acceleration / 2f;
        float b = speed;
        float c = difference.length();
        Vector2f solutionSet = quad(a, b, c);
        if (solutionSet != null) {
            float t = Math.min(solutionSet.x, solutionSet.y);
            if (t < 0) {
                t = Math.max(solutionSet.x, solutionSet.y);
            }
            if (t > 0) {
                s = acceleration * t;
                s = s / 2f + speed;
                s = Math.min(s, maxspeed);
            }
        }

        //determinant????
        a = targetVel.x * targetVel.x + targetVel.y * targetVel.y - s * s;
        b = 2 * (targetVel.x * difference.x + targetVel.y * difference.y);
        c = difference.x * difference.x + difference.y * difference.y;

        solutionSet = quad(a, b, c);

        Vector2f intercept = null;
        if (solutionSet != null) {
            float bestFit = Math.min(solutionSet.x, solutionSet.y);
            if (bestFit < 0) {
                bestFit = Math.max(solutionSet.x, solutionSet.y);
            }
            if (bestFit > 0) {
                intercept = new Vector2f(target.x + targetVel.x * bestFit, target.y + targetVel.y * bestFit);
            }
        }

        return intercept;
    }



    @Override
    public void advanceSafely(CombatEngineAPI engineAPI, ShipAPI shipAPI, float timestep, ARCData customData) {

        if (shipAPI.isFighter()) return;

        float suifactor = 0f;

        float hullLevel = shipAPI.getHullLevel();
        float fluxLevel = shipAPI.getFluxLevel();

        //if fluxLevel is high and hullLevel is low

        float desireToCommitSuicide = (float) (suifactor + (fluxLevel + (1/Math.max(Math.pow(hullLevel, 2), 0.05))));


        if (desireToCommitSuicide > 3 + Math.random()) {
            customData.shouldTryToIED = true;
        }

        if (customData.currentTarget == null) {
            customData.currentTarget = findBestTarget(shipAPI);
        }

        if ((float) Math.random() >= 0.95f) {
            customData.currentTarget = findBestTarget(shipAPI);
        }

        if (customData.currentTarget == null || (customData.currentTarget instanceof ShipAPI && ((ShipAPI)customData.currentTarget).isHulk()) || (shipAPI.getOwner() == customData.currentTarget.getOwner())
                || !Global.getCombatEngine().isEntityInPlay(customData.currentTarget)) {
            customData.currentTarget = findBestTarget(shipAPI);
            return;
        }

        Vector2f targetLocation = customData.currentTarget.getLocation();
        Vector2f targetVelocity = customData.currentTarget.getVelocity();



        if (customData.shouldTryToIED) {
            shipAPI.getEngineController().getShipEngines().forEach(ShipEngineControllerAPI.ShipEngineAPI::repair);

            shipAPI.setJitter(shipAPI, Color.PINK, 4 - customData.timerTicksBeforeJihad, 5, 3 - customData.timerTicksBeforeJihad);

            //ai
            shipAPI.giveCommand(ShipCommand.ACCELERATE, null, 0);
            shipAPI.blockCommandForOneFrame(ShipCommand.DECELERATE);
            shipAPI.blockCommandForOneFrame(ShipCommand.ACCELERATE_BACKWARDS);

            shipAPI.setDefenseDisabled(true);
            //shipAPI.getTravelDrive().forceState(ShipSystemAPI.SystemState.ACTIVE, 1);

            //ied
            float distance = MathUtils.getDistance(shipAPI.getLocation(), targetLocation);
            float acceleration = shipAPI.getMutableStats().getAcceleration().getModifiedValue();
            float maxSpeed = shipAPI.getMutableStats().getMaxSpeed().getModifiedValue();
            Vector2f guidedTarget = intercept(
                    shipAPI.getLocation(),
                    shipAPI.getVelocity().length(),
                    acceleration,
                    maxSpeed,
                    targetLocation,
                    targetVelocity
            );

            if (guidedTarget == null) {
                Vector2f projection = new Vector2f(targetVelocity);
                float scalar = distance / 50f;
                projection.scale(scalar);
                guidedTarget = Vector2f.add(targetLocation, projection, null);
            }

            float velocityFacing = VectorUtils.getFacing(shipAPI.getVelocity());
            float absoluteDistance = MathUtils.getShortestRotation(velocityFacing, VectorUtils.getAngleStrict(shipAPI.getLocation(),
                    guidedTarget));
            float angularDistance = MathUtils.getShortestRotation(shipAPI.getFacing(), VectorUtils.getAngleStrict(shipAPI.getLocation(),
                    guidedTarget));
            float compensationDifference = MathUtils.getShortestRotation(angularDistance, absoluteDistance);
            if (Math.abs(compensationDifference) <= 75f) {
                angularDistance += 0.5f * compensationDifference;
            }
            float absAngularDistance = Math.abs(angularDistance);




            float turnFlipChance = 0f;
            if (Math.abs(angularDistance) < (Math.abs(shipAPI.getAngularVelocity()) * VELOCITY_DAMPING_FACTOR)) {
                turnFlipChance = 1f - (0.5f * (Math.abs(angularDistance) / VELOCITY_DAMPING_FACTOR));
            }

            if (Math.abs(angularDistance) < Math.abs(shipAPI.getAngularVelocity()) * VELOCITY_DAMPING_FACTOR) {
                shipAPI.setAngularVelocity(angularDistance / VELOCITY_DAMPING_FACTOR);
            }

            if (absAngularDistance > 3f && !shipAPI.getTravelDrive().isOn()) {
                if (Math.random() < turnFlipChance) {
                    shipAPI.giveCommand(angularDistance > 0f ? ShipCommand.TURN_RIGHT : ShipCommand.TURN_LEFT, null, 0);
                } else {
                    shipAPI.giveCommand(angularDistance > 0f ? ShipCommand.TURN_LEFT : ShipCommand.TURN_RIGHT, null, 0);
                }


            }

            if (absAngularDistance < 3f) {
                shipAPI.getTravelDrive().forceState(ShipSystemAPI.SystemState.ACTIVE, 1f);
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
