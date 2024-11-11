package arc.weapons.blackbox;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.input.InputEventAPI;

import java.util.List;

public class BlackboxMissileManagerPlugin implements EveryFrameCombatPlugin {

    static final String HAS_LOCK = "arc_has_locked";

    public static void incrementLocksCount(CombatEntityAPI entity) {
        Object out = entity.getCustomData().get(HAS_LOCK);

        if (out == null) {
            entity.setCustomData(HAS_LOCK, 1); return;
        } else {
            entity.setCustomData(HAS_LOCK, ((int) out) + 1);
        }
    }

    public static void decrementLocksCount(CombatEntityAPI entityAPI) {
        Object out = entityAPI.getCustomData().get(HAS_LOCK);

        if (out != null) {
            int currentLocks = (int) out;
            int nextLocks = currentLocks - 1;

            entityAPI.setCustomData(HAS_LOCK, out);
        }
    }

    public static boolean hasAnyLocks(CombatEntityAPI entity) {
        Object out = entity.getCustomData().get(HAS_LOCK);
        if (out == null) return false;

        return ((int)out) >= 1;
    }



    @Override
    public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {

    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (Global.getCombatEngine().isCombatOver()) {

        }
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
}
