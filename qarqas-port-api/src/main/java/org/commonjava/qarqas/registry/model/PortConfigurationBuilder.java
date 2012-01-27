package org.commonjava.qarqas.registry.model;

import java.util.HashMap;
import java.util.Map;

public class PortConfigurationBuilder
{

    private final Map<String, Integer> ports = new HashMap<String, Integer>();

    private Integer key;

    public PortConfigurationBuilder key( final Integer key )
    {
        this.key = key;
        return this;
    }

    public PortConfigurationBuilder port( final String name, final Integer port )
    {
        ports.put( name, port );
        return this;
    }

    public PortConfiguration build()
    {
        return new PortConfiguration( key, new HashMap<String, Integer>( ports ) );
    }

}
