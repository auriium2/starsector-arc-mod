package data.missions;


import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FactionAPI.ShipPickMode;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflater;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflaterParams;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;
import com.fs.starfarer.api.plugins.AutofitPlugin;
import com.fs.starfarer.api.plugins.impl.CoreAutofitPlugin;
import com.fs.starfarer.api.util.Misc;
import data.missions.MS_MissionStations.AvailableStation;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Vector2f;

import java.util.*;

//TODO stolen from shadowyards, which seems stolen from xhan
public class BaseRandomARCMission implements MissionDefinitionPlugin {

    public static final Comparator<FleetMemberAPI> PRIORITY = new Comparator<FleetMemberAPI>()
    {
        // -1 means member1 is first, 1 means member2 is first
        @Override
        public int compare(FleetMemberAPI member1, FleetMemberAPI member2)
        {
            if (!member1.isCivilian())
            {
                if (member2.isCivilian())
                {
                    return -1;
                }
            }
            else if (!member2.isCivilian())
            {
                return 1;
            }

            int sizeCompare = member2.getHullSpec().getHullSize().
                    compareTo(member1.getHullSpec().getHullSize());
            if (sizeCompare != 0)
            {
                return sizeCompare;
            }

            if (member1.getFleetPointCost() > member2.getFleetPointCost())
            {
                return -1;
            }
            else if (member1.getFleetPointCost() < member2.getFleetPointCost())
            {
                return 1;
            }

            return MathUtils.getRandomNumberInRange(-1, 1);
        }
    };

    public static final float STATION_CHANCE = 0f;
    public static final float STATION_CHANCE_PLAYER = 0.4f; // Decides whether the station should be on player or enemy side

    // Types of objectives that may be randomly used
    private static final String[] OBJECTIVE_TYPES =
            {
                    "sensor_array", "nav_buoy", "comm_relay"
            };
    private static final Map<String, Float> QUALITY_FACTORS = new HashMap<>(13);


    // This is to force specific (Domain Drones and Remnants are handled elsewhere) hidden from intel factions to be available in Test Mode
    protected static final Set<String> FORCE_AVAILABLE_IN_TEST = new HashSet<>(Arrays.asList("OCI",
            "uaf",
            "derelict",
            "omega",
            "blade_breakers",
            "draco",
            "enigma",
            "remnant",
            "armaarmatura",

            "sephira_conclave",
            "sephira_nightravens",
            "arc",
            "fang",
            "mess",
            "junk_pirates_technicians"
    ));

    // Variant Test Mode hulls will be listed in exactly this order
    protected static final List<String> TEST_HULLS = new ArrayList<>(Arrays.asList("arc_malkuth",
            "arc_daat",
            "arc_gevurah",
            "arc_netzach"
            //"fake_hull_lol" // You know, to test if the error message works
    ));
    protected static final List<String> TEST_VARIANTS = new ArrayList<>();
    protected static final List<String> MISSING_HULLS = new ArrayList<>();

