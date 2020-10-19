package technology.rocketjump.undermount.ui.widgets;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.rendering.entities.EntityRenderer;

@Singleton
public class EntityDrawableFactory {

    private final EntityRenderer entityRenderer;

    @Inject
    public EntityDrawableFactory(EntityRenderer entityRenderer) {
        this.entityRenderer = entityRenderer;
    }

    public EntityDrawable create(Entity entity) {
        return new EntityDrawable(entity, entityRenderer);
    }

}
