import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;

public class ChatClient {

    JFrame frame = new JFrame("Chat Client");
    private JTextField chatBox = new JTextField();
    private JTextArea chatArea = new JTextArea();

    static private ByteBuffer buffer = ByteBuffer.allocate( 16384 );
    private SocketChannel sc = null;

    static private final Charset charset = Charset.forName("UTF8");
    static private final CharsetDecoder decoder = charset.newDecoder();

 
    public void printMessage(final String message) {
        chatArea.append(message);
    }


    public ChatClient(String server, int port) throws IOException {

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(chatBox);
        frame.setLayout(new BorderLayout());
        frame.add(panel, BorderLayout.SOUTH);
        frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        frame.setSize(500, 300);
        frame.setVisible(true);
        chatArea.setEditable(false);
        chatBox.setEditable(true);
        chatBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    newmessage(chatBox.getText());
                } catch (IOException ex) {
                } finally {
                    chatBox.setText("");
                }
            }
        });
        frame.addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent e) {
                chatBox.requestFocus();
            }
        });
      
        Thread thread = new Thread();
        thread.run();
        InetSocketAddress socketAddress = new InetSocketAddress(server,port);
        sc = SocketChannel.open(socketAddress);
    }


    
    public void newmessage(String message) throws IOException {
        buffer.clear();
        sc.write(charset.encode(message+"\n"));
    }


    public void run() throws IOException {
        while(true){
          try{
            buffer.clear();
            sc.read(buffer);
            buffer.flip();

            if(buffer.limit() == 0)
              continue;

            String msg = decoder.decode(buffer).toString();
            String totmsg[] = msg.split(" ");
            String newmsg;
            String newtp[];

            if(msg.charAt(msg.length()-1)!='\n')
              continue;

    
            if(totmsg[0].equals("MESSAGE")){
               newmsg = totmsg[1]+":";
               int i =2;
               while(i<totmsg.length){
                newmsg+=" "+totmsg[i];
                i++;
              }
              printMessage(newmsg);
            }
            else if(totmsg[0].equals("LEFT")){
               newtp = totmsg[1].split("\n");
               newmsg = "LEFT " + newtp[0] + "\n";
              printMessage(newmsg);
            }
            else if(totmsg[0].equals("JOINED")){
               newtp = totmsg[1].split("\n");
               newmsg = "JOINED" + newtp[0] + "\n";
              printMessage(newmsg);
            }
            else
              printMessage(msg);
          } catch( IOException ie ){
            System.out.println("ERROR");
          }
        }

    }


    public static void main(String[] args) throws IOException {
        ChatClient client = new ChatClient(args[0], Integer.parseInt(args[1]));
        client.run();
    }

}
