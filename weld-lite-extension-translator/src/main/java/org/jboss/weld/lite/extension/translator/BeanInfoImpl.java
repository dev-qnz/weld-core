package org.jboss.weld.lite.extension.translator;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.stream.Collectors;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.build.compatible.spi.BeanInfo;
import jakarta.enterprise.inject.build.compatible.spi.DisposerInfo;
import jakarta.enterprise.inject.build.compatible.spi.InjectionPointInfo;
import jakarta.enterprise.inject.build.compatible.spi.InvokerInfo;
import jakarta.enterprise.inject.build.compatible.spi.ScopeInfo;
import jakarta.enterprise.inject.build.compatible.spi.StereotypeInfo;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Decorator;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.invoke.InvokerBuilder;
import jakarta.enterprise.lang.model.AnnotationInfo;
import jakarta.enterprise.lang.model.declarations.ClassInfo;
import jakarta.enterprise.lang.model.declarations.FieldInfo;
import jakarta.enterprise.lang.model.declarations.MethodInfo;
import jakarta.enterprise.lang.model.types.Type;

import org.jboss.weld.exceptions.DeploymentException;
import org.jboss.weld.invokable.InvokerInfoBuilder;
import org.jboss.weld.lite.extension.translator.util.reflection.AnnotatedTypes;

class BeanInfoImpl implements BeanInfo {
    final jakarta.enterprise.inject.spi.Bean<?> cdiBean;
    final jakarta.enterprise.inject.spi.Annotated cdiDeclaration;
    final jakarta.enterprise.inject.spi.AnnotatedParameter<?> cdiDisposerDeclaration;
    final BeanManager bm;

    BeanInfoImpl(jakarta.enterprise.inject.spi.Bean<?> cdiBean, jakarta.enterprise.inject.spi.Annotated cdiDeclaration,
            jakarta.enterprise.inject.spi.AnnotatedParameter<?> cdiDisposerDeclaration, BeanManager bm) {
        this.cdiBean = cdiBean;
        this.cdiDeclaration = cdiDeclaration;
        this.cdiDisposerDeclaration = cdiDisposerDeclaration;
        this.bm = bm;
    }

    @Override
    public ScopeInfo scope() {
        jakarta.enterprise.inject.spi.AnnotatedType<?> scopeType = bm.createAnnotatedType(cdiBean.getScope());
        boolean isNormal = scopeType.isAnnotationPresent(jakarta.enterprise.context.NormalScope.class);
        return new ScopeInfoImpl(new ClassInfoImpl(scopeType, bm), isNormal);
    }

    @Override
    public Collection<Type> types() {
        return cdiBean.getTypes()
                .stream()
                .map(it -> TypeImpl.fromReflectionType(AnnotatedTypes.from(it), bm))
                .collect(Collectors.toList());
    }

    @Override
    public Collection<AnnotationInfo> qualifiers() {
        return cdiBean.getQualifiers()
                .stream()
                .map(annotation -> new AnnotationInfoImpl(annotation, bm))
                .collect(Collectors.toList());
    }

    @Override
    public ClassInfo declaringClass() {
        jakarta.enterprise.inject.spi.AnnotatedType<?> beanClass = bm.createAnnotatedType(cdiBean.getBeanClass());
        return new ClassInfoImpl(beanClass, bm);
    }

    @Override
    public boolean isClassBean() {
        return cdiDeclaration instanceof jakarta.enterprise.inject.spi.AnnotatedType;
    }

    @Override
    public boolean isProducerMethod() {
        return cdiDeclaration instanceof jakarta.enterprise.inject.spi.AnnotatedMethod;
    }

    @Override
    public boolean isProducerField() {
        return cdiDeclaration instanceof jakarta.enterprise.inject.spi.AnnotatedField;
    }

    @Override
    public boolean isSynthetic() {
        return cdiDeclaration == null;
    }

    @Override
    public MethodInfo producerMethod() {
        if (cdiDeclaration instanceof jakarta.enterprise.inject.spi.AnnotatedMethod) {
            return new MethodInfoImpl((jakarta.enterprise.inject.spi.AnnotatedMethod<?>) cdiDeclaration, bm);
        }
        return null;
    }

