package arc.hullmod.coordinator;

import arc.Index;
import arc.StopgapUtils;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import data.scripts.util.MagicRender;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.Iterator;

public class VariableCoordinator extends BaseHullMod {

    final SpriteAPI sprite = Global.getSettings().getSprite("fx","tac_warning");
    float rotation = 0;


    final IntervalUtil intervalUtil = new IntervalUtil(0.1f, 0.1f);

    public static final String BUFF = "arc_variable_buff";
    public static final String DEBUFF = "arc_variable_debuff";

    public static class Buff {
        public int power = 50;
        public boolean isInRangeThisTick = false;

    }

    public static class Debuff {
        public int power = 50;
        public boolean isInRangeThisTick = false;
    }


    @Override
    public void advanceInCombat(ShipAPI ship, float delta) {

        boolean isPlayer = (ship.getOwner() == 0);

        CombatEngineAPI combatEngineAPI = Global.getCombatEngine();

        if (combatEngineAPI == null || combatEngineAPI.isPaused()) {
            return;
        }

        if (ship.isHulk() || !ship.isAlive()) {
            return;
        }

        ViewportAPI viewport = Global.getCombatEngine().getViewport();
        final Vector2f location = ship.getLocation();
        float radius = 2000f; //TODO change this to calculate from ship
        float diameter = radius * 2f; //radius * 2 = dia

        if (Global.getCombatEngine().isUIShowingHUD()) {


            //rotator
            if (viewport.isNearViewport(location, radius)) {
                MagicRender.singleframe(
                        this.sprite,
                        new Vector2f(location.getX(), location.getY()),
                        new Vector2f(diameter, diameter),
                        this.rotation,
                        isPlayer ? Index.ALLIED : Index.HOSTILE,
                        false,
                        CombatEngineLayers.BELOW_SHIPS_LAYER
                );
            }
            this.rotation += 5.0f * delta;
            if (this.rotation > 360.0f) {
                this.rotation -= 360.0f;
            }

        }

        intervalUtil.advance(delta);
        if (!intervalUtil.intervalElapsed()) return;




        //buff allies, curse enemies
        for (Iterator<ShipAPI> it = StopgapUtils.getShipsWithinRange(ship.getLocation(), 2000f); it.hasNext(); ) {
            ShipAPI otherShip = it.next();

            if (otherShip.isFighter()) continue;
            ;
            if (otherShip.equals(ship)) continue;
            ;

            if (otherShip.getOwner() == ship.getOwner()) {
                Buff buff = (Buff) otherShip.getCustomData().get(BUFF);

                if (buff == null) {
                    buff = new Buff();
                    combatEngineAPI.spawnEmpArcVisual(ship.getLocation(), ship, otherShip.getLocation(), otherShip, 20f, Color.CYAN, Color.WHITE);

                }

                buff.power = Math.min(buff.power + 2, 500);
                buff.isInRangeThisTick = true;

                otherShip.setCustomData(BUFF, buff);

            } else {
                Debuff debuff = (Debuff) otherShip.getCustomData().get(DEBUFF);

                if (debuff == null) {
                    debuff = new Debuff();
                    combatEngineAPI.spawnEmpArcVisual(ship.getLocation(), ship, otherShip.getLocation(), otherShip, 20f, Color.MAGENTA, Color.WHITE);

                }

                debuff.power = Math.min(debuff.power + 2, 500);
                debuff.isInRangeThisTick = true;

                otherShip.setCustomData(DEBUFF, debuff);
            }

        }



        //add stuff
    }








}
