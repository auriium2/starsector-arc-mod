package arc.plugin;

import ch.ethz.globis.phtree.PhTreeF;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.FleetMemberDeploymentListener;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.util.vector.Vector2f;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * if it is needed
 *
 * iteration is fast and querying is fast
 * insertion and removal are slow because yes
 */
public class LocalityPlugin implements EveryFrameCombatPlugin, FleetMemberDeploymentListener {

    final static byte X = 0;
    final static byte Y = 1;

    final static IntervalUtil updates = new IntervalUtil(0.1f, 0.2f);

    //SOMEWHAT MUTABLE INTERNAL STATE

    final static PhTreeF<ShipAPI> localityShipMap = PhTreeF.create(2);
    final static PhTreeF.PhQueryF<ShipAPI> cachedQuery = localityShipMap.query(new double[] {0,0}, new double[] {0,0});
    final static Map<ShipAPI, Integer> shipToIndexBackup = new HashMap<>();

    //MUTABLE and DANGEROUS we HATE MUTABLE

    static float[][] lastPositions = new float[0][2];
    static ShipAPI[] trackedShips = new ShipAPI[0];





    @Override
    public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {

    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        updates.advance(amount);
        if (!updates.intervalElapsed()) return;




    }


    void update() {


        //THIS LOOP ONLY DOES UPDATES

        int quantityOfShips = trackedShips.length;

        float[][] currentPositions = new float[quantityOfShips][2];

        //this loop will load
        for (int i = 0; i < quantityOfShips; i++) {
            Vector2f location = trackedShips[i].getLocation();
            currentPositions[i][X] = location.x;
            currentPositions[i][Y] = location.y;
        }

        for (int i = 0; i < quantityOfShips; i++) {
            float[] current = currentPositions[i];
            float[] past = lastPositions[i];

            boolean xChanged = Math.abs(current[X] - past[X]) > 0.5;
            boolean yChanged = Math.abs(current[Y] - past[Y]) > 0.5;

            //update the locality map
            if (xChanged || yChanged) {
                double[] currentAsDouble = new double[] {current[X], current[Y]};
                double[] pastAsDouble = new double[] {past[X], current[X]};

                localityShipMap.update(pastAsDouble, currentAsDouble);
            }
        }

        lastPositions = currentPositions;
    }

    @Override
    public void renderInWorldCoords(ViewportAPI viewport) {

    }

    @Override
    public void renderInUICoords(ViewportAPI viewport) {

    }

    @Override
    public void init(CombatEngineAPI engine) {

    }

    //inserting elements is fine
    @Override
    public void reportFleetMemberDeployed(DeployedFleetMemberAPI member) {
        //welcome to hell

        ShipAPI[] insertedShips = null;
        if (member.isFighterWing()) {
            insertedShips = member.getShip().getWing().getWingMembers().toArray(new ShipAPI[0]);
        } else {
            insertedShips = new ShipAPI[] {member.getShip() };
        }

        int newLength = trackedShips.length + insertedShips.length;

        //these are synchronized and nothing is ever removed from them
        //TODO: memory leak?
        trackedShips = Arrays.copyOf(trackedShips, newLength);
        lastPositions = Arrays.copyOf(lastPositions, newLength);



    }

    /**
     * Only returns active entities because i do not care if the entity is inactive
     * @param forEach
     * @param point
     * @param radius
     */
    public static void forEachNearPoint(Consumer<ShipAPI> forEach, Vector2f point, double radius) {
        double[] min = new double[] { point.x - radius, point.y - radius };
        double[] max = new double[] {point.x + radius, point.y + radius };

        cachedQuery.reset(min, max);
        cachedQuery.forEachRemaining(forEach);
    }

    public static ShipAPI[] getNearPoint(Vector2f point, double radius) {
        double[] min = new double[] { point.x - radius, point.y - radius };
        double[] max = new double[] {point.x + radius, point.y + radius };


        List<PhTreeF.PhEntryF<ShipAPI>> entryList = localityShipMap.queryAll(min, max);
        ShipAPI[] output = new ShipAPI[entryList.size()];

        for (int i = 0; i < entryList.size(); i++) {
            output[i] = entryList.get(i).getValue();
        }

        return output;
    }
}
