package dev.rummage.net;

import java.io.*;
import java.net.Socket;

/** Adapts Rummage's bounded TCP frames to the opaque text messages carried by OpenDraft's Worker. */
final class FrameLines implements Lines, AutoCloseable {
    static final int MAX_LINE = 16 * 1024;
    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;
    private volatile boolean closed;

    FrameLines(Socket socket) throws IOException {
        this.socket=socket;
        socket.setTcpNoDelay(true);
        socket.setKeepAlive(true);
        socket.setSoTimeout(15000);
        in=new DataInputStream(socket.getInputStream());
        out=new DataOutputStream(socket.getOutputStream());
    }
    public String readLine() throws IOException {
        try { return Wire.pack(Wire.read(in)); } catch(EOFException e) { return null; }
    }
    public synchronized void send(String line) {
        if(closed)return;
        try { if(line.length()>MAX_LINE)throw new IOException("Message too large");Wire.write(out,Wire.unpack(line)); }
        catch(IOException|IllegalArgumentException e){close();}
    }
    public boolean isClosed(){return closed||socket.isClosed();}
    public void close(){closed=true;try{socket.close();}catch(IOException ignored){}}
}
