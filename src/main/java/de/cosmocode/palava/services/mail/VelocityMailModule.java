package de.cosmocode.palava.services.mail;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Singleton;

/**
 * Binds the {@link MailService} to the {@link VelocityMailService}.
 *
 * @author Willi Schoenborn
 */
public final class VelocityMailModule implements Module {

    @Override
    public void configure(Binder binder) {
        binder.bind(MailService.class).to(VelocityMailService.class).in(Singleton.class);
    }

}
