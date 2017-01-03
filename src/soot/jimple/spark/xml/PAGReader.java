package soot.jimple.spark.xml;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import soot.jimple.spark.summary.ClassObjects;
import soot.jimple.spark.summary.MethodObjects;
import soot.jimple.spark.summary.BaseObject;
import soot.jimple.spark.summary.BaseObjectType;
import soot.jimple.spark.summary.FieldObject;
import soot.jimple.spark.summary.GapDefinition;


public class PAGReader {
	
	private enum State{
		summary, methods, method,baseobjects,baseobject,objects,object, gaps, gap
	}

	public ClassObjects read(File fileName) throws XMLStreamException,SummaryXMLException,IOException{
		String name=fileName.getName();
		String className=name.substring(0,name.lastIndexOf("."));
		ClassObjects classObjects=new ClassObjects(className);
		InputStream in = null;
		XMLStreamReader reader = null;
		try {
			in = new FileInputStream(fileName);
			reader = XMLInputFactory.newInstance().createXMLStreamReader(in);
			
			String currentMethod = "";
			
			State state = State.summary;
			while(reader.hasNext()){
				// Read the next tag
				reader.next();
				if(!reader.hasName())
					continue;
				
				if (reader.getLocalName().equals(PAGConstants.TREE_METHODS) && reader.isStartElement()) {
					if (state == State.summary)
						state = State.methods;
					else
						throw new SummaryXMLException();
				}
				else if (reader.getLocalName().equals(PAGConstants.TREE_METHOD) && reader.isStartElement() ){
					if(state == State.methods){
						currentMethod = getAttributeByName(reader,PAGConstants.ATTRIBUTE_METHOD_SIG);
						MethodObjects methodObjects=new MethodObjects(currentMethod);
						classObjects.merge(methodObjects);
						state = State.method;
					}			
					else
						throw new SummaryXMLException();
				}else if (reader.getLocalName().equals(PAGConstants.TREE_METHOD) && reader.isEndElement() ){
					if(state == State.method){
						state = State.methods;
					}else
						throw new SummaryXMLException();
				}
				else if (reader.getLocalName().equals(PAGConstants.TREE_BASEOBJECTS) && reader.isStartElement()) {
					if(state == State.method){
						state = State.baseobjects;
					}
					else
						throw new SummaryXMLException();
				}
				else if (reader.getLocalName().equals(PAGConstants.TREE_BASEOBJECTS) && reader.isEndElement()) {
					if(state == State.baseobjects){
						state = State.method;
					}
					else
						throw new SummaryXMLException();
				}else if (reader.getLocalName().equals(PAGConstants.TREE_BASEOBJECT) && reader.isStartElement()) {
					if(state == State.baseobjects){
						MethodObjects methodObjects=classObjects.getMethodObjects(currentMethod);
						methodObjects.addBaseObject(createBaseObject(reader));
						state = State.baseobject;
					}
					else
						throw new SummaryXMLException();
				}else if (reader.getLocalName().equals(PAGConstants.TREE_BASEOBJECT) && reader.isEndElement()) {
					if(state == State.baseobject){
						state = State.baseobjects;
					}
					else
						throw new SummaryXMLException();
				}else if (reader.getLocalName().equals(PAGConstants.TREE_OBJECTS) && reader.isStartElement()) {
					if(state == State.method){
						state = State.objects;
					}
					else
						throw new SummaryXMLException();
				}
				else if (reader.getLocalName().equals(PAGConstants.TREE_OBJECTS) && reader.isEndElement()) {
					if(state == State.objects){
						state = State.method;
					}
					else
						throw new SummaryXMLException();
				}else if (reader.getLocalName().equals(PAGConstants.TREE_OBJECT) && reader.isStartElement()) {
					if(state == State.objects){
						MethodObjects methodObjects=classObjects.getMethodObjects(currentMethod);
						Map<FieldObject,Set<FieldObject>> summary=createSummary(reader,methodObjects);
						for(FieldObject dest:summary.keySet()){
							methodObjects.addSummary(dest, summary.get(dest));
						}
						state = State.object;
					}
					else
						throw new SummaryXMLException();
				}else if (reader.getLocalName().equals(PAGConstants.TREE_OBJECT) && reader.isEndElement()) {
					if(state == State.object){
						state = State.objects;
					}
					else
						throw new SummaryXMLException();
				}else if (reader.getLocalName().equals(PAGConstants.TREE_METHODS) && reader.isEndElement()) {
					if (state == State.methods)
						state = State.summary;
					else
						throw new SummaryXMLException();
				}
				else if(reader.getLocalName().equals(PAGConstants.TREE_GAPS) && reader.isStartElement()){
					if(state == State.summary)
						state = State.gaps;
					else
						throw new SummaryXMLException();
				}
				else if(reader.getLocalName().equals(PAGConstants.TREE_GAPS) && reader.isEndElement()){
					if(state == State.gaps)
						state = State.summary;
					else
						throw new SummaryXMLException();
				}
				else if(reader.getLocalName().equals(PAGConstants.TREE_GAP) && reader.isStartElement()){
					if(state == State.gaps) {
						GapDefinition definition=getGapDefinition(reader);
						classObjects.addGap(definition);
						state = State.gap;
					}
					else
						throw new SummaryXMLException();
				}
				else if (reader.getLocalName().equals(PAGConstants.TREE_GAP) && reader.isEndElement()){
					if(state == State.gap) {
						state = State.gaps;
					}
					else
						throw new SummaryXMLException();
				}
			}
			classObjects.loadMethodGaps();
			return classObjects;
		}
		finally {
			if (reader != null)
				reader.close();
			if (in != null)
				in.close();
		}
	}

