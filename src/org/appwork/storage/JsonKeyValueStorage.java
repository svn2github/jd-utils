package org.appwork.storage;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.ModifyLock;
import org.appwork.utils.logging.Log;

public class JsonKeyValueStorage extends Storage {

    private final Map<String, Object> internalMap;
    private final String              name;
    private final File                file;
    private final boolean             plain;
    private final byte[]              key;
    private boolean                   autoPutValues = true;
    private volatile boolean          closed        = false;
    private final AtomicLong          setMark       = new AtomicLong(0);
    private final AtomicLong          writeMark     = new AtomicLong(0);
    private boolean                   enumCacheEnabled;
    private final ModifyLock          modifyLock    = new ModifyLock();

    private final Map<String, Object> getMap() {
        return internalMap;
    }

    private final ModifyLock getLock() {
        return modifyLock;
    }

    public JsonKeyValueStorage(final File file) throws StorageException {
        this(file, false);
    }

    public JsonKeyValueStorage(final File file, final boolean plain) throws StorageException {
        this(file, plain, JSonStorage.KEY);
    }

    public JsonKeyValueStorage(final File file, final boolean plain, final byte[] key) throws StorageException {
        this(file, null, plain, key);
    }

    /**
     * @param file
     * @param resource
     * @param b
     * @param key2
     */
    public JsonKeyValueStorage(final File file, final URL resource, final boolean plain, final byte[] key) {
        this.internalMap = new HashMap<String, Object>();
        this.plain = plain;
        this.file = file;
        this.name = file.getName();
        this.key = key;
        if (resource != null) {
            Log.L.info("Load JSon Storage from Classpath url: " + resource);
            try {
                final HashMap<String, Object> load = JSonStorage.restoreFromString(IO.readURL(resource), plain, key, TypeRef.HASHMAP, new HashMap<String, Object>());
                this.putAll(load);
            } catch (final IOException e) {
                throw new WTFException(e);
            }
        }
        if (file.exists()) {
            Log.L.info("Prefer (merged) JSon Storage from File: " + file);
            final HashMap<String, Object> load = JSonStorage.restoreFrom(file, plain, key, TypeRef.HASHMAP, new HashMap<String, Object>());
            this.putAll(load);
        }
    }

    public JsonKeyValueStorage(final String name) throws StorageException {
        this(name, false);
    }

    public JsonKeyValueStorage(final String name, final boolean plain) throws StorageException {
        this(name, plain, JSonStorage.KEY);
    }

    public JsonKeyValueStorage(final String name, final boolean plain, final byte[] key) throws StorageException {
        this.internalMap = new HashMap<String, Object>();
        this.name = name;
        this.plain = plain;
        this.file = Application.getResource("cfg/" + name + (plain ? ".json" : ".ejs"));
        Log.L.finer("Read Config: " + this.file.getAbsolutePath());
        this.key = key;
        final HashMap<String, Object> load = JSonStorage.restoreFrom(this.file, plain, key, TypeRef.HASHMAP, new HashMap<String, Object>());
        this.putAll(load);
    }

    @Override
    public void clear() throws StorageException {
        getLock().writeLock();
        try {
            getMap().clear();
        } finally {
            getLock().writeUnlock();
            this.requestSave();
        }
    }

