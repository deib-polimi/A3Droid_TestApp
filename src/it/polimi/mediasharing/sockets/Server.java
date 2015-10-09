package it.polimi.mediasharing.sockets;

import it.polimi.mediasharing.a3.ExperimentSupervisorRole;
import it.polimi.mediasharing.activities.MainActivity;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

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
        ByteArrayOutputStream baos = null;

        int count = 0;
        while(count >= -1){
	        try {
	            socket = serverSocket.accept();
	        } catch (IOException ex) {
	            System.out.println("Can't accept client connection. ");
	        }
	
	        try {
	            bis = new BufferedInputStream(socket.getInputStream());
	        } catch (IOException ex) {
	            System.out.println("Can't get socket input stream. ");
	        }
	
	        /* try {
	        	File file = new File(Environment.getExternalStorageDirectory() + "/a3droid/image.jpg");
	            if (!file.exists()) {
					file.createNewFile();
				}
	            System.out.println("Time diff: " + (new Date().getTime() - file.lastModified())/1000);
	            out = new FileOutputStream(file);*/
	        baos = new ByteArrayOutputStream();
	        /*} catch (FileNotFoundException ex) {
	            System.out.println("File not found. ");
	        }*/
	
	        byte[] buffer = new byte[8192];
	        
	        while ((count = bis.read(buffer)) > 0) {
	        	baos.write(buffer, 0, count);
	        }
	
	        //out.close();
	        bis.close();
	        A3Message content = new A3Message(MainActivity.MEDIA_DATA, Arrays.toString(baos.toByteArray()));
	        role.receiveApplicationMessage(content);
        }
        socket.close();
        serverSocket.close();
    }
}