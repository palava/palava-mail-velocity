/**
 * palava - a java-php-bridge
 * Copyright (C) 2007-2010  CosmoCode GmbH
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package de.cosmocode.palava.mail.templating.velocity;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import de.cosmocode.palava.mail.attachments.MailAttachmentSource;
import de.cosmocode.palava.mail.templating.LocalizedMailTemplate;
import de.cosmocode.palava.mail.templating.MailAttachmentTemplate;
import de.cosmocode.palava.mail.templating.TemplateEngine;
import de.cosmocode.palava.mail.templating.TemplateException;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Tobias Sarnowski
 */
public class ParsedMailTemplate implements LocalizedMailTemplate {
    private static final Logger LOG = LoggerFactory.getLogger(ParsedMailTemplate.class);

    private String prefix;
    private LocalizedMailTemplate template;
    private VelocityContext context;

    private String subject;
    private String body;
    private Set<MailAttachmentTemplate> embedded;
    private Set<MailAttachmentTemplate> attachments;


    public ParsedMailTemplate(String prefix, LocalizedMailTemplate template, VelocityContext context) throws TemplateException {
        this.prefix = prefix;
        this.template = template;
        this.context = context;

        // prepare snippets
        for (Map.Entry<String,String> snippetEntry: template.getSnippets().entrySet()) {
            String snippet = parse(VelocityTemplateEngine.K_SNIPPETS + "/" + snippetEntry.getKey());
            context.put(snippetEntry.getKey(), snippet);
        }

        // parse actual things
        subject = parse(VelocityTemplateEngine.K_SUBJECT);
        body = parse(VelocityTemplateEngine.K_BODY);

        embedded = Sets.newHashSet();
        for (final MailAttachmentTemplate e: template.getEmbedded()) {

            final String embeddedName = parse(VelocityTemplateEngine.K_EMBEDDED + "/" + e.getName());
            final Map<String, String> configuration = Maps.newHashMap();
            for (Map.Entry<String,String> c: e.getConfiguration().entrySet()) {
                configuration.put(c.getKey(), parse(VelocityTemplateEngine.K_EMBEDDED + "/" + e.getName() + "/" + c.getKey()));
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
        for (final MailAttachmentTemplate a: template.getAttachments()) {

            final String attachmentName = parse(VelocityTemplateEngine.K_EMBEDDED + "/" + a.getName());
            final Map<String, String> configuration = Maps.newHashMap();
            for (Map.Entry<String,String> c: a.getConfiguration().entrySet()) {
                configuration.put(c.getKey(), parse(VelocityTemplateEngine.K_EMBEDDED + "/" + a.getName() + "/" + c.getKey()));
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
        StringWriter sw = new StringWriter();
        try {
            Velocity.mergeTemplate(prefix + key, VelocityTemplateEngine.ENCODING, context, sw);
            return sw.toString();
        } catch (Exception e) {
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