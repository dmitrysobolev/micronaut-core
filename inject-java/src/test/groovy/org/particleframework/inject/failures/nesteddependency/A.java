package org.particleframework.inject.failures.nesteddependency;

import javax.inject.Singleton;

@Singleton
public class A {
    public A(C c) {

    }
}