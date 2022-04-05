
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

import javax.sound.sampled.SourceDataLine;
import javax.swing.*;

class MainMenuBar extends MenuBar {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
    GameBoy gameBoy;

    public MainMenuBar(GameBoy gb) {
    
        this.gameBoy = gb;
        Menu fileMenu = new Menu("File");
        Menu controlMenu = new Menu("Control");
        Menu saveMenu = new Menu("Save");
        Menu loadMenu = new Menu("Load");
        
        MenuItem reset = new MenuItem("Reset", new MenuShortcut(KeyEvent.VK_R));
        reset.addActionListener((ActionEvent e) -> {
            gameBoy.pause();
            int n = JOptionPane.showConfirmDialog(
                gameBoy,
                "Are you sure?",
                "Confirm reset",
                JOptionPane.YES_NO_OPTION);
            
            if(n == JOptionPane.YES_OPTION) {
                gameBoy.switchRom(gameBoy.romFileName);
            }
            
            gameBoy.start();
        });
        
        MenuItem quickSave = new MenuItem("Quicksave", new MenuShortcut(KeyEvent.VK_S));
        MenuItem quickLoad = new MenuItem("Quickload", new MenuShortcut(KeyEvent.VK_L));
        MenuItem loadFile = new MenuItem("Load save file", new MenuShortcut(KeyEvent.VK_L, true));
        MenuItem snapshot = new MenuItem("Snapshot", new MenuShortcut(KeyEvent.VK_S, true));
        MenuItem loadAutosave = new MenuItem("Load previous auto-save", new MenuShortcut(KeyEvent.VK_Z));
        CheckboxMenuItem autoSaveToggle = new CheckboxMenuItem("Create auto-saves", gameBoy.autoSaveEnabled);
        quickSave.addActionListener((ActionEvent e) -> {
            gameBoy.queueSave("quicksave.gbsave");
        });
        quickLoad.addActionListener((ActionEvent e) -> {
            gameBoy.mmu.cleanUp();
            gameBoy.queueLoad("quicksave.gbsave");
        });
        snapshot.addActionListener((ActionEvent e) -> {
            gameBoy.queueSave("snapshot-" + DATE_FORMAT.format(new Date()) + ".gbsave");
        });
        loadFile.addActionListener((ActionEvent e) -> {
            gameBoy.mmu.cleanUp();
            gameBoy.pause();
            JFileChooser fc = new JFileChooser(new File(System.getProperty("user.dir")));
            int returnVal = fc.showOpenDialog(gameBoy);

            if(returnVal == JFileChooser.APPROVE_OPTION){
                String fileName = fc.getSelectedFile().getAbsolutePath();
                gameBoy.queueLoad(fileName);
            }

            gameBoy.start();
        });
        loadAutosave.addActionListener((ActionEvent e) -> {
            gameBoy.queueLoadPreviousAutoSave();
        });
        autoSaveToggle.addItemListener((ItemEvent e) -> {
            gameBoy.autoSaveEnabled = autoSaveToggle.getState();
        });
        
        MenuItem openRom = new MenuItem("Open ROM", new MenuShortcut(KeyEvent.VK_N));
        openRom.addActionListener((ActionEvent e) -> {
            gameBoy.pause();
            JFileChooser fc = new JFileChooser(new File(System.getProperty("user.dir")));
            int returnVal = fc.showOpenDialog(gameBoy);

            if(returnVal == JFileChooser.APPROVE_OPTION){
                String fileName = fc.getSelectedFile().getAbsolutePath();
                gameBoy.switchRom(fileName);
            }
            
            gameBoy.start();
        });
        
        MenuItem exit = new MenuItem("Exit");
        exit.addActionListener((ActionEvent e) -> {
            gameBoy.pause();
            int n = JOptionPane.showConfirmDialog(
                    gameBoy,
                    "Are you sure?",
                    "Confirm exit",
                    JOptionPane.YES_NO_OPTION);

            if(n == JOptionPane.YES_OPTION) {
                gameBoy.mmu.cleanUp();
                System.exit(0);
            }
            
            gameBoy.start();
        });

        MenuItem pause = new MenuItem("Pause", new MenuShortcut(KeyEvent.VK_P));
        pause.addActionListener((ActionEvent e) -> {
            if(!gameBoy.paused) {
                gameBoy.pause();
                pause.setLabel("Resume");
            }else{
                gameBoy.start();
                pause.setLabel("Pause");
            }
        });

        fileMenu.add(openRom);
        fileMenu.add(exit);
        controlMenu.add(pause);
        controlMenu.add(reset);
        saveMenu.add(quickSave);
        loadMenu.add(quickLoad);
        saveMenu.add(snapshot);
        loadMenu.add(loadFile);
        saveMenu.add(autoSaveToggle);
        loadMenu.add(loadAutosave);
        
        this.add(fileMenu);
        this.add(controlMenu);
        this.add(saveMenu);
        this.add(loadMenu);
    }
}

