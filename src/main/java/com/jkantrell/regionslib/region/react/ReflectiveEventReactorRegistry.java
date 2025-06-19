package com.jkantrell.regionslib.region.react;

import com.jkantrell.regionslib.region.RegionContext;
import io.avaje.lang.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public abstract class ReflectiveEventReactorRegistry<T extends RegionEventReactor> extends EventReactorRegistry<T> {

    private final Class<T> class_;
    private final Class<? extends Annotation> flag_;

    public ReflectiveEventReactorRegistry(RegionContext context, Class<T> reactorClass, Class<? extends Annotation> flagAnnotation) {
        super(context);
        this.class_ = reactorClass;
        this.flag_ = flagAnnotation;
    }

    public void registerFrom(Class<?> clazz) {
        this.extract(clazz, null).forEach(this::register);
    }
    public void registerFrom(Object obj) {
        this.extract(obj.getClass(), obj).forEach(this::register);
    }

    @SuppressWarnings("unchecked")
    protected List<T> extract(Class<?> clazz, @Nullable Object obj) {
        var members = Stream.concat(Arrays.stream(clazz.getFields()), Arrays.stream(clazz.getMethods())).toList();

        List<T> reactors = new ArrayList<>();

        for (var member : members) {
            if (!member.isAnnotationPresent(this.flag_)) { continue; }

            Class<?> type = (member instanceof Field f) ? f.getType() : ((Method) member).getReturnType();
            if (!this.class_.isAssignableFrom(type)) {
                this.plugin_.getServer().getLogger().warning(
                        "Member '" + member.getName() + "' of class '" + clazz.getName() + "' annotated as @" + this.flag_.getSimpleName() + ", but is not of type '" + this.class_.getName() + "'. Ignored."
                );
                continue;
            }

            boolean isStatic = Modifier.isStatic(member.getModifiers());
            if ((obj == null && !isStatic) || (obj != null && isStatic)) { continue; }

            T r;
            try {
                r = (T) ((member instanceof Field f) ? f.get(obj) : ((Method) member).invoke(obj));
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
            if (r == null) { continue; }
            if (r instanceof ReflectiveNameable unNamed) {
                unNamed.setName(member.getName());
            }
            reactors.add(r);
        }

        return reactors;
    }
}
