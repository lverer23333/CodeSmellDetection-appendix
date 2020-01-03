package gr.uom.java.distance;

import gr.uom.java.ast.*;
import gr.uom.java.ast.association.Association;
import gr.uom.java.ast.association.AssociationDetection;
import gr.uom.java.ast.decomposition.MethodBodyObject;

import java.util.*;

public class MySystem {

    private Map<String,MyClass> classMap;
    private AssociationDetection associationDetection;
    private SystemObject systemObject;

    public MySystem(SystemObject systemObject, boolean includeStaticMembers) {
    	this.systemObject = systemObject;
        this.classMap = new HashMap<String,MyClass>();
        // 检测一个类在哪里被用作全局变量，并把这些association添加到associationDetection.associationList
        this.associationDetection = new AssociationDetection(systemObject);
        if(includeStaticMembers)
        	generateSystemWithStaticMembers();
        else
        	generateSystem();
    }
    
    /**  */
    private void generateSystem() {
        ListIterator<ClassObject> classIterator1 = systemObject.getClassListIterator();
        while(classIterator1.hasNext()) {
            ClassObject co = classIterator1.next();
            MyClass myClass = new MyClass(co.getName());
            myClass.setClassObject(co);
            TypeObject superclassType = co.getSuperclass();
            if(superclassType != null) {
            	String superclass = superclassType.getClassType();
            	if(systemObject.getClassObject(superclass) != null) {
            		myClass.setSuperclass(superclass);
            	}
            }

            ListIterator<FieldObject> fieldIt = co.getFieldIterator();
            while(fieldIt.hasNext()) {
            	FieldObject fo = fieldIt.next();
            	if(!fo.isStatic()) {
            		MyAttribute myAttribute = new MyAttribute(co.getName(),fo.getType().toString(),fo.getName());
            		myAttribute.setAccess(fo.getAccess().toString());
            		myAttribute.setFieldObject(fo);
            		if(associationDetection.containsFieldObject(fo))
            			myAttribute.setReference(true);
            		myClass.addAttribute(myAttribute);
            	}
            }
            classMap.put(co.getName(),myClass);
        }
        
        ListIterator<ClassObject> classIterator2 = systemObject.getClassListIterator();
        while(classIterator2.hasNext()) {
            ClassObject co = classIterator2.next();
            MyClass myClass = classMap.get(co.getName());
            ListIterator<MethodObject> methodIt = co.getMethodIterator();
            while(methodIt.hasNext()) {
            	MethodObject mo = methodIt.next();
            	if(!mo.isStatic() && systemObject.containsGetter(mo.generateMethodInvocation()) == null &&
            			systemObject.containsSetter(mo.generateMethodInvocation()) == null && systemObject.containsCollectionAdder(mo.generateMethodInvocation()) == null) {
            		MethodInvocationObject delegation = systemObject.containsDelegate(mo.generateMethodInvocation());
            		if(delegation == null || (delegation != null && systemObject.getClassObject(delegation.getOriginClassName()) == null)) {
            			MyMethod myMethod = new MyMethod(mo.getClassName(),mo.getName(),
            					mo.getReturnType().toString(),mo.getParameterList());
            			if(mo.isAbstract())
            				myMethod.setAbstract(true);
            			myMethod.setAccess(mo.getAccess().toString());
            			myMethod.setMethodObject(mo);
            			MethodBodyObject methodBodyObject = mo.getMethodBody();
            			if(methodBodyObject != null) {
            				MyMethodBody myMethodBody = new MyMethodBody(methodBodyObject);
            				myMethod.setMethodBody(myMethodBody);
            			}
            			myClass.addMethod(myMethod);
            			ListIterator<MyAttributeInstruction> attributeInstructionIterator = myMethod.getAttributeInstructionIterator();
            			while(attributeInstructionIterator.hasNext()) {
            				MyAttributeInstruction myInstruction = attributeInstructionIterator.next();
            				MyClass ownerClass = classMap.get(myInstruction.getClassOrigin());
            				MyAttribute accessedAttribute = ownerClass.getAttribute(myInstruction);
            				if(accessedAttribute != null) {
            					if(accessedAttribute.isReference())
            						myMethod.setAttributeInstructionReference(myInstruction, true);
            					accessedAttribute.addMethod(myMethod);
            				}
            			}
            		}
            	}
            }
        }
    }

