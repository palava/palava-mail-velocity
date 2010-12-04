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

package de.cosmocode.palava.mail.templating.velocity;

import java.io.StringWriter;
import java.util.Map;
import java.util.Set;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import de.cosmocode.palava.mail.attachments.MailAttachmentSource;
import de.cosmocode.palava.mail.templating.LocalizedMailTemplate;
import de.cosmocode.palava.mail.templating.MailAttachmentTemplate;
import de.cosmocode.palava.mail.templating.TemplateEngine;
import de.cosmocode.palava.mail.templating.TemplateException;

/**
 * A velocity based {@link LocalizedMailTemplate}.
 * 
 * @author Tobias Sarnowski
 */
public class ParsedMailTemplate implements LocalizedMailTemplate {

    private String prefix;
    private LocalizedMailTemplate template;
    private VelocityContext context;

    private String subject;
    private String body;
    private Set<MailAttachmentTemplate> embedded;
    private Set<MailAttachmentTemplate> attachments;

    public ParsedMailTemplate(String prefix, LocalizedMailTemplate template, VelocityContext context) 
        throws TemplateException {
        
        this.prefix = prefix;
        this.template = template;
        this.context = context;

        // prepare snippets
        for (Map.Entry<String, String> snippetEntry : template.getSnippets().entrySet()) {
            final String snippet = parse(VelocityTemplateEngine.K_SNIPPETS + "/" + snippetEntry.getKey());
            context.put(snippetEntry.getKey(), snippet);
        }

        // parse actual things
        subject = parse(VelocityTemplateEngine.K_SUBJECT);
        body = parse(VelocityTemplateEngine.K_BODY);

        embedded = Sets.newHashSet();
        for (final MailAttachmentTemplate e : template.getEmbedded()) {

            final String embeddedName = parse(VelocityTemplateEngine.K_EMBEDDED + "/" + e.getName());
            final Map<String, String> configuration = Maps.newHashMap();
            for (Map.Entry<String, String> c : e.getConfiguration().entrySet()) {
                configuration.put(
                    c.getKey(), 
                    parse(VelocityTemplateEngine.K_EMBEDDED + "/" + e.getName() + "/" + c.getKey())
                );
            }

            embedded.add(new MailAttachmentTemplate() {
                @Override
                public String getName() {
                    return embeddedName;
                }

                @Override
                public Class<? extends MailAttachmentSource> getSource() {
                    return e.getSource();
                }

                @Override
                public Map<String, String> getConfiguration() {
                    return configuration;
                }
            });
        }

        attachments = Sets.newHashSet();
        for (final MailAttachmentTemplate a : template.getAttachments()) {

            final String attachmentName = parse(VelocityTemplateEngine.K_EMBEDDED + "/" + a.getName());
            final Map<String, String> configuration = Maps.newHashMap();
            for (Map.Entry<String, String> c : a.getConfiguration().entrySet()) {
                configuration.put(
                    c.getKey(), 
                    parse(VelocityTemplateEngine.K_EMBEDDED + "/" + a.getName() + "/" + c.getKey())
                );
            }

            embedded.add(new MailAttachmentTemplate() {
                @Override
                public String getName() {
                    return attachmentName;
                }

                @Override
                public Class<? extends MailAttachmentSource> getSource() {
                    return a.getSource();
                }

                @Override
                public Map<String, String> getConfiguration() {
                    return configuration;
                }
            });
        }
    }

    private String parse(String key) throws TemplateException {
        final StringWriter sw = new StringWriter();
        try {
            Velocity.mergeTemplate(prefix + key, VelocityTemplateEngine.ENCODING, context, sw);
            return sw.toString();
        /* CHECKSTYLE:OFF */
        } catch (Exception e) {
        /* CHECKSTYLE:ON */
            throw new TemplateException(e);
        }
    }

    @Override
    public String getName() {
        return template.getName();
    }

    @Override
    public String getSubject() {
        return subject;
    }

    @Override
    public String getBody() {
        return body;
    }

    @Override
    public Map<String, String> getSnippets() {
        return template.getSnippets();
    }

    @Override
    public Set<MailAttachmentTemplate> getEmbedded() {
        return embedded;
    }

    @Override
    public Set<MailAttachmentTemplate> getAttachments() {
        return attachments;
    }

    @Override
    public Class<? extends TemplateEngine> getTemplateEngine() {
        return template.getTemplateEngine();
    }
    
}
