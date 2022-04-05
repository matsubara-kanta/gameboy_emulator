
import java.io.Serializable;

public class SpriteManager implements Serializable {
    private TileSetManager tileSetManager;
    private LCDControl lcd;
    private MMU mmu;
    
    public SpriteManager(MMU mmu, TileSetManager tileSetManager, LCDControl lcd) {
        this.tileSetManager = tileSetManager;
        this.lcd = lcd;
        this.mmu = mmu;
    }
    


}