    static
    {
        QUALITY_FACTORS.put("sephira_conclave", 0.5f);
        QUALITY_FACTORS.put("sephira_nightravens", 0.5f);
        QUALITY_FACTORS.put("armaarmatura", 0.5f);
        QUALITY_FACTORS.put("ii", 0.5f);
        QUALITY_FACTORS.put("uaf", 0.5f);
        QUALITY_FACTORS.put("enigma", 0.8f);
        QUALITY_FACTORS.put("magellan_protectorate", 0.5f);
        QUALITY_FACTORS.put("arc", 0.6f); //super high tech, but scattered among the stars
        QUALITY_FACTORS.put("default", 0.5f);
        QUALITY_FACTORS.put("shadow_industry", 0.65f);      // Pre-collapse organization that is well equipped
        QUALITY_FACTORS.put(Factions.DERELICT, 0f);         // Old and worn out von Neumann probes that are are very poorly equipped
        QUALITY_FACTORS.put(Factions.DIKTAT, 0.5f);         // Bog standard dictatorship with average gear
        QUALITY_FACTORS.put(Factions.HEGEMONY, 0.5f);       // Comsec approved average gear
        QUALITY_FACTORS.put(Factions.LIONS_GUARD, 0.75f);   // Elite subdivision of the Diktat with above average gear
        QUALITY_FACTORS.put(Factions.LUDDIC_PATH, 0f);      // Fanatics who are very poorly equipped
        QUALITY_FACTORS.put(Factions.PERSEAN, 0.55f);       // Space NATO has slightly above average gear
        QUALITY_FACTORS.put(Factions.PIRATES, 0f);          // Criminals who are very poorly equipped
        QUALITY_FACTORS.put(Factions.REMNANTS, 1f);         // Are you Omega? Top of the line gear baby
        QUALITY_FACTORS.put(Factions.TRITACHYON, 0.85f);    // Mega-corp with high-quality gear
        QUALITY_FACTORS.put("blackrock_driveyards", 0.75f); // Esoteric tech-lords with above average gear
        QUALITY_FACTORS.put("diableavionics", 0.75f);       // Slavers with mysterious backers that posses above average gear
        QUALITY_FACTORS.put("exigency", 1f);                // Stay out from under foot or be stepped on
        QUALITY_FACTORS.put("exipirated", 0.55f);           // These pirates have some remarkable technology...
        QUALITY_FACTORS.put("interstellarimperium", 0.6f);  // Well equipped and well disciplined
        QUALITY_FACTORS.put("junk_pirates", 0.45f);         // Janky ships and weapons that are surprisingly effective
        QUALITY_FACTORS.put("pack", 0.5f);                  // Isolationists with effective and unique gear
        QUALITY_FACTORS.put("syndicate_asp", 0.5f);         // Space FedEx is well funded and well armed
        QUALITY_FACTORS.put("templars", 1f);                // What, did aliens give them this shit?
        QUALITY_FACTORS.put("ORA", 0.75f);                  // They found a hell of a cache of ships and weapons
        QUALITY_FACTORS.put("SCY", 0.55f);                  // Well equipped spies and tech-hoarders
        QUALITY_FACTORS.put("tiandong", 0.55f);             // Refits tend to be made with care and have slightly above average gear
        QUALITY_FACTORS.put("Coalition", 0.65f);            // Well entrenched and equipped coalition
        QUALITY_FACTORS.put("dassault_mikoyan", 0.75f);     // Mega-corp with above average gear
        QUALITY_FACTORS.put("6eme_bureau", 0.85f);          // Elite subdivision of DME with high-quality gear
        QUALITY_FACTORS.put("blade_breakers", 1f);          // Jesus, who developed this tech?
        QUALITY_FACTORS.put("OCI", 0.75f);                  // Anyone who traveled as far as they have is well equipped
        QUALITY_FACTORS.put("al_ars", 0.5f);                // The average of their ships tend to be middle of the road
        QUALITY_FACTORS.put("gmda", 0.5f);                  // Space Police with average gear
        QUALITY_FACTORS.put("draco", 0.55f);                // Space Vampire pirates with slightly enhanced tech
        QUALITY_FACTORS.put("fang", 0.5f);                  // Psycho Werewolves with average gear
        QUALITY_FACTORS.put("HMI", 0.5f);                   // Miners and "legitimate" pirates with average gear
        QUALITY_FACTORS.put("mess", 0.9f);                  // Gray goo enhanced ships and weapons
        QUALITY_FACTORS.put("sylphon", 0.75f);              // AI collaborators with advanced tech
        QUALITY_FACTORS.put("fob", 0.8f);                   // Aliens with... Alien tech
    }
    // This complicated code lets us add variants in a specified sequence of hull IDs, in an optimized manner
    protected static boolean testVariantsLoaded = false;

