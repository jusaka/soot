package soot.jimple.spark.summary;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import soot.Scene;
import soot.SootMethod;
import soot.jimple.spark.xml.PAGReader;
import soot.jimple.spark.xml.SummaryXMLException;

public class ClassesObjects {
	private PAGReader reader;
	private Map<String,ClassObjects> classObjectsMap;
	private Set<String> supportedClasses = new HashSet<String>();
	private Set<String> loadableClasses;
	private Map<String,File> files;
	public ClassesObjects(String dirPath){
		File source=new File(dirPath);
		if (!source.exists())
			throw new RuntimeException("Source directory " + dirPath + " does not exist");
		
		init();
		if (source.isFile()&&isXml(source)){
			addFile(source);
		}
		else if (source.isDirectory()) {
			File[] filesInDir = source.listFiles();
			if(filesInDir!=null){
				for(File file:filesInDir){
					addFile(file);
				}
			}
		}
		else
			throw new RuntimeException("Invalid input file: " + source);
		
	}
	private void init() {
		files=new HashMap<String,File>();
		loadableClasses = new HashSet<String>();
		supportedClasses = new HashSet<String>();
		classObjectsMap=new HashMap<String,ClassObjects>();
		this.reader=new PAGReader();
	}
	private void addFile(File file){
		String className=fileToClass(file);
		files.put(className, file);
		loadableClasses.add(className);
	}
	private String fileToClass(File f) {
		return f.getName().replace(".xml", "");
	}
	private boolean isXml(File f){
		return f.getName().endsWith(".xml");
	}
	
	public boolean supportsClass(String clazz) {
		if (supportedClasses.contains(clazz))
			return true;
		if (loadableClasses.contains(clazz))
			return true;
		return false;
	}
	
	public ClassObjects getClassObjects(String className){
		read(className);
		return classObjectsMap.get(className);
	}
	public MethodObjects getMethodObjects(SootMethod method){
		String className=method.getDeclaringClass().getName();
		ClassObjects classObjects=getClassObjects(className);
		if(classObjects!=null){
			return classObjects.getMethodObjects(method.getSignature());
		}
		return null;
	}
	public MethodObjects getMethodObjects(String signature){
		String className=Scene.v().signatureToClass(signature);
		return getMethodObjects(className,signature,false);
	}
	public MethodObjects getMethodObjects(String className,String methodSig,boolean isSub){
		ClassObjects classObjects=getClassObjects(className);
		if(classObjects==null) return null;
		if(!isSub){
			return classObjects.getMethodObjects(methodSig);
		}else{
			return classObjects.getMethodObjectsBySubSig(methodSig);
		}
	}
	public void read(String className){
		if(!loadableClasses.contains(className)) return;
		File file=files.get(className);
		try {
			ClassObjects classObjects=reader.read(file);
			classObjectsMap.put(className, classObjects);
			supportedClasses.add(className);
			loadableClasses.remove(className);
		} catch (XMLStreamException | SummaryXMLException | IOException e) {
			e.printStackTrace();
		}
	}
}
