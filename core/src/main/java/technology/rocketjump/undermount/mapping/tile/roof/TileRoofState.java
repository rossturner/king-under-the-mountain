package technology.rocketjump.undermount.mapping.tile.roof;

import com.badlogic.gdx.graphics.Color;
import technology.rocketjump.undermount.rendering.utils.HexColors;

public enum TileRoofState {

    OPEN(HexColors.get("#54faed99")),
    CONSTRUCTED(HexColors.get("#463e3a99")),
    MINED(HexColors.get("#dfcfba99")),
    CAVERN(HexColors.get("#8aeab399")),
    MOUNTAIN_ROOF(HexColors.get("#aaaaaa99"));

    public final Color viewColor;

    TileRoofState(Color viewColor) {
        this.viewColor = viewColor;
    }
}
