
import javax.sound.sampled.SourceDataLine;
import java.io.Serializable;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;

/* メモリ */
public class MMU implements Serializable {
    private byte[] mem = new byte[0xFFFF+1];
    private Cartridge rom;
    private byte[] bootRom = Base64.getDecoder().decode("Mf7/IQCAIstsKPs+gOAm4BE+8+AS4CU+d+AkPvzgRxEEASEQgBpHzYIAzYIAE3vuNCDyEbEADggaEyIjDSD5PhnqEJkhL5kODD0oCDINIPkuDxj1PpHgQAYtzaMAPoPNqgAGBc2jAD7BzaoABkbNowAhsAHl8SFNAQETABHYAMP+AD4EDgDLIPXLEfHLET0g9XkiIyIjyeUhD//LhstGKPzhyc2XAAUg+sngEz6H4BTJPEK5pbmlQjwAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAADgUA==");
    private byte[] cgbBootRom = Base64.getDecoder().decode("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");
    private byte[][] wram = new byte[8][0x1000]; 
    private int wramBank = 1;
    private int[] vramBank0 = new int[0x2000];
    private int[] vramBank1 = new int[0x2000];
    
    public static final int IF_REGISTER = 0xFF0F;
    public static final int IE_REGISTER = 0xFFFF;
    public static final int DIV_REGISTER = 0xFF04;
    public static final int TIMA_REGISTER = 0xFF05;
    public static final int TMA_REGISTER = 0xFF06;
    public static final int TAC_REGISTER = 0xFF07;
    public static final int DMA_REGISTER = 0xFF46;
    public static final int LY_COMPARE_REGISTER = 0xFF45;
    public static final int STEREO_SOUND_REGISTER = 0xFF25;
    public static final int VRAM_BANK_SELECT_REGISTER = 0xFF4F;
    public static final int CGB_DMA_SOURCE_HIGH = 0xFF51;
    public static final int CGB_DMA_SOURCE_LOW = 0xFF52;
    public static final int CGB_DMA_DESTINATION_HIGH = 0xFF53;
    public static final int CGB_DMA_DESTINATION_LOW = 0xFF54;
    public static final int CGB_DMA_START = 0xFF55;
    
    private int dmaSourceHigh;
    private int dmaSourceLow;
    private int dmaDestHigh;
    private int dmaDestLow;
    private int dmaSource;
    private int dmaDest;
    private int bytesToTransfer;
    private boolean hBlankDMA;

    
    boolean DEBUG = false;
    private boolean bootRomEnabled = true;
    private CPU cpu;
    private IPPU ppu;
    private transient Joypad joypad;
    private int currentVRAMBank = 0;
    private TileSetManager tileSetManager;
    private SpriteManager spriteManager;
    
    public void setSpriteManager(SpriteManager manager) {
        this.spriteManager = manager;
    }
    public void setTileSetManager(TileSetManager manager) {
        this.tileSetManager = manager;
    }
    
    
    public void setCPU(CPU cpu){
        this.cpu = cpu;
    }
    
    public CPU getCPU() {
        return this.cpu;
    }

    public Joypad getJoypad() {
        return joypad;
    }

    public void setPPU(IPPU ppu) {
        this.ppu = ppu;
    }
    
    public IPPU getPPU() {
        return this.ppu;
    }
    
    public Cartridge getROM() {
        return this.rom;
    }
    
    /* ロムから読み出し */ 
    public MMU(String fileName) {
        this.rom = Cartridge.fromFile(fileName);
    }
    
    public MMU(String fileName, SourceDataLine sourceDL){
        this.rom = Cartridge.fromFile(fileName);
    }
    
    public void cleanUp() {
        if (rom != null) {
            rom.cleanUp();
        }
    }
    
    public void setJoypad(Joypad joypad) {
        this.joypad = joypad;
    }
    
    /* VRAMとのやり取り */
    public int readByteFromVRAM(int location, int bank) {
        if (location < 0x8000 || location > 0x9FFF) {
            System.out.printf("Invalid VRAM address: %x\n", location);
        }
        
        int index = location % 0x8000;
        if (bank == 0) {
            return vramBank0[index];
        }
        else {
            return vramBank1[index];
        }
    }
    
