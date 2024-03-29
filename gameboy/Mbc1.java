
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidParameterException;

class Mbc1 implements Cartridge {
    private static final long serialVersionUID = -3769278111043856834L;

    public static final int BANK_SIZE = 0x4000;
    private static final int RAM_BANK_SIZE = 0x2000;
    
    private boolean ramEnabled;
    private boolean isRomBankingMode;
    private boolean hasBattery;
    private int ramBank;
    private int upperBits;
    private String fileName;
    private byte[][] banks;
    private byte[] ram;
    int currentBank = 1;
    
    public Mbc1(byte[] rom, String fileName){
        this.fileName = fileName + ".sav";
        int numBanks = rom.length / BANK_SIZE;
        banks = new byte[numBanks][BANK_SIZE];
        ramEnabled = false;
        isRomBankingMode = true;
        for(int i = 0; i < rom.length; i++){
            banks[i / BANK_SIZE][i % BANK_SIZE] = rom[i];
        }
        ram = new byte[0xFFFF];
        hasBattery = rom[0x0147] == 0x03;
        if (hasBattery) {
            File ramData = new File(this.fileName);
            if (ramData.exists()) {
                try {
                    System.out.println("found data");
                    FileInputStream ramInput = new FileInputStream(ramData);
                    ramInput.read(ram);
                    ramInput.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else {
                try {
                    ramData.createNewFile();
                    FileOutputStream out = new FileOutputStream(ramData);
                    for (int i = 0; i < ram.length; i++) {
                        out.write(0);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                
            }
        }
    }

    /* 読み出し */
    public int readByte(int location) {
        if(location > 0xBFFF) throw new InvalidParameterException("Out of cartridge memory");
        
        if (location >= 0xA000 && location <= 0xBFFF) {
            int ramLocation = (ramBank * RAM_BANK_SIZE) + location % 0xA000;
            return ram[ramLocation] & 0xFF;
        }
        
        if(location < BANK_SIZE){
            return banks[0][location] & 0xff;
        }
        
        return banks[currentBank % banks.length][location - BANK_SIZE] & 0xff;
    }

    /* 書き出し */
    public void writeByte(int location, int toWrite) {
        if(location >= 0x2000 && location <= 0x3fff) {
            int newBank = toWrite & 0x1f;
            if(newBank == 0) newBank = 1;
            
            if (isRomBankingMode) {
                newBank += upperBits << 5;
            }
            this.currentBank = newBank;
        }
        
        if (location >= 0x0000 && location <= 0x1FFF) {
            ramEnabled = (toWrite & 0x0A) == 0x0A;
        }
        
        if (location >= 0xA000 && location <= 0xBFFF && ramEnabled) {
            int ramLocation = (ramBank * RAM_BANK_SIZE) + location % 0xA000;
            ram[ramLocation] = (byte) toWrite;
        }
        
        if (location >= 0x4000 && location <= 0x5FFF) {
            ramBank = toWrite & 3;
            upperBits = toWrite & 3;
        }
        
        if (location >= 0x6000 && location <= 0x7FFF) {
            isRomBankingMode = toWrite == 0;
        }
    }

    @Override
    public void cleanUp() {
        if (hasBattery) {
            try {
                FileOutputStream cartridgeRam = new FileOutputStream(this.fileName);
                cartridgeRam.write(ram);
                cartridgeRam.close();
                
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
