package com.yunki.lessonpt.relationship.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

class EmailSenderProfileTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(
                    ConsoleEmailSender.class,
                    InMemoryEmailSender.class,
                    SmtpEmailSender.class,
                    DisabledEmailSender.class,
                    ProdMailConfiguration.class);

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
    void prodUsesSmtpSenderWhenMailHostIsSet() {
        runner.withPropertyValues("spring.profiles.active=prod", "spring.mail.host=smtp.example.invalid")
                .run(context -> {
                    assertThat(context).hasSingleBean(EmailSender.class);
                    assertThat(context.getBean(EmailSender.class)).isInstanceOf(SmtpEmailSender.class);
                });
    }

    @Test
    void prodDisablesMailWhenMailHostIsMissing() {
        new ApplicationContextRunner()
                .withUserConfiguration(
                        ConsoleEmailSender.class,
                        InMemoryEmailSender.class,
                        SmtpEmailSender.class,
                        DisabledEmailSender.class)
                .withPropertyValues("spring.profiles.active=prod")
                .run(context -> {
                    assertThat(context).hasSingleBean(EmailSender.class);
                    assertThat(context.getBean(EmailSender.class)).isInstanceOf(DisabledEmailSender.class);
                    assertThat(context).doesNotHaveBean(JavaMailSender.class);
                });
    }

    @Test
    void prodYamlDoesNotRequireSmtpPlaceholders() throws Exception {
        String prod = Files.readString(Path.of("src/main/resources/application-prod.yml"));
        String mail = Files.readString(Path.of("src/main/resources/application-mail.yml"));
        assertThat(prod).doesNotContain("${SPRING_MAIL_HOST}");
        assertThat(prod).doesNotContain("${SPRING_MAIL_USERNAME}");
        assertThat(prod).doesNotContain("${SPRING_MAIL_PASSWORD}");
        assertThat(mail).contains("on-property: spring.mail.host");
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
