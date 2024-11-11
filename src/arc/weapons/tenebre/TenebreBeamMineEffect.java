package arc.weapons.tenebre;

import arc.util.ARCUtils;
import arc.weapons.ArcChargeupWeaponEffect;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.Iterator;
import java.util.List;

import static com.fs.starfarer.api.impl.combat.RiftBeamEffect.TARGET_RANGE;

public class TenebreBeamMineEffect extends ArcChargeupWeaponEffect {

    static final float RIFT_RANGE = 150f;
    final IntervalUtil burstBeamInterval = new IntervalUtil(0.025f, 0.05f);


    float maximumBurstFireTime = 0f;

    public TenebreBeamMineEffect() {
        super(new Color(164, 106, 93,130), 4f, true);
    }

    public void advance(final float n, final CombatEngineAPI combatEngineAPI, final WeaponAPI weaponAPI) {
        super.advance(n, combatEngineAPI, weaponAPI);


        final List<BeamAPI> beams = weaponAPI.getBeams();
        if (beams.isEmpty()) {
            return;
        }
        final BeamAPI beamAPI = beams.get(0);

        if (beamAPI.getWeapon().getBurstFireTimeRemaining() > maximumBurstFireTime) {
            maximumBurstFireTime = beamAPI.getWeapon().getBurstFireTimeRemaining();
        }


        if (beamAPI.getBrightness() < 1) return;

        burstBeamInterval.advance(n * 2.0f);
        if (burstBeamInterval.intervalElapsed()) {
            if (beamAPI.getLengthPrevFrame() < 10.0f) {
                return;
            }
            final CombatEntityAPI target = this.findTarget(beamAPI, beamAPI.getWeapon(), combatEngineAPI);
            Vector2f toDamagePoint;
            if (target == null || Math.random() < 0.25) {
                toDamagePoint = this.pickNoTargetDest(beamAPI, beamAPI.getWeapon(), combatEngineAPI);
            }
            else {
                toDamagePoint = target.getLocation();
            }

            Vector2f from = beamAPI.getFrom();
            Vector2f toOnLine = Misc.closestPointOnSegmentToPoint(beamAPI.getFrom(), beamAPI.getRayEndPrevFrame(), toDamagePoint);
            Vector2f directional = Misc.getUnitVectorAtDegreeAngle(Misc.getAngleInDegrees(from, toOnLine));

            float progress = (beamAPI.getWeapon().getBurstFireTimeRemaining() - beamAPI.getWeapon().getSpec().getBeamChargeupTime()) / beamAPI.getWeapon().getSpec().getBurstDuration();


            //Global.getCombatEngine().addFloatingText(weaponAPI.getLocation(), progress + ": p", 10f, Color.RED, weaponAPI.getShip(), 1f, 10f);


            progress = Math.abs(progress);
            if (progress < 0) {
                progress = 0;
            }


            directional.scale(progress * beamAPI.getWeapon().getRange());

            Vector2f solution = new Vector2f();

            Vector2f.add(from, directional, solution);
            Vector2f.add(
                    solution,
                    new Vector2f(MathUtils.getRandomNumberInRange(-100, 100), MathUtils.getRandomNumberInRange(-100, 100)),
                    solution
            );
            ARCUtils.spawnMine(beamAPI.getSource(), solution, "arc_tenebre_minelayer");
        }
    }

    public Vector2f pickNoTargetDest(final BeamAPI beamAPI, final WeaponAPI weaponAPI, final CombatEngineAPI combatEngineAPI) {
        Vector2f from = beamAPI.getFrom();
        Vector2f rayEndPrevFrame = beamAPI.getRayEndPrevFrame();
        float lengthPrevFrame = beamAPI.getLengthPrevFrame();

        float progress = lengthPrevFrame / beamAPI.getLength();


        //get a vec a little ways away from the pt
        float n = 0.25f + (float)Math.random() * 0.75f;
        Vector2f unitVectorAtDegreeAngle = Misc.getUnitVectorAtDegreeAngle(Misc.getAngleInDegrees(from, rayEndPrevFrame));
        unitVectorAtDegreeAngle.scale(lengthPrevFrame * n);
        Vector2f.add(from, unitVectorAtDegreeAngle, unitVectorAtDegreeAngle);

        return Misc.getPointWithinRadius(unitVectorAtDegreeAngle, RIFT_RANGE);
    }

    public CombatEntityAPI findTarget(final BeamAPI beamAPI, final WeaponAPI weaponAPI, final CombatEngineAPI combatEngineAPI) {
        final Vector2f rayEndPrevFrame = beamAPI.getRayEndPrevFrame();
        final Iterator<Object> checkIterator = Global.getCombatEngine().getAllObjectGrid().getCheckIterator(rayEndPrevFrame, RIFT_RANGE * 2.0f, RIFT_RANGE * 2.0f);
        final int owner = weaponAPI.getShip().getOwner();
        final WeightedRandomPicker<CombatEntityAPI> weightedRandomPicker = new WeightedRandomPicker<>();
        while (checkIterator.hasNext()) {
            final CombatEntityAPI next = (CombatEntityAPI) checkIterator.next();
            if (!(next instanceof MissileAPI) && !(next instanceof ShipAPI)) {
                continue;
            }
            if (next.getOwner() == owner) {
                continue;
            }
            if (next instanceof ShipAPI) {
                final ShipAPI shipAPI = (ShipAPI) next;
                if (!shipAPI.isFighter() && !shipAPI.isDrone()) {
                    continue;
                }
            }
            if (Misc.getDistance(Misc.closestPointOnSegmentToPoint(beamAPI.getFrom(), beamAPI.getRayEndPrevFrame(), next.getLocation()), next.getLocation()) - Misc.getTargetingRadius(rayEndPrevFrame, next, false) > TARGET_RANGE) {
                continue;
            }
            weightedRandomPicker.add(next);
        }
        return weightedRandomPicker.pick();
    }


}