    public void writeByteToVRAM(int location, int data, int bank) {
        if (bank != 0 && bank != 1) {
            throw new IllegalArgumentException("invalid vram bank");
        }
        if (location >= 0x8000 && location <= 0x97FF) {
            tileSetManager.updateTileSets(location, data, 0);
        }
        int index = location % 0x8000;
        if (bank == 0) {
            vramBank0[index] = data;
        }
        else {
            vramBank1[index] = data;
        }
    }
    
    private boolean withinCgbBootRom(int location){
        return  (location >= 0x000 && location <= 0x0ff) ||
                (location >= 0x200 && location <= 0x8ff);
    }
    
    public int slowReadByte(int location) {
        GameBoy.getInstance().clockTick(4);
        return readByte(location);
    }
    
    public int readByte(int location) {
        if (bootRomEnabled) {
            if(location < bootRom.length) {
                return bootRom[location] & 0xff;
            }
        }
        
        if (location == 0xFF68) {
            System.out.println("requested read of index bg");
        }
        
        if (location == 0xFF69) {
            System.out.println("requested read of data bg");
        }
        
        if (location == 0xFF6A) {
            System.out.println("requested read of index sprite");
        }
        
        if (location == 0xFF6B) {
            System.out.println("requested read of data sprite");
        }
        
        
        if (location >= 0xC000 && location <= 0xCFFF) {
            int wramLocation = location % 0xC000;
            return wram[0][wramLocation] & 0xFF;
        }
        
        if (location >= 0xD000 && location <= 0xDFFF) {
            int wramLocation = location % 0xD000;     
            return wram[1][wramLocation] & 0xFF;
        }
        
        if(location < 0x8000){
            return rom == null? 0 : rom.readByte(location);
        }
        
        if (location >= 0xA000 && location <= 0xBFFF) {
            return rom == null? 0 : rom.readByte(location);
        }
        
        if (location >= 0x8000 && location <= 0x9FFF) {
            return readByteFromVRAM(location, currentVRAMBank);
        }
        
        if(location == DIV_REGISTER){
            return cpu.timer.getDIV();
        }
        
        if(location == TIMA_REGISTER){
            return cpu.timer.getTIMA();
        }
        
        if(location == 0xFF00){
            
            if (Bit.extract(mem[0xFF00] & 0xff, 5, 5) == 0) {
                return joypad.readButtons();
            }
            
            if(Bit.extract(mem[0xFF00] & 0xff, 4, 4) == 0) {
                return joypad.readDirections();
            }
            else {
                return 0xFF;
            }
        }
        
        return mem[location] & 0xff;
    }
    
    public void memdump(int startLocation, int numBytes){
        int endLocation = startLocation + numBytes;
        int trueStartLocation = (startLocation / 8) * 8;
        endLocation = (endLocation + 7) / 8 * 8; 
        
        for(int i = trueStartLocation; i < endLocation; i++){
            if(i % 8 == 0){
                System.out.printf("%04x: ", i);
            }
            
            if(i - startLocation >= 0 && i - startLocation < numBytes){
                System.out.print("\033[34m");
            }else{
                System.out.print("\033[0m");
            }
            
            System.out.printf("%02x ", readByte(i));
            
            if(i % 8 == 7){
                System.out.println();
            }
            
            System.out.print("\033[0m");
        }

        System.out.print("\033[0m");
    }
    
    public int slowReadWord(int location) {
        return (slowReadByte(location+1) << 8) + slowReadByte(location);
    }
    
    public int readWord(int location) {
        return (readByte(location+1) << 8) + readByte(location);
    }

    public void slowWriteByte(int location, int toWrite) {
        GameBoy.getInstance().clockTick(4);
        writeByte(location, toWrite);
    }
    
