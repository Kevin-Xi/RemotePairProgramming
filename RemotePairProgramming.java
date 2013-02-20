import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

public class RemotePairProgramming extends JFrame{
	private JTextPane codeBlock=new JTextPane();
	private JPanel codeBlockPanel=new JPanel();
	private	JPanel controlPanel=new JPanel();
	private	JButton connectButton=new JButton("Connect");
	private JButton getKeyboardButton=new JButton("Get Keyboard");
	private JButton aboutButton=new JButton("About");
	private JLabel statusLable=new JLabel("No active connection.");
	private JButton nullButton=new JButton();
	private JFrame frame=this;

	private String address="localhost";

	private DataOutputStream outputStream;
	private DataInputStream inputStream;

	private Socket socket;
	private ServerSocket serverSocket;

	private boolean canIInput=false;

	public static void main(String[] args) {
		new RemotePairProgramming();
	}

	public RemotePairProgramming(){
		createGUI();

		createSocket();
	}

	private void createGUI(){
        codeBlock.setBackground(Color.BLACK);
        codeBlock.setForeground(Color.GREEN);
        codeBlock.setFont(new Font("LucidaBrightDemiItalic",Font.ITALIC,18));
        codeBlock.getDocument().addDocumentListener(new CodeBlockListener());
        
        codeBlockPanel.setLayout(new BorderLayout());
        codeBlock.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(codeBlock);
        codeBlockPanel.add(scrollPane,BorderLayout.CENTER);
        //controlPanel.setLayout(new BoxLayout(controlPanel,BoxLayout.Y_AXIS));
        controlPanel.setLayout(new GridLayout(11,1));
        controlPanel.add(connectButton);
        getKeyboardButton.setEnabled(false);
        controlPanel.add(getKeyboardButton);
        nullButton.setVisible(false);
        nullButton.setEnabled(false);
        controlPanel.add(nullButton);
        controlPanel.add(aboutButton);
        aboutButton.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e){
        		JOptionPane.showMessageDialog(frame,"Author: Kevin\nEmail: kevin.xgr@gmail.com\nSoftware Page: http://github.com/kevin-xi\nGNU GPL","About",JOptionPane.INFORMATION_MESSAGE);
        	}
        });

        connectButton.addActionListener(new ConnectListener());
        getKeyboardButton.addActionListener(new GetKeyboardListener());
        
        setLayout(new BorderLayout());
        add(codeBlockPanel,BorderLayout.CENTER);
        add(controlPanel,BorderLayout.EAST);
        add(statusLable,BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter(){
        	public void windowClosing(WindowEvent we){
        		boolean willclose=disconnect();
        		if(willclose)
        			System.exit(0);
        	}
        });

        setTitle("Remote Pair Programming");
        setPreferredSize(new Dimension(500,400));
        setLocation(250,150);
        //setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
        pack();
    }

    private void createSocket(){
    	try{   
            serverSocket=new ServerSocket(8000);
            statusLable.setText("Server started at "+new Date()+'\n');

            acceptConnection();
        }catch(IOException ex){
            System.err.println(ex);
        }
    }

    private void acceptConnection() throws IOException{
    	codeBlock.setEditable(false);
        connectButton.setText("Connect");
	    getKeyboardButton.setEnabled(false);

    	socket=serverSocket.accept();
        InetAddress inetAddress=socket.getInetAddress();
        outputStream=new DataOutputStream(socket.getOutputStream());
        int willIAccept=JOptionPane.showConfirmDialog(frame,"The host:"+inetAddress.getHostName()+"("+inetAddress.getHostAddress()+") intend to establish a connection. Accept?", "Connection Request Come In",JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE);
        if(willIAccept==JOptionPane.YES_OPTION){
        	outputStream.writeUTF("Y");
            outputStream.flush();
	        statusLable.setText("Connection with "+inetAddress.getHostName()+"("+inetAddress.getHostAddress()+") established.");
	            
            codeBlock.setEditable(false);
            connectButton.setText("Disconnect");
	        getKeyboardButton.setEnabled(true);

	        inputStream=new DataInputStream(socket.getInputStream());
	        //outputStream=new DataOutputStream(socket.getOutputStream());

	        Thread thread=new Thread(new Runnable(){
	            public void run(){
	                try{
	                    while(!canIInput){
	                        codeBlock.setText(inputStream.readUTF());
	                    }
	                }catch(IOException ex){
	                    System.err.println(ex);
	                }
	            }
	        });

	        thread.start();
	    }
	    else{
	    	outputStream.writeUTF("N");
            outputStream.flush();
            outputStream.close();
            socket.close();
	        acceptConnection();
	    }
    }

    private class ConnectListener implements ActionListener{
		public void actionPerformed(ActionEvent e){
			if(connectButton.getText().equals("Connect")){
				address=JOptionPane.showInputDialog(frame,"Please input IP address or hostname: ","Input Target",JOptionPane.QUESTION_MESSAGE);
				//System.out.println(address);
				if(address!=null&&!address.equals("")){
					try{
						socket=new Socket(address,8000);
						inputStream=new DataInputStream(socket.getInputStream());
						if(inputStream.readUTF().equals("Y")){
							outputStream=new DataOutputStream(socket.getOutputStream());
							canIInput=true;
							connectButton.setText("Disconnect");
							codeBlock.setEditable(canIInput);
							getKeyboardButton.setEnabled(!canIInput);
							JOptionPane.showMessageDialog(frame,"Connection established.","Life is happy",JOptionPane.INFORMATION_MESSAGE);
							statusLable.setText("Connection with "+socket.getInetAddress().getHostName()+"("+socket.getInetAddress().getHostAddress()+") established.");
						}
						else{
							JOptionPane.showMessageDialog(frame,"Connection request denied.","Life is sad",JOptionPane.ERROR_MESSAGE);
							inputStream.close();
							socket.close();
						}
					}catch(UnknownHostException ex){
							System.err.println(ex);
							JOptionPane.showMessageDialog(frame,"Unknown IP address or hostname. Please check again.","ERROR",JOptionPane.ERROR_MESSAGE);
					}catch(ConnectException ex){
							System.err.println(ex);
							JOptionPane.showMessageDialog(frame,"Unable to connect with target. Please check web connection.","ERROR",JOptionPane.ERROR_MESSAGE);
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

	private boolean disconnect(){
		if(socket!=null&&!socket.isClosed()){
			int willIDisconnect=JOptionPane.showConfirmDialog(frame,"The host:"+socket.getInetAddress().getHostName()+"("+socket.getInetAddress().getHostAddress()+") has established a connection. Is that your wish to disconnect?", "Disconnect Affirmation",JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE);
			if(willIDisconnect==JOptionPane.YES_OPTION){
				try{
					statusLable.setText("No active connection");
					socket.close();
					acceptConnection();
				}catch(IOException ex){
					System.err.println(ex);
				}
			}
			else
				return false;
		}
		return true;
	}

	private class GetKeyboardListener implements ActionListener{
		public void actionPerformed(ActionEvent e){
			statusLable.setText("Get keyboard.");
			codeBlock.setEditable(true);
		}
	}

	private class CodeBlockListener implements DocumentListener{
        public void insertUpdate(DocumentEvent e) {
        	System.out.println("inserted.LOL"+e.getLength());
        	try{
				if(canIInput){
                    outputStream.writeUTF(codeBlock.getText().trim());
                    outputStream.flush();
                }
			}catch(IOException ex){
				System.err.println(ex);
			}
        }
        public void removeUpdate(DocumentEvent e) {
        	System.out.println("removed.LOL"+e.getLength());
        	try{
				if(canIInput){
                    outputStream.writeUTF(codeBlock.getText().trim());
                    outputStream.flush();
                }
			}catch(IOException ex){
				System.err.println(ex);
			}
        }
        public void changedUpdate(DocumentEvent e) {
        	System.out.println("changed.LOL"+e.getLength());
        	try{
				if(canIInput){
                    outputStream.writeUTF(codeBlock.getText().trim());
                    outputStream.flush();
                }
			}catch(IOException ex){
				System.err.println(ex);
			}
        }
    }
}