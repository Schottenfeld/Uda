package UDAServer;

// Interface for first contact with server; rest is done thread specific. 
public interface ServerInterface extends java.rmi.Remote
{
  int UDAConnect() throws java.rmi.RemoteException;
  void UDAClose(int con) throws java.rmi.RemoteException;
}
