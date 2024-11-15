package arc.campaign;

/*

public class BeaconEntityPlugin extends BaseCustomEntityPlugin {

    static String GLOW_COLOR_KEY = "$core_beaconGlowColor";
    static String PING_COLOR_KEY = "$core_beaconPingColor";

    static float GLOW_FREQUENCY = 1.2f; // on/off cycles per second

    SpriteAPI sprite;
    SpriteAPI glow;

    public void init(SectorEntityToken entity, Object pluginParams) {
        super.init(entity, pluginParams);
        //this.entity = entity;
        entity.setDetectionRangeDetailsOverrideMult(0.75f);
        readResolve();
    }

    Object readResolve() {
        sprite = Global.getSettings().getSprite("campaignEntities", "istl_breaker_beacon");
        glow = Global.getSettings().getSprite("campaignEntities", "istl_breaker_beacon_glow");
        return this;
    }

    private float phase = 0f;
    private float freqMult = 1f;
    private float sincePing = 10f;
    public void advance(float amount) {
        phase += amount * GLOW_FREQUENCY * freqMult;
        while (phase > 1) phase --;

        if (entity.isInCurrentLocation()) {
            sincePing += amount;
            if (sincePing >= 6f && phase > 0.1f && phase < 0.2f) {
                sincePing = 0f;
                CampaignFleetAPI playerFleet = Global.getSector().getPlayerFleet();
                if (playerFleet != null &&
                        entity.getVisibilityLevelTo(playerFleet) == SectorEntityToken.VisibilityLevel.COMPOSITION_AND_FACTION_DETAILS) {

                    String pingId = Pings.WARNING_BEACON1;
                    freqMult = 1f;
                    if (entity.getMemoryWithoutUpdate().getBoolean(BladeBreakerSystemType.SUPPRESSED.getBeaconFlag())) { //Change this later to point to Blade Breaker procgen
                        pingId = Pings.WARNING_BEACON2;
                        freqMult = 1.25f;
                    } else if (entity.getMemoryWithoutUpdate().getBoolean(BladeBreakerSystemType.RESURGENT.getBeaconFlag())) { //Change this later to point to Blade Breaker procgen
                        pingId = Pings.WARNING_BEACON3;
                        freqMult = 1.5f;
                    }

                    //Global.getSector().addPing(entity, pingId);

                    //Color pingColor = entity.getFaction().getBrightUIColor();
                    Color pingColor = new Color(75,255,175,255);
                    if (entity.getMemoryWithoutUpdate().contains(PING_COLOR_KEY)) {
                        pingColor = (Color) entity.getMemoryWithoutUpdate().get(PING_COLOR_KEY);
                    }

                    Global.getSector().addPing(entity, pingId, pingColor);
                }
            }
        }
    }

    public float getRenderRange() {
        return entity.getRadius() + 100f;
    }

    public void render(CampaignEngineLayers layer, ViewportAPI viewport) {
        float alphaMult = viewport.getAlphaMult();
        if (alphaMult <= 0f) return;

        CustomEntitySpecAPI spec = entity.getCustomEntitySpec();
        if (spec == null) return;

        float w = spec.getSpriteWidth();
        float h = spec.getSpriteHeight();

        Vector2f loc = entity.getLocation();

        sprite.setAngle(entity.getFacing() - 90f);
        sprite.setSize(w, h);
        sprite.setAlphaMult(alphaMult);
        sprite.setNormalBlend();
        sprite.renderAtCenter(loc.x, loc.y);


        float glowAlpha = 0f;
        if (phase < 0.5f) glowAlpha = phase * 2f;
        if (phase >= 0.5f) glowAlpha = (1f - (phase - 0.5f) * 2f);

        float glowAngle1 = (((phase * 1.3f) % 1) - 0.5f) * 12f;
        float glowAngle2 = (((phase * 1.9f) % 1) - 0.5f) * 12f;
//		glowAngle1 = 0f;
//		glowAngle2 = 0f;

        boolean glowAsLayer = true;
        //glow.setAngle(entity.getFacing() - 90f);
        Color glowColor = new Color(95, 166, 178,255);
        //Color glowColor = entity.getFaction().getBrightUIColor();
        if (entity.getMemoryWithoutUpdate().contains(GLOW_COLOR_KEY)) {
            glowColor = (Color) entity.getMemoryWithoutUpdate().get(GLOW_COLOR_KEY);
        }

        //glow.setColor(Color.white);
        glow.setColor(glowColor);

        glow.setSize(w, h);
        glow.setAlphaMult(alphaMult * glowAlpha);
        glow.setAdditiveBlend();

        glow.setAngle(entity.getFacing() - 90f + glowAngle1);
        glow.renderAtCenter(loc.x, loc.y);

        glow.setAngle(entity.getFacing() - 90f + glowAngle2);
        glow.setAlphaMult(alphaMult * glowAlpha * 0.5f);
        glow.renderAtCenter(loc.x, loc.y);
    }

    @Override
    public void createMapTooltip(TooltipMakerAPI tooltip, boolean expanded) {
        String post = "";
        Color color = entity.getFaction().getBaseUIColor();
        Color postColor = color;
        if (entity.getMemoryWithoutUpdate().getBoolean(BladeBreakerSystemType.DESTROYED.getBeaconFlag())) {
            post = " - Low";
            postColor = Misc.getPositiveHighlightColor();
        } else if (entity.getMemoryWithoutUpdate().getBoolean(BladeBreakerSystemType.SUPPRESSED.getBeaconFlag())) {
            post = " - Medium";
            postColor = Misc.getHighlightColor();
        } else if (entity.getMemoryWithoutUpdate().getBoolean(BladeBreakerSystemType.RESURGENT.getBeaconFlag())) {
            post = " - High";
            postColor = Misc.getNegativeHighlightColor();
        }

        tooltip.addPara(entity.getName() + post, 0f, color, postColor, post.replaceFirst(" - ", ""));
    }

    @Override
    public boolean hasCustomMapTooltip() {
        return true;
    }

    @Override
    public void appendToCampaignTooltip(TooltipMakerAPI tooltip, SectorEntityToken.VisibilityLevel level) {
        if (level == SectorEntityToken.VisibilityLevel.COMPOSITION_AND_FACTION_DETAILS ||
                level == SectorEntityToken.VisibilityLevel.COMPOSITION_DETAILS) {

            String post = "";
            Color color = Misc.getTextColor();
            Color postColor = color;
            if (entity.getMemoryWithoutUpdate().getBoolean(BladeBreakerSystemType.DESTROYED.getBeaconFlag())) {
                post = "low";
                postColor = Misc.getPositiveHighlightColor();
            } else if (entity.getMemoryWithoutUpdate().getBoolean(BladeBreakerSystemType.SUPPRESSED.getBeaconFlag())) {
                post = "medium";
                postColor = Misc.getHighlightColor();
            } else if (entity.getMemoryWithoutUpdate().getBoolean(BladeBreakerSystemType.RESURGENT.getBeaconFlag())) {
                post = "high";
                postColor = Misc.getNegativeHighlightColor();
            }
            if (!post.isEmpty()) {
                tooltip.setParaFontDefault();
                tooltip.addPara(BaseIntelPlugin.BULLET + "Danger level: " + post, 10f, color, postColor, post);
            }
        }

    }
}


*/