public class GameBoy extends JFrame{
    
    public static final String DEFAULT_ROM = "";
    public static final int NUM_FRAMES_PER_AUTOSAVE = 120;
    public static final int MAX_AUTOSAVES = 30;
    public static final int MAX_HISTORY = 100;
    public static final int NUM_FRAMES_PER_SPEEDCHECK = 30;

    HashSet<Integer> breakPoints = new HashSet<>();
    LinkedList<Integer> history = new LinkedList<>();
    MMU mmu;
    CPU cpu;
    IPPU ppu;
    GameBoyScreen gbs;
    String romFileName;
    boolean paused;
    boolean autoSaveEnabled = true;
    boolean haltEnabled = true;
    private boolean quickSave;
    private boolean quickLoad;
    Joypad joypad;
    
    boolean audioOn = true;
    boolean fastMode = false;
    long timeSinceSpeedCheck = -1;
    int framesSinceSpeedCheck = 0;
    
    Scanner fin = new Scanner(System.in);
    int numInstructonsUntilBreak = -1;
    long framesDrawn = 0;
    boolean breaked = false;
    
    private int numClocks = 0;
    
    OutputStream saveFile = null;
    InputStream loadFile = null;
    LinkedList<ByteArrayOutputStream> autoSaves = new LinkedList<>();
    
    List<Integer> hexEditorCandidates = null;
    
    SourceDataLine sourceDL;
    
    private static GameBoy gb;
    
    public static GameBoy getInstance() {
        return gb;
    }

    WindowListener listener = new WindowListener() {
        @Override
        public void windowActivated(WindowEvent e) { }

        @Override
        public void windowClosed(WindowEvent e) {
            mmu.cleanUp();
        }

        @Override
        public void windowClosing(WindowEvent e) { }

        @Override
        public void windowDeactivated(WindowEvent e) { }

        @Override
        public void windowDeiconified(WindowEvent e) { }

        @Override
        public void windowIconified(WindowEvent e) { }

        @Override
        public void windowOpened(WindowEvent e) { }
    };
    
    public void switchRom(String newRom) {
        this.romFileName = newRom;
        if(mmu != null) mmu.cleanUp();
        mmu = new MMU(newRom, sourceDL);
        cpu = new CPU(mmu);
        ppu = new PPU(mmu, gbs);
        
        mmu.setPPU(ppu);
        joypad = new Joypad(mmu, cpu.interruptHandler);
        gbs.addKeyListener(joypad);
    }
    
