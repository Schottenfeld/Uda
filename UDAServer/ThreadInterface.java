package UDAServer;

import java.util.Vector;

// Client-Server RMI Interface
public interface ThreadInterface extends java.rmi.Remote
{
  int Authenticate(String id, String pwd) throws java.rmi.RemoteException;
  String[] getStudent() throws java.rmi.RemoteException;
  Vector getSubject(int stage) throws java.rmi.RemoteException;
  Vector getModule(int subject, int stage) throws java.rmi.RemoteException;
  String Commit(UDAChosenSM chosenSM) throws java.rmi.RemoteException;
  UDAChosenSM getChosenSM(String Student, int Stage) throws java.rmi.RemoteException;
}
