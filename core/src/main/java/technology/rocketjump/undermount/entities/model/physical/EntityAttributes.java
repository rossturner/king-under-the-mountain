package technology.rocketjump.undermount.entities.model.physical;

import com.badlogic.gdx.graphics.Color;
import technology.rocketjump.undermount.assets.entities.model.ColoringLayer;
import technology.rocketjump.undermount.persistence.model.ChildPersistable;

public interface EntityAttributes extends ChildPersistable {

	long getSeed();

	Color getColor(ColoringLayer coloringLayer);

	EntityAttributes clone();

}
