package technology.rocketjump.undermount.ui.widgets;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.LocationComponent;
import technology.rocketjump.undermount.rendering.RenderMode;
import technology.rocketjump.undermount.rendering.entities.EntityRenderer;

import static technology.rocketjump.undermount.rendering.entities.EntityRenderer.PIXELS_PER_TILE;

public class EntityDrawable extends BaseDrawable {

    private final Entity entity;
    private final EntityRenderer entityRenderer;

    public EntityDrawable(Entity entity, EntityRenderer entityRenderer) {
        this.entity = entity;
        this.entityRenderer = entityRenderer;

        setMinWidth(PIXELS_PER_TILE * 1.2f);
        setMinHeight(PIXELS_PER_TILE * 1.2f);
    }

    @Override
    public void draw (Batch batch, float x, float y, float width, float height) {
        LocationComponent originalLocationComponent = entity.getLocationComponent();
        LocationComponent overrideLocationComponent = originalLocationComponent.clone(null, null);
        Vector2 screenPosition = new Vector2(x + (width / 2), y + (width / 2));
        overrideLocationComponent.setWorldPosition(screenPosition, false);
        entity.setLocationComponent(overrideLocationComponent);

        float tempPixelsPerTile = PIXELS_PER_TILE;
        PIXELS_PER_TILE = 1f;

        entityRenderer.render(entity, batch, RenderMode.DIFFUSE, null, null, null);

        PIXELS_PER_TILE = tempPixelsPerTile;

        entity.setLocationComponent(originalLocationComponent);
    }
}
