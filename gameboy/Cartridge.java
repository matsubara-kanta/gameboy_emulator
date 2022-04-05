
import java.io.*;
import java.security.InvalidParameterException;

public interface Cartridge extends Serializable {
    static Cartridge fromFile(String fileName) {
        File file = new File(fileName);
        byte[] rom = new byte[(int)file.length()];
        
        try {
            new FileInputStream(file).read(rom);
        } catch (IOException e) {
            System.out.println("File not found: " + fileName);
            return null;
        }

        int cartridgeType = rom[0x0147] & 0xff;
        if(cartridgeType == 0x00){
            return new Rom(rom);
        }else if(cartridgeType < 0x04){
            return new Mbc1(rom, fileName);
        }
        else if (cartridgeType >= 0x0F && cartridgeType <= 0x13) {
            return new Mbc3(rom, fileName);
        }
        else if (cartridgeType >= 0x19 && cartridgeType <= 0x1E) {
            return new Mbc5(rom, fileName);
        }
        else {
        }
        
        throw new InvalidParameterException("Cartridge type not supported");
    }
     int readByte(int location);
    void writeByte(int location, int toWrite);
    void cleanUp();
}



