package utils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

public final class XMLUtils {

    public static void createXML(File destination, String rootName) {  //创建带有根节点XML文件
        // 创建Document对象
        Document document = DocumentHelper.createDocument();
        // 创建根节点
        @SuppressWarnings("unused")
		Element root = document.addElement(rootName);
        //保存到destination文件中
        saveToFile(destination, document);
    }

    public static void addOneRecord(File destination, String subName, String attributeName, String value)  {
        try {
            SAXReader reader = new SAXReader();
            Document dom = reader.read(destination);
            Element root = dom.getRootElement();
            Element subEle = root.addElement(subName);
            subEle.addAttribute(attributeName, value);
            saveToFile(destination, dom);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void addOneRecordToChoseFolder(File destination, String folder, String subName, String fileName)  {
        try {
            SAXReader reader = new SAXReader();
            Document dom = reader.read(destination);
            Element root = dom.getRootElement();
            @SuppressWarnings("unchecked")
			List<Element> choseFolderList = root.elements();
            for (Element choseFolder : choseFolderList) {
                if (choseFolder.attributeValue("path").equals(folder)) {
                    Element subEle = choseFolder.addElement(subName);
                    subEle.setText(fileName);
                }
            }
            saveToFile(destination, dom);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void removeOneRecord(File destination, String attributeName, String deleteValue)  {
        try {
            SAXReader reader = new SAXReader();
            Document dom = reader.read(destination);
            Element root = dom.getRootElement();
            @SuppressWarnings("unchecked")
			List<Element> list = root.elements();
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).attributeValue(attributeName).equals(deleteValue)) {
                    list.get(i).detach();
                }
            }
            saveToFile(destination, dom);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    //删除ChoseFolder元素下面的所有子元素
    public static void removeChoseFolderSubElements(File destination, String folder) {
        try {
            SAXReader reader = new SAXReader();
            Document dom = reader.read(destination);
            Element root = dom.getRootElement();
            @SuppressWarnings("unchecked")
			List<Element> list = root.elements();
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).attributeValue("path").equals(folder)) {
                    @SuppressWarnings("unchecked")
					List<Element> songElementList = list.get(i).elements();
                    for (Element songElement : songElementList) {
                        songElement.detach();
                    }
                }
            }
            saveToFile(destination, dom);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static List<String> getAllRecord(File destination, String attributeName)  {
        List<String> list = new ArrayList<>();
        try {
            SAXReader reader = new SAXReader();
            Document dom = reader.read(destination);
            Element root = dom.getRootElement();
            if (root == null)
                return list;

            @SuppressWarnings("unchecked")
			List<Element> elementList = root.elements();
            if (elementList == null || elementList.size() == 0)
                return list;

            for (Element element : elementList) {
                list.add(element.attributeValue(attributeName));
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return list;
    }

    public static List<String> getAllSong(File destination, String folder) {
        List<String> list = new ArrayList<>();
        try {
            SAXReader reader = new SAXReader();
            Document dom = reader.read(destination);
            Element root = dom.getRootElement();
            @SuppressWarnings("unchecked")
			List<Element> choseFolderList = root.elements();
            for (Element choseFolder : choseFolderList) {
                if (choseFolder.attributeValue("path").equals(folder)) {
                    @SuppressWarnings("unchecked")
					List<Element> songElementList = choseFolder.elements();
                    for (Element songElement : songElementList) {
                        list.add(songElement.getText());
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return list;
    }
    //对xml文件进行修改后执行的保存函数
    private static void saveToFile(File destination, Document dom) {
        try {
            OutputFormat outputFormat = OutputFormat.createPrettyPrint();
            outputFormat.setEncoding("UTF-8");
            outputFormat.setExpandEmptyElements(true);
            // 创建XMLWriter对象
            XMLWriter writer = new XMLWriter(new FileOutputStream(destination), outputFormat);
            // 设置不自动进行转义
            writer.setEscapeText(false);
            // 生成XML文件
            writer.write(dom);
            // 关闭XMLWriter对象
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
