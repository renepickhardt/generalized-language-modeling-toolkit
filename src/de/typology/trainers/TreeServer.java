package de.typology.trainers;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.InvalidParameterException;
import java.util.Comparator;
import java.util.HashMap;

import de.typology.trainers.SuggestTree.Node;
import de.typology.trainers.SuggestTree.Pair;
import de.typology.utils.IOHelper;

public class TreeServer extends UnicastRemoteObject implements TreeServerInterface{
	/**
	 * 
	 */
	private String name;
	private static final long serialVersionUID = 1L;
	private BufferedReader reader;
	private int joinLength = 12;
	private String line;
	private String[] lineSplit;
	private Float edgeCount;
	private SuggestTree<Float> tree;
	private HashMap<String, Float> edgeMap;
	private Comparator<Float> comparator;
	private boolean isEdge; //if false-->ngram

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException { 
		String path=args[0];
		TreeServer ts=new TreeServer(path);
		if(path.contains("typoEdges")){
			ts.isEdge=true;
		}else{
			if(path.contains("nGrams")){
				ts.isEdge=false;
			}
			else{
				throw new InvalidParameterException();
			}
		}
	}

	public TreeServer(String path) throws IOException { 
		//		if(LocateRegistry.getRegistry(Registry.REGISTRY_PORT)==null) {
		//			try {
		//				LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
		//			}
		//			catch (RemoteException ex) {
		//				System.out.println(ex.getMessage());
		//			}
		//		}
		File file=new File(path);
		this.name=file.getName();
		Naming.rebind(this.name, this);

		this.comparator = new Comparator<Float>() {

			@Override
			public int compare(Float o1, Float o2) {
				return o1.compareTo(o2);

			}
		};
		IOHelper.log("indexing " + file.getAbsolutePath());
		this.indexFile(new File(path));
		System.out.println("done indexing");
		//Socket client=serverSocket.accept();
		//read path to edges

	}


	private int indexFile(File file) throws IOException {
		this.reader = IOHelper.openReadFile(file.getAbsolutePath());
		this.edgeMap = new HashMap<String, Float>();
		int docCount = 0;
		while ((this.line = this.reader.readLine()) != null) {
			this.lineSplit = this.line.split("\t#");
			if (this.lineSplit.length != 2) {
				IOHelper.strongLog("can;t index line split is incorrectly"
						+ this.lineSplit.length);
				continue;
			}
			this.edgeCount = Float
					.parseFloat(this.lineSplit[this.lineSplit.length - 1]);
			String tmp = this.lineSplit[0].replace('\t', ' ');

			this.edgeMap.put(tmp, this.edgeCount);
			docCount++;
		}
		this.tree = new SuggestTree<Float>();
		this.tree.build(this.edgeMap, this.comparator, this.joinLength);
		return docCount;
	}
	@Override
	public 	HashMap<String,Float> getBestSuggestions (String prefix, Float weight){
		HashMap<String, Float> result = new HashMap<String, Float>();
		Node<Float> node = this.tree.getBestSuggestions(prefix);
		if (node != null) {
			for (int join = 0; join < this.joinLength
					&& join < node.listLength(); join++) {
				Pair<Float> pair = node.getSuggestion(join);
				String key;
				if(this.isEdge){
					key = pair.getString().split(" ")[1];}
				else{
					String[] split = pair.getString().split(" ");
					key = split[split.length - 1];
				}
				Float value = pair.getScore();
				if (result.containsKey(key)) {
					result.put(key, weight * value + result.get(key));
				} else {
					result.put(key, weight * value);
				}
			}
		}
		return result;
	}


	@Override
	public void exit() throws RemoteException
	{
		try{
			Naming.unbind(this.name);		
			UnicastRemoteObject.unexportObject(this, true);
			System.out.println("exiting server: "+this.name);
		}
		catch(Exception e){}
	}

	//	String readMessage(Socket socket) throws IOException {
	//		BufferedReader bufferedReader = 
	//				new BufferedReader(
	//						new InputStreamReader(
	//								socket.getInputStream()));
	//		char[] buffer = new char[200];
	//		int digitNumber = bufferedReader.read(buffer, 0, 200); // blocks until message received 
	//		String message = new String(buffer, 0, digitNumber);
	//		return message;
	//	}
	//
	//	void writeMessage(Socket socket, String message) throws IOException {
	//		PrintWriter printWriter =
	//				new PrintWriter(
	//						new OutputStreamWriter(
	//								socket.getOutputStream()));
	//		printWriter.print(message);
	//		printWriter.flush();
	//	}

}