    private void generateSystemWithStaticMembers() {
    	/** 下面这串目的是对自定义的类获得MyAttribute并添加到myClass，并添加到classMap */
    	// 这个循环，对class进行处理
        ListIterator<ClassObject> classIterator1 = systemObject.getClassListIterator();
        while(classIterator1.hasNext()) {
            ClassObject co = classIterator1.next();
            MyClass myClass = new MyClass(co.getName());
            myClass.setClassObject(co);
            // 获得它的父类
            TypeObject superclassType = co.getSuperclass();
            // 如果有父类，加到myClass中的属性
            if(superclassType != null) {
            	String superclass = superclassType.getClassType();
            	if(systemObject.getClassObject(superclass) != null) {
            		myClass.setSuperclass(superclass);
            	}
            }
            	
            // 这个循环，对class中的成员变量进行处理
            ListIterator<FieldObject> fieldIt = co.getFieldIterator();
            while(fieldIt.hasNext()) {
            	FieldObject fo = fieldIt.next();
            	MyAttribute myAttribute = new MyAttribute(co.getName(),fo.getType().toString(),fo.getName());
            	myAttribute.setAccess(fo.getAccess().toString());
            	myAttribute.setFieldObject(fo);
            	if(associationDetection.containsFieldObject(fo))
            		myAttribute.setReference(true);
            	myClass.addAttribute(myAttribute);
            }
            // Class名字和该MyClass对应的对象，后续使用mapclassMap.get(co.getName())获得该myclass
            classMap.put(co.getName(),myClass);
        }
        
        /** 下面这串目的主要是对用户自定义函数获得myMethod并添加到myClass，accessedAttribute（但目前没看到在哪儿有用） */
        // 这个循环，对class进行处理
        ListIterator<ClassObject> classIterator2 = systemObject.getClassListIterator();
        while(classIterator2.hasNext()) {
            ClassObject co = classIterator2.next();
            MyClass myClass = classMap.get(co.getName());
            
            // 这个循环，对method进行处理
            ListIterator<MethodObject> methodIt = co.getMethodIterator();
            while(methodIt.hasNext()) {
            	MethodObject mo = methodIt.next();
            	// 如果该函数不是下面三种函数类型（getter/setter/CollectionAdder)
            	if(systemObject.containsGetter(mo.generateMethodInvocation()) == null &&
            			systemObject.containsSetter(mo.generateMethodInvocation()) == null && systemObject.containsCollectionAdder(mo.generateMethodInvocation()) == null) {
            		MethodInvocationObject delegation = systemObject.containsDelegate(mo.generateMethodInvocation());
            		if(delegation == null || (delegation != null && systemObject.getClassObject(delegation.getOriginClassName()) == null)) {//也不是委托函数
            			MyMethod myMethod = new MyMethod(mo.getClassName(),mo.getName(),
            					mo.getReturnType().toString(),mo.getParameterList());
            			if(mo.isAbstract())
            				myMethod.setAbstract(true);
            			myMethod.setAccess(mo.getAccess().toString());
            			myMethod.setMethodObject(mo);
            			MethodBodyObject methodBodyObject = mo.getMethodBody();
            			if(methodBodyObject != null) {
            				MyMethodBody myMethodBody = new MyMethodBody(methodBodyObject);
            				myMethod.setMethodBody(myMethodBody);
            			}
            			myClass.addMethod(myMethod);
            			//method内的变量
            			ListIterator<MyAttributeInstruction> attributeInstructionIterator = myMethod.getAttributeInstructionIterator(); 
            			while(attributeInstructionIterator.hasNext()) {
            				MyAttributeInstruction myInstruction = attributeInstructionIterator.next();
            				MyClass ownerClass = classMap.get(myInstruction.getClassOrigin());
            				MyAttribute accessedAttribute = ownerClass.getAttribute(myInstruction);
            				if(accessedAttribute != null) {
            					if(accessedAttribute.isReference())
            						myMethod.setAttributeInstructionReference(myInstruction, true);
            					accessedAttribute.addMethod(myMethod);
            				}
            			}
            		}
            	}
            }
        }
    }

    public Iterator<MyClass> getClassIterator() {
        return classMap.values().iterator();
    }

    public MyClass getClass(String className) {
    	return classMap.get(className);
    }

    public void addClass(MyClass newClass) {
    	if(!classMap.containsKey(newClass.getName())) {
    		classMap.put(newClass.getName(), newClass);
    	}
    }
    
    public void removeClass(MyClass oldClass) {
    	if(classMap.containsKey(oldClass.getName())) {
    		classMap.remove(oldClass.getName());
    	}
    }

    public SystemObject getSystemObject() {
		return systemObject;
	}

    public List<Association> getAssociationsOfClass(ClassObject classObject) {
    	return associationDetection.getAssociationsOfClass(classObject);
    }

    public Association containsAssociationWithMultiplicityBetweenClasses(String from, String to) {
    	Association association = associationDetection.getAssociation(from, to);
    	if(association != null && association.isContainer())
    		return association;
    	return null;
    }
}
