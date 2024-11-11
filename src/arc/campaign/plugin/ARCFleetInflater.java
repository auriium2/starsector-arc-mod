package arc.campaign.plugin;

/*
public class ARCFleetInflater implements FleetInflater, AutofitPlugin.AutofitPluginDelegate {


    public static class WeaponsForTier {
        private Map<String, WeaponsForSize> catMap = new LinkedHashMap<>();

        public WeaponsForSize getWeapons(String cat) {
            WeaponsForSize size = catMap.get(cat);
            if (size == null) {
                size = new WeaponsForSize();
                catMap.put(cat, size);
            }
            return size;
        }
    }

    public static class WeaponsForSize {
        private Map<WeaponSize, List<AutofitPlugin.AvailableWeapon>> sizeMap = new LinkedHashMap<>();
        public List<AutofitPlugin.AvailableWeapon> getWeapons(WeaponSize size) {
            return sizeMap.computeIfAbsent(size, k -> new ArrayList<>());
        }
    }

    public static class AvailableFighterImpl implements AvailableFighter {
        private final FighterWingSpecAPI spec;
        private int quantity = 0;

        public AvailableFighterImpl(FighterWingSpecAPI spec, int quantity) {
            this.spec = spec;
            this.quantity = quantity;
        }

        public String getId() {
            return spec.getId();
        }
        public float getPrice() {
            return 0;
        }
        public int getQuantity() {
            return quantity;
        }
        public CargoAPI getSource() {
            return null;
        }
        public SubmarketAPI getSubmarket() {
            return null;
        }
        public FighterWingSpecAPI getWingSpec() {
            return spec;
        }
        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }
    }

    public static class AvailableWeaponImpl implements AutofitPlugin.AvailableWeapon {
        private final WeaponSpecAPI spec;
        private int quantity = 0;
        public AvailableWeaponImpl(WeaponSpecAPI spec, int quantity) {
            this.spec = spec;
            this.quantity = quantity;
        }

        public String getId() {
            return spec.getWeaponId();
        }
        public float getPrice() {
            return 0;
        }
        public int getQuantity() {
            return quantity;
        }
        public CargoAPI getSource() {
            return null;
        }
        public SubmarketAPI getSubmarket() {
            return null;
        }
        public WeaponSpecAPI getSpec() {
            return spec;
        }
        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        private MutableShipStatsAPI savedCostStats = null;
        private float cachedOPCost = -1;

        public float getOPCost(MutableCharacterStatsAPI stats, MutableShipStatsAPI shipStats) {
            if (savedCostStats == shipStats && cachedOPCost >= 0) return cachedOPCost;

            cachedOPCost = spec.getOrdnancePointCost(stats, shipStats);
            savedCostStats = shipStats;
            return cachedOPCost;
        }
    }

    protected DefaultFleetInflaterParams p;

    //TODO copied from sotf- why are these transient? is alex doing reflection based copies? oh no...
    transient FleetMemberAPI currMember = null;
    transient ShipVariantAPI currVariant = null;
    transient List<AutofitPlugin.AvailableFighter> fighters;
    transient List<AutofitPlugin.AvailableWeapon> weapons;
    transient List<String> hullmods;
    transient CampaignFleetAPI fleet;
    transient FactionAPI faction;

    public static class SortedWeapons {
        private final Map<Integer, WeaponsForTier> tierMap = new LinkedHashMap<>();

        public WeaponsForTier getWeapons(int tier) {
            return tierMap.computeIfAbsent(tier, i -> new WeaponsForTier());
        }
    }

    public static float getTierProbability(int tier, float quality) {
        if (tier == 1) return Math.min(0.9f, 0.75f + quality);
        if (tier == 2) return Math.min(0.9f, 0.5f + quality * 0.5f);
        if (tier == 3) return Math.min(0.9f, 0.25f + quality * 0.25f);

        return 1f;
    }

    public ARCFleetInflater(DefaultFleetInflaterParams p) {
        this.p = p;
    }

    public void inflate(CampaignFleetAPI fleet) {
        Random random = new Random();
        //p.seed = null;
        if (p.seed != null) random = new Random(p.seed);

        //p.quality = 2f;

        //random = new Random();


        Random dmodRandom = new Random();
        if (p.seed != null) dmodRandom = Misc.getRandom(p.seed, 5);

        boolean usesAuxiliaries = false;
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            if (SotfMisc.isDustkeeperAuxiliary(member)) {
                usesAuxiliaries = true;
            }
        }

        // autofit for regulars
        // see for details: tldr they use CH, ECM more often
        CoreAutofitPlugin auto = new SotfDustkeeperAutofitPlugin(fleet.getCommander(), false);
        // anyone who uses Cyberwarfare (typically just the flagship) WILL ensure their loadout has ECM to beef it up
        CoreAutofitPlugin cyberAuto = new SotfDustkeeperAutofitPlugin(fleet.getCommander(), true);
        // autofit for auxiliary drones
        CoreAutofitPlugin auxAuto = new CoreAutofitPlugin(fleet.getCommander());
        auto.setRandom(random);
        cyberAuto.setRandom(random);
        auxAuto.setRandom(random);

        boolean upgrade = random.nextFloat() < Math.min(0.1f + p.quality * 0.5f, 0.5f);
        auto.setChecked(CoreAutofitPlugin.UPGRADE, upgrade);
        cyberAuto.setChecked(CoreAutofitPlugin.UPGRADE, true);
        // auxiliaries don't have good weapons unless required by their fit
        // e.g Close Support warden will still try to have a Hvel Driver, but no railguns on a Picket
        auxAuto.setChecked(CoreAutofitPlugin.UPGRADE, false);

        this.fleet = fleet;
        this.faction = fleet.getFaction();
        if (p.factionId != null) {
            this.faction = Global.getSector().getFaction(p.factionId);
        }

        hullmods = new ArrayList<String>(faction.getKnownHullMods());

        SortedWeapons nonPriorityWeapons = new SortedWeapons();
        SortedWeapons priorityWeapons = new SortedWeapons();


        Set<String> weaponCategories = new LinkedHashSet<String>();
        for (String weaponId : faction.getKnownWeapons()) {
            if (!faction.isWeaponKnownAt(weaponId, p.timestamp)) continue;

            WeaponSpecAPI spec = Global.getSettings().getWeaponSpec(weaponId);

            if (spec == null) {
                throw new RuntimeException("Weapon with spec id [" + weaponId + "] not found");
            }

            int tier = spec.getTier();
            String cat = spec.getAutofitCategory();

            if (isPriority(spec)) {
                List<AutofitPlugin.AvailableWeapon> list = priorityWeapons.getWeapons(tier).getWeapons(cat).getWeapons(spec.getSize());
                list.add(new AvailableWeaponImpl(spec, 1000));
            } else {
                List<AutofitPlugin.AvailableWeapon> list = nonPriorityWeapons.getWeapons(tier).getWeapons(cat).getWeapons(spec.getSize());
                list.add(new AvailableWeaponImpl(spec, 1000));
            }
            weaponCategories.add(cat);
        }

        ListMap<AvailableFighter> nonPriorityFighters = new ListMap<>();
        ListMap<AvailableFighter> priorityFighters = new ListMap<>();
        Set<String> fighterCategories = new LinkedHashSet<>();
        for (String wingId : faction.getKnownFighters()) {
            if (!faction.isFighterKnownAt(wingId, p.timestamp)) continue;

            FighterWingSpecAPI spec = Global.getSettings().getFighterWingSpec(wingId);
            if (spec == null) {
                throw new RuntimeException("Fighter wing with spec id [" + wingId + "] not found");
            }

            // Remnant-only fleets don't use Derelict fighters ever
            if (!usesAuxiliaries && spec.hasTag(SotfIDs.TAG_DUSTKEEPER_AUXILIARY)) {
                continue;
            }

            String cat = spec.getAutofitCategory();

            if (isPriority(spec)) {
                priorityFighters.add(cat, new AvailableFighterImpl(spec, 1000));
            } else {
                nonPriorityFighters.add(cat, new AvailableFighterImpl(spec, 1000));
            }
            fighterCategories.add(cat);
        }


        //float averageDmods = (1f - quality) / Global.getSettings().getFloat("qualityPerDMod");
        float averageDmods = getAverageDmodsForQuality(p.quality);

        //System.out.println("Quality: " + quality + ", Average: " + averageDmods);

        boolean forceAutofit = fleet.getMemoryWithoutUpdate().getBoolean(MemFlags.MEMORY_KEY_FORCE_AUTOFIT_ON_NO_AUTOFIT_SHIPS);
        int memberIndex = 0;
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {

            if (!forceAutofit && member.getHullSpec().hasTag(Items.TAG_NO_AUTOFIT)) {
                continue;
            }
            if (!forceAutofit && member.getVariant() != null && member.getVariant().hasTag(Items.TAG_NO_AUTOFIT)) {
                continue;
            }

            // need this so that when reinflating a fleet that lost members, the members reinflate consistently
            if (p.seed != null) {
                int extra = member.getShipName().hashCode();
                random = new Random(p.seed * extra);
                auto.setRandom(random);
                cyberAuto.setRandom(random);
                auxAuto.setRandom(random);
                dmodRandom = Misc.getRandom(p.seed * extra, 5);
            }

            List<WeaponSize> sizes = new ArrayList<>();
            sizes.add(WeaponSize.SMALL);
            sizes.add(WeaponSize.MEDIUM);
            sizes.add(WeaponSize.LARGE);

            weapons = new ArrayList<>();
            for (String cat : weaponCategories) {
                for (WeaponSize size : sizes) {
                    boolean foundSome = false;
                    for (int tier = 0; tier < 4; tier++) {
                        float p = getTierProbability(tier, this.p.quality);
                        if (this.p.allWeapons != null && this.p.allWeapons) {
                            p = 1f;
                        }

                        List<AutofitPlugin.AvailableWeapon> priority = priorityWeapons.getWeapons(tier).getWeapons(cat).getWeapons(size);
                        List<AutofitPlugin.AvailableWeapon> nonPriority = nonPriorityWeapons.getWeapons(tier).getWeapons(cat).getWeapons(size);

                        if (!foundSome) {
                            p = 1f;
                        }

                        boolean tierAvailable = random.nextFloat() < p;
                        if (!tierAvailable) continue;
                        //if (random.nextFloat() >= p) continue;

                        int num = 2;

                        // get a better selection of smaller weapons compared to normal autofit
                        switch (size) {
                            case MEDIUM:
                                num += 1;
                                break;
                            case SMALL:
                                num += 2;
                                break;
                        }
//						if (!tierAvailable) {
//							num = 1;
//						}

                        if (this.p.allWeapons != null && this.p.allWeapons) {
                            num = 500;
                        }

                        Set<Integer> picks = makePicks(num, priority.size(), random);
                        for (Integer index : picks) {
                            AutofitPlugin.AvailableWeapon w = priority.get(index);
                            weapons.add(w);
                            foundSome = true;
                        }

                        num -= picks.size();
                        if (num > 0) {
                            picks = makePicks(num, nonPriority.size(), random);
                            for (Integer index : picks) {
                                AutofitPlugin.AvailableWeapon w = nonPriority.get(index);
                                weapons.add(w);
                                foundSome = true;
                            }
                        }
                    }
                }
            }

            fighters = new ArrayList<>();
            for (String cat : fighterCategories) {
                List<AvailableFighter> priority = priorityFighters.get(cat);

                boolean madePriorityPicks = false;
                if (priority != null) {
                    int num = random.nextInt(2) + 1;
                    if (this.p.allWeapons != null && this.p.allWeapons) {
                        num = 100;
                    }

                    Set<Integer> picks = makePicks(num, priority.size(), random);
                    for (Integer index : picks) {
                        AvailableFighter f = priority.get(index);
                        fighters.add(f);
                        madePriorityPicks = true;
                    }
                }

                if (!madePriorityPicks) {
                    int num = random.nextInt(2) + 1;
                    if (this.p.allWeapons != null && this.p.allWeapons) {
                        num = 100;
                    }


                    List<AvailableFighter> nonPriority = nonPriorityFighters.get(cat);
                    Set<Integer> picks = makePicks(num, nonPriority.size(), random);
                    for (Integer index : picks) {
                        AvailableFighter f = nonPriority.get(index);
                        fighters.add(f);
                    }
                }
            }


            ShipVariantAPI target = member.getVariant();
            if (target.getOriginalVariant() != null) {
                // needed if inflating the same fleet repeatedly to pick up weapon availability changes etc
                target = Global.getSettings().getVariant(target.getOriginalVariant());
            }

            currVariant = Global.getSettings().createEmptyVariant(fleet.getId() + "_" + memberIndex, target.getHullSpec());
            currMember = member;

            if (target.isStockVariant()) {
                currVariant.setOriginalVariant(target.getHullVariantId());
            }

            float rProb = faction.getDoctrine().getAutofitRandomizeProbability();
            if (p.rProb != null) rProb = p.rProb;
            boolean randomize = random.nextFloat() < rProb;
            if (member.isStation()) randomize = false;
            auto.setChecked(CoreAutofitPlugin.RANDOMIZE, randomize);

            cyberAuto.setChecked(CoreAutofitPlugin.RANDOMIZE, false);
            auxAuto.setChecked(CoreAutofitPlugin.RANDOMIZE, false);

            memberIndex++;

            int maxSmods = 0;
            if (p.averageSMods != null && !member.isCivilian()) {
                maxSmods = getMaxSMods(currVariant, p.averageSMods, dmodRandom) - currVariant.getSMods().size();
            }
            // gimme smods lol
            if (member.getCaptain() != null) {
                cyberAuto.doFit(currVariant, target, Math.min(maxSmods + 1, 3), this);
            } else if (!isAuxiliary) {
                auto.doFit(currVariant, target, maxSmods, this);
            } else {
                // no smods on auxiliaries, even if the regulars have them
                auxAuto.doFit(currVariant, target, 0, this);
            }
            currVariant.setSource(VariantSource.REFIT);
            member.setVariant(currVariant, false, false);

            if (!currMember.isStation()) {
                float averageDmodsToAdd = averageDmods;
                // flagship is generally a bit more pristine
                if (member.isFlagship()) {
                    averageDmodsToAdd -= 1f;
                }
                // auxiliaries are in worse states
                if (isAuxiliary) {
                    averageDmodsToAdd += 2f;
                }
                int addDmods = getNumDModsToAdd(currVariant, averageDmodsToAdd, dmodRandom);
                if (addDmods > 0) {
                    DModManager.setDHull(currVariant);
                    DModManager.addDMods(member, true, addDmods, dmodRandom);
                }
            }
        }


        fleet.getFleetData().setSyncNeeded();
        fleet.getFleetData().syncIfNeeded();

    }

    public static int getNumDModsToAdd(ShipVariantAPI variant, float averageDMods, Random random) {
        int dmods = (int) Math.round(averageDMods + random.nextDouble() * 3f - 2f);
        if (dmods > 5) dmods = 5;
        int dmodsAlready = DModManager.getNumDMods(variant);
        dmods -= dmodsAlready;

        return Math.max(0, dmods);
    }

    public static int getMaxSMods(ShipVariantAPI variant, int averageSMods, Random random) {
        float f = random.nextFloat();
        int sMods = averageSMods;
        if (f < 0.25f) {
            sMods = averageSMods - 1;
        } else if (f < 0.5f) {
            sMods = averageSMods + 1;
        }
        if (sMods > 3) sMods = 3;
        if (sMods < 0) sMods = 0;
        return sMods;
    }

    public static float getAverageDmodsForQuality(float quality) {
        return (1f - quality) / Global.getSettings().getFloat("qualityPerDMod");
    }

    public static Set<Integer> makePicks(int num, int max, Random random) {
        if (num > max) num = max;
        Set<Integer> result = new LinkedHashSet<Integer>();
        if (num == 0) return result;

        if (num == max) {
            for (int i = 0; i < max; i++) {
                result.add(i);
            }
            return result;
        }

        while (result.size() < num) {
            int add = random.nextInt(max);
            result.add(add);
        }

        return result;
    }


    public boolean removeAfterInflating() {
        return p.persistent == null || !p.persistent;
    }

    public void setRemoveAfterInflating(boolean removeAfterInflating) {
        p.persistent = !removeAfterInflating;
        if (!p.persistent) p.persistent = null;
    }

    public void clearFighterSlot(int index, ShipVariantAPI variant) {
        variant.setWingId(index, null);
        for (AutofitPlugin.AvailableFighter curr : fighters) {
            if (curr.getId().equals(curr.getId())) {
                curr.setQuantity(curr.getQuantity() + 1);
                break;
            }
        }
    }

    public void clearWeaponSlot(WeaponSlotAPI slot, ShipVariantAPI variant) {
        variant.clearSlot(slot.getId());
        for (AutofitPlugin.AvailableWeapon curr : weapons) {
            if (curr.getId().equals(curr.getId())) {
                curr.setQuantity(curr.getQuantity() + 1);
                break;
            }
        }
    }

    public void fitFighterInSlot(int index, AutofitPlugin.AvailableFighter fighter, ShipVariantAPI variant) {
        fighter.setQuantity(fighter.getQuantity() - 1);
        variant.setWingId(index, fighter.getId());
    }

    public void fitWeaponInSlot(WeaponSlotAPI slot, AutofitPlugin.AvailableWeapon weapon, ShipVariantAPI variant) {
        weapon.setQuantity(weapon.getQuantity() - 1);
        variant.addWeapon(slot.getId(), weapon.getId());
    }

    public List<AutofitPlugin.AvailableFighter> getAvailableFighters() {
        return fighters;
    }

    public List<AutofitPlugin.AvailableWeapon> getAvailableWeapons() {
        return weapons;
    }

    public List<String> getAvailableHullmods() {
        return hullmods;
    }

    public ShipAPI getShip() {
        return null;
    }

    public void syncUIWithVariant(ShipVariantAPI variant) {

    }

    public boolean isPriority(WeaponSpecAPI weapon) {
        return faction.isWeaponPriority(weapon.getWeaponId());
    }

    public boolean isPriority(FighterWingSpecAPI wing) {
        return faction.isFighterPriority(wing.getId());
    }

    public FactionAPI getFaction() {
        return faction;
    }

    public float getQuality() {
        return p.quality;
    }
    public void setQuality(float quality) {
        this.p.quality = quality;
    }

    public Object getParams() {
        return p;
    }


    public boolean canAddRemoveHullmodInPlayerCampaignRefit(String modId) {
        return true;
    }

    public boolean isPlayerCampaignRefit() {
        return false;
    }


    public boolean isAllowSlightRandomization() {
        return true;
    }

}
*/
