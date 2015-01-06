package de.glmtk.testutil;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.Parameterized;
import org.junit.runners.Suite;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import de.glmtk.common.ProbMode;
import de.glmtk.exceptions.SwitchCaseNotImplementedException;
import de.glmtk.querying.EstimatorTest;
import de.glmtk.querying.estimator.Estimator;
import de.glmtk.util.StringUtils;

/**
 * Highly specialized JUnit-Test Runner, so we can have nice code, output and
 * eclipse JUnit view for {@link EstimatorTest}.
 *
 * This class is heavily based on JUnits {@link Parameterized} Runner and
 * behaves similarly.
 */
public class EstimatorTestRunner extends Suite {
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public static @interface IgnoreProbMode {
        ProbMode[] value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public static @interface EstimatorTestParameters {
    }

    public static class EstimatorTestParams {
        private Estimator estimator;
        private boolean continuationEstimator;
        private int condOrder;
        private int margOrder;

        public EstimatorTestParams(Estimator estimator,
                                   boolean continuationEstimator,
                                   int condOrder,
                                   int margOrder) {
            this.estimator = estimator;
            this.continuationEstimator = continuationEstimator;
            this.condOrder = condOrder;
            this.margOrder = margOrder;
        }
    }

    public class TestRunnerForEstimator extends Suite {
        private class TestRunnerForProbMode extends BlockJUnit4ClassRunner {
            private ProbMode probMode;
            private int highestOrder;

            public TestRunnerForProbMode(Class<?> type,
                                         ProbMode probMode,
                                         int highestOrder) throws InitializationError {
                super(type);
                this.probMode = probMode;
                this.highestOrder = highestOrder;
            }

            @Override
            public Object createTest() throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
                return getTestClass().getOnlyConstructor().newInstance(
                        estimator, continuationEstimator, probMode,
                        highestOrder);
            }

            @Override
            protected String getName() {
                switch (probMode) {
                    case COND:
                        return "Conditional";
                    case MARG:
                        return "Marginal";
                    default:
                        throw new SwitchCaseNotImplementedException();
                }
            }

            @Override
            protected String testName(FrameworkMethod method) {
                return String.format("%s - %s (%s)", method.getName(),
                        TestRunnerForEstimator.this.getName(), getName());
            }

            @Override
            protected void validateConstructor(List<Throwable> errors) {
                validateOnlyOneConstructor(errors);
                Constructor<?> constructor = getTestClass().getJavaClass().getConstructors()[0];
                Class<?>[] types = constructor.getParameterTypes();
                if (types.length != 4 || !types[0].equals(Estimator.class)
                        || !types[1].equals(boolean.class)
                        || !types[2].equals(ProbMode.class)
                        || !types[3].equals(int.class))
                    errors.add(new Exception(
                            "Test class constructor should take exactly these arguments: (Estimator, boolean, ProbMode, integer)"));
            }

            @Override
            protected Statement classBlock(RunNotifier notifier) {
                return childrenInvoker(notifier);
            }

            @Override
            protected Annotation[] getRunnerAnnotations() {
                return new Annotation[0];
            }
        }

        private Estimator estimator;
        private boolean continuationEstimator;
        private final List<TestRunnerForProbMode> runners;

        public TestRunnerForEstimator(Class<?> type,
                                      EstimatorTestParams params) throws InitializationError {
            super(type, Collections.<Runner> emptyList());

            estimator = params.estimator;
            continuationEstimator = params.continuationEstimator;

            runners = new ArrayList<>();
            createRunners(params);
        }

        @Override
        protected String getName() {
            // Due to https://bugs.eclipse.org/bugs/show_bug.cgi?id=102512
            // we have to replace () with [].
            return StringUtils.replaceAll(estimator.getName(), "()", "[]")
                    + " Estimator";
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        @Override
        protected List<Runner> getChildren() {
            return (List) runners;
        }

        private void createRunners(EstimatorTestParams params) throws InitializationError {
            if (!ignoredProbModes.contains(ProbMode.COND)
                    && params.condOrder != 0)
                runners.add(new TestRunnerForProbMode(
                        getTestClass().getJavaClass(), ProbMode.COND,
                        params.condOrder));
            if (!ignoredProbModes.contains(ProbMode.MARG)
                    && params.margOrder != 0)
                runners.add(new TestRunnerForProbMode(
                        getTestClass().getJavaClass(), ProbMode.MARG,
                        params.margOrder));
        }
    }

    private Set<ProbMode> ignoredProbModes;
    private List<TestRunnerForEstimator> runners;

    public EstimatorTestRunner(Class<?> type) throws Throwable {
        super(type, Collections.<Runner> emptyList());
        loadIgnoredProbModes();
        createRunnersForParameters(allParameters());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    protected List<Runner> getChildren() {
        return (List) runners;
    }

    private void loadIgnoredProbModes() {
        ignoredProbModes = new HashSet<>();
        for (Annotation annotation : getTestClass().getAnnotations())
            if (annotation instanceof IgnoreProbMode) {
                IgnoreProbMode a = (IgnoreProbMode) annotation;
                for (ProbMode probMode : a.value())
                    ignoredProbModes.add(probMode);
            }
    }

    @SuppressWarnings("unchecked")
    private Iterable<EstimatorTestParams> allParameters() throws Throwable {
        Object parameters = getParametersMethod().invokeExplosively(null);
        if (parameters instanceof Iterable<?>)
            return (Iterable<EstimatorTestParams>) parameters;

        throw parametersMethodReturnedWrongType();
    }

    private FrameworkMethod getParametersMethod() throws Exception {
        List<FrameworkMethod> methods = getTestClass().getAnnotatedMethods(
                EstimatorTestParameters.class);
        for (FrameworkMethod method : methods)
            if (method.isStatic() && method.isPublic())
                return method;

        throw new Exception(String.format(
                "No public static parameters method on class %s.",
                getTestClass().getName()));
    }

    private void createRunnersForParameters(Iterable<EstimatorTestParams> allParameters) throws InitializationError {
        runners = new ArrayList<>();
        for (EstimatorTestParams parameters : allParameters)
            runners.add(new TestRunnerForEstimator(
                    getTestClass().getJavaClass(), parameters));
    }

    private Exception parametersMethodReturnedWrongType() throws Exception {
        return new Exception(String.format(
                "%s.%s() must return an Iterable of %s.",
                getTestClass().getName(), getParametersMethod().getName(),
                EstimatorTestParams.class.getName()));
    }
}
