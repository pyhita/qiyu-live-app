package com.yangtao.interfaces;

import org.springframework.beans.BeanUtils;

import java.util.List;
import java.util.stream.Collectors;

public class ConvertBeanUtils {

    public static <T> T convert(Object source, Class<T> targetClass) {
        if (source == null) {
            return null;
        }

        T target = newInstånce(targetClass);
        BeanUtils.copyProperties(source, target);

        return target;
    }

    public static <K, T> List<T> convertList(List<K> sources, Class<T> targetClass) {
        if (sources == null) {
            return null;
        }

        return sources.stream()
                .map(k -> convert(k, targetClass))
                .collect(Collectors.toList());
    }

    private static <T> T newInstånce(Class<T> targetClass) {
        try {
            return targetClass.newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
