package technology.rocketjump.undermount.ui.widgets;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import technology.rocketjump.undermount.rooms.components.StockpileComponent;
import technology.rocketjump.undermount.ui.Scene2DUtils;

import java.util.Arrays;

public class StockpileManagementTree extends Table {

	private final StockpileComponent stockpileComponent;
	private final ScrollPane scrollPane;

	public StockpileManagementTree(Skin uiSkin, MessageDispatcher messageDispatcher, StockpileComponent stockpileComponent) {
		this.stockpileComponent = stockpileComponent;

		Tree<StockpileTreeNode, String> treeExample = new Tree<>(uiSkin);

		for (String test : Arrays.asList("A", "B", "C", "D")) {
			StockpileTreeNode node = new StockpileTreeNode();
			node.setActor(new CheckBox(test, uiSkin));
			node.setValue(test);

			for (String test2 : Arrays.asList("1", "2", "3", "4", "5")) {
				StockpileTreeNode childNode = new StockpileTreeNode();
				childNode.setActor(new CheckBox(test2, uiSkin));
				childNode.setValue(test2);
				node.add(childNode);
			}

			treeExample.add(node);
		}

		scrollPane = Scene2DUtils.wrapWithScrollPane(treeExample, uiSkin);
		this.add(scrollPane).width(550).height(400).left();
	}

	public static class StockpileTreeNode extends Tree.Node<StockpileTreeNode, String, CheckBox> {

	}
}
