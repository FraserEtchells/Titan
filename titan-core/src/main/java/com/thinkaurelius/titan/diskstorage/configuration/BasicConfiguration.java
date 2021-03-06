package com.thinkaurelius.titan.diskstorage.configuration;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

/**
 *
 * @author Matthias Broecheler (me@matthiasb.com)
 */
public class BasicConfiguration extends AbstractConfiguration {

    private static final Logger log =
            LoggerFactory.getLogger(BasicConfiguration.class);

    public enum Restriction { LOCAL, GLOBAL, NONE }

    protected static final String FROZEN_KEY = "hidden.frozen";

    private final ReadConfiguration config;
    private final Restriction restriction;
    protected boolean isFrozen;

    public BasicConfiguration(ConfigNamespace root, ReadConfiguration config, Restriction restriction) {
        super(root);
        Preconditions.checkNotNull(config);
        Preconditions.checkNotNull(restriction);
        this.config = config;
        this.restriction = restriction;
        Boolean frozen = config.get(FROZEN_KEY,Boolean.class);
        if (frozen==null) this.isFrozen=false;
        else this.isFrozen=frozen;
    }

    protected void verifyOption(ConfigOption option) {
        Preconditions.checkNotNull(option);
        super.verifyElement(option);
        if (restriction==Restriction.GLOBAL) Preconditions.checkArgument(option.isGlobal(),"Can only accept global options: %s",option);
        else if (restriction==Restriction.LOCAL) Preconditions.checkArgument(option.isLocal(),"Can only accept local options: %s",option);
    }


    @Override
    public boolean has(ConfigOption option, String... umbrellaElements) {
        verifyOption(option);
        return config.get(super.getPath(option,umbrellaElements),option.getDatatype())!=null;
    }

    @Override
    public<O> O get(ConfigOption<O> option, String... umbrellaElements) {
        verifyOption(option);
        O result = config.get(super.getPath(option,umbrellaElements),option.getDatatype());
        return option.get(result);
    }

    @Override
    public Set<String> getContainedNamespaces(ConfigNamespace umbrella, String... umbrellaElements) {
        return super.getContainedNamespaces(config,umbrella,umbrellaElements);
    }

    @Override
    public Map<String, Object> getSubset(ConfigNamespace umbrella, String... umbrellaElements) {
        return super.getSubset(config,umbrella,umbrellaElements);
    }

    @Override
    public Configuration restrictTo(String... umbrellaElements) {
        return restrictTo(this,umbrellaElements);
    }

    public Map<ConfigElement.PathIdentifier,Object> getAll() {
        Map<ConfigElement.PathIdentifier,Object> result = Maps.newHashMap();

        for (String key : config.getKeys("")) {
            Preconditions.checkArgument(StringUtils.isNotBlank(key));
            try {
                ConfigElement.PathIdentifier pid = ConfigElement.parse(getRootNamespace(),key);
                Preconditions.checkArgument(pid.element.isOption() && !pid.lastIsUmbrella);
                result.put(pid,get((ConfigOption)pid.element,pid.umbrellaElements));
            } catch (IllegalArgumentException e) {
                log.info("Ignored configuration entry for {} since it does not map to an option",key,e);
                continue;
            }
        }
        return result;
    }

    public boolean isFrozen() {
        return isFrozen;
    }

    public ReadConfiguration getConfiguration() {
        return config;
    }

    @Override
    public void close() {
        config.close();
    }
}
