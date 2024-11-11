package arc.campaign.plugin;

import arc.Index;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflaterParams;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.plugins.CreateFleetPlugin;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;

import java.util.List;
import java.util.Random;

import static com.fs.starfarer.api.campaign.FactionAPI.ShipPickMode.PRIORITY_THEN_ALL;
import static com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3.*;


public class ARCFleetPlugin implements CreateFleetPlugin {

    public static float DEFAULT_AUX_PERCENT = 0.25f;
    public static float RANDOM_AUX_PERCENT = 0.25f;

    @Override
    public int getHandlingPriority(Object params) {
        if (!(params instanceof FleetParamsV3)) return -1;
        FleetParamsV3 fleetParams = (FleetParamsV3) params;
        if (!fleetParams.factionId.equals(Index.ARC_FACTION)) return -1;
        return GenericPluginManagerAPI.MOD_SUBSET + 1;
    }

    protected static int sizeOverride = 0;

    @Override
    public CampaignFleetAPI createFleet(FleetParamsV3 params) {

        // Stuff we don't care about

        boolean fakeMarket = false;
        MarketAPI market = pickMarket(params);
        if (market == null) {
            market = Global.getFactory().createMarket("fake", "fake", 5);
            market.getStability().modifyFlat("fake", 10000);
            market.setFactionId(params.factionId);
            SectorEntityToken token = Global.getSector().getHyperspace().createToken(0, 0);
            market.setPrimaryEntity(token);

            market.getStats().getDynamic().getMod(Stats.FLEET_QUALITY_MOD).modifyFlat("fake", BASE_QUALITY_WHEN_NO_MARKET);

            market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).modifyFlat("fake", 1f);

            fakeMarket = true;
        }

        boolean sourceWasNull = params.source == null;
        params.source = market;
        if (sourceWasNull && params.qualityOverride == null) {
            params.updateQualityAndProducerFromSourceMarket();
        }
        String factionId = params.factionId;
        if (factionId == null) factionId = params.source.getFactionId();

        FactionAPI.ShipPickMode mode = PRIORITY_THEN_ALL;
        if (params.modeOverride != null) mode = params.modeOverride;

        CampaignFleetAPI fleet = createEmptyFleet(factionId, params.fleetType, market);
        fleet.getFleetData().setOnlySyncMemberLists(true);

        Misc.getSalvageSeed(fleet);

        FactionDoctrineAPI doctrine = fleet.getFaction().getDoctrine();
        if (params.doctrineOverride != null) {
            doctrine = params.doctrineOverride;
        }