    public void writeByte(int location, int toWrite){
        if(location == 0xff50){
            bootRomEnabled = false;
        }
        
        if(location < 0x7fff){
            if(rom != null) rom.writeByte(location, toWrite);
            return;
        }
        
        if (location >= 0xA000 && location <= 0xBFFF) {
            if(rom != null) rom.writeByte(location, toWrite);
            return;
        }
        
        if (location >= 0x8000 && location <= 0x9FFF) {
            writeByteToVRAM(location, toWrite, currentVRAMBank);
            return;
        }
        

        if (location == 0xFF70) {
            wramBank = toWrite;
            if (wramBank == 0) {
                wramBank = 1;
            }
        }

        if (location >= 0xC000 && location <= 0xCFFF) {
            int wramLocation = location % 0xC000;
            wram[0][wramLocation] = (byte) (toWrite & 0xFF);
            return;
        }

        if (location >= 0xD000 && location <= 0xDFFF) {
            int wramLocation = location % 0xD000;
            wram[1][wramLocation] = (byte) (toWrite & 0xFF);   
            return;
        }
        
        if(location == IF_REGISTER) { 
            boolean interrupted = cpu.interruptHandler.handleIF(toWrite);
            if (interrupted) return;
        }
        
        if(location == IE_REGISTER) { 
            cpu.interruptHandler.handleIE(toWrite);
        }
        
        if(location == DMA_REGISTER) { 
            int sourceBegin = toWrite << 8;
            int destBegin = 0xfe00;
            for(int i = 0; i < 256; i++){
                this.writeByte(destBegin + i, this.readByte(sourceBegin + i));
            }
            
        }
        
        if(location == TMA_REGISTER){ 
            cpu.timer.setModulo(toWrite);
        }
        
        if(location == TIMA_REGISTER){
            cpu.timer.setTIMA(toWrite);
        }
        
        if(location == DIV_REGISTER){
            cpu.timer.resetDIV();
        }
        
        if(location == TAC_REGISTER){
            cpu.timer.handleTAC(toWrite);
        }
        
        if(location == LY_COMPARE_REGISTER){
            ppu.setLYCompare(toWrite);
        }
        
        
        mem[location] = (byte)(toWrite & 0xFF);
        
        if(DEBUG && location == 0xff44){
            System.out.printf("Wrote to LY: %x\n", mem[location] & 0xff);
        }
        if(DEBUG && location == 0xFF42) {
            System.out.printf("Wrote to Scroll Y: %x\n", mem[location] & 0xff);
        }
    }
    
    public void writeWord(int location, int toWrite) {
        toWrite &= 0xffff;
        writeByte(location, toWrite & 0xff);
        writeByte(location + 1, toWrite >> 8);
    }
    
    public void hBlankDMA() {
        if (hBlankDMA) {
            for (int i = 0; i < 16; i++) {
                this.writeByte(dmaDest, this.readByte(dmaSource + i));
                dmaDest++;
            }
            dmaSource += 16;
            bytesToTransfer -= 16;
            if (bytesToTransfer == 0) {
                hBlankDMA = false;
            }
        }
    }
    

    class Location implements ReadWritable{
        private int address;

        public Location(int address){
            this.address = address;
        }

        @Override
        public int read() {
            return MMU.this.slowReadByte(address);
        }

        @Override
        public void write(int val) {
            MMU.this.slowWriteByte(address, val);
        }
        
        public void writeLong(int val) {
            MMU.this.slowWriteByte(address, val & 0xff);
            MMU.this.slowWriteByte(address + 1, (val >> 8) & 0xff);
        }
    }

    public Location shortRegisterLocation(Register r) {
        return new Location(0xff00 + r.read());
    }
    
    public Location registerLocation(Readable r) {
        return new Location(r.read());
    }

    public ReadWritable a8Location(Register pc){
        int address = 0xff00;
        address += slowReadByte(pc.read()+1);
        
        return new Location(address);
    }

    public ReadWritable a16Location(Register pc) {
        int address = slowReadWord(pc.read()+1);
        
        return new Location(address);
    }
    
    public void writeBytes(int location, byte[] sequence) {
        for(int i = 0; i < sequence.length; i++){
            writeByte(location + i, sequence[i]);
        }
    }
    
    public boolean match(int location, byte[] sequence) {
        for(int i = 0; i < sequence.length; i++){
            if(location + i > 0xffff) {
                return false;
            }
            if(readByte(location + i) != sequence[i]) {
                return false;
            }
        }
        return true;
    }
    
    public List<Integer> search(byte[] sequence) {
        LinkedList<Integer> candidates = new LinkedList<>();
        for(int i = 0; i <= 0xffff; i++) {
            if (match(i, sequence)) {
                candidates.addLast(i);
            }
        }
        return candidates;
    }
    
    public List<Integer> filter(List<Integer> candidates, byte[] sequence) {
        LinkedList<Integer> result = new LinkedList<>();
        for(int location : candidates) {
            if(match(location, sequence)) {
                result.add(location);
            }
        }
        return result;
    }
}
