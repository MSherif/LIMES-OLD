/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.learning.oracle.mappingreader;

import de.uni_leipzig.simba.data.Mapping;
import java.io.*;
import org.w3c.dom.*;
import javax.xml.parsers.*;

/**
 *
 * @author ngonga
 */
public class XMLMappingReader implements MappingReader {

    public Mapping getMapping(String filePath) {
        Mapping m = new Mapping();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            //make sure document is valid
            factory.setValidating(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document xmlDocument = builder.parse(new FileInputStream(filePath));

            //0. Prefixes
            NodeList list = xmlDocument.getElementsByTagName("Alignment");
            NodeList children, grandChildren, ggChildren;
            String namespace = "", label = "", s, t;
            for (int i = 0; i < list.getLength(); i++) {
                children = list.item(i).getChildNodes();
                for (int j = 0; j < children.getLength(); j++) {
                    Node child = children.item(j);
                    if (child.getNodeName().equals("map")) {
                        grandChildren = child.getChildNodes();
                        for (int k = 0; k < grandChildren.getLength(); k++) {
                            Node grandChild = grandChildren.item(k);
                            if (grandChild.getNodeName().equals("Cell")) {
                                s=""; t="";
                                ggChildren = grandChild.getChildNodes();
                                for (int z = 0; z < ggChildren.getLength(); z++) {
                                    Node ggChild = ggChildren.item(z);
                                    if(ggChild.getNodeName().equalsIgnoreCase("entity1"))
                                    {
                                        //System.out.println("Name = "+ggChild.getNodeName());
                                        s = ggChild.getAttributes().getNamedItem("rdf:resource").getNodeValue();
                                    }
                                    if(ggChild.getNodeName().equalsIgnoreCase("entity2"))
                                    {
                                        //System.out.println("Name = "+ggChild.getNodeName());
                                        t = ggChild.getAttributes().getNamedItem("rdf:resource").getNodeValue();
                                    }
                                }
                                //System.out.println(s+"->"+t);
                                if(!s.endsWith("/") && !t.endsWith("/"))
                                m.add(s, t, 1.0);
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return m;
    }

    public static void main(String args[])
    {
        Mapping m = new XMLMappingReader().getMapping("D:/Work/Data/Linking/DI03062010/sider-alignment-test.xml");
        System.out.println(m.size());
    }

    public String getType() {
        return "XML";
    }
}
