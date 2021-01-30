import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.*;

public class ChatServer {
  static private final ByteBuffer buffer = ByteBuffer.allocate(16384);

  static private final Charset charset = Charset.forName("UTF8");
  static private final CharsetDecoder decoder = charset.newDecoder();

  static private HashMap<SocketChannel, Client> clients = new HashMap<SocketChannel, Client>();
  static private HashSet<String> usernames = new HashSet<String>();

  static private Client.State init = Client.State.INIT;
  static private Client.State outside = Client.State.OUTSIDE;
  static private Client.State inside = Client.State.INSIDE;

  static public void main(String args[]) throws Exception {
    int port = Integer.parseInt(args[0]);
    usernames.add("");

    try {
      ServerSocketChannel ssc = ServerSocketChannel.open();

      ssc.configureBlocking(false);

      ServerSocket ss = ssc.socket();
      InetSocketAddress isa = new InetSocketAddress(port);
      ss.bind(isa);

      Selector selector = Selector.open();

      ssc.register(selector, SelectionKey.OP_ACCEPT);
      System.out.println("Listening on port " + port);

      while (true) {
        int num = selector.select();
        if (num == 0) {
          continue;
        }

        Set<SelectionKey> keys = selector.selectedKeys();
        Iterator<SelectionKey> it = keys.iterator();
        while (it.hasNext()) {
         
          SelectionKey key = it.next();

         
          if (key.isAcceptable()) {

     
            Socket s = ss.accept();
            System.out.println("Got connection from " + s);

           
            SocketChannel sc = s.getChannel();
            sc.configureBlocking(false);
            Client client = new Client();
            clients.put(sc, client);
           
            sc.register(selector, SelectionKey.OP_READ, client);
           
          } else if (key.isReadable()) {

            SocketChannel sc = null;

            try {

            
              sc = (SocketChannel) key.channel();
              buffer.clear();
              sc.read(buffer);
              buffer.flip();

              if (buffer.limit() == 0)
                continue;

             
              String message = decoder.decode(buffer).toString();

              if (message.charAt(message.length() - 1) != '\n')
                continue;

              String userMessages[] = message.split("\n");

             
              int i = 0;
              while (i < userMessages.length) {
                boolean ok = processInput(sc, (Client) key.attachment(), userMessages[i]);
                if (!ok) {
                  key.cancel();
                  Socket sock = null;
                  try {
                    sock = sc.socket();
                    System.out.println("Closing connection to " + sock);
                    if (clients.get(sc).status == init) {
                      publicMessage("LEFT " + clients.get(sc).nick, clients.get(sc).room);
                    }
                    usernames.remove(clients.get(sc).nick);
                    clients.remove(sc);
                    sock.close();
                  } catch (IOException ie) {
                    System.err.println("Error closing socket " + sock + ": " + ie);
                  }
                }
                i++;
              }

            } catch (IOException ie) {

             
              key.cancel();

              try {
                sc.close();
                usernames.remove(clients.get(sc).nick);
                clients.remove(sc);
              } catch (IOException ie2) {
                System.out.println(ie2);
              }

              System.out.println("Closed " + sc);
            }
          }
        }

      
        keys.clear();
      }
    } catch (IOException ie) {
      System.err.println(ie);
    }
  }

  static private void publicMessage(String message, String room) throws IOException {
    for (SocketChannel sc : clients.keySet()) {
      if (clients.get(sc).room.equals(room)) {
        buffer.clear();
        buffer.put(message.getBytes());
        buffer.flip();
        while (buffer.hasRemaining()) {
          sc.write(buffer);
        }
      }
    }
  }

  static private void privateMessage(String message, String person) throws IOException {
    for (SocketChannel sc : clients.keySet()) {
      if (clients.get(sc).nick.equals(person)) {
        buffer.clear();
        buffer.put(message.getBytes());
        buffer.flip();
        while (buffer.hasRemaining()) {
          sc.write(buffer);
        }
        break;
      }
    }
  }

  static private void callok(SocketChannel sc) throws IOException {
    buffer.clear();
    buffer.put("OK\n".getBytes());
    buffer.flip();
    sc.write(buffer);
  }

  static private void callerror(SocketChannel sc) throws IOException {
    buffer.clear();
    buffer.put("ERROR\n".getBytes());
    buffer.flip();
    sc.write(buffer);

  }

  static private void callbye(SocketChannel sc) throws IOException {
    buffer.clear();
    buffer.put("BYE\n".getBytes());
    buffer.flip();
    sc.write(buffer);
  }

