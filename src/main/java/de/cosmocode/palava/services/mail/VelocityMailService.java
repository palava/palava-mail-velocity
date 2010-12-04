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
import com.google.inject.name.Named;

import de.cosmocode.palava.core.lifecycle.Initializable;
import de.cosmocode.palava.mail.templating.velocity.VelocityTemplateEngine;

/**
 * Velocity based implementation of the {@link MailService} interface.
 *
 * @deprecated in favor of {@link VelocityTemplateEngine}
 * @author Willi Schoenborn
 */
@Deprecated
final class VelocityMailService implements MailService, Initializable {

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
