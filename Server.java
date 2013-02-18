import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;

public class Server extends JFrame {
    private JTextPane codeBlock=new JTextPane();
	private JPanel codeBlockPanel=new JPanel();
	private JPanel controlPanel=new JPanel();
	private JButton connectButton=new JButton("Connect");

    private DataInputStream inputFromClient;
    private DataOutputStream outputToClient;
    private Socket socket;

    private boolean flag=false;

    public static void main(String[] args) {
        new Server();
    }

    public Server() {
        createGUI();

        createSocket();
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
        
        setLayout(new BorderLayout());
        add(controlPanel,BorderLayout.EAST);
        add(codeBlockPanel,BorderLayout.CENTER);

        setTitle("Remote Pair Programming---Server");
        setPreferredSize(new Dimension(500,400));
        setLocation(200,100);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
        pack();
    }

    private void createSocket(){
        try{    
            ServerSocket serverSocket=new ServerSocket(8000);
            System.out.println("Server started at "+new Date()+'\n');

            socket=serverSocket.accept();

            InetAddress inetAddress=socket.getInetAddress();
            System.out.println("Client "+inetAddress.getHostName()+"("+inetAddress.getHostAddress()+") connected.");

            inputFromClient=new DataInputStream(socket.getInputStream());
            outputToClient=new DataOutputStream(socket.getOutputStream());

            Thread thread=new Thread(new Runnable(){
                public void run(){
                    try{
                        while(inputFromClient.available()!=0){
                            flag=true;
                            codeBlock.setText(inputFromClient.readUTF());
                        }
                    }catch(IOException ex){
                        System.err.println(ex);
                    }
                }
            });

            thread.start();

        }catch(IOException ex){
            System.err.println(ex);
        }
    }

    private class CodeBlockListener implements DocumentListener{
        public void insertUpdate(DocumentEvent e) {
        	System.out.println("inserted.LOL"+e.getLength()+flag);
            if(!flag)
            try{
                outputToClient.writeUTF(codeBlock.getText().trim());
                outputToClient.flush();
                flag=true;
            }catch(IOException ex){
                System.err.println(ex);
            }
        }
        public void removeUpdate(DocumentEvent e) {
        	System.out.println("removed.LOL"+e.getLength()+flag);
            if(!flag)
            try{
                outputToClient.writeUTF(codeBlock.getText().trim());
                outputToClient.flush();
                flag=true;
            }catch(IOException ex){
                System.err.println(ex);
            }
        }
        public void changedUpdate(DocumentEvent e) {
        	System.out.println("changed.LOL"+e.getLength());
            try{
                outputToClient.writeUTF(codeBlock.getText().trim());
                outputToClient.flush();
            }catch(IOException ex){
                System.err.println(ex);
            }
        }
    }
}
