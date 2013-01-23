package de.typology.evaluation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

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
		BufferedWriter writer2;

		String path=args[0];
		File dir=new File(path);
		String match=args[1].replace("*", ".*");
		for(String file:dir.list()) {
			if(file.matches(match)){

				//NKSS + standard deviation
				System.out.println("evaluating: "+file);
				Double[] resultDouble={0.0,0.0,0.0,0.0,0.0,0.0};
				Double[] parametersDouble={0.0,0.0,0.0,0.0,0.0,0.0};
				ArrayList<Double[]> results=new ArrayList<Double[]>();
				writer = IOHelper.openWriteFile(path+"res."+file, 32 * 1024 * 1024);
				writer2 = IOHelper.openWriteFile(path+"stdev."+file, 32 * 1024 * 1024);
				reader = IOHelper.openReadFile(path+file);
				String line;
				int matchCount=0;
				while((line=reader.readLine()) != null){
					if(line.contains("MATCH")){
						matchCount++;
						results.add(parametersDouble.clone());

						for(int k=1;k<6;k++){
							resultDouble[k]+=parametersDouble[k];
							System.out.print(parametersDouble[k]+" ");
						}
						System.out.println();
						for(int i=1;i<parametersDouble.length;i++){
							parametersDouble[i]=0.0;
						}
					}else{
						for(int k=1;k<6;k++){
							if(line.startsWith("NKSS AT "+k)){
								if(parametersDouble[k]==0.0){
									parametersDouble[k]=Double.parseDouble(line.split(": ")[1].replaceAll(" ", ""));
									//System.out.println(line+"-->"+parametersDouble[k]);
								} else {
									//System.out.println(line+"-->no changes");
								}
							}
						}
					}
				}
				//store last parameters
				results.add(parametersDouble.clone());
				for(int k=1;k<6;k++){
					resultDouble[k]+=parametersDouble[k];
				}
				//print NKSS to res.
				for(int k=1;k<6;k++){
					System.out.println("NKSS at k="+k+": "+resultDouble[k]/matchCount);
					writer.write("NKSS at k="+k+": "+resultDouble[k]/matchCount+"\n");
				}


				//calculate standard deviation:
				Double[] stDevPowTwo={0.0,0.0,0.0,0.0,0.0,0.0};
				Double[] arithMean={0.0,0.0,0.0,0.0,0.0,0.0};
				int[] CntNotZero={0,0,0,0,0,0};
				//count entries that are not zero
				for(Double[] result:results) {
					for(int k=1;k<6;k++){
						if(result[k]>0.0){
							CntNotZero[k]+=1;
						}

					}
				}
				//calculate arithmetic mean
				for(Double[] result:results) {
					for(int k=1;k<6;k++){
						arithMean[k]+=result[k];
					}
				}
				for(int k=1;k<6;k++){
					arithMean[k]/=CntNotZero[k];
					System.out.println(arithMean[k]);
				}

				//calculate numerator
				for(Double[] result:results) {
					for(int k=1;k<6;k++){
						if(result[k]>0.0){
							stDevPowTwo[k]+=Math.pow(result[k]-arithMean[k], 2);
						}
					}
				}
				//divide by count
				for(int k=1;k<6;k++){
					stDevPowTwo[k]/=CntNotZero[k];
				}
				//calculate square root
				Double[] stDev=new Double[6];
				for(int k=1;k<6;k++){
					stDev[k]=Math.sqrt(stDevPowTwo[k]);
				}
				//print standard deviation to stdev.
				for(int k=1;k<6;k++){
					System.out.println("Standard deviation at k="+k+": "+stDev[k]);
					writer2.write("Standard deviation at k="+k+": "+stDev[k]+"\n");
				}

				writer2.close();
				reader.close();

				//KSS
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
