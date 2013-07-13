

// fmSynthesizer
//
// This is the 8-bit version
// Start experimenting with M:C*10 ratinos of 10, 20, 5, the most important ones.
import static java.lang.Math.sin;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import javax.bluetooth.RemoteDevice;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;



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
    
    //bluetooth
    static String macLinvor = "001212041025"; //mac do dispositivo sem a URL
	static int BLUETOOTH_SSMAX = 60;
	OutputStream os;
	InputStream is;
	InputStreamReader isr;
	StreamConnection con;
	String url;
	BufferedReader bufReader;
	RemoteDevice dev;
	byte bufferBt[];
	int bytes_read;
	int valueSonar1;
	int valueSonar2;
	int oldValueSonar1;
	int oldValueSonar2;
	Thread bluetoothThread;
	int threshold1 = 1;
	int distanciaMax = 1800;
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
        
        bluetoothThread = new ReadBluetooth("BluetoothThread");
        bluetoothThread.start();
        setVisible(true);
        
        
    }

    // Colocar o volume master do SO no máximo.
    public synchronized void setVolume(int percent) {
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
    private class ReadBluetooth extends Thread {
    	//static String macLinvor = "001212041025"; //mac do dispositivo sem a URL
    	//static int BLUETOOTH_SSMAX = 60;
    	byte buffer[];
    	int bytes_read;
    	String received; //received data converted into a string
    	int valueSonar1;
    	
    	public ReadBluetooth(String str) {
        	super(str);
        }
        
        public synchronized void run() {
        	try {	
        	String url = "btspp://"+macLinvor+":1";   		
       		System.out.println("Connecting to the 'smart' remote Device, with the url: "+ url);
            StreamConnection con = 
                (StreamConnection) Connector.open(url);
            OutputStream os = con.openOutputStream();
            InputStream is = con.openInputStream();
            
            System.out.println("Client started");
            InputStreamReader isr = new InputStreamReader(System.in);
            BufferedReader bufReader = new BufferedReader(isr);

        
            RemoteDevice dev = RemoteDevice.getRemoteDevice(con);
            buffer = new byte[BLUETOOTH_SSMAX];

         /**   if (dev !=null) {
             File f = new File("test.xml");
             InputStream sis = new FileInputStream("test.xml");
             OutputStream oo = new FileOutputStream(f);
             byte buf[] = new byte[1024];
             int len;
             while ((len=sis.read(buf))&gt;0)
              oo.write(buf,0,len);
             sis.close();
            }  **/
            
         if (con !=null) {
          while (true) {
        	  
        	  
            buffer[0]=0;
            buffer[1]=0;
           
        		
            bytes_read = is.read( buffer );
            
            if(bytes_read == 2){            	
            	if((buffer[1] & 192) == 192) {
            		//if(true){
            		/* Read bluetooth data from Sonar 1*/
            		valueSonar1 = (buffer[0]&0xff) + (((buffer[1]&0xff)  << 8) & 0x3ff); 
            		//System.out.println("Readed Value: " + valueSonar1+ " from:"+dev.getBluetoothAddress());
            		System.out.println("Readed Value [1]: " + valueSonar1);
            		//Som
    		        if (Math.abs(valueSonar1*threshold1 - oldValueSonar1) < 400){
    			        if ((valueSonar1 > 0) && (valueSonar1 < distanciaMax))  freq = valueSonar1*threshold1;
    			        	oldValueSonar1 = valueSonar1*threshold1;
    		        }
            	}}
            
            if(bytes_read == 1){ 
            	if((buffer[1] & 192) != 192) {
            	{
            		/* Read bluetooth data from Sonar 2*/
            		valueSonar2 = ((buffer[0]&0xff)*100)/20;
            		System.out.println("Readed Value [2]: " + (buffer[0]&0xff) + " volume: "+valueSonar2+" %");            		
            		//Som do sonar 2
    		        //if (Math.abs(valueSonar1*threshold1 - oldValueSonar1) < 400){
            		//if (Math.abs(valueSonar2- oldValueSonar2) < 50){
            		if ((valueSonar2 > 0) && (valueSonar2 < 100)){ 
    			    	 setVolume((int) (valueSonar2));
    			    	 
            		//}   	
    		        }
            		
            	}
            }}
	        
	        
	        //Thread.sleep(30);	        
                       
          }}}  catch(Exception e){ e.printStackTrace();}
          
        	
        	
        	
        	
        }
    }
    
}
  
// END