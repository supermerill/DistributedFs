package remi.distributedFS.fs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class Parameters {
	
	public Map<String, String> data;
	public String filename;
	
	public Parameters(String filename){
		data = new HashMap<>();
		this.filename = filename;
		reload();
	}

	public void reload(){
		try{
		File fic = getFileOrCreate();
		try(BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(fic), StandardCharsets.UTF_8))){
			data.clear();
			String line = in.readLine();
			while(line != null){
				line = line.replace('\r',' ').trim();
				int indexofSpace = line.indexOf(' ');
				if(line.length()>2 && indexofSpace >0 && line.charAt(0) != '#' && indexofSpace < line.length()){
					String header = line.substring(0, indexofSpace);
					String value = line.substring(1+indexofSpace);
					data.put(header, value);
				}
				//relance
				line = in.readLine();
			}
		}
		}catch(IOException e ){ throw new RuntimeException(e); }
	}
	
	private File getFileOrCreate() throws IOException {
		File fic = new File(filename);
		if(fic.exists() && fic.isDirectory()){
			filename = "fic_"+filename;
			return getFileOrCreate();
		}
		if(!fic.exists()){
			fic.createNewFile();
		}
		return fic;
	}

	public void save(){
		try{
		File fic = getFileOrCreate();
		try(PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(fic), StandardCharsets.UTF_8))){
			for(Entry<String, String> entry : data.entrySet()){
				out.print(entry.getKey());
				out.print(' ');
				out.println(entry.getValue());
			}
		}
		}catch(IOException e ){ throw new RuntimeException(e); }
	}
	
	public String get(String key){
		return data.get(key);
	}
	
	public double getDouble(String key){
		return Double.parseDouble(data.get(key));
	}
	
	public int getInt(String key){
		return Integer.parseInt(data.get(key));
	}

	public long getLong(String key){
		return Long.parseLong(data.get(key));
	}
	
	public long getLongOrDef(String key, long def){
		if(!data.containsKey(key)){
			data.put(key, ""+def);
			save();
			return def;
		}
		return Long.parseLong(data.get(key));
	}

}
