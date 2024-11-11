package data.missions;


import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.Pair;
import com.fs.starfarer.api.util.WeightedRandomPicker;

import java.util.*;

class MS_MissionStations {
    public static final Map<String, AvailableStation> commonStationDefs = new HashMap<>();
    public static final Map<String, AvailableStationSet> stationsByFaction = new HashMap<>();

    public static final float STATION_MIN_FP_MULT = 0.9f;     // Modifies min FP required for station to spawn
    public static final float STATION_FP_MOD_MULT = 1f;       // Modifies how much station shrinks its accompanying fleet

    static
    {
        addDefaultStationDefs();

        AvailableStationSet allVanillaSet = new AvailableStationSet(
                new Pair<>(getCommonStation("station1"), 1f),
                new Pair<>(getCommonStation("station2"), 1f),
                new Pair<>(getCommonStation("station3"), 1f),
                new Pair<>(getCommonStation("station1_midline"), 1f),
                new Pair<>(getCommonStation("station2_midline"), 1f),
                new Pair<>(getCommonStation("station3_midline"), 1f),
                new Pair<>(getCommonStation("station1_hightech"), 1f),
                new Pair<>(getCommonStation("station2_hightech"), 1f),
                new Pair<>(getCommonStation("station3_hightech"), 1f)
        );
        AvailableStationSet allVanillaSetFavorLow = new AvailableStationSet(
                new Pair<>(getCommonStation("station1"), 1f),
                new Pair<>(getCommonStation("station2"), 1f),
                new Pair<>(getCommonStation("station3"), 1f),
                new Pair<>(getCommonStation("station1_midline"), 0.75f),
                new Pair<>(getCommonStation("station2_midline"), 0.75f),
                new Pair<>(getCommonStation("station3_midline"), 0.75f),
                new Pair<>(getCommonStation("station1_hightech"), 0.5f),
                new Pair<>(getCommonStation("station2_hightech"), 0.5f),
                new Pair<>(getCommonStation("station3_hightech"), 0.5f)
        );
        AvailableStationSet lowSet = new AvailableStationSet(
                new Pair<>(getCommonStation("station1"), 1f),
                new Pair<>(getCommonStation("station2"), 1f),
                new Pair<>(getCommonStation("station3"), 1f)
        );
        AvailableStationSet lowAndMidSet = new AvailableStationSet(
                new Pair<>(getCommonStation("station1"), 1f),
                new Pair<>(getCommonStation("station2"), 1f),
                new Pair<>(getCommonStation("station3"), 1f),
                new Pair<>(getCommonStation("station1_midline"), 0.5f),
                new Pair<>(getCommonStation("station2_midline"), 0.5f),
                new Pair<>(getCommonStation("station3_midline"), 0.5f)
        );
        AvailableStationSet midSet = new AvailableStationSet(
                new Pair<>(getCommonStation("station1_midline"), 1f),
                new Pair<>(getCommonStation("station2_midline"), 1f),
                new Pair<>(getCommonStation("station3_midline"), 1f)
        );
        AvailableStationSet midAndHighSet = new AvailableStationSet(
                new Pair<>(getCommonStation("station1_midline"), 1f),
                new Pair<>(getCommonStation("station2_midline"), 1f),
                new Pair<>(getCommonStation("station3_midline"), 1f),
                new Pair<>(getCommonStation("station1_hightech"), 0.5f),
                new Pair<>(getCommonStation("station2_hightech"), 0.5f),
                new Pair<>(getCommonStation("station3_hightech"), 0.5f)
        );
        AvailableStationSet highSet = new AvailableStationSet(
                new Pair<>(getCommonStation("station1_hightech"), 1f),
                new Pair<>(getCommonStation("station2_hightech"), 1f),
                new Pair<>(getCommonStation("station3_hightech"), 1f)
        );

        AvailableStationSet pirateSet = new AvailableStationSet(
                new Pair<>(getCommonStation("station1"), 1f),
                new Pair<>(getCommonStation("station1_midline"), 0.8f),
                new Pair<>(getCommonStation("station1_hightech"), 0.5f)
        );
        // 4 in 7 chance to remove 67% of modules, 2 in 7 chance to remove 33%, and 1 in 7 chance to remove none
        pirateSet.removalMultPicker.add(0.67f, 4);
        pirateSet.removalMultPicker.add(0.33f, 2);
        pirateSet.removalMultPicker.add(0f, 1);

        AvailableStationSet remnantSet = new AvailableStationSet(
                new Pair<>(new AvailableStation("remnant_station2_Standard", 250, 250), 1f), // Is like a tier 2+ station
                new Pair<>(new AvailableStation("remnant_station2_Damaged", 100, 100), 1f) // This version is damaged and much weaker
        );
        AvailableStationSet derelictSet = new AvailableStationSet(
                new Pair<>(new AvailableStation("station_derelict_survey_mothership_Standard", 40, 40), 1f) // Immobile super capital ship that is very weak
        );
        AvailableStationSet shadowYardsSet = new AvailableStationSet(
                new Pair<>(new AvailableStation("ms_station1_SRA_Standard", 100, 100), 1f),
                new Pair<>(new AvailableStation("ms_station2_SRA_Standard", 200, 200), 1f),
                new Pair<>(new AvailableStation("ms_station3_SRA_Standard", 400, 400), 1f)
        );
        AvailableStationSet tyradorSet = new AvailableStationSet(
                new Pair<>(new AvailableStation("TSC_Forgeship_Fortress", 250, 250), 1f), // Super capital ship roughly analog to a tier 2+ station in power
                new Pair<>(new AvailableStation("TSC_Forgeship_Siege", 250, 250), 1f),
                new Pair<>(getCommonStation("station1_hightech"), 0.33f),
                new Pair<>(getCommonStation("station2_hightech"), 0.33f),
                new Pair<>(getCommonStation("station3_hightech"), 0.33f)
        );
        AvailableStationSet bladeBreakerSet = new AvailableStationSet(
                new Pair<>(new AvailableStation("istl_bbsuperheavy_std", 200, 200), 1f), // Super capital ship roughly analog to a tier 2 station in power
                new Pair<>(new AvailableStation("istl_bbsuperheavy_dmg", 125, 125), 1f) // This version is damaged and a fair bit weaker
        );
        AvailableStationSet ociSet = new AvailableStationSet(
                new Pair<>(new AvailableStation("kh_station_small_variant", 125, 125), 1f), // Is like a tier 1+ station
                new Pair<>(new AvailableStation("kh_station_medium_variant", 250, 250), 1f), // Is like a tier 2+ station
                new Pair<>(new AvailableStation("kh_station_large_variant", 500, 500), 1f) // Is like a tier 3+ station
        );
        AvailableStationSet anarchistsSet = new AvailableStationSet(
                new Pair<>(new AvailableStation("pack_anarchist_station_Den", 100, 100), 1f), // Is like a tier 1 station
                new Pair<>(new AvailableStation("pack_anarchist_station_Camp", 175, 175), 1f) // Is like a tier 2- station
        );

        // To make a faction not use stations, set its station set to null
        stationsByFaction.put("default", allVanillaSet);
        stationsByFaction.put("tiandong", lowSet);
        stationsByFaction.put(Factions.DERELICT, derelictSet);
        stationsByFaction.put(Factions.DIKTAT, allVanillaSetFavorLow);
        stationsByFaction.put(Factions.HEGEMONY, lowAndMidSet);
        stationsByFaction.put(Factions.INDEPENDENT, allVanillaSet);
        stationsByFaction.put(Factions.LIONS_GUARD, highSet);
        stationsByFaction.put(Factions.LUDDIC_CHURCH, lowSet);
        stationsByFaction.put(Factions.LUDDIC_PATH, lowSet);
        stationsByFaction.put(Factions.PERSEAN, midAndHighSet);
        stationsByFaction.put(Factions.PIRATES, pirateSet);
        stationsByFaction.put(Factions.REMNANTS, remnantSet);
        stationsByFaction.put(Factions.TRITACHYON, highSet);
        stationsByFaction.put("blackrock_driveyards", midSet);
        stationsByFaction.put("diableavionics", midSet);
        stationsByFaction.put("exigency", null);                          // Doesn't use stations... Yet
        stationsByFaction.put("exipirated", midSet);
        stationsByFaction.put("interstellarimperium", lowAndMidSet);
        stationsByFaction.put("junk_pirates", lowSet);
        stationsByFaction.put("junk_pirates_technicians", anarchistsSet);
        stationsByFaction.put("pack", midSet);
        stationsByFaction.put("syndicate_asp", highSet);
        stationsByFaction.put("templars", midAndHighSet);
        stationsByFaction.put("ORA", highSet);
        stationsByFaction.put("SCY", midSet);
        stationsByFaction.put("shadow_industry", shadowYardsSet);
        stationsByFaction.put("Coalition", tyradorSet);
        stationsByFaction.put("dassault_mikoyan", highSet);
        stationsByFaction.put("6eme_bureau", highSet);
        stationsByFaction.put("blade_breakers", bladeBreakerSet);
        stationsByFaction.put("OCI", ociSet);
        stationsByFaction.put("al_ars", lowSet);
        stationsByFaction.put("gmda", null);                              // Doesn't use stations
        stationsByFaction.put("draco", null);                             // Doesn't use stations... Yet
        stationsByFaction.put("fang", null);                              // Doesn't use stations... Yet
        stationsByFaction.put("HMI", lowSet);
        stationsByFaction.put("mess", null);                              // Doesn't use stations... Yet
        stationsByFaction.put("sylphon", highSet);
        stationsByFaction.put("fob", highSet);
    }

