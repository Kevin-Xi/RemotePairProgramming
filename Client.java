import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

public class Client extends JFrame{
	private JTextPane codeBlock=new JTextPane();
	private JPanel codeBlockPanel=new JPanel();
	private	JPanel controlPanel=new JPanel();
	private	JButton connectButton=new JButton("Connect");

	private String address="localhost";

	private DataOutputStream toServer;
	private DataInputStream fromServer;

	private Socket socket;

	private boolean flag=false;

	public static void main(String[] args) {
		new Client();
	}

	public Client(){
		createGUI();
	}

	private void createGUI(){
        codeBlock.setBackground(Color.BLACK);
        codeBlock.setForeground(Color.GREEN);
        codeBlock.setFont(new Font("LucidaBrightDemiItalic",Font.ITALIC,18));
        codeBlock.getDocument().addDocumentListener(new CodeBlockListener());
        
        codeBlockPanel.setLayout(new BorderLayout());
        JScrollPane scrollPane = new JScrollPane(codeBlock);
        codeBlockPanel.add(scrollPane,BorderLayout.CENTER);
        controlPanel.add(connectButton);
        connectButton.addActionListener(new ConnectListener());
        
        setLayout(new BorderLayout());
        add(controlPanel,BorderLayout.EAST);
        add(codeBlockPanel,BorderLayout.CENTER);

        setTitle("Remote Pair Programming---Client");
        setPreferredSize(new Dimension(500,400));
        setLocation(250,150);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
        pack();
    }

	private class CodeBlockListener implements DocumentListener{
        public void insertUpdate(DocumentEvent e) {
        	System.out.println("inserted.LOL"+e.getLength()+flag);
        	if(!flag)
        	try{
				toServer.writeUTF(codeBlock.getText().trim());
				toServer.flush();
				flag=true;
			}catch(IOException ex){
				System.err.println(ex);
			}
        }
        public void removeUpdate(DocumentEvent e) {
        	System.out.println("removed.LOL"+e.getLength()+flag);
        	if(!flag)
        	try{
				toServer.writeUTF(codeBlock.getText().trim());
				toServer.flush();
				flag=true;
			}catch(IOException ex){
				System.err.println(ex);
			}
        }
        public void changedUpdate(DocumentEvent e) {
        	System.out.println("changed.LOL"+e.getLength());
        	try{
				toServer.writeUTF(codeBlock.getText().trim());
				toServer.flush();
			}catch(IOException ex){
				System.err.println(ex);
			}
        }
    }

    private class ConnectListener implements ActionListener{
		public void actionPerformed(ActionEvent e){
			address=JOptionPane.showInputDialog("Please input IP address or hostname: ");
			System.out.println(address);
			try{
				socket=new Socket(address,8000);
				fromServer=new DataInputStream(socket.getInputStream());
				toServer=new DataOutputStream(socket.getOutputStream());
				Thread thread=new Thread(new Runnable(){
					public void run(){
						try{
							while(fromServer.available()!=0){
								flag=true;
								codeBlock.setText(fromServer.readUTF());
							}
						}catch(IOException ex){
							System.err.println(ex);
						}
					}
				});

				thread.start();
			}catch(UnknownHostException ex){
					System.err.println(ex);
					JOptionPane.showMessageDialog(null,"Unknown IP address or hostname. Please check again.","ERROR",JOptionPane.ERROR_MESSAGE);
			}catch(ConnectException ex){
					System.err.println(ex);
					JOptionPane.showMessageDialog(null,"Unable to connect with target. Please check web connection.","ERROR",JOptionPane.ERROR_MESSAGE);
			}catch(IOException ex){
					System.err.println(ex);
			}
		}
	}
}