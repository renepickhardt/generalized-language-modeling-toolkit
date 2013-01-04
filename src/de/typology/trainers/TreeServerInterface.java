package de.typology.trainers;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashMap;

public interface TreeServerInterface extends Remote
{
	public HashMap<String,Float> getBestSuggestions(String prefix, Float weight) throws RemoteException;
	public void exit() throws RemoteException;
}