	private BaseObject createBaseObject(XMLStreamReader reader){
		int num=Integer.parseInt(getAttributeByName(reader,PAGConstants.ATTRIBUTE_ID));
		BaseObjectType baseObjectType=BaseObjectType.
				getTypeByValue(getAttributeByName(reader,PAGConstants.ATTRIBUTE_TYPE));
		String type=getAttributeByName(reader,PAGConstants.ATTRIBUTE_BASETYPE);
		BaseObject baseObject=new BaseObject(num,type,baseObjectType);
		if(BaseObjectType.isGap(baseObjectType)){
			int gap=Integer.parseInt(getAttributeByName(reader,PAGConstants.ATTRIBUTE_GAP));
			baseObject.setGapId(gap);
		}
		if(BaseObjectType.isParameter(baseObjectType)){
			int index=Integer.parseInt(getAttributeByName(reader,PAGConstants.ATTRIBUTE_INDEX));
			baseObject.setIndex(index);
		}
		return baseObject;
	}
	private Map<FieldObject,Set<FieldObject>> createSummary(XMLStreamReader reader,MethodObjects methodObjects){
		String field=getAttributeByName(reader,PAGConstants.ATTRIBUTE_FIELD);
		String p2Set=getAttributeByName(reader,PAGConstants.ATTRIBUTE_P2SET);
		String fieldType=getAttributeByName(reader,PAGConstants.ATTRIBUTE_FIELD_TYPE);
		Map<FieldObject,Set<FieldObject>> summary=new HashMap<FieldObject,Set<FieldObject>>();
		FieldObject dest=getFieldObject(field,fieldType,methodObjects);
		summary.put(dest, new HashSet<FieldObject>());
		String[] fields=p2Set.split(",");
		for(String sourceField:fields){
			FieldObject source=getFieldObject(sourceField,fieldType,methodObjects);
			summary.get(dest).add(source);
		}
		return summary;
	}
	public FieldObject getFieldObject(String field,String fieldType,MethodObjects methodObjects){
		FieldObject fieldObject;
		if(field.startsWith("new ")){
			String baseType=field.substring(4);
			fieldObject=new FieldObject(baseType,fieldType);
		}else{
			int baseId;
			if(!field.contains(".")){
				baseId=Integer.parseInt(field);
				BaseObject baseObject=methodObjects.getBaseObject(baseId);
				fieldObject=new FieldObject(baseObject,fieldType);
			}else{
				baseId=Integer.parseInt(field.substring(0, field.indexOf(".")));
				String accessPath=field.substring(field.indexOf("."));
				BaseObject baseObject=methodObjects.getBaseObject(baseId);
				fieldObject=new FieldObject(baseObject,accessPath,fieldType);
			}
		}
		return fieldObject;
	}
	
	
	private String getAttributeByName(XMLStreamReader reader, String id) {
		for (int i = 0; i < reader.getAttributeCount(); i++)
			if (reader.getAttributeLocalName(i).equals(id))
				return reader.getAttributeValue(i);
		return "";
	}

	private GapDefinition getGapDefinition(XMLStreamReader reader) {
		int id = Integer.parseInt(getAttributeByName(reader,PAGConstants.ATTRIBUTE_ID));
		String signature=getAttributeByName(reader,PAGConstants.ATTRIBUTE_METHOD_SIG);
		GapDefinition definition=new GapDefinition(id,signature);
		return definition;
	}
	
}

