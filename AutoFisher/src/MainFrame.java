import java.awt.AWTException;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.awt.MouseInfo;
import javax.swing.*;
import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.dispatcher.SwingDispatchService;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;


@SuppressWarnings("serial")
public class MainFrame extends JFrame
{
	public MainFrame()
	{
		this.setBounds(200, 200, 300, 300);
		this.setTitle("D2 AutoFish");
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.setResizable(false);
		//this.setLayout(null);
		
		MainPane p = new MainPane(this);
		this.add(p);
		p.requestFocusInWindow();
		
		this.pack();
	}


}

@SuppressWarnings("serial")
class MainPane extends JPanel implements NativeKeyListener
{
	private static PropertiesWrapper pw;
	private static ImageComparator ic;
	
	private HashMap<String, JButton> binds; //binds for key to button
	private HashMap<String, Runnable> rawBinds; //binds for key to method
	private static boolean bindsEnabled;
	
	private Thread fishThread;
	private Thread updateMouseXY;
	//the amount of shit that is static annoys me but im too lazy to change it
	private static JLabel mouseXY;
	private static JButton startButton;
	private static JButton stopButton;
	private static JTextField x1Field;
	private static JTextField y1Field;
	private static JTextField x2Field;
	private static JTextField y2Field;
	
	public MainPane(MainFrame f)
	{
		//set up fields
		pw = new PropertiesWrapper();
		ic = new ImageComparator();
		binds = new HashMap<String, JButton>();
		rawBinds = new HashMap<String, Runnable>();
		bindsEnabled = true;
		//everything else
		
		this.setLayout(null);
		this.setFocusable(true);
		this.setFocusTraversalKeysEnabled(true);
		this.setPreferredSize(new Dimension(300, 300));
		
		
		JCheckBox onTop = new JCheckBox("Window Always On Top");
		onTop.setBounds(0, 0, 200, 25);
		onTop.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) 
			{
				f.setAlwaysOnTop(onTop.isSelected());
			}
			
		});
		this.add(onTop);
		onTop.doClick();
		
		JCheckBox mouseOn = new JCheckBox("Enable Mouse Coords");
		mouseOn.setBounds(0, 30, 250, 25);
		mouseOn.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) 
			{
				if (mouseOn.isSelected())
				{
					updateMouseXY = new Thread(MainPane::updateMouseXY);
					updateMouseXY.start();
				}
				else
				{
					if(updateMouseXY != null)
					{
						updateMouseXY.interrupt();
						updateMouseXY = null;
					}
						
				}
				
			}
			
		});
		this.add(mouseOn);
		
		mouseXY = new JLabel("Mouse Coords: Disabled");
		mouseXY.setBounds(5, 40, 250, 50);
		this.add(mouseXY);
		
		x1Field = new JTextField(pw.getStringProp("x1"));
		x1Field.setBounds(0, 90, 50, 25);
		this.add(x1Field);
		
		y1Field = new JTextField(pw.getStringProp("y1"));
		y1Field.setBounds(50, 90, 50, 25);
		this.add(y1Field);
		
		JLabel coord1Label = new JLabel("coord1 auto set: " + pw.getStringProp("coordone"));
		coord1Label.setBounds(100, 90, 200, 25);
		this.add(coord1Label);
		addRawBind("coordone", this::setCoords1);
		
		x2Field = new JTextField(pw.getStringProp("x2"));
		x2Field.setBounds(0, 115, 50, 25);
		this.add(x2Field);
		
		y2Field = new JTextField(pw.getStringProp("y2"));
		y2Field.setBounds(50, 115, 50, 25);
		this.add(y2Field);
		
		JLabel coord2Label = new JLabel("coord2 auto set: " + pw.getStringProp("coordtwo"));
		coord2Label.setBounds(100, 115, 200, 25);
		this.add(coord2Label);
		addRawBind("coordtwo", this::setCoords2);
		
		JCheckBox lockCoords = new JCheckBox("Lock Coords");
		lockCoords.setBounds(0, 150, 150, 25);
		lockCoords.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) 
			{
				boolean s = !lockCoords.isSelected();
				x1Field.setEnabled(s);
				y1Field.setEnabled(s);
				x2Field.setEnabled(s);
				y2Field.setEnabled(s);
			}
			
		});
		this.add(lockCoords);
		lockCoords.doClick();
		
		JCheckBox enableBinds = new JCheckBox("Enable Binds");
		enableBinds.setBounds(150, 150, 150, 25);
		enableBinds.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) 
			{
				MainPane.bindsEnabled = enableBinds.isSelected();
			}
			
		});
		this.add(enableBinds);
		enableBinds.doClick();
		
		JButton saveCoords = new JButton("Save Coords");
		saveCoords.setBounds(0, 180, 150, 50);
		saveCoords.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) 
			{
				int x1, y1, x2, y2;
				try {
					x1 = Integer.parseInt(x1Field.getText());
					y1 = Integer.parseInt(y1Field.getText());
					x2 = Integer.parseInt(x2Field.getText());
					y2 = Integer.parseInt(y2Field.getText());
				}catch(NumberFormatException ex) {
					return;
				}
				pw.setIntProp("x1", x1);
				pw.setIntProp("y1", y1);
				pw.setIntProp("x2", x2);
				pw.setIntProp("y2", y2);
			}
			
		});
		this.add(saveCoords);
		
		startButton = new JButton("Start (" + pw.getStringProp("start") + ")");
		startButton.setBounds(0, 250, 150, 50);
		startButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) 
			{
				fishThread = new Thread(MainPane::fish);
				fishThread.start();
				startButton.setEnabled(false);
				stopButton.setEnabled(true);
			}
			
		});
		this.add(startButton);
		addBind("start", startButton);
		
		stopButton = new JButton("Stop (" + pw.getStringProp("stop") + ")");
		stopButton.setBounds(150, 250, 150, 50);
		stopButton.setEnabled(false);
		stopButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) 
			{
				fishThread.interrupt();
				fishThread = null;
			}
			
		});
		this.add(stopButton);
		addBind("stop", stopButton);
		
		
        try {
            //what the fuck is happening here
        	Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        	logger.setLevel(Level.OFF);
        	GlobalScreen.addNativeKeyListener(this);
            GlobalScreen.registerNativeHook();
            GlobalScreen.setEventDispatcher(new SwingDispatchService());
        } catch (NativeHookException ex) {
            ex.printStackTrace();
            System.exit(ERROR);
        }
		
		
		this.setVisible(true);
	}
	
	@Override
    public void nativeKeyPressed(NativeKeyEvent e) 
	{
		if(!bindsEnabled) return;
		//System.out.println("pressed: " + e.getKeyCode());
        binds.forEach((k, v) -> {
        	if(NativeKeyEvent.getKeyText(e.getKeyCode()).equals(k))
        		simulateButtonPress(v);
        });
        rawBinds.forEach((k, v) -> {
        	if(NativeKeyEvent.getKeyText(e.getKeyCode()).equals(k))
        		v.run();
        });
    }
	
	@Override
	public void nativeKeyReleased(NativeKeyEvent arg0) {}

	@Override
	public void nativeKeyTyped(NativeKeyEvent arg0) {}
	
	//magic code do not touch
	private void simulateButtonPress(JButton button) 
	{
        ActionListener[] listeners = button.getActionListeners();
        if (listeners.length > 0)
        {
            ActionEvent event = new ActionEvent(button, ActionEvent.ACTION_PERFORMED, "");
            for (ActionListener listener : listeners)
            {
                listener.actionPerformed(event);
            }
        }
    }
	
	private static void updateMouseXY()
	{
		try {
			while(!Thread.currentThread().isInterrupted())
			{
				PointerInfo pointerInfo = MouseInfo.getPointerInfo();
				if(pointerInfo != null) 
				{
			        Point point = pointerInfo.getLocation();

			        int x = (int) point.getX();
			        int y = (int) point.getY();
			        SwingUtilities.invokeLater(() -> {
			        	mouseXY.setText("Mouse Coords:  " + x + ", " + y);
			        });
				}
				Thread.sleep(250);
			}
		}catch(InterruptedException e) {
			e.printStackTrace();
			mouseXY.setText("Mouse Coords: Disabled");
		}
	}
	
	private void setCoords1()
	{
		if(!x1Field.isEnabled()) return;
		PointerInfo pointerInfo = MouseInfo.getPointerInfo();
        Point point = pointerInfo.getLocation();

        x1Field.setText(((int) point.getX()) + "");
        y1Field.setText(((int) point.getY()) + "");
	}
	
	private void setCoords2()
	{
		if(!x2Field.isEnabled()) return;
		PointerInfo pointerInfo = MouseInfo.getPointerInfo();
        Point point = pointerInfo.getLocation();

        x2Field.setText(((int) point.getX()) + "");
        y2Field.setText(((int) point.getY()) + "");
	}
	
	private void addBind(String key, JButton button)
	{
		String prop = pw.getStringProp(key);
		binds.put(prop, button);
	}
	
	private void addRawBind(String key, Runnable r)
	{
		String prop = pw.getStringProp(key);
		rawBinds.put(prop, r);
	}
	
	private static void fish()
	{
		try {
			int timeout = pw.getIntProp("timeout");
			int interval = pw.getIntProp("interval");
			int whiteThreshold = pw.getIntProp("white");
			double significance = pw.getDoubleProp("significance");
			int key = KeyEvent.getExtendedKeyCodeForChar(pw.getStringProp("fishbutton").charAt(0));

			Robot robot = new Robot();
			while(!Thread.currentThread().isInterrupted())
			{
				System.out.println("start");
				robot.keyPress(key);
				Thread.sleep(1000);
				robot.keyRelease(key);
				Thread.sleep(3000);
				
				System.out.println("initial");
				int x1 = Integer.parseInt(x1Field.getText());
				int y1 = Integer.parseInt(y1Field.getText());
				int x2 = Integer.parseInt(x2Field.getText());
				int y2 = Integer.parseInt(y2Field.getText());
				int left = Math.min(x1, x2);
		        int top = Math.min(y1, y2);
		        int width = Math.abs(x2 - x1);
		        int height = Math.abs(y2 - y1);
				BufferedImage initial = robot.createScreenCapture(new Rectangle(left, top, width, height));
				
				int timer = 0;
				BufferedImage comp = new Robot().createScreenCapture(new Rectangle(left, top, width, height));
				
				System.out.println("loopstart");
				do {
					System.out.println("   loop");
					comp = robot.createScreenCapture(new Rectangle(left, top, width, height));
					timer += interval;
					Thread.sleep(interval);
				} while(timer < timeout && ic.isFirstImageMoreWhite(initial, comp, whiteThreshold, significance));
				
				if(timer >= timeout)
				{
					System.out.println("timeout");
					throw new InterruptedException();
				}
				
				System.out.println("catch");
				robot.keyPress(key);
				Thread.sleep(100);
				robot.keyRelease(key);
				Thread.sleep(4000);
			}
		}catch(InterruptedException | AWTException e) {
			e.printStackTrace();
			startButton.setEnabled(true);
			stopButton.setEnabled(false);
			return;
		}
		
	}

}