    protected static void loadTestVariants()
    {
        List<String> allVariants = Global.getSettings().getAllVariantIds();
        Map<String, List<String>> hullsToVariants = new HashMap<>();
        Collections.sort(allVariants);

        // Generate map entry for each test hull
        for (String hullId : TEST_HULLS)
        {
            hullsToVariants.put(hullId, new ArrayList<String>());
        }

        // Get hull of each variant, save the ones we need
        for (String variantId : allVariants)
        {
            //log.info("Checking variant " + variantId);
            if (variantId.endsWith("_Hull"))
            {
                continue;
            }
            String hullId = Global.getSettings().getVariant(variantId).getHullSpec().getHullId();
            if (TEST_HULLS.contains(hullId))    // This is a variant of one of our test hulls
            {
                //log.info("Adding variant " + variantId + " for hull " + hullId);
                hullsToVariants.get(hullId).add(variantId);
            }
        }

        // Add all variants for each hull
        for (String hullId : TEST_HULLS)
        {
            if (hullsToVariants.get(hullId).isEmpty())
            {
                //log.info("Hull ID " + hullId + " missing variants");
                MISSING_HULLS.add(hullId);
            }
            else
            {
                TEST_VARIANTS.addAll(hullsToVariants.get(hullId));
            }
        }

        testVariantsLoaded = true;
    }

    // True enables fighting against all show-in-intel-tab factions, not just the ones listed above
    protected static boolean testMode = false;
    // If true, ship variants are randomized like in the campaign
    protected static boolean randomMode = false;
    // Adds admiral AI to player side
    protected static boolean useAdmiralAI = true;
    // If false, enemy gets no point advantage over player
    protected static boolean useDifficultyModifier = true;
    // THI variant dump. Only works if variant test mode is also on
    protected static boolean variantTester = false;
    // Speeds up battle
    protected static boolean speedMode = false;

    private FactionAPI enemy;
    private FactionAPI player;
    private final Random rand = new Random();

