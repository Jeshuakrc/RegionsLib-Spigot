package com.kntrel.mc.regionLib.trigger;

import com.kntrel.mc.regionLib.region.context.RegionContext;
import com.kntrel.mc.regionLib.trigger.build.ReflectiveNameable;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public abstract class ReflectiveTriggerListernerRegistry<T extends RegionTrigger<? extends Event>, L extends TriggerListener<? extends T>>
extends TriggerListenerRegistry<T, L> {

    private final Class<L> listenerClass_;
    private final Class<? extends Annotation> flag_;

    public ReflectiveTriggerListernerRegistry(RegionContext context, Class<L> reactorClass, Class<? extends Annotation> flagAnnotation) {
        super(context);
        this.listenerClass_ = reactorClass;
        this.flag_ = flagAnnotation;
    }

    public void registerFrom(Class<?> clazz) {
        this.extract(clazz, null).forEach(this::register);
    }
    public void registerFrom(Object obj) {
        this.extract(obj.getClass(), obj).forEach(this::register);
    }

    @SuppressWarnings("unchecked")
    protected List<L> extract(Class<?> clazz, @Nullable Object obj) {
        var members = Stream.concat(Arrays.stream(clazz.getFields()), Arrays.stream(clazz.getMethods())).toList();

        List<L> listeners = new ArrayList<>();

        for (var member : members) {
            if (!member.isAnnotationPresent(this.flag_)) { continue; }

            Class<?> type = (member instanceof Field f) ? f.getType() : ((Method) member).getReturnType();
            if (!this.listenerClass_.isAssignableFrom(type)) {
                this.plugin_.getServer().getLogger().warning(
                        "Member '" + member.getName() + "' of class '" + clazz.getName() + "' annotated as @" + this.flag_.getSimpleName() + ", but is not of type '" + this.listenerClass_.getName() + "'. Ignored."
                );
                continue;
            }

            boolean isStatic = Modifier.isStatic(member.getModifiers());
            if ((obj == null && !isStatic) || (obj != null && isStatic)) { continue; }

            L r;
            try {
                r = (L) ((member instanceof Field f) ? f.get(obj) : ((Method) member).invoke(obj));
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
            if (r == null) { continue; }
            if (r instanceof ReflectiveNameable<?> unNamed) {
                r = (L) unNamed.namedAs(member.getName());
            }
            listeners.add(r);
        }

        return listeners;
    }

}
