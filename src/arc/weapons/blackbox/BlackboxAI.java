//by Tartiflette, Anti-missile missile AI: precise and able to randomly choose a target between nearby enemy missiles.
//feel free to use it, credit is appreciated but not mandatory
//V2 done
package arc.weapons.blackbox;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import data.scripts.util.MagicLensFlare;
import data.scripts.util.MagicRender;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class BlackboxAI implements MissileAIPlugin, GuidedMissileAI {


    static final float DAMPING = 0.05f;
    static final int NUM_PARTICLES = 10;

    static final String HAS_LOCK = "arc_has_locked";

    enum Mode {
        ANTI_FIGHTER,
        ANTI_MUNITION,
        LOITER
    }

    final MissileAPI missile;
    final float MAX_SPEED;

    CombatEntityAPI currentTarget;

    public BlackboxAI(MissileAPI missile, CombatEntityAPI currentTarget) {
        this.missile = missile;
        MAX_SPEED = missile.getMaxSpeed()*1.25f; //slight over lead
        this.currentTarget = currentTarget;
    }



    void colorTheMissile() {
        if (currentTarget instanceof ShipAPI) {
            missile.setJitter(missile, Color.CYAN, 4,4,4);
        }

        if (currentTarget instanceof MissileAPI){
            missile.setJitter(missile, Color.RED, 4,4,4);
        }

        if (currentTarget == null) {
            missile.setJitter(missile, Color.ORANGE, 4,4,4);
        }
    }




    void untargetCurrentTarget() {
        if (currentTarget != null) {
            currentTarget.getCustomData().remove(HAS_LOCK);
        }
    }




    @Override
    public void advance(float amount) {


        if (missile.isFizzling() || missile.isFading() || missile.isExpired()) {
            if (currentTarget != null) {
                currentTarget.setCustomData(HAS_LOCK, null);
            }

            return;
        }

        CombatEngineAPI engine = Global.getCombatEngine();

        if (engine.isPaused()){
            return;
        }

        colorTheMissile();

        if (missile.getVelocity().length() < missile.getMaxSpeed() / 8.0f) {
            missile.giveCommand(ShipCommand.ACCELERATE); //this is a loitering munition, don't send it flying off if we dont have to
        }

        if (!Global.getCombatEngine().isEntityInPlay(currentTarget) || (currentTarget != null && currentTarget.getOwner()==missile.getOwner() ) || (currentTarget != null && currentTarget.isExpired()) || (currentTarget != null && currentTarget instanceof ShipAPI && !((ShipAPI) currentTarget).isPiece())) {

            currentTarget.setCustomData(HAS_LOCK, null);
            currentTarget = null;

        }

        if (currentTarget == null) {
            if (missile.getVelocity().length() > missile.getMaxSpeed() / 8.0f) {
                missile.giveCommand(ShipCommand.DECELERATE); //this is a loitering munition, don't send it flying off if we dont have to
            }
        }

        if (currentTarget == null) return; //stop that

        
        //finding lead point to aim to    
        float dist = MathUtils.getDistanceSquared(missile.getLocation(), currentTarget.getLocation());
        if (dist<2500){

            proximityFuse();

            return;
        }
        Vector2f lead = AIUtils.getBestInterceptPoint(
                missile.getLocation(),
                MAX_SPEED,
                currentTarget.getLocation(),
                currentTarget.getVelocity()
        );
        if (lead == null ) {
            lead = currentTarget.getLocation();
        }
                
        //best velocity vector angle for interception
        float correctAngle = VectorUtils.getAngle(
                        missile.getLocation(), lead
        );

        float correction = MathUtils.getShortestRotation(VectorUtils.getFacing(missile.getVelocity()),correctAngle);
        if(correction>0){
            correction= -11.25f * ( (float)Math.pow(FastTrig.cos(MathUtils.FPI*correction/90)+1, 2) -4 );
        } else {
            correction= 11.25f * ( (float)Math.pow(FastTrig.cos(MathUtils.FPI*correction/90)+1, 2) -4 );
        }
        correctAngle+= correction;        
        
        //turn the missile
        float aimAngle = MathUtils.getShortestRotation(missile.getFacing(), correctAngle);
        if (aimAngle < 0) {
            missile.giveCommand(ShipCommand.TURN_RIGHT);
        } else {
            missile.giveCommand(ShipCommand.TURN_LEFT);
        }
        if (Math.abs(aimAngle)<45){
            missile.giveCommand(ShipCommand.ACCELERATE);
        }
        
        // Damp angular velocity if we're getting close to the target angle
        if (Math.abs(aimAngle) < Math.abs(missile.getAngularVelocity()) * DAMPING) {
            missile.setAngularVelocity(aimAngle / DAMPING);
        }
    }
    
    void proximityFuse(){

        CombatEngineAPI engine = Global.getCombatEngine();

        Color color1 = null;
        Color color2 = null;

        if (currentTarget == null || currentTarget instanceof MissileAPI) {
            color1 = new Color(231, 151, 112, 218);
            color2 = new Color(176, 90, 38,255);
        }

        if (currentTarget instanceof ShipAPI) {
            color1 = new Color(95, 166, 178);
            color2 = new Color(51, 114, 134);
        }

        float radius = 0;
        float coreRadius = 0;

        if (currentTarget == null || currentTarget instanceof MissileAPI) {

            radius = 200;
            coreRadius = 120;
        }
        if (currentTarget instanceof ShipAPI) {
            radius = 50;
            coreRadius = 30;
        }

        double damage = missile.getDamageAmount();

        if (currentTarget instanceof ShipAPI) {
            damage = damage / 2;
        } else {
            damage = damage * 2.54;
        }

        DamagingExplosionSpec boom = new DamagingExplosionSpec(
                0.2f,
                radius,
                coreRadius,
                missile.getDamageAmount(),
                20,
                CollisionClass.PROJECTILE_NO_FF,
                CollisionClass.PROJECTILE_FIGHTER,
                2,
                5,
                5,
                25,
                color1,
                color2
        );


        if (currentTarget instanceof ShipAPI) {
            //TODO custom explosion: small and energy
            boom.setDamageType(DamageType.ENERGY);
        } else {
            //TODO custom explosion: large and fragmentation
            boom.setDamageType(DamageType.FRAGMENTATION);
        }

        boom.setSoundSetId("explosion_flak");

        if (currentTarget instanceof ShipAPI) {
            boom.setUseDetailedExplosion(true);
            boom.setDetailedExplosionRadius(120f);
            boom.setDetailedExplosionFlashRadius(200f);
            boom.setDetailedExplosionFlashDuration(0.2f);

            MagicLensFlare.createSharpFlare(
                    Global.getCombatEngine(),
                    missile.getSource(),
                    currentTarget.getLocation(),
                    15,
                    700,
                    15,
                    color1,
                    color2
            );
        } else {
            boom.setShowGraphic(true);
        }


        engine.spawnDamagingExplosion(boom, missile.getSource(), missile.getLocation());
        
        if(MagicRender.screenCheck(0.1f, missile.getLocation())){
            engine.addHitParticle(
                missile.getLocation(),
                new Vector2f(),
                100,
                1,
                0.25f,
                color2
            );
            for (int i=0; i<NUM_PARTICLES; i++){
                float axis = (float)Math.random()*360;
                float range = (float)Math.random()*100;
                engine.addHitParticle(
                    MathUtils.getPointOnCircumference(missile.getLocation(), range/5, axis),
                    MathUtils.getPointOnCircumference(new Vector2f(), range, axis),
                    2+(float)Math.random()*2,
                    1,
                    1+(float)Math.random(),
                    color1
                );
            }
            engine.applyDamage(
                    missile,
                    missile.getLocation(),
                    missile.getHitpoints() * 2f,
                    DamageType.FRAGMENTATION,
                    0f,
                    false,
                    false,
                    missile
            );
        } else {
            engine.removeEntity(missile);
        }
    }

    @Override
    public CombatEntityAPI getTarget() {
        return currentTarget;
    }

    @Override
    public void setTarget(CombatEntityAPI target) {
        this.currentTarget = target;
    }


}
