package eu.einfracentral.utils;

import java.lang.reflect.Method;

public class ObjectUtils {

    public static Object merge(Object existing, Object update){
        if(!existing.getClass().isAssignableFrom(update.getClass())){
            return existing;
        }

        Method[] methods = existing.getClass().getMethods();

        for(Method fromMethod: methods){
            if(fromMethod.getDeclaringClass().equals(existing.getClass())
                    && fromMethod.getName().startsWith("get")){

                String fromName = fromMethod.getName();
                String toName = fromName.replace("get", "set");

                try {
                    Method toMetod = existing.getClass().getMethod(toName, fromMethod.getReturnType());
                    Object value = fromMethod.invoke(update, (Object[])null);
                    if(value != null){
                        toMetod.invoke(existing, value);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return existing;
    }
}
