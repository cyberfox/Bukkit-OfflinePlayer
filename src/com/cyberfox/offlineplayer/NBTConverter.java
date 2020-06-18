package com.cyberfox.offlineplayer;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.ListTag;
import com.sk89q.jnbt.*;
import com.sk89q.jnbt.Tag;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NBTConverter {
  Class<?> nativeBase;
  Class<?> nativeCompound;
  Class<?> nativeList;
  Class<?> nativeByteArray;
  Class<?> nativeByte;
  Class<?> nativeShort;
  Class<?> nativeInt;
  Class<?> nativeLong;
  Class<?> nativeFloat;
  Class<?> nativeDouble;
  Class<?> nativeEnd;
  Class<?> nativeIntArray;
  Class<?> nativeLongArray;
  Class<?> nativeString;

  //  Compound Tag setter methods.
  Method setBoolean;
  Method setByte;
  Method setShort;
  Method setInteger;
  Method setLong;
  Method setFloat;
  Method setDouble;
  Method setByteArray;
  Method setCompoundTag;
  Method setString;

  public NBTConverter() {
    nativeBase = Util.getCraftClass("NBTBase");
    nativeByte = Util.getCraftClass("NBTTagByte");
    nativeShort = Util.getCraftClass("NBTTagShort");
    nativeInt = Util.getCraftClass("NBTTagInt");
    nativeLong = Util.getCraftClass("NBTTagLong");
    nativeFloat = Util.getCraftClass("NBTTagFloat");
    nativeDouble = Util.getCraftClass("NBTTagDouble");
    nativeString = Util.getCraftClass("NBTTagString");

    nativeByteArray = Util.getCraftClass("NBTTagByteArray");
    nativeIntArray = Util.getCraftClass("NBTTagIntArray");
    nativeLongArray = Util.getCraftClass("NBTTagLongArray");

    nativeEnd = Util.getCraftClass("NBTTagEnd");

    nativeList = Util.getCraftClass("NBTTagList");
    nativeCompound = Util.getCraftClass("NBTTagCompound");

    setBoolean = Util.getMethod(nativeCompound, "setBoolean", new Class<?>[] { String.class, boolean.class });
    setByte = Util.getMethod(nativeCompound, "setByte", new Class<?>[] { String.class, byte.class });
    setShort = Util.getMethod(nativeCompound, "setShort", new Class<?>[] { String.class, short.class });
    setInteger = Util.getMethod(nativeCompound, "setInteger", new Class<?>[] { String.class, int.class });
    setLong = Util.getMethod(nativeCompound, "setLong", new Class<?>[] { String.class, long.class });
    setFloat = Util.getMethod(nativeCompound, "setFloat", new Class<?>[] { String.class, float.class });
    setDouble = Util.getMethod(nativeCompound, "setDouble", new Class<?>[] { String.class, double.class });
    setByteArray = Util.getMethod(nativeCompound, "setByteArray", new Class<?>[] { String.class, byte[].class });
    setCompoundTag = Util.getMethod(nativeCompound, "setCompoundTag", new Class<?>[] { String.class, nativeCompound });
    setString = Util.getMethod(nativeCompound, "setString", new Class<?>[] { String.class, String.class });
  }

  public Object convert(List<Tag> rawList) {
    List<Object> converted = new ArrayList<>(rawList.size());
    for(Tag t : rawList) {
      converted.add(convert(t));
    }
    try {
      Constructor c = nativeList.getDeclaredConstructor(List.class, byte.class);
      c.setAccessible(true);
      if(converted.size() > 0) {
        Object o = converted.get(0);
        Method getType = nativeBase.getMethod("getTypeId");
        Byte b = (Byte) getType.invoke(o);
        return c.newInstance(converted, b.byteValue());
      } else {
        return c.newInstance(converted, (byte)0);
      }
    } catch(NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException none) {
      none.printStackTrace();
    }

    return null;
  }

  public Object convert(Tag from) {
    Object value = from.getValue();
    Class<?> valueClass = value.getClass();

    try {
      if (from instanceof ByteTag) return constructor(nativeByte, byte.class).newInstance(value);
      if (from instanceof ShortTag) return constructor(nativeShort, short.class).newInstance(value);
      if (from instanceof IntTag) return constructor(nativeInt, int.class).newInstance(value);
      if (from instanceof LongTag) return constructor(nativeLong, long.class).newInstance(value);
      if (from instanceof FloatTag) return constructor(nativeFloat, float.class).newInstance(value);
      if (from instanceof DoubleTag) return constructor(nativeDouble, double.class).newInstance(value);
      if (from instanceof StringTag) return constructor(nativeString, valueClass).newInstance(value);

      if (from instanceof ByteArrayTag) return constructor(nativeByteArray, valueClass).newInstance(value);
      if (from instanceof IntArrayTag) return constructor(nativeIntArray, valueClass).newInstance(value);
      if (from instanceof LongArrayTag) return constructor(nativeLongArray, valueClass).newInstance(value);

      if (from instanceof EndTag) return constructor(nativeEnd).newInstance();

      if (from instanceof ListTag) {
        return convert(((ListTag)from).getValue());
      }

      if (from instanceof CompoundTag) {
        return convertCompound((CompoundTag)from);
      }
    } catch(NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException nope) {
      nope.printStackTrace();
    }
    return null;
  }

  private Object convertCompound(CompoundTag from) throws NoSuchMethodException, IllegalAccessException,
      InvocationTargetException, InstantiationException {
    Map<String, Object> convertedMap = new HashMap<>();
    for (Map.Entry<String, Tag> entry : from.getValue().entrySet()) {
      Tag tag = entry.getValue();
      String key = entry.getKey();
      convertedMap.put(key, convert(tag));
    }
    return constructor(nativeCompound, Map.class).newInstance(convertedMap);
  }

  private Constructor constructor(Class<?> c, Class<?> valueClass) throws NoSuchMethodException {
    Constructor out = c.getDeclaredConstructor(valueClass);
    out.setAccessible(true);
    return out;
  }

  private Constructor constructor(Class<?> c) throws NoSuchMethodException {
    Constructor out = c.getDeclaredConstructor();
    out.setAccessible(true);
    return out;
  }
}