class ImageComparator 
{
    public boolean isFirstImageMoreWhite(BufferedImage image1, BufferedImage image2, int whiteThreshold, double significance) 
    {
    	int totalPixels = image1.getHeight() * image1.getWidth();
        int whitePixels1 = 0;
        int whitePixels2 = 0;
        
        for (int y = 0; y < image1.getHeight(); y++) 
        {
            for (int x = 0; x < image1.getWidth(); x++) 
            {
                Color color1 = new Color(image1.getRGB(x, y));
                Color color2 = new Color(image2.getRGB(x, y));
                
                if (isWhite(color1, whiteThreshold)) 
                    whitePixels1++;
                
                if (isWhite(color2, whiteThreshold)) 
                    whitePixels2++;
            }
        }
        
        double whiteRatio1 = (double)(whitePixels1) / totalPixels;
        double whiteRatio2 = (double)(whitePixels2) / totalPixels;
        //System.out.println(whiteRatio1);
        //System.out.println(whiteRatio2);
        //System.out.println(whiteRatio1 > whiteRatio2);
        
        return (whiteRatio1 + significance) >= (whiteRatio2);
    }
    
    private boolean isWhite(Color color, int whiteThreshold) 
    {
        int red = color.getRed();
        int green = color.getGreen();
        int blue = color.getBlue();
        
        return (red >= whiteThreshold && green >= whiteThreshold && blue >= whiteThreshold);
    }
    
}


