package technology.rocketjump.undermount.settlement.production;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.crafting.CraftingRecipeDictionary;
import technology.rocketjump.undermount.crafting.model.CraftingRecipe;
import technology.rocketjump.undermount.entities.components.ItemAllocationComponent;
import technology.rocketjump.undermount.entities.model.Entity;
import technology.rocketjump.undermount.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.undermount.entities.model.physical.item.ItemType;
import technology.rocketjump.undermount.entities.model.physical.item.QuantifiedItemTypeWithMaterial;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.Updatable;
import technology.rocketjump.undermount.jobs.model.JobPriority;
import technology.rocketjump.undermount.materials.model.GameMaterial;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.messaging.types.ProductionAssignmentRequestMessage;
import technology.rocketjump.undermount.rendering.ScreenWriter;
import technology.rocketjump.undermount.settlement.ItemTracker;
import technology.rocketjump.undermount.settlement.LiquidTracker;
import technology.rocketjump.undermount.settlement.SettlerTracker;

import java.util.*;

import static technology.rocketjump.undermount.jobs.model.JobPriority.DISABLED;

/**
 * This class is responsible for queueing up crafting and other production jobs across the settlement, to meet a set quota of required items and liquids
 */
@Singleton
public class ProductionManager implements Updatable, Telegraph {

	private static float UPDATE_PERIOD_IN_SECONDS = 1.313f;

	private final ItemTracker itemTracker;
	private final LiquidTracker liquidTracker;
	private final SettlerTracker settlerTracker;
	private final CraftingRecipeDictionary craftingRecipeDictionary;
	private final ScreenWriter screenWriter;

	private float timeSinceLastUpdate = 0f;
	private GameContext gameContext;

