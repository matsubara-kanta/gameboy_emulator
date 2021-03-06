
import java.io.Serializable;

public class Timer implements Serializable {
    private MMU mmu;
    public static final int[] PERIODS = new int[] { 
            1024, 
            16, 
            64, 
            256 
    };
    public static final int DIV_PERIOD = 256;
    
    private boolean timerEnabled = false;
    private int currentClock = 0;
    private int modulo = 0;
    
    private int counter = 0; 
    private int countRegister = 0; 
    private int divCounter = 0; 
    private int divRegister = 0; 
    
    public Timer(MMU mmu) {
        this.mmu = mmu;
    }
    
    public void tick() {
        divCounter++;
        if(divCounter >= DIV_PERIOD){
            divRegister = (divRegister + 1) & 0xff;
            divCounter = 0;
        }
        
        if(timerEnabled){
            counter++;
            if(counter >= PERIODS[currentClock]){
                countRegister++;
                if(countRegister > 0xff){
                    countRegister = modulo;
                    mmu.writeByte(MMU.IF_REGISTER, 0b100);
                }
                counter = 0;
            }
        }
    }
    
    public void handleTAC(int TAC) {
        this.timerEnabled = ((TAC >> 2) & 1) == 1;
        
        this.currentClock = (TAC & 0x3);
    }
    
    public void setModulo(int modulo){
        this.modulo = modulo & 0xff;
    }
    
    public void setTIMA(int countRegister){
        this.countRegister = countRegister;
    }
    
    public void resetDIV(){
        this.divRegister = 0;
    }
    
    public int getDIV() {
        return divRegister;
    }
    
    public int getTIMA() {
        return countRegister;
    }
}
