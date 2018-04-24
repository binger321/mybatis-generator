package com.zibuyu.tech;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.xml.Attribute;
import org.mybatis.generator.api.dom.xml.Element;
import org.mybatis.generator.api.dom.xml.TextElement;
import org.mybatis.generator.api.dom.xml.XmlElement;

import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数字值为-99999 或 String类型值为erpsetnull 或日期值为计算机元年(1970-01-01 08:00:00.000), 使用updateSelective则为null
 * Created by zhuzhixian on 2017/11/10.
 */
public class MySetNullPlugin extends PluginAdapter {

    private enum DBTypes {STRING, NUMBER, DATE}

    ;

    @Override
    public boolean validate(List<String> warnings) {
        return true;
    }

    @Override
    public boolean sqlMapUpdateByPrimaryKeySelectiveElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
        try {
            List<IntrospectedColumn> columnList = new ArrayList<>();
            columnList.addAll(introspectedTable.getBaseColumns());
            columnList.addAll(introspectedTable.getBLOBColumns());
            setNull(element, columnList, true);
        }catch (Exception e){
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public boolean sqlMapUpdateByExampleSelectiveElementGenerated(XmlElement element, IntrospectedTable introspectedTable) {
        try {
            List<IntrospectedColumn> columnList = new ArrayList<>();
            columnList.addAll(introspectedTable.getBaseColumns());
            columnList.addAll(introspectedTable.getBLOBColumns());
            setNull(element, columnList, false);
        }catch (Exception e){
            e.printStackTrace();
        }
        return true;
    }


    private void setNull(XmlElement element, List<IntrospectedColumn> columnList, boolean isUpdateByPk) {
        //第一步, 截取设置null字段
        Map<DBTypes, List<IntrospectedColumn>> setNullColumnMap = new HashMap<>();
        List<IntrospectedColumn> noSetNullColumnList = new ArrayList<>();
        filterColums(setNullColumnMap, noSetNullColumnList, columnList);

        //第二步, 对这些字段设置null, 原理是在<set></set>判空节点下, 加特殊数字,字符串,日期, 然后set null
        XmlElement setElement = null;
        for (Element subElement : element.getElements()) {//获取<set></set>
            if (subElement instanceof XmlElement && ((XmlElement) subElement).getName().equals("set")) {
                setElement = (XmlElement) subElement;
                break;
            }
        }
        setElement.getElements().clear();

        //处理可设置为空的字段
        for (Map.Entry<DBTypes, List<IntrospectedColumn>> entry : setNullColumnMap.entrySet()) {
            for (IntrospectedColumn column : entry.getValue()) {
                StringBuilder testAttrValueBuilder = new StringBuilder(); //最外面一层<if></if>
                StringBuilder testAttrValueBuilder1 = new StringBuilder(); //第二层<if></if>等于特殊值
                StringBuilder testAttrValueBuilder2 = new StringBuilder(); //第二层<if></if>不等于特殊值
                StringBuilder ifTxtValueBuilder1 = new StringBuilder(); //measure_code = null
                StringBuilder ifTxtValueBuilder2 = new StringBuilder(); //measure_code = #{record.measureCode,jdbcType=VARCHAR},
                if (isUpdateByPk) {
                    buildSetNullUpdateByPkRelatedVal(testAttrValueBuilder, testAttrValueBuilder1, testAttrValueBuilder2, ifTxtValueBuilder1, ifTxtValueBuilder2,
                            entry.getKey(), column);
                } else {
                    buildSetNullUpdateByExampleRelatedVal(testAttrValueBuilder, testAttrValueBuilder1, testAttrValueBuilder2, ifTxtValueBuilder1, ifTxtValueBuilder2,
                            entry.getKey(), column);
                }

                //第二层<if></if> 等于特殊值
                Attribute testAttribute1 = new Attribute("test", testAttrValueBuilder1.toString());
                XmlElement ifXmlElement1 = new XmlElement("if");
                ifXmlElement1.addAttribute(testAttribute1);
                ifXmlElement1.addElement(new TextElement(ifTxtValueBuilder1.toString()));

                //第二层<if></if> 不等于特殊值
                Attribute testAttribute2 = new Attribute("test", testAttrValueBuilder2.toString());
                XmlElement ifXmlElement2 = new XmlElement("if");
                ifXmlElement2.addAttribute(testAttribute2);
                ifXmlElement2.addElement(new TextElement(ifTxtValueBuilder2.toString()));

                //最外面一层<if></if>
                Attribute testAttribute = new Attribute("test", testAttrValueBuilder.toString());
                XmlElement ifXmlElement = new XmlElement("if");
                ifXmlElement.addAttribute(testAttribute);
                ifXmlElement.addElement(ifXmlElement1);
                ifXmlElement.addElement(ifXmlElement2);

                setElement.addElement(ifXmlElement);
            }
        }

        //处理不支持设置为空的字段, bit类型,blob类型等
        for (IntrospectedColumn column : noSetNullColumnList) {
            StringBuilder testAttrValueBuilder = new StringBuilder(); //<if test="xx"></if>
            StringBuilder ifTxtValueBuilder = new StringBuilder(); //measure_code = #{record.measureCode,jdbcType=VARCHAR},
            if (isUpdateByPk) {
                buildNoSetNullUpdateByPkRelatedVal(testAttrValueBuilder, ifTxtValueBuilder, column);
            } else {
                buildNoSetNullUpdateByExampleRelatedVal(testAttrValueBuilder, ifTxtValueBuilder, column);
            }

            Attribute testAttribute = new Attribute("test", testAttrValueBuilder.toString());
            XmlElement ifXmlElement = new XmlElement("if");
            ifXmlElement.addAttribute(testAttribute);
            ifXmlElement.addElement(new TextElement(ifTxtValueBuilder.toString()));
            setElement.addElement(ifXmlElement);
        }
    }


    private void filterColums(Map<DBTypes, List<IntrospectedColumn>> setNullColumnMap, List<IntrospectedColumn> noSetNullColumnList,
                              List<IntrospectedColumn> columnList) {
        for (IntrospectedColumn column : columnList) {
            //String类型数据
            if (column.getJdbcType() == Types.CHAR || column.getJdbcType() == Types.VARCHAR || column.getJdbcType() == Types.NVARCHAR ||
                    column.getJdbcType() == Types.LONGVARCHAR || column.getJdbcType() == Types.LONGNVARCHAR) {
                if (setNullColumnMap.get(DBTypes.STRING) == null)
                    setNullColumnMap.put(DBTypes.STRING, new ArrayList<>());
                setNullColumnMap.get(DBTypes.STRING).add(column);
                continue;
            }
            //数字类型数据
            if (column.getJdbcType() == Types.INTEGER || column.getJdbcType() == Types.NUMERIC || column.getJdbcType() == Types.DECIMAL ||
                    column.getJdbcType() == Types.BIGINT || column.getJdbcType() == Types.FLOAT) {
                if (setNullColumnMap.get(DBTypes.NUMBER) == null)
                    setNullColumnMap.put(DBTypes.NUMBER, new ArrayList<>());
                setNullColumnMap.get(DBTypes.NUMBER).add(column);
                continue;
            }
            //日期类型数据
            if (column.getJdbcType() == Types.DATE || column.getJdbcType() == Types.TIMESTAMP) {
                if (setNullColumnMap.get(DBTypes.DATE) == null) setNullColumnMap.put(DBTypes.DATE, new ArrayList<>());
                setNullColumnMap.get(DBTypes.DATE).add(column);
                continue;
            }
            noSetNullColumnList.add(column);
        }
    }

    private void buildSetNullUpdateByPkRelatedVal(StringBuilder testAttrValueBuilder, StringBuilder testAttrValueBuilder1, StringBuilder testAttrValueBuilder2,
                                                  StringBuilder ifTxtValueBuilder1, StringBuilder ifTxtValueBuilder2,
                                                  DBTypes dbTypes, IntrospectedColumn column) {
        testAttrValueBuilder.append(column.getJavaProperty()).append(" != null");
        //test属性, e.g test="measureCode == 'erpsetnull'"
        if (dbTypes.equals(DBTypes.STRING)) {
            testAttrValueBuilder1.append(column.getJavaProperty()).append(" == 'erpsetnull'");
            testAttrValueBuilder2.append(column.getJavaProperty()).append(" != 'erpsetnull'");
        }
        if (dbTypes.equals(DBTypes.NUMBER)) {
            testAttrValueBuilder1.append(column.getJavaProperty()).append(" == -99999");
            testAttrValueBuilder2.append(column.getJavaProperty()).append(" != -99999");
        }
        if (dbTypes.equals(DBTypes.DATE)) {
            testAttrValueBuilder1.append(column.getJavaProperty()).append(".toString() == 'Thu Jan 01 08:00:00 CST 1970'");
            testAttrValueBuilder2.append(column.getJavaProperty()).append(".toString() != 'Thu Jan 01 08:00:00 CST 1970'");
        }

        //if具体内容, e.g measure_code = null
        ifTxtValueBuilder1.append(column.getActualColumnName()).append("= null,");
        ifTxtValueBuilder2.append(column.getActualColumnName()).append("= #{").append(column.getJavaProperty())
                .append(",jdbcType="+column.getJdbcTypeName()+"},");
    }

    private void buildSetNullUpdateByExampleRelatedVal(StringBuilder testAttrValueBuilder, StringBuilder testAttrValueBuilder1, StringBuilder testAttrValueBuilder2,
                                                       StringBuilder ifTxtValueBuilder1, StringBuilder ifTxtValueBuilder2,
                                                       DBTypes dbTypes, IntrospectedColumn column) {
        testAttrValueBuilder.append("record." + column.getJavaProperty()).append(" != null");
        //test属性, e.g test="record.measureCode = 'erpsetnull'"
        if (dbTypes.equals(DBTypes.STRING)) {
            testAttrValueBuilder1.append("record." + column.getJavaProperty()).append(" == 'erpsetnull'");
            testAttrValueBuilder2.append("record." + column.getJavaProperty()).append(" != 'erpsetnull'");
        }
        if (dbTypes.equals(DBTypes.NUMBER)) {
            testAttrValueBuilder1.append("record." + column.getJavaProperty()).append(" == -99999");
            testAttrValueBuilder2.append("record." + column.getJavaProperty()).append(" != -99999");
        }
        if (dbTypes.equals(DBTypes.DATE)) {
            testAttrValueBuilder1.append("record." + column.getJavaProperty()).append(".toString() == 'Thu Jan 01 08:00:00 CST 1970'");
            testAttrValueBuilder2.append("record." + column.getJavaProperty()).append(".toString() != 'Thu Jan 01 08:00:00 CST 1970'");
        }

        //if具体内容, e.g measure_code = null
        ifTxtValueBuilder1.append(column.getActualColumnName()).append("= null,");
        ifTxtValueBuilder2.append(column.getActualColumnName()).append("= #{record.").append(column.getJavaProperty())
                .append(",jdbcType="+column.getJdbcTypeName()+"},");
    }


    private void buildNoSetNullUpdateByPkRelatedVal(StringBuilder testAttrValueBuilder, StringBuilder ifTxtValueBuilder, IntrospectedColumn column) {
        //test属性, e.g test="measureCode != null"
        testAttrValueBuilder.append(column.getJavaProperty()).append(" != null");

        //if具体内容, e.g measure_code = #{measureCode,jdbcType=VARCHAR},
        ifTxtValueBuilder.append(column.getActualColumnName()).append("= #{").append(column.getJavaProperty())
                .append(",jdbcType="+column.getJdbcTypeName()+"},");
    }

    private void buildNoSetNullUpdateByExampleRelatedVal(StringBuilder testAttrValueBuilder, StringBuilder ifTxtValueBuilder, IntrospectedColumn column) {
        //test属性, e.g test="measureCode != null"
        testAttrValueBuilder.append("record." + column.getJavaProperty()).append(" != null");

        //if具体内容, e.g measure_code = #{record.measureCode,jdbcType=VARCHAR},
        ifTxtValueBuilder.append(column.getActualColumnName()).append("= #{record.").append(column.getJavaProperty())
                .append(",jdbcType="+column.getJdbcTypeName()+"},");
    }
}
