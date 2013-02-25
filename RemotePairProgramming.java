import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

public class RemotePairProgramming extends JFrame{

	/* Component*/
	private JTextPane codeBlock=new JTextPane();
	private JScrollPane scrollPane = new JScrollPane(codeBlock,
		JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
	private JPanel codeBlockPanel=new JPanel();

	private	JButton connectButton=new JButton("Connect");
	private JButton getKeyboardButton=new JButton("Get Keyboard");
	private JButton aboutButton=new JButton("About");
	private	JPanel controlPanel=new JPanel();
	
	private JLabel statusLable=new JLabel("No active connection.");

	private JFrame frame=this;

	/* Networking*/
	private Thread handleConnectionRequest;

	private Socket msgSocket;
	private ServerSocket msgServerSocket;
	private DataOutputStream msgOutputStream;
	private DataInputStream msgInputStream;

	private Socket dataSocket;
	private ServerSocket dataServerSocket;
	private DataOutputStream dataOutputStream;
	private DataInputStream dataInputStream;

	private String address="localhost";

	private boolean canIInput=false;

	private Thread setTextThread;
	private Thread getKeyboardThread;

	/* ctor */
	public RemotePairProgramming(){
		createGUI();
		addListeners();
		initServerSocket();
	}

	/* use in ctor */
	private void createGUI(){

		/* code block */
        codeBlock.setBackground(Color.BLACK);
        codeBlock.setForeground(Color.GREEN);
        codeBlock.setFont(new Font("LucidaBrightDemiItalic",Font.ITALIC,18));
        codeBlock.setEditable(canIInput);
        
        codeBlockPanel.setLayout(new BorderLayout());
        codeBlockPanel.add(scrollPane,BorderLayout.CENTER);

        /* control panel */
        getKeyboardButton.setEnabled(false);

        controlPanel.setLayout(new GridLayout(11,1));
        controlPanel.add(connectButton);
        controlPanel.add(getKeyboardButton);
        controlPanel.add(aboutButton);
                
        /* main frame */
        setLayout(new BorderLayout());
        add(codeBlockPanel,BorderLayout.CENTER);
        add(controlPanel,BorderLayout.EAST);
        add(statusLable,BorderLayout.SOUTH);    

		setTitle("Remote Pair Programming");
        setPreferredSize(new Dimension(500,400));
        setLocation(250,150);
        setVisible(true);
        pack();
    }

    private void addListeners(){

    	codeBlock.getDocument().addDocumentListener(new CodeBlockListener());

        connectButton.addActionListener(new ConnectButtonListener());

        getKeyboardButton.addActionListener(new GetKeyboardButtonListener());

        aboutButton.addActionListener(new AboutButtonListener());

        frame.addWindowListener(new CloseWindowListener());
    }

    private void initServerSocket(){
    	try{   
            msgServerSocket=new ServerSocket(8000);
            statusLable.setText("ServerSocket started at "+new Date());

            handleConnectionRequest=new Thread(new HandleConnectionRequest());
            handleConnectionRequest.start();
        }catch(IOException ex){
            System.err.println(ex);
        }
    }

    private class HandleConnectionRequest implements Runnable{
    	public void run(){
    		try{
		    	msgSocket=msgServerSocket.accept();
		        InetAddress inetAddress=msgSocket.getInetAddress();
		        msgOutputStream=new DataOutputStream(msgSocket.getOutputStream());

		        int willIAccept=JOptionPane.showConfirmDialog(frame,
		        	"The host:"+inetAddress.getHostName()+"("+inetAddress.getHostAddress()+
		        	")intend to establish a connection. Accept?",
		        	 "Connection Request Come In",JOptionPane.YES_NO_OPTION);

		        if(willIAccept==JOptionPane.YES_OPTION){
		        	msgOutputStream.writeUTF("A");
		            msgOutputStream.flush();

		            msgInputStream=new DataInputStream(msgSocket.getInputStream());

		            dataServerSocket=new ServerSocket();
		            dataServerSocket.bind(new InetSocketAddress(inetAddress,8001));
		            dataSocket=dataServerSocket.accept();
		            dataInputStream=new DataInputStream(dataSocket.getInputStream());
			        dataOutputStream=new DataOutputStream(dataSocket.getOutputStream());        

			        if(setTextThread==null||!setTextThread.isAlive()){
			        	setTextThread=new Thread(new SetTextThread());
			        	setTextThread.start();
			        }
		       
			        connectButton.setText("Disconnect");
			        changeStates();
			        statusLable.setText("Connection with "+inetAddress.getHostName()+
			        	"("+inetAddress.getHostAddress()+") established.");
			    }
			    else{
			    	msgOutputStream.writeUTF("D");
		            msgOutputStream.flush();
		            msgOutputStream.close();
		            msgSocket.close();

		            /* Can This Done? */
			        handleConnectionRequest=new Thread(new HandleConnectionRequest());
			        handleConnectionRequest.start();
			    }
			}catch(IOException ex){
				System.err.println(ex);
			}
		}
    }

    private void changeStates(){
    	getKeyboardButton.setEnabled(!canIInput);
    	codeBlock.setEditable(canIInput);
    }

    private class SetTextThread implements Runnable{
    	public void run(){
	        try{
	        	System.out.println("setTextThread start");
	            while(!canIInput){
	                codeBlock.setText(dataInputStream.readUTF());
	            }
	        }catch(IOException ex){
	            System.err.println(ex);
	        }
	    }
    }

    private class GetKeyboardThread implements Runnable{
		public void run(){
			try{
			    System.out.println("getKeyboardThread start");
				while(true){
				    if(msgInputStream.readUTF().equals("K")){
					    System.out.println("inside getKeyboardThread");
					    int willIGiveKeyboard=JOptionPane.showConfirmDialog(frame,
					    	"Remote host intend to get keyboard. Agree?", 
					    	"GetKeyboard Request Come In",JOptionPane.YES_NO_OPTION);
					    if(willIGiveKeyboard==JOptionPane.YES_OPTION){
						    msgOutputStream.writeUTF("Y");
						    msgOutputStream.flush();

						    if(setTextThread==null||!setTextThread.isAlive()){
						    	setTextThread=new Thread(new SetTextThread());
	        					setTextThread.start();
	        				}

							canIInput=false;
							changeStates();
							statusLable.setText("Give keyboard.");
						}
						else{
						    msgOutputStream.writeUTF("N");
						    msgOutputStream.flush();
						}
					}
				}
			}catch(IOException ex){
				System.err.println(ex);
			}
		}
	}

    /* use in listeners */
    private boolean disconnect(){
		if(msgSocket!=null&&!msgSocket.isClosed()){
			int willIDisconnect=JOptionPane.showConfirmDialog(frame,
				"The host:"+msgSocket.getInetAddress().getHostName()+
				"("+msgSocket.getInetAddress().getHostAddress()+
				") has established a connection. Is that your wish to disconnect?", 
				"Disconnect Affirmation",JOptionPane.YES_NO_OPTION);
			if(willIDisconnect==JOptionPane.YES_OPTION){
				try{
					canIInput=false;
					codeBlock.setEditable(false);
					getKeyboardButton.setEnabled(false);
					connectButton.setText("Connect");
					statusLable.setText("No active connection");

					msgSocket.close();
					dataSocket.close();

					handleConnectionRequest=new Thread(new HandleConnectionRequest());
					handleConnectionRequest.start();
				}catch(IOException ex){
					System.err.println(ex);
				}
			}
			else
				return false;
		}
		return true;
	}

	/* listeners */
    private class CodeBlockListener implements DocumentListener{

        public void insertUpdate(DocumentEvent e) {
        	System.out.println("inserted "+e.getLength());
            writeData();

        }

        public void removeUpdate(DocumentEvent e) {
        	System.out.println("removed "+e.getLength());
            writeData();
        }

        public void changedUpdate(DocumentEvent e) {
        	System.out.println("changed "+e.getLength());
            writeData();
        }

        private void writeData(){
        	try{
        		if(canIInput){
                	dataOutputStream.writeUTF(codeBlock.getText().trim());
                	dataOutputStream.flush();
            	}
            }catch(IOException ex){
            	System.err.println(ex);
            }
        }
    }

    private class ConnectButtonListener implements ActionListener{
		public void actionPerformed(ActionEvent e){
			if(connectButton.getText().equals("Connect")){
				address=JOptionPane.showInputDialog(frame,
					"Please input IP address or hostname: ",
					"Input Target",JOptionPane.QUESTION_MESSAGE);
				if(address!=null&&!address.equals("")){
					try{
						msgSocket=new Socket(address,8000);
						msgInputStream=new DataInputStream(msgSocket.getInputStream());
						msgOutputStream=new DataOutputStream(msgSocket.getOutputStream());
						
						if(msgInputStream.readUTF().equals("A")){
							dataSocket=new Socket(address,8001);
							dataInputStream=new DataInputStream(dataSocket.getInputStream());
							dataOutputStream=new DataOutputStream(dataSocket.getOutputStream());
							
							canIInput=true;
							connectButton.setText("Disconnect");
							changeStates();

							JOptionPane.showMessageDialog(frame,
								"Connection established.",
								"Life is happy",JOptionPane.INFORMATION_MESSAGE);
							statusLable.setText("Connection with "+
								msgSocket.getInetAddress().getHostName()+
								"("+msgSocket.getInetAddress().getHostAddress()+") established.");

							if(getKeyboardThread==null||!getKeyboardThread.isAlive()){
								Thread getKeyboardThread=new Thread(new GetKeyboardThread());
					        	getKeyboardThread.start();
					        }
						}
						else{
							JOptionPane.showMessageDialog(frame,
								"Connection request denied.",
								"Life is sad",JOptionPane.ERROR_MESSAGE);
							msgInputStream.close();
							msgSocket.close();
						}
					}catch(UnknownHostException ex){
							System.err.println(ex);
							JOptionPane.showMessageDialog(frame,
								"Unknown IP address or hostname. Please check again.",
								"ERROR",JOptionPane.ERROR_MESSAGE);
					}catch(ConnectException ex){
							System.err.println(ex);
							JOptionPane.showMessageDialog(frame,
								"Unable to connect with target. Please check web connection.",
								"ERROR",JOptionPane.ERROR_MESSAGE);
					}catch(IOException ex){
							System.err.println(ex);
					}
				}
			}
			else{
				disconnect();
			}
		}
	}

    private class GetKeyboardButtonListener implements ActionListener{
		public void actionPerformed(ActionEvent e){
			try{
				msgOutputStream.writeUTF("K");
				msgOutputStream.flush();

				statusLable.setText("Asking for keyboard");

				if(msgInputStream.readUTF().equals("Y")){
					canIInput=true;
					changeStates();
					statusLable.setText("Get keyboard.");

					if(getKeyboardThread==null||!getKeyboardThread.isAlive()){
						Thread getKeyboardThread=new Thread(new GetKeyboardThread());
			    		getKeyboardThread.start();
			    	}
				}
				else
					JOptionPane.showMessageDialog(frame,
						"GetKeyboard request denied.",
						"Life is sad",JOptionPane.ERROR_MESSAGE);
			}catch(IOException ex){
				System.err.println(ex);
			}
		}
	}

    private class AboutButtonListener implements ActionListener{
        public void actionPerformed(ActionEvent e){
       		JOptionPane.showMessageDialog(frame,"Author: Kevin\n"+
       			"Email: kevin.xgr@gmail.com\n"+
       			"Software Page: https://github.com/Kevin-Xi/RemotePairProgramming\n"+
       			"GNU GPL","About",JOptionPane.INFORMATION_MESSAGE);
       	}
    }

    private class CloseWindowListener extends WindowAdapter{
        public void windowClosing(WindowEvent e){
        	//boolean willclose=disconnect();
        	//if(willclose)
        		System.exit(0);
        }
    }   
  
  	/* main */
	public static void main(String[] args) {

		new RemotePairProgramming();
	}
}