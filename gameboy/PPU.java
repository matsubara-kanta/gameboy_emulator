
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.Map;
import java.util.HashMap;

/* PPU(GPU) */

/* 下から Background, Window, Sprites というレイヤー */

public class PPU implements Serializable, IPPU {

    private static final long serialVersionUID = 2887802651514454071L;
    private boolean enabled = true;
    private TileSet tileset1;
    private TileSet tileset2;
    private TileMap map;
    private TileMap window;
    private MMU mem;
    private int currentX;
    private int currentY;
    private transient BufferedImage frame;
    private GameBoyScreen gbs;
    private int scrollX;
    private int scrollY;
    private int cycleCount;
    private boolean drewFrame;
    private Map<Integer, ISprite> sprites;
    private Pallette background;
    private Pallette obp0;
    private Pallette obp1;
    private boolean spritesEnabled;
    private boolean windowEnabled;
    private int windowX;
    private int windowY;
    private int LYCompare = -1;
    private boolean largeSpriteMode;
    private TileSetManager tileSetManager;
    private boolean vBlank;
    private boolean hBlank;
    private int currentMode = 0;
    
    
    public static final int OAM_SEARCH_LENGTH = 80;
    public static final int OAM_SEARCH_START = 0;
    public static final int OAM_SEARCH_END = 79;
    public static final int PIXEL_TRANSFER_LENGTH = 172;
    public static final int PIXEL_TRANSFER_START = 80;
    public static final int PIXEL_TRANSFER_END = 251;
    public static final int H_BLANK_LENGTH = 204;
    public static final int H_BLANK_START = 252;
    public static final int H_BLANK_END = 455;
    public static final int V_BLANK = 10;
    public static final int ACTUAL_LINES = 144;
    public static final int V_BLANK_LINES = 10;
    public static final int LINE_LENGTH = 456;
    
    public static final int H_BLANK_MODE = 0;
    public static final int V_BLANK_MODE = 1;
    public static final int OAM_SEARCH_MODE = 2;
    public static final int PIXEL_TRANSFER_MODE = 3;
    
    int framesDrawn = 0;
    
    public PPU(MMU mem, GameBoyScreen gbs) {
        mem.setPPU(this);
        this.mem = mem;
        frame = new BufferedImage(160, 144, BufferedImage.TYPE_3BYTE_BGR);
        currentX = 0;
        currentY = 0;
        this.gbs = gbs;
        sprites = new HashMap<Integer, ISprite>();
        tileSetManager = new TileSetManager(false);
        mem.setTileSetManager(tileSetManager);
    }
    
    public PPU() {
        frame = new BufferedImage(160, 144, BufferedImage.TYPE_3BYTE_BGR);
    }
    
    public boolean drewFrame() {
        return drewFrame;
    }
    
    public void loadTileSets() {
        tileset1 = tileSetManager.getTileSet(0, 0);
        tileset2 = tileSetManager.getTileSet(0, 1);
    }
    
    
    public void loadMap(boolean useTileSet0, boolean useMap1) {
        int ts = useTileSet0 ? 0 : 1;
        int address = useMap1 ? 0x9800 : 0x9c00;
        map = new TileMap(mem, address, ts, tileSetManager);
    }
    
    public void setTileSetManager(TileSetManager manager) {
        this.tileSetManager = manager;
    }
    
    public void loadWindow(boolean useTileSet0, boolean useMap1) {
        int ts = useTileSet0 ? 0 : 1;
        int address = useMap1 ? 0x9800 : 0x9c00;
        window = new TileMap(mem, address, ts, tileSetManager);
    }
    
    public void loadPallettes() {
        background = new Pallette(mem.readByte(0xFF47));
        obp0 = new Pallette(mem.readByte(0xFF48));
        obp1 = new Pallette(mem.readByte(0xFF49));
    }
    
    public void setLYCompare(int lyCompare){
        this.LYCompare = lyCompare;
    }
    
