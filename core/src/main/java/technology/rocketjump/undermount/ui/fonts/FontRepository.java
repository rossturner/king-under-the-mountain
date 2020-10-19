package technology.rocketjump.undermount.ui.fonts;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.undermount.constants.ConstantsRepo;
import technology.rocketjump.undermount.constants.UiConstants;
import technology.rocketjump.undermount.ui.i18n.I18nRepo;

@Singleton
public class FontRepository {

	public static final String UNICODE_FONT_FILENAME = "NotoSansCJKjp-Regular.otf";

	private final I18nRepo i18nRepo;
	private final UiConstants uiConstants;

	private String currentFontName;
	private GameFont largestFont;
	private GameFont defaultUIFont;
	private GameFont guaranteedUnicodeFont;

	@Inject
	public FontRepository(I18nRepo i18nRepo, ConstantsRepo constantsRepo) {
		this.i18nRepo = i18nRepo;
		this.uiConstants = constantsRepo.getUiConstants();
		// MODDING - Expose the font selction and sizes from small to large

		this.currentFontName = uiConstants.getDefaultFont();
		if (i18nRepo.getCurrentLanguageType().getFontName() != null) {
			this.currentFontName = i18nRepo.getCurrentLanguageType().getFontName();
		}
		loadFontFile();

		this.guaranteedUnicodeFont = generateFont(UNICODE_FONT_FILENAME);
	}

	public boolean changeFontName(String fontName) {
		if (fontName == null) {
			fontName = uiConstants.getDefaultFont();
		}
		if (!fontName.equals(currentFontName)) {
			this.currentFontName = fontName;
			this.defaultUIFont.dispose();
			this.largestFont.dispose();
			loadFontFile();
			return true;
		} else {
			return false;
		}
	}

	private GameFont generateFont(String fontFilename) {
		FileHandle fontFile = Gdx.files.internal("assets/ui/fonts/" + fontFilename);
		if (!fontFile.exists()) {
			Logger.error(fontFile.toString() + " does not exist");
			return defaultUIFont;
		}
		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(fontFile);
		FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
		parameter.characters = i18nRepo.getAllCharacters(FreeTypeFontGenerator.DEFAULT_CHARS);
		return new GameFont(generator.generateFont(parameter), 16);
	}

	private void loadFontFile() {
		FileHandle fontFile = Gdx.files.internal("assets/ui/fonts/" + this.currentFontName);
		if (!fontFile.exists()) {
			Logger.error(fontFile.toString() + " does not exist");
			return;
		}
		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(fontFile);
		FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
		parameter.characters = i18nRepo.getAllCharacters(FreeTypeFontGenerator.DEFAULT_CHARS);
		Logger.debug("Generating font from " + fontFile.name() + " with characters: " + parameter.characters);

		parameter.size = 12;
		GameFont font12pt = new GameFont(generator.generateFont(parameter), 12);

		parameter.size = 14;
		GameFont font14pt = new GameFont(generator.generateFont(parameter), 14);

		parameter.size = 16;
		GameFont font16pt = new GameFont(generator.generateFont(parameter), 16);

		parameter.size = 18;
		GameFont font18pt = new GameFont(generator.generateFont(parameter), 18);

		parameter.size = 20;
		GameFont font20pt = new GameFont(generator.generateFont(parameter), 20);

		generator.dispose(); // don't forget to dispose to avoid memory leaks!

		font20pt.setSmaller(font18pt);
		font18pt.setSmaller(font16pt);
		font16pt.setSmaller(font14pt);
		font14pt.setSmaller(font12pt);

		font12pt.setBigger(font14pt);
		font14pt.setBigger(font16pt);
		font16pt.setBigger(font18pt);
		font18pt.setBigger(font20pt);

		this.defaultUIFont = font16pt;
		this.largestFont = font20pt;
	}

	public GameFont getDefaultFontForUI() {
		return defaultUIFont;
	}

	public GameFont getLargestFont() {
		return largestFont;
	}

	public GameFont getUnicodeFont() {
		return this.guaranteedUnicodeFont;
	}
}
