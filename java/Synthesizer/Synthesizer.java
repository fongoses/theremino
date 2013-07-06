package Synthesizer;

// fmSynthesizer
//
// This is the 8-bit version
// Start experimenting with M:C*10 ratinos of 10, 20, 5, the most important ones.
import java.awt.*;
import java.awt.event.*;
import static java.lang.Math.*;
import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.event.*;



class Synthesizer extends Frame implements LineListener, ChangeListener {

    static final String windowName =
            "fmSynthesizer base on JJFM Synthesizer";
    public JPanel panel;
    public JSlider sliderRatio;
    public JSlider sliderIntensity;
    public JSlider sliderFrequency;
    public JSlider sliderVolume;
    public JLabel labelRatio;
    public JLabel labelIntensity;
    public JLabel labelFrequency;
    public JLabel labelVolume;

    public Synthesizer() {
        super(windowName);
        setLayout(new BorderLayout());
        setSize(240, 240);

        panel = new JPanel();
        panel.setLayout(new GridLayout(10, 1));
        add(panel, BorderLayout.NORTH);

        labelRatio = new JLabel("Ratio:");
        panel.add(labelRatio);
        sliderRatio = new JSlider();
        sliderRatio.addChangeListener(this);
        panel.add(sliderRatio);

        labelIntensity = new JLabel("Intensity:");
        panel.add(labelIntensity);
        sliderIntensity = new JSlider(0, 1024);
        sliderIntensity.addChangeListener(this);
        panel.add(sliderIntensity);

        labelFrequency = new JLabel("Frequency:");
        panel.add(labelFrequency);
        sliderFrequency = new JSlider(110, 2096);
        sliderFrequency.addChangeListener(this);
        panel.add(sliderFrequency);

        labelVolume = new JLabel("Volume:");
        panel.add(labelVolume);
        sliderVolume = new JSlider(0, 100);
        sliderVolume.addChangeListener(this);
        panel.add(sliderVolume);

        setVisible(true);
    }

    // Colocar o volume master do SO no máximo.
    public void setVolume(int percent) {
        FloatControl volumeControl = (FloatControl) line.getControl(FloatControl.Type.VOLUME); // MASTER_GAIN or VOLUME
        // System.out.println("Máximo: " + volumeControl.getMaximum());
        
        volume = (int) (((float) volumeControl.getMaximum()) * ((float) percent) / 100.0);
        // System.out.println("Volume: " + volume);

        volumeControl.setValue(volume);
    }

    public void stateChanged(ChangeEvent e) {
        if (e.getSource() == sliderRatio) {
            MCratio = sliderRatio.getValue();
        }
        if (e.getSource() == sliderIntensity) {
            intensity = sliderIntensity.getValue();
        }
        if (e.getSource() == sliderFrequency) {
            freq = sliderFrequency.getValue();
        }
        if (e.getSource() == sliderVolume) {
            setVolume(sliderVolume.getValue());
        }
        System.out.println("M:C Ratio * 10 = " + MCratio 
                + " | Intensity = " + intensity 
                + " | Frequency = " + freq
                + " | Volume = " + volume);
    }

    public static void main(String args[]) {
        Synthesizer fm = new Synthesizer();

        fm.init();

        fm.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        fm.process();
    }
    
    public byte buffer[];
    public int bufferSize = 4096;
    public int time = 0;
    public byte table[];
    public final int TABLE_SIZE = 1024;
    public int rate = 44100;
    public int freq = 440;
    public int volume = 50;
    public int span = rate / freq;
    public int fraction = (TABLE_SIZE / span);  // VELOCITY IN TABLE ENTRIES
    public byte modulator;
    // public int modulator;
    public int intensity = 512;
    public int MCratio = 50;
    public byte oscilator;
    // public int oscilator;
    public SourceDataLine line;
    // public int value;
    public int value;

    public void init() {
        AudioFormat format = new AudioFormat(rate, 16, 1, true, true);
        try {
            line = AudioSystem.getSourceDataLine(format);
            line.open(format, bufferSize);
            bufferSize = line.getBufferSize();
            System.out.println("Buffer is " + bufferSize);
            line.start();
        } catch (javax.sound.sampled.LineUnavailableException e) {
            System.out.println("Erro ao carregar Javax.");
        }
        line.addLineListener(this);
        buffer = new byte[bufferSize];

        table = new byte[TABLE_SIZE];
        for (int i = 0; i < TABLE_SIZE; ++i) {
            table[i] = (byte) ((float) sin(i / (TABLE_SIZE / (2 * 3.14))) * 127);
            //System.out.println(table[i]);
        }

        System.out.println("span " + span);
        System.out.println("frac " + fraction);
    }

    public void update(LineEvent event) {
    }

    public void process() {
        int i;
        while (true) {
            for (i = 0; i < bufferSize; i += 2, ++time) {
                span = rate / freq;
                fraction = (TABLE_SIZE / span); 

                // Note that time (in samples) is associated to table entries. So if you increase the size
                // of the sin() table, you have to adjust the intensity slider or intensity divisor to get
                // about the same modulation effect.
                // The addition of 100000 allows the circular access to the table, necessary at the beginning 
                // as negative values are generated by the modulator. 
                // In VHDL, we shall just use a fixed number of bits to address the table (must check that 
                // the two's complement can access it linearly, if not, change de coding). We should also use
                // all divisions by powers of 2, just ignoring the least significant bits.
                try {
                    modulator = table[((time * fraction * MCratio / 10) + 100000) % TABLE_SIZE];
                    oscilator = table[((time * fraction + modulator * intensity / 127) + 100000) % TABLE_SIZE];                   
                }
                catch(ArrayIndexOutOfBoundsException e) {
                    // System.out.println("Erro: "+ e.getMessage());
                    // System.out.println("Modulator: "+modulator+" | table["+((time * fraction * MCratio / 10) + 100000) % TABLE_SIZE+"]");
                    // System.out.println("Oscilator: "+oscilator+" | table["+((time * fraction + modulator * intensity / 127) + 100000) % TABLE_SIZE+"]");
                    time = 0;
                }
                value = oscilator * 256; // just to increase output, which is 16bit in our computers.
                buffer[i] = (byte) ((value >>> 8) & 0xFF);
                buffer[i + 1] = (byte) (value & 0xFF);
            }
            line.write(buffer, 0, bufferSize);
        }
    }
}
  
// END