    public GameBoy(String fileName) {
        super();
        this.romFileName = fileName;
        BufferedImage img = new BufferedImage(160, 144, BufferedImage.TYPE_3BYTE_BGR);
        gbs = new GameBoyScreen(img);
        gbs.setDoubleBuffered(true);
        gbs.setPreferredSize(new Dimension(500, 500));
        gbs.setFocusable(true);
        this.add(gbs);
        this.setMenuBar(new MainMenuBar(this));
        this.pack();
        this.setTitle("GameBoy");
        this.setVisible(true);    
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        this.addWindowListener(listener);
        mmu = new MMU(fileName);
        cpu = new CPU(mmu);

        ppu = new PPU(mmu, gbs);
        mmu.setPPU(ppu);
        this.joypad = new Joypad(mmu, cpu.interruptHandler);
        gbs.addKeyListener(joypad);
        ppu.loadMap(true, true);
        quickSave = false;
        quickLoad = false;
    }
    
    public void saveState() {
        try {
            ObjectOutputStream saveState = new ObjectOutputStream(this.saveFile);
            saveState.writeObject(mmu);
            saveState.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void loadState() {
        gbs.removeKeyListener(this.mmu.getJoypad());
        try {
            ObjectInputStream saveState = new ObjectInputStream(this.loadFile);
            this.mmu = (MMU) saveState.readObject();
            this.cpu = mmu.getCPU();
            this.ppu = mmu.getPPU();
            this.mmu.setJoypad(new Joypad(this.mmu, this.cpu.interruptHandler));
            saveState.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        gbs.addKeyListener(this.mmu.getJoypad());
        ppu.setGBS(gbs);
    }
    
    public byte[] hexStringToBytes(String sequenceStr) {
        byte[] sequence = new byte[sequenceStr.length() / 2];
        for(int i = 0; i < sequenceStr.length(); i+=2) {
            sequence[i / 2] = (byte)Integer.parseInt(sequenceStr.substring(i, i+2), 16);
        }
        return sequence;
    } 
    
    public void tick() {
        gbs.setFocusable(true);
        if(numInstructonsUntilBreak < 0 && breakPoints.contains(cpu.regs.PC.read())){
            breaked = true;
        }

        if(breaked) {
            System.out.print("Suspended at " + Integer.toString(cpu.regs.PC.read(), 16) + ": ");
            String cmd = fin.next();
            if (cmd.equals("b")) {
                breakPoints.add(fin.nextInt(16));
                return;
            } else if (cmd.equals("d")) {
                breakPoints.remove(fin.nextInt(16));
                return;
            } else if (cmd.equals("c")) {
                breaked = false;
            } else if (cmd.equals("xc")) {
                cpu.coreDump();
                return;
            } else if (cmd.equals("xm")) {
                mmu.memdump(fin.nextInt(16), fin.nextInt());
                return;
            } else if (cmd.equals("sm")) {
                mmu.writeBytes(fin.nextInt(16), hexStringToBytes(fin.next()));
                return;
            } else if (cmd.equals("nm")) {
                breaked = false;
                numInstructonsUntilBreak = fin.nextInt();
            } else if (cmd.equals("xh")) {
                for (int i : history) {
                    System.out.printf("%x ", i);
                }
                System.out.println();
                return;
            } else if (cmd.equals("hes")) {
                String sequenceStr = fin.next();
                if (sequenceStr.length() % 2 != 0) {
                    System.out.println("Must give an integral number of bytes");
                    return;
                }
                byte[] sequence = hexStringToBytes(sequenceStr);
                
                System.out.println(Arrays.toString(sequence));

                if (hexEditorCandidates != null) {
                    hexEditorCandidates = mmu.filter(hexEditorCandidates, sequence);
                } else {
                    hexEditorCandidates = mmu.search(sequence);
                }

                System.out.println(hexEditorCandidates.size() + " candidates remaining. Type her to reset or hec to view candidates");
                return;
            } else if (cmd.equals("her")){
                hexEditorCandidates = null;
                return;
            } else if(cmd.equals("hec")) {
                if(hexEditorCandidates == null){
                    System.out.println("A search has not yet been performed. Use hes to perform a search.");
                }
                for(int candidate: hexEditorCandidates){
                    System.out.printf("%04x ", candidate);
                }
                System.out.println();
                return;
            } else if(!cmd.equals("n")){
                System.out.println("Command not recognized");
                return;
            }
        }

        if(history.size() >= MAX_HISTORY) {
            history.removeFirst();
        }
        history.addLast(cpu.regs.PC.read());
        cpu.executeOneInstruction(breaked, haltEnabled);
        if (quickSave) {
            saveState();
            quickSave = false;
        }
        if (quickLoad) {
            loadState();
            quickLoad = false;
        }

        if(numInstructonsUntilBreak >= 0){
            if(numInstructonsUntilBreak == 0){
                breaked = true;
            }
            numInstructonsUntilBreak --;
            System.out.println(numInstructonsUntilBreak);
        }
    }
    
    public void resetClocks() {
        numClocks = 0;
    }
    
    public int getClocks() {
        return numClocks;
    }
    
    public void clockTick(int ticks) {
        numClocks += ticks;
        for(int i = 0; i < ticks; i++) {
            ppu.tick();
           
            if (ppu.drewFrame()) {
                framesSinceSpeedCheck++;
                framesDrawn++;
                if (framesSinceSpeedCheck >= NUM_FRAMES_PER_SPEEDCHECK) {
                    this.framesSinceSpeedCheck = 1;
                    this.timeSinceSpeedCheck = System.currentTimeMillis();
                }
                long currentTime = System.currentTimeMillis();
                long deltaTime = currentTime - timeSinceSpeedCheck;
                if (!fastMode && deltaTime < 16 * framesSinceSpeedCheck) {
                    try {
                        Thread.sleep(16 * framesSinceSpeedCheck - deltaTime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if (framesDrawn % NUM_FRAMES_PER_AUTOSAVE == 0) {
                    this.queueAutoSaveIfEnabled();
                }
            }
            cpu.timer.tick();
        }
    }
    
    public void pause() {
        paused = true;
    }
    
    public void start() {
        if(mmu.getROM() == null){
            System.out.println("Invalid cartridge");
            return;
        }
        
        new Thread(() -> {
            paused = false;
            framesSinceSpeedCheck = NUM_FRAMES_PER_SPEEDCHECK;

            while (!paused) {
                this.tick();
            }
            mmu.cleanUp();
        }).start();
    }
    
    public void dispose() {
        this.pause();
        super.dispose();
    }

    public static void main(String[] args) throws InterruptedException {

        if(args.length > 0) {
            if(!args[0].equals("-d")) {
                gb = new GameBoy(args[0]);
            }else{
                gb = new GameBoy(DEFAULT_ROM);
            }
            
            if(args[args.length-1].equals("-d")) {
                Scanner fin = new Scanner(System.in);
                System.out.print("First breakpoint (hex): ");
                gb.breakPoints.add(fin.nextInt(16));
            }
        }else{
            gb = new GameBoy(DEFAULT_ROM);
        }
        
        gb.start();
    }
    
    public void queueAutoSaveIfEnabled() {
        if (!this.autoSaveEnabled) {
            return;
        }
        ByteArrayOutputStream autoSave = new ByteArrayOutputStream();
        this.saveFile = autoSave;
        autoSaves.addLast(autoSave);
        if (autoSaves.size() > MAX_AUTOSAVES) {
            autoSaves.removeFirst();
        }
        quickSave = true;
    }
    
    public void queueLoadPreviousAutoSave() {
        try {
            ByteArrayOutputStream autoSave = autoSaves.removeLast();
            this.loadFile = new ByteArrayInputStream(autoSave.toByteArray());
            quickLoad = true;
        } catch (NoSuchElementException e) {
            System.out.println("No auto-saves available!");
        }
    }
    
    public void queueSave(String fileName){
        try {
            this.saveFile = new FileOutputStream(fileName);
            quickSave = true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    
    public void queueLoad(String fileName) {
        try {
            this.loadFile = new FileInputStream(fileName);
            quickLoad = true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

}