	@Inject
	public ProductionManager(ItemTracker itemTracker, SettlerTracker settlerTracker,
							 MessageDispatcher messageDispatcher, LiquidTracker liquidTracker,
							 CraftingRecipeDictionary craftingRecipeDictionary, ScreenWriter screenWriter) {
		this.itemTracker = itemTracker;
		this.settlerTracker = settlerTracker;
		this.liquidTracker = liquidTracker;
		this.craftingRecipeDictionary = craftingRecipeDictionary;
		this.screenWriter = screenWriter;


		onContextChange(null);

		messageDispatcher.addListener(this, MessageType.REQUEST_PRODUCTION_ASSIGNMENT);
		messageDispatcher.addListener(this, MessageType.PRODUCTION_ASSIGNMENT_ACCEPTED);
		messageDispatcher.addListener(this, MessageType.PRODUCTION_ASSIGNMENT_CANCELLED);
		messageDispatcher.addListener(this, MessageType.PRODUCTION_ASSIGNMENT_COMPLETED);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.REQUEST_PRODUCTION_ASSIGNMENT: {
				return handle((ProductionAssignmentRequestMessage) msg.extraInfo);
			}
			case MessageType.PRODUCTION_ASSIGNMENT_ACCEPTED: {
				ProductionAssignment assignment = (ProductionAssignment) msg.extraInfo;
				for (QuantifiedItemTypeWithMaterial output : assignment.targetRecipe.getOutput()) {
					if (output.isLiquid()) {
						Map<Long, ProductionAssignment> productionAssignmentMap = gameContext.getSettlementState().liquidProductionAssignments.computeIfAbsent(output.getMaterial(), o -> new HashMap<>());
						productionAssignmentMap.put(assignment.productionAssignmentId, assignment);
						float quantityRequired = gameContext.getSettlementState().requiredLiquidCounts.getOrDefault(output.getMaterial(), 0f);
						quantityRequired -= output.getQuantity();
						gameContext.getSettlementState().requiredLiquidCounts.put(output.getMaterial(), quantityRequired);
					} else {
						Map<Long, ProductionAssignment> productionAssignmentMap = gameContext.getSettlementState().itemTypeProductionAssignments.computeIfAbsent(output.getItemType(), (o) -> new HashMap<>());
						productionAssignmentMap.put(assignment.productionAssignmentId, assignment);
						int quantityRequired = gameContext.getSettlementState().requiredItemCounts.getOrDefault(output.getItemType(), 0);
						quantityRequired -= output.getQuantity();
						gameContext.getSettlementState().requiredItemCounts.put(output.getItemType(), quantityRequired);
					}

				}
				return true;
			}
			case MessageType.PRODUCTION_ASSIGNMENT_CANCELLED:
			case MessageType.PRODUCTION_ASSIGNMENT_COMPLETED: {
				ProductionAssignment assignment = (ProductionAssignment) msg.extraInfo;
				if (assignment == null || assignment.targetRecipe == null) {
					Logger.error("PRODUCTION_ASSIGNMENT_COMPLETED with null targetRecipe");
				} else {
					for (QuantifiedItemTypeWithMaterial output : assignment.targetRecipe.getOutput()) {
						Map<Long, ProductionAssignment> productionAssignmentMap;
						if (output.isLiquid()) {
							productionAssignmentMap = gameContext.getSettlementState().liquidProductionAssignments.computeIfAbsent(output.getMaterial(), (o) -> new HashMap<>());
						} else {
							productionAssignmentMap = gameContext.getSettlementState().itemTypeProductionAssignments.computeIfAbsent(output.getItemType(), (o) -> new HashMap<>());
						}
						productionAssignmentMap.remove(assignment.productionAssignmentId);
					}
				}
				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	private boolean handle(ProductionAssignmentRequestMessage requestMessage) {
		List<CraftingRecipe> recipesForCraftingType = craftingRecipeDictionary.getByCraftingType(requestMessage.craftingType);

		Map<JobPriority, List<CraftingRecipe>> requiredCraftingRecipes = new HashMap<>();
		for (CraftingRecipe craftingRecipe : recipesForCraftingType) {
			JobPriority recipePriority = getRecipePriority(craftingRecipe);
			if (recipePriority.equals(DISABLED)) {
				continue;
			}

			for (QuantifiedItemTypeWithMaterial output : craftingRecipe.getOutput()) {
				if (output.isLiquid()) {
					float numRequired = gameContext.getSettlementState().requiredLiquidCounts.getOrDefault(output.getMaterial(), 0f);
					if (numRequired > 0.1f) {
						requiredCraftingRecipes.computeIfAbsent(recipePriority, a -> new ArrayList<>()).add(craftingRecipe);
						break;
					}
				} else {
					int numRequired = gameContext.getSettlementState().requiredItemCounts.getOrDefault(output.getItemType(), 0);
					if (numRequired > 0) {
						requiredCraftingRecipes.computeIfAbsent(recipePriority, a -> new ArrayList<>()).add(craftingRecipe);
						break;
					}
				}

			}
		}


		Map<JobPriority, List<CraftingRecipe>> availableCraftingRecipes = new HashMap<>();
		for (Map.Entry<JobPriority, List<CraftingRecipe>> entry : requiredCraftingRecipes.entrySet()) {
			JobPriority recipePriority = entry.getKey();
			List<CraftingRecipe> craftingRecipes = entry.getValue();
			for (CraftingRecipe craftingRecipe : craftingRecipes) {
				boolean allInputsAvailable = true;
				for (QuantifiedItemTypeWithMaterial input : craftingRecipe.getInput()) {
					if (input.isLiquid()) {
						// Just assume all liquid available for now
						break;
					}

					List<Entity> unallocatedItems;
					if (input.getMaterial() == null) {
						unallocatedItems = itemTracker.getItemsByType(input.getItemType(), true);
					} else {
						unallocatedItems = itemTracker.getItemsByTypeAndMaterial(input.getItemType(), input.getMaterial(), true);
					}

					int quantityFound = 0;
					for (Entity unallocatedItem : unallocatedItems) {
						quantityFound += unallocatedItem.getOrCreateComponent(ItemAllocationComponent.class).getNumUnallocated();
						if (quantityFound >= input.getQuantity()) {
							break;
						}
					}
					if (quantityFound < input.getQuantity()) {
						// Not enough of this item
						allInputsAvailable = false;
						break;
					}
				}

				if (allInputsAvailable) {
					availableCraftingRecipes.computeIfAbsent(recipePriority, a -> new ArrayList<>()).add(craftingRecipe);
				}
			}
		}

		List<CraftingRecipe> callbackResult = new ArrayList<>();
		if (!availableCraftingRecipes.isEmpty()) {
			// Add in priority order
			for (JobPriority priority : JobPriority.values()) {
				if (availableCraftingRecipes.containsKey(priority)) {
					List<CraftingRecipe> recipeList = availableCraftingRecipes.get(priority);
					Collections.shuffle(recipeList, gameContext.getRandom());
					callbackResult.addAll(recipeList);
				}
			}
		}

		requestMessage.callback.productionAssignmentCallback(callbackResult);
		return true;
	}

	@Override
	public void update(float deltaTime) {
		timeSinceLastUpdate += deltaTime;
		if (timeSinceLastUpdate > UPDATE_PERIOD_IN_SECONDS) {
			doUpdate();
			timeSinceLastUpdate = 0f;
		}

//		if (GlobalSettings.DEV_MODE) {
//
//			for (Map.Entry<GameMaterial, ProductionQuota> productionQuotaEntry : gameContext.getSettlementState().liquidProductionQuotas.entrySet()) {
//				String message = productionQuotaEntry.getKey().getMaterialName() + " quota: " + productionQuotaEntry.getValue().toString() +
//						" required: " + gameContext.getSettlementState().requiredLiquidCounts.getOrDefault(productionQuotaEntry.getKey(), 0f) + " assignments: " +
//						gameContext.getSettlementState().liquidProductionAssignments.getOrDefault(productionQuotaEntry.getKey(), new HashMap<>()).size();
//				screenWriter.printLine(message);
//			}
//		}

	}

	public ProductionQuota getProductionQuota(ItemType itemType) {
		return gameContext.getSettlementState().itemTypeProductionQuotas.getOrDefault(itemType, new ProductionQuota());
	}

	public void productionQuoteModified(ItemType itemType, ProductionQuota newQuota) {
		if (gameContext != null) {
			gameContext.getSettlementState().itemTypeProductionQuotas.put(itemType, newQuota);
		}
	}

	public ProductionQuota getProductionQuota(GameMaterial liquidMaterial) {
		return gameContext.getSettlementState().liquidProductionQuotas.getOrDefault(liquidMaterial, new ProductionQuota());
	}

	public void productionQuoteModified(GameMaterial liquidMaterial, ProductionQuota newQuota) {
		if (gameContext != null) {
			gameContext.getSettlementState().liquidProductionQuotas.put(liquidMaterial, newQuota);
		}
	}

	public JobPriority getRecipePriority(CraftingRecipe craftingRecipe) {
		if (gameContext != null) {
			return gameContext.getSettlementState().craftingRecipePriority.getOrDefault(craftingRecipe, JobPriority.NORMAL);
		} else {
			return JobPriority.NORMAL;
		}
	}

	public void setRecipePriority(CraftingRecipe craftingRecipe, JobPriority priority) {
		if (gameContext != null && priority != null) {
			if (JobPriority.NORMAL.equals(priority)) {
				gameContext.getSettlementState().craftingRecipePriority.remove(craftingRecipe);
			} else {
				gameContext.getSettlementState().craftingRecipePriority.put(craftingRecipe, priority);
			}
		}
	}

	private void doUpdate() {
		int numSettlers = settlerTracker.count();
		for (Map.Entry<ItemType, ProductionQuota> quotaEntry : gameContext.getSettlementState().itemTypeProductionQuotas.entrySet()) {
			ItemType itemType = quotaEntry.getKey();
			int requiredAmount = quotaEntry.getValue().getRequiredAmount(numSettlers);
			int currentAmount = 0;
			for (Entity item : itemTracker.getItemsByType(itemType, false)) {
				currentAmount += ((ItemEntityAttributes) item.getPhysicalEntityComponent().getAttributes()).getQuantity();
			}
			int inProduction = 0;
			for (ProductionAssignment productionAssignment : gameContext.getSettlementState().itemTypeProductionAssignments.computeIfAbsent(itemType, (o) -> new HashMap<>()).values()) {
				Optional<QuantifiedItemTypeWithMaterial> matchingOutput = productionAssignment.targetRecipe.getOutput().stream()
						.filter((output) -> output.getItemType().equals(itemType))
						.findFirst();
				if (matchingOutput.isPresent()) {
					inProduction += matchingOutput.get().getQuantity();
				}
			}


			int missingCount = Math.max(requiredAmount - (currentAmount + inProduction), 0);
			gameContext.getSettlementState().requiredItemCounts.put(itemType, missingCount);
		}


		for (Map.Entry<GameMaterial, ProductionQuota> quotaEntry : gameContext.getSettlementState().liquidProductionQuotas.entrySet()) {
			GameMaterial liquidMaterial = quotaEntry.getKey();
			int requiredAmount = quotaEntry.getValue().getRequiredAmount(numSettlers);
			float currentAmount = liquidTracker.getCurrentLiquidAmount(liquidMaterial);
			int inProduction = 0;
			for (ProductionAssignment productionAssignment : gameContext.getSettlementState().liquidProductionAssignments.computeIfAbsent(liquidMaterial, (o) -> new HashMap<>()).values()) {
				Optional<QuantifiedItemTypeWithMaterial> matchingOutput = productionAssignment.targetRecipe.getOutput().stream()
						.filter((output) -> output.getMaterial().equals(liquidMaterial) && output.isLiquid())
						.findFirst();
				if (matchingOutput.isPresent()) {
					inProduction += matchingOutput.get().getQuantity();
				}
			}

			float missingCount = Math.max(requiredAmount - (currentAmount + inProduction), 0);
			gameContext.getSettlementState().requiredLiquidCounts.put(liquidMaterial, missingCount);
		}

	}

	@Override
	public boolean runWhilePaused() {
		return true;
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {
	}
}