    @Override
    public void close() {
        this.closed = true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <E> E get(final String key, final E def) throws StorageException {
        final boolean readL = getLock().readLock();
        final boolean contains;
        Object ret = null;
        try {
            contains = getMap().containsKey(key);
            ret = contains ? getMap().get(key) : null;
        } finally {
            getLock().readUnlock(readL);
        }

        if (ret != null && def != null && ret.getClass() != def.getClass()) {
            /* ret class different from def class, so we have to convert */
            if (def instanceof Long) {
                if (ret instanceof Integer) {
                    ret = ((Integer) ret).longValue();
                } else if (ret instanceof String) {
                    ret = Long.parseLong((String) ret);
                }
            } else if (def instanceof Integer) {
                if (ret instanceof Long) {
                    ret = ((Long) ret).intValue();
                } else if (ret instanceof String) {
                    ret = Integer.parseInt((String) ret);
                }
            } else if (def instanceof Double) {
                if (ret instanceof Float) {
                    ret = ((Float) ret).doubleValue();
                }
            } else if (def instanceof Float) {
                if (ret instanceof Double) {
                    ret = ((Double) ret).floatValue();
                }
            }
        }
        // put entry if we have no entry
        if (!contains) {
            ret = def;
            if (this.autoPutValues) {
                if (def instanceof Boolean) {
                    this.put(key, (Boolean) def);
                } else if (def instanceof Long) {
                    this.put(key, (Long) def);
                } else if (def instanceof Integer) {
                    this.put(key, (Integer) def);
                } else if (def instanceof Byte) {
                    this.put(key, (Byte) def);
                } else if (def instanceof String || def == null) {
                    this.put(key, (String) def);
                } else if (def instanceof Enum<?>) {
                    this.put(key, (Enum<?>) def);
                } else if (def instanceof Double) {
                    this.put(key, (Double) def);
                } else if (def instanceof Float) {
                    this.put(key, (Float) def);
                } else {
                    throw new StorageException("Invalid datatype: " + (def != null ? def.getClass() : "null"));
                }
            }
        }

        if (def instanceof Enum<?> && ret instanceof String) {
            try {
                ret = Enum.valueOf(((Enum<?>) def).getDeclaringClass(), (String) ret);
                if (this.isEnumCacheEnabled()) {
                    this.put(key, (Enum<?>) ret);
                }
            } catch (final IllegalArgumentException e) {
                Log.L.info("Could not restore the enum. There is no value for " + ret + " in " + ((Enum<?>) def).getDeclaringClass());
                Log.exception(e);
                if (this.autoPutValues) {
                    this.put(key, (Enum<?>) def);
                }
                ret = def;
            } catch (final Throwable e) {

                Log.exception(e);
                if (this.autoPutValues) {
                    this.put(key, (Enum<?>) def);
                }
                ret = def;
            }
        }
        return (E) ret;
    }

    /**
     * @return the key
     */
    @Override
    public byte[] getCryptKey() {
        return this.key;
    }

    public File getFile() {
        return this.file;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.appwork.storage.Storage#getID()
     */
    @Override
    public String getID() {
        return this.file.getAbsolutePath();
    }

    public String getName() {
        return this.name;
    }

    @Override
    public boolean hasProperty(final String key) {
        final boolean readL = getLock().readLock();
        try {
            return getMap().containsKey(key);
        } finally {
            getLock().readUnlock(readL);
        }
    }

    private Object internal_put(final String key, final Object value) {
        if (key == null) {
            throw new WTFException("key == null is forbidden!");
        }
        final boolean readL = getLock().readLock();
        boolean requestSave = true;
        try {
            final Object ret = getMap().put(key, value);
            requestSave = !requestSave(ret, value);
            return ret;
        } finally {
            getLock().readUnlock(readL);
            if (requestSave) {
                this.requestSave();
            }
        }
    }

    private boolean requestSave(Object x, Object y) {
        try {
            if (x == null && y == null) {
                return true;
            } else if (x != null && y != null) {
                if (x == y || x.equals(y)) {
                    return true;
                } else {
                    if (x.getClass().isArray() && y.getClass().isArray()) {
                        final int xL = Array.getLength(x);
                        final int yL = Array.getLength(y);
                        if (xL == yL) {
                            for (int index = 0; index < xL; index++) {
                                final Object xE = Array.get(x, index);
                                final Object yE = Array.get(y, index);
                                if (requestSave(xE, yE) == false) {
                                    return false;
                                }
                            }
                            return true;
                        } else {
                            return false;
                        }
                    } else {
                        return false;
                    }
                }
            }
            return false;
        } catch (final Throwable e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * @return the autoPutValues
     */
    @Override
    public boolean isAutoPutValues() {
        return this.autoPutValues;
    }

    /**
     * @return
     */
    private boolean isEnumCacheEnabled() {
        return this.enumCacheEnabled;
    }

    public boolean isPlain() {
        return this.plain;
    }

    public void put(final String key, final boolean value) throws StorageException {
        this.internal_put(key, value);
    }

    @Override
    public void put(final String key, final Boolean value) throws StorageException {
        this.internal_put(key, value);
    }

    @Override
    public void put(final String key, final Byte value) throws StorageException {
        this.internal_put(key, value);
    }

    @Override
    public void put(final String key, final Double value) throws StorageException {
        this.internal_put(key, value);
    }

    @Override
    public void put(final String key, final Enum<?> value) throws StorageException {
        if (value == null) {
            this.internal_put(key, null);
        } else {
            if (this.isEnumCacheEnabled()) {
                this.internal_put(key, value);
            } else {
                this.internal_put(key, value.name());
            }
        }
    }

    @Override
    public void put(final String key, final Float value) throws StorageException {
        this.internal_put(key, value);
    }

    public void put(final String key, final int value) throws StorageException {
        this.internal_put(key, value);
    }

    @Override
    public void put(final String key, final Integer value) throws StorageException {
        this.internal_put(key, value);
    }

    public void put(final String key, final long value) throws StorageException {
        this.internal_put(key, value);
    }

    @Override
    public void put(final String key, final Long value) throws StorageException {
        this.internal_put(key, value);
    }

    @Override
    public void put(final String key, final String value) throws StorageException {
        this.internal_put(key, value);
    }

    private void putAll(final Map<String, Object> map) {
        if (map != null) {
            getLock().writeLock();
            try {
                final Iterator<Entry<String, Object>> it = map.entrySet().iterator();
                while (it.hasNext()) {
                    final Entry<String, Object> next = it.next();
                    if (next.getKey() != null) {
                        getMap().put(next.getKey(), next.getValue());
                    }
                }
            } finally {
                getLock().writeUnlock();
            }
        }
    }

    @Override
    public Object remove(final String key) {
        if (key == null) {
            throw new WTFException("key ==null is forbidden!");
        }
        if (hasProperty(key)) {
            getLock().writeLock();
            try {
                return getMap().remove(key);
            } finally {
                getLock().writeUnlock();
                this.requestSave();
            }
        }
        return null;
    }

    public void requestSave() {
        final long mark = this.setMark.incrementAndGet();
        if (false) {
            new WTFException("requestSave:" + this.getID() + "|" + mark).printStackTrace();
        }
    }

    @Override
    public void save() throws StorageException {
        if (this.closed) {
            throw new StorageException("StorageChest already closed!");
        }
        final long lastSetMark = this.setMark.get();
        if (this.writeMark.getAndSet(lastSetMark) != lastSetMark) {
            final String json;
            final boolean readL = getLock().readLock();
            try {
                json = JSonStorage.getMapper().objectToString(getMap());
                writeMark.set(setMark.get());
            } finally {
                getLock().readUnlock(readL);
            }
            JSonStorage.saveTo(this.file, this.plain, this.key, json);
        }
    }

    /**
     * @param autoPutValues
     *            the autoPutValues to set
     */
    @Override
    public void setAutoPutValues(final boolean autoPutValues) {
        this.autoPutValues = autoPutValues;
    }

    public void setEnumCacheEnabled(final boolean enumCacheEnabled) {
        this.enumCacheEnabled = enumCacheEnabled;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.appwork.storage.Storage#size()
     */
    @Override
    public int size() {
        final boolean readL = getLock().readLock();
        try {
            return getMap().size();
        } finally {
            getLock().readUnlock(readL);
        }
    }

    @Override
    public String toString() {
        final boolean readL = getLock().readLock();
        try {
            return JSonStorage.getMapper().objectToString(getMap());
        } catch (final Throwable e) {
            return getMap().toString();
        } finally {
            getLock().readUnlock(readL);
        }
    }

}
