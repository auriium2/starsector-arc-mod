package arc;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class StopgapUtils {

    static class ShipIterator implements Iterator<ShipAPI> {

        public final Iterator<Object> objectIterator;

        public ShipIterator(Iterator<Object> objectIterator) {
            this.objectIterator = objectIterator;
        }

        @Override
        public boolean hasNext() {
            return objectIterator.hasNext();
        }

        @Override
        public ShipAPI next() {
            return (ShipAPI) objectIterator.next();
        }
    }

    static class MissileIterator implements Iterator<MissileAPI> {

        public final Iterator<Object> objectIterator;

        public MissileIterator(Iterator<Object> objectIterator) {
            this.objectIterator = objectIterator;
        }

        @Override
        public boolean hasNext() {
            return objectIterator.hasNext();
        }

        @Override
        public MissileAPI next() {
            return (MissileAPI) objectIterator.next();
        }
    }

    static class ProjectileCheckIterator implements Iterator<DamagingProjectileAPI> {

        public final Iterator<Object> objectIterator;

        public ProjectileCheckIterator(Iterator<Object> objectIterator) {
            this.objectIterator = objectIterator;
        }

        @Override
        public boolean hasNext() {
            while (!(objectIterator.next() instanceof DamagingProjectileAPI)) {
                objectIterator.remove();
                if (!objectIterator.hasNext()) return false;
            }
            
            return objectIterator.hasNext();
        }

        @Override
        public DamagingProjectileAPI next() {
            while (!(objectIterator.next() instanceof DamagingProjectileAPI)) {
                objectIterator.remove();
                if (!objectIterator.hasNext()) return null;
            }

            return (DamagingProjectileAPI) objectIterator.next();
        }
    }

    public static Iterator<ShipAPI> getShipsWithinRange(Vector2f location, float range)
    {

        Iterator<Object> ut =  Global.getCombatEngine().getShipGrid().getCheckIterator(location, range * 2, range * 2);
        return new ShipIterator(ut);

    }

    public static Iterator<MissileAPI> getMissilesWithinRange(Vector2f location, float range)
    {

        Iterator<Object> ut =  Global.getCombatEngine().getMissileGrid().getCheckIterator(location, range * 2, range * 2);
        return new MissileIterator(ut);

    }

    public static List<DamagingProjectileAPI> getProjectilesWithinRange(Vector2f location, float range) {
        Iterator<Object> ut = Global.getCombatEngine().getAllObjectGrid().getCheckIterator(location, range *2, range* 2);

        List<DamagingProjectileAPI> projectiles = new ArrayList<>();
        while (ut.hasNext()) {
            Object o = ut.next();
            if (!(o instanceof DamagingProjectileAPI)) {
                continue;
            }
            projectiles.add((DamagingProjectileAPI) o);

        }

        return projectiles;
    }

}
