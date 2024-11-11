package arc.plugin;

import arc.weapons.mml.MacrossOnHitEffect;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;

import java.util.List;

public class MacrossProximityDetonatePlugin implements EveryFrameCombatPlugin {


    final IntervalUtil intervalUtil = new IntervalUtil(0.05f, 0.3f);

    @Override
    public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {

    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {

        if (Global.getCombatEngine().isPaused()) return;
        intervalUtil.advance(amount);
        if (!intervalUtil.intervalElapsed()) return;

        List<ShipAPI> ships = Global.getCombatEngine().getShips();

        for (MissileAPI missileAPI : Global.getCombatEngine().getMissiles()) {
            if (!Global.getCombatEngine().isInPlay(missileAPI)) continue;
            if (Math.random() > 0.22f) continue;
            if (!missileAPI.getProjectileSpecId().equals("arc_mml_missile")) continue;

            float scale  = MathUtils.getRandomNumberInRange(0.7f, 1.2f);
            float range = 500 * scale;

            for (ShipAPI enemyPossibleShip : ships) {
                if (!enemyPossibleShip.isAlive() || enemyPossibleShip.isHulk()) continue;
                if (enemyPossibleShip.getOwner() == missileAPI.getOwner()) continue;
                if (MathUtils.isWithinRange(enemyPossibleShip.getLocation(), missileAPI.getLocation(), range)) {
                    //happy birthday!!
                    RunnableQueuePlugin.queueTask(
                            () -> {
                                MacrossOnHitEffect.explode(missileAPI.getLocation(), missileAPI, scale, 1f);
                                Global.getCombatEngine().removeEntity(missileAPI);
                            },
                            MathUtils.getRandomNumberInRange(1,5)
                    );

                }
            }
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
