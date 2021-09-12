package technology.rocketjump.undermount.mapping;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.Updatable;
import technology.rocketjump.undermount.mapping.factories.WaterFlowCalculator;
import technology.rocketjump.undermount.mapping.tile.CompassDirection;
import technology.rocketjump.undermount.mapping.tile.MapTile;
import technology.rocketjump.undermount.mapping.tile.MapVertex;
import technology.rocketjump.undermount.mapping.tile.underground.TileLiquidFlow;
import technology.rocketjump.undermount.mapping.tile.underground.UnderTile;
import technology.rocketjump.undermount.materials.GameMaterialDictionary;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.rendering.ScreenWriter;

import java.util.*;

import static technology.rocketjump.undermount.mapping.factories.WaterFlowCalculator.CHANCE_SINGLE_WATER_EVAPORATES;
import static technology.rocketjump.undermount.mapping.tile.underground.TileLiquidFlow.MAX_LIQUID_FLOW_PER_TILE;

@Singleton
public class LiquidFlowProcessor implements Updatable, Telegraph {

	private static final float MAX_UPDATES_PER_SECOND = 200f;
	private final GameMaterial waterMaterial;
	private GameContext gameContext;
	private Deque<MapTile> currentLiquidFlowTiles = new ArrayDeque<>();
	private List<WaterFlowCalculator.FlowTransition> transitionsToApply = new ArrayList<>();
	private final Set<MapVertex> verticesToUpdate = new HashSet<>();

	@Inject
	public LiquidFlowProcessor(GameMaterialDictionary gameMaterialDictionary, MessageDispatcher messageDispatcher, ScreenWriter screenWriter) {
		// TODO all liquid flow is currently water only, needs to be based on what is adding liquid to flow
		this.waterMaterial = gameMaterialDictionary.getByName("Water");

		messageDispatcher.addListener(this, MessageType.ADD_LIQUID_TO_FLOW);
		messageDispatcher.addListener(this, MessageType.ADD_CHANNEL);
		messageDispatcher.addListener(this, MessageType.REMOVE_CHANNEL);
		messageDispatcher.addListener(this, MessageType.PIPE_ADDED);
		messageDispatcher.addListener(this, MessageType.REMOVE_PIPE);
	}

	@Override
	public void update(float deltaTime) {
		// gameContext.settlementState.activeLiquidFlowTiles are actually the tiles that should be active *next* frame

		transitionsToApply.clear();
		verticesToUpdate.clear();
		int numPerFrame = Math.round(MAX_UPDATES_PER_SECOND * deltaTime);
		int numTilesToUpdateThisFrame = Math.min(numPerFrame, Math.max(1, currentLiquidFlowTiles.size()));

		if (currentLiquidFlowTiles.isEmpty()) {
			if (!gameContext.getSettlementState().activeLiquidFlowTiles.isEmpty()) {
				currentLiquidFlowTiles.addAll(gameContext.getSettlementState().activeLiquidFlowTiles);
				gameContext.getSettlementState().activeLiquidFlowTiles.clear();
			}
		} else {
			while (numTilesToUpdateThisFrame > 0) {
				if (currentLiquidFlowTiles.isEmpty()) {
					break;
				} else {
					MapTile tileToUpdate = currentLiquidFlowTiles.pop();
					updateTile(tileToUpdate);
				}
				numTilesToUpdateThisFrame--;
			}
		}

		transitionsToApply.forEach(this::transitionFlow);
	}

	private void updateTile(MapTile tileToUpdate) {
		if (tileToUpdate.getUnderTile().getLiquidFlow() == null) {
			return;
		}
		int cursorTileWaterAmount = tileToUpdate.getUnderTile().getLiquidFlow().getLiquidAmount();
		if (cursorTileWaterAmount == 0) {
			return;
		}
		GridPoint2 cursorPosition = tileToUpdate.getTilePosition();

		List<CompassDirection> randomisedDirections = new ArrayList<>(CompassDirection.CARDINAL_DIRECTIONS);
		Collections.shuffle(randomisedDirections, gameContext.getRandom());

		for (CompassDirection directionToTry : randomisedDirections) {
			MapTile tileInDirection = gameContext.getAreaMap().getTile(cursorPosition.x + directionToTry.getXOffset(), cursorPosition.y + directionToTry.getYOffset());

			if (tileInDirection != null && tileInDirection.getUnderTile() != null && tileInDirection.getUnderTile().liquidCanFlowFrom(tileToUpdate)) {
				// Liquid flow tile to move to
				TileLiquidFlow liquidFlowInDirection = tileInDirection.getUnderTile().getOrCreateLiquidFlow();
				int liquidAmountInDirection = liquidFlowInDirection.getLiquidAmount();

//				if (liquidAmountInDirection != MAX_LIQUID_FLOW_PER_TILE) {
//					surroundedByMaxLiquid = false;
//				}

				// Only one water transition per tile
				if (liquidAmountInDirection < cursorTileWaterAmount) {
					transitionsToApply.add(new WaterFlowCalculator.FlowTransition(tileToUpdate, tileInDirection, directionToTry));
					if (liquidAmountInDirection < cursorTileWaterAmount / 2) {
						// Double move for more than twice different
						transitionsToApply.add(new WaterFlowCalculator.FlowTransition(tileToUpdate, tileInDirection, directionToTry));
					}
					break;
				}
			}
		}

		transitionsToApply.forEach(this::transitionFlow);
		verticesToUpdate.forEach(this::updateVertexFlow);
	}

