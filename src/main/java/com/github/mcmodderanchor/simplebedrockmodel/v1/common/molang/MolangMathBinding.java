package com.github.mcmodderanchor.simplebedrockmodel.v1.common.molang;

import team.unnamed.mocha.runtime.value.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public final class MolangMathBinding implements ObjectValue {
    public static final MolangMathBinding INSTANCE = new MolangMathBinding();

    private static final double RADIAN = Math.toRadians(1);
    private static final Random RANDOM = new Random();

    private final Map<String, ObjectProperty> entries = new HashMap<>();

    private MolangMathBinding() {
        func1("abs", Math::abs);
        func1("acos", v -> normalize(Math.acos(v) / RADIAN));
        func1("asin", v -> normalize(Math.asin(v) / RADIAN));
        func1("atan", v -> Math.atan(v) / RADIAN);
        func2("atan2", (y, x) -> Math.atan2(y, x) / RADIAN);
        func1("ceil", Math::ceil);
        func3("clamp", (v, min, max) -> Math.max(Math.min(v, max), min));
        func1("cos", v -> Math.cos(v * RADIAN));
        func3("die_roll", (amount, low, high) -> {
            double result = 0;
            for (int i = 0; i < (int) amount; i++) result += RANDOM.nextInt((int) high) + low;
            return result / 4.0;
        });
        func3("die_roll_integer", (amount, low, high) -> {
            int result = 0;
            for (int i = 0; i < (int) amount; i++) result += RANDOM.nextInt((int) low, (int) high);
            return result;
        });
        func1("exp", Math::exp);
        func1("floor", Math::floor);
        func1("hermite_blend", t -> { double t2 = t * t; return 3 * t2 - 2 * t2 * t; });
        func3("lerp", (start, end, t) -> start + t * (end - start));
        func3("lerprotate", MolangMathBinding::lerpRotate);
        func1("ln", Math::log);
        func2("max", Math::max);
        func2("min", Math::min);
        func1("min_angle", MolangMathBinding::minAngle);
        func2("mod", (a, b) -> a % b);
        constant("pi", Math.PI);
        func2("pow", Math::pow);
        func2("random", (min, max) -> RANDOM.nextDouble(min, max));
        func2("random_integer", (min, max) -> (double) RANDOM.nextInt((int) min, (int) max));
        func1("round", v -> (double) Math.round(v));
        func1("sin", v -> Math.sin(v * RADIAN));
        func1("sqrt", Math::sqrt);
        func1("trunc", v -> v - v % 1);
    }

    private void constant(String name, double value) {
        entries.put(name, ObjectProperty.property(NumberValue.of(value), true));
    }

    private void func1(String name, DoubleFunction1 fn) {
        entries.put(name, ObjectProperty.property(
                (Function<?>) (ctx, args) -> NumberValue.of(fn.apply(args.next().eval().getAsNumber())),
                true));
    }

    private void func2(String name, DoubleFunction2 fn) {
        entries.put(name, ObjectProperty.property(
                (Function<?>) (ctx, args) -> NumberValue.of(fn.apply(
                        args.next().eval().getAsNumber(),
                        args.next().eval().getAsNumber())),
                true));
    }

    private void func3(String name, DoubleFunction3 fn) {
        entries.put(name, ObjectProperty.property(
                (Function<?>) (ctx, args) -> NumberValue.of(fn.apply(
                        args.next().eval().getAsNumber(),
                        args.next().eval().getAsNumber(),
                        args.next().eval().getAsNumber())),
                true));
    }

    @Override
    public ObjectProperty getProperty(String name) {
        return entries.get(name);
    }

    private static double normalize(double v) {
        return Double.isNaN(v) || Double.isInfinite(v) ? 0 : v;
    }

    private static double minAngle(double angle) {
        while (angle > 180) angle -= 360;
        while (angle < -180) angle += 360;
        return angle;
    }

    private static double lerpRotate(double start, double end, double lerp) {
        start = radify(start);
        end = radify(end);
        if (start > end) { double tmp = start; start = end; end = tmp; }
        double diff = end - start;
        if (diff > 180) return radify(end + lerp * (360 - diff));
        else return start + lerp * diff;
    }

    private static double radify(double n) {
        return (((n + 180) % 360) + 180) % 360;
    }

    @FunctionalInterface
    private interface DoubleFunction1 { double apply(double a); }
    @FunctionalInterface
    private interface DoubleFunction2 { double apply(double a, double b); }
    @FunctionalInterface
    private interface DoubleFunction3 { double apply(double a, double b, double c); }
}