        float numShipsMult = 1f;
        if (params.ignoreMarketFleetSizeMult == null || !params.ignoreMarketFleetSizeMult) {
            numShipsMult = market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).computeEffective(0f);
        }

        float quality = params.quality + params.qualityMod;
        if (params.qualityOverride != null) {
            quality = params.qualityOverride;
        }
        Random random = new Random();
        if (params.random != null) random = params.random;


        //Determine how much Diable should be shitted into the fleet
        float auxiliaryUsage = doctrine.getCombatFreighterCombatUseFraction();

        if (auxiliaryUsage == DEFAULT_AUX_PERCENT) {
            auxiliaryUsage += RANDOM_AUX_PERCENT * random.nextFloat();
        }
        boolean usesAuxiliaries = auxiliaryUsage > 0f;

        float combatPts = params.combatPts * numShipsMult * (1f - auxiliaryUsage);
        int auxiliaryPts = (int) (params.combatPts * numShipsMult * auxiliaryUsage);

        if (params.onlyApplyFleetSizeToCombatShips != null && params.onlyApplyFleetSizeToCombatShips) {
            numShipsMult = 1f;
        }

        float freighterPts = params.freighterPts * numShipsMult;
        float tankerPts = params.tankerPts * numShipsMult;
        float transportPts = params.transportPts * numShipsMult;
        float linerPts = params.linerPts * numShipsMult;
        float utilityPts = params.utilityPts * numShipsMult;

        if (combatPts < 10 && combatPts > 0) {
            combatPts = Math.max(combatPts, 5 + random.nextInt(6));
        }

        float dW = (float) doctrine.getWarships() + random.nextInt(3) - 2;
        float dC = (float) doctrine.getCarriers() + random.nextInt(3) - 2;
        float dP = (float) doctrine.getPhaseShips() + random.nextInt(3) - 2;

        boolean strict = doctrine.isStrictComposition();
        if (strict) {
            dW = (float) doctrine.getWarships() - 1;
            dC = (float) doctrine.getCarriers() - 1;
            dP = (float) doctrine.getPhaseShips() -1;
        }

        if (!strict) {
            float r1 = random.nextFloat();
            float r2 = random.nextFloat();
            float min = Math.min(r1, r2);
            float max = Math.max(r1, r2);

            float mag = 1f;
            float v1 = min;
            float v2 = max - min;
            float v3 = 1f - max;

            v1 *= mag;
            v2 *= mag;
            v3 *= mag;

            v1 -= mag/3f;
            v2 -= mag/3f;
            v3 -= mag/3f;

            dW += v1;
            dC += v2;
            dP += v3;
        }

        if (doctrine.getWarships() <= 0) dW = 0;
        if (doctrine.getCarriers() <= 0) dC = 0;
        if (doctrine.getPhaseShips() <= 0) dP = 0;

        boolean banPhaseShipsEtc = !fleet.getFaction().isPlayerFaction() &&
                combatPts < FLEET_POINTS_THRESHOLD_FOR_ANNOYING_SHIPS;
        if (params.forceAllowPhaseShipsEtc != null && params.forceAllowPhaseShipsEtc) {
            banPhaseShipsEtc = false;
        }

        params.mode = mode;
        params.banPhaseShipsEtc = banPhaseShipsEtc;

        if (dW < 0) dW = 0;
        if (dC < 0) dC = 0;
        if (dP < 0) dP = 0;

        float extra = 7 - (dC + dP + dW);
        if (extra < 0) extra = 0f;
        if (doctrine.getWarships() > doctrine.getCarriers() && doctrine.getWarships() > doctrine.getPhaseShips()) {
            dW += extra;
        } else if (doctrine.getCarriers() > doctrine.getWarships() && doctrine.getCarriers() > doctrine.getPhaseShips()) {
            dC += extra;
        } else if (doctrine.getPhaseShips() > doctrine.getWarships() && doctrine.getPhaseShips() > doctrine.getCarriers()) {
            dP += extra;
        }


        float doctrineTotal = dW + dC + dP;

        combatPts = (int) combatPts;
        int warships = (int) (combatPts * dW / doctrineTotal);
        int carriers = (int) (combatPts * dC / doctrineTotal);
        int phase = (int) (combatPts * dP / doctrineTotal);

        warships += (combatPts - warships - carriers - phase);

        addCombatFleetPoints(fleet, random, warships, carriers, phase, params);

        addFreighterFleetPoints(fleet, random, freighterPts, params);
        addTankerFleetPoints(fleet, random, tankerPts, params);
        addTransportFleetPoints(fleet, random, transportPts, params);
        addLinerFleetPoints(fleet, random, linerPts, params);
        addUtilityFleetPoints(fleet, random, utilityPts, params);

        // Dustkeepers who use auxiliaries will field fewer but more powerful Remnant ships
        int maxShips = Global.getSettings().getInt("maxShipsInAIFleet");
        // e.g if 50% FP as auxiliaries, then cap Remnants at 15
        if (usesAuxiliaries) {
            maxShips = Math.round(maxShips * (1f - auxiliaryUsage));
        }
        if (params.maxNumShips != null) {
            maxShips = params.maxNumShips;
        }

        if (fleet.getFleetData().getNumMembers() > maxShips) {
            if (params.doNotPrune == null || !params.doNotPrune) {
                float targetFP = getFP(fleet);
                if (params.doNotAddShipsBeforePruning == null || !params.doNotAddShipsBeforePruning) {
                    sizeOverride = 5;
                    addCombatFleetPoints(fleet, random, warships, carriers, phase, params);
                    addFreighterFleetPoints(fleet, random, freighterPts, params);
                    addTankerFleetPoints(fleet, random, tankerPts, params);
                    addTransportFleetPoints(fleet, random, transportPts, params);
                    addLinerFleetPoints(fleet, random, linerPts, params);
                    addUtilityFleetPoints(fleet, random, utilityPts, params);
                    sizeOverride = 0;
                }

                int size = doctrine.getShipSize();
                pruneFleet(maxShips, size, fleet, targetFP, random);

                float currFP = getFP(fleet);
            }
            fleet.getFleetData().sort();
        } else {
            fleet.getFleetData().sort();
        }

        // ... and their auxiliaries ignore the ship cap!
        if (usesAuxiliaries) {
           // addAuxiliaryPoints(fleet, random, auxiliaryPts, params);
        }

        fleet.getFleetData().sort();

        if (params.withOfficers) {
            addCommanderAndOfficers(fleet, params, random);
        }

        if (fleet.getFlagship() != null) {
            if (params.flagshipVariantId != null) {
                fleet.getFlagship().setVariant(Global.getSettings().getVariant(params.flagshipVariantId), false, true);
            } else if (params.flagshipVariant != null) {
                fleet.getFlagship().setVariant(params.flagshipVariant, false, true);
            }
        }

        if (params.onlyRetainFlagship != null && params.onlyRetainFlagship) {
            for (FleetMemberAPI curr : fleet.getFleetData().getMembersListCopy()) {
                if (curr.isFlagship()) continue;
                fleet.getFleetData().removeFleetMember(curr);
            }
        }
        //fleet.getFlagship()
        fleet.forceSync();

        if (fakeMarket) {
            params.source = null;
        }

        DefaultFleetInflaterParams p = new DefaultFleetInflaterParams();
        p.quality = quality;
        if (params.averageSMods != null) {
            p.averageSMods = params.averageSMods;
        }
        p.persistent = true;
        p.seed = random.nextLong();
        p.mode = mode;
        p.timestamp = params.timestamp;
        p.allWeapons = params.allWeapons;
        if (params.doctrineOverride != null) {
            p.rProb = params.doctrineOverride.getAutofitRandomizeProbability();
        }
        if (params.factionId != null) {
            p.factionId = params.factionId;
        }

        FleetInflater inflater = Misc.getInflater(fleet, p);
        fleet.setInflater(inflater);

        fleet.getFleetData().setOnlySyncMemberLists(false);
        fleet.getFleetData().sort();

        List<FleetMemberAPI> members = fleet.getFleetData().getMembersListCopy();
        for (FleetMemberAPI member : members) {
            member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());
            int timesRecovered = random.nextInt(4) + 1;
            if (!member.getCaptain().isDefault()) {
                // Slivers get the hand-me-downs
                if (member.getCaptain().getRank().equals(Ranks.SPACE_LIEUTENANT)) {
                    timesRecovered += random.nextInt(7);
                }
            }
            // and the autopiloted ones are the oldest
            else {
                timesRecovered += random.nextInt(9) + 2;
            }
            // XIV is considered unlucky by Dustkeepers, so they skip to XV
            if (timesRecovered == 13) {
                timesRecovered++;
            }
            member.setShipName(member.getShipName() + " " + Global.getSettings().getRoman(timesRecovered + 1));
        }

        if (fleet.getCommander().getVoice() == null) {
            if (auxiliaryUsage > DEFAULT_AUX_PERCENT + (RANDOM_AUX_PERCENT * 0.5f)) {
                fleet.getCommander().setVoice("sotf_dkhunter");
            } else {
                fleet.getCommander().setVoice("sotf_dkfaithful");
            }
        }

        // give the fleet a name incorporating its commander's suffix
        // three-quarters chance to go for a regular name, like "Fabric's Shard"
        // one-quarter chance to go for a fancy name, like "Equinox's Ethereal Castellans" or "Lyric's Hellfire Wolves"
        assignFleetName(fleet, params.fleetType, auxiliaryUsage, false);

        float requestedPoints = params.getTotalPts();
        float actualPoints = fleet.getFleetPoints();

        Misc.setSpawnFPMult(fleet, actualPoints / Math.max(1f, requestedPoints));

        return fleet;
    }

    // OH GOD PLEASE HELP THERE'S MORE NAME LISTS TO ADD TO INSTEAD OF DOING REAL CONTENT
    public static void assignFleetName(CampaignFleetAPI fleet, String fleetType, float auxiliaryUsage, boolean forceFancy) {
        boolean fancyName = Math.random() < 0.25f;
        if (forceFancy) {
            fancyName = true;
        }

        String baseName;
        if (!fancyName) {
            baseName = fleet.getFaction().getFleetTypeName(fleetType);
        } else {
            boolean oldguardName = auxiliaryUsage > (DEFAULT_AUX_PERCENT + (RANDOM_AUX_PERCENT * 0.5f));
            WeightedRandomPicker<String> post1 = new WeightedRandomPicker<String>();
            WeightedRandomPicker<String> post2 = new WeightedRandomPicker<String>();


            post1.add("Abyss ");
            post1.add("Ardent ");
            post1.add("Blazing ");
            post1.add("Brazen ");
            post1.add("Dawn ");
            post1.add("Dusk ");
            post1.add("Endless ");
            post1.add("Ether ");
            post1.add("Fervent ");
            post1.add("Flame ");
            post1.add("Infinite ");
            post1.add("Hyperspace ");
            post1.add("Machine-");
            post1.add("Neutron ");
            post1.add("Night ");
            post1.add("Oathsworn ");
            post1.add("Sacrificial ");
            post1.add("Soul ");
            post1.add("Space ");
            post1.add("Star ");
            post1.add("Steel ");
            post1.add("Tachyon ");
            post1.add("Twilight ");
            post1.add("Vengeful ");
            post1.add("Void ");
            post1.add("Unbroken ");
            post1.add("Unforgiving ");
            post1.add("Unyielding ");
            post1.add("Zealous ");

            post2.add("Blades");
            post2.add("Dragons");
            post2.add("Drakes");
            post2.add("Gryphons");
            post2.add("Heralds");
            post2.add("Keepers");
            post2.add("Owls");
            post2.add("Spears");
            post2.add("Swords");
            post2.add("Watchdogs");
            post2.add("Wyrms");




            baseName = post1.pick() + post2.pick();
        }

        fleet.setName(baseName);
        boolean useCommanderSuffix = false; // should generally be true but who knows what'll happen
        if (fleet.getCommander() != null) {
            if (fleet.getCommander().getMemoryWithoutUpdate().getString("$sotf_suffix") != null) {
                useCommanderSuffix = true;
            }
        }
        // e.g "Dustkeeper Shard", but "Lamia's Splinter" / "Relief's Hell Hounds"
        if (useCommanderSuffix) {
            fleet.setName(fleet.getCommander().getMemoryWithoutUpdate().getString("$sotf_suffix") + "'s " + fleet.getName());
            fleet.setNoFactionInName(true);
        }
        // "The Pale Eyes"
        else if (fancyName) {
            fleet.setName("The " + fleet.getName());
            fleet.setNoFactionInName(true);
        }
    }
