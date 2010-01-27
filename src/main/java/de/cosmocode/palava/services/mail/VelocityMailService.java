/**
 * palava - a java-php-bridge
 * Copyright (C) 2007  CosmoCode GmbH
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package de.cosmocode.palava.services.mail;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Locale;
import java.util.Map;

import javax.mail.internet.MimeMessage;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.mail.Email;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import de.cosmocode.palava.core.lifecycle.Initializable;
import de.cosmocode.palava.services.mail.MailService;

/**
 * 
 *
 * @author Willi Schoenborn
 */
@Singleton
public class VelocityMailService implements MailService, Initializable {

    private static final Locale NO_LOCALE = null;
    
    private static final String CHARSET = "UTF-8";
    
    private final String hostname;
    
    private final File properties;
    
    private final VelocityEngine engine = new VelocityEngine();

    @Inject
    public VelocityMailService(
        @Named("mail.velocity.properties") File properties, 
        @Named("mail.velocity.hostname") String hostname) {
        this.properties = Preconditions.checkNotNull(properties, "Properties");
        this.hostname = Preconditions.checkNotNull(hostname, "Hostname");
    }
    
    @Override
    public void initialize() {
        /*CHECKSTYLE:OFF*/
        try {
            engine.init(properties.getAbsolutePath());
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
        /*CHECKSTYLE:ON*/
    }
    
    @Override
    public MimeMessage send(TemplateDescriptor descriptor, Map<String, ?> params) throws Exception {
        return send(descriptor, NO_LOCALE, params);
    }
    
    @Override
    public MimeMessage send(TemplateDescriptor descriptor, Locale locale, Map<String, ?> params) throws Exception {
        return send(descriptor, locale == null ? null : locale.toString(), params);
    }
    
    @Override
    public MimeMessage send(TemplateDescriptor descriptor, String lang, Map<String, ?> params) throws Exception {
        return sendMessage(descriptor.getName(), lang, params);
    }

    @Override
    public MimeMessage sendMessage(String templateName, String lang, Map<String, ?> params, String... to) 
        throws Exception {
        if (templateName == null) throw new IllegalArgumentException("Template name is null");
        
        final VelocityContext ctx = new VelocityContext(params);
        
        final String prefix = StringUtils.isBlank(lang) ? "" : lang + "/";
        final Template template = engine.getTemplate(prefix + templateName, CHARSET);
        
        final Embedder embed = new Embedder(engine);
        ctx.put("embed", embed);
        
        ctx.put("entity", EntityEncoder.getInstance());
        
        final StringWriter writer = new StringWriter();
        template.merge(ctx, writer);
        
        final EmailFactory factory = EmailFactory.getInstance();
        final SAXBuilder builder = new SAXBuilder();
        final Document document = builder.build(new StringReader(writer.toString()));
        
        final Email email = factory.build(document, embed);
        email.setHostName(hostname);
        
        for (String recipient : to) {
            email.addTo(recipient);
        }
        
        email.send();
        
        return email.getMimeMessage();
    }

}