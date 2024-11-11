package arc.plugin;

import arc.util.ARCUtils;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.util.MagicRender;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;
import java.util.*;

public class RunnableQueuePlugin implements EveryFrameCombatPlugin {


    final static Map<Integer, List<Runnable>> map = new HashMap<>();
    final static IntervalUtil interval = new IntervalUtil(0.01f, 0.1f); //ever 0.05f

    static int current = 0;

    //TODO move tihs somewhere else

    public static Set<ShipAPI> shipsGlowing = new HashSet<>();
    public static Set<ShipAPI> shipsBeaming = new HashSet<>();


    public static void queueTask(Runnable runnable, int intervalTicksLater) {
        map.computeIfAbsent(current+intervalTicksLater , i -> new ArrayList<>()).add(runnable);
    }

    @Override
    public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {

    }



    @Override
    public void advance(float amount, List<InputEventAPI> events) {

        if (Global.getCombatEngine().isPaused()) return;
        if (Global.getCombatEngine().isCombatOver()) {
            shipsGlowing.clear();
            shipsBeaming.clear();
            map.clear();
        }

        /// FUNNY












        /// ARMOR





        List<ShipAPI> fuckOff = new ArrayList<>();

        for (ShipAPI shipAPI : shipsGlowing) {
            if (shipAPI.isPiece() || !shipAPI.isAlive() || !Global.getCombatEngine().isEntityInPlay(shipAPI)) {
                fuckOff.add(shipAPI);
            }
            float fluxLevel = shipAPI.getFluxLevel();
            float[] colors = ARCUtils.shine(fluxLevel);


            String inquire = "";
            if (shipAPI.getHullSpec().getHullId().contains("yesod")) {
                inquire = "yesod";
            }

            if (shipAPI.getHullSpec().getHullId().contains("daat")) {
                inquire = "daat";
            }

            if (shipAPI.getHullSpec().getHullId().contains("gevurah")) {
                inquire = "gevurah";
            }

            if (shipAPI.getHullSpec().getHullId().contains("malkuth")) {
                inquire = "malkuth";
            }

            if (inquire != "") {
                MagicRender.singleframe(
                        Global.getSettings().getSprite("shine", inquire),
                        shipAPI.getLocation(),
                        new Vector2f(shipAPI.getSpriteAPI().getWidth(), shipAPI.getSpriteAPI().getHeight()),
                        shipAPI.getFacing() - 90,
                        new Color(colors[0], colors[1], colors[2], MathUtils.clamp(1 - fluxLevel + 0.1f, 0f,1f)),
                        false,
                        CombatEngineLayers.ABOVE_SHIPS_LAYER
                );
            }




            shipAPI.addAfterimage(new Color(170, 254, 255, (int) (25 * (1.0 - fluxLevel))), 0f, 0f, 0f, 0f, 0f, 0f, 0.75f, 0.33f, true, true, true);
            shipAPI.addAfterimage(new Color(170, 254, 255, (int) (10 * (1.0 - fluxLevel))), 0f, 0f, 0f, 0f, 15f, 0f, 0.75f, 0.33f, true, false, false);

        }

        fuckOff.forEach(shipsGlowing::remove);

        interval.advance(amount);
        if (interval.intervalElapsed()) {









            current++;
            List<Runnable> runnables = map.remove(current);
            if (runnables == null) return;

            for (Runnable runnable : runnables) {
                runnable.run();
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
