package soot.jimple.spark.xml;

public class PAGConstants {
	
//<summary>
//		<methods>
//			<method subSig="">
//				<baseobjects>
//					<baseobject num="1" type="This"  baseType=""/>
//					<baseobject num="2" type="Parameter"  index="" baseType=""/>
//					<baseobject num="3" type="GapBaseObject" gap="" baseType=""/>
//					<baseobject num="4" type="GapParameter"  gap="" index="" baseType=""/>
//					<baseobject num="5" type="GapReturn"  gap="" baseType=""  />
//					<baseobject num="6" type="Return" baseType=""/>
//				</baseobjects>
//				<objects>
//					<obejct field="1.field1.field2" p2set="1,new type()" fieldType=""/>
//				</objects>
//			</method>
//		</methods>
//		<gaps>
//			<gap num="" signature=""> 
//		</gaps>
//</summary>

	public static final String TREE_SUMMARY = "summary";
	public static final String TREE_METHODS = "methods";
	public static final String TREE_METHOD = "method";
	public static final String TREE_BASEOBJECTS = "baseobjects";
	public static final String TREE_BASEOBJECT = "baseobject";
	public static final String TREE_OBJECTS = "objects";
	public static final String TREE_OBJECT = "object";
	public static final String TREE_GAPS = "gaps";
	public static final String TREE_GAP = "gap";
	
	public static final String ATTRIBUTE_FORMAT_VERSION = "fileFormatVersion";
	public static final String ATTRIBUTE_ID = "num";
	public static final String ATTRIBUTE_METHOD_SIG = "signature";
	public static final String ATTRIBUTE_TYPE = "type";
	public static final String ATTRIBUTE_BASETYPE = "BaseType";
	public static final String ATTRIBUTE_INDEX = "index";
	public static final String ATTRIBUTE_FIELD = "field";
	public static final String ATTRIBUTE_P2SET = "p2set";
	public static final String ATTRIBUTE_FIELD_TYPE = "fieldType";
	public static final String ATTRIBUTE_GAP = "gap";
}
