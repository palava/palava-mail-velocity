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

import java.util.Map;
import java.util.Properties;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.runtime.resource.loader.StringResourceLoader;
import org.apache.velocity.runtime.resource.util.StringResourceRepository;
import org.apache.velocity.runtime.resource.util.StringResourceRepositoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.cosmocode.palava.mail.templating.LocalizedMailTemplate;
import de.cosmocode.palava.mail.templating.MailAttachmentTemplate;
import de.cosmocode.palava.mail.templating.TemplateEngine;
import de.cosmocode.palava.mail.templating.TemplateException;

/**
 * @author Tobias Sarnowski
 */
// FIXME should be package private and final
public class VelocityTemplateEngine implements TemplateEngine {
    private static final Logger LOG = LoggerFactory.getLogger(VelocityTemplateEngine.class);

    // FIXME should be configurable
    public static final String ENCODING = "UTF-8";

    protected static final String K_SUBJECT = "subject";
    protected static final String K_BODY = "body";
    protected static final String K_SNIPPETS = "snippets";
    protected static final String K_EMBEDDED = "embedded";
    protected static final String K_ATTACHMENTS = "attachments";

    public VelocityTemplateEngine() {
        final Properties config = new Properties();

        // see http://velocity.apache.org/engine/devel/apidocs/org/apache/velocity/runtime/resource/loader/StringResourceLoader.html
        config.put("resource.loader", "string");
        config.put("string.resource.loader.description", "Velocity StringResource loader");
        config.put("string.resource.loader.class", StringResourceLoader.class.getName());
        config.put("string.resource.loader.repository.class", StringResourceRepositoryImpl.class.getName());
        config.put("string.resource.loader.repository.name", VelocityTemplateEngine.class.getName());

        try {
            Velocity.init(config);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public LocalizedMailTemplate generate(final LocalizedMailTemplate template, Map<String, ? extends Object> variables) throws TemplateException {
        // generate template
        final String NAME = "/" + template.getName() + "/";
        StringResourceRepository repo = StringResourceLoader.getRepository(VelocityTemplateEngine.class.getName());

        repo.putStringResource(NAME + K_SUBJECT, template.getSubject());
        repo.putStringResource(NAME + K_BODY, template.getBody());
        for (Map.Entry<String,String> snippet: template.getSnippets().entrySet()) {
            repo.putStringResource(NAME + K_SNIPPETS + "/" + snippet.getKey(), snippet.getValue());
        }
        for (MailAttachmentTemplate embedded: template.getEmbedded()) {
            repo.putStringResource(NAME + K_EMBEDDED + "/" + embedded.getName(), embedded.getName());
            for (Map.Entry<String,String> config: embedded.getConfiguration().entrySet()) {
                repo.putStringResource(NAME + K_EMBEDDED + "/" + embedded.getName() + "/" + config.getKey(), config.getValue());
            }
        }
        for (MailAttachmentTemplate attachment: template.getEmbedded()) {
            repo.putStringResource(NAME + K_ATTACHMENTS + "/" + attachment.getName(), attachment.getName());
            for (Map.Entry<String,String> config: attachment.getConfiguration().entrySet()) {
                repo.putStringResource(NAME + K_ATTACHMENTS + "/" + attachment.getName() + "/" + config.getKey(), config.getValue());
            }
        }

        // configure variables
        final VelocityContext context = new VelocityContext();
        for (Map.Entry<String, ? extends Object> entry : variables.entrySet()) {
            context.put(entry.getKey(), entry.getValue());
        }

        return new ParsedMailTemplate(NAME, template, context);
    }
}