    /**
     * Picks a random station available from the specified faction's set to
     * spawn.
     *
     * @param factionId
     * @param availableFP Will not pick stations whose minFP exceeds this.
     * @return
     */
    public static AvailableStation getStationForFaction(String factionId, float availableFP)
    {
        if (!stationsByFaction.containsKey(factionId))
        {
            factionId = "default";
        }
        AvailableStationSet stations = stationsByFaction.get(factionId);
        if (stations == null)
        {
            return null;
        }

        return stations.pickStation(availableFP);
    }

    public static boolean factionHasStation(String factionId, float availableFP)
    {
        return getStationForFaction(factionId, availableFP) != null;
    }

    public static float getModuleRemovalChanceForFaction(String factionId)
    {
        if (!stationsByFaction.containsKey(factionId))
        {
            factionId = "default";
        }
        return stationsByFaction.get(factionId).getModuleRemovalMult();
    }

    protected static AvailableStation getCommonStation(String id)
    {
        return commonStationDefs.get(id);
    }

    /**
     * Creates station defs for vanilla stations: Orbital Station to Star
     * Fortress, low tech to high tech.
     */
    protected static void addDefaultStationDefs()
    {
        for (int type = 0; type <= 2; type++)
        {
            for (int level = 1; level <= 3; level++)
            {
                String variantId = "station" + level;
                if (type == 1)
                {
                    variantId += "_midline";
                }
                else if (type == 2)
                {
                    variantId += "_hightech";
                }
                String entryId = variantId;

                variantId += "_Standard";

                float minFP = 100, fpValue = 100;
                if (level == 2)
                {
                    minFP = 200;
                    fpValue = 200;
                }
                else if (level == 3)
                {
                    minFP = 400;
                    fpValue = 400;
                }
                minFP *= STATION_MIN_FP_MULT;
                fpValue *= STATION_FP_MOD_MULT;

                AvailableStation station = new AvailableStation(variantId, minFP, fpValue);
                commonStationDefs.put(entryId, station);
            }
        }
    }

