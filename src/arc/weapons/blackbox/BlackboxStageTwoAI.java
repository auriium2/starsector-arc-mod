package arc.weapons.blackbox;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class BlackboxStageTwoAI implements MissileAIPlugin, GuidedMissileAI {

    static final float DAMPING = 0.05f;

    final MissileAPI missile;
    final BlackboxEveryFrameEffect parent;
    CombatEntityAPI currentTarget;


    public BlackboxStageTwoAI(MissileAPI missile, BlackboxEveryFrameEffect parent, CombatEntityAPI currentTarget) {
        this.missile = missile;
        this.parent = parent;
        this.currentTarget = currentTarget;
    }

    //TODO this is probably laggy and bad
    CombatEntityAPI getNextBest() {
        for (BlackboxEveryFrameEffect.RankingStruct struct : BlackboxEveryFrameEffect.getTargets(parent.ship, 2000)) {
            if (parent.handledTargetStorage.contains(struct.entityAPI)) continue;
            parent.handledTargetStorage.add(struct.entityAPI);

            return struct.entityAPI;
        }


        return null;

    }

    @Override
    public void advance(float v) {

        if (missile.isFizzling() || missile.isFading() || missile.isExpired() ) {
            parent.handledTargetStorage.remove(currentTarget);

            return; //i am ballin
            //i am faded
        }

        if (currentTarget == null ||currentTarget.isExpired() || !Global.getCombatEngine().isInPlay(currentTarget) || (currentTarget instanceof ShipAPI && !((ShipAPI) currentTarget).isAlive())) {
            currentTarget = getNextBest();

            if (currentTarget != null) {
                BlackboxEveryFrameEffect.doTargeting(parent.ship, currentTarget);
            }

            return;
        }




        Vector2f lead = AIUtils.getBestInterceptPoint(
                missile.getLocation(),
                missile.getMaxSpeed() * 1.25f,
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

        //exploding

        float dist = MathUtils.getDistanceSquared(missile.getLocation(), currentTarget.getLocation());



        if (dist < (50 * 50)){

            parent.handledTargetStorage.remove(currentTarget);

            if (currentTarget instanceof ShipAPI) {
                shapedCharge();
            } else {
                proximityFuse();
            }

            if (missile == null) return; //?????
            if (Global.getCombatEngine().isInPlay(missile)) {
                Global.getCombatEngine().removeEntity(missile);
            }






            return;
        }

    }

    static final Color color1 = new Color(231, 151, 112, 218);
    static final Color color2 = new Color(176, 90, 38,255);
    static final float radius = 120;
    static final float coreRadius = 60;

    //stolen from diable
    static Vector2f penetration(CombatEntityAPI target, Vector2f point, float direction){
        //check if the point is within the ship
        if(CollisionUtils.isPointWithinBounds(point, target) && target instanceof ShipAPI){
            ShipAPI ship = (ShipAPI)target;
            //find the state of the armor at the impact point
            if(ship.getArmorGrid().getCellAtLocation(point)!=null){
                List<Integer> armorCell = new ArrayList<>();
                for(int i : ship.getArmorGrid().getCellAtLocation(point)){
                    armorCell.add(i);
                }
                float armorAmount = ship.getArmorGrid().getArmorValue(armorCell.get(0),armorCell.get(1));


                //        //debug
                //        Global.getCombatEngine().addFloatingText(point, ""+armorAmount, 15, Color.BLUE, ship, 0.1f, 0.1f);


                //find the level of protection it offers
                float penAmount = 1 - Math.min(1, armorAmount/ 100);
                //apply modifiers
                penAmount*=ship.getMutableStats().getArmorDamageTakenMult().getModifiedValue();
                //find the actual penetration distance
                float penDepth = Math.max(3, penAmount*20);
                //return the actual hole
                return MathUtils.getPoint(point, penDepth, direction);
            } else {
                //if the point is outside the armor grid, just return the max distance
                return MathUtils.getPoint(point, 20, direction);
            }
        } else {
            //if the point is outside the bounds, just return the max distance
            return MathUtils.getPoint(point, 20, direction);
        }
    }


    void shapedCharge() {
        DamagingExplosionSpec boom = new DamagingExplosionSpec(
                0.2f,
                20f,
                5f,
                0,
                0,
                CollisionClass.PROJECTILE_NO_FF,
                CollisionClass.PROJECTILE_FIGHTER,
                2,
                3,
                3,
                10,
                color1,
                color2
        );

        boom.setDamageType(DamageType.HIGH_EXPLOSIVE);

        boom.setSoundSetId("prox_charge_explosion");
        boom.setUseDetailedExplosion(false);

        Global.getCombatEngine().spawnDamagingExplosion(boom, missile.getSource(), missile.getLocation());

        Vector2f wound = currentTarget.getLocation();

        //recursive damage spots
        for(int i=1; i<=5; i++){

//                    wound = penetration(ship,wound,projectile.getFacing());
            wound = penetration(currentTarget,wound,currentTarget.getFacing());

            Global.getCombatEngine().applyDamage(
                    currentTarget,
                    wound,
                    missile.getBaseDamageAmount(),
                    DamageType.HIGH_EXPLOSIVE,
                    missile.getBaseDamageAmount(),
                    false,
                    true,
                    missile.getSource()
            );

            //debug
//                    engine.addFloatingText(wound, projectile.getBaseDamageAmount()/5+"",10, Color.green, target, 0.1f,0.1f);
        }

        //visual fluff
        //use the end wound to find a penetration vector
        Vector2f.sub(wound, currentTarget.getLocation(), wound);

        for(int i=1;i<=14;i++){
            float mult = (float)i/25;
            Vector2f pos = new Vector2f();
            Vector2f.add(currentTarget.getLocation(), (Vector2f)(new Vector2f(wound)).scale(mult), pos);
            Vector2f vel = MathUtils.getRandomPointInCone(currentTarget.getVelocity(), MathUtils.getRandomNumberInRange(10-5*mult,30-20*mult), missile.getFacing()-5, missile.getFacing()+5);

            //smoke
            float grey = MathUtils.getRandomNumberInRange(0.1f+mult/4, 0.2f+mult/2);
            Global.getCombatEngine().addNebulaParticle(
                    pos, (Vector2f)(new Vector2f(vel)).scale(2),
                    MathUtils.getRandomNumberInRange(35-mult*25, 45-mult*30),
                    MathUtils.getRandomNumberInRange(1+mult*2, 1+mult*5),
                    mult/2,
                    0.1f,
                    MathUtils.getRandomNumberInRange(0.5f+mult, 2f+mult),
                    new Color(
                            grey,
                            grey,
                            grey,
                            MathUtils.getRandomNumberInRange(0.2f, 0.5f)
                    ),
                    false
            );

            //flames
            Global.getCombatEngine().addSwirlyNebulaParticle(
                    pos, vel,
                    MathUtils.getRandomNumberInRange(30-mult*25, 40-mult*30),
                    MathUtils.getRandomNumberInRange(1+mult, 1+mult*2),
                    0.05f,
                    0.05f,
                    MathUtils.getRandomNumberInRange(0.05f+(mult/4), 0.1f+(mult/2)),
                    new Color(
                            1f,
                            MathUtils.getRandomNumberInRange(0.75f-mult*0.75f, 1f-mult*0.75f),
                            0,
                            1f),
                    true
            );
            Global.getCombatEngine().addHitParticle(
                    pos,
                    vel,
                    MathUtils.getRandomNumberInRange(35-mult*25, 45-mult*30),
                    1,
                    MathUtils.getRandomNumberInRange(0.05f+mult/20, 0.05f+mult/10),
                    Color.white
            );



        }

    }
    void proximityFuse() {

            float damage = missile.getDamageAmount();

            DamagingExplosionSpec boom = new DamagingExplosionSpec(
                    0.2f,
                    radius / 2,
                    coreRadius / 2,
                    damage,
                    damage,
                    CollisionClass.PROJECTILE_NO_FF,
                    CollisionClass.PROJECTILE_FIGHTER,
                    2,
                    5,
                    5,
                    25,
                    color1,
                    color2
            );

            boom.setDamageType(DamageType.FRAGMENTATION);

            boom.setSoundSetId("explosion_flak");
            boom.setUseDetailedExplosion(false);

            Global.getCombatEngine().spawnDamagingExplosion(boom, missile.getSource(), missile.getLocation());
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