    @Override
    public FieldInfo producerField() {
        if (cdiDeclaration instanceof jakarta.enterprise.inject.spi.AnnotatedField) {
            return new FieldInfoImpl((jakarta.enterprise.inject.spi.AnnotatedField<?>) cdiDeclaration, bm);
        }
        return null;
    }

    @Override
    public boolean isAlternative() {
        return cdiBean.isAlternative();
    }

    @Override
    public Integer priority() {
        if (cdiDeclaration instanceof jakarta.enterprise.inject.spi.AnnotatedType
                && cdiDeclaration.isAnnotationPresent(Priority.class)) {
            return cdiDeclaration.getAnnotation(Priority.class).value();
        }
        if (cdiBean instanceof jakarta.enterprise.inject.spi.Prioritized) {
            return ((jakarta.enterprise.inject.spi.Prioritized) cdiBean).getPriority();
        }

        return null;
    }

    @Override
    public String name() {
        return cdiBean.getName();
    }

    @Override
    public DisposerInfo disposer() {
        if (cdiDisposerDeclaration != null) {
            return new DisposerInfoImpl(cdiDisposerDeclaration, bm);
        }
        return null;
    }

    @Override
    public Collection<StereotypeInfo> stereotypes() {
        return cdiBean.getStereotypes()
                .stream()
                .map(aClass -> new StereotypeInfoImpl(aClass, bm))
                .collect(Collectors.toList());
    }

    @Override
    public Collection<InjectionPointInfo> injectionPoints() {
        return cdiBean.getInjectionPoints()
                .stream()
                .map((InjectionPoint cdiInjectionPoint) -> new InjectionPointInfoImpl(cdiInjectionPoint, bm))
                .collect(Collectors.toList());
    }

    @Override
    public InvokerBuilder<InvokerInfo> createInvoker(MethodInfo methodInfo) {
        if (!isClassBean()) {
            throw new DeploymentException("Cannot build invoker for a bean which is not a managed bean: " + this);
        }
        if (isInterceptor()) {
            throw new DeploymentException("Cannot build invoker for an interceptor: " + this);
        }
        if (cdiBean instanceof Decorator) { // not representable in BCE, but can theoretically happen
            throw new DeploymentException("Cannot build invoker for a decorator: " + this);
        }

        if (methodInfo.isConstructor()) {
            throw new DeploymentException("Cannot build invoker for a constructor: " + methodInfo);
        }
        if (Modifier.isPrivate(methodInfo.modifiers())) {
            throw new DeploymentException("Cannot build invoker for a private method: " + methodInfo);
        }
        if ("java.lang.Object".equals(methodInfo.declaringClass().name())
                && !"toString".equals(methodInfo.name())) {
            throw new DeploymentException("Cannot build invoker for a method declared on java.lang.Object: " + methodInfo);
        }

        if (methodInfo instanceof MethodInfoImpl) {
            // at this point we can be sure it is a Method, not a Constructor, so we cast it
            AnnotatedMethod<?> cdiMethod = (AnnotatedMethod<?>) ((MethodInfoImpl) methodInfo).cdiDeclaration;

            // verify that the `methodInfo` belongs to this bean
            if (!ReflectionMembers.allMethods(cdiBean.getBeanClass()).contains(cdiMethod.getJavaMember())) {
                throw new DeploymentException("Method does not belong to bean " + cdiBean.getBeanClass().getName()
                        + ": " + methodInfo);
            }

            return new InvokerInfoBuilder<>(cdiBean.getBeanClass(), cdiMethod.getJavaMember(), bm);
        } else {
            throw new DeploymentException("Custom implementations of MethodInfo are not supported!");
        }
    }

    @Override
    public String toString() {
        return "@" + cdiBean.getScope().getSimpleName() + " bean [types=" + cdiBean.getTypes()
                + ", qualifiers=" + cdiBean.getQualifiers() + "]"
                + (cdiDeclaration != null ? " declared at " + cdiDeclaration : "");
    }
}
