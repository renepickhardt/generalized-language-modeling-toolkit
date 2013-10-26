package de.typology.evaluationOld;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import de.typology.utils.IOHelper;

public class StatisticalHypothesis {

	/**
	 * output format:
	 * two columns, first column=first file, second column=ssecond file
	 * NKSS_1 NKSS_1
	 * NKSS_2 NKSS_2
	 * NKSS_3 NKSS_3
	 * NKSS_4 NKSS_4
	 * NKSS_5 NKSS_5
	 * NKSS_1 NKSS_1
	 * ...
	 * 
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		if(args.length!=2){
			System.out.println("need parameters: path/to/.logs regular_expression");
			System.out.println("example for parameters: /home/martin/results/ trainedOn-wiki-*");
			return;
		}
		String path=args[0];
		String match=args[1].replace("*", ".*");
		File dir=new File(path);
		ArrayList<String> files=new ArrayList<String>();
		for(String file:dir.list()){
			if(file.matches(match)&&file.contains("-lm-")){
				files.add(file);
			}
		}
		for(String file:files){
			if(new File(path+file.replace("-lm-", "-typolgy-")).exists()) {
				getStatisticalHypothesis(path,file.replace("-lm-", "-typolgy-"), file);
			}else{
				IOHelper.strongLog("coudn't find: "+file.replace("-lm-", "-typolgy-"));
			}
		}
	}

	public static void getStatisticalHypothesis(String path, String file1,String file2) throws NumberFormatException, IOException{
		BufferedReader reader1;
		BufferedReader reader2;
		BufferedWriter[]writers=new BufferedWriter[6];
		BufferedWriter[]nkssWriters1=new BufferedWriter[6];
		BufferedWriter[]nkssWriters2=new BufferedWriter[6];


		//statistical hypothesis testing
		IOHelper.strongLog("evaluating: "+ file1 +" againgst "+file2);
		Double[] parametersDouble1={0.0,0.0,0.0,0.0,0.0,0.0};
		Double[] parametersDouble2={0.0,0.0,0.0,0.0,0.0,0.0};

		//initialze writers
		File sthyDir=new File(path+"sthy/");
		sthyDir.mkdirs();
		File nkss=new File(path+"nkss/");
		nkss.mkdirs();
		for(int i=1;i<writers.length;i++){
			writers[i]= IOHelper.openWriteFile(path+"sthy/"+"sthy."+file1+".minus."+file2+".nkss"+i, 8 * 1024 * 1024);
			nkssWriters1[i]= IOHelper.openWriteFile(path+"nkss/"+file1+".nkss"+i, 8 * 1024 * 1024);
			nkssWriters2[i]= IOHelper.openWriteFile(path+"nkss/"+file2+".nkss"+i, 8 * 1024 * 1024);
		}
		reader1 = IOHelper.openReadFile(path+file1);
		reader2 = IOHelper.openReadFile(path+file2);
		String line1;
		String line2;
		int matchCount1=0;
		int matchCount2=0;
		while((line1=reader1.readLine()) != null){
			if(line1.contains("MATCH")){
				matchCount1++;
				//switch to second file
				while((line2=reader2.readLine()) != null){
					if(line2.contains("MATCH")){
						matchCount2++;
						if(matchCount1==1){
							//go back to first file
							break;
						}
						//write both results into file

						if(matchCount1==matchCount2&&line1.equals(line2)){
							for(int i=1;i<parametersDouble1.length;i++){
								//writers[i].write(parametersDouble1[i]+"-"+parametersDouble2[i]+"=");
								writers[i].write(parametersDouble1[i]-parametersDouble2[i]+"\n");
								nkssWriters1[i].write(parametersDouble1[i]+"\n");
								nkssWriters2[i].write(parametersDouble2[i]+"\n");
								//reset parameters
								parametersDouble1[i]=0.0;
								parametersDouble2[i]=0.0;
							}
						}else{
							IOHelper.strongLog("match1: "+line1);
							IOHelper.strongLog("is not the same as");
							IOHelper.strongLog("match2: "+line2);
							return;
						}	//go back to first file
						break;
					}
					else{
						for(int k=1;k<6;k++){
							if(line2.startsWith("NKSS AT "+k)){
								if(parametersDouble2[k]==0.0){
									parametersDouble2[k]=Double.parseDouble(line2.split(": ")[1].replaceAll(" ", ""));
									//System.out.println(line+"-->"+parametersDouble[k]);
								} else {
									//System.out.println(line+"-->no changes");
								}
							}
						}
					}
				}
			}else{
				for(int k=1;k<6;k++){
					if(line1.startsWith("NKSS AT "+k)){
						if(parametersDouble1[k]==0.0){
							parametersDouble1[k]=Double.parseDouble(line1.split(": ")[1].replaceAll(" ", ""));
							//System.out.println(line+"-->"+parametersDouble[k]);
						} else {
							//System.out.println(line+"-->no changes");
						}
					}
				}
			}
		}
		//get last parameters from file 2
		while((line2=reader2.readLine()) != null){
			for(int k=1;k<6;k++){
				if(line2.startsWith("NKSS AT "+k)){
					if(parametersDouble2[k]==0.0){
						parametersDouble2[k]=Double.parseDouble(line2.split(": ")[1].replaceAll(" ", ""));
						//System.out.println(line+"-->"+parametersDouble[k]);
					} else {
						//System.out.println(line+"-->no changes");
					}
				}
			}
		}
		//write last parameters
		if(matchCount1==matchCount2){
			for(int i=1;i<parametersDouble1.length;i++){
				writers[i].write(parametersDouble1[i]-parametersDouble2[i]+"\n");
				nkssWriters1[i].write(parametersDouble1[i]+"\n");
				nkssWriters2[i].write(parametersDouble2[i]+"\n");
			}
		}else{
			IOHelper.strongLog("matchCount1: "+matchCount1+" != matchCount2: "+matchCount2);
		}
		IOHelper.strongLog("evaluation done");
		for(int i=1;i<writers.length;i++){
			nkssWriters1[i].flush();
			nkssWriters1[i].close();
			nkssWriters2[i].flush();
			nkssWriters2[i].close();
			writers[i].flush();
			writers[i].close();
		}

		reader1.close();
		reader2.close();
	}
}

