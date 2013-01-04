package de.typology.trainers;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map.Entry;

public class TreeTestClient {

	/**
	 * @param args
	 * @throws NotBoundException 
	 * @throws RemoteException 
	 * @throws MalformedURLException 
	 */
	public static void main(String[] args) throws MalformedURLException, RemoteException, NotBoundException {
		TreeServerInterface server = (TreeServerInterface)Naming.lookup("//127.0.0.1/1a.1es");
		HashMap<String,Float> result = server.getBestSuggestions("1991" + " a",1F);
		for(Entry<String, Float> e:result.entrySet()){
			System.out.println(e.getKey()+" "+e.getValue());
		}
		//server.exit();
	}

}
