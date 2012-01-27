package org.commonjava.qarqas.registry.model;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class PortConfiguration
    implements Comparable<PortConfiguration>, Iterable<Map.Entry<String, Integer>>
{

    private Integer key;

    private Map<String, Integer> ports;

    public PortConfiguration( final Integer key, final Map<String, Integer> ports )
    {
        this.key = key;
        this.ports = ports;
    }

    PortConfiguration()
    {
    }

    public Integer getKey()
    {
        return key;
    }

    public Map<String, Integer> getPorts()
    {
        return ports;
    }

    public Integer getPort( final String name )
    {
        return ports == null ? null : ports.get( name );
    }

    void setKey( final Integer key )
    {
        this.key = key;
    }

    void setPorts( final Map<String, Integer> ports )
    {
        this.ports = ports;
    }

    public boolean isSane()
    {
        for ( final Integer port : ports.values() )
        {
            if ( port < 1024 || port > 65535 )
            {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( key == null ) ? 0 : key.hashCode() );
        return result;
    }

    @Override
    public boolean equals( final Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        final PortConfiguration other = (PortConfiguration) obj;
        if ( key == null )
        {
            if ( other.key != null )
            {
                return false;
            }
        }
        else if ( !key.equals( other.key ) )
        {
            return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "PortReservation [key=" )
          .append( key )
          .append( ", ports:" );
        for ( final Map.Entry<String, Integer> entry : ports.entrySet() )
        {
            sb.append( "\n" )
              .append( entry.getKey() )
              .append( ":\t\t" )
              .append( entry.getValue() );
        }
        sb.append( "\n]" );

        return sb.toString();
    }

    @Override
    public int compareTo( final PortConfiguration other )
    {
        return key.compareTo( other.key );
    }

    @Override
    public Iterator<Entry<String, Integer>> iterator()
    {
        return new HashMap<String, Integer>( ports ).entrySet()
                                                    .iterator();
    }

}
