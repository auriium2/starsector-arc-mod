package arc.campaign.procgen;


/*

public class ArcThemeGenerator extends BaseThemeGenerator {

    public static enum BladeBreakerSystemType {
        DESTROYED("theme_breakers_destroyed", "$breakerDestroyed"),
        SUPPRESSED("theme_breakers_suppressed", "$breakerSuppressed"),
        RESURGENT("theme_breakers_resurgent", "$breakerResurgent"),
        HOMEWORLD("theme_breakers_homeworld", "$breakerHomeworld"),
        ;

        private String tag;
        private String beaconFlag;
        private BladeBreakerSystemType(String tag, String beaconFlag) {
            this.tag = tag;
            this.beaconFlag = beaconFlag;
        }
        public String getTag() {
            return tag;
        }
        public String getBeaconFlag() {
            return beaconFlag;
        }
    }

    public String getThemeId() {
        return Index.ARC_FACTION;
    }

    @Override
    public void generateForSector(ThemeGenContext context, float allowedUnusedFraction) {

        float total = (float) (context.constellations.size() - context.majorThemes.size()) * allowedUnusedFraction;
        if (total <= 0) return;

        int MIN_CONSTELLATIONS_WITH_BREAKERS=5;
        int MAX_CONSTELLATIONS_WITH_BREAKERS=7;

        float CONSTELLATION_SKIP_PROB=0.75f;

        int num = (int) StarSystemGenerator.getNormalRandom(MIN_CONSTELLATIONS_WITH_BREAKERS, MAX_CONSTELLATIONS_WITH_BREAKERS);
        if (num > total) num = (int) total;

        int numDestroyed = (int) (num * (0.23f + 0.1f * random.nextFloat()));
        if (numDestroyed < 1) numDestroyed = 1;
        int numSuppressed = (int) (num * (0.23f + 0.1f * random.nextFloat()));
        if (numSuppressed < 1) numSuppressed = 1;

        float suppressedStationMult = 0.5f;
        int suppressedStations = (int) Math.ceil(numSuppressed * suppressedStationMult);

        WeightedRandomPicker<Boolean> addSuppressedStation = new WeightedRandomPicker<Boolean>(random);
        for (int i = 0; i < numSuppressed; i++) {
            if (i < suppressedStations) {
                addSuppressedStation.add(true, 1f);
            } else {
                addSuppressedStation.add(false, 1f);
            }
        }

        List<Constellation> constellations = getSortedAvailableConstellations(context, false, new Vector2f(), null);
        Collections.reverse(constellations);

        float skipProb = CONSTELLATION_SKIP_PROB;
        if (total < num / (1f - skipProb)) {
            skipProb = 1f - (num / total);
        }
        //skipProb = 0f;

        List<StarSystemData> breakerSystems = new ArrayList<StarSystemData>();

        if (DEBUG) System.out.println("\n\n\n");
        if (DEBUG) System.out.println("Generating Blade Breaker systems");

        int count = 0;

        int numUsed = 0;
        for (int i = 0; i < num && i < constellations.size(); i++) {
            Constellation c = constellations.get(i);
            if (random.nextFloat() < skipProb) {
                if (DEBUG) System.out.println("Skipping constellation " + c.getName());
                continue;
            }

            List<StarSystemData> systems = new ArrayList<StarSystemData>();
            for (StarSystemAPI system : c.getSystems()) {
                StarSystemData data = computeSystemData(system);
                systems.add(data);
            }

            List<StarSystemData> mainCandidates = getSortedSystemsSuitedToBePopulated(systems);

            int numMain = 1 + random.nextInt(2);
            if (numMain > mainCandidates.size()) numMain = mainCandidates.size();
            if (numMain <= 0) {
                if (DEBUG) System.out.println("Skipping constellation " + c.getName() + ", no suitable main candidates");
                continue;
            }

            BladeBreakerSystemType type = BladeBreakerSystemType.RESURGENT;
            if (numUsed < numDestroyed) {
                type = BladeBreakerSystemType.DESTROYED;
            } else if (numUsed < numDestroyed + numSuppressed) {
                type = BladeBreakerSystemType.SUPPRESSED;
            }

            context.majorThemes.put(c, istl_Themes.BREAKERS);
            numUsed++;

            if (DEBUG) System.out.println("Generating " + numMain + " main systems in " + c.getName());
            for (int j = 0; j < numMain; j++) {
                StarSystemData data = mainCandidates.get(j);
                populateMain(data, type);

                data.system.addTag(Tags.THEME_INTERESTING);
                data.system.addTag(istl_Tags.THEME_BREAKER);
                if (type != BladeBreakerSystemType.DESTROYED) {
                    data.system.addTag(Tags.THEME_UNSAFE);
                }
                data.system.addTag(istl_Tags.THEME_BREAKER_MAIN);
                data.system.addTag(type.getTag());
                breakerSystems.add(data);

                if (!NameAssigner.isNameSpecial(data.system)) {
                    NameAssigner.assignSpecialNames(data.system);
                }

                if (type == BladeBreakerSystemType.DESTROYED) {
                    BladeBreakerSeededFleetManager fleets = new BladeBreakerSeededFleetManager(data.system, 3, 8, 1, 2, 0.05f);
                    data.system.addScript(fleets);
                } else if (type == BladeBreakerSystemType.SUPPRESSED) {
                    BladeBreakerSeededFleetManager fleets = new BladeBreakerSeededFleetManager(data.system, 7, 12, 4, 12, 0.25f);
                    data.system.addScript(fleets);

                    boolean addStation = random.nextFloat() < suppressedStationMult;
                    if (j == 0 && !addSuppressedStation.isEmpty()) addSuppressedStation.pickAndRemove();
                    if (addStation) {
                        List<CampaignFleetAPI> stations = addBattlestations(data, 1f, 1, 1, createStringPicker(istl_Tags.BATTLESTATION_BREAKER_LIGHT, 10f));
                        for (CampaignFleetAPI station : stations) {
                            int maxFleets = 2 + random.nextInt(3);
                            BladeBreakerStationFleetManager activeFleets = new BladeBreakerStationFleetManager(
                                    station, 1f, 0, maxFleets, 20f, 6, 12);
                            data.system.addScript(activeFleets);
                        }
                    }
                } else if (type == BladeBreakerSystemType.RESURGENT) {
                    List<CampaignFleetAPI> stations = addBattlestations(data, 1f, 1, 1, createStringPicker(istl_Tags.BATTLESTATION_BREAKER_HEAVY, 10f));
                    for (CampaignFleetAPI station : stations) {
                        int maxFleets = 8 + random.nextInt(5);
                        BladeBreakerStationFleetManager activeFleets = new BladeBreakerStationFleetManager(
                                station, 1f, 2, maxFleets, 10f, 8, 24);
                        data.system.addScript(activeFleets);
                    }
                }
            }

            for (StarSystemData data : systems) {
                int index = mainCandidates.indexOf(data);
                if (index >= 0 && index < numMain) continue;

                populateNonMain(data);

                if (type == BladeBreakerSystemType.DESTROYED) {
                    data.system.addTag(Tags.THEME_INTERESTING_MINOR);
                } else {
                    data.system.addTag(Tags.THEME_INTERESTING);
                }
                data.system.addTag(istl_Tags.THEME_BREAKER);
                //data.system.addTag(Tags.THEME_UNSAFE); // just a few 1-2 frigate fleets, and not even always
                data.system.addTag(istl_Tags.THEME_BREAKER_SECONDARY);
                data.system.addTag(type.getTag());
                breakerSystems.add(data);

                if (random.nextFloat() < 0.5f) {
                    BladeBreakerSeededFleetManager fleets = new BladeBreakerSeededFleetManager(data.system, 1, 3, 1, 2, 0.05f);
                    data.system.addScript(fleets);
                } else {
                    data.system.addTag(istl_Tags.THEME_BREAKER_NO_FLEETS);
                }
            }
//			if (count == 18) {
//				System.out.println("REM RANDOM INDEX " + count + ": " + random.nextLong());
//			}
            count++;
        }

        SpecialCreationContext specialContext = new SpecialCreationContext();
        specialContext.themeId = getThemeId();
        SalvageSpecialAssigner.assignSpecials(breakerSystems, specialContext);

        addDefenders(breakerSystems);

        if (DEBUG) System.out.println("Finished generating Blade Breaker systems\n\n\n\n\n");
    }

    public void addDefenders(List<StarSystemData> systemData) {
        for (StarSystemData data : systemData) {
//			float prob = 0.1f;
//			float max = 3f;
//			if (data.system.hasTag(Tags.THEME_BREAKER_SECONDARY)) {
//				prob = 0.05f;
//				max = 1f;
//			}
            float mult = 1f;
            if (data.system.hasTag("theme_breakers_secondary")) {
                mult = 0.5f;
            }

            for (AddedEntity added : data.generated) {
                if (added.entityType == null) continue;
                if (Entities.WRECK.equals(added.entityType)) continue;

                float prob = 0f;
                float min = 1f;
                float max = 1f;
                if (istl_Entities.STATION_MINING_BREAKER.equals(added.entityType)) {
                    prob = 0.25f;
                    min = 8;
                    max = 15;
                } else if (istl_Entities.ORBITAL_HABITAT_BREAKER.equals(added.entityType)) {
                    prob = 0.25f;
                    min = 8;
                    max = 15;
                } else if (istl_Entities.STATION_RESEARCH_BREAKER.equals(added.entityType)) {
                    prob = 0.25f;
                    min = 10;
                    max = 20;
                }
                // to compensate for this being changed to use fleet points
                min *= 3;
                max *= 3;

                prob *= mult;
                min *= mult;
                max *= mult;
                if (min < 1) min = 1;
                if (max < 1) max = 1;

                if (random.nextFloat() < prob) {
                    Misc.setDefenderOverride(added.entity, new DefenderDataOverride("blade_breakers", 1f, min, max, 4));
                }
                //Misc.setDefenderOverride(added.entity, new DefenderDataOverride("blade_breakers", prob, 1, max));
            }
        }
    }

    public void populateNonMain(StarSystemData data) {
        if (DEBUG) System.out.println(" Generating secondary breaker system in " + data.system.getName());
        boolean special = data.isBlackHole() || data.isNebula() || data.isPulsar();
        if (special) {
            addResearchStations(data, 0.75f, 1, 1, createStringPicker(istl_Entities.STATION_RESEARCH_BREAKER, 10f));
        }

        if (random.nextFloat() < 0.5f) return;

        if (!data.resourceRich.isEmpty()) {
            addMiningStations(data, 0.5f, 1, 1, createStringPicker(istl_Entities.STATION_MINING_BREAKER, 10f));
        }

        if (!special && !data.habitable.isEmpty()) {
            // ruins on planet, or orbital station
            addHabCenters(data, 0.25f, 1, 1, createStringPicker(istl_Entities.ORBITAL_HABITAT_BREAKER, 10f));
        }

        addShipGraveyard(data, 0.05f, 1, 1,
                createStringPicker(Factions.INDEPENDENT, 10f, Factions.SCAVENGERS, 8f, istl_Factions.DASSAULT, 6f, Factions.PIRATES, 3f));

        //addDebrisFields(data, 0.25f, 1, 2, "blade_breakers", 0.1f, 1, 1);
        addDebrisFields(data, 0.25f, 1, 2);

        addDerelictShips(data, 0.5f, 0, 3,
                createStringPicker(Factions.INDEPENDENT, 10f, Factions.SCAVENGERS, 8f, istl_Factions.DASSAULT, 6f, Factions.PIRATES, 3f));

        addCaches(data, 0.25f, 0, 2, createStringPicker(
                istl_Entities.WEAPONS_CACHE_BREAKER, 2f,
                istl_Entities.WEAPONS_CACHE_SMALL_BREAKER, 5f,
                "supply_cache", 4f,
                "supply_cache_small", 10f,
                "equipment_cache", 4f,
                "equipment_cache_small", 10f
        ));
    }

    public void populateMain(StarSystemData data, BladeBreakerSystemType type) {

        if (DEBUG) System.out.println(" Generating Blade Breaker center in " + data.system.getName());

        StarSystemAPI system = data.system;

        addBeacon(system, type);

        if (DEBUG) System.out.println("    Added hardened warning beacon");

        int maxHabCenters = 1 + random.nextInt(3);

        HabitationLevel level = HabitationLevel.LOW;
        if (maxHabCenters == 2) level = HabitationLevel.MEDIUM;
        if (maxHabCenters >= 3) level = HabitationLevel.HIGH;

        addHabCenters(data, 1, maxHabCenters, maxHabCenters, createStringPicker(
                istl_Entities.ORBITAL_HABITAT_BREAKER, 10f));

        // add various stations, orbiting entities, etc
        float probGate = 1f;
        float probRelay = 1f;
        float probMining = 0.5f;
        float probResearch = 0.25f;

        switch (level) {
            case HIGH:
                probGate = 0.75f;
                probRelay = 1f;
                break;
            case MEDIUM:
                probGate = 0.5f;
                probRelay = 0.75f;
                break;
            case LOW:
                probGate = 0.25f;
                probRelay = 0.5f;
                break;
        }

        //addCommRelay(data, probRelay);
        addObjectives(data, probRelay);
        addInactiveGate(data, probGate, 0.5f, 0.5f,
                createStringPicker(Factions.TRITACHYON, 10f, Factions.HEGEMONY, 7f, Factions.INDEPENDENT, 3f));

        addShipGraveyard(data, 0.25f, 1, 1,
                createStringPicker(Factions.TRITACHYON, 10f, Factions.HEGEMONY, 7f, Factions.INDEPENDENT, 3f));

        addMiningStations(data, probMining, 1, 1, createStringPicker(istl_Entities.STATION_MINING_BREAKER, 10f));

        addResearchStations(data, probResearch, 1, 1, createStringPicker(istl_Entities.STATION_RESEARCH_BREAKER, 10f));

        addDebrisFields(data, 0.75f, 1, 5);

        addDerelictShips(data, 0.75f, 0, 7,
                createStringPicker(Factions.TRITACHYON, 10f, Factions.HEGEMONY, 7f, Factions.INDEPENDENT, 3f));

        addCaches(data, 0.75f, 0, 3, createStringPicker(
                istl_Entities.WEAPONS_CACHE_BREAKER, 5f,
                istl_Entities.WEAPONS_CACHE_SMALL_BREAKER, 5f,
                "supply_cache", 10f,
                "supply_cache_small", 10f,
                "equipment_cache", 10f,
                "equipment_cache_small", 10f
        ));
    }

    public List<StarSystemData> getSortedSystemsSuitedToBePopulated(List<StarSystemData> systems) {
        List<StarSystemData> result = new ArrayList<>();

        for (StarSystemData data : systems) {
            if (data.isBlackHole() || data.isNebula() || data.isPulsar()) continue;

            if (data.planets.size() >= 4 || data.habitable.size() >= 1) {
                result.add(data);

//				Collections.sort(data.habitable, new Comparator<PlanetAPI>() {
//					public int compare(PlanetAPI o1, PlanetAPI o2) {
//						return (int) Math.signum(o1.getMarket().getHazardValue() - o2.getMarket().getHazardValue());
//					}
//				});
            }
        }

        Collections.sort(systems, new Comparator<StarSystemData>() {
            public int compare(StarSystemData o1, StarSystemData o2) {
                float s1 = getMainCenterScore(o1);
                float s2 = getMainCenterScore(o2);
                return (int) Math.signum(s2 - s1);
            }
        });
        return result;
    }

    public float getMainCenterScore(StarSystemData data) {
        float total = 0f;
        total += data.planets.size() * 1f;
        total += data.habitable.size() * 2f;
        total += data.resourceRich.size() * 0.25f;
        return total;
    }

    public static CustomCampaignEntityAPI addBeacon(StarSystemAPI system, BladeBreakerSystemType type) {

        SectorEntityToken anchor = system.getHyperspaceAnchor();
        List<SectorEntityToken> points = Global.getSector().getHyperspace().getEntities(JumpPointAPI.class);

        float minRange = 600;

        float closestRange = Float.MAX_VALUE;
        JumpPointAPI closestPoint = null;
        for (SectorEntityToken entity : points) {
            JumpPointAPI point = (JumpPointAPI) entity;

            if (point.getDestinations().isEmpty()) continue;

            JumpDestination dest = point.getDestinations().get(0);
            if (dest.getDestination().getContainingLocation() != system) continue;

            float dist = Misc.getDistance(anchor.getLocation(), point.getLocation());
            if (dist < minRange + point.getRadius()) continue;

            if (dist < closestRange) {
                closestPoint = point;
                closestRange = dist;
            }
        }

        CustomCampaignEntityAPI beacon = Global.getSector().getHyperspace().addCustomEntity(null, null, "istl_bladebreaker_beacon", Factions.NEUTRAL);
        //beacon.getMemoryWithoutUpdate().set("$breaker", true);
        //beacon.addTag(istl_Tags.HARDENED_WARNING_BEACON); not sure this needs to be added AGAIN
        beacon.getMemoryWithoutUpdate().set(type.getBeaconFlag(), true);

        switch (type) {
            case DESTROYED: beacon.addTag(Tags.BEACON_LOW); break;
            case SUPPRESSED: beacon.addTag(Tags.BEACON_MEDIUM); break;
            case RESURGENT: beacon.addTag(Tags.BEACON_HIGH); break;
        }

        if (closestPoint == null) {
            float orbitDays = minRange / (10f + StarSystemGenerator.random.nextFloat() * 5f);
            //beacon.setCircularOrbit(anchor, StarSystemGenerator.random.nextFloat() * 360f, minRange, orbitDays);
            beacon.setCircularOrbitPointingDown(anchor, StarSystemGenerator.random.nextFloat() * 360f, minRange, orbitDays);
        } else {
            float angleOffset = 20f + StarSystemGenerator.random.nextFloat() * 20f;
            float angle = Misc.getAngleInDegrees(anchor.getLocation(), closestPoint.getLocation()) + angleOffset;
            float radius = closestRange;

            if (closestPoint.getOrbit() != null) {
//				OrbitAPI orbit = Global.getFactory().createCircularOrbit(anchor, angle, radius,
//																closestPoint.getOrbit().getOrbitalPeriod());
                OrbitAPI orbit = Global.getFactory().createCircularOrbitPointingDown(anchor, angle, radius,
                        closestPoint.getOrbit().getOrbitalPeriod());
                beacon.setOrbit(orbit);
            } else {
                Vector2f beaconLoc = Misc.getUnitVectorAtDegreeAngle(angle);
                beaconLoc.scale(radius);
                Vector2f.add(beaconLoc, anchor.getLocation(), beaconLoc);
                beacon.getLocation().set(beaconLoc);
            }
        }

        Color glowColor = new Color(75,255,175,255);
        Color pingColor = new Color(75,255,175,255);
        if (type == BladeBreakerSystemType.SUPPRESSED) {
            glowColor = new Color(175,145,0,255);
            pingColor = new Color(175,145,0,255);
        } else if (type == BladeBreakerSystemType.RESURGENT) {
            glowColor = new Color(250,55,0,255);
            pingColor = new Color(250,55,0,255);
        }
        Misc.setWarningBeaconColors(beacon, glowColor, pingColor);

        return beacon;
    }


    protected List<Constellation> getSortedAvailableConstellations(ThemeGenContext context, boolean emptyOk, final Vector2f sortFrom, List<Constellation> exclude) {
        List<Constellation> constellations = new ArrayList<>();
        for (Constellation c : context.constellations) {
            if (context.majorThemes.containsKey(c)) continue;
            if (!emptyOk && constellationIsEmpty(c)) continue;

            constellations.add(c);
        }

        if (exclude != null) {
            constellations.removeAll(exclude);
        }

        Collections.sort(constellations, new Comparator<Constellation>() {
            public int compare(Constellation o1, Constellation o2) {
                float d1 = Misc.getDistance(o1.getLocation(), sortFrom);
                float d2 = Misc.getDistance(o2.getLocation(), sortFrom);
                return (int) Math.signum(d2 - d1);
            }
        });
        return constellations;
    }

    public static boolean constellationIsEmpty(Constellation c) {
        for (StarSystemAPI s : c.getSystems()) {
            if (!systemIsEmpty(s)) return false;
        }
        return true;
    }
    public static boolean systemIsEmpty(StarSystemAPI system) {
        for (PlanetAPI p : system.getPlanets()) {
            if (!p.isStar()) return false;
        }
        //system.getTerrainCopy().isEmpty()
        return true;
    }

    public List<CampaignFleetAPI> addBattlestations(StarSystemData data, float chanceToAddAny, int min, int max,
                                                    WeightedRandomPicker<String> stationTypes) {
        List<CampaignFleetAPI> result = new ArrayList<CampaignFleetAPI>();
        if (random.nextFloat() >= chanceToAddAny) return result;

        int num = min + random.nextInt(max - min + 1);
        if (DEBUG) System.out.println("    Adding " + num + " guardians");
        for (int i = 0; i < num; i++) {

            EntityLocation loc = pickCommonLocation(random, data.system, 200f, true, null);

            String type = stationTypes.pick();
            if (loc != null) {

                CampaignFleetAPI fleet = FleetFactoryV3.createEmptyFleet("blade_breakers", FleetTypes.BATTLESTATION, null);

                FleetMemberAPI member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, type);
                fleet.getFleetData().addFleetMember(member);

                //fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_PIRATE, true);
                fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
                fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_JUMP, true);
                fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_ALLOW_DISENGAGE, true);
                fleet.addTag(Tags.NEUTRINO_HIGH);

                fleet.setStationMode(true);

                addBladeBreakerStationInteractionConfig(fleet);

                data.system.addEntity(fleet);

                //fleet.setTransponderOn(true);
                fleet.clearAbilities();
                fleet.addAbility(Abilities.TRANSPONDER);
                fleet.getAbility(Abilities.TRANSPONDER).activate();
                fleet.getDetectedRangeMod().modifyFlat("gen", 1000f);

                fleet.setAI(null);

                setEntityLocation(fleet, loc, null);
                convertOrbitWithSpin(fleet, 5f);

                boolean damaged = type.toLowerCase().contains("damaged");
                float mult = 25f;
                int level = 20;
                if (damaged) {
                    mult = 10f;
                    level = 10;
                    fleet.getMemoryWithoutUpdate().set("$damagedStation", true);
                } //else {
                PersonAPI commander = OfficerManagerEvent.createOfficer(
                        Global.getSector().getFaction("blade_breakers"), level, true);
                if (!damaged) {
                    commander.getStats().setSkillLevel(Skills.GUNNERY_IMPLANTS, 3);
                }
                FleetFactoryV3.addCommanderSkills(commander, fleet, random);
                fleet.setCommander(commander);
                fleet.getFlagship().setCaptain(commander);
                //}

                member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());

                //BladeBreakerSeededFleetManager.addBladeBreakerAICoreDrops(random, fleet, mult);

                result.add(fleet);

//				MarketAPI market = Global.getFactory().createMarket("station_market_" + fleet.getId(), fleet.getName(), 0);
//				market.setPrimaryEntity(fleet);
//				market.setFactionId(fleet.getFaction().getId());
//				market.addCondition(Conditions.ABANDONED_STATION);
//				market.addSubmarket(Submarkets.SUBMARKET_STORAGE);
//				((StoragePlugin)market.getSubmarket(Submarkets.SUBMARKET_STORAGE).getPlugin()).setPlayerPaidToUnlock(true);
//				fleet.setMarket(market);

            }
        }

        return result;
    }

    public static void addBladeBreakerStationInteractionConfig(CampaignFleetAPI fleet) {
        fleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_INTERACTION_DIALOG_CONFIG_OVERRIDE_GEN,
                new BladeBreakerStationInteractionConfigGen());
    }

    @Override
    public int getOrder() {
        return 1500;
    }

    public static class BladeBreakerStationInteractionConfigGen implements FIDConfigGen {
        public FIDConfig createConfig() {
            FIDConfig config = new FIDConfig();

            config.alwaysAttackVsAttack = true;
            config.leaveAlwaysAvailable = true;
            config.showFleetAttitude = false;
            config.showTransponderStatus = false;
            config.showEngageText = false;

            config.delegate = new BaseFIDDelegate() {
                public void postPlayerSalvageGeneration(InteractionDialogAPI dialog, FleetEncounterContext context, CargoAPI salvage) {
                }
                public void notifyLeave(InteractionDialogAPI dialog) {
                }
                public void battleContextCreated(InteractionDialogAPI dialog, BattleCreationContext bcc) {
                    bcc.aiRetreatAllowed = false;
                    bcc.objectivesAllowed = false;
                }
            };
            return config;
        }
    }
}


*/
