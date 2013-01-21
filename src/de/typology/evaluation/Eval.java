package de.typology.evaluation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

import de.typology.utils.IOHelper;

public class Eval {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		if(args.length!=2){
			System.out.println("need parameters: path/to/.logs regular_expression");
			System.out.println("example for parameters: /home/martin/shells/ trainedOn-wiki-*");
		}
		BufferedReader reader;
		BufferedWriter writer;

		String path=args[0];
		File dir=new File(path);
		String match=args[1].replace("*", ".*");
		for(String file:dir.list()) {
			if(file.matches(match)){
				System.out.println("evaluating: "+file);
				Double[] resultDouble={0.0,0.0,0.0,0.0,0.0,0.0};
				Double[] parametersDouble={0.0,0.0,0.0,0.0,0.0,0.0};
				writer = IOHelper.openWriteFile(path+"res."+file, 32 * 1024 * 1024);
				reader = IOHelper.openReadFile(path+file);
				String line;
				int matchCount=0;
				while((line=reader.readLine()) != null){
					if(line.contains("MATCH")){
						matchCount++;
						for(int k=1;k<6;k++){
							resultDouble[k]+=parametersDouble[k];
						}
						for(int i=1;i<parametersDouble.length;i++){
							parametersDouble[i]=0.0;
						}
					}else{
						for(int k=1;k<6;k++){
							if(line.startsWith("NKSS AT "+k)){
								if(parametersDouble[k]==0.0){
									parametersDouble[k]=Double.parseDouble(line.split(": ")[1].replaceAll(" ", ""));
									//System.out.println(line+"-->"+parameters[k]);
								} else {
									//System.out.println(line+"-->no changes");
								}
							}
						}
					}
				}
				//store last parameters
				for(int k=1;k<6;k++){
					resultDouble[k]+=parametersDouble[k];
				}
				for(int k=1;k<6;k++){
					System.out.println("NKSS at k="+k+": "+resultDouble[k]/matchCount);
					writer.write("NKSS at k="+k+": "+resultDouble[k]/matchCount+"\n");
				}

				reader.close();

				System.out.println("KSS:");
				reader = IOHelper.openReadFile(path+file);
				Long[] resultLong={0L,0L,0L,0L,0L,0L};
				Long[] parametersLong={0L,0L,0L,0L,0L,0L};
				matchCount=0;
				while((line=reader.readLine()) != null){
					if(line.contains("MATCH")){
						matchCount++;
						for(int k=1;k<6;k++){
							resultLong[k]+=parametersLong[k];
						}
						for(int i=1;i<parametersLong.length;i++){
							parametersLong[i]=0L;
						}
					}else{
						for(int k=1;k<6;k++){
							if(line.startsWith("KSS AT "+k)){
								if(parametersLong[k]==0L){
									parametersLong[k]=Long.parseLong(line.split(": ")[1].replaceAll(" ", ""));
									//System.out.println(line+"-->"+parametersLong[k]);
								} else {
									//System.out.println(line+"-->no changes");
								}
							}
						}
					}
				}
				//store last parameters
				for(int k=1;k<6;k++){
					resultLong[k]+=parametersLong[k];
				}
				for(int k=1;k<6;k++){
					System.out.println("KSS at k="+k+": "+resultLong[k]+"/"+matchCount+"="+(double)resultLong[k]/matchCount);
					writer.write("KSS at k="+k+": "+(double)resultLong[k]/matchCount+"\n");
				}


				reader.close();
				writer.flush();
				writer.close();
			}
		}
		System.out.println("run this:");
		System.out.println(path+"eval.sh "+args[1]);
	}
}
