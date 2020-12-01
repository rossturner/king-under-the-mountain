package technology.rocketjump.undermount.ui.widgets;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.ui.actions.ButtonAction;

// TODO extended button/table functionality from ImageButton/IconButton/ClickableTable needs refactoring together
public class ClickableTable extends Table {

	private Color defaultColor = Color.WHITE;
	private Color highlightColor = Color.GRAY;

	private ButtonAction action;
	private ButtonAction onClickSoundAction;
	private boolean isHighlighted;
	private IconButton.IconButtonOnEnter onEnter;

	public ClickableTable(Skin skin) {
		super(skin);

		this.setTouchable(Touchable.enabled);
		this.setColor(defaultColor);
		this.addListener(new ClickableTableListener(this));
	}

	public void setDefaultColor(Color defaultColor) {
		this.defaultColor = defaultColor;
	}

	public void setHighlightColor(Color highlightColor) {
		this.highlightColor = highlightColor;
	}

	public void setAction(ButtonAction action) {
		this.action = action;
	}

	public void setOnClickSoundAction(ButtonAction onClickSoundAction) {
		this.onClickSoundAction = onClickSoundAction;
	}

	public void setOnEnter(IconButton.IconButtonOnEnter onEnter) {
		this.onEnter = onEnter;
	}

	public void setHighlighted(boolean highlight) {
		this.isHighlighted = highlight;
		if (highlight) {
			this.setColor(highlightColor);
		} else {
			this.setColor(defaultColor);
		}
	}

	protected static class ClickableTableListener extends ClickListener {

		protected final ClickableTable parent;

		public ClickableTableListener(ClickableTable parent) {
			this.parent = parent;
		}

		@Override
		public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
//			if (button == Input.Buttons.RIGHT && parent.rightClickAction != null) {
//				parent.rightClickAction.onClick();
//				if (parent.onClickSoundAction != null) {
//					parent.onClickSoundAction.onClick();
//				}
//			}
			Actor target = parent.hit(x, y, true);
			if (target != null) {
				if (target instanceof SelectBox || target instanceof TextField || (target.hasParent() && target.getParent() instanceof IconOnlyButton)) {
					return false;
				}
			}
			return super.touchDown(event, x, y, pointer, button);
		}

		public void clicked(InputEvent event, float x, float y) {
			if (parent.onClickSoundAction != null) {
				parent.onClickSoundAction.onClick();
			}
			if (parent.action == null) {
				Logger.error("No action set for " + parent.getClass().getSimpleName());
			} else {
				parent.action.onClick();
			}
		}

		public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
			if (fromActor == null || !fromActor.isDescendantOf(parent)) {
				if (parent.onEnter != null && !parent.isHighlighted) {
					parent.onEnter.onEnterAction();
				}
				parent.setHighlighted(true);
			}
		}

		public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
			if (toActor == null || !toActor.isDescendantOf(parent)) {
				parent.setHighlighted(false);
			}
		}

	}


}
