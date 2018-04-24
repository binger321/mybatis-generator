package com.zibuyu.tech;

import com.rits.cloning.Cloner;
import org.mybatis.generator.api.GeneratedXmlFile;
import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.*;
import org.mybatis.generator.api.dom.xml.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 *
 * Created by zhuzhixian on 2017/11/11.
 */
public class MySelectiveColumnPlugin  extends PluginAdapter {

    protected String exampleShortName;
    protected String exampleFullName;
    protected String selectByExampleWithColSelected = "selectByExampleWithColSelected";
    protected String selectByPrimaryKeyWithColSelected = "selectByPrimaryKeyWithColSelected";

    @Override
    public boolean validate(List<String> warnings) {
        return true;
    }

    @Override
    public boolean modelExampleClassGenerated(TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
        try {
            exampleShortName = topLevelClass.getType().getShortName();
            exampleFullName = topLevelClass.getType().getFullyQualifiedName();

            topLevelClass.addImportedType(new FullyQualifiedJavaType("java.util.Set"));
            topLevelClass.addImportedType(new FullyQualifiedJavaType("java.util.HashSet"));
            topLevelClass.addImportedType(new FullyQualifiedJavaType("java.util.Iterator"));
            //Example类加入SelectiveFiled类
            InnerClass innerClass = new InnerClass("SelectiveField");
            innerClass.setVisibility(JavaVisibility.PUBLIC);
            innerClass.setStatic(true);

            //SelectiveFiled类私有化构造方法, 防止用户可以new
            Method constructor = new Method("SelectiveField");
            constructor.setVisibility(JavaVisibility.PRIVATE);
            constructor.setConstructor(true);
            constructor.addBodyLine("fields = new HashSet<>();");
            innerClass.addMethod(constructor);

            //SelectiveFiled类私构造静态方法创建实例
            Method constructor2 = new Method("select");
            constructor2.setVisibility(JavaVisibility.PUBLIC);
            constructor2.setReturnType(innerClass.getType());
            constructor2.setStatic(true);
            constructor2.addBodyLine("return new SelectiveField();");
            innerClass.addMethod(constructor2);

            //SelectiveFiled类 private Set<String> fields;
            Field fieldsSetField = new Field("fields", new FullyQualifiedJavaType("Set<String>"));
            fieldsSetField.setVisibility(JavaVisibility.PRIVATE);
            innerClass.addField(fieldsSetField);

            //SelectiveFiled类构造流式方法
            int i = 0;
            StringBuilder allFieldsSb = new StringBuilder();
            for (IntrospectedColumn column : introspectedTable.getAllColumns()) {
                Method fluentMethod = new Method(column.getJavaProperty());
                fluentMethod.setReturnType(innerClass.getType());
                fluentMethod.setVisibility(JavaVisibility.PUBLIC);
                fluentMethod.addBodyLine("fields.add(\"" + column.getActualColumnName() + "\");");
                fluentMethod.addBodyLine("return this;");
                innerClass.addMethod(fluentMethod);

                if (i > 0) allFieldsSb.append(',');
                allFieldsSb.append(column.getActualColumnName());
                i++;
            }

            //SelectiveFiled类 private allFields fields;
            Field allFiledsField = new Field("allFields", new FullyQualifiedJavaType("String"));
            allFiledsField.setVisibility(JavaVisibility.PRIVATE);
            allFiledsField.setStatic(true);
            allFiledsField.setInitializationString("\"" + allFieldsSb.toString() + "\"");
            innerClass.addField(allFiledsField);

            Method toStringMethod = new Method("toString");
            toStringMethod.setVisibility(JavaVisibility.PUBLIC);
            toStringMethod.setReturnType(new FullyQualifiedJavaType("String"));
            toStringMethod.addAnnotation("@Override");
            toStringMethod.addBodyLine("if(fields.isEmpty()) return allFields;");
            toStringMethod.addBodyLine("StringBuilder sb = new StringBuilder();");
            toStringMethod.addBodyLine("char seperator = ',';");
            toStringMethod.addBodyLine("Iterator<String> fieldInterator = fields.iterator();");
            toStringMethod.addBodyLine("int i = 0;");
            toStringMethod.addBodyLine("while(fieldInterator.hasNext()){");
            toStringMethod.addBodyLine("if(i>0) sb.append(seperator);");
            toStringMethod.addBodyLine("sb.append(fieldInterator.next());");
            toStringMethod.addBodyLine("i++;");
            toStringMethod.addBodyLine("}");
            toStringMethod.addBodyLine("return sb.toString();");
            innerClass.addMethod(toStringMethod);

            topLevelClass.addInnerClass(innerClass);

            //加入SelectiveFiled selectiveFiled到顶层example类
            Field topClasssSlectiveFiled = new Field("selectiveField",
                    new FullyQualifiedJavaType("SelectiveField"));
            topClasssSlectiveFiled.setVisibility(JavaVisibility.PRIVATE);
            topLevelClass.getFields().add(topClasssSlectiveFiled);
            Method getSelectiveFieldMethod = new Method("getSelectiveField");
            getSelectiveFieldMethod.setReturnType(innerClass.getType());
            getSelectiveFieldMethod.setVisibility(JavaVisibility.PUBLIC);
            getSelectiveFieldMethod.addBodyLine("return this.selectiveField;");
            topLevelClass.getMethods().add(getSelectiveFieldMethod);
            Method setSelectiveFieldMethod = new Method("setSelectiveField");
            setSelectiveFieldMethod.setVisibility(JavaVisibility.PUBLIC);
            setSelectiveFieldMethod.addParameter(new Parameter(innerClass.getType(), "selectiveField"));
            setSelectiveFieldMethod.addBodyLine("this.selectiveField = selectiveField;");
            topLevelClass.getMethods().add(setSelectiveFieldMethod);

            //初始化selectiveFiled到顶层example类构造器
            for (Method tmpMethod : topLevelClass.getMethods()) {
                if (tmpMethod.isConstructor()) {
                    tmpMethod.addBodyLine("selectiveField = SelectiveField.select();");
                    break;
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        return true;
    }

//    @Override
//    public boolean clientSelectByExampleWithBLOBsMethodGenerated(Method method,
//                                                                 Interface interfaze, IntrospectedTable introspectedTable) {
//        try {
//            addWithColSelectedMethodToExample(method, interfaze, introspectedTable);
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//        return true;
//    }

    @Override
    public boolean clientSelectByExampleWithoutBLOBsMethodGenerated(
            Method method, Interface interfaze,
            IntrospectedTable introspectedTable) {
        try {
            addWithColSelectedMethodToExample(method, interfaze, introspectedTable);
        }catch (Exception e){
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public boolean clientSelectByPrimaryKeyMethodGenerated(Method method, Interface interfaze, IntrospectedTable introspectedTable) {
        try {
            addWithColSelectedMethodToPk(method, interfaze, introspectedTable);
        }catch (Exception e){
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public boolean sqlMapDocumentGenerated(Document document, IntrospectedTable introspectedTable) {
        try {
            //-----  主要实现功能是用到Example类里面的selectiveField.toString()方法, 作为sqlmap里面的Base_Column_List <sql></sql>
            //修改mapper接口方案, 取出selectByExample, selectByPrimaryKey　并深度复制
            // 对象selectByExampleWithColSelected, selectByPrimaryKeyWithColSelected, 然后改掉对应id和属性
            XmlElement selectByExampleElement = null;
            XmlElement selectByPrimaryKeyElement = null;
            for (Element element : document.getRootElement().getElements()) {
                XmlElement xmlElement = (XmlElement) element;
                if (xmlElement.getName().equals("select")) {
                    for (Attribute attr : xmlElement.getAttributes()) {
                        if (attr.getName().equals("id") && attr.getValue().equals("selectByExample")) {
                            selectByExampleElement = xmlElement;
                            break;
                        }
                        if (attr.getName().equals("id") && attr.getValue().equals("selectByPrimaryKey")) {
                            selectByPrimaryKeyElement = xmlElement;
                            break;
                        }
                    }
                }
            }

            Cloner cloner = new Cloner();
            //selectByExample-----------------------------
            XmlElement newsSlectByExampleElement = cloner.deepClone(selectByExampleElement);
            //替换id属性
            Iterator<Attribute> newsSlectByExampleAttrIt = newsSlectByExampleElement.getAttributes().iterator();
            while (newsSlectByExampleAttrIt.hasNext()) {
                Attribute attr = newsSlectByExampleAttrIt.next();
                if (attr.getName().equals("id")) {
                    newsSlectByExampleAttrIt.remove();
                    break;
                }
            }
            newsSlectByExampleElement.addAttribute(new Attribute("id", selectByExampleWithColSelected));
            //替换Base_Column_List
            ListIterator<Element> exampleIt = ((ArrayList<Element>) newsSlectByExampleElement.getElements()).listIterator();
            while (exampleIt.hasNext()) {
                Element element = exampleIt.next();
                if (element instanceof XmlElement && ((XmlElement) element).getName().equals("include")) {
                    XmlElement tmpXmlElement = (XmlElement) element;
                    for (Attribute attr : tmpXmlElement.getAttributes()) {
                        if (attr.getName().equals("refid") && attr.getValue().equals("Base_Column_List")) {
                            exampleIt.set(new TextElement("${selectiveField.toString()} "));
                            break;
                        }
                    }
                }
            }
            document.getRootElement().addElement(newsSlectByExampleElement);

            if(selectByPrimaryKeyElement != null) {
                //selectByPrimaryKey----------------------
                XmlElement newSelectByPrimaryKeyElement = cloner.deepClone(selectByPrimaryKeyElement);
                //删除parameterType属性, 替换id属性
                Iterator<Attribute> newSelectByPrimaryKeyAttrIt = newSelectByPrimaryKeyElement.getAttributes().iterator();
                while (newSelectByPrimaryKeyAttrIt.hasNext()) {
                    Attribute attr = newSelectByPrimaryKeyAttrIt.next();
                    if (attr.getName().equals("parameterType")) {
                        newSelectByPrimaryKeyAttrIt.remove();
                    }
                    if (attr.getName().equals("id")) {
                        newSelectByPrimaryKeyAttrIt.remove();
                    }
                }
                newSelectByPrimaryKeyElement.addAttribute(new Attribute("id", selectByPrimaryKeyWithColSelected));
                //替换Base_Column_List
                ListIterator<Element> pkIt = ((ArrayList<Element>) newSelectByPrimaryKeyElement.getElements()).listIterator();
                while (pkIt.hasNext()) {
                    Element element = pkIt.next();
                    if (element instanceof XmlElement && ((XmlElement) element).getName().equals("include")) {
                        XmlElement tmpXmlElement = (XmlElement) element;
                        for (Attribute attr : tmpXmlElement.getAttributes()) {
                            if (attr.getName().equals("refid") && attr.getValue().equals("Base_Column_List")) {
                                pkIt.set(new TextElement("${selectiveField.toString()} "));
                                break;
                            }
                        }
                    }
                }
                document.getRootElement().addElement(newSelectByPrimaryKeyElement);
            }

        }catch (Exception e){
            e.printStackTrace();
        }
        return true;
    }

    private void addWithColSelectedMethodToExample(Method method, Interface interfaze,
                                          IntrospectedTable introspectedTable){
        Cloner cloner = new Cloner();
        Method newMethod = cloner.deepClone(method);
        newMethod.setName(method.getName() + "WithColSelected");
        selectByExampleWithColSelected = newMethod.getName();
        interfaze.addMethod(newMethod);
    }

    private void addWithColSelectedMethodToPk(Method method, Interface interfaze,
                                          IntrospectedTable introspectedTable){
        Cloner cloner = new Cloner();
        Method newMethod = cloner.deepClone(method);
        newMethod.setName(method.getName() + "WithColSelected");
        selectByPrimaryKeyWithColSelected = newMethod.getName();
        newMethod.getParameters().get(0).addAnnotation("@Param(\"id\")");
        newMethod.addParameter(new Parameter(new FullyQualifiedJavaType(exampleShortName+".SelectiveField"),
                "selectiveField"));
        newMethod.getParameters().get(1).addAnnotation("@Param(\"selectiveField\")");
        interfaze.addMethod(newMethod);
    }
}