  // Just read the message from the socket and send it to stdout
  static private boolean processInput(SocketChannel sc, Client client, String message) throws IOException {
    // Read the message to the buffer
    buffer.clear();
    sc.read(buffer);
    buffer.flip();

    String mensagem_geral[] = message.split(" ");

    if (mensagem_geral[0].equals("/nick")) {

      if (!usernames.contains(mensagem_geral[1]) && clients.get(sc).status == init) {
        clients.get(sc).nick = mensagem_geral[1];
        clients.get(sc).status = outside;
        usernames.add(mensagem_geral[1]);
        callok(sc);
      } 

      else if (usernames.contains(mensagem_geral[1]) && clients.get(sc).status == init) {
        callerror(sc);
      
      }

      else if (!usernames.contains(mensagem_geral[1]) && clients.get(sc).status == outside) {
        usernames.remove(clients.get(sc).nick);
        clients.get(sc).nick = mensagem_geral[1];
        usernames.add(mensagem_geral[1]);
        callok(sc);
     
      } 

      else if (usernames.contains(mensagem_geral[1]) && clients.get(sc).status == outside) {
        callerror(sc);
      
      } 

      else if (!usernames.contains(mensagem_geral[1]) && clients.get(sc).status == inside) {
        publicMessage("NEWNICK " + clients.get(sc).nick + " " + mensagem_geral[1] + "\n", clients.get(sc).room);
        usernames.remove(clients.get(sc).nick);
        clients.get(sc).nick = mensagem_geral[1];
        usernames.add(mensagem_geral[1]);
        callok(sc);
      
      } 

      else if (usernames.contains(mensagem_geral[1]) && clients.get(sc).status == inside) {
        callerror(sc);
      }

    } 

    else if (mensagem_geral[0].equals("/join")) {

      if (clients.get(sc).status == outside) {

        clients.get(sc).status = inside;
        callok(sc);
        publicMessage("JOIN " + clients.get(sc).nick + "\n", mensagem_geral[1]);
        clients.get(sc).room = mensagem_geral[1];
      } 

      else if (clients.get(sc).status == inside) {

        callok(sc);
        String room = clients.get(sc).room;
        clients.get(sc).room = "";
        publicMessage("JOIN " + clients.get(sc).nick + "\n", room);
        publicMessage("JOIN " + clients.get(sc).nick + "\n", mensagem_geral[1]);
        clients.get(sc).room = mensagem_geral[1];
      } 

      else {
        callerror(sc);
      }
    } 

    else if (mensagem_geral[0].equals("/leave")) {

      if (clients.get(sc).status == inside) {
        callok(sc);
        String room = clients.get(sc).room;
        clients.get(sc).room = "";
        publicMessage("LEFT " + clients.get(sc).nick + "\n", room);
        clients.get(sc).status = outside;
      } 

      else {
        callerror(sc);
      }

    } 
    else if (mensagem_geral[0].equals("/priv")) {

      if (clients.get(sc).status == inside) {
        int flag = 0;
        for (SocketChannel sct : clients.keySet()) {
          if (clients.get(sct).nick.equals(mensagem_geral[1]) && clients.get(sct).room.equals(clients.get(sc).room)) {
            callok(sc);
            flag = 1;
            String mensagem = "";
            for (int i = 2; i < mensagem_geral.length; i++)
              mensagem += " " + mensagem_geral[i];
            privateMessage("PRIVATE " + clients.get(sc).nick + ":" + mensagem + "\n", mensagem_geral[1]);
            break;
          }
        }
        if (flag == 0) {
          callerror(sc);
        }
      } 

      else {
        callerror(sc);
      }
    }

     else if (mensagem_geral[0].equals("/bye")) {
      if (clients.get(sc).status != inside) {
        callbye(sc);
        usernames.remove(clients.get(sc).nick);
        clients.get(sc).room = "";
        return false;
      } 

      else if (clients.get(sc).status == inside) {
        callbye(sc);
        usernames.remove(clients.get(sc).nick);
        String room = clients.get(sc).room;
        clients.get(sc).room = "";
        publicMessage("LEFT " + clients.get(sc).nick + "\n", room);
        return false;
      }
    } 

    else if (mensagem_geral[0].charAt(0) == '/' && mensagem_geral[0].charAt(1) != '/') {
      callerror(sc);
    } 

    else {
      if (clients.get(sc).status == inside)
        publicMessage("MESSAGE " + clients.get(sc).nick + " " + message + "\n", clients.get(sc).room);
      else {
        callerror(sc);
      }

    }

    return true;
  }
}