    public void setMMU(MMU mmu) {
        this.mem = mmu;
    }
    
    public void setGBS(GameBoyScreen gbs) {
        this.gbs = gbs;
    }
    
    public void toggleHBlankIndicator() {
        hBlank = false;
    }
    
    /* ディスプレイはlineごとに描写、H-Blankに*/
    /* 144lineでV-Blankに */
    
    public void tick() {
        scrollX = mem.readByte(0xFF43);
        int lcdc = mem.readByte(0xff40);
        spritesEnabled = Bit.extract(lcdc, 1, 1) == 1;
        enabled = Bit.extract(lcdc, 7, 7) == 1;
        
        if (cycleCount == OAM_SEARCH_START) {
            currentMode = OAM_SEARCH_MODE;
            hBlank = false;
            scrollY = mem.readByte(0xFF42);
            if (currentY < ACTUAL_LINES) {
                int status = mem.readByte(0xFF41) & 0x3F;
                mem.writeByte(0xFF41, status | 0x80);
            }
            mem.writeByte(0xFF44, currentY);
            boolean useTileSet0 = Bit.extract(lcdc, 4, 4) == 1;
            boolean useWindowTileMap0 = Bit.extract(lcdc, 6, 6) == 0;
            if (Bit.extract(lcdc, 2, 2) == 1) {
                largeSpriteMode = true;
            }
            else {
                largeSpriteMode = false;
            }
            boolean useBackgroundMap0 = Bit.extract(lcdc, 3, 3) == 0;
            this.loadMap(useTileSet0, useBackgroundMap0);
            this.loadTileSets();
            if (currentY == 0) {
                vBlank = false;
                this.loadPallettes();
            }
            loadSprites();
            windowEnabled = Bit.extract(lcdc, 5, 5) == 1;
            loadWindow(useTileSet0, useWindowTileMap0);
            windowX = mem.readByte(0xff4b) - 7;
            windowY = mem.readByte(0xff4a);
            currentX = 0;
            scrollX = mem.readByte(0xFF43);
        }
        if (cycleCount == PIXEL_TRANSFER_START) {
            int status = mem.readByte(0xFF41) & 0x3F;
            currentMode = PIXEL_TRANSFER_MODE;
            mem.writeByte(0xFF41, status | 0xC0);
        }
        
        
        
        if (cycleCount >= PIXEL_TRANSFER_START && cycleCount < PIXEL_TRANSFER_START + 160 && currentY < ACTUAL_LINES) {
            int yPos = currentY + scrollY;
            int xPos = scrollX + currentX;
            Tile currentTile;
            Pallette currentPallette;
            int pixel;
            Tile backgroundTile = map.getTile(yPos / 8, xPos / 8);
            if (windowEnabled && currentX >= windowX && currentY >= windowY) {
                Tile windowTile = window.getTile((currentY - windowY) / 8, (currentX - windowX) / 8);
                int windowPixel = windowTile.getPixel((currentY - windowY)  % 8, (currentX - windowX) % 8);
                if (spritesEnabled && sprites.containsKey(currentX + 8)) {
                    ISprite currentSprite = sprites.get(currentX + 8);
                    int spritePixel = currentSprite.getPixel(currentY - (currentSprite.getSpriteY() - 16), currentX - (currentSprite.getSpriteX() - 8));
                    if ((currentSprite.getPriority() == 0 || windowPixel == 0) && spritePixel != 0) {
                        currentPallette = currentSprite.usePalletteZero() ? obp0 : obp1;
                        pixel = spritePixel;
                    }
                    else {
                        currentTile = windowTile;
                        currentPallette = background;
                        pixel = windowPixel;
                    }
                }
                else {
                    currentTile = windowTile;
                    currentPallette = background;
                    pixel = windowPixel;
                }
            }
            else if (spritesEnabled && sprites.containsKey(currentX + 8)) {
                ISprite currentSprite = sprites.get(currentX + 8);
                int spritePixel = currentSprite.getPixel(currentY - (currentSprite.getSpriteY() - 16), currentX - (currentSprite.getSpriteX() - 8));
                if ((currentSprite.getPriority() == 0 || backgroundTile.getPixel(yPos % 8, xPos % 8) == 0)
                        && spritePixel != 0) {
                    currentPallette = currentSprite.usePalletteZero() ? obp0 : obp1;
                    pixel = spritePixel;
                }
                else {
                    currentTile = backgroundTile;
                    currentPallette = background;
                    pixel = currentTile.getPixel(yPos % 8, xPos % 8);
                }
            }
            else {
                currentTile = backgroundTile;
                currentPallette = background;
                pixel = currentTile.getPixel(yPos % 8, xPos % 8);
            }
            if (frame == null) {
                frame = new BufferedImage(160, 144, BufferedImage.TYPE_3BYTE_BGR);
            }
            if (!enabled) {
                pixel = 0;
            }
            frame.setRGB(currentX, currentY, currentPallette.getColor(pixel, currentX, currentY).getRGB());
            currentX++;
        }
        if (cycleCount == H_BLANK_START && currentY < ACTUAL_LINES) {
            if (!vBlank) {
                hBlank = true;
            }
            int status = mem.readByte(0xFF41) & 0x3F;
            currentMode = H_BLANK_MODE;
            mem.writeByte(0xFF41, status | 0xC0);
        }
        
        if (cycleCount == H_BLANK_END) {
            hBlank = false;
            currentY++;
            if (currentY == 154) {
                currentY = 0;
            }
        }
        
        drewFrame = false;
        if (currentY == 145 && cycleCount == 0) {
            vBlank = true;
            currentMode = V_BLANK_MODE;
            drawFrame();
        }
        
        if (currentY == LYCompare){
            int interruptRegister = mem.readByte(0xFF0F) & 0xFE;
            if(enabled) mem.writeByte(0xFF0F, interruptRegister | 0x02);
        }

        mem.writeByte(0xFF41, mem.readByte(0xFF41) & (~3) | currentMode);
        
        cycleCount++;
        cycleCount %= LINE_LENGTH;
    }
    
    
    private void drawFrame() {
        int status = mem.readByte(0xFF41) & 0x3F;
        mem.writeByte(0xFF41, status | 0x40);
        gbs.drawFrame(frame);
        drewFrame = true;
        int interruptRegister = mem.readByte(0xFF0F) & 0xFE;
        if(enabled) mem.writeByte(0xFF0F, interruptRegister | 0x01);
    }
    
    
    public void loadSprites() {
        sprites.clear();
        int spriteCount = 0;
        int spritesFound = 0;
        int memAddress = 0xFE00;
        while (spriteCount < 40 && spritesFound < 10) {
            ISprite s = null;
            if (largeSpriteMode) {
                s = new LargeSprite(mem, memAddress, tileset1);
            }
            else {
                s = new SmallSprite(mem, memAddress, tileset1);
            }
            if (s.inRange(currentY + 16)) {
                spritesFound++;
                for (int i = 0; i < 8; i++) {
                    if (!sprites.containsKey(s.getSpriteX() + i)) {
                        if (s.getPixel(currentY - (s.getSpriteY() - 16), i) != 0) {
                            sprites.put(s.getSpriteX() + i, s);
                        }
                    }
                    else {
                        ISprite conflictSprite = sprites.get(s.getSpriteX() + i);
                        boolean isTransparent = s.getPixel(currentY - (s.getSpriteY() - 16), i) == 0;
                        if (s.getSpriteX() < conflictSprite.getSpriteX() && !isTransparent) {
                            sprites.put(s.getSpriteX() + i, s);
                        }
                    }
                }
            }
            spriteCount++;
            memAddress += 4;
        }
    }
    
    @Override
    public boolean isHBlank() {
        return hBlank;
    }
    
}
