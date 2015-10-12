package it.polimi.mediasharing.sockets;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.Charset;

public class Client {
	
	public void sendMessage(String host, int port, int reason, String message) throws IOException {		
		BufferedInputStream bis = new BufferedInputStream(new ByteArrayInputStream(Charset.forName("UTF-16").encode(message).array()));
        sendToTheServer(host, port, reason, bis);
    }
	
    public void sendFile(String host, int port, int reason, File file) throws IOException {
    	BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
        sendToTheServer(host, port, reason, bis);
    }
    
    public void sendToTheServer(String host, int port, int reason, BufferedInputStream bis) throws IOException {
    	Socket socket = new Socket(host, port);
    	DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        byte[] buffer = new byte[8192];
        int count;
        dos.writeInt(reason);
        if(bis != null){
	        while ((count = bis.read(buffer)) > 0) {
	        	dos.write(buffer, 0, count);
	        }
        	bis.close();
    	}
        dos.close();        
        socket.close();
    }
}