    // Modified from PirateBaseIntel
    public static void removeModulesFromStation(FleetMemberAPI station, float removeMult)
    {
        WeightedRandomPicker<Integer> picker = new WeightedRandomPicker<>();
        int index = 1; // Index 0 is station body
        for (String slotId : station.getVariant().getModuleSlots())
        {
            ShipVariantAPI mv = station.getVariant().getModuleVariant(slotId);
            if (Misc.isActiveModule(mv))
            {
                picker.add(index, 1f);
            }
            index++;
        }

        int remove = Math.round(picker.getItems().size() * removeMult);
        if (remove < 1 && removeMult > 0)
        {
            remove = 1;
        }
        if (remove >= picker.getItems().size())
        {
            remove = picker.getItems().size() - 1;
        }

        for (int i = 0; i < remove; i++)
        {
            Integer pick = picker.pickAndRemove();
            if (pick != null)
            {
                station.getStatus().setHullFraction(pick, 0f);
                station.getStatus().setDetached(pick, true);
                station.getStatus().setPermaDetached(pick, true);
            }
        }
    }

    public static class AvailableStation
    {
        // Note: minFP is currently set to be smaller than the FP value in ship_data.csv
        // This is because the "true" FP values are huge (400 for a star fortress),
        // so we might never see higher-end stations without a discount

        // fpValue can be higher/lower than the one in ship data,
        // to make its accompanying fleet smaller/bigger depending on station balance
        // FYI ship_data FP values for the three station levels are 100-200-400
        public String variantId;
        public float minFP;    // Don't spawn in battles smaller than this
        public float fpValue;  // Deduct this much FP from rest of fleet

        public AvailableStation(String variantId, float minFP, float fpValue)
        {
            this(variantId, minFP, fpValue, true);
        }

        public AvailableStation(String variantId, float minFP, float fpValue, boolean useMults)
        {
            if (useMults)
            {
                minFP *= STATION_MIN_FP_MULT;
                fpValue *= STATION_FP_MOD_MULT;
            }

            this.variantId = variantId;
            this.minFP = minFP;
            this.fpValue = fpValue;
        }
    }

    public static class AvailableStationSet
    {
        // First value is station def ID, second is weight
        List<Pair<AvailableStation, Float>> stations = new ArrayList<>();
        WeightedRandomPicker<Float> removalMultPicker = new WeightedRandomPicker<>();

        public AvailableStationSet(Pair<AvailableStation, Float>... stations)
        {
            this.stations.addAll(Arrays.asList(stations));
        }

        public void addStation(AvailableStation station, float weight)
        {
            stations.add(new Pair<>(station, weight));
        }

        public float getModuleRemovalMult()
        {
            if (removalMultPicker.isEmpty())
            {
                return 0;
            }
            return removalMultPicker.pick();
        }

        /**
         * Randomly picks an available station to spawn.
         *
         * @param availableFP Will not pick stations whose minFP exceeds this.
         * @return Station def to spawn
         */
        public AvailableStation pickStation(float availableFP)
        {
            WeightedRandomPicker<AvailableStation> picker = new WeightedRandomPicker<>();

            for (Pair<AvailableStation, Float> stationEntry : stations)
            {
                AvailableStation station = stationEntry.one;
                if (station.minFP > availableFP)
                {
                    continue;
                }
                picker.add(station, stationEntry.two);
            }
            return picker.pick();
        }
    }
}
