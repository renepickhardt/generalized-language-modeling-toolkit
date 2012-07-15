package de.typology.Keystrokes;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import de.typology.utils.Config;

public class KeystrokesPredictor extends Keystrokes {
	
	public static HashMap<String, Float> tanimotoEdges = null;
	public static HashMap<String, Float> normalizedTanimotoEdges = null;
	
	public static void main(String[] args){
		
		edges = new HashMap<String, Long>();
		nodes = new HashMap<String, Long>();
		
		KeystrokesHelper.loadEdges(edges, nodes);
		generateTanimotoGraph();
		normalizeGraph();		
		makePredictions();
		
		
	}
	
	private static void makePredictions() {
		String filePath = Config.get().germanWikiText;
		BufferedReader br = null;
		try {
			String sCurrentLine;
			br = new BufferedReader(new FileReader(filePath));
			int cnt = 4000 ;
			int top5=0;
			int top1=0;
			while ((sCurrentLine = br.readLine()) != null) {
				if (sCurrentLine.length()<100)continue;
				int index = Math.max(Math.min((int)(Math.random()*sCurrentLine.length()/2),sCurrentLine.length()/2 - Config.get().keystrokesWindowSize),Config.get().keystrokesWindowSize);
//				System.out.println("\n");
//				for (int i = 1;i<Config.get().keystrokesWindowSize; i++){
//					System.out.print(sCurrentLine.charAt(index - Config.get().keystrokesWindowSize + i));
//				}
//				System.out.print("...\t" + sCurrentLine.charAt(index) +"\n\n");
				//start retrieval procedure:
				HashMap<String, Float> res = new HashMap<String, Float>();
				for (int i = 1;i<Config.get().keystrokesWindowSize; i++){
					//int pastPos = Config.get().keystrokesWindowSize - i;
					String k1 = sCurrentLine.charAt(index-i)+"";
					for (String k2:nodes.keySet()){
						String edgeKey = k1+k2+"#"+i;
						//TODO: exchage edges with tanimotoEdges or normalizedTanimotoEdges
						if (edges.containsKey(edgeKey)){
							float value =0;
							if (res.containsKey(k2)){
								value = res.get(k2);
							}
							//TODO: exchage edges with tanimotoEdges or normalizedTanimotoEdges
							res.put(k2, value +edges.get(edgeKey));
						}
					}
				}
				
				if (outputTopk(res,5).contains(sCurrentLine.charAt(index)+"")){
					top5++;
				}
				if (outputTopk(res,1).contains(sCurrentLine.charAt(index)+"")){
					top1++;
				}

				
/*				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
*/			
				if (cnt % 1000 == 0){
					for (int i = 1;i<Config.get().keystrokesWindowSize; i++){
						System.out.print(sCurrentLine.charAt(index - Config.get().keystrokesWindowSize + i));
					}
					System.out.print("...\t" + sCurrentLine.charAt(index) +"\n\n");
					System.out.println(cnt + "durchlaeufe\t\t top5: " + top5 +"("+top5*100/cnt+"%)\t\tTop1: " + top1 +"("+top1*100/cnt+"%)");
				}
				cnt++;
			}
 
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}		
	}



	private static ArrayList<String> outputTopk(HashMap<String, Float> res, int k) {
		ArrayList<String> result = new ArrayList<String>();
		for (int i=0;i<k;i++){
			float max = 0;
			String maxKey ="";
			for (String key:res.keySet()){
				if (res.get(key)>max){
					max = res.get(key);
					maxKey = key;
				}
			}
//			System.out.println(maxKey + "\t" + max);
			result.add(maxKey);
			res.remove(maxKey);
		}
		return result;
	}



	private static void generateTanimotoGraph() {
		tanimotoEdges = new HashMap<String, Float>();
		for (int i = 1;i<Config.get().keystrokesWindowSize;i++){
			for (String k1:nodes.keySet()){
				for (String k2:nodes.keySet()){
					String edgeKey = k1+k2+"#"+i;
					float result = 0;
					if (edges.containsKey(edgeKey)){
						float cut = edges.get(edgeKey);
						float a = nodes.get(k1);
						float b = nodes.get(k2);
						result = cut / (a+b-cut);
					}
					tanimotoEdges.put(edgeKey, result);
				}
			}
		}
	}
	
	private static void normalizeGraph(){
		normalizedTanimotoEdges = new HashMap<String, Float>();
		for (int i = 1;i<Config.get().keystrokesWindowSize;i++){
			for (String k1:nodes.keySet()){
				//find max out edge
				float maxValue = 0;
				for (String k2:nodes.keySet()){
					String edgeKey = k1+k2+"#"+i;
					if (tanimotoEdges.containsKey(edgeKey)){
						float value = tanimotoEdges.get(edgeKey);
						if (value > maxValue)
							maxValue = value;
					}
				}
				// save normalized values
				for (String k2:nodes.keySet()){
					String edgeKey = k1+k2+"#"+i;
					if (tanimotoEdges.containsKey(edgeKey)){
						float value = tanimotoEdges.get(edgeKey);
						normalizedTanimotoEdges.put(edgeKey, value / maxValue);
					}
				}
			}
		}		
	}
}
