package arc;

import arc.plugin.ai.RepairDroneAI;
import arc.weapons.buster.BusterAI;
import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.CampaignPlugin;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.dark.shaders.util.ShaderLib;

import java.util.List;

public class ARCPlugin extends BaseModPlugin {

    @Override
    public void onApplicationLoad() {
        System.out.println("[ARC] Loading Base Plugin");

        ShaderLib.init();




//        if (!Global.getSettings().getModManager().isModEnabled("MagicLib")) {
//            throw new RuntimeException(
//                    "[ARC] Missing MagicLib, which is needed to start! \nGet it at http://fractalsoftworks.com/forum/index.php?topic=13718"
//            );
//        }
    }

    @Override
    public PluginPick<ShipAIPlugin> pickDroneAI(ShipAPI drone, ShipAPI mothership, DroneLauncherShipSystemAPI system) {
        String id = drone.getHullSpec().getHullId();

        if (id.contentEquals("arc_chesed")) {
            return new PluginPick<ShipAIPlugin>(new RepairDroneAI(drone, mothership, system), CampaignPlugin.PickPriority.MOD_SET);
        }


        return super.pickDroneAI(drone, mothership, system);
    }


    @Override
    public PluginPick<MissileAIPlugin> pickMissileAI(MissileAPI missile, ShipAPI launchingShip) {

        switch (missile.getProjectileSpecId()) {
            case Index.BUSTER_MIRV:
                return new PluginPick<>(new BusterAI(missile, launchingShip), CampaignPlugin.PickPriority.MOD_SPECIFIC);
            default:
                return super.pickMissileAI(missile, launchingShip);
        }



    }

    @Override
    public PluginPick<ShipAIPlugin> pickShipAI(FleetMemberAPI member, ShipAPI ship) {
        //By default, ARC ships are built to fight aggressively. Since aurelia commands them it doesn't matter what the player sticks in the cockpit, she just overrides

        if (ship.isFighter()) return super.pickShipAI(member, ship);
        if (ship.getHullSpec().getHullId().startsWith("arc_")) {

            ShipAIConfig config = new ShipAIConfig();
            config.personalityOverride = "aggressive";
            config.turnToFaceWithUndamagedArmor = false;


            if (ship.getHullSpec().getHullId().contentEquals("malkuth")) {
                config.personalityOverride = "reckless";
                config.backingOffWhileNotVentingAllowed = false;
            }

            if (ship.getHullSpec().getHullId().contentEquals("rdg")) {
                config.personalityOverride = "reckless";
                config.backingOffWhileNotVentingAllowed = false;
            }

            if (ship.getHullSpec().getHullId().contentEquals("arc_yesod")) {
                config.backingOffWhileNotVentingAllowed = true;
                config.personalityOverride = "steady";
            }


            if (ship.getHullSpec().getHullId().contentEquals("arc_chokmah")) {
                config.personalityOverride = "cautious";
            }

            ShipAIPlugin ai = Global.getSettings().createDefaultShipAI(ship, config);

            return new PluginPick<>(ai, CampaignPlugin.PickPriority.MOD_SPECIFIC);

        }

        else return super.pickShipAI(member, ship);
    }

    @Override
    public void onNewGame() {

        SectorAPI sector = Global.getSector();
        List<FactionAPI> factionList = sector.getAllFactions();
        FactionAPI us = sector.getFaction("arc");

        for (FactionAPI faction : factionList) {
            if (faction.equals(us)) continue;
            us.setRelationship(faction.getId(), -60); //arc is ANGRY
        }

        us.setRelationship("diableavionics", -20);
        //new Constellation()

        //do procgen stuff
        //SectorThemeGenerator.generators.add(1, new BladeBreakerThemeGenerator());



    }

    //stolen from sotf
    /*void addScriptsIfNeeded() {
        SectorAPI sector = Global.getSector();
        if (!sector.getIntelManager().hasIntelOfClass(SierraConvIntel.class)) {
            Global.getSector().getIntelManager().addIntel(new SierraConvIntel(), false);
        } else if (!Global.getSector().getListenerManager().hasListenerOfClass(SierraConvIntel.class) && WATCHER) {
            Global.getSector().getListenerManager().addListener(Global.getSector().getIntelManager().getFirstIntel(SierraConvIntel.class));
        }
        sector.registerPlugin(new SotfCampaignPluginImpl());
        if (!sector.hasScript(SotfSierraEFS.class) && WATCHER) {
            sector.addScript(new SotfSierraEFS());
        }
        if (!sector.hasScript(SotfAMemoryHintScript.class) && WATCHER && !sector.getMemoryWithoutUpdate().contains("$sotf_AMemoryCombatStarted")) {
            sector.addScript(new SotfAMemoryHintScript());
        }

        if (!sector.getGenericPlugins().hasPlugin(SotfDustkeeperOfficerPlugin.class)) {
            sector.getGenericPlugins().addPlugin(new SotfDustkeeperOfficerPlugin(), true);
        }
        if (!sector.getGenericPlugins().hasPlugin(SotfDustkeeperFleetCreator.class)) {
            sector.getGenericPlugins().addPlugin(new SotfDustkeeperFleetCreator(), true);
        }
        if (!sector.getGenericPlugins().hasPlugin(SotfDustkeeperChipIconProvider.class)) {
            sector.getGenericPlugins().addPlugin(new SotfDustkeeperChipIconProvider(), true);
        }

        if (!sector.getGenericPlugins().hasPlugin(SotfSalDefModPlugin.class)) {
            sector.getGenericPlugins().addPlugin(new SotfSalDefModPlugin(), true);
        }
    }*/



    @Override
    public void onGameLoad(boolean newGame) {
        SectorAPI sector = Global.getSector();
        MemoryAPI sector_mem = Global.getSector().getMemoryWithoutUpdate();


        if (sector_mem.get(Index.MEM_AURELIA_THOUGHTS) == null) {
            sector_mem.set(Index.MEM_AURELIA_THOUGHTS, 0);
        }
    }
}
