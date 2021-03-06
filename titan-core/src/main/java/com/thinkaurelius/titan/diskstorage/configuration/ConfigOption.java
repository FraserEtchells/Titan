package com.thinkaurelius.titan.diskstorage.configuration;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

import java.lang.reflect.Array;
import java.util.Collection;

/**
 * @author Matthias Broecheler (me@matthiasb.com)
 */
public class ConfigOption<O> extends ConfigElement {

    public enum Type {
        /**
         * Once the database has been opened, these configuration options cannot
         * be changed for the entire life of the database
         */
        FIXED,
        /**
         * These options can only be changed for the entire database cluster at
         * once when all instances are shut down
         */
        GLOBAL_OFFLINE,
        /**
         * These options can only be changed globally across the entire database
         * cluster
         */
        GLOBAL,
        /**
         * These options are global but can be overwritten by a local
         * configuration file
         */
        MASKABLE,
        /**
         * These options can ONLY be provided through a local configuration file
         */
        LOCAL;
    }

    private final Type type;
    private final Class<O> datatype;
    private final O defaultValue;
    private final Predicate<O> verificationFct;
    private boolean isHidden = false;
    private ConfigOption<?> supersededBy;

    public ConfigOption(ConfigNamespace parent, String name, String description, Type type, O defaultValue) {
        this(parent,name,description,type,defaultValue, disallowEmpty((Class<O>) defaultValue.getClass()));
    }

    public ConfigOption(ConfigNamespace parent, String name, String description, Type type, O defaultValue, Predicate<O> verificationFct) {
        this(parent,name,description,type,(Class<O>)defaultValue.getClass(),defaultValue,verificationFct);
    }

    public ConfigOption(ConfigNamespace parent, String name, String description, Type type, Class<O> datatype) {
        this(parent,name,description,type,datatype, disallowEmpty(datatype));
    }

    public ConfigOption(ConfigNamespace parent, String name, String description, Type type, Class<O> datatype, Predicate<O> verificationFct) {
        this(parent,name,description,type,datatype,null,verificationFct);
    }

    public ConfigOption(ConfigNamespace parent, String name, String description, Type type, Class<O> datatype, O defaultValue) {
        this(parent,name,description,type,datatype,defaultValue,disallowEmpty(datatype));
    }

    public ConfigOption(ConfigNamespace parent, String name, String description, Type type, Class<O> datatype, O defaultValue, Predicate<O> verificationFct) {
        this(parent, name, description, type, datatype, defaultValue, verificationFct, null);
    }

    public ConfigOption(ConfigNamespace parent, String name, String description, Type type, Class<O> datatype, O defaultValue, Predicate<O> verificationFct, ConfigOption<?> supersededBy) {
        super(parent, name, description);
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(datatype);
        Preconditions.checkNotNull(verificationFct);
        this.type = type;
        this.datatype = datatype;
        this.defaultValue = defaultValue;
        this.verificationFct = verificationFct;
        this.supersededBy = supersededBy;
    }

    public ConfigOption<O> hide() {
        this.isHidden = true;
        return this;
    }

    public boolean isHidden() {
        return isHidden;
    }

    public Type getType() {
        return type;
    }

    public Class<O> getDatatype() {
        return datatype;
    }

    public O getDefaultValue() {
        return defaultValue;
    }

    public boolean isFixed() {
        return type==Type.FIXED;
    }

    public boolean isGlobal() {
        return type==Type.FIXED || type==Type.GLOBAL_OFFLINE || type==Type.GLOBAL || type==Type.MASKABLE;
    }

    public boolean isLocal() {
        return type==Type.MASKABLE || type==Type.LOCAL;
    }

    public boolean isDeprecated() {
        return null != supersededBy;
    }

    public ConfigOption<?> getDeprecationReplacement() {
        return supersededBy;
    }

    @Override
    public boolean isOption() {
        return true;
    }

    public O get(Object input) {
        if (input==null) {
            input=defaultValue;
        }
        if (input==null) {
            Preconditions.checkState(verificationFct.apply((O) input), "Need to set configuration value: %s", this.toString());
            return null;
        } else {
            return verify(input);
        }
    }

    public O verify(Object input) {
        Preconditions.checkNotNull(input);
        Preconditions.checkArgument(datatype.isInstance(input),"Invalid class for configuration value [%s]. Expected [%s] but given [%s]",this.toString(),datatype,input.getClass());
        O result = (O)input;
        Preconditions.checkArgument(verificationFct.apply(result),"Invalid configuration value for [%s]: %s",this.toString(),input);
        return result;
    }


    //########### HELPER METHODS ##################

    public static final<O> Predicate<O> disallowEmpty(Class<O> clazz) {
        return new Predicate<O>() {
            @Override
            public boolean apply(@Nullable O o) {
                if (o==null) return false;
                if (o.getClass().isArray() && (Array.getLength(o)==0 || Array.get(o,0)==null)) return false;
                if (o instanceof Collection && (((Collection)o).isEmpty() || ((Collection)o).iterator().next()==null)) return false;
                return true;
            }
        };
    }

    public static final Predicate<Integer> positiveInt() {
        return new Predicate<Integer>() {
            @Override
            public boolean apply(@Nullable Integer num) {
                return num!=null && num>0;
            }
        };
    }

    public static final Predicate<Integer> nonnegativeInt() {
        return new Predicate<Integer>() {
            @Override
            public boolean apply(@Nullable Integer num) {
                return num!=null && num>=0;
            }
        };
    }

    public static final Predicate<Long> positiveLong() {
        return new Predicate<Long>() {
            @Override
            public boolean apply(@Nullable Long num) {
                return num!=null && num>0;
            }
        };
    }


}
