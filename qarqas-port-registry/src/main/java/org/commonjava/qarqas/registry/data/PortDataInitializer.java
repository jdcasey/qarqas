package org.commonjava.qarqas.registry.data;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Singleton;

import org.commonjava.qarqas.registry.model.PortConfiguration;
import org.commonjava.qarqas.registry.model.PortReservationBuilder;

@Singleton
public class PortDataInitializer
{

    private static final PortConfiguration STANDARD_RESERVATION =
        new PortReservationBuilder().key( 0 )
                                    .port( "http", 8080 )
                                    .port( "https", 8443 )
                                    .port( "jacorb", 3528 )
                                    .port( "jacorb-ssl", 3529 )
                                    .port( "jmx-connector-registry", 1090 )
                                    .port( "jmx-connector-server", 1091 )
                                    .port( "management-native", 9999 )
                                    .port( "management-http", 9990 )
                                    .port( "messaging", 5445 )
                                    .port( "messaging-throughput", 5455 )
                                    .port( "osgi-http", 8090 )
                                    .port( "remoting", 4447 )
                                    .port( "txn-recovery-environment", 4712 )
                                    .port( "txn-status-manager", 4713 )
                                    .build();

    public static Set<PortConfiguration> createReservationDefinitions()
    {
        final Set<PortConfiguration> reservations = new HashSet<PortConfiguration>();
        reservations.add( STANDARD_RESERVATION );

        for ( int i = 1; i < 64; i++ )
        {
            final PortReservationBuilder rb = new PortReservationBuilder().key( i );
            for ( final Map.Entry<String, Integer> entry : STANDARD_RESERVATION )
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
