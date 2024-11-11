package arc.weapons.blackbox;

import arc.Index;
import arc.StopgapUtils;
import arc.plugin.RunnableQueuePlugin;
import arc.util.ARCUtils;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.WingRole;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.util.MagicRender;
import org.jetbrains.annotations.NotNull;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.CombatUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * TODO this needs a rewrite it sucks and lags the game
 */
public class BlackboxEveryFrameEffect implements EveryFrameWeaponEffectPlugin {


    WeaponAPI weapon;
    ShipAPI ship;
    List<CombatEntityAPI> handledTargetStorage;

    int maximumBurst = 3;
    int range = 0;
    float beep = 0;

    final IntervalUtil rangeIncrease = new IntervalUtil(0.06f, 0.06f);



    boolean runOnce = false;

    //TODO optimize
    public void fire(float amount) {
        beep-=amount;



        for (RankingStruct struct : getTargets(ship, range)) {
            if (handledTargetStorage.contains(struct.entityAPI)) continue;
            handledTargetStorage.add(struct.entityAPI); //TODO if this breaks it neds to go at the start
            maximumBurst--;

            if (maximumBurst <= 0) {
                return;
            }

            //sound
            if(ship.getOwner() == 0 && beep<=0){
                beep=0.075f;
                Global.getSoundPlayer().playSound(
                        "diableavionics_virtuousTarget_beep",
                        MathUtils.getRandomNumberInRange(0.8f, 1.1f),1,
                        ship.getLocation(),
                        ship.getVelocity()
                );
            }

            //send a missile (or a few) after it



            Vector2f firepoint = weapon.getFirePoint(MathUtils.getRandomNumberInRange(0,5));
            float facing = weapon.getCurrAngle();


            for (int a = 0; a < 3; ++a) {
                Vector2f vel = MathUtils.getRandomPointInCone(new Vector2f(), (float) (a * 50), facing - 182f, facing - 180f);
                Vector2f.add(vel, ship.getVelocity(), vel);

                float size = MathUtils.getRandomNumberInRange(10f, 30f);
                float duration = MathUtils.getRandomNumberInRange(0.3f, 1f);

                Global.getCombatEngine().addSmokeParticle(firepoint, vel, size, 30f, duration, Color.lightGray);
            }

            Vector2f vel = MathUtils.getRandomPointInCone(new Vector2f(), (float) (10), facing - 181f, facing - 179f);
            Vector2f direc = VectorUtils.getDirectionalVector(firepoint, vel);

            Global.getSoundPlayer().playSound(
                    "sabot_srm_fire",
                    1,1,
                    firepoint,
                    ship.getVelocity()
            );

            CombatEntityAPI child = Global.getCombatEngine().spawnProjectile(
                    ship,
                    weapon,
                    Index.BLACKBOX_STAGE_ONE,
                    firepoint,
                    90,
                    direc
            );


            Vector2f newVelocity = new Vector2f();
            Vector2f.add(vel, ship.getVelocity(), newVelocity);

            child.getVelocity().set(newVelocity.x, newVelocity.y);
            CombatUtils.applyForce(child, direc, MathUtils.getRandomNumberInRange(2f, 6f));


            MissileAPI childAsMissile = (MissileAPI) child;
            childAsMissile.setMissileAI(new BlackboxStageOneAI(Index.BLACKBOX_STAGE_TWO, childAsMissile, struct.entityAPI, this));


            int delay = MathUtils.getRandomNumberInRange(10,22);
            weapon.setRefireDelay(delay);

            //HUD only part



        }
    }

















    @Override
    public void advance(float amount, CombatEngineAPI combatEngineAPI, WeaponAPI weaponAPI) {
        rangeIncrease.advance(amount);

        if (!runOnce) {
            runOnce = true;

            this.weapon = weaponAPI;
            this.ship = weapon.getShip();
            this.handledTargetStorage = new ArrayList<>(); //TODO
        }


        if(!weaponAPI.getShip().isAlive() || combatEngineAPI.isCombatOver()){
            handledTargetStorage.clear();
        }

        //Weapon is in firing phase
        if (weaponAPI.isFiring() && weaponAPI.getChargeLevel() > 0.8) {
            if (rangeIncrease.intervalElapsed()) {
                range += weapon.getRange() / 8f; //scan bigger and bigger
            }

            RunnableQueuePlugin.queueTask(() -> fire(amount), MathUtils.getRandomNumberInRange(1,4));
        }

        if (!weaponAPI.isFiring() && weaponAPI.getChargeLevel() <= 0.1f) {
            handledTargetStorage.clear();
            maximumBurst = ARCUtils.decideBasedOnHullSize(
                    ship,
                    MathUtils.getRandomNumberInRange(2,3),
                    MathUtils.getRandomNumberInRange(2,4),
                    MathUtils.getRandomNumberInRange(3,4),
                    MathUtils.getRandomNumberInRange(4,5)
            ); //reload virtual ammo counter to max
            range = 0;
        }

    }


    static class RankingStruct implements Comparable<RankingStruct> {
        final CombatEntityAPI entityAPI;
        final float ranking;

        public RankingStruct(CombatEntityAPI entityAPI, float ranking) {
            this.entityAPI = entityAPI;
            this.ranking = ranking;
        }