    protected boolean isCtrlKeyPressed(int key)
    {
        return (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL))
                && Keyboard.isKeyDown(key);
    }

    @Override
    public void defineMission(MissionDefinitionAPI api) {
        // More factions hotkey combo
        // Checked when mission is clicked
        if (isCtrlKeyPressed(Keyboard.KEY_F))
        {
            testMode = !testMode;

            if (testMode)
            {
                Global.getSoundPlayer().playUISound("tiandong_ui_test_mode_on", 1f, 1f);
            }
            else
            {
                variantTester = false;
                Global.getSoundPlayer().playUISound("tiandong_ui_test_mode_off", 1f, 1f);
            }
        }

        // Random variant mode hotkey combo (does nothing if variant tester is on)
        // Doesn't work in Starsector 0.9.1, disabled for now
        /*
        if (isCtrlKeyPressed(Keyboard.KEY_R) && testMode && !variantTester)
        {
            randomMode = !randomMode;
            // TODO new sounds
            if (randomMode)
            {
                Global.getSoundPlayer().playUISound("tiandong_ui_test_mode_on", 1f, 1f);
            }
            else
            {
                Global.getSoundPlayer().playUISound("tiandong_ui_test_mode_off", 1f, 1f);
            }
        }
         */
        // Variant tester hotkey combo (does nothing if test mode is not currently on)
        if (isCtrlKeyPressed(Keyboard.KEY_V) && testMode)
        {
            variantTester = !variantTester;

            if (variantTester)
            {
                Global.getSoundPlayer().playUISound("tiandong_ui_test_ALL_ships_mode_on", 1f, 1f);
                randomMode = false;
            }
            else
            {
                Global.getSoundPlayer().playUISound("tiandong_ui_test_ALL_ships_mode_off", 1f, 1f);
            }
        }

        // Admiral AI toggle hotkey combo
        if (isCtrlKeyPressed(Keyboard.KEY_A))
        {
            useAdmiralAI = !useAdmiralAI;

            if (useAdmiralAI)
            {
                Global.getSoundPlayer().playUISound("tiandong_ui_admiral_AI_enabled", 1f, 1f);
            }
            else
            {
                Global.getSoundPlayer().playUISound("tiandong_ui_admiral_AI_disabled", 1f, 1f);
            }
        }

        // Difficulty toggle hotkey combo
        if (isCtrlKeyPressed(Keyboard.KEY_D))
        {
            useDifficultyModifier = !useDifficultyModifier;

            if (useDifficultyModifier)
            {
                Global.getSoundPlayer().playUISound("tiandong_ui_difficulty_enabled", 1f, 1f);
            }
            else
            {
                Global.getSoundPlayer().playUISound("tiandong_ui_difficulty_disabled", 1f, 1f);
            }
        }

        // Speed mode hotkey combo
        if (isCtrlKeyPressed(Keyboard.KEY_T))
        {
            speedMode = !speedMode;

            if (speedMode)
            {
                Global.getSoundPlayer().playUISound("tiandong_ui_fast_time_on", 1f, 1f);
            }
            else
            {
                Global.getSoundPlayer().playUISound("tiandong_ui_fast_time_off", 1f, 1f);
            }
        }

        // Amber nebulae
        api.setNebulaTex("graphics/terrain/nebula_amber.png");
        api.setNebulaMapTex("graphics/terrain/nebula_amber_map.png");

        if (player == null || enemy == null)
        {
            chooseFactions(null, null);
        }

        api.initFleet(FleetSide.PLAYER, "", FleetGoal.ATTACK, useAdmiralAI, 5);
        api.initFleet(FleetSide.ENEMY, "", FleetGoal.ATTACK, true, 5);

        api.setFleetTagline(FleetSide.PLAYER, Misc.ucFirst(player.getDisplayNameLong()) + " forces");
        api.setFleetTagline(FleetSide.ENEMY, Misc.ucFirst(enemy.getDisplayNameLong()) + " forces");

        // Fleet size randomization
        int size = 25 + (int) (Math.random() * 225);
        int sizeForMap = size;	// Not affected by station bonus
        float difficulty = 0.7f + rand.nextFloat() * 0.3f;
        if (!useDifficultyModifier)
        {
            difficulty = 1;
        }

        // Station stuff
        AvailableStation stationPlayer = null, stationEnemy = null;
        if (Math.random() < STATION_CHANCE)
        {
            // 0 = player, 1 = enemy, -1 = don't spawn a station
            int stationSide = Math.random() < STATION_CHANCE_PLAYER ? 0 : 1;
            if (stationSide == 0)
            {
                stationPlayer = MS_MissionStations.getStationForFaction(player.getId(), size * difficulty * 2);
            }
            else {
                stationEnemy = MS_MissionStations.getStationForFaction(enemy.getId(), size * 2);
            }
        }
        boolean hasStation = (stationPlayer != null || stationEnemy != null);

        // Add more FP to both sides to account for station presence, if appropriate
        float stationFPmult = MathUtils.getRandomNumberInRange(0.6f, 1f);
        if (stationPlayer != null)
        {
            size += stationPlayer.fpValue * stationFPmult;
        }
        else if (stationEnemy != null)
        {
            size += stationEnemy.fpValue * stationFPmult;
        }

        // You don't need a difficulty modifier when you're playing against ARC :)
        int playerFP;
        int enemyFP;
        if (player.getId().equals("arc")) {
            playerFP = generateFleet(player, api, FleetSide.PLAYER, (int) (size * 0.8), stationPlayer);
            enemyFP = generateFleet(enemy, api, FleetSide.ENEMY, (int)(size * difficulty * 1.2), stationEnemy);

        } else {
            playerFP = generateFleet(player, api, FleetSide.PLAYER, (int) (size * difficulty * 1.2), stationPlayer);
            enemyFP = generateFleet(enemy, api, FleetSide.ENEMY, (int)(size * 0.8), stationEnemy);
        }


        // Set up the map
        float width = 13000f + 13000f * (sizeForMap / 200);
        float height = 13000f + 13000f * (sizeForMap / 200);
        if (hasStation)
        {
            // No objectives, so don't make too big
            width = (float) Math.min(width, 18000f);
            height = (float) Math.min(height, 18000f);
        }
        api.initMap(-width / 2f, width / 2f, -height / 2f, height / 2f);

        float minX = -width / 2;
        float minY = -height / 2;

        int objectiveCount = (int) Math.floor(sizeForMap / 40f);
        if (hasStation)
        {
            objectiveCount = 0; // Objectives don't spawn in station battles, right?
        }
        List<Vector2f> objectives = new ArrayList<>();

        while (objectiveCount > 0)
        {
            String type = OBJECTIVE_TYPES[rand.nextInt(3)];

            if (objectiveCount == 1)
            {
                api.addObjective(0, 0, type);
                objectiveCount -= 1;
            }
            else
            {
                int tries = 0;
                while (true)
                {
                    boolean allow = true;
                    tries++;
                    if (tries >= 15)
                    {
                        objectiveCount = 0;   // Screw this, we're outta here
                        break;
                    }

                    float theta = (float) (Math.random() * Math.PI);
                    double radius = Math.min(width, height);
                    radius = radius * 0.1 + radius * 0.3 * Math.random();
                    int x = (int) (Math.cos(theta) * radius);
                    int y = (int) (Math.sin(theta) * radius);

                    // Check for distance to existing objectives
                    Vector2f pos = new Vector2f(x, -y);
                    for (Vector2f existing : objectives)
                    {
                        if (MathUtils.isWithinRange(existing, pos, 1500))
                        {
                            allow = false;
                            break;
                        }
                    }
                    if (allow)
                    {
                        api.addObjective(x, -y, type);
                        api.addObjective(-x, y, type);
                        objectives.add(pos);
                        objectiveCount -= 2;
                        break;
                    }
                }
            }
        }

        // If the player faction or enemy faction is Tiandong, there is a 50% chance of planets being added
        if (Math.random() < 0.5 && (player.getId().contentEquals("tiandong") || enemy.getId().contentEquals("tiandong")))
        {
            // The possible planet backgrounds
            String[] planets =
                    {
                            "tiandong_shaanxi", "tiandong_gan", "tiandong_tiexiu", "tiandong_zaolei"
                    };
            String planet = planets[(int) (Math.random() * planets.length)];

            float radius = 0;

            // If the planet is 'Shaanxi', set the radius to...
            if (planet.contentEquals("tiandong_shaanxi"))
            {
                radius = 250f;
            }
            // If the planet is 'Gan', set the radius to...
            if (planet.contentEquals("tiandong_gan"))
            {
                radius = 125f;
            }
            // If the planet is 'Tiexiu', set the radius to...
            if (planet.contentEquals("tiandong_tiexiu"))
            {
                radius = 100f;
            }
            // If the planet is 'Zaolei', set the radius to...
            if (planet.contentEquals("tiandong_zaolei"))
            {
                radius = 80f;
            }
            api.addPlanet(0, 0, radius, planet, 200f, true);
        }

        // Show the factions versus and their FP
        if (!(testMode && variantTester))
        {
            String str = player.getDisplayName() + "  (" + playerFP + ")   vs.  " + enemy.getDisplayName() + "  (" + enemyFP + ")";
            if (hasStation)
            {
                //str += " || Attempting station";      // Debug message, comment out later
            }
            if (randomMode)
            {
                str += " || Randomized variants";
            }
            api.addBriefingItem(str);
        }
        else
        {
            api.addBriefingItem("VARIANT TESTING MODE: Listing all variants of THI hulls");
        }

        String asteroidAndNebulaLine;

        // Chance of generating a nebula
        float nebulaChance = MathUtils.getRandomNumberInRange(0, 100);

        // So basically half the time (if less than 50 out of 100)
        if (nebulaChance < 50)
        {
            // Do regular nebula generation
            float nebulaCount = 10 + (float) Math.random() * 30;
            float nebulaSize = (float) Math.random();

            for (int i = 0; i < nebulaCount; i++)
            {
                float x = (float) Math.random() * width - width / 2;
                float y = (float) Math.random() * height - height / 2;
                float nebulaRadius = (400f + (float) Math.random() * 1600f) * nebulaSize;
                api.addNebula(x, y, nebulaRadius);
            }

            asteroidAndNebulaLine = "Nebulosity:  " + (int) (((nebulaCount * nebulaSize) / 40f) * 100) + "%";
        }
        else
        {
            // Mention that there is no nebula, this line could be commented out if you don't want this line item added
            asteroidAndNebulaLine = "Nebulosity: N/A";
        }

        // Asteroid generation random chance
        float asteroidChance = MathUtils.getRandomNumberInRange(0, 100);

        // If chance is less than 50
        if (asteroidChance < 50)
        {
            // Do regular asteroid generation
            // Minimum asteroid speed between 15 and 50
            int minAsteroidSpeed = MathUtils.getRandomNumberInRange(15, 50);

            // Asteroid count
            int asteroidCount = sizeForMap + (int) (sizeForMap * 4 * Math.pow(Math.random(), 2));

            // Add the asteroid field
            api.addAsteroidField(
                    minX + width * 0.5f, // X
                    minY + height * 0.5f, // Y
                    rand.nextInt(90) - 45 + (rand.nextInt() % 2) * 180, // Angle
                    width * 0.25f + ((float) Math.random() * height / 2), // Width
                    minAsteroidSpeed, // Min speed
                    minAsteroidSpeed * 1.1f, // Max speed
                    asteroidCount); // Count

            asteroidAndNebulaLine += "  |  Asteroids:  " + (int) ((asteroidCount / 1000f) * 100) + "% density; speed " + minAsteroidSpeed;
        }
        else
        {
            // If not asteroid field, specify as N/A, you can comment this out
            asteroidAndNebulaLine += "  |  Asteroids: N/A";
        }
        api.addBriefingItem(asteroidAndNebulaLine);

        if (testMode)
        {
            if (!variantTester)
            {
                api.addBriefingItem("TEST MODE enabled: Other mod factions now available and THI can now infight");
            }
            else if (!MISSING_HULLS.isEmpty())
            {
                // Print missing hulls
                StringBuilder sb = new StringBuilder();
                for (String s : MISSING_HULLS)
                {
                    sb.append(s);
                    sb.append(";");
                }
                api.addBriefingItem("WARNING: Test hulls with no variants specified: " + sb.toString());
            }
        }

        boolean showLine4 = (!useDifficultyModifier && !variantTester) || useAdmiralAI || speedMode;
        if (showLine4)
        {
            List<String> strings = new ArrayList<>();

            if (!useDifficultyModifier && !variantTester)
            {
                strings.add("Difficulty modifier disabled");
            }
            if (useAdmiralAI)
            {
                strings.add("Player admiral AI enabled");
            }
            if (speedMode)
            {
                strings.add("Speed mode enabled");
            }

            String str = "";
            for (int i = 0; i < strings.size(); i++)
            {
                str += strings.get(i);
                if (i < strings.size() - 1)  // Not last item
                {
                    str += "  |  ";
                }
            }
            api.addBriefingItem(str);
        }

        if (speedMode)
        {
            api.addPlugin(new BaseEveryFrameCombatPlugin()
            {
                @Override
                public void advance(float amount, List<InputEventAPI> events)
                {
                    if (Global.getCombatEngine().isPaused())
                    {
                        return;
                    }

                    float trueFrameTime = Global.getCombatEngine().getElapsedInLastFrame();
                    float trueFPS = 1 / trueFrameTime;
                    float newTimeMult = Math.max(1f, trueFPS / 30f);
                    Global.getCombatEngine().getTimeMult().modifyMult("tiandong_tester", newTimeMult);
                }
            });
        }
    }

    protected float getQuality(FactionAPI faction)
    {
        String id = faction.getId();

        if (QUALITY_FACTORS.containsKey(id))
        {
            return QUALITY_FACTORS.get(id);
        }

        return QUALITY_FACTORS.get("default");
    }

    protected void chooseFactions(String playerFactionId, String enemyFactionId)
    {
        if (testMode && variantTester)
        {
            player = Global.getSettings().createBaseFaction("arc");
            enemy = Global.getSettings().createBaseFaction("arc");
            return;
        }

        if (playerFactionId != null)
        {
            player = Global.getSettings().createBaseFaction(playerFactionId);
        }
        if (enemyFactionId != null)
        {
            enemy = Global.getSettings().createBaseFaction(enemyFactionId);
        }

        List<FactionAPI> acceptableFactions = new ArrayList<>(11);

        // Always use the vanilla factions
        acceptableFactions.add(Global.getSettings().createBaseFaction(Factions.DIKTAT));
        acceptableFactions.add(Global.getSettings().createBaseFaction(Factions.HEGEMONY));
        acceptableFactions.add(Global.getSettings().createBaseFaction(Factions.INDEPENDENT));
        acceptableFactions.add(Global.getSettings().createBaseFaction(Factions.LIONS_GUARD));
        acceptableFactions.add(Global.getSettings().createBaseFaction(Factions.LUDDIC_CHURCH));
        acceptableFactions.add(Global.getSettings().createBaseFaction(Factions.LUDDIC_PATH));
        acceptableFactions.add(Global.getSettings().createBaseFaction(Factions.PERSEAN));
        acceptableFactions.add(Global.getSettings().createBaseFaction(Factions.PIRATES));
        acceptableFactions.add(Global.getSettings().createBaseFaction(Factions.TRITACHYON));

        /*if (testMode || tiandong_PersistentUnlocker.knowDerelicts)
        {
            acceptableFactions.add(Global.getSettings().createBaseFaction(Factions.DERELICT));
        }
        if (testMode || tiandong_PersistentUnlocker.knowRemnants)
        {
            acceptableFactions.add(Global.getSettings().createBaseFaction(Factions.REMNANTS));
        }*/

        for (FactionAPI faction : Global.getSector().getAllFactions())
        {
            // Don't add duplicate entries
            if (acceptableFactions.contains(faction))
            {
                continue;
            }

            if (!faction.isShowInIntelTab() && !FORCE_AVAILABLE_IN_TEST.contains(faction.getId()))
            {
                continue;
            }
            acceptableFactions.add(Global.getSettings().createBaseFaction(faction.getId()));
        }

        // If the player faction is not null, and is not specified a parameter in the input, choose from one of the acceptable factions
        // Could be a WeightedRandomPicker instead but bah
        player = player != null ? player : acceptableFactions.get(rand.nextInt(acceptableFactions.size()));

        // Ditto for enemies
        enemy = enemy != null ? enemy : acceptableFactions.get(rand.nextInt(acceptableFactions.size()));
    }

    protected void generateVariantTestFleet(MissionDefinitionAPI api, FleetSide side)
    {
        if (!testVariantsLoaded)
        {
            loadTestVariants();
        }
        boolean first = true;
        for (String variantId : TEST_VARIANTS)
        {
            api.addToFleet(side, variantId, FleetMemberType.SHIP, first);
            first = false;
        }
    }

    // Generate a fleet from the campaign fleet generator
    protected int generateFleet(FactionAPI faction, MissionDefinitionAPI api, FleetSide side, int fp, AvailableStation station)
    {
        String factionId = faction.getId();

        if (testMode && variantTester)
        {
            generateVariantTestFleet(api, side);
            return 0;
        }

        FleetMemberAPI stationMember = null;
        if (station != null)
        {
            stationMember = api.addToFleet(side, station.variantId, FleetMemberType.SHIP, faction.pickRandomShipName(), false);
            // Remove modules (e.g. for pirate stations)
            float removeMult = MS_MissionStations.getModuleRemovalChanceForFaction(factionId);
            if (removeMult > 0)
            {
                MS_MissionStations.removeModulesFromStation(stationMember, removeMult);
            }
            fp -= station.fpValue;
        }

        // The station ate all our FP, nothing more to do
        if (fp <= 0)
        {
            return (stationMember != null ? stationMember.getFleetPointCost() : 0);
        }

        float quality = getQuality(faction);
        FleetParamsV3 params = new FleetParamsV3(
                null, // LocInHyper
                factionId,
                quality,
                FleetTypes.PATROL_LARGE,
                fp, // CombatPts
                0f, // FreighterPts
                0f, // TankerPts
                0f, // TransportPts
                0f, // LinerPts
                0f, // UtilityPts
                0f // QualityMod
        );
        params.modeOverride = ShipPickMode.PRIORITY_THEN_ALL;

        CampaignFleetAPI fleet = FleetFactoryV3.createFleet(params);

        List<FleetMemberAPI> fleetList = new ArrayList<>(fleet.getFleetData().getMembersListCopy());
        Collections.sort(fleetList, PRIORITY);

        // Debugging autofit
        /*
		fleetList.clear();
		for (int i=0; i<15; i++) {
			fleetList.add(Global.getFactory().createFleetMember(FleetMemberType.SHIP, "tiandong_dingjun_Firesupport"));
		}
         */
        // Randomization stuff
        AutofitPlugin.AutofitPluginDelegate inflater = null;
        CoreAutofitPlugin auto = null;
        Random random = new Random();

        // Prepare variant randomizer
        if (randomMode)
        {
            //inflater = new tiandong_MissionAutofit(faction);

            DefaultFleetInflaterParams dfip = new DefaultFleetInflaterParams();
            dfip.allWeapons = false;
            dfip.factionId = faction.getId();
            dfip.mode = ShipPickMode.PRIORITY_THEN_ALL;
            dfip.persistent = false;
            dfip.quality = quality;

            /*boolean isTiandongFaction = factionId.equals("tiandong");
            boolean useCustomAutofitPlugin = (isTiandongFaction && TiandongModPlugin.useCustomTiandongAutofit) /*|| (!isTiandongFaction && TiandongModPlugin.useCustomNonTiandongAutofit && TiandongAutofitDefs.doesFactionHaveDef(factionId))*/;

            /*if (useCustomAutofitPlugin)
            {
                inflater = new TiandongFleetInflater(dfip);
                ((TiandongFleetInflater) inflater).setFaction(faction);
                ((TiandongFleetInflater) inflater).inflate(fleet);
                auto = new TiandongNonFuckedAutofitPlugin(fleet.getCommander(), factionId);
            }
            else
            {*/
            inflater = new DefaultFleetInflater(dfip);
            ((DefaultFleetInflater) inflater).inflate(fleet);
            auto = new CoreAutofitPlugin(fleet.getCommander());
            //}

            auto.setRandom(random);
            auto.setChecked(CoreAutofitPlugin.UPGRADE, true);
            auto.setChecked(CoreAutofitPlugin.STRIP, true);
            auto.setChecked(CoreAutofitPlugin.RANDOMIZE, true);
        }

        boolean flagshipChosen = false;
        int index = 0;
        for (FleetMemberAPI baseMember : fleetList)
        {
            String variant;
            if (baseMember.isFighterWing())
            {
                variant = baseMember.getSpecId();
            }
            else if (randomMode && !baseMember.getVariant().isStockVariant())
            {
                variant = baseMember.getVariant().getOriginalVariant();
            }
            else
            {
                variant = baseMember.getVariant().getHullVariantId();
            }

            FleetMemberAPI member = api.addToFleet(side, variant, baseMember.getType(), baseMember.getShipName(),
                    (!baseMember.isFighterWing() && !flagshipChosen));

            // Apply randomizer if appropriate
            if (randomMode)
            {
                randomizeVariant(auto, inflater, member, fleet, faction, index, random);
            }

            if (!baseMember.isFighterWing() && !flagshipChosen)
            {
                flagshipChosen = true;
            }

            index++;
        }

        // Randomize station as well if needed
        if (randomMode && stationMember != null)
        {
            randomizeVariant(auto, inflater, stationMember, fleet, faction, 999, random);
        }

        int finalFP = fleet.getFleetPoints();
        if (stationMember != null)
        {
            finalFP += stationMember.getFleetPointCost();
        }
        return finalFP;
    }

    protected void randomizeVariant(CoreAutofitPlugin auto, AutofitPlugin.AutofitPluginDelegate inflater, FleetMemberAPI member,
                                    CampaignFleetAPI fleet, FactionAPI faction, int index, Random random)
    {
        if (member.getHullSpec().hasTag("no_autofit"))
        {
            return;
        }

        ShipVariantAPI currVariant = Global.getSettings().createEmptyVariant(fleet.getId() + "_" + index, member.getHullSpec());
        ShipVariantAPI target = member.getVariant();

        if (target.isStockVariant())
        {
            currVariant.setOriginalVariant(target.getHullVariantId());
        }

        boolean randomize = random.nextFloat() < faction.getDoctrine().getAutofitRandomizeProbability();
        auto.setChecked(CoreAutofitPlugin.RANDOMIZE, randomize);

        auto.doFit(currVariant, target, index, inflater);
        currVariant.setSource(VariantSource.REFIT);
        member.setVariant(currVariant, false, false);
    }
}
