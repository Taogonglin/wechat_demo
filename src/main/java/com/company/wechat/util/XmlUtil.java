package com.company.wechat.util;

import com.company.wechat.model.dto.CallbackMessage;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * XML解析工具类
 * 
 * @author Company
 */
public class XmlUtil {

    /**
     * 解析企业微信回调XML消息
     */
    public static CallbackMessage parseCallbackXml(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

            Element root = document.getDocumentElement();

            CallbackMessage message = new CallbackMessage();
            message.setToUserName(getElementValue(root, "ToUserName"));
            message.setFromUserName(getElementValue(root, "FromUserName"));
            message.setCreateTime(getLongElementValue(root, "CreateTime"));
            message.setMsgType(getElementValue(root, "MsgType"));
            message.setEvent(getElementValue(root, "Event"));
            message.setChangeType(getElementValue(root, "ChangeType"));
            
            String msgIdStr = getElementValue(root, "MsgId");
            if (msgIdStr != null && !msgIdStr.isEmpty()) {
                message.setMsgId(Long.parseLong(msgIdStr));
    }

            message.setOriginalXml(xml);

            return message;
        } catch (Exception e) {
            throw new RuntimeException("解析XML失败", e);
        }
    }

    /**
     * 获取XML元素的值
     */
    private static String getElementValue(Element parent, String tagName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList != null && nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent();
        }
        return null;
    }

    /**
     * 获取XML元素的Long值
     */
    private static Long getLongElementValue(Element parent, String tagName) {
        String value = getElementValue(parent, tagName);
        if (value != null && !value.isEmpty()) {
            return Long.parseLong(value);
        }
        return null;
    }

    /**
     * 构造响应XML
     */
    public static String buildResponseXml(String encrypt) {
        return "<xml>" +
                "<Encrypt><![CDATA[" + encrypt + "]]></Encrypt>" +
                "<MsgSignature><![CDATA[" + WechatSignUtil.computeSignature("", "", "", encrypt) + "]]></MsgSignature>" +
                "<TimeStamp>" + System.currentTimeMillis() / 1000 + "</TimeStamp>" +
                "<Nonce><![CDATA[" + AesUtil.getRandomStr() + "]]></Nonce>" +
                "</xml>";
        }
    }
