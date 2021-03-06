
class Rom implements Cartridge {

    private static final long serialVersionUID = -7294699536390467641L;
    byte[] rom;

    public Rom(byte[] rom) {
        this.rom = rom;
    }
    
    public int readByte(int location){
        if (location > rom.length) {
            return 0xff;
        }
        return rom[location] & 0xff;
    }
    
    public void writeByte(int location, int toWrite){
    }

    @Override
    public void cleanUp() {
        return;
    }

}
