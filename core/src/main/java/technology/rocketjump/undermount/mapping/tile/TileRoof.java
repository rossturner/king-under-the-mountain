package technology.rocketjump.undermount.mapping.tile;

import com.badlogic.gdx.graphics.Color;
import technology.rocketjump.undermount.rendering.utils.HexColors;

public enum TileRoof {

    OPEN(HexColors.get("#54faed99")),
    CONSTRUCTED(HexColors.get("#d9913699")),
    MINED(HexColors.get("#dfcfba99")),
    CAVERN(HexColors.get("#8aeab399")),
    MOUNTAIN_ROOF(HexColors.get("#aaaaaa99"));

    public final Color viewColor;

    TileRoof(Color viewColor) {
        this.viewColor = viewColor;
    }
}
