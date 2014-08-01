package UDAServer;

import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;

// Implementation of the "first contact" interface; the rest is done by the thread-class.
public class UDAServerImpl extends UnicastRemoteObject implements ServerInterface
{
	private int MAXTHREADS=100; // Number of allowed concurrent connections
	private Thread ConThread[] = new Thread[MAXTHREADS];  // Thread-Array

	public UDAServerImpl(String s) throws RemoteException
	{
		super();
		for (int i=0; i<MAXTHREADS; i++) ConThread[i]=null; // Initialize the Thread-Array
	}

	// Create an own thread for the connection, the client can connect to the returned thread-number afterwords
	public int UDAConnect() throws RemoteException
	{
		int con;
		con = getFreeConnection();
		if (con == -1)
		{
			System.out.println("No free channel available; Please try again later.");
			return -1;
		}
		Runnable rThread = new ServerThread(con);
		ConThread[con] = new Thread(rThread);
		ConThread[con].start();
		return con;
	}

	// Stop the thread for that connection.
	public void UDAClose(int con) throws RemoteException
	{
		ConThread[con].stop();
		ConThread[con]=null;
		System.out.println("Connection Number " + con + " freed.");
	}

	// Search for a free connection.
	private int getFreeConnection()
	{
		for (int i=0; i<MAXTHREADS; i++)
		{
			if (ConThread[i]==null)
			{
				System.out.println("Connection Number " + i + " occupied.");
				return i;
			}
		}
		System.out.println("Warning: No free connection found.");
		return -1;
	}

	public static void main(String args[])
	{
		// Create and install a security manager
		System.setSecurityManager(new RMISecurityManager());
		try
		{
			UDAServerImpl obj = new UDAServerImpl("UDAServer");
			Naming.rebind("//localhost/UDAServer", obj);
			System.out.println("UDAServer bound in registry");
		}
		catch (Exception e)
		{
			System.out.println("UDA Server Exception: " + e.getMessage());
		}
	}
}


