
public class SmallSprite implements ISprite {

    private static final long serialVersionUID = -4898821488597421936L;
    private Tile tile1;
    int spriteX;
    int spriteY;
    int flags;
    boolean usePalletteZero;
    int priority;
    int spriteAddress;
    
    public SmallSprite(MMU mem, int spriteAddress, TileSet tileset) {
        this.spriteAddress = spriteAddress;
        spriteY = mem.readByte(spriteAddress);
        spriteX = mem.readByte(spriteAddress + 1);
        flags = mem.readByte(spriteAddress + 3);
        this.priority = (int) Bit.extract(flags, 7, 7);
        int tileNum = mem.readByte(spriteAddress + 2);
        tile1 = tileset.getTile(tileNum);
        if (Bit.extract(flags, 5, 5) == 1) {
            tile1 = tile1.flipTileOverXAxis();
        }
        if (Bit.extract(flags, 6, 6) == 1) {
            tile1 = tile1.flipTileOverYAxis();
        }
        if (Bit.extract(flags, 4, 4) == 0) {
            usePalletteZero = true;
        }
        else {
            usePalletteZero = false;
        }
    }
    
    @Override
    public int getSpriteY() {
        return spriteY;
    }
    
    @Override
    public int getSpriteX() {
        return spriteX;
    }
    
    
    @Override
    public int getPixel(int posY, int posX) {
        return tile1.getPixel(posY, posX);
    }
    
    @Override
    public boolean inRange(int posY) {
        return posY >= spriteY && posY < spriteY + 8;
    }
    
    @Override
    public boolean usePalletteZero() {
        return usePalletteZero;
    }
    
    @Override
    public int getSpriteAddress() {
        return spriteAddress;
    }
    
    @Override
    public int getPriority() {
        return priority;
    }
}
