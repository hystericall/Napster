package com.infinitescript.napster.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SessionGateway extends Thread {
	public SessionGateway(Socket socket) {
		this.socket = socket;
		this.fileServer = FileServer.getInstance();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	public void run() {
		try {
			// Decorate the streams so we can send characters
			// and not just bytes.  Ensure output is flushed
			// after every newline.
			BufferedReader in = new BufferedReader(
					new InputStreamReader(socket.getInputStream()));
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			
			// Get messages from the client, line by line; return them
			// capitalized
			while ( true ) {
				String command = in.readLine();
				
				// Check if the user has logged in
				LOGGER.debug("Received new message: " + command);
				
				if ( command == null || command.equals("QUIT") ) {
					// The user is willing to leave
					String nickName = users.get(socket);
					LOGGER.info("User leaved " + nickName + ", Current Online Users: " + (users.size() - 1));
					users.remove(socket);
					// Fix infinite loop after client exit
					break;
				} else if ( !users.containsKey(socket) ) {
					if ( !command.startsWith("CONNECT ") ) {
						out.println("[WARN] Socket is going to close.");
						closeSocket();
					} else {
						String nickName = command.substring(7);
						
						out.println("ACCEPT");
						users.put(socket, nickName.trim());
						LOGGER.info("New user joined " + nickName + ", Current Online Users: " + users.size());
					}
				} else {
					// Invoke FileServer for other request
					if ( command.startsWith("ADD ") ) {
						SharedFile sharedFile = JSON.parseObject(command.substring(4), SharedFile.class);
						String ipAddress = socket.getInetAddress().toString();
						fileServer.shareNewFile(sharedFile, ipAddress);

						LOGGER.info("File shared at " + ipAddress + ", " + sharedFile);
					} else if ( command.startsWith("DELETE ") ) {

					} else if ( command.equals("LIST") ) {
						List<SharedFile> sharedFiles = fileServer.getSharedFiles();
						out.println(JSON.toJSONString(sharedFiles));
					} else if ( command.startsWith("REQUEST ") ) {

					}
				}
			}
		} catch (IOException e) {
			LOGGER.catching(e);
		} finally {
			closeSocket();
		}
	}

	/**
	 * Close socket.
	 */
	private void closeSocket() {
		if ( socket == null ) {
			return;
		}
		
		try {
			socket.close();
			LOGGER.info("Socket has closed for " + socket);
		} catch ( IOException e ) {
			LOGGER.catching(e);
		}
	}
	
	/**
	 * The socket between the server and client.
	 */
	private Socket socket;

	/**
	 * The file server for sharing files.
	 */
	private FileServer fileServer;
	
	/**
	 * The map used for storing the nick name for online users.
	 */
	private static Map<Socket, String> users = new Hashtable<Socket, String>();
	
	/**
	 * Logger.
	 */
	private static final Logger LOGGER = LogManager.getLogger(SessionGateway.class);
}
