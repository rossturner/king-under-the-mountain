package technology.rocketjump.undermount.rendering;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.google.inject.Inject;
import technology.rocketjump.undermount.mapping.model.TiledMap;

public class DebugRenderer {

	private final RenderingOptions renderingOptions;
	private final ShapeRenderer shapeRenderer = new ShapeRenderer();
	private final ScreenWriter screenWriter;

	@Inject
	public DebugRenderer(RenderingOptions renderingOptions, ScreenWriter screenWriter) {
		this.renderingOptions = renderingOptions;
		this.screenWriter = screenWriter;
	}

	public void render(TiledMap worldMap, OrthographicCamera camera) {
		screenWriter.render();

		if (renderingOptions.debug().showPathfindingNodes()) {
			shapeRenderer.setProjectionMatrix(camera.combined);
//			shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

//			for (Entity entity : entityStore.getEntities()) {
//				if (entity.getLocationComponent().getCursorWorldPosition() == null) {
//					continue;
//				}

//				if (entity.getId() == 1L) {
//					BigDecimal xRounded = new BigDecimal(entity.getLocationComponent().getCursorWorldPosition().x);
//					xRounded = xRounded.round(new MathContext(3));
//					BigDecimal yRounded = new BigDecimal(entity.getLocationComponent().getCursorWorldPosition().y);
//					yRounded = yRounded.round(new MathContext(3));
//
//
//					screenWriter.printLine("World position: " + xRounded + ", " + yRounded);
//					Array<MapTile> nearestTiles = worldMap.getNearestTiles(entity.getLocationComponent().getCursorWorldPosition());
//					shapeRenderer.setColor(0.6f, 0.6f, 0.6f, 1f);
//					for (MapTile nearestTile : nearestTiles) {
//						shapeRenderer.rect(nearestTile.getTileX(), nearestTile.getTileY(), 1f, 1f);
//					}
//				}

//				if (entity.getLocationComponent().isSlowed()) {
//					shapeRenderer.setColor(1f, 0f, 0f, 1.0f);
//				} else {
//					shapeRenderer.setColor(0.3f, 1.0f, 0f, 1.0f);
//				}
//				shapeRenderer.circle(entity.getLocationComponent().getCursorWorldPosition().x,
//						entity.getLocationComponent().getCursorWorldPosition().y,
//						entity.getLocationComponent().getRadius(), 10);

//				BehaviourComponent behaviourComponent = entity.getBehaviourComponent();
//				if (behaviourComponent instanceof HumanoidBehaviour) {
//					HumanoidBehaviour humanoidBehaviour = (HumanoidBehaviour)behaviourComponent;
//
//					GoToLocationGoal goToLocationGoal = null;
//					if (humanoidBehaviour.getCurrentGoal() instanceof GoToLocationGoal) {
//						goToLocationGoal = (GoToLocationGoal)humanoidBehaviour.getCurrentGoal();
//					} else if (humanoidBehaviour.getCurrentGoal() != null && humanoidBehaviour.getCurrentGoal().getChildGoals().peek() instanceof GoToLocationGoal) {
//						goToLocationGoal = (GoToLocationGoal)humanoidBehaviour.getCurrentGoal().getChildGoals().peek();
//					}
//
//
//					if (goToLocationGoal != null) {
//						if (goToLocationGoal.getChildGoals().peek() instanceof FollowPathGoal) {
//							FollowPathGoal followPathGoal = (FollowPathGoal)goToLocationGoal.getChildGoals().peek();
//							GraphPath<Vector2> pathToFollow = followPathGoal.getPathToFollow();
//							int pathCursor = followPathGoal.getPathCursor();
//							Vector2 lastNode = pathToFollow.get(0);
//							int renderingCursor = 1;
//							shapeRenderer.setColor(1, 1, 1, 1);
//
//							while (renderingCursor < pathToFollow.getCount()) {
//								Vector2 currentNode = pathToFollow.get(renderingCursor);
//								if (renderingCursor >= pathCursor) {
//									shapeRenderer.setColor(0.3f, 0.3f, 1.0f, 1.0f);
//								}
//								shapeRenderer.line(lastNode, currentNode);
//								lastNode = currentNode;
//								renderingCursor++;
//							}
//
//							// Show waypoint
//							if (pathCursor < pathToFollow.getCount()) {
//								shapeRenderer.setColor(0.3f, 1.0f, 0.3f, 1.0f);
//								shapeRenderer.line(entity.getLocationComponent().getCursorWorldPosition(),
//										pathToFollow.get(pathCursor));
//							}
//						}
//
//					}
//
//					// Show velocity
//					shapeRenderer.setColor(1f, 0.3f, 0.3f, 1f);
//					shapeRenderer.line(entity.getLocationComponent().getCursorWorldPosition(),
//							entity.getLocationComponent().getCursorWorldPosition().cpy().add(entity.getLocationComponent().getLinearVelocity()));
//
//				}
//			}

//			shapeRenderer.end();

		}


	}
}
