
import java.io.Serializable;

public class Tile implements Serializable {

    private static final long serialVersionUID = 3314132218790890629L;
    int[][] tileData;

    public Tile(int[] tileBytes) {
        tileData = new int[8][8];
        for (int i = 0; i < 8; i++) {
            int b1 = tileBytes[2 * i] & 0xFF;
            int b2 = tileBytes[2 * i + 1] & 0xFF;
            for (int j = 0; j < 8; j++) {
                int x = (int) Bit.extract(b2, 7 - j, 7 - j);
                x = x << 1;
                int y = (int) Bit.extract(b1, 7 - j, 7 - j);
                tileData[i][j] = x + y;
            }
        }
    }
    
    public Tile() {
        tileData = new int[8][8];
    }
    
    public void updateTile(int byteNum, int data) {
        int line = byteNum / 2;
        for (int i = 0; i < 8; i++) {
            int current = tileData[line][i];
            if (byteNum % 2 == 0) {
                current = current & 0b10;
                current += (int) Bit.extract(data, 7 - i, 7 - i) & 0xFF;
                tileData[line][i] = current;
            }
            else {
                current = current & 0x01;
                int newBits = (int) Bit.extract(data, 7 - i, 7 - i) & 0xFF;
                current += (newBits << 1);
                tileData[line][i] = current;
            }
        }
    }
    
    public Tile(int[][] tileData) {
        this.tileData = tileData;
    }
    
    public int getPixelXFlip(int y, int x) {
        return tileData[y][7 - x];
    }
    
    public int getPixelYFlip(int y, int x) {
        return tileData[7 - y][x];
    }
    
    public int getPixelXandYFlip(int y, int x) {
        return tileData[x][y];
    }
    
    public Tile flipTileOverXAxis() {
        int[][] tileFlip = new int[8][8];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                tileFlip[i][j] = tileData[i][7 - j];
            }
        }
        return new Tile(tileFlip);
        
    }
    
    public Tile flipTileOverYAxis() {
        int[][] tileFlip = new int[8][8];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                tileFlip[j][i] = tileData[7 - j][i];
            }
        }
        return new Tile(tileFlip);
        
    }
    
    public Tile transpose() {
        int[][] tileTranspose = new int[8][8];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                tileTranspose[i][j] = tileData[j][i];
            }
        }
        return new Tile(tileTranspose);
    }
    
    public int getPixel(int x, int y) {
        return tileData[x][y];
    }
}
