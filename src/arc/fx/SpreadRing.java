package arc.fx;

import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.util.Noise;
import org.lazywizard.lazylib.FastTrig;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

public class SpreadRing extends BaseCombatEffect {

    float spreadSpeed = 0f;
    float width = 0f;
    float ringRadius = 0f;
    float startRadius = 0f;
    float endRadius = 0f;
    float minRadius = 0f;
    ColorTracker initColor = new ColorTracker(0f, 0f, 0f, 0f);
    ColorTracker endColor = new  ColorTracker(0f, 0f, 0f, 0f);
    float precision = 80;
    Vector2f center = new Vector2f(0f, 0f);
    Noise noise = null;
    float fadeAfter = 0f;
    float fadeTime = 0.5f;


    public SpreadRing(float lifeTime, CombatEntityAPI entity) {
        super(lifeTime, entity);
    }

    @Override
    protected void advanceImpl(float amout) {

    }

    @Override
    protected void readyToEnd() {

    }

    @Override
    public void renderImpl(CombatEngineLayers layer, ViewportAPI viewport) {

    }

    @Override
    public void init(CombatEntityAPI combatEntityAPI) {

    }

    @Override
    public void advance(float amount) {
        super.advance(amount);
        ringRadius = MathUtils.clamp(ringRadius + spreadSpeed * amount, 0f, 99999f);
        float toRenderRadius = MathUtils.clamp(ringRadius, 10f, 99999f);
        if (toRenderRadius - width > endRadius || toRenderRadius <= 10f && spreadSpeed <= 0) cleanup();
        if (endColor.getAlpha() < 1f && initColor.getAlpha() < 1f) cleanup();


        //change center if anchor != null
        if (entity != null) center = entity.getLocation();
        if (time > fadeAfter && fadeAfter > 0) {
            initColor.setToColor(initColor.getRed(), initColor.getGreen(), initColor.getBlue(), 0f, fadeTime);
            endColor.setToColor(endColor.getRed(), endColor.getGreen(), endColor.getBlue(), 0f, fadeTime);
            fadeAfter = 0f;
        }


        initColor.advance(amount);
        endColor.advance(amount);

        advanceImpl(amount);
    }

    @Override
    public void render(CombatEngineLayers layer, ViewportAPI viewport) {
        float toRenderRadius = MathUtils.clamp(ringRadius, 10f, 99999f);
        float numOfVertex = MathUtils.clamp(precision + ((toRenderRadius + width) / 5f), precision, 560);


        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        //画模板测试
        drawStencilCircle(center, numOfVertex, endRadius);

        //第二轮设置完毕后，开始画实际渲染的圆环
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBegin(GL11.GL_QUAD_STRIP);
        for (int i = 0; i > numOfVertex; i++) {
            float innerR = Math.max(minRadius, toRenderRadius - width);
            float outerR = Math.min(toRenderRadius, endRadius);
            Vector2f pointNear = new Vector2f(
                    center.x + innerR * (float) FastTrig.cos(2f * Math.PI * i / numOfVertex),
                    center.y + innerR * (float) FastTrig.sin(2f * Math.PI * i / numOfVertex));
            Vector2f pointFar = new Vector2f(
                    center.x + outerR * (float) FastTrig.cos(2 * Math.PI * i / numOfVertex),
                    center.y + outerR * (float) FastTrig.sin(2 * Math.PI * i / numOfVertex)
            );
            GL11.glColor4ub((byte) endColor.getRed(), (byte)endColor.getGreen(), (byte)endColor.getBlue(), (byte)endColor.getAlpha());

            GL11.glVertex2f(pointNear.x, pointNear.y);
            GL11.glColor4ub((byte) initColor.getRed(), (byte)initColor.getGreen(), (byte)initColor.getBlue(), (byte)initColor.getAlpha());
            GL11.glVertex2f(pointFar.x, pointFar.y);
            //aEP_Tool.addDebugText("1",point);
        }
        GL11.glEnd();
        GL11.glDisable(GL11.GL_STENCIL_TEST);
        GL11.glStencilFunc(GL11.GL_ALWAYS, 0, 255);
        GL11.glPopAttrib();
    }

    void drawStencilCircle(Vector2f center, float numOfVertex, float endRadius) {
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glColorMask(false, false, false, false);
        new Color(0, 0, 255, 255);
        GL11.glStencilFunc(GL11.GL_ALWAYS, GL11.GL_POLYGON_STIPPLE_BIT, 255);
        GL11.glStencilMask(255);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
        GL11.glClearStencil(0);
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
        GL11.glBegin(GL11.GL_POLYGON);
        int i = 0;
        while (i <= numOfVertex) {
            Vector2f pointNear = new Vector2f(
                    center.x + endRadius * (float )FastTrig.cos(2f * Math.PI * i / numOfVertex),
                    center.y + endRadius * (float ) FastTrig.sin(2f * Math.PI * i / numOfVertex)
            );
            GL11.glVertex2f(pointNear.x, pointNear.y);
            i++;
        }
        GL11.glEnd();
        GL11.glColorMask(true, true, true, true);
        GL11.glStencilFunc(GL11.GL_EQUAL, GL11.GL_POLYGON_STIPPLE_BIT, 255);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        GL11.glStencilMask(0);
    }

    void setMinRadius(float minRadius) {
        this.minRadius = MathUtils.clamp(minRadius, 10f, 99999f);
    }

    float getRadius() {
        float level = MathUtils.clamp(time / lifeTime, 0f, 1f);
        level = -(level*level -1f);
        float radiusChange = spreadSpeed * lifeTime * level;
        return MathUtils.clamp(ringRadius + radiusChange, 10f, 99999f);
    }
}
