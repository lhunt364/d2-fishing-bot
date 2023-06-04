import javax.swing.SwingUtilities;

public class Main 
{
	public static void main(String[] args) 
	{
		SwingUtilities.invokeLater(() -> {
			MainFrame f = new MainFrame();
			f.setVisible(true);
		});
	}
}
