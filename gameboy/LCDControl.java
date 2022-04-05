
import java.io.Serializable;

public class LCDControl implements Serializable {
    
    private static final int LCDControlAddress = 0xFF40;
    
    private MMU mem;
    private boolean isDisplayEnabled;
    private boolean use9800TileMapAddressingForWindow;
    private boolean isWindowEnabled;
    private boolean use8000TileDataForWindowAndBackground;
    private boolean use9800TileMapAddressingForBackground;
    private boolean useSmallSprites;
    private boolean spritesEnabled;
    private boolean backgroundDisplay;
    
    public LCDControl(MMU mem) {
        this.mem = mem;
        this.isDisplayEnabled = true;
    }
    
    public void update() {
        int lcdcontrol = mem.readByte(LCDControlAddress) & 0xFF;
        isDisplayEnabled = Bit.extract(lcdcontrol, 7, 7) == 1;
        use9800TileMapAddressingForWindow = Bit.extract(lcdcontrol, 6, 6) == 0;
        isWindowEnabled = Bit.extract(lcdcontrol, 5, 5) == 1;
        use8000TileDataForWindowAndBackground = Bit.extract(lcdcontrol, 4, 4) == 1;
        use9800TileMapAddressingForBackground = Bit.extract(lcdcontrol, 3, 3) == 0;
        useSmallSprites = Bit.extract(lcdcontrol, 2, 2) == 0;
        spritesEnabled = Bit.extract(lcdcontrol, 1, 1) == 1;
        backgroundDisplay = Bit.extract(lcdcontrol, 0, 0) == 1;
    }
    
    public boolean isDisplayEnabled() {
        return isDisplayEnabled;
    }
    
    public boolean isUse9800TileMapAddressingForWindow() {
        return use9800TileMapAddressingForWindow;
    }
    
    public boolean isWindowEnabled() {
        return isWindowEnabled;
    }
    
    public boolean isUse8000TileDataForWindowAndBackground() {
        return use8000TileDataForWindowAndBackground;
    }
    
    public boolean isUseSmallSprites() {
        return useSmallSprites;
    }
    
    public boolean isSpritesEnabled() {
        return spritesEnabled;
    }

    public boolean isUse9800TileMapAddressingForBackground() {
        return use9800TileMapAddressingForBackground;
    }

    public boolean isBackgroundDisplay() {
        return backgroundDisplay;
    }

    


    
    
}
