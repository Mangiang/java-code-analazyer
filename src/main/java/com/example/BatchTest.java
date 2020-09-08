package com.example;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.builder.SpringApplicationBuilder;
import spoon.MavenLauncher;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.visitor.filter.TypeFilter;
import spoon.support.reflect.code.CtLiteralImpl;

import java.util.*;

public class BatchTest implements ApplicationRunner {
    public static void main(String... args) {
        try {
            new SpringApplicationBuilder(BatchTest.class).run(args);
        } catch (Exception e) {
            System.exit(-1);
        }
        System.exit(0);
    }

    public void run(ApplicationArguments args) {
        MavenLauncher launcher = new MavenLauncher(System.getenv("SRC_DIR"), MavenLauncher.SOURCE_TYPE.APP_SOURCE);
        launcher.buildModel();
        CtModel model = launcher.getModel();
        for (CtClass<?> s : model.getElements(new TypeFilter<CtClass<?>>(CtClass.class))) {

            for (CtMethod<?> method : model.getElements(new TypeFilter<CtMethod<?>>(CtMethod.class))) {
                if (!method.getDeclaringType().getQualifiedName().equals(s.getQualifiedName()))
                    continue;
                final List<CtAnnotation<?>> methodAnnotations = method.getAnnotations();
                Optional<CtAnnotation<?>> foundRequestMapping = Optional.empty();
                for (CtAnnotation<?> ctAnnotation : methodAnnotations) {
                    if (ctAnnotation.getAnnotationType().getSimpleName().equals("RequestMapping")) {
                        foundRequestMapping = Optional.of(ctAnnotation);
                        break;
                    }
                }
                if (foundRequestMapping.equals(Optional.empty())) {
                    continue;
                }

                final CtAnnotation<?> requestMappingAnnotation = foundRequestMapping.get();

                if (methodAnnotations.stream().noneMatch(ctAnnotation -> ctAnnotation.getAnnotationType().getSimpleName().equals("RequestMapping")))
                    continue;

                final ArrayList<CtInvocation<?>> invocs = new ArrayList<>(method.getElements(new TypeFilter<>(CtInvocation.class)));

                while (invocs.size() > 0) {
                    final CtInvocation<?> currentInvocation = invocs.remove(0);
                    final String methodName = currentInvocation.getExecutable().getSimpleName();
                    if (currentInvocation.getTarget() == null || currentInvocation.getExecutable() == null)
                        continue;
                    final CtType<?> targetClassDeclaration = currentInvocation.getTarget().getType().getDeclaration();
                    if (targetClassDeclaration == null)
                        continue;
                    final List<CtAnnotation<?>> currentClassAnnotations = targetClassDeclaration.getAnnotations();
                    final CtMethod<?> targetMethod = targetClassDeclaration.getMethod(methodName);
                    final List<CtAnnotation<?>> targetMethodAnnotations = targetMethod.getAnnotations();

                    Optional<CtAnnotation<?>> foundFeignClient = Optional.empty();
                    for (CtAnnotation<?> ctAnnotation : currentClassAnnotations) {
                        if (ctAnnotation.getAnnotationType().getSimpleName().equals("FeignClient")) {
                            foundFeignClient = Optional.of(ctAnnotation);
                            break;
                        }
                    }
                    if (!foundFeignClient.equals(Optional.empty())) {
                        final CtAnnotation<?> feignClientAnnotation = foundFeignClient.get();
                        Optional<CtAnnotation<?>> found = Optional.empty();
                        for (CtAnnotation<?> ctAnnotation : targetMethodAnnotations) {
                            if (ctAnnotation.getAnnotationType().getSimpleName().equals("RequestMapping")) {
                                found = Optional.of(ctAnnotation);
                                break;
                            }
                        }
                        if (!found.equals(Optional.empty())) {
                            final CtAnnotation<?> feignAnnotation = found.get();
                            System.out.println("[" + s.getSimpleName() + "] " + requestMappingAnnotation.getValue("value") + " -> [" + targetClassDeclaration.getSimpleName() +"] " + ((CtLiteralImpl<?>) feignClientAnnotation.getValue("value")).getValue() + ((CtLiteralImpl<?>) feignAnnotation.getValue("value")).getValue());
                            continue;
                        }
                    }

                    final ArrayList<CtInvocation<?>> targetInvocs = new ArrayList<>(targetMethod.getElements(new TypeFilter<>(CtInvocation.class)));
                    invocs.addAll(targetInvocs);
                }
            }

        }
    }
}
