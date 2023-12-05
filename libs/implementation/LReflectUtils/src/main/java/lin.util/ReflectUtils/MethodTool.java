package lin.util.ReflectUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MethodTool {
    private static final Map<String, Method> METHOD_CACHE = new HashMap<>();
    private TargetMethodInfo targetMethodInfo;

    private MethodTool() {

    }

    /**
     * @param findClassName 要查找的类名
     */
    public static MethodTool find(String findClassName) {
        MethodTool methodTool = new MethodTool();
        methodTool.targetMethodInfo = new TargetMethodInfo();
        methodTool.targetMethodInfo.findClassName = findClassName;
        return methodTool;
    }


    /**
     * @param findClass 要查找的类
     */
    public static MethodTool find(Class<?> findClass) {
        MethodTool methodTool = new MethodTool();
        methodTool.targetMethodInfo = new TargetMethodInfo();
        methodTool.targetMethodInfo.findClass = findClass;
        return methodTool;
    }

    /**
     * 构建方法签名
     *
     * @return com.linl.get(Object, int)的格式
     */
    private static StringBuilder buildMethodSignature(String findClass, String methodName, Class<?>[] paramTypes, Class<?> returnType) {
        StringBuilder sb = new StringBuilder();
        sb.append(findClass).append(".").append(methodName).append("(");
        for (Class<?> type : paramTypes) {
            sb.append(type.getName()).append(",");
        }
        if (sb.charAt(sb.length() - 1) == ',') sb.delete(sb.length() - 1, sb.length());
        sb.append(")");
        if (returnType != null) sb.append(returnType.getName());
        return sb;
    }

    public MethodTool name(String name) {
        this.targetMethodInfo.methodName = name;
        return this;
    }

    public MethodTool returnType(Class<?> returnType) {
        this.targetMethodInfo.returnType = returnType;
        return this;
    }

    public MethodTool params(Class<?>... methodParamsType) {
        this.targetMethodInfo.methodParams = methodParamsType;
        return this;
    }


    public <T> T call(Object runtimeObject, Object... params) {
        try {
            Method method = get();
            return (T) method.invoke(runtimeObject, params);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T callStatic(Object... params) {
        try {
            Method method = get();
            return (T) method.invoke(null, params);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public Method get() {
        TargetMethodInfo target = this.targetMethodInfo;

        //构造方法签名
        String signature = buildMethodSignature(target.findClassName, targetMethodInfo.methodName, target.methodParams, target.returnType).toString();
        if (METHOD_CACHE.containsKey(signature)) {
            return METHOD_CACHE.get(signature);
        }

        for (Class<?> currentFindClass = target.findClass == null ? ClassUtils.getClass(target.findClassName) : target.findClass; currentFindClass != Object.class; currentFindClass = currentFindClass.getSuperclass()) {
            MethodFor:
            for (Method method : currentFindClass.getDeclaredMethods()) {
                if ((method.getName().equals(target.methodName) || target.methodName == null) && (method.getReturnType().equals(target.returnType) || target.returnType == null)) {
                    Class<?>[] methodParams = method.getParameterTypes();
                    if (methodParams.length == target.methodParams.length) {
                        for (int i = 0; i < methodParams.length; i++) {
                            if (!Objects.equals(methodParams[i], target.methodParams[i]))
                                continue MethodFor;
                            if (!CheckClassType.CheckClass(methodParams[i], target.methodParams[i]))
                                continue MethodFor;
                        }
                        method.setAccessible(true);
                        METHOD_CACHE.put(signature, method);
                        return method;
                    }
                }
            }
        }
        throw new ReflectException("没有查找到方法 : " + signature);
    }

    private static class TargetMethodInfo {
        public Class<?> findClass;
        public String findClassName;
        public String methodName;
        public Class<?> returnType;
        public Class<?>[] methodParams = new Class[0];
    }
}
