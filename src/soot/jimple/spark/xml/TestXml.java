package soot.jimple.spark.xml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import soot.jimple.spark.summary.ClassObjects;

public class TestXml {
	public TestXml(String readPath,String writePath){
		ClassObjects classObjects=read(readPath);
		write(classObjects,writePath);
	}
	private void write(ClassObjects classObjects, String writePath) {
		// Create the target folder if it does not exist
		File f = new File(writePath);
		
		// Dump the flows
		PAGWriter writer = new PAGWriter();
		try {
			writer.write(f,classObjects);
		} catch (XMLStreamException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
	public ClassObjects read(String readPath){
		File f=new File(readPath);
		PAGReader reader=new PAGReader();
		try {
			return reader.read(f);
		} catch (XMLStreamException | SummaryXMLException | IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}
}