class PropertiesWrapper
{
	private Properties props;
	private File propFile;
	
	public PropertiesWrapper()
	{
		props = new Properties();
		try {
            propFile = FileLocator.getFileAdjacentToJar("config.properties");
            FileInputStream fileInputStream = new FileInputStream(propFile);
            props.load(fileInputStream);
        } catch (IOException e) {
            e.printStackTrace();
            
        }
	}
	
	public int getIntProp(String s)
	{
		return Integer.parseInt(props.getProperty(s));
	}
	
	public void setIntProp(String s, int i)
	{
		props.setProperty(s, i+"");
		try (FileOutputStream fileOutputStream = new FileOutputStream(propFile)) {
            props.store(fileOutputStream, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
	
	public String getStringProp(String s)
	{
		return props.getProperty(s);
	}
	
	public void setStringProp(String s, String v) {
	    props.setProperty(s, v);
	    try (FileOutputStream fileOutputStream = new FileOutputStream(propFile)) {
	        props.store(fileOutputStream, null);
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}
	
	public double getDoubleProp(String s)
	{
		return Double.parseDouble(props.getProperty(s));
	}
	
	public void setDoubleProp(String s, double d) {
	    props.setProperty(s, String.valueOf(d));
	    try (FileOutputStream fileOutputStream = new FileOutputStream(propFile)) {
	        props.store(fileOutputStream, null);
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}
	
}

//chatgpt gobleguk; i hate java
class FileLocator {
    public static File getFileAdjacentToJar(String fileName) {
        // Check if running from a JAR file
        if (isRunningFromJar()) {
            String jarPath = getJarPath();
            if (jarPath != null) {
                String parentDirectoryPath = new File(jarPath).getParent();
                File adjacentFile = new File(parentDirectoryPath, fileName);
                if (adjacentFile.exists()) {
                    return adjacentFile;
                }
            }
        } else { // Running from Eclipse or other IDE
            File adjacentFile = new File(fileName);
            if (adjacentFile.exists()) {
                return adjacentFile;
            }
        }

        return null;
    }

    private static boolean isRunningFromJar() {
        return FileLocator.class.getResource("FileLocator.class").toString().startsWith("jar:");
    }

    private static String getJarPath() {
        String jarPath = FileLocator.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        try {
            jarPath = java.net.URLDecoder.decode(jarPath, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return jarPath;
    }
}