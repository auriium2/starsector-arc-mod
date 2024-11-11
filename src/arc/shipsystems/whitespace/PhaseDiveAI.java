//By Nicke535, edited from the vanilla shipsystem AI by Alex Mosolov
//Attempts to give the Phase Dive some decent AI
package arc.shipsystems.whitespace;

import arc.StopgapUtils;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.CollisionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.Iterator;

public class PhaseDiveAI implements ShipSystemAIScript {

	ShipAPI ship;
	ShipwideAIFlags flags;
	ShipSystemAPI system;
	
	final IntervalUtil tracker = new IntervalUtil(0.06f, 0.10f);
	
	public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
		this.ship = ship;
		this.flags = flags;
		this.system = system;
	}
	
	@SuppressWarnings("unchecked")
	public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
		//Only run after our tracker has passed

		tracker.advance(amount);
		if (tracker.intervalElapsed()) {

			if (ship.getFluxTracker().isOverloadedOrVenting()) return;

			for (WeaponAPI weaponAPI : ship.getAllWeapons()) {
				//shitty hack to stop it from phasing while using the burst gun
				if (weaponAPI.getSpec().getTags().contains("hyper") && weaponAPI.isFiring()) {
					return;
				}



				if (weaponAPI.isBurstBeam() && weaponAPI.isFiring()) return;

				//TODO stop phasing while shooting needlers, etc

			}

			//Shorthand declaration of things we check often
			FluxTrackerAPI ftrack = ship.getFluxTracker();
			float fluxLevel = ftrack.getCurrFlux() / ftrack.getMaxFlux();
			float hFluxLevel = ftrack.getHardFlux() / ftrack.getMaxFlux();
			int charges = system.getAmmo();
			int maxCharges = system.getMaxAmmo();

			//-----START OF CONFIG-----
			//If there's no enemies within 3000 SU, we use up to 50% charges to expidite travel
			if (charges > maxCharges*0.5f &&
					!enemiesInRange(3000f)) {
				useSystem(charges);

				return;
			}

			
			//If we're not above 70% hardflux, 90% flux, and performing a direct retreat, activate to get the heck away!
			if (ship.isDirectRetreat() &&
					hFluxLevel < 0.7f &&
					fluxLevel < 0.9f) {
				useSystem(charges);

				return;
			}
			
			//If there's a dangerous amount of shield damage incoming (20%, and within 500 SU, looking 1 second into the future),
			// we're below 60% flux, and we're below 50% hardflux, use our system
			if (hFluxLevel < 0.5f &&
					fluxLevel < 0.6f &&
					tooMuchShieldDamageIncoming(0.20f, 500f, 1f)) {
				useSystem(charges);

				return;
			}

//                      Depricated condition
                        
//			//If we're pursuing a target, and they're within 10% of max weapon range, use up to 50% charges to chase
//                        //This Condition is essentially disabled
//			if (charges > maxCharges*0.5f &&
//					flags.hasFlag(ShipwideAIFlags.AIFlags.PURSUING) &&
//					ship.getShipTarget() != null &&
//					MathUtils.getDistance(ship.getLocation(), ship.getShipTarget().getLocation()) <= getLongestWeaponRange(ship)*0.1f) {
//				useSystem(charges);
//			}

			
			//If we're pursuing a target, we're below 70% flux, we have below 50% hardflux, they're outside 120% of max weapon range, and we're
			// too slow to chase effectively (above 95% of our speed), use the system
			if (!flags.hasFlag(ShipwideAIFlags.AIFlags.DO_NOT_PURSUE) &&
					hFluxLevel < 0.5f &&
					ship.getShipTarget() != null &&
					MathUtils.getDistance(ship.getLocation(), ship.getShipTarget().getLocation()) > getShortestOffensiveWeaponRange(ship) * 1.2f &&
					ship.getShipTarget().getMaxSpeed() * 0.9 > ship.getMaxSpeed()
			) {
				useSystem(charges);

				return;

			}

			//If we're backing off currently, and have enemies with weapons that could shoot us, we want to use the system no matter what
			if (flags.hasFlag(ShipwideAIFlags.AIFlags.BACKING_OFF) &&
					enemiesWithWeaponsInRange(3000f)) {
				useSystem(charges);

				return;
			}

			//If we're at above 70% flux, our target is above 80% flux, but we're at below 30% hardflux, use the
			// system to give us a short venting respite
			if (fluxLevel > 0.70f &&
						hFluxLevel < 0.3f &&
						ship.getShipTarget() != null &&
						ship.getShipTarget().getFluxLevel() > 0.8f) {
				useSystem(charges);

				return;
			}


			//-----END OF CONFIG-----
		}
	}


	//Shorthand for using the ship system
	private void useSystem (int currentCharges) {
		if (currentCharges > 0 && system.getState() == ShipSystemAPI.SystemState.IDLE) {
			ship.useSystem();
		}
	}


	//Shorthand to check for enemies within a certain range
	private boolean enemiesInRange (float range) {
		for (ShipAPI potEnemy : AIUtils.getNearbyEnemies(ship, range)) {
			if (potEnemy.getHullSize().equals(ShipAPI.HullSize.FIGHTER)) {
				continue;
			}

			if (MathUtils.getDistance(potEnemy, ship) <= range) {
				return true;
			}
		}
		return false;
	}


	//Checks if incoming damage against shields is too high
	private boolean tooMuchShieldDamageIncoming (float threshhold, float radiusToCheck, float durationToCheck) {
		//If we don't have a shield, always return false
		if (ship.getShield() == null || ship.getShield().getType().equals(ShieldAPI.ShieldType.NONE)) {
			return false;
		}

		//First check all "dumb" projectiles...
		float totalDamage = 0f;
		for (DamagingProjectileAPI proj : StopgapUtils.getProjectilesWithinRange(ship.getLocation(),radiusToCheck)) {
			//Ignore friendlies
			if (proj.getOwner() == ship.getOwner()) {
				continue;
			}

			//Calculate the velocity vector of the projectile some time in the future, and check if this projectile would feasonably collide with us at that point
			Vector2f projDest = Vector2f.add((Vector2f)(new Vector2f(proj.getVelocity()).scale(durationToCheck)), proj.getLocation(), new Vector2f(0f, 0f));
			if (!CollisionUtils.getCollides(proj.getLocation(), projDest, ship.getShield().getLocation(), ship.getShield().getRadius())) {
				//If it doesn't collide, ignore it
				continue;
			}

			//If it's passed all checks, we add its damage (with consideration for damage type) to our counter
			totalDamage += proj.getDamageAmount() * proj.getDamageType().getShieldMult();
		}

		//...then check missiles
		for (Iterator<MissileAPI> it = StopgapUtils.getMissilesWithinRange(ship.getLocation(), radiusToCheck); it.hasNext(); ) {
			MissileAPI msl = it.next();
			//Ignore friendlies
			if (msl.getOwner() == ship.getOwner()) {
				continue;
			}

			//Non-guided missiles use the same threat calculation vector as projectiles, while guided ones are assumed to
			//always be capable of hitting
			if (!msl.isGuided()) {
				Vector2f projDest = Vector2f.add((Vector2f)(new Vector2f(msl.getVelocity()).scale(durationToCheck)), msl.getLocation(), new Vector2f(0f, 0f));
				if (CollisionUtils.getCollides(msl.getLocation(), projDest, ship.getShield().getLocation(), ship.getShield().getRadius())) {
					//If it doesn't collide, ignore it
					continue;
				}
			}

			//If it's passed all checks, we add its damage (with consideration for damage type) to our counter
			totalDamage += msl.getDamageAmount() * msl.getDamageType().getShieldMult();
		}

		//If our total incoming damage is higher than our threshhold, we return true. Otherwise, we return false
		return totalDamage >= (ship.getMaxFlux() / ship.getShield().getFluxPerPointOfDamage()) * threshhold;
	}


	//Shorthand for getting the shortest-ranged non-PD, non-missile weapon range on a ship
	private float getShortestOffensiveWeaponRange (ShipAPI source) {
		//Go through all weapons, discard missiles, and return the highest range
		float minRange = 0f;
		for (WeaponAPI wep : source.getAllWeapons()) {
			if (wep.getType() == WeaponAPI.WeaponType.MISSILE || wep.getSpec().getAIHints().contains(WeaponAPI.AIHints.PD)) {
				continue;
			}

			if (minRange > wep.getRange()) {
				minRange = wep.getRange();
			}
		}
		return minRange;
	}


	//Shorthand for getting the longest weapon range on the ship
	private float getLongestWeaponRange (ShipAPI source) {
		//Go through all weapons, discard missiles, and return the highest range
		float maxRange = 0f;
		for (WeaponAPI wep : source.getAllWeapons()) {
			if (wep.getType() == WeaponAPI.WeaponType.MISSILE) {
				continue;
			}

			if (maxRange < wep.getRange()) {
				maxRange = wep.getRange();
			}
		}
		return maxRange;
	}


	//Shorthand to check for enemies within a certain range *who are within range of their own weapons*
	private boolean enemiesWithWeaponsInRange (float range) {
		for (ShipAPI potEnemy : AIUtils.getNearbyEnemies(ship, range)) {
			if (potEnemy.getHullSize().equals(ShipAPI.HullSize.FIGHTER)) {
				continue;
			}

			if (MathUtils.getDistance(potEnemy, ship) <= getLongestWeaponRange(potEnemy)) {
				return true;
			}
		}
		return false;
	}
}
