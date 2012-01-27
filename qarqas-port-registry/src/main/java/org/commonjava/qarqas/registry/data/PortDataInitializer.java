package org.commonjava.qarqas.registry.data;

import static org.commonjava.qarqas.registry.model.PortConfiguration.STANDARD;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Singleton;

import org.commonjava.qarqas.registry.model.PortConfiguration;
import org.commonjava.qarqas.registry.model.PortConfigurationBuilder;

@Singleton
public class PortDataInitializer
{

    public static Set<PortConfiguration> createReservationDefinitions()
    {
        final Set<PortConfiguration> reservations = new HashSet<PortConfiguration>();
        reservations.add( STANDARD );

        for ( int i = 1; i < 64; i++ )
        {
            final PortConfigurationBuilder rb = new PortConfigurationBuilder().key( i );
            for ( final Map.Entry<String, Integer> entry : STANDARD )
            {
                final int port = entry.getValue() + ( i * 1000 );
                rb.port( entry.getKey(), port );
            }

            final PortConfiguration res = rb.build();
            reservations.add( res );
        }

        return reservations;
    }

}
