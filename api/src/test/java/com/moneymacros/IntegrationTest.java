package com.moneymacros;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * One base for every database test: a full {@link SpringBootTest} context wired
 * to the shared PostgreSQL Testcontainer. Because all database tests carry the
 * same annotations, Spring reuses a single application context and a single
 * container across them, so the suite starts PostgreSQL once.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringBootTest
@Import(TestcontainersConfiguration.class)
public @interface IntegrationTest {
}
