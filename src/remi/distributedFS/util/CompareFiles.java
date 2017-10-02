package remi.distributedFS.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class CompareFiles {
	
	public static void main(String[] args) throws IOException {
		String filename = "heatMei_20.png";
		filename = "si_empireearth_trailer_1600k_v9.wmv";

//		File fic1 = new File("C:/Users/Admin/Pictures/"+filename);

		System.out.println(" -- compare 32768 --");
		compare(filename, 0, 32768);
		System.out.println(" -- compare 4096 --");
		compare(filename, 0, 4096);
		System.out.println(" -- compare 65536 --");
		compare(filename, 0, 65536);
	}
	
	public static void  compareDirect(String fileName,int min, int max) throws IOException{
		
		File fic1 = new File("C:/Users/Admin/Videos/"+fileName);
		File fic2 = new File("Q:/"+fileName);

		FileChannel in1 = new FileInputStream(fic1).getChannel();
		FileChannel in2 = new FileInputStream(fic2).getChannel();

		ByteBuffer buff1 = ByteBuffer.allocate(max-min);
		ByteBuffer buff2 = ByteBuffer.allocate(max-min);
		
		int taille1 = in1.read(buff1);
		int taille2 = in2.read(buff2);
		if(taille1 != taille2){
			System.out.println("not same length : "+taille1+" != "+taille2);
		}
		int lastError = -2;
		int idx = 0;
		while(buff1.position()<buff1.limit()){
			byte by1 = buff1.get();
			byte by2 = buff2.get();
			if(by1 != by2 || (by1 == 0 && lastError == idx-1)){
				if(lastError != idx-1){
					System.out.println("Error at byte "+idx+" : "+by1+" != "+by2);
				}
				lastError = idx;
			}else if(lastError == idx-1){
				System.out.println("No more Error at byte "+idx+" : "+by1+" == "+by2);
			}
			idx++;
		}
		
//			for(int i=0;i<100;i++){
//			b1 = in1.read();
//			b2 = in2.read();
//			idx++;
//			System.out.println("Next byte : "+idx+" : "+b1+" != "+b2);
//			}
//		}
		
		 in1.close();
		 in2.close();
		
	}
		
	public static void  compare(String fileName,int min, int max) throws IOException{
			
		File fic1 = new File("C:/Users/Admin/Videos/"+fileName);
		File fic2 = new File("Q:/"+fileName);

		BufferedInputStream in1 = new BufferedInputStream(new FileInputStream(fic1));
		BufferedInputStream in2 = new BufferedInputStream(new FileInputStream(fic2));

		int idx = 0;
		int b1 = in1.read();
		int b2 = in2.read();
		int lastError = -2;
		while(b1>=0){
			b1 = in1.read();
			b2 = in2.read();
			if(b1 != b2 || (b1 == 0 && lastError == idx-1)){
				if(lastError != idx-1){
					System.out.println("Error at byte "+idx+" : "+b1+" != "+b2);
				}
				lastError = idx;
			}else if(lastError == idx-1){
				System.out.println("No more Error at byte "+idx+" : "+b1+" == "+b2);
			}
			idx++;
		}
		
//			for(int i=0;i<100;i++){
//			b1 = in1.read();
//			b2 = in2.read();
//			idx++;
//			System.out.println("Next byte : "+idx+" : "+b1+" != "+b2);
//			}
//		}
		
		 in1.close();
		 in2.close();
		
	}

}
