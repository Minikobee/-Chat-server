import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.*;

class Client {

    String nick;
    String room;
    State status;

    static enum State{
        INIT,
        OUTSIDE,
        INSIDE
    }

    public Client() {
        this.nick = "";
        this.room = "";
        this.status = State.INIT;
    }
}
