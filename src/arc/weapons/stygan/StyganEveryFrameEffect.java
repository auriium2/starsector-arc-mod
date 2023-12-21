package arc.weapons.stygan;

import arc.weapons.ArcChargeupWeaponEffect;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.lazywizard.lazylib.FastTrig;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

//Credit to SCY
public class StyganEveryFrameEffect extends ArcChargeupWeaponEffect {


    private float timer=0, randomization=0;
    private Vector2f muzzle= new Vector2f();
    private boolean runOnce=false, hasFired=false;
    private List<Float> ANGLES = new ArrayList();

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        super.advance(amount, engine, weapon);

        //game paused, no script
        if (engine.isPaused()) {
            return;
        }

        //get the base offsets
        if(!runOnce){
            runOnce=true;
            ANGLES=new ArrayList<>(weapon.getSpec().getTurretAngleOffsets());
            randomization=(float)Math.random()*10;
            if(weapon.getSlot().isHardpoint()){
                muzzle=weapon.getSpec().getHardpointFireOffsets().get(2);
            } else if(weapon.getSlot().isTurret()){
                muzzle=weapon.getSpec().getTurretFireOffsets().get(2);
            }
            float originalRange = weapon.getSpec().getMaxRange();
        }

        //move the offsets while firing
        if(weapon.getChargeLevel()>0){
            timer+=amount;
            for(int i=0; i<ANGLES.size(); i++){
                float offset=(float) FastTrig.cos(5*(timer+i+randomization))*2;
                weapon.getSpec().getTurretAngleOffsets().set(i, ANGLES.get(i)+offset);
                weapon.getSpec().getHardpointAngleOffsets().set(i, ANGLES.get(i)+offset);
                weapon.getSpec().getHiddenAngleOffsets().set(i, ANGLES.get(i)+offset);
            }
        }

        //Eyecandy plus firing sound (since it is fireOnFullCharge)
        if(weapon.getChargeLevel()==1 && !hasFired){
            hasFired=true;
            //weapon glow
            Vector2f LOC = new Vector2f(weapon.getLocation());
            Vector2f.add(LOC, muzzle, LOC);
            Global.getSoundPlayer().playSound("arc_stygan", 1f, 1f, LOC, weapon.getShip().getVelocity());
            engine.addHitParticle(
                    LOC,
                    weapon.getShip().getVelocity(),
                    40,
                    1f,
                    0.3f,
                    new Color(50,100,255,255)
            );
            engine.addHitParticle(
                    LOC,
                    weapon.getShip().getVelocity(),
                    20,
                    1f,
                    0.1f,
                    Color.WHITE
            );
        } else if(hasFired && weapon.getChargeLevel()==0){
            hasFired=false;
        }
    }

}
