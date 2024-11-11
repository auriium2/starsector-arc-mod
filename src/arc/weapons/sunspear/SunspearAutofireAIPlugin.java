// 
// Decompiled by Procyon v0.5.36
// 

package arc.weapons.sunspear;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.CollectionUtils;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class SunspearAutofireAIPlugin implements AutofireAIPlugin
{
    private boolean shouldFire;
    private ShipAPI targetShip;
    private WeaponAPI weapon;
    private final IntervalUtil tracker = new IntervalUtil(0.1f, 0.2f);
    
    public SunspearAutofireAIPlugin(WeaponAPI weapon) {
        this.shouldFire = false;
        this.weapon = weapon;
    }
    
    public void advance(float amount) {
        tracker.advance(amount);
        if (!tracker.intervalElapsed()) return;

        shouldFire = false;
        targetShip = null;

        float fluxCostToFire = weapon.getFluxCostToFire();
        ShipAPI source = weapon.getShip();

        if (source.getCurrFlux() + fluxCostToFire >= source.getMaxFlux() * 0.9f) return; //dont shoot when overfluxed
        float range = this.weapon.getRange();

        List<ShipAPI> targets = new ArrayList<>();
        for (ShipAPI target : Global.getCombatEngine().getShips()) {
            if (!target.isFighter() && !target.isHulk()) {
                if (target == source) {
                    continue;
                }
                if (!MathUtils.isWithinRange(source, target, range) || this.weapon.distanceFromArc(target.getLocation()) > 10.0f) {
                    continue;
                }
                targets.add(target);
            }
        }



        Collections.sort(targets, new CollectionUtils.SortEntitiesByDistance(this.weapon.getLocation(), true));
        for (final ShipAPI target : targets) {
            float angleDisToFireLine = MathUtils.getShortestRotation(
                    this.weapon.getCurrAngle(),
                    VectorUtils.getAngle(
                            this.weapon.getLocation(),
                            target.getLocation()
                    )
            );

            angleDisToFireLine = Math.abs(angleDisToFireLine);
            final float distanceToFireLine = (float)(
                    FastTrig.sin(
                            0.0174533 * (double) angleDisToFireLine
                    ) * MathUtils.getDistance(
                            this.weapon.getLocation(),
                            target.getLocation()
                    )
            );


            //avoid friendly fire or shooting things we can't hit
            if (source.getOwner() == target.getOwner() && distanceToFireLine < target.getCollisionRadius() * 1.5f) continue;
            if (distanceToFireLine > target.getCollisionRadius()) continue;

            WeaponAPI.DerivedWeaponStatsAPI derivedStats = this.weapon.getDerivedStats();
            if (target.getShield() != null && target.getShield().isOn() && target.getShield().isWithinArc(this.weapon.getLocation())) {
                //if we can't overflux them don't bother
                if (target.getFluxLevel() < 0.2f) continue;
                if (target.getCurrFlux() + derivedStats.getBurstDamage() < target.getMaxFlux() * 1.1f) continue;
            }
            FluxTrackerAPI fluxTracker = target.getFluxTracker();

            //if they are't venting/overloading and their flux level is below 40%, don't bother shooting
            if (!fluxTracker.isOverloadedOrVenting() && target.getFluxLevel() <= 0.6f) continue;

            //they are venting or overloading, fuck them in the asshole
            float weakTime = 0.0f;
            if (fluxTracker.isOverloaded()) weakTime = fluxTracker.getOverloadTimeRemaining();
            if (fluxTracker.isVenting()) weakTime = fluxTracker.getTimeToVent();
            if (weakTime < 1.5f) continue;

            //found the weak link

            this.shouldFire = true;
            this.targetShip = target;
            break;
        }

    }

    @Override
    public boolean shouldFire() {
        return this.shouldFire;
    }

    @Override
    public void forceOff() {
        //noops
    }

    @Override
    public Vector2f getTarget() {
        return weapon.getShip().getMouseTarget();
    }

    @Override
    public ShipAPI getTargetShip() {
        return targetShip;
    }

    @Override
    public WeaponAPI getWeapon() {
        return weapon;
    }

    @Override
    public MissileAPI getTargetMissile() {
        return null;
    }


}