        @Override
        public int compareTo(@NotNull BlackboxEveryFrameEffect.RankingStruct o) {
            return Float.compare(ranking, o.ranking);
        }
    }
    static List<RankingStruct> getTargets(CombatEntityAPI entity, int range) {
        List<RankingStruct> structs = new ArrayList<>();

        for (Iterator<MissileAPI> it = StopgapUtils.getMissilesWithinRange(entity.getLocation(), range); it.hasNext(); ) {
            MissileAPI msl = it.next();
            if (msl.getOwner() == entity.getOwner()) continue;

            //farther missiles should get more priority

            float damage = msl.getDamageAmount();
            float damageMappedToCost = damage / 4000; // a reaper is worth 1 cost point, increase linearly

            float speed = msl.getMaxSpeed();
            float speedMappedToCost = speed / 600; //a fast missile is worth 1 cost point, increase linearly

            float rangeSquared = MathUtils.getDistanceSquared(msl.getLocation(), entity.getLocation());
            float rangeMappedToCost = rangeSquared / 700 / 700; //penalize closer missiles since anti-missiles work better vs ranged missiles

            float damageTypeCost = 0f;
            if (msl.getDamageType() == DamageType.KINETIC && entity instanceof ShipAPI) {
                //we hate kinetic missiles since they put us at large risk

                damageTypeCost += ((ShipAPI)entity).getFluxLevel();
            }

            //extra 1f for being a missile
            structs.add(new RankingStruct(msl, 1f + damageTypeCost + speedMappedToCost + rangeMappedToCost + damageMappedToCost));

        }

        for (Iterator<ShipAPI> it = StopgapUtils.getShipsWithinRange(entity.getLocation(), range); it.hasNext(); ) {
            ShipAPI shipAPI = it.next();
            if (!shipAPI.isFighter() || !shipAPI.isAlive() || shipAPI.isPiece() || shipAPI.isPhased() || shipAPI.isAlly()) continue;
            if (entity.getOwner() == shipAPI.getOwner()) continue;

            float shipTypeCost = 0f;
            if (shipAPI.getWing() != null && shipAPI.getWing().getRole() == WingRole.BOMBER) {
                shipTypeCost+= 2f; //oh ew
            }

            float rangeSquared = MathUtils.getDistanceSquared(entity.getLocation(), entity.getLocation());
            float rangeMappedToCost = rangeSquared / 1000 / 1000; //penalize closer fighters since anti-missiles work better vs ranged

            float speed = shipAPI.getMaxSpeed();
            float speedMappedToCost = Math.max(2f, 1 / (speed / 150)); //penalize faster fighters to get higher intercept rates, since these aren't proxy fused

            //TODO some other way of evaluating if a ship is a threat
            structs.add(new RankingStruct(shipAPI, shipTypeCost + rangeMappedToCost + speedMappedToCost));
        }


        Collections.sort(structs);

        return structs;


    }

    //TODO caching


    public static void doTargeting(ShipAPI missileOwner, CombatEntityAPI target) {

        if (!Global.getCombatEngine().isUIShowingHUD()) return;

        Color toUse = Index.ALLIED;

        if (missileOwner.getOwner() != target.getOwner()) { //this ship is an enemy!
            toUse = Index.HOSTILE;

            Vector2f playerLoc = Global.getCombatEngine().getPlayerShip().getLocation();
            Vector2f entityLoc = target.getLocation();

            if (MathUtils.getDistanceSquared(playerLoc, entityLoc) > 500 * 500) { //dont alert the player if farther than 500 units
                return;
            }
        }

        if (!checkEntityIsOkToPlayTarget(target)) return; //do not play effects if it is dumb

        MagicRender.objectspace(
                Global.getSettings().getSprite("fx","targeting"),
                target,
                new Vector2f(),
                new Vector2f(),
                new Vector2f(64,64),
                new Vector2f(0,0),
                45,
                0,
                false,
                toUse,
                true,
                0,
                0,
                2, 1, 0.2f,
                0.5f, 1f, 0.5f,
                true,
                CombatEngineLayers.BELOW_INDICATORS_LAYER
        );

        //swirly stolen from diable
        if(MagicRender.screenCheck( 0.2f, target.getLocation() )){
            MagicRender.objectspace(
                    Global.getSettings().getSprite("fx","targeting"),
                    target, //anchor
                    new Vector2f(), //offset
                    new Vector2f(), //velocity
                    new Vector2f(192,192), //size
                    new Vector2f(-256,-256), //growth
                    45, //angle
                    360, //spin
                    false, //parented
                    toUse,
                    true, //additive
                    0, 0, //jitter
                    0, 0, 0, //flicker
                    0.35f, 0.05f, 0.1f, //timing
                    true,
                    CombatEngineLayers.BELOW_INDICATORS_LAYER
            );
        }
    }
    public static boolean checkEntityIsOkToPlayTarget(CombatEntityAPI entityAPI) {
        if (entityAPI instanceof ShipAPI) {
            return true;
        }

        if (entityAPI instanceof MissileAPI) {
            MissileAPI missile = (MissileAPI) entityAPI;

            if (missile.getDamageAmount() * missile.getDamageType().getArmorMult() > 200) {
                return true;
            }

            if (missile.getMaxSpeed() > 300) {
                return true;
            }
        }

        return false;
    }
}
