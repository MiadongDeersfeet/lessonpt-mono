package com.yunki.lessonpt.relationship.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

class EmailSenderProfileTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(ConsoleEmailSender.class, InMemoryEmailSender.class, SmtpEmailSender.class, ProdMailConfiguration.class);

    @Test
    void localUsesConsoleSenderOnly() {
        runner.withPropertyValues("spring.profiles.active=local").run(context -> {
            assertThat(context).hasSingleBean(EmailSender.class);
            assertThat(context.getBean(EmailSender.class)).isInstanceOf(ConsoleEmailSender.class);
        });
    }

    @Test
    void testUsesInMemorySenderOnly() {
        runner.withPropertyValues("spring.profiles.active=test").run(context -> {
            assertThat(context).hasSingleBean(EmailSender.class);
            assertThat(context.getBean(EmailSender.class)).isInstanceOf(InMemoryEmailSender.class);
        });
    }

    @Test
    void prodUsesSmtpSenderOnly() {
        runner.withPropertyValues("spring.profiles.active=prod").run(context -> {
            assertThat(context).hasSingleBean(EmailSender.class);
            assertThat(context.getBean(EmailSender.class)).isInstanceOf(SmtpEmailSender.class);
        });
    }

    @Configuration
    static class ProdMailConfiguration {

        @Bean
        @org.springframework.context.annotation.Profile("prod")
        JavaMailSender javaMailSender() {
            return mock(JavaMailSender.class);
        }
    }
}
