package arc.weapons.glanz;

import arc.weapons.ArcBaseEveryFrameWeaponEffect;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.scripts.util.MagicTargeting;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.HashMap;
import java.util.Map;
//Thanks tart
public class GlanzEveryFrameEffect extends ArcBaseEveryFrameWeaponEffect {

    boolean runOnce=false;
    final Map <Integer,CombatEntityAPI> BEAMS = new HashMap<>();
    float time = 0;
    boolean lastIsFiring = false;



    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        super.advance(amount, engine, weapon);

        if (!runOnce){
            runOnce=true;

            weapon.ensureClonedSpec();
            for(int i=1; i<weapon.getSpec().getHardpointAngleOffsets().size(); i++){
                BEAMS.put(i, null);
            }
        }

        if (engine.isPaused() || weapon.isDisabled()) {
            return;
        }

        time += Math.PI / 48;

        if (lastIsFiring && !weapon.isFiring()) {
            //stopped shooting
            weapon.setRemainingCooldownTo(MathUtils.getRandomNumberInRange(0.3f, 0.7f));

        }

        lastIsFiring= weapon.isFiring();


        if(weapon.isFiring()){
            //get all rearby missiles

            for(int i : BEAMS.keySet()){
                //does the beam has a target
                if(BEAMS.get(i)==null || !engine.isEntityInPlay(BEAMS.get(i))){
                    //find target
                    CombatEntityAPI target = MagicTargeting.randomMissile(
                            weapon.getShip(),
                            MagicTargeting.missilePriority.DAMAGE_PRIORITY,
                            weapon.getLocation(),
                            weapon.getCurrAngle(),
                            90,
                            (int)weapon.getRange(),
                            true
                    );

                    if (target == null) {
                        target = MagicTargeting.pickShipTarget(weapon.getShip(), MagicTargeting.targetSeeking.LOCAL_RANDOM, 1000, (int) weapon.getArc(), 20, 0, 0,0,0);

                        if (target != null) {
                            if (target.isExpired() || !((ShipAPI) target).isAlive() || !Global.getCombatEngine().isInPlay(target) || ((ShipAPI) target).isHulk()) target = null;
                        }
                    }


                    if(target!=null){
                        //found a suitable target
                        BEAMS.put(i, target);
                        //hit that target
                        float angle=VectorUtils.getAngle(weapon.getLocation(), BEAMS.get(i).getLocation());
                        angle = MathUtils.getShortestRotation(weapon.getCurrAngle(), angle);


                        weapon.getSpec().getHardpointAngleOffsets().set(i,angle);
                        weapon.getSpec().getTurretAngleOffsets().set(i,angle);

                        weapon.getSpec().getHardpointFireOffsets().set(i, MathUtils.getPoint(new Vector2f(), 5, angle));
                        weapon.getSpec().getTurretFireOffsets().set(i, MathUtils.getPoint(new Vector2f(), 5, angle));
                    } else {

                        float mod = 0 + ((float)(FastTrig.sin((time + i))));

                        BEAMS.put(i, null);
                        weapon.getSpec().getHardpointAngleOffsets().set(i,mod);
                        weapon.getSpec().getTurretAngleOffsets().set(i,mod);
                        weapon.getSpec().getHardpointFireOffsets().set(i, new Vector2f(5,0));
                        weapon.getSpec().getTurretFireOffsets().set(i, new Vector2f(5,0));
                    }
                } else if(
                        !MathUtils.isWithinRange(BEAMS.get(i).getLocation(),weapon.getLocation(),weapon.getRange())
                                || Math.abs(MathUtils.getShortestRotation(VectorUtils.getAngle(weapon.getLocation(), BEAMS.get(i).getLocation()),weapon.getCurrAngle()))>90
                ){


                    float mod = ((float)(FastTrig.sin(time + i)));
                    BEAMS.put(i, null);
                    weapon.getSpec().getHardpointAngleOffsets().set(i,mod);
                    weapon.getSpec().getTurretAngleOffsets().set(i,mod);
                    weapon.getSpec().getHardpointFireOffsets().set(i, new Vector2f(5,0));
                    weapon.getSpec().getTurretFireOffsets().set(i, new Vector2f(5,0));



                } else {


                    //keep hitting that target
                    float angle=VectorUtils.getAngle(weapon.getLocation(), BEAMS.get(i).getLocation());
                    angle = MathUtils.getShortestRotation(weapon.getCurrAngle(), angle);

                    weapon.getSpec().getHardpointAngleOffsets().set(i,angle);
                    weapon.getSpec().getTurretAngleOffsets().set(i,angle);

                    weapon.getSpec().getHardpointFireOffsets().set(i, MathUtils.getPoint(new Vector2f(), 5, angle));
                    weapon.getSpec().getTurretFireOffsets().set(i, MathUtils.getPoint(new Vector2f(), 5, angle));
                }
            }
        }
    }
}
