package com.unisky.live.mlive;

import android.os.Bundle;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author kenping.liu
 * @date 2014-8-20
 */
public class KPQNode
{
    // XML 节点名
    public String tag      = "";
    // XML 节点文本内容
    public String value    = "";
    // XML 节点属性
    public Bundle attr     = new Bundle();
    // 子节点
    public List<KPQNode> children = new ArrayList<KPQNode>();

    public KPQNode()
    {
    }

    public KPQNode(String tag)
    {
        this.tag = tag;
    }

    public int getAttrInt(String key)
    {
        return getAttrInt(key, 0);
    }

    public int getAttrInt(String key, int defvalue)
    {
        try
        {
            return Integer.parseInt(attr.getString(key));
        }
        catch (Exception ex)
        {
        }
        return defvalue;
    }

    public String getAttrString(String key)
    {
        return getAttrString(key, "");
    }

    public String getAttrString(String key, String defvalue)
    {
        String str = attr.getString(key);
        return null != str ? str.trim() : defvalue;
    }

    public KPQNode addChild(String tag)
    {
        KPQNode child = new KPQNode(tag);
        children.add(child);
        return child;
    }

    public KPQNode getOneChildByTag(String tag)
    {
        for (KPQNode child : children)
        {
            if (child.tag.equals(tag))
            {
                return child;
            }
        }
        return null;
    }

    public static KPQNode parseNode(byte[] data)
    {
        KPQNode node = null;
        InputStream ins = new ByteArrayInputStream(data);
        try
        {
            XmlPullParser xpp = Xml.newPullParser();
            xpp.setInput(ins, "UTF-8");
            int eventType = XmlPullParser.START_DOCUMENT;
            while ((eventType = xpp.next()) != XmlPullParser.END_DOCUMENT)
            {
                if (eventType == XmlPullParser.START_TAG)
                {
                    node = parseNode(xpp);
                    break;
                }
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        finally
        {
            try
            {
                ins.close();
            }
            catch (Exception ex)
            {
            }
        }
        return null != node ? node : new KPQNode();
    }

    public static byte[] buildNode(KPQNode node)
    {
        // StringWriter xmlwriter = new StringWriter();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try
        {
            XmlSerializer xs = Xml.newSerializer();
            // xs.setOutput(xmlwriter);
            xs.setOutput(bos, "UTF-8");
            xs.startDocument("UTF-8", true);
            try
            {
                xs.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            }
            catch (Exception exx)
            {
            }
            buildNode(xs, node);
            xs.endDocument();
            xs.flush();

            // xmlwriter.close();
            bos.close();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        // return xmlwriter.toString();
        return bos.toByteArray();
    }

    /**
     * 解析xml节点为对象
     * 
     * @param xpp
     * @return
     * @throws Exception
     */
    private static KPQNode parseNode(XmlPullParser xpp) throws Exception
    {
        KPQNode node = new KPQNode(xpp.getName());
        for (int i = 0; i < xpp.getAttributeCount(); i++)
        {
            node.attr.putString(xpp.getAttributeName(i), xpp.getAttributeValue(i));
        }
        node.value = KPKit.tweakString(xpp.getText());
        if(xpp.getName().equals("content")){
    		node.value = xpp.nextText();
    		return node;
    	}
        int eventType = XmlPullParser.START_TAG;
        while ((eventType = xpp.next()) != XmlPullParser.END_TAG)
        {
            // 子节点
            if (eventType == XmlPullParser.START_TAG)
            {
            	node.children.add(parseNode(xpp));
            }
        }
        return node;
    }

    /**
     * 从对象构建xml节点
     * 
     * @param xs
     * @param node
     * @throws Exception
     */
    private static void buildNode(XmlSerializer xs, KPQNode node) throws Exception
    {
        xs.startTag(null, node.tag);
        for (String k : node.attr.keySet())
        {
            Object v = node.attr.get(k);
            // ?? 如果v为空是否考虑不发此属性，以节约带宽
            xs.attribute(null, k, null == v ? "" : v.toString());
        }
        if (null != node.value && node.value.length() > 0)
        {
            xs.text(node.value);
        }
        for (KPQNode child : node.children)
        {
            buildNode(xs, child);
        }
        xs.endTag(null, node.tag);
    }
}
