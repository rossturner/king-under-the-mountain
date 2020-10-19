package technology.rocketjump.undermount.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import technology.rocketjump.undermount.gamecontext.GameContext;
import technology.rocketjump.undermount.gamecontext.GameContextAware;
import technology.rocketjump.undermount.messaging.MessageType;
import technology.rocketjump.undermount.persistence.UserPreferences;
import technology.rocketjump.undermount.rendering.utils.HexColors;
import technology.rocketjump.undermount.ui.i18n.I18nTranslator;
import technology.rocketjump.undermount.ui.skins.GuiSkinRepository;
import technology.rocketjump.undermount.ui.widgets.*;

import static technology.rocketjump.undermount.persistence.UserPreferences.PreferenceKey.UI_SCALE;
import static technology.rocketjump.undermount.rendering.camera.DisplaySettings.DEFAULT_UI_SCALE;

public abstract class ManagementScreen implements GameScreen, Telegraph, GameContextAware {

    protected final MessageDispatcher messageDispatcher;
    protected final I18nTranslator i18nTranslator;
    protected final I18nWidgetFactory i18nWidgetFactory;

    protected final OrthographicCamera camera = new OrthographicCamera();
    protected final Table containerTable;
    protected final Skin uiSkin;
    protected final Stage stage;
    protected final I18nLabel titleLabel;
    protected Float uiScale;
    protected GameContext gameContext;

    public ManagementScreen(UserPreferences userPreferences, MessageDispatcher messageDispatcher,
                                   GuiSkinRepository guiSkinRepository, I18nWidgetFactory i18nWidgetFactory,
                                   I18nTranslator i18nTranslator, IconButtonFactory iconButtonFactory) {
        this.uiSkin = guiSkinRepository.getDefault();
        this.messageDispatcher = messageDispatcher;
        this.i18nTranslator = i18nTranslator;
        this.i18nWidgetFactory = i18nWidgetFactory;

        containerTable = new Table(uiSkin);
        containerTable.setFillParent(true);
        containerTable.center().top();

        uiScale = Float.valueOf(userPreferences.getPreference(UI_SCALE, DEFAULT_UI_SCALE));
        ScreenViewport viewport = new ScreenViewport();
        viewport.setUnitsPerPixel(1 / Float.valueOf(uiScale));
        stage = new Stage(viewport);
        stage.addActor(containerTable);

        IconButton backButton = iconButtonFactory.create("GUI.BACK_LABEL", "arrow-left", HexColors.get("#D9D9D9"), ButtonStyle.DEFAULT);
        backButton.setAction(() -> messageDispatcher.dispatchMessage(MessageType.SWITCH_SCREEN, "MAIN_GAME"));
        Container<IconButton> backButtonContainer = new Container<>(backButton);
        backButtonContainer.left().bottom();
        stage.addActor(backButtonContainer);

        titleLabel = i18nWidgetFactory.createLabel(getTitleI18nKey());

        messageDispatcher.addListener(this, MessageType.GUI_SCALE_CHANGED);
    }

    public abstract String getTitleI18nKey();



    @Override
    public void show() {
        clearContextRelatedState();

        InputMultiplexer inputMultiplexer = new InputMultiplexer();
        inputMultiplexer.addProcessor(stage);
        inputMultiplexer.addProcessor(new ManagementScreenInputHandler(messageDispatcher));
        Gdx.input.setInputProcessor(inputMultiplexer);

        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    public abstract void reset();

    @Override
    public void resize(int width, int height) {
        camera.setToOrtho(false, width, height);

        ScreenViewport viewport = new ScreenViewport(new OrthographicCamera(width, height));
        viewport.setUnitsPerPixel(1 / uiScale);
        stage.setViewport(viewport);
        stage.getViewport().update(width, height, true);

        reset();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.4f, 0.4f, 0.4f, 1); // MODDING expose default background color
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        stage.act(delta);

        stage.draw();
    }

    @Override
    public boolean handleMessage(Telegram msg) {
        switch (msg.message) {
            case MessageType.GUI_SCALE_CHANGED: {
                this.uiScale = (Float) msg.extraInfo;
                resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                return true;
            }
            default:
                throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
        }
    }

    @Override
    public void hide() {
        clearContextRelatedState();
    }

    @Override
    public void showDialog(GameDialog dialog) {
        dialog.show(stage);
    }

    @Override
    public void onContextChange(GameContext gameContext) {
        this.gameContext = gameContext;
    }

}
