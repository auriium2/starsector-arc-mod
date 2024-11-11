package arc.plugin;

import arc.hullmod.coordinator.VariableCoordinator;
import arc.util.ARCUtils;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;

import java.awt.*;
import java.util.List;

import static arc.hullmod.coordinator.VariableCoordinator.BUFF;
import static arc.hullmod.coordinator.VariableCoordinator.DEBUFF;

public class VariableBuffPlugin implements EveryFrameCombatPlugin {

    final IntervalUtil intervalUtil = new IntervalUtil(0.1f, 0.1f);

    @Override
    public void processInputPreCoreControls(float amount, List<InputEventAPI> events) {

    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {

        CombatEngineAPI combatEngineAPI = Global.getCombatEngine();


        if (combatEngineAPI == null || combatEngineAPI.isPaused()) {
            return;
        }

        intervalUtil.advance(amount);



        for (ShipAPI shipAPI : Global.getCombatEngine().getShips()) {

            if (shipAPI.getShield() == null) continue;
            if (!combatEngineAPI.isInPlay(shipAPI) ||!shipAPI.isAlive() || shipAPI.isPiece() || shipAPI.isFighter()) continue;

            VariableCoordinator.Buff buff = (VariableCoordinator.Buff) shipAPI
                    .getCustomData()
                    .get(BUFF);
            VariableCoordinator.Debuff debuff = (VariableCoordinator.Debuff) shipAPI
                    .getCustomData()
                    .get(VariableCoordinator.DEBUFF);

            if (buff != null) {


                if (!buff.isInRangeThisTick && intervalUtil.intervalElapsed()) {
                    buff.power--;
                }

                float percent = (float) Math.min(500, buff.power) / 500;

                if (buff.power <= 0) {
                    //buff has worn off, reset the shield efficiency

                    shipAPI.getMutableStats()
                            .getShieldUpkeepMult()
                            .unmodifyMult(BUFF);

                    shipAPI.getMutableStats()
                            .getShieldAbsorptionMult()
                            .unmodifyMult(BUFF);


                    shipAPI.getShield().setInnerColor(shipAPI.getHullSpec().getShieldSpec().getInnerColor());
                    shipAPI.setCustomData(BUFF, null);
                } else {

                    shipAPI.getMutableStats()
                            .getShieldUpkeepMult()
                            .modifyMult(BUFF, Math.max(1 - percent,0));

                    shipAPI.getMutableStats()
                            .getShieldAbsorptionMult()
                            .modifyMult(BUFF, Math.max(1 - percent, 0.3f));


                    //todo clamp isnt needed
                    Color toUseForStuff = new Color(50, 120, (int) ARCUtils.clamp(0,200, percent * 255), 190);

                    shipAPI.setJitterShields(true);
                    shipAPI.getShield().setInnerColor(toUseForStuff);
                    shipAPI.setJitter(shipAPI, toUseForStuff, percent * 3.0f, (int)(3.0f * percent), 1);


                    buff.isInRangeThisTick = false;

                    shipAPI.setCustomData(BUFF, buff);
                }







            }

            if (debuff != null) {


                if (!debuff.isInRangeThisTick && intervalUtil.intervalElapsed()) {
                    debuff.power--;
                }

                float percent = (float) Math.min(500, debuff.power) / 500;

                if (debuff.power <= 0) {
                    //buff has worn off, reset the shield efficiency

                    shipAPI.getMutableStats()
                            .getShieldUpkeepMult()
                            .unmodifyMult(DEBUFF);

                    shipAPI.getMutableStats()
                            .getShieldAbsorptionMult()
                            .unmodifyMult(DEBUFF);


                    shipAPI.getShield().setInnerColor(shipAPI.getHullSpec().getShieldSpec().getInnerColor());
                    shipAPI.setCustomData(DEBUFF, null);
                } else {

                    shipAPI.getMutableStats()
                            .getShieldUpkeepMult()
                            .modifyMult(DEBUFF, 1 + percent);

                    shipAPI.getMutableStats()
                            .getShieldAbsorptionMult()
                            .modifyMult(DEBUFF, 1 + percent);

                    //todo clamp isnt needed
                    Color c = new Color((int)ARCUtils.clamp(0,255, percent * 255), 20, 20, 190);

                    shipAPI.setJitterShields(true);
                    shipAPI.getShield().setInnerColor(c);
                    shipAPI.setJitter(shipAPI, c, percent * 3.0f, (int)(5.0f * percent), 1);

                    debuff.isInRangeThisTick = false;

                    shipAPI.setCustomData(DEBUFF, debuff);

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
