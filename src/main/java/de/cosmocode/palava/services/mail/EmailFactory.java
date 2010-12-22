/**
 * Copyright 2010 CosmoCode GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.cosmocode.palava.services.mail;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.activation.FileDataSource;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.SimpleEmail;
import org.jdom.Document;
import org.jdom.Element;

/**
 * Creates org.apache.commons.mail.Email Objects from XML-Templates.
 * 
 * @deprecated no need to use anymore
 * @author schoenborn@cosmocode.de
 */
@Deprecated
class EmailFactory {

    private static final EmailFactory INSTANCE = new EmailFactory();

    private static final String CHARSET = "UTF-8";
    private static final String EMAIL_SEPARATOR = ";";
    
    protected EmailFactory() {
        
    }

    /* CHECKSTYLE:OFF */
    @SuppressWarnings("unchecked")
    Email build(Document document, Embedder embed) throws EmailException, FileNotFoundException {
    /* CHECKSTYLE:ON */
        
        final Element root = document.getRootElement();
        
        final List<Element> messages = root.getChildren("message");
        if (messages.isEmpty()) throw new IllegalArgumentException("No messages found");
        
        final List<Element> attachments = root.getChildren("attachment");
        
        final Map<ContentType, String> available = new HashMap<ContentType, String>();
        
        for (Element element : messages) {
            final String type = element.getAttributeValue("type");
            final ContentType messageType = StringUtils.equals(type, "html") ? ContentType.HTML : ContentType.PLAIN;
            if (available.containsKey(messageType)) {
                throw new IllegalArgumentException("Two messages with the same types have been defined.");
            }
            available.put(messageType, element.getText());
        }
        
        final Email email;
        
        if (available.containsKey(ContentType.HTML) || attachments.size() > 0) {
            final HtmlEmail htmlEmail = new HtmlEmail();
            htmlEmail.setCharset(CHARSET);
            
            if (embed.hasEmbeddings()) {
                htmlEmail.setSubType("related");
            } else if (attachments.size() > 0) {
                htmlEmail.setSubType("related");
            } else {
                htmlEmail.setSubType("alternative");
            }
            
            /**
             * Add html message
             */
            if (available.containsKey(ContentType.HTML)) {
                htmlEmail.setHtmlMsg(available.get(ContentType.HTML));
            }
            
            /**
             * Add plain text alternative
             */
            if (available.containsKey(ContentType.PLAIN)) {
                htmlEmail.setTextMsg(available.get(ContentType.PLAIN));
            }
            
            /**
             * Embedded binary data
             */
            for (Map.Entry<String, String> entry : embed.getEmbeddings().entrySet()) {
                final String path = entry.getKey();
                final String cid = entry.getValue();
                final String name = embed.name(path);
                
                final File file;
                
                if (path.startsWith(File.separator)) {
                    file = new File(path);
                } else {
                    file = new File(embed.getResourcePath(), path);
                }
                
                if (file.exists()) {
                    htmlEmail.embed(new FileDataSource(file), name, cid);
                } else {
                    throw new FileNotFoundException(file.getAbsolutePath());
                }
            }
            
            /**
             * Attached binary data
             */
            for (Element attachment : attachments) {
                final String name = attachment.getAttributeValue("name", "");
                final String description = attachment.getAttributeValue("description", "");
                final String path = attachment.getAttributeValue("path");
                
                if (path == null) throw new IllegalArgumentException("Attachment path was not set");
                File file = new File(path);
                
                if (!file.exists()) file = new File(embed.getResourcePath(), path);
                
                if (file.exists()) {
                    htmlEmail.attach(new FileDataSource(file), name, description);
                } else {
                    throw new FileNotFoundException(file.getAbsolutePath());
                }
            }
            
            email = htmlEmail;
        } else if (available.containsKey(ContentType.PLAIN)) {
            email = new SimpleEmail();
            email.setCharset(CHARSET);
            email.setMsg(available.get(ContentType.PLAIN));
        } else {
            throw new IllegalArgumentException("No valid message found in template.");
        }
        
        final String subject = root.getChildText("subject");
        email.setSubject(subject);
        
        final Element from = root.getChild("from");
        final String fromAddress = from == null ? null : from.getText();
        final String fromName = from == null ? fromAddress : from.getAttributeValue("name", fromAddress);
        email.setFrom(fromAddress, fromName);

        
        final Element to = root.getChild("to");
        if (to != null) {
            final String toAddress = to.getText();
            if (StringUtils.isNotBlank(toAddress) && toAddress.contains(EMAIL_SEPARATOR)) {
                final String[] toAddresses = toAddress.split(EMAIL_SEPARATOR);
                for (String address : toAddresses) {
                    email.addTo(address);
                }
            } else if (StringUtils.isNotBlank(toAddress)) {
                final String toName = to.getAttributeValue("name", toAddress);
                email.addTo(toAddress, toName);
            }
        }
        
        final Element cc = root.getChild("cc");
        if (cc != null) {
            final String ccAddress = cc.getText();
            if (StringUtils.isNotBlank(ccAddress) && ccAddress.contains(EMAIL_SEPARATOR)) {
                final String[] ccAddresses = ccAddress.split(EMAIL_SEPARATOR);
                for (String address : ccAddresses) {
                    email.addCc(address);
                }
            } else if (StringUtils.isNotBlank(ccAddress)) {
                final String ccName = cc.getAttributeValue("name", ccAddress);
                email.addCc(ccAddress, ccName);
            }
        }
        
        final Element bcc = root.getChild("bcc");
        if (bcc != null) {
            final String bccAddress = bcc.getText();
            if (StringUtils.isNotBlank(bccAddress) && bccAddress.contains(EMAIL_SEPARATOR)) {
                final String[] bccAddresses = bccAddress.split(EMAIL_SEPARATOR);
                for (String address : bccAddresses) {
                    email.addBcc(address);
                }
            } else if (StringUtils.isNotBlank(bccAddress)) {
                final String bccName = bcc.getAttributeValue("name", bccAddress);
                email.addBcc(bccAddress, bccName);
            }
        }
        
        final Element replyTo = root.getChild("replyTo");
        if (replyTo != null) {
            final String replyToAddress = replyTo.getText();
            if (StringUtils.isNotBlank(replyToAddress) && replyToAddress.contains(EMAIL_SEPARATOR)) {
                final String[] replyToAddresses = replyToAddress.split(EMAIL_SEPARATOR);
                for (String address : replyToAddresses) {
                    email.addReplyTo(address);
                }
            } else if (StringUtils.isNotBlank(replyToAddress)) {
                final String replyToName = replyTo.getAttributeValue("name", replyToAddress);
                email.addReplyTo(replyToAddress, replyToName);
            }
        }
        
        return email;
    }
    
    public static final EmailFactory getInstance() {
        return INSTANCE;
    }
    
}
