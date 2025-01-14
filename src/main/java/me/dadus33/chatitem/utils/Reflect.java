package me.dadus33.chatitem.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

import me.dadus33.chatitem.ChatItem;

public class Reflect {

    public static Method getMethod(Class<?> clazz, String methodName, Class<?>... params) {
        try {
            return clazz.getMethod(methodName, params);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static Method getMethod(Class<?> clazz, Class<?> returned, Class<?>... params) {
        try {
        	for(Method m : clazz.getDeclaredMethods()) {
        		if(returned.equals(m.getReturnType()) && areParamsEquals(m.getParameterTypes(), params))
        			return m;
        	}
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    private static boolean areParamsEquals(Class<?>[] p1, Class<?>[] p2) {
		if(p1.length != p2.length)
			return false;
		for(int i = 0; i < p1.length; i++) {
			if(!p1[i].equals(p2[i]))
				return false;
		}
		return true;
    }

    public static Field getField(Class<?> clazz, String... fieldNames){
    	for(String field : fieldNames) {
	        try{
	            Field f = clazz.getDeclaredField(field);
	            f.setAccessible(true);
	            return f;
	        } catch (NoSuchFieldException e) {
	        	// go to next
	        }
    	}
    	ChatItem.getInstance().getLogger().severe("Failed to find field: " + Arrays.asList(fieldNames) + " in class: " + clazz.getCanonicalName());
        return null;
    }

}