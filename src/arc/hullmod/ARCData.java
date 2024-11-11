package arc.hullmod;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import org.dark.shaders.distortion.WaveDistortion;

public class ARCData {


    public enum Mode {
        REPAIR, //if no soft flux, slowly repair armor. consume soft flux to rapidly repair armor up to 70%. Disable shield and reduce speed. orange.
        ASSAULT, //increase speed/manuever as flux goes up, blue mode
        BERSERK, //boost damage N%, boost timeflow, disable shields, consume soft flux to scale, red mode
    }



    public float ventAbilityCooldown = 0;
    public float ventAbilityScale = 0;

    public float modeAbilityScale = 0;
    public Mode mode = Mode.ASSAULT;
    public Mode lastMode = Mode.ASSAULT;
    public boolean holdFireBefore = false;

    public boolean shouldTryToIED = false;
    public float timerTicksBeforeJihad = JIHAD_TICKS;
    public WaveDistortion iedPrep = null;
    public CombatEntityAPI currentTarget = null;

    public static final float JIHAD_TICKS = 2.4f;


}
