import java.awt.AWTException;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.awt.MouseInfo;
import javax.swing.*;
import javax.swing.border.Border;

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
		super();
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
	private static boolean antiAFKEnabled;
	
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
	
	private static volatile int lastKeyPressed; //USED FOR REBINDING KEYS, DO NOT USE FOR ANYTHING ELSE (maybe you could idk)
	
	public MainPane(MainFrame f)
	{
		super();
		//set up fields
		pw = new PropertiesWrapper();
		ic = new ImageComparator();
		binds = new HashMap<String, JButton>();
		rawBinds = new HashMap<String, Runnable>();
		bindsEnabled = false;
		antiAFKEnabled = false;
		lastKeyPressed = 0;
		//everything else
		
		this.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
		this.setFocusable(true);
		this.setFocusTraversalKeysEnabled(true);
		
		
		JCheckBox onTop = new JCheckBox("Window Always On Top");
		gbc.gridx = 0;
		gbc.gridy = 0;
		onTop.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) 
			{
				f.setAlwaysOnTop(onTop.isSelected());
			}
			
		});
		this.add(onTop, gbc);
		onTop.doClick();
		
		JCheckBox mouseOn = new JCheckBox("Enable Mouse Coords");
		gbc.gridx = 0;
		gbc.gridy = 1;
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
		this.add(mouseOn, gbc);
		
		mouseXY = new JLabel("Mouse Coords: Disabled");
		gbc.gridx = 1;
		gbc.gridy = 1;
		this.add(mouseXY, gbc);
		
		//coord1pane
		JPanel coord1Pane = new JPanel();
		coord1Pane.setLayout(new BoxLayout(coord1Pane, BoxLayout.X_AXIS));
		
		x1Field = new JTextField(pw.getStringProp("x1"));
		coord1Pane.add(x1Field);
		
		y1Field = new JTextField(pw.getStringProp("y1"));
		coord1Pane.add(y1Field);
		//coord1pane end
		gbc.gridx = 0;
		gbc.gridy = 2;
		this.add(coord1Pane, gbc);
		
		JLabel coord1Label = new JLabel("coord1 auto set: " + pw.getStringProp("coordone"));
		gbc.gridx = 1;
		gbc.gridy = 2;
		this.add(coord1Label, gbc);
		addRawBind("coordone", this::setCoords1);
		
		//coord2pane
		JPanel coord2Pane = new JPanel();
		coord2Pane.setLayout(new BoxLayout(coord2Pane, BoxLayout.X_AXIS));
		
		x2Field = new JTextField(pw.getStringProp("x2"));
		
		coord2Pane.add(x2Field);
		
		y2Field = new JTextField(pw.getStringProp("y2"));
		coord2Pane.add(y2Field);
		//coord2pane end
		gbc.gridx = 0;
		gbc.gridy = 3;
		this.add(coord2Pane, gbc);
				
		JLabel coord2Label = new JLabel("coord2 auto set: " + pw.getStringProp("coordtwo"));
		gbc.gridx = 1;
		gbc.gridy = 3;
		this.add(coord2Label, gbc);
		addRawBind("coordtwo", this::setCoords2);
		
		JCheckBox lockCoords = new JCheckBox("Lock Coords");
		gbc.gridx = 0;
		gbc.gridy = 4;
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
		this.add(lockCoords, gbc);
		lockCoords.doClick();
		
		JCheckBox enableBinds = new JCheckBox("Enable Binds");
		gbc.gridx = 0;
		gbc.gridy = 5;
		enableBinds.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) 
			{
				MainPane.bindsEnabled = enableBinds.isSelected();
			}
			
		});
		this.add(enableBinds, gbc);
		enableBinds.doClick();
		
		JCheckBox enableAntiAFK = new JCheckBox("Enable Anti AFK");
		gbc.gridx = 1;
		gbc.gridy = 5;
		enableAntiAFK.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) 
			{
				MainPane.antiAFKEnabled = enableAntiAFK.isSelected();
			}
			
		});
		this.add(enableAntiAFK, gbc);
		enableAntiAFK.doClick();
		
		JButton saveCoords = new JButton("Save Coords");
		gbc.gridx = 0;
		gbc.gridy = 6;
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
		this.add(saveCoords, gbc);

		HashMap<String, String> convert = new HashMap<String, String>() {{
			put("Jump", "jump");
			put("Move Backward", "backward");
			put("Move Forward", "forward");
			put("Set Coord 2", "coordtwo");
            put("Set Coord 1", "coordone");
            put("Stop Button", "stop");
            put("Start Button", "start");
        }};
        
        //TODO: it is a miracle this actually works without exploding
		JButton updateConfig = new JButton("Update Binds");
		gbc.gridx = 1;
		gbc.gridy = 6;
		updateConfig.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) 
			{
				PointerInfo pointerInfo = MouseInfo.getPointerInfo();
				Point point = pointerInfo.getLocation();
		        int x = (int) point.getX();
		        int y = (int) point.getY();
				
				JDialog dialog = new JDialog(f, "Rebind", true);
				dialog.setLocation(x, y);
				
				JPanel cardPanel = new JPanel();
				CardLayout cardLayout = new CardLayout();
				cardPanel.setLayout(cardLayout);
				
				//first panel
				JPanel selectPanel = new JPanel();
				
				selectPanel.setLayout(new GridBagLayout());
	            GridBagConstraints gbc = new GridBagConstraints();
	            gbc.fill = GridBagConstraints.BOTH; // Fill horizontally
				
				JLabel label = new JLabel("What to rebind?");
				gbc.gridx = 0;
				gbc.gridy = 0;
				selectPanel.add(label, gbc);
				
				Border border = BorderFactory.createLineBorder(Color.BLACK);
				
		        JList<String> list = new JList<>(convert.keySet().toArray(new String[0]));
		        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		        list.setBorder(border);
				gbc.gridy = 1;
		        selectPanel.add(list, gbc);
		        
		        //second panel
		        JPanel rebindPanel = new JPanel();
		        rebindPanel.setLayout(new BoxLayout(rebindPanel, BoxLayout.Y_AXIS));
		        
		        JLabel rebindLabel = new JLabel("Press any key");
		        rebindPanel.add(rebindLabel);
		        
		        AtomicReference<Thread> keyUpdateRef = new AtomicReference<>();
		        AtomicReference<String> newKeyRebind = new AtomicReference<>();
		        
		        JButton okButton = new JButton("Confirm");
		        okButton.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) 
					{
						String selected = list.getSelectedValue();
						String prop = convert.get(selected);
						pw.setStringProp(prop, newKeyRebind.get());
						cardLayout.show(cardPanel, "Select Panel");
					}
		        	
		        });
		        rebindPanel.add(okButton);
		        
		        JButton cancelButton = new JButton("Cancel");
				cancelButton.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) 
					{
						if(keyUpdateRef.get() != null)
							keyUpdateRef.get().interrupt();
						cardLayout.show(cardPanel, "Select Panel");
					}
					
				});
		        rebindPanel.add(cancelButton);
		        //second panel done
		        
		        JButton button = new JButton("Rebind");
		        button.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) 
					{
						String selected = list.getSelectedValue();
						if(selected == null) return;
						
						cardLayout.show(cardPanel, "Rebind Panel");
						
						Thread keyUpdate = new Thread(() -> {
							try {
						        okButton.setEnabled(false);
						        rebindLabel.setText("Press any key");
								System.out.println("started rebind thread");
						        lastKeyPressed = -1;
						        while(lastKeyPressed == -1) //quick and dirty wait until lastkeypressed changes, shouldnt fuck cpu i hope
						        {
						        	Thread.sleep(50);
						        }
						        okButton.setEnabled(true);
						        newKeyRebind.set(NativeKeyEvent.getKeyText(lastKeyPressed));
						        rebindLabel.setText("<html>Rebind " + selected + "<br>to ( " + newKeyRebind.get() + " )?</html>"); //praise be html formatting
						        System.out.println("stopped rebind thread (natural)");
						        return;
					        } catch (InterruptedException e1) {
								e1.printStackTrace();
								System.out.println("stopped rebind thread (forced)");
								return;
							}
					        
						});
						keyUpdateRef.set(keyUpdate);
						keyUpdate.start();
						
					}
		        	
		        });
		        gbc.gridy = 2;
		        selectPanel.add(button, gbc);
		        //first panel done
		        
		        dialog.addWindowListener(new WindowAdapter() {
		        	
		        	@Override
		        	public void windowClosed(WindowEvent e)
		        	{
		        		if(keyUpdateRef.get() != null)
		        			keyUpdateRef.get().interrupt();
		        	}
		        	
		        });
		        
		        cardPanel.add(selectPanel, "Select Panel");
		        cardPanel.add(rebindPanel, "Rebind Panel");
		        dialog.setContentPane(cardPanel);
		        
		        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		        dialog.setResizable(false);
		        dialog.pack();
		        dialog.setVisible(true);
			}
			
		});
		this.add(updateConfig, gbc);
		
		startButton = new JButton("Start (" + pw.getStringProp("start") + ")");
		gbc.gridx = 0;
		gbc.gridy = 7;
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
		this.add(startButton, gbc);
		addBind("start", startButton);
		
		stopButton = new JButton("Stop (" + pw.getStringProp("stop") + ")");
		gbc.gridx = 1;
		gbc.gridy = 7;
		stopButton.setEnabled(false);
		stopButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) 
			{
				fishThread.interrupt();
				fishThread = null;
			}
			
		});
		this.add(stopButton, gbc);
		addBind("stop", stopButton);
		
		
        try {
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
        
        lastKeyPressed = e.getKeyCode();
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
	
	private static int getKeyCode(String keyName) 
	{
        try {
            int keyCode = KeyEvent.class.getField("VK_" + keyName.toUpperCase()).getInt(null);
            return keyCode;
        } catch (NoSuchFieldException | IllegalAccessException | IllegalArgumentException ex) {
            ex.printStackTrace();
            return -1;
        }
    }
	
	private static void fish()
	{
		Robot robot = null;
		int key = 0;
		int forwardKey = 0;
		int backwardKey = 0;
		int jumpKey = 0;
		try {
			int timeout = pw.getIntProp("timeout");
			int interval = pw.getIntProp("interval");
			int afkInterval = pw.getIntProp("afkinterval");
			int afkTimer = 0;
			int whiteThreshold = pw.getIntProp("white");
			double significance = pw.getDoubleProp("significance");
			key = getKeyCode(pw.getStringProp("fishbutton"));
			forwardKey = getKeyCode(pw.getStringProp("forward"));
			backwardKey = getKeyCode(pw.getStringProp("backward"));
			jumpKey = getKeyCode(pw.getStringProp("jump"));
			
			robot = new Robot();
			while(!Thread.currentThread().isInterrupted())
			{
				long startTime = System.currentTimeMillis();
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
				
				long endTime = System.currentTimeMillis();
				long timeDiff = endTime - startTime;
				afkTimer += timeDiff;
				if(afkTimer >= afkInterval)
				{
					afkTimer = 0;
					if(MainPane.antiAFKEnabled)
					{
						System.out.println("antiafk");
						int holdTime = (int)(Math.random() * 25) + 25;
						int waitTime = (int)(Math.random() * 35) + 30;
						robot.keyPress(forwardKey);
						Thread.sleep(holdTime);
						robot.keyRelease(forwardKey);
						Thread.sleep(waitTime);
						robot.keyPress(backwardKey);
						Thread.sleep(holdTime);
						robot.keyRelease(backwardKey);
						Thread.sleep(waitTime);
						robot.keyPress(jumpKey);
						Thread.sleep(waitTime + holdTime);
						robot.keyRelease(jumpKey);
						Thread.sleep(3000);
						System.out.println("antiafkdone");
					}
				}
			}
		}catch(InterruptedException | AWTException | IllegalArgumentException e) {
			e.printStackTrace();
			startButton.setEnabled(true);
			stopButton.setEnabled(false);
			
			if(robot != null) //release keys in case they are down
			{
				robot.keyRelease(key);
				robot.keyRelease(forwardKey);
				robot.keyRelease(backwardKey);
				robot.keyRelease(jumpKey);
			}
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

//chatgpt gobleguk; i hate java stupid high level garbage, next bot im making is gonna use C
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