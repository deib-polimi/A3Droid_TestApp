package it.polimi.mediasharing.sockets;

import it.polimi.mediasharing.a3.ExperimentSupervisorRole;
import it.polimi.mediasharing.activities.MainActivity;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Date;

import a3.a3droid.A3Message;

public class Server extends Thread{
	
	ExperimentSupervisorRole role;
	int port;
	
	public Server(int port, ExperimentSupervisorRole role) throws IOException {
		this.role = role;
		this.port = port;
	}
	
	@Override
	public void run() {
		super.run();
		try {
			this.createFileServer(port);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
    public void createFileServer(int port) throws IOException {         	    	
    	ServerSocket serverSocket = null;

        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException ex) {
            System.out.println("Can't setup server on this port number. ");
        }

        Socket socket = null;
        BufferedInputStream bis = null;
        DataInputStream dis = null;
        ByteArrayOutputStream baos = null;
        byte[] buffer = new byte[8192];

        int count = 0;
        while(count >= -1){
	        try {
	            socket = serverSocket.accept();
	        } catch (IOException ex) {
	            System.out.println("Can't accept client connection. ");
	        }
	        try {
	            bis = new BufferedInputStream(socket.getInputStream());
	            dis = new DataInputStream(bis);
	            baos = new ByteArrayOutputStream();
	        } catch (IOException ex) {
	            System.out.println("Can't get socket input stream. ");
	        }
	        
	        int reason = dis.readInt();
	        switch (reason) {
			case MainActivity.RFS:
				long time = dis.readLong();
				A3Message rfs = new A3Message(MainActivity.RFS, 
						time + "#" + 
						socket.getLocalAddress() + "#" +
						socket.getRemoteSocketAddress());
		        role.receiveApplicationMessage(rfs);
				break;
			case MainActivity.MC:
				while ((count = bis.read(buffer)) > 0) {
		        	baos.write(buffer, 0, count);
		        }		        
		        StringBuilder stringContent = new StringBuilder();
		        stringContent.append(Arrays.toString(baos.toByteArray()));
		        A3Message content = new A3Message(MainActivity.MEDIA_DATA, stringContent);
		        role.receiveApplicationMessage(content);
		        break;
			default:
				break;
			}
	        
        }
        bis.close();
        socket.close();
        serverSocket.close();
    }
}