	private void transitionFlow(WaterFlowCalculator.FlowTransition transition) {
		if (transition.source.getUnderTile().getLiquidFlow().getLiquidAmount() <= transition.target.getUnderTile().getOrCreateLiquidFlow().getLiquidAmount()) {
			// This transition is no longer valid
			return;
		}


		transition.source.getUnderTile().getLiquidFlow().decrementWater(transition.flowDirection, gameContext.getRandom());
		Collections.addAll(verticesToUpdate, gameContext.getAreaMap().getVertices(transition.source.getTileX(), transition.source.getTileY()));
		// activate tiles around source
		activateTile(transition.source);
		for (CompassDirection neighbourDirection : CompassDirection.CARDINAL_DIRECTIONS) {
			MapTile neighbourTile = gameContext.getAreaMap().getTile(transition.source.getTileX() + neighbourDirection.getXOffset(), transition.source.getTileY() + neighbourDirection.getYOffset());
			if (neighbourTile != null && neighbourTile.getUnderTile() != null && neighbourTile.getUnderTile().liquidCanFlow()) {
				activateTile(neighbourTile);
			}
		}

		boolean liquidEvaporated = transition.source.getUnderTile().getLiquidFlow().getLiquidAmount() == 0 && gameContext.getRandom().nextFloat() < CHANCE_SINGLE_WATER_EVAPORATES;
		if (!liquidEvaporated) {
			transition.target.getUnderTile().getOrCreateLiquidFlow().incrementWater(transition.flowDirection);
			Collections.addAll(verticesToUpdate, gameContext.getAreaMap().getVertices(transition.target.getTileX(), transition.target.getTileY()));
			transition.target.getUnderTile().getLiquidFlow().setLiquidMaterial(transition.source.getUnderTile().getLiquidFlow().getLiquidMaterial());
			// activate tiles around target
			activateTile(transition.target);
			for (CompassDirection neighbourDirection : CompassDirection.CARDINAL_DIRECTIONS) {
				MapTile neighbourTile = gameContext.getAreaMap().getTile(transition.target.getTileX() + neighbourDirection.getXOffset(), transition.target.getTileY() + neighbourDirection.getYOffset());
				if (neighbourTile != null && neighbourTile.getUnderTile() != null && neighbourTile.getUnderTile().liquidCanFlow()) {
					activateTile(neighbourTile);
				}
			}
		}

		if (transition.source.getUnderTile().getLiquidFlow().getLiquidAmount() == 0) {
			transition.source.getUnderTile().getLiquidFlow().setLiquidMaterial(null);
		}
	}

	private void updateVertexFlow(MapVertex mapVertex) {
		Vector2 flowDirection = new Vector2();
		List<Float> nearbyFlowDepths = new ArrayList<>();
		for (MapTile tileNeighbour : gameContext.getAreaMap().getTileNeighboursOfVertex(mapVertex).values()) {
			if (tileNeighbour != null && tileNeighbour.getUnderTile() != null) {
				TileLiquidFlow liquidFlow = tileNeighbour.getUnderTile().getLiquidFlow();
				if (liquidFlow != null) {
					flowDirection.add(liquidFlow.getAveragedFlowDirection());
					nearbyFlowDepths.add((float) liquidFlow.getLiquidAmount());
				}
			}
		}
		flowDirection.scl(0.125f); // divide by 4 so flow is slower at edges, divide by 2 again
		mapVertex.setWaterFlowDirection(flowDirection);

		if (nearbyFlowDepths.isEmpty()) {
			mapVertex.setAverageWaterDepth(0);
		} else {
			// else set to average of values
			mapVertex.setAverageWaterDepth(nearbyFlowDepths.stream().reduce(0f, Float::sum) / (float)nearbyFlowDepths.size());
		}
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.ADD_LIQUID_TO_FLOW: {
				MapTile targetTile = (MapTile) msg.extraInfo;
				UnderTile underTile = targetTile.getOrCreateUnderTile();
				if (underTile.liquidCanFlow()) {
					TileLiquidFlow liquidFlow = underTile.getOrCreateLiquidFlow();
					if (liquidFlow.getLiquidAmount() < MAX_LIQUID_FLOW_PER_TILE) {
						liquidFlow.setLiquidAmount(liquidFlow.getLiquidAmount() + 1);
						liquidFlow.setLiquidMaterial(waterMaterial);
					}
					activateTile(targetTile);
				}
				return true;
			}
			case MessageType.ADD_CHANNEL:
			case MessageType.REMOVE_CHANNEL:
			case MessageType.PIPE_ADDED:
			case MessageType.REMOVE_PIPE:
			case MessageType.LIQUID_REMOVED_FROM_FLOW:
				GridPoint2 targetTile = (GridPoint2) msg.extraInfo;
				activateTile(gameContext.getAreaMap().getTile(targetTile));
				for (CompassDirection neighbourDirection : CompassDirection.CARDINAL_DIRECTIONS) {
					MapTile neighbourTile = gameContext.getAreaMap().getTile(targetTile.x + neighbourDirection.getXOffset(), targetTile.y + neighbourDirection.getYOffset());
					if (neighbourTile != null && neighbourTile.getUnderTile() != null && neighbourTile.getUnderTile().liquidCanFlow()) {
						activateTile(neighbourTile);
					}
				}
				return true;
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this + ", " + msg);
		}
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	private void activateTile(MapTile tileToUpdate) {
		if (!gameContext.getSettlementState().activeLiquidFlowTiles.contains(tileToUpdate)) {
			gameContext.getSettlementState().activeLiquidFlowTiles.add(tileToUpdate);
		}
	}

	@Override
	public void clearContextRelatedState() {
		currentLiquidFlowTiles.clear();
	}

	@Override
	public boolean runWhilePaused() {
		return false;
	}
}
