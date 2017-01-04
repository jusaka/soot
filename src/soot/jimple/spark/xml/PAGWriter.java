package soot.jimple.spark.xml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Set;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import soot.jimple.spark.summary.BaseObject;
import soot.jimple.spark.summary.ClassObjects;
import soot.jimple.spark.summary.FieldObject;
import soot.jimple.spark.summary.GapDefinition;
import soot.jimple.spark.summary.MethodObjects;

public class PAGWriter  {
	
	private final int FILE_FORMAT_VERSION = 101;
	
	public PAGWriter(){
		
	}
	
	public void write(File file, ClassObjects classObjects)
			throws FileNotFoundException, XMLStreamException  {
		if (classObjects.isEmpty())
			return;
		
		OutputStream out = new FileOutputStream(file);
		XMLOutputFactory factory = XMLOutputFactory.newInstance();
		XMLStreamWriter writer = factory.createXMLStreamWriter(out);
		
		writer.writeStartDocument();
		writer.writeStartElement(PAGConstants.TREE_SUMMARY);
		writer.writeAttribute(PAGConstants.ATTRIBUTE_FORMAT_VERSION, FILE_FORMAT_VERSION + "");
		
		writer.writeStartElement(PAGConstants.TREE_METHODS);		
		writeMethodObjects(classObjects, writer);
		writer.writeEndElement(); //end methods tree
		
		writer.writeStartElement(PAGConstants.TREE_GAPS);		
		writeGaps(classObjects, writer);
		writer.writeEndElement(); //end gaps tree
		
		writer.writeEndDocument();
		writer.close();
	}

	private void writeGaps(ClassObjects classObjects, XMLStreamWriter writer) throws XMLStreamException {
		for(int i=0;i<classObjects.getLastGapId();i++){
			GapDefinition gap=classObjects.getGap(i);
			if(gap!=null){
				writer.writeStartElement(PAGConstants.TREE_GAP);
				writer.writeAttribute(PAGConstants.ATTRIBUTE_ID, gap.getID() + "");			
				writer.writeAttribute(PAGConstants.ATTRIBUTE_METHOD_SIG, gap.getSignature());
				writer.writeEndElement(); // close gap
			}
		}
	}

	private void writeMethodObjects(ClassObjects classObjects, XMLStreamWriter writer) throws XMLStreamException {
		for(String methodSig:classObjects.getMethodObjectsMap().keySet()){
			MethodObjects methodObjects=classObjects.getMethodObjects(methodSig);
			if(methodObjects.isEmpty()){
				continue;
			}
			writer.writeStartElement(PAGConstants.TREE_METHOD);
			writer.writeAttribute(PAGConstants.ATTRIBUTE_METHOD_SIG, methodSig);
			
			writer.writeStartElement(PAGConstants.TREE_BASEOBJECTS);
			for(int i=0;i<methodObjects.getLastBaseObjectId();i++){
				BaseObject baseObject=methodObjects.getBaseObject(i);
				if(baseObject!=null){
					writeBaseObject(writer,baseObject);
				}
			}
			writer.writeEndElement();
			
			writer.writeStartElement(PAGConstants.TREE_OBJECTS);
			for(FieldObject fieldObject:methodObjects.getSummaries().keySet()){
				writeFieldObject(writer,fieldObject,methodObjects.getSummaries().get(fieldObject));
			}
			writer.writeEndElement();
			
			writer.writeEndElement(); // close method
		}
	}
	private void writeBaseObject(XMLStreamWriter writer,BaseObject baseObject)throws XMLStreamException {
		writer.writeStartElement(PAGConstants.TREE_BASEOBJECT);
		writer.writeAttribute(PAGConstants.ATTRIBUTE_ID, baseObject.getID()+"");
		writer.writeAttribute(PAGConstants.ATTRIBUTE_TYPE, baseObject.getBaseObjectType()+"");
		if(baseObject.getGapId()!=-1){
			writer.writeAttribute(PAGConstants.ATTRIBUTE_GAP, baseObject.getGapId()+"");
		}
		if(baseObject.getIndex()!=-1){
			writer.writeAttribute(PAGConstants.ATTRIBUTE_INDEX, baseObject.getIndex()+"");
		}
		writer.writeAttribute(PAGConstants.ATTRIBUTE_BASETYPE, baseObject.getTypeString()+"");
		writer.writeEndElement();
	}
	private void writeFieldObject(XMLStreamWriter writer,FieldObject fieldObject,Set<FieldObject> sourceObjects)throws XMLStreamException {
		writer.writeStartElement(PAGConstants.TREE_OBJECT);
		writer.writeAttribute(PAGConstants.ATTRIBUTE_FIELD, fieldObject.getFieldString());
		StringBuilder sb=new StringBuilder();
		for(FieldObject sourceObject:sourceObjects){
			sb.append(sourceObject.getFieldString());
			sb.append(",");
		}
		String str=sb.substring(0,sb.length()-1);
		writer.writeAttribute(PAGConstants.ATTRIBUTE_P2SET, str);
		writer.writeAttribute(PAGConstants.ATTRIBUTE_FIELD_TYPE, fieldObject.getFieldType());
		writer.writeEndElement();
	}

}