/*
    public static void addAuxiliaryPoints(CampaignFleetAPI fleet, Random random, float auxiliaryFP, FleetParamsV3 params) {
        WeightedRandomPicker<String> smallAuxPicker = new WeightedRandomPicker<>(random);
        WeightedRandomPicker<String> mediumAuxPicker = new WeightedRandomPicker<>(random);
        WeightedRandomPicker<String> largeAuxPicker = new WeightedRandomPicker<>(random);

        smallAuxPicker.add(SotfIDs.ROLE_AUXILIARY_SMALL, auxiliaryFP);
        mediumAuxPicker.add(SotfIDs.ROLE_AUXILIARY_MEDIUM, auxiliaryFP);
        largeAuxPicker.add(SotfIDs.ROLE_AUXILIARY_LARGE, auxiliaryFP);

        Map<String, FPRemaining> auxRemaining = new HashMap<String, FPRemaining>();
        FPRemaining remAux = new FPRemaining((int)auxiliaryFP);

        auxRemaining.put(SotfIDs.ROLE_AUXILIARY_SMALL, remAux);
        auxRemaining.put(SotfIDs.ROLE_AUXILIARY_MEDIUM, remAux);
        auxRemaining.put(SotfIDs.ROLE_AUXILIARY_LARGE, remAux);

        int numFails = 0;
        while (numFails < 2) {
            int small = BASE_COUNTS_WITH_4[2][0] + random.nextInt(MAX_EXTRA_WITH_4[1][0] + 1);
            int medium = BASE_COUNTS_WITH_4[2][1] + random.nextInt(MAX_EXTRA_WITH_4[1][1] + 1);
            int large = BASE_COUNTS_WITH_4[2][2] + random.nextInt(MAX_EXTRA_WITH_4[1][2] + 1);

            int smallPre = small / 2;
            small -= smallPre;

            int mediumPre = medium / 2;
            medium -= mediumPre;

            boolean addedSomething = false;

            Set<String> empty = new HashSet<String>();
            addedSomething |= addShips(smallAuxPicker, empty, auxRemaining, null, smallPre, fleet, random, params);
            addedSomething |= addShips(mediumAuxPicker, empty, auxRemaining, null, mediumPre, fleet, random, params);
            addedSomething |= addShips(smallAuxPicker, empty, auxRemaining, null, small, fleet, random, params);
            addedSomething |= addShips(largeAuxPicker, empty, auxRemaining, null, large, fleet, random, params);
            addedSomething |= addShips(mediumAuxPicker, empty, auxRemaining, null, medium, fleet, random, params);

            if (!addedSomething) {
                numFails++;
            }
        }
    }